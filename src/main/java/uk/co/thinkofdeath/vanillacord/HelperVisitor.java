package uk.co.thinkofdeath.vanillacord;

import com.google.common.primitives.Primitives;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

public abstract class HelperVisitor extends ClassVisitor {
    protected final HashMap<String, Object> values = new HashMap<>();
    private final LinkedHashMap<String, byte[]> queue;

    protected HelperVisitor(LinkedHashMap<String, byte[]> queue, ClassWriter writer) {
        super(Opcodes.ASM9, writer);
        this.queue = queue;
    }

    protected abstract void generate();

    protected boolean keepField(String tag) {
        return true;
    }

    protected boolean keepMethod(String tag) {
        return true;
    }

    protected static final RuntimeException NOT_WRITTEN = new NotWrittenException();
    protected MethodVisitor rewriteMethod(String tag, MethodVisitor mv) {
        throw NOT_WRITTEN;
    }

    Class<?> getClass(String value) throws ClassNotFoundException {
        return Class.forName(value.substring(0, value.length() - 6));
    }

    Class<?> getClass(Type value) throws ClassNotFoundException {
        return Class.forName(value.getClassName());
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

                @Override // Replace volatile primitive constants with final ones
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    if (keepField(innerName + '.' + name)) {
                        if ((access & Opcodes.ACC_FINAL) == 0) {
                            int sort = Type.getType(desc).getSort();
                            if ((Type.ARRAY > sort && sort > Type.VOID)) {
                                Object replacement = values.get("VCCR-" + innerName + '-' + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1));
                                if (replacement != null && Primitives.isWrapperType(replacement.getClass())) {
                                    return super.visitField((access & ~Opcodes.ACC_VOLATILE) | Opcodes.ACC_FINAL, name, desc, signature, replacement);
                                }
                            }
                        }
                        return super.visitField(access, name, desc, signature, value);
                    } else {
                        return null;
                    }
                }

                @Override // Replace direct string constants in <clinit>
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    String tag = innerName + "::" + name;
                    if (keepMethod(tag)) {
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                        try {
                            return rewriteMethod(tag, mv);
                        } catch (NotWrittenException e) {
                            return new MethodVisitor(Opcodes.ASM9, mv) {
                                boolean ignorecast = false;

                                @Override
                                public void visitLdcInsn(Object value) {
                                    Object replacement;
                                    if (value instanceof String && (replacement = values.get(value)) != null) {
                                        ignorecast = !(replacement instanceof String);
                                        super.visitLdcInsn(replacement);
                                    } else {
                                        super.visitLdcInsn(value);
                                    }
                                }

                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    if (ignorecast && opcode == Opcodes.CHECKCAST) {
                                        ignorecast = false;
                                    } else {
                                        super.visitTypeInsn(opcode, type);
                                    }
                                }
                            };
                        }
                    } else {
                        return null;
                    }
                }
            }, 0);
            queue.put(name + ".class", classWriter.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final class NotWrittenException extends RuntimeException {
        // This exception exists only for its type reference
    }

}
