package uk.co.thinkofdeath.vanillacord.helper;

import com.mojang.authlib.properties.Property;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.co.thinkofdeath.vanillacord.helper.BungeeHelper.*;

@SuppressWarnings("ConstantConditions")
public class VelocityHelper {

    private static final Object NAMESPACE = NamespacedKey.construct("velocity", "player_info");
    static final AttributeKey<Integer> TRANSACTION_ID_KEY = AttributeKey.valueOf("-vch-transaction");
    static final AttributeKey<Object> INTERCEPTED_PACKET_KEY = AttributeKey.valueOf("-vch-intercepted");

    private static byte[] seecret = null;
    private static int lastTID = Integer.MIN_VALUE;

    public static void initializeTransaction(Object networkManager, Object intercepted) {
        try {
            Channel channel = (Channel) NetworkManager.channel.get(networkManager);
            if (channel.attr(TRANSACTION_ID_KEY).get() != null) {
                throw new IllegalStateException("Unexpected login request");
            }

            // Reserve a unique key
            int key;
            synchronized (NAMESPACE) {
                if (lastTID == Integer.MAX_VALUE) lastTID = Integer.MIN_VALUE;
                key = ++lastTID;
            }
            channel.attr(TRANSACTION_ID_KEY).set(key);
            channel.attr(INTERCEPTED_PACKET_KEY).set(intercepted);

            // Send the packet
            Object qObject = LoginRequestPacket.construct(key, NAMESPACE, PacketData.construct(new EmptyByteBuf(ByteBufAllocator.DEFAULT)));
            NetworkManager.sendPacket(networkManager, qObject);

        } catch (Exception e) {
            throw exception(null, e);
        }
    }

