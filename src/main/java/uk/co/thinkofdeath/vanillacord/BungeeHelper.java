package uk.co.thinkofdeath.vanillacord;

import io.netty.channel.Channel;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.LinkedHashMap;

public class BungeeHelper extends HelperVisitor {
    private final Class<?> networkManager;
    private final Class<?> handshakePacket;
    private boolean useFields = true;

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
            useFields = true;
            for (Method m : handshakePacket.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0) {
                    if (m.getReturnType() == String.class) {
                        useFields = false;
                        values.put("VCM-HandshakePacket-GetHostName", m);
                        break;
                    }
                }
            }

            if (useFields) {
                for (Field field : handshakePacket.getDeclaredFields()) {
                    if (field.getType().equals(String.class)) {
                        values.put("VCFR-HandshakePacket-HostName", field.getName());
                    }
                }
            }
        }
    }

    @Override
    protected MethodVisitor rewriteMethod(String tag, MethodVisitor mv) {
        if (tag.equals("Handshake::getHostName")) {
            if (useFields) {
                return mv;
            } else {
                Type handshake = (Type) values.get("VCTR-HandshakePacket");
                Method method = (Method) values.get("VCM-HandshakePacket-GetHostName");

                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.CHECKCAST, handshake.getInternalName());
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        handshake.getInternalName(),
                        method.getName(),
                        Type.getMethodDescriptor(method), false
                );
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
                return null;
            }
        } else {
            throw NOT_WRITTEN;
        }
    }

    @Override
    protected boolean keepMethod(String tag) {
        return useFields || !tag.equals("Handshake::<clinit>");
    }

    @Override
    protected boolean keepField(String tag) {
        return useFields || !tag.startsWith("Handshake.");
    }
}
