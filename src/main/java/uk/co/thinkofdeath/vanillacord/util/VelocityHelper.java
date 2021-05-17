package uk.co.thinkofdeath.vanillacord.util;

import com.google.common.base.Charsets;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.co.thinkofdeath.vanillacord.util.BungeeHelper.UUID_KEY;
import static uk.co.thinkofdeath.vanillacord.util.BungeeHelper.PROPERTIES_KEY;

public class VelocityHelper {

    private static final Object SYNC = new Object();
    private static final AttributeKey<Integer> TRANSACTION_ID_KEY = AttributeKey.valueOf("vc-transaction");
    private static final AttributeKey<Object> INTERCEPTED_PACKET_KEY = AttributeKey.valueOf("vc-intercepted");
    private static byte[] seecret = null;
    private static int lastTID = Integer.MIN_VALUE;

    private static void classSearch(Class<?> next, ArrayList<Class<?>> types) {
        types.add(next);

        if (next.getSuperclass() != null && !types.contains(next.getSuperclass())) types.add(next.getSuperclass());
        for (Class<?> c : next.getInterfaces()) if (!types.contains(c)) {
            classSearch(c, types);
        }
    }

    public static void initializeTransaction(Object networkManager, String queryLocation, Object intercepted) {
        try {
            Channel channel = null;
            for (Field field : networkManager.getClass().getDeclaredFields()) {
                if (field.getType().equals(Channel.class)) {
                    field.setAccessible(true);
                    channel = (Channel) field.get(networkManager);
                    break;
                }
            }

            if (channel.attr(TRANSACTION_ID_KEY).get() != null) {
                throw new RuntimeException("Unexpected login request");
            }

            // Reserve a unique key
            int key;
            synchronized (SYNC) {
                if (lastTID == Integer.MAX_VALUE) lastTID = Integer.MIN_VALUE;
                key = ++lastTID;
            }
            channel.attr(TRANSACTION_ID_KEY).set(key);
            channel.attr(INTERCEPTED_PACKET_KEY).set(intercepted);

            // Construct the packet
            Class<?> qClass = Class.forName(queryLocation);
            ArrayList<Class<?>> qTypes = new ArrayList<>();
            classSearch(qClass, qTypes);

            Constructor<?> qConstruct = null;
            for (Constructor<?> c : qClass.getConstructors()) {
                if (c.getParameterCount() == 3) {
                    qConstruct = c;
                    break;
                }
            }
            Object qObject;
            if (qConstruct == null) {
                qObject = qClass.getConstructor().newInstance();
                for (Field f : qClass.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == int.class) {
                        f.set(qObject, key);
                    } else if (ByteBuf.class.isAssignableFrom(f.getType())) {
                        f.set(qObject, f.getType().getConstructor(ByteBuf.class).newInstance(new EmptyByteBuf(ByteBufAllocator.DEFAULT)));
                    } else if (!f.getType().isPrimitive()) {
                        f.set(qObject, f.getType().getConstructor(String.class, String.class).newInstance("velocity", "player_info"));
                    }
                }
            } else {
                Object namespace = qConstruct.getParameterTypes()[1].getConstructor(String.class, String.class).newInstance("velocity", "player_info");
                Object payload = qConstruct.getParameterTypes()[2].getConstructor(ByteBuf.class).newInstance(new EmptyByteBuf(ByteBufAllocator.DEFAULT));
                qObject = qConstruct.newInstance(key, namespace, payload);
            }

            // Send the packet
            Method sender = null;
            for (Method m : networkManager.getClass().getMethods()) {
                if (m.getParameterCount() == 1) {
                    Class<?> param = m.getParameterTypes()[0];
                    boolean found = false;
                    for (Class<?> type : qTypes) {
                        if (param == type) {
                            sender = m;
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }

            sender.invoke(networkManager, qObject);

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    public static void completeTransaction(Object networkManager, Object loginManager, Object response, String secret) {
        try {
            Channel channel = null;
            Field socket = null;
            for (Field field : networkManager.getClass().getDeclaredFields()) {
                if (field.getType().equals(Channel.class)) {
                    field.setAccessible(true);
                    channel = (Channel) field.get(networkManager);
                }
                if (field.getType().equals(SocketAddress.class)) {
                    socket = field;
                }
            }

            Object intercepted = channel.attr(INTERCEPTED_PACKET_KEY).getAndSet(null);
            if (intercepted == null) {
                throw new RuntimeException("Unexpected login response");
            }

            // Retrieve & release the previously generated unique key
            Integer key = null;
            ByteBuf data = null;
            for (Method m : response.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 0) {
                    if (m.getReturnType() == int.class) {
                        key = (int) m.invoke(response);
                    } else if (ByteBuf.class.isAssignableFrom(m.getReturnType())) {
                        data = (ByteBuf) m.invoke(response);
                    }
                }
            }

            if (key == null) {
                for (Field f : response.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == int.class) {
                        key = (int) f.get(response);
                    } else if (ByteBuf.class.isAssignableFrom(f.getType())) {
                        data = (ByteBuf) f.get(response);
                    }
                }
            }

            if (key == null || !key.equals(channel.attr(TRANSACTION_ID_KEY).get())) {
                throw new RuntimeException("Invalid transaction ID: " + key);
            } if (data == null) {
                throw new RuntimeException("If you wish to use modern IP forwarding, please enable it in your Velocity config as well!");
            }


            // Validate the signature on the data
            {
                byte[] received = new byte[32];
                byte[] raw = new byte[data.readableBytes() - received.length];

                data.readBytes(received);
                data.copy(received.length, raw.length).readBytes(raw);
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(parseSecret(secret), mac.getAlgorithm()));
                mac.update(raw);
                byte[] calculated = mac.doFinal();
                if (!Arrays.equals(calculated, received)) {
                    StringBuilder s1 = new StringBuilder();
                    for (int i=0; i < calculated.length; i++) {
                        s1.append(Integer.toString((calculated[i] & 0xff) + 0x100, 16).substring(1));
                    }
                    StringBuilder s2 = new StringBuilder();
                    for (int i=0; i < received.length; i++) {
                        s2.append(Integer.toString((received[i] & 0xff) + 0x100, 16).substring(1));
                    }

                    throw new RuntimeException("Received invalid IP forwarding data: " + s1 + " != " + s2);
                }
            }

            // Compare versioning information
            int version = readVarInt(data); /*
            if (version != 1) {
                throw new RuntimeException("Received incompatible IP forwarding data")
            } */

            // Retrieve IP forwarding data
            socket.setAccessible(true);
            socket.set(networkManager, new InetSocketAddress(readString(data), ((InetSocketAddress) socket.get(networkManager)).getPort()));
            channel.attr(UUID_KEY).set(new UUID(data.readLong(), data.readLong()));

            readString(data); // we don't do anything with the username field

            Property[] properties = new Property[readVarInt(data)];
            for (int i = 0; i < properties.length; ++i) {
                properties[i] = new Property(readString(data), readString(data), (data.readBoolean())? readString(data) : null);
            }
            channel.attr(PROPERTIES_KEY).set(properties);

            // Continue login flow
            Method next = null;
            for (Method m : loginManager.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == intercepted.getClass()) {
                    next = m;
                    break;
                }
            }

            next.invoke(loginManager, intercepted);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static String readString(ByteBuf buf) {
        int len = readVarInt(buf);
        if (len > Short.MAX_VALUE * 4) {
            throw new RuntimeException("String is too long");
        }

        byte[] b = new byte[len];
        buf.readBytes(b);

        String s = new String(b, Charsets.UTF_8);
        if (s.length() > Short.MAX_VALUE) {
            throw new RuntimeException("String is too long");
        }

        return s;
    }

    private static int readVarInt(ByteBuf input) {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = input.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);

            if (bytes > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((in & 0x80) == 0x80);

        return out;
    }

    private static byte[] parseSecret(String def) throws IOException {
        if (seecret == null) {
            File config = new File("seecret.txt");
            if (config.exists()) {
                Properties properties = new Properties();
                try (FileInputStream reader = new FileInputStream(config)) {
                    properties.load(reader);
                    seecret = properties.getProperty("modern-forwarding-secret", def).getBytes(UTF_8);
                }
            } else {
                seecret = def.getBytes(UTF_8);
                PrintWriter writer = new PrintWriter(config);
                writer.println("# Hey, there. We know you already patched in a default secret key for VanillaCord to use,");
                writer.println("# but if you ever need to change it, you can do so here without re-installing the patches.");
                writer.println("# ");
                writer.println("# This file is automatically generated by VanillaCord once a player attempts to join the server.");
                writer.println();
                writer.println("modern-forwarding-secret=" + def);
                writer.close();
            }
        }
        return seecret;
    }
}
