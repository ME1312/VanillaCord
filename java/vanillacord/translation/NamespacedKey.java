package vanillacord.translation;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.data.ClassData;
import vanillacord.packaging.Package;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static bridge.asm.Types.push;
import static org.objectweb.asm.Opcodes.*;
import static vanillacord.translation.Translations.*;

public final class NamespacedKey {
    private NamespacedKey() {}

    public static void translate(Package file, ZipOutputStream stream) throws IOException {
        HierarchicalWriter cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String type = Type.getInternalName(NamespacedKey.class);
        ClassData namespace;

        cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", null);
        if (((namespace = file.sources.namespace).methods.get("<init>(Ljava/lang/String;Ljava/lang/String;)V").access & ACC_PUBLIC) == 0) {
            cv.visitField(VCT_FIELD, "new", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

            MethodVisitor mv = cv.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitMethodInsn(INVOKESTATIC,
                        "java/lang/invoke/MethodHandles",
                        "lookup",
                        "()Ljava/lang/invoke/MethodHandles$Lookup;",
                        false
                );
                push(mv, namespace.clazz);
                push(mv, 1);
                push(mv, 2);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
                mv.visitInsn(DUP_X1);
                mv.visitInsn(DUP_X1);
                push(mv, 0);
                push(mv, Type.getObjectType("java/lang/String"));
                mv.visitInsn(DUP_X2);
                mv.visitInsn(AASTORE);
                mv.visitInsn(AASTORE);
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/Class",
                        "getDeclaredConstructor",
                        "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                        false
                );
                mv.visitInsn(DUP);
                push(mv, true);
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/reflect/Constructor",
                        "setAccessible",
                        "(Z)V",
                        false
                );
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "unreflectConstructor",
                        "(Ljava/lang/reflect/Constructor;)Ljava/lang/invoke/MethodHandle;",
                        false
                );
                mv.visitFieldInsn(PUTSTATIC,
                        type,
                        "new",
                        "Ljava/lang/invoke/MethodHandle;"
                );
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            mv = cv.visitMethod(VCT_METHOD, "new", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitFieldInsn(GETSTATIC,
                        type,
                        "new",
                        "Ljava/lang/invoke/MethodHandle;"
                );
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandle",
                        "invokeExact",
                        "(Ljava/lang/String;Ljava/lang/String;)" + namespace.clazz.type.getDescriptor(),
                        false
                );
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        } else { // 1.21 > version
            MethodVisitor mv = cv.visitMethod(VCT_METHOD, "new", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitTypeInsn(NEW, namespace.clazz.type.getInternalName());
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL,
                        namespace.clazz.type.getInternalName(),
                        "<init>",
                        "(Ljava/lang/String;Ljava/lang/String;)V",
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
