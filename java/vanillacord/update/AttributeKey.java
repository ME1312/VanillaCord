package vanillacord.update;

import bridge.asm.LinkedVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Locale;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

class AttributeKey extends MethodVisitor implements LinkedVisitor {
    AttributeKey(MethodVisitor delegate) {
        super(ASM9, delegate);
    }

    @Override
    public <T extends MethodVisitor> T setDelegate(T value) {
        mv = value;
        return value;
    }

    @Override
    public MethodVisitor getParent() {
        return null;
    }

    @Override
    public <T extends MethodVisitor> T setParent(T value) {
        return value;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == NEW && type.equals("io/netty/util/AttributeKey")) {
            final class Constructor extends MethodVisitor implements LinkedVisitor {
                private MethodVisitor parent;
                private Boolean state;

                private Constructor(MethodVisitor delegate) {
                    super(ASM9, delegate);
                    if (delegate instanceof LinkedVisitor) {
                        ((LinkedVisitor) delegate).setParent(this);
                    }
                }

                @Override
                public <T extends MethodVisitor> T setDelegate(T value) {
                    mv = value;
                    return value;
                }

                @Override
                public MethodVisitor getParent() {
                    return parent;
                }

                @Override
                public <T extends MethodVisitor> T setParent(T value) {
                    parent = value;
                    return value;
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (state == null && opcode == NEW) {
                        state = Boolean.FALSE;
                    } else {
                        super.visitTypeInsn(opcode, type);
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state != Boolean.TRUE && opcode == DUP) {
                        state = Boolean.TRUE;
                    } else {
                        super.visitInsn(opcode);
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (state != null && descriptor.equals("(Ljava/lang/String;)V")) {
                        mv.visitMethodInsn(INVOKESTATIC,
                                "io/netty/util/AttributeKey",
                                "valueOf",
                                "(Ljava/lang/String;)Lio/netty/util/AttributeKey;",
                                false
                        );
                        if (((LinkedVisitor) parent).setDelegate(mv) instanceof LinkedVisitor) ((LinkedVisitor) mv).setParent(parent);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }

                @Override
                public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                public void visitMaxs(int maxStack, int maxLocals) {}
                public void visitEnd() {
                    throw new IllegalStateException("Method ended despite incomplete update [" + Objects.toString(state).toUpperCase(Locale.ROOT) + ']');
                }
            }
            setDelegate(new Constructor(mv)).setParent(this);
        }
        super.visitTypeInsn(opcode, type);
    }
}
