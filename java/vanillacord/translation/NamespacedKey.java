package vanillacord.translation;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.packaging.Package;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.*;
import static vanillacord.translation.Translations.VCT_CLASS;
import static vanillacord.translation.Translations.VCT_METHOD;

public class NamespacedKey {

    public static void translate(Package file, ZipOutputStream stream) throws IOException {
        HierarchicalWriter cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String type = Type.getInternalName(NamespacedKey.class);

        cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", null);
        MethodVisitor mv = cv.visitMethod(VCT_METHOD, "new", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitTypeInsn(NEW, file.sources.namespace.clazz.type.getInternalName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL,
                    file.sources.namespace.clazz.type.getInternalName(),
                    "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;)V",
                    false
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cv.visitEnd();
        stream.putNextEntry(new ZipEntry(type + ".class"));
        stream.write(cv.toByteArray());
    }
}
