package uk.co.thinkofdeath.vanillacord.helper;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public class BungeeHelper {

    private static final Gson GSON = new Gson();
    static final AttributeKey<UUID> UUID_KEY = AttributeKey.valueOf("-vch-uuid");
    static final AttributeKey<Property[]> PROPERTIES_KEY = AttributeKey.valueOf("-vch-properties");

    public static void parseHandshake(Object networkManager, Object handshake) {
        try {
            Channel channel = (Channel) NetworkManager.channel.get(networkManager);
            String host = (String) Handshake.hostName.get(handshake);

            String[] split = host.split("\00");
            if (split.length != 3 && split.length != 4) {
                throw new QuietException("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
            }

            // split[0]; // Vanilla doesn't use this
            NetworkManager.socket.set(networkManager, new InetSocketAddress(split[1], ((InetSocketAddress) NetworkManager.socket.get(networkManager)).getPort()));

            String uuid = split[2];
            channel.attr(UUID_KEY).set(UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32)));
            channel.attr(PROPERTIES_KEY).set(GSON.fromJson((split.length > 3)?split[3]:"[]", Property[].class));
        } catch (Exception e) {
            throw exception(null, e);
        }
    }

    public static GameProfile injectProfile(Object networkManager, GameProfile gameProfile) {
        try {
            Channel channel = (Channel) NetworkManager.channel.get(networkManager);

            GameProfile profile = new GameProfile(channel.attr(UUID_KEY).get(), gameProfile.getName());
            for (Property property : channel.attr(PROPERTIES_KEY).get()) {
                profile.getProperties().put(property.getName(), property);
            }
            return profile;
        } catch (Exception e) {
            throw exception(null, e);
        }
    }

    static RuntimeException exception(String text, Throwable e) {
        if (e instanceof QuietException) {
            return (QuietException) e;
        } else if (e.getCause() instanceof QuietException) {
            return (QuietException) e.getCause();
        } else {
            if (text != null) e = new RuntimeException(text, e);
            e.printStackTrace();
            return new QuietException(e);
        }
    }

    // Pre-calculate reflection for obfuscated references
    static final class NetworkManager {
        public static final Class<?> clazz;
        public static final Field channel;
        public static final Field socket;

        static {
            try {
                clazz = (Class<?>) (Object) "VCTR-NetworkManager";

                channel = clazz.getDeclaredField("VCFR-NetworkManager-Channel");
                channel.setAccessible(true);

                socket = clazz.getDeclaredField("VCFR-NetworkManager-Socket");
                socket.setAccessible(true);
            } catch (Throwable e) {
                throw exception("Class generation failed", e);
            }
        }
    }
    static final class Handshake {
        public static final Class<?> clazz;
        public static final Field hostName;

        static {
            try {
                clazz = (Class<?>) (Object) "VCTR-HandshakePacket";

                hostName = clazz.getDeclaredField("VCFR-HandshakePacket-HostName");
                hostName.setAccessible(true);
            } catch (Throwable e) {
                throw exception("Class generation failed", e);
            }
        }
    }
}
