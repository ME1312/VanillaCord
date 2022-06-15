package uk.co.thinkofdeath.vanillacord.patcher;

import com.mojang.authlib.GameProfile;
import org.objectweb.asm.*;

import java.util.LinkedList;

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
            private final LinkedList<Runnable> undo = new LinkedList<Runnable>();
            private int state = 0;

            @Override
            public void visitLdcInsn(Object cst) {
                if (cst.equals("Unexpected hello packet")) {
                    if (state != 0) throw new IllegalStateException("Inject failed");
                    packetName = methodArgs.getArgumentTypes()[0].getInternalName();
                    state = 1;
                } else {
                    undo();
                }
                super.visitLdcInsn(cst);
            }

            @Override
            public void visitVarInsn(int opcode, int index) {
                if (state == 2 && opcode == Opcodes.ALOAD && index == 0) {
                    undo.add(() -> super.visitVarInsn(opcode, index));
                    state = 3;
                    return;
                } else {
                    undo();
                }
                super.visitVarInsn(opcode, index);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (state == 1 && opcode == Opcodes.PUTFIELD && desc.contains("GameProfile")) {
                    state = 2;

                } else if (state == 3 && opcode == Opcodes.GETFIELD && desc.contains("Minecraft")) {
                    undo.add(() -> super.visitFieldInsn(opcode, owner, name, desc));
                    state = 4;
                    return;
                } else {
                    undo();
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                if (state == 4 && opcode == Opcodes.INVOKEVIRTUAL && desc.equals("()Z")) {
                    mv.visitInsn(Opcodes.ICONST_0);
                    state = -1;
                    return;
                } else {
                    undo();
                }
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            }

            @Override
            public void visitEnd() {
                if (state > 0) throw new IllegalStateException("Inject failed");
                super.visitEnd();
            }

            private void undo() {
                if (state > 2) {
                    for (Runnable action : undo) action.run();
                    undo.clear();
                    state = 2;
                }
            }
            // Only unexpected operations beyond this point
            @Override
            public void visitInsn(int opcode) {
                undo();
                super.visitInsn(opcode);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                undo();
                super.visitInvokeDynamicInsn(name, descriptor, handle, args);
            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                undo();
                super.visitJumpInsn(opcode, label);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                undo();
                super.visitTypeInsn(opcode, type);
            }
        };
    }

    public String getPacket() {
        return packetName;
    }
}
