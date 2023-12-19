package vanillacord.translation;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.data.ClassData;
import vanillacord.data.FieldData;
import vanillacord.data.MethodData;
import vanillacord.packaging.Package;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static bridge.asm.Types.push;
import static org.objectweb.asm.Opcodes.*;
import static vanillacord.translation.Translations.*;

public class HandshakePacket {

    public static void translate(Package file, ZipOutputStream stream) throws IOException {
        HierarchicalWriter cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassData handshake = (ClassData) file.sources.handshake.arguments[0].data();
        String type = Type.getInternalName(HandshakePacket.class);
        String host = null;

        for (MethodData method : handshake.methods.values()) {
            if ((method.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC && method.arguments.length == 0
                    && method.returns.type.equals(Type.getObjectType("java/lang/String")) && !method.name.equals("toString")) {
                host = method.name;
                break;
            }
        }

        cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", null);
        if (host != null) {
            MethodVisitor mv = cv.visitMethod(VCT_METHOD, "getHostName", "(Ljava/lang/Object;)Ljava/lang/String;", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(CHECKCAST, handshake.clazz.type.getInternalName());
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        handshake.clazz.type.getInternalName(),
                        host,
                        "()Ljava/lang/String;",
                        false
                );
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        } else { // 1.17 > version
            cv.visitField(VCT_FIELD, "getHostName", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

            for (FieldData field : handshake.fields.values()) {
                if (field.descriptor.equals("Ljava/lang/String;")) {
                    host = field.name;
                    break;
                }
            }

            MethodVisitor mv = cv.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitMethodInsn(INVOKESTATIC,
                        "java/lang/invoke/MethodHandles",
                        "lookup",
                        "()Ljava/lang/invoke/MethodHandles$Lookup;",
                        false
                );
                push(mv, handshake.clazz);
                push(mv, host);
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
                        "getHostName",
                        "Ljava/lang/invoke/MethodHandle;"
                );
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            mv = cv.visitMethod(VCT_METHOD, "getHostName", "(Ljava/lang/Object;)Ljava/lang/String;", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitFieldInsn(GETSTATIC,
                        type,
                        "getHostName",
                        "Ljava/lang/invoke/MethodHandle;"
                );
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(CHECKCAST, handshake.clazz.type.getInternalName());
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandle",
                        "invokeExact",
                        '(' + handshake.clazz.type.getDescriptor() + ")Ljava/lang/String;",
                        false
                );
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        cv.visitEnd();
        stream.putNextEntry(new ZipEntry(type + ".class"));
        stream.write(cv.toByteArray());
    }
}
