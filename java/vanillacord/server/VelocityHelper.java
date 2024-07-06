package vanillacord.server;

import bridge.Invocation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import vanillacord.translation.LoginExtension;
import vanillacord.translation.LoginListener;
import vanillacord.translation.NamespacedKey;
import vanillacord.translation.PlayerConnection;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("SpellCheckingInspection")
public class VelocityHelper extends ForwardingHelper {
    private static final Object NAMESPACE = new Invocation(NamespacedKey.class).ofMethod("new").with("velocity").with("player_info").invoke();
    private static final AttributeKey<Object> LOGIN_KEY = new AttributeKey<>("-vch-login");
    private static final AttributeKey<GameProfile> PROFILE_KEY = new AttributeKey<>("-vch-profile");
    private final byte[][] seecrets;

    VelocityHelper(LinkedList<String> seecrets) {
        int length, i = 0;
        byte[][] array = this.seecrets = new byte[length = seecrets.size()][];
        for (Iterator<String> it = seecrets.iterator(); i != length;) {
            array[i++] = it.next().getBytes(UTF_8);
        }
    }

    public boolean initializeTransaction(Object connection, Object intercepted) {
        try {
            Channel channel = new Invocation(PlayerConnection.class).ofMethod("getChannel").with(connection).invoke();
            if (channel.attr(LOGIN_KEY).get() != null)
                throw new IllegalStateException("Unexpected login request");
            if (channel.attr(PROFILE_KEY).get() != null) {
                return false;
            }

            // Send the packet
            channel.attr(LOGIN_KEY).set(intercepted);
            new Invocation(LoginExtension.class).ofMethod("send")
                    .with(connection)
                    .with(0)
                    .with(NAMESPACE)
                    .with(ByteBuf.class, new EmptyByteBuf(channel.alloc()))
                    .invoke();

        } catch (Exception e) {
            throw QuietException.show(e);
        }
        return true;
    }

    public boolean completeTransaction(Object connection, Object login, Object response) {
        try {
            Channel channel = new Invocation(PlayerConnection.class).ofMethod("getChannel").with(connection).invoke();
            Object intercepted = channel.attr(LOGIN_KEY).get();
            if (intercepted == null) {
                return false;
            }

            // Check the metadata
            int id = new Invocation(LoginExtension.class).ofMethod("getTransactionID").with(response).invoke();
            ByteBuf data = new Invocation(LoginExtension.class).ofMethod("getData").with(response).invoke();

            if (id != 0)
                throw QuietException.notify("Unknown transaction ID: " + id);
            if (data == null)
                throw QuietException.notify("If you wish to use modern IP forwarding, please enable it in your Velocity config as well!");
            if (invalidSignature(data)) {
                throw QuietException.notify("Received invalid IP forwarding data. Did you use the right forwarding secret?");
            }

            // Retrieve IP forwarding data
            readVarint(data); // we don't do anything with the protocol version at this time

            new Invocation(PlayerConnection.class).ofMethod("setAddress").with(connection).with(readString(data)).invoke();
            GameProfile profile = new GameProfile(new UUID(data.readLong(), data.readLong()), readString(data));
            channel.attr(PROFILE_KEY).set(profile);

            PropertyMap properties = profile.getProperties();
            for (int i = 0, length = readVarint(data); i < length; ++i) {
                final String name = readString(data);
                properties.put(name, new Property(name, readString(data), (data.readBoolean())? readString(data) : null));
            }

            // Continue login flow
            try {
                new Invocation(LoginListener.class).ofMethod("hello")
                        .with(login)
                        .with(intercepted)
                        .invoke();

            } finally {
                channel.attr(LOGIN_KEY).set(null);
            }
        } catch (Exception e) {
            throw QuietException.show(e);
        }
        return true;
    }

    @Override
    public GameProfile injectProfile(Object connection, String username) {
        try {
            return ((Channel) new Invocation(PlayerConnection.class).ofMethod("getChannel").with(connection).invoke()).attr(PROFILE_KEY).get();
        } catch (Exception e) {
            throw QuietException.show(e);
        }
    }

    private boolean invalidSignature(ByteBuf data) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] signature = new byte[32];
        data.readBytes(signature);

        byte[] raw = new byte[data.readableBytes()];
        data.readBytes(raw).readerIndex(signature.length);
        Mac mac = Mac.getInstance("HmacSHA256");
        for (byte[] seecret : seecrets) {
            mac.init(new SecretKeySpec(seecret, "HmacSHA256"));
            mac.update(raw);
            if (Arrays.equals(signature, mac.doFinal())) {
                return false;
            }
        }
        return true;
    }

    private static String readString(ByteBuf buf) {
        int len = readVarint(buf);
        if (len > Short.MAX_VALUE * 3) {
            throw new RuntimeException("String is too long");
        }

        String s = buf.toString(buf.readerIndex(), len, UTF_8);
        buf.readerIndex(buf.readerIndex() + len);
        if (s.length() > Short.MAX_VALUE) {
            throw new RuntimeException("String is too long");
        }

        return s;
    }

    private static int readVarint(ByteBuf input) {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = input.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);

            if (bytes > 5) {
                throw new RuntimeException("Varint is too big");
            }
        } while ((in & 0x80) == 0x80);

        return out;
    }
}
