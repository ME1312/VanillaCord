package uk.co.thinkofdeath.vanillacord.patcher;

import com.mojang.authlib.GameProfile;
import org.objectweb.asm.*;

public class LoginListener extends ClassVisitor {
    private final String networkManager;
    private final Class<?> clientQuery;
    private final boolean secure;
    private final String secret;
    private String packetName;
    String fieldName;
    String fieldDesc;
    String thisName;

    public LoginListener(ClassWriter classWriter, String networkManager, String clientQuery, String secret) throws ClassNotFoundException {
        super(Opcodes.ASM9, classWriter);
        this.networkManager = networkManager;
        this.secure = secret != null;
        this.secret = secret;
        this.clientQuery = (clientQuery != null)?Class.forName(clientQuery.substring(0, clientQuery.length() - 6)):null;
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
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "uk/co/thinkofdeath/vanillacord/helper/BungeeHelper",
                    "injectProfile",
                    "(Ljava/lang/Object;Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            return null;
        }
        if (secure && methodArgs.getArgumentTypes().length == 1
                && methodArgs.getArgumentTypes()[0].equals(Type.getType(clientQuery))
                && methodArgs.getReturnType().equals(Type.VOID_TYPE)) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();

            mv.visitLabel(new Label());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, thisName, fieldName, fieldDesc);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(secret);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "uk/co/thinkofdeath/vanillacord/helper/VelocityHelper",
                    "completeTransaction",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V", false
            );
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            return null;
        }
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
            private byte state = 0;

            @Override
            public void visitLdcInsn(Object cst) {
                if (cst.equals("Unexpected hello packet")) {
                    if (state != 0) throw new IllegalStateException("Inject failed");
                    packetName = methodArgs.getArgumentTypes()[0].getInternalName();
                    state = 1;
                }
                super.visitLdcInsn(cst);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (state == 1 && opcode == Opcodes.INVOKEVIRTUAL && desc.contains("GameProfile")) {
                    state = 2;

                } else if (state == 4) {
                    state = 5;
                    mv.visitInsn(Opcodes.ICONST_0);
                    return;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (state == 3) {
                    if (desc.contains("GameProfile")) {
                        state = 2;
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                    } else {
                        state = 4;
                        return;
                    }
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                if (state == 2 && opcode == Opcodes.ALOAD && var == 0) {
                    state = 3;
                    return;
                }
                super.visitVarInsn(opcode, var);
            }
        };
    }

    public String getPacket() {
        return packetName;
    }
}