    public static void completeTransaction(Object networkManager, Object loginManager, Object response, String secret) {
        try {
            Channel channel = (Channel) NetworkManager.channel.get(networkManager);
            Object intercepted = channel.attr(INTERCEPTED_PACKET_KEY).getAndSet(null);
            if (intercepted == null) {
                throw new IllegalStateException("Unexpected login response");
            }

            // Retrieve & release the previously generated unique key
            int key = LoginResponsePacket.getTransactionID(response);
            ByteBuf data = LoginResponsePacket.getData(response);

            if (key != channel.attr(TRANSACTION_ID_KEY).get()) {
                throw QuietException.notify("Invalid transaction ID: " + key);
            } if (data == null) {
                throw QuietException.notify("If you wish to use modern IP forwarding, please enable it in your Velocity config as well!");
            }


            // Validate the signature on the data
            {
                byte[] received = new byte[32];
                byte[] raw = new byte[data.readableBytes() - received.length];

                data.readBytes(received);
                data.copy(received.length, raw.length).readBytes(raw);
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(getSecret(secret), mac.getAlgorithm()));
                mac.update(raw);
                byte[] calculated = mac.doFinal();
                if (!Arrays.equals(calculated, received)) {
                    throw QuietException.notify("Received invalid IP forwarding data. Did you use the right forwarding secret?");
                }
            }

            readVarInt(data); // we don't do anything with the protocol version at this time

            // Retrieve IP forwarding data
            NetworkManager.socket.set(networkManager, new InetSocketAddress(readString(data), ((InetSocketAddress) NetworkManager.socket.get(networkManager)).getPort()));
            channel.attr(UUID_KEY).set(new UUID(data.readLong(), data.readLong()));

            readString(data); // we don't do anything with the username field

            Property[] properties = new Property[readVarInt(data)];
            for (int i = 0; i < properties.length; ++i) {
                properties[i] = new Property(readString(data), readString(data), (data.readBoolean())? readString(data) : null);
            }
            channel.attr(PROPERTIES_KEY).set(properties);

            // Continue login flow
            LoginListener.handleIntercepted(loginManager, intercepted);
        } catch (Exception e) {
            throw exception(null, e);
        }
    }

    private static byte[] getSecret(String def) throws IOException {
        if (seecret == null) {
            File config = new File("seecret.txt");
            if (config.exists()) {
                Properties properties = new Properties();
                try (FileInputStream reader = new FileInputStream(config)) {
                    properties.load(reader);

                    String secret = properties.getProperty("modern-forwarding-secret");
                    if (secret == null || secret.length() == 0) secret = def;
                    seecret = secret.getBytes(UTF_8);
                }
            } else {
                seecret = def.getBytes(UTF_8);
                PrintWriter writer = new PrintWriter(config, UTF_8.name());
                writer.println("# Hey, there. We know you already gave VanillaCord a default secret key to use,");
                writer.println("# but if you ever need to change it, you can do so here without re-installing the patches.");
                writer.println("# ");
                writer.println("# This file is automatically generated by VanillaCord once a player attempts to join the server.");
                writer.println();
                writer.println("modern-forwarding-secret=");
                writer.close();
            }
        }
        return seecret;
    }

    private static String readString(ByteBuf buf) {
        int len = readVarInt(buf);
        if (len > Short.MAX_VALUE * 4) {
            throw new RuntimeException("String is too long");
        }

        byte[] b = new byte[len];
        buf.readBytes(b);

        String s = new String(b, UTF_8);
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

    // Pre-calculate references to obfuscated classes
    static final class NetworkManager {
        public static final Field channel = BungeeHelper.NetworkManager.channel;
        public static final Field socket = BungeeHelper.NetworkManager.socket;

        public static void sendPacket(Object instance, Object packet) {
            throw exception("Class generation failed", new NoSuchMethodError());
        }
    }

    static final class LoginListener {

        public static void handleIntercepted(Object instance, Object packet) {
            throw exception("Class generation failed", new NoSuchMethodError());
        }
    }

    static final class NamespacedKey {

        public static Object construct(String name, String id) {
            throw exception("Class generation failed", new NoSuchMethodError());
        }
    }

    static final class PacketData {

        public static ByteBuf construct(ByteBuf data) {
            throw exception("Class generation failed", new NoSuchMethodError());
        }
    }

    static final class LoginRequestPacket {
        private static final Field transactionID;
        private static final Field namespace;
        private static final Field data;

        static {
            try {
                Class<?> clazz = (Class<?>) (Object) "VCTR-LoginRequestPacket";

                transactionID = clazz.getDeclaredField("VCFR-LoginRequestPacket-TransactionID");
                transactionID.setAccessible(true);

                namespace = clazz.getDeclaredField("VCFR-LoginRequestPacket-Namespace");
                namespace.setAccessible(true);

                data = clazz.getDeclaredField("VCFR-LoginRequestPacket-Data");
                data.setAccessible(true);
            } catch (Throwable e) {
                throw exception("Class generation failed", e);
            }
        }

        public static Object construct(int transactionID, Object namespace, ByteBuf data) {
            try {
                Object packet = "VCIR-LoginRequestPacket-Construct";

                LoginRequestPacket.transactionID.set(packet, transactionID);
                LoginRequestPacket.namespace.set(packet, namespace);
                LoginRequestPacket.data.set(packet, PacketData.construct(data));

                return packet;
            } catch (Exception e) {
                throw exception(null, e);
            }
        }
    }

    static final class LoginResponsePacket {
        private static final Field transactionID;
        private static final Field data;

        static {
            try {
                Class<?> clazz = (Class<?>) (Object) "VCTR-LoginResponsePacket";

                transactionID = clazz.getDeclaredField("VCFR-LoginResponsePacket-TransactionID");
                transactionID.setAccessible(true);

                data = clazz.getDeclaredField("VCFR-LoginResponsePacket-Data");
                data.setAccessible(true);
            } catch (Throwable e) {
                throw exception("Class generation failed", e);
            }
        }

        public static int getTransactionID(Object instance) {
            try {
                return (int) LoginResponsePacket.transactionID.get(instance);
            } catch (Exception e) {
                throw exception(null, e);
            }
        }

        public static ByteBuf getData(Object instance) {
            try {
                return (ByteBuf) LoginResponsePacket.data.get(instance);
            } catch (Exception e) {
                throw exception(null, e);
            }
        }
    }
}
