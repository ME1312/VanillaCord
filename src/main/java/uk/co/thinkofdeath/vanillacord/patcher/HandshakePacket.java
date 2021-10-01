package uk.co.thinkofdeath.vanillacord.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class HandshakePacket extends ClassVisitor {
    public HandshakePacket(ClassWriter classWriter) {
        super(Opcodes.ASM9, classWriter);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
            @Override
            public void visitIntInsn(int opcode, int operand) {
                if (opcode == Opcodes.SIPUSH && operand == 255) {
                    super.visitIntInsn(opcode, Short.MAX_VALUE);
                } else {
                    super.visitIntInsn(opcode, operand);
                }
            }
        };
    }
}
