package uk.co.thinkofdeath.vanillacord;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.logging.LogManager;

public class BungeeHelper extends HelperVisitor {
    private final Class<?> networkManager;
    private final Class<?> handshakePacket;
    private boolean useFields = true;

    public BungeeHelper(LinkedHashMap<String, byte[]> queue, ClassWriter classWriter, String networkManager, String handshakePacket) throws ClassNotFoundException {
        super(queue, classWriter);

        // The following code prevents some unrelated messages from appearing
        PrintStream err = System.err;
        System.setErr(new QuietStream());
        LogManager.getLogManager().reset();
        try {
            this.networkManager = Class.forName(Type.getType(networkManager).getClassName());
            this.handshakePacket = Class.forName(handshakePacket);
        } finally {
            System.setErr(err);
        }
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
        if (tag.equals("NetworkManager::getAttribute")) {
            try {
                Type attribute = Type.getType(AttributeKey.class);
                Constructor<?> constructor = AttributeKey.class.getConstructor(String.class);

                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitTypeInsn(Opcodes.NEW, attribute.getInternalName());
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        attribute.getInternalName(),
                        "<init>",
                        Type.getConstructorDescriptor(constructor), false
                );
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
                return null;
            } catch (NoSuchMethodException e) {
                return mv;
            }
        } else if (tag.equals("Handshake::getHostName")) {
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

    private static final class QuietStream extends PrintStream {
        private QuietStream() {
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // This is a quiet stream
                }
            });
        }
    }
}
