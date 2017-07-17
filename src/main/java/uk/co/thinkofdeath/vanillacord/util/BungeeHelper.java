package uk.co.thinkofdeath.vanillacord.util;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class BungeeHelper {

    private static final Gson gson = new Gson();
    public static AttributeKey<UUID> UUID_KEY = AttributeKey.valueOf("spoofed-uuid");
    public static AttributeKey<Property[]> PROPERTIES_KEY = AttributeKey.valueOf("spoofed-props");

    public static void parseHandshake(Object networkManager, Object handshake) {
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

            String host = null;
            for (Field field : handshake.getClass().getDeclaredFields()) {
                if (field.getType().equals(String.class)) {
                    field.setAccessible(true);
                    host = (String) field.get(handshake);
                }
            }

            if (host == null || channel == null || socket == null) {
                throw new RuntimeException("Hook failed");
            }

            String[] split = host.split("\00");
            if (split.length != 4) {
                throw new RuntimeException("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
            }

            // split[0]; Vanilla doesn't use this
            socket.setAccessible(true);
            socket.set(networkManager, new InetSocketAddress(split[1], ((InetSocketAddress) socket.get(networkManager)).getPort()));

            String uuid = split[2];
            channel.attr(UUID_KEY).set(UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32)));

            channel.attr(PROPERTIES_KEY).set(gson.fromJson(split[3], Property[].class));
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    public static GameProfile injectProfile(Object networkManager, GameProfile gameProfile) {
        try {
            Channel channel = null;
            for (Field field : networkManager.getClass().getDeclaredFields()) {
                if (field.getType().equals(Channel.class)) {
                    field.setAccessible(true);
                    channel = (Channel) field.get(networkManager);
                }
            }
            if (channel == null) {
                throw new RuntimeException("Hook failed");
            }

            GameProfile profile = new GameProfile(channel.attr(UUID_KEY).get(), gameProfile.getName());
            for (Property property : channel.attr(PROPERTIES_KEY).get()) {
                profile.getProperties().put(property.getName(), property);
            }
            return profile;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
