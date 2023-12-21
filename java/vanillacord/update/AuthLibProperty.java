package vanillacord.update;

import org.objectweb.asm.MethodVisitor;

import java.util.Locale;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

class AuthLibProperty extends AttributeKey {
    AuthLibProperty(MethodVisitor visitor) {
        super(visitor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == INVOKEVIRTUAL && owner.equals("com/mojang/authlib/properties/Property")) switch (name) {
            case "getName":
            case "getValue":
            case "getSignature":
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        owner,
                        name.substring(3, 4).toLowerCase(Locale.ROOT) + name.substring(4),
                        descriptor,
                        isInterface
                );
                return;
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
