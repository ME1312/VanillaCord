package uk.co.thinkofdeath.vanillacord;

import com.mojang.authlib.GameProfile;
import org.objectweb.asm.*;

public class LoginListener extends ClassVisitor {
    private final String networkManager;
    private String fieldName;
    private String fieldDesc;
    private String thisName;

    public LoginListener(ClassWriter classWriter, String networkManager) {
        super(Opcodes.ASM5, classWriter);
        this.networkManager = "L" + networkManager + ";";
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        thisName = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (desc.equals(networkManager)) {
            fieldName = name;
            fieldDesc = desc;
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Type methodArgs = Type.getMethodType(desc);
        if (methodArgs.getArgumentTypes().length == 1
                && methodArgs.getArgumentTypes()[0].equals(Type.getType(GameProfile.class))
                && methodArgs.getReturnType().equals(Type.getType(GameProfile.class))) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, thisName, fieldName, fieldDesc);
            mv.visitVarInsn(Opcodes.ASTORE, 2);

            mv.visitLabel(new Label());
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "uk/co/thinkofdeath/vanillacord/util/BungeeHelper",
                    "injectProfile",
                    "(Ljava/lang/Object;Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
