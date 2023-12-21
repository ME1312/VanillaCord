package vanillacord.update;

import org.objectweb.asm.MethodVisitor;

import java.util.Locale;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

class AttributeKey extends MethodVisitor {
    AttributeKey(MethodVisitor visitor) {
        super(ASM9, visitor);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == NEW && type.equals("io/netty/util/AttributeKey")) mv = new MethodVisitor(ASM9, mv) {
            private Boolean state;

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
                    (AttributeKey.this.mv = mv).visitMethodInsn(INVOKESTATIC,
                            "io/netty/util/AttributeKey",
                            "valueOf",
                            "(Ljava/lang/String;)Lio/netty/util/AttributeKey;",
                            false
                    );
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
        };
        super.visitTypeInsn(opcode, type);
    }
}
