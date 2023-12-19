package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.function.Function;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class HandshakePacket extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {

    public HandshakePacket() {
        super(ASM9);
    }

    @Override
    public ClassVisitor apply(ClassVisitor visitor) {
        cv = visitor;
        return this;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {

            @Override
            public void visitIntInsn(int opcode, int operand) {
                super.visitIntInsn(opcode, (opcode != SIPUSH || operand != 255)? operand : Short.MAX_VALUE);
            }
        };
    }
}
