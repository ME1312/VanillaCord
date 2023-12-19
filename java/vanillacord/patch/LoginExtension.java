package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import vanillacord.packaging.Package;

import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class LoginExtension extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {
    private final Package file;

    public LoginExtension(Package file) {
        super(ASM9);
        this.file = file;
    }

    @Override
    public ClassVisitor apply(ClassVisitor visitor) {
        cv = visitor;
        return this;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & ACC_STATIC) != 0 && file.sources.receive.name.equals(name) && file.sources.receive.descriptor.equals(descriptor)) {
            return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                private Label label = new Label();
                private int state = 0;

                @Override
                public void visitCode() {
                    super.visitCode();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                            file.sources.receive.arguments[0].type.getInternalName(),
                            "readBoolean",
                            "()Z",
                            false

                    );
                    mv.visitJumpInsn(IFEQ, label);
                }

                @Override
                public void visitLabel(Label label) {
                    mv.visitLabel(label);
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if ("Payload may not be larger than 1048576 bytes".equals(value)) {
                        state = 1;
                    }
                    super.visitLdcInsn(value);
                }

                @Override
                public void visitVarInsn(int opcode, int index) {
                    if (state == 1 && opcode == ALOAD && index == 0) {
                        mv.visitTypeInsn(NEW, "vanillacord/translation/LoginExtension");
                        mv.visitInsn(DUP);
                        state = 2;
                    }
                    super.visitVarInsn(opcode, index);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (state == 2 && opcode == INVOKEVIRTUAL && descriptor.startsWith("(I)")) {
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                owner,
                                "readBytes",
                                "(I)Lio/netty/buffer/ByteBuf;",
                                false
                        );
                        mv.visitMethodInsn(INVOKESPECIAL,
                                "vanillacord/translation/LoginExtension",
                                "<init>",
                                "(Lio/netty/buffer/ByteBuf;)V",
                                false
                        );
                        mv.visitInsn(ARETURN);
                        mv.visitLabel(label);
                        state = 3;
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state == 3 && opcode == POP) {
                        state = 4;
                        return;
                    }
                    super.visitInsn(opcode);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (state < 3) throw new IllegalStateException("Rewrite failed: 0x0" + state);
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
