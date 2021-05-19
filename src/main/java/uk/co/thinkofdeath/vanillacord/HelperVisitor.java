package uk.co.thinkofdeath.vanillacord;

import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

public abstract class HelperVisitor extends ClassVisitor {
    protected final HashMap<String, String> values = new HashMap<>();
    private final LinkedHashMap<String, byte[]> queue;

    protected HelperVisitor(LinkedHashMap<String, byte[]> queue, ClassWriter writer) {
        super(Opcodes.ASM9, writer);
        this.queue = queue;
    }

    protected abstract void generate();

    Class<?> getClass(String value) throws ClassNotFoundException {
        return Class.forName(value.substring(0, value.length() - 6));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        generate();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);

        try {
            ClassReader classReader = new ClassReader(Main.class.getResourceAsStream('/' + name + ".class"));
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            String replacement;
                            if (value instanceof String && (replacement = values.get(value)) != null) {
                                super.visitLdcInsn(replacement);
                            } else {
                                super.visitLdcInsn(value);
                            }
                        }
                    };
                }
            }, 0);
            queue.put(name + ".class", classWriter.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
