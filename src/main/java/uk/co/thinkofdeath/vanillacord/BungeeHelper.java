package uk.co.thinkofdeath.vanillacord;

import io.netty.channel.Channel;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.LinkedHashMap;

public class BungeeHelper extends HelperVisitor {
    private final Class<?> networkManager;
    private final Class<?> handshakePacket;

    public BungeeHelper(LinkedHashMap<String, byte[]> queue, ClassWriter classWriter, String networkManager, String handshakePacket) throws ClassNotFoundException {
        super(queue, classWriter);
        this.networkManager = Class.forName(Type.getType(networkManager).getClassName());
        this.handshakePacket = Class.forName(handshakePacket);
    }

    @Override
    protected void generate() {
        values.put("VCTR-NetworkManager", Type.getType(networkManager));
        {
            for (Field field : networkManager.getDeclaredFields()) {
                if (field.getType().equals(Channel.class)) {
                    values.put("VCFR-NetworkManager-Channel", field.getName());
                } else if (field.getType().equals(SocketAddress.class)) {
                    values.put("VCFR-NetworkManager-Socket", field.getName());
                }
            }
        }

        values.put("VCTR-HandshakePacket", Type.getType(handshakePacket));
        {
            for (Field field : handshakePacket.getDeclaredFields()) {
                if (field.getType().equals(String.class)) {
                    values.put("VCFR-HandshakePacket-HostName", field.getName());
                }
            }
        }
    }
}
