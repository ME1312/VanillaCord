package vanillacord.translation;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.data.ClassData;
import vanillacord.data.FieldData;
import vanillacord.packaging.Package;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static bridge.asm.Types.push;
import static org.objectweb.asm.Opcodes.*;
import static vanillacord.translation.Translations.*;

public final class PlayerConnection {
    private PlayerConnection() {}

    public static void translate(Package file, ZipOutputStream stream) throws IOException {
        HierarchicalWriter cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String type = Type.getInternalName(PlayerConnection.class);
        String channel = null, socket = null;

        for (FieldData field : ((ClassData) file.sources.connection.type.data()).fields.values()) {
            switch (field.descriptor) {
                case "Lio/netty/channel/Channel;":
                    channel = field.name;
                    break;
                case "Ljava/net/SocketAddress;":
                    socket = field.name;
                    break;
            }
        }

        cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", null);
        cv.visitField(VCT_FIELD, "getChannel", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
        cv.visitField(VCT_FIELD, "getAddress", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
        cv.visitField(VCT_FIELD, "setAddress", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

        MethodVisitor mv = cv.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitMethodInsn(INVOKESTATIC,
                    "java/lang/invoke/MethodHandles",
                    "lookup",
                    "()Ljava/lang/invoke/MethodHandles$Lookup;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.connection.type);
            push(mv, channel);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectGetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "getChannel",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.connection.type);
            push(mv, socket);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitInsn(DUP_X1);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectGetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "getAddress",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectSetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "setAddress",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "getChannel", "(Ljava/lang/Object;)Lio/netty/channel/Channel;", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "getChannel",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.connection.type.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.connection.descriptor + ")Lio/netty/channel/Channel;",
                    false
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "setAddress", "(Ljava/lang/Object;Ljava/lang/String;)V", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "setAddress",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.connection.type.type.getInternalName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitTypeInsn(NEW, "java/net/InetSocketAddress");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "getAddress",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.connection.descriptor + ")Ljava/net/SocketAddress;",
                    false
            );
            mv.visitTypeInsn(CHECKCAST, "java/net/InetSocketAddress");
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/net/InetSocketAddress",
                    "getPort",
                    "()I"
            );
            mv.visitMethodInsn(INVOKESPECIAL,
                    "java/net/InetSocketAddress",
                    "<init>",
                    "(Ljava/lang/String;I)V"
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.connection.descriptor + "Ljava/net/SocketAddress;)V",
                    false
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cv.visitEnd();
        stream.putNextEntry(new ZipEntry(type + ".class"));
        stream.write(cv.toByteArray());
    }
}
