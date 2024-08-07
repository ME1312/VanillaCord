package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import vanillacord.data.FieldData;
import vanillacord.data.MethodData;
import vanillacord.packaging.Package;

import java.util.LinkedList;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class LoginListener extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {
    private final Package file;
    private final MethodData extension;

    public LoginListener(Package file) throws Throwable {
        super(ASM9);
        this.file = file;
        for (FieldData field : file.sources.login.owner.fields.values()) {
            if ((field.access & ACC_STATIC) == 0 && field.type.equals(file.sources.connection.type)) {
                file.sources.connection = field;
            }
        }
        if (file.sources.receive != null) {
            for (MethodData method : file.sources.login.owner.methods.values()) {
                if (method.arguments.length == 1) {
                    if (file.sources.receive.owner.clazz.equals(method.arguments[0])) {
                        extension = method;
                        return;
                    } else if (file.sources.send.owner.clazz.equals(method.arguments[0])) {
                        extension = method;
                        method = file.sources.send;
                        file.sources.send = file.sources.receive;
                        file.sources.receive = method;
                        return;
                    }
                }
            }
        }
        extension = null;
    }

    @Override
    public ClassVisitor apply(ClassVisitor visitor) {
        cv = visitor;
        return this;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & ACC_STATIC) == 0 && descriptor.equals("(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;")) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitFieldInsn(GETSTATIC,
                    "vanillacord/server/VanillaCord",
                    "helper",
                    "Lvanillacord/server/ForwardingHelper;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD,
                    file.sources.connection.owner.clazz.type.getInternalName(),
                    file.sources.connection.name,
                    file.sources.connection.descriptor
            );
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "com/mojang/authlib/GameProfile",
                    "getName",
                    "()Ljava/lang/String;",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "vanillacord/server/ForwardingHelper",
                    "injectProfile",
                    "(Ljava/lang/Object;Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;",
                    false
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return null;
        } else if (extension != null && extension.name.equals(name) && extension.descriptor.equals(descriptor)) {
            return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                private boolean prelabel = true;

                @Override
                public void visitLabel(Label label) {
                    super.visitLabel(label);
                    if (prelabel) {
                        label = new Label();
                        mv.visitFieldInsn(GETSTATIC,
                                "vanillacord/server/VanillaCord",
                                "helper",
                                "Lvanillacord/server/ForwardingHelper;"
                        );
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD,
                                file.sources.connection.owner.clazz.type.getInternalName(),
                                file.sources.connection.name,
                                file.sources.connection.descriptor
                        );
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "vanillacord/server/ForwardingHelper",
                                "completeTransaction",
                                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
                                false
                        );
                        mv.visitJumpInsn(IFEQ, label);
                        mv.visitInsn(RETURN);
                        mv.visitLabel(label);
                        prelabel = false;
                    }
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (prelabel) throw new IllegalStateException("Hook failed");
                }
            };
        } else if (file.sources.login.name.equals(name) && file.sources.login.descriptor.equals(descriptor)) {
            return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                private final LinkedList<Runnable> undo = new LinkedList<Runnable>();
                private final MethodVisitor mv = super.mv;
                private byte state = 0;
                private Label skip;

                @Override
                public void visitVarInsn(int opcode, int index) {
                    if (state == 1 && opcode == ALOAD && index == 0) {
                        undo.add(() -> super.visitVarInsn(ALOAD, 0));
                        state = 2;
                        return;
                    }
                    undo();
                    if (state == 6 && opcode == ALOAD && index == 0) {
                        undo.add(() -> super.visitVarInsn(ALOAD, 0));
                        return;
                    }
                    super.visitVarInsn(opcode, index);
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state == 0 && opcode == RETURN) {
                        state = 1;
                    } else {
                        undo();
                    }
                    super.visitInsn(opcode);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    if (state == 0 && opcode == PUTFIELD && desc.equals("Lcom/mojang/authlib/GameProfile;")) {
                        state = 1;

                    } else if (state == 2 && opcode == GETFIELD && desc.equals("Lnet/minecraft/server/MinecraftServer;")) {
                        state = 3;
                        undo.add(() -> super.visitFieldInsn(GETFIELD, owner, name, "Lnet/minecraft/server/MinecraftServer;"));
                        return;
                    } else if (state == 6 && opcode == GETFIELD && desc.equals("Ljava/lang/String;") && file.sources.login.owner.clazz.type.getInternalName().equals(owner)) {
                        state = 7;
                        undo.add(() -> super.visitFieldInsn(GETFIELD, owner, name, "Ljava/lang/String;"));
                        return;
                    } else {
                        undo();
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                    if (state == 3 && opcode == INVOKEVIRTUAL && desc.equals("()Z")) {
                        super.mv = null;
                        undo.clear();
                        state = 4;
                        return;
                    } else if (state == 7 && opcode == INVOKESTATIC && desc.equals("(Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;")) {
                        mv.visitFieldInsn(GETSTATIC,
                                "vanillacord/server/VanillaCord",
                                "helper",
                                "Lvanillacord/server/ForwardingHelper;"
                        );
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(
                                GETFIELD,
                                file.sources.connection.owner.clazz.type.getInternalName(),
                                file.sources.connection.name,
                                file.sources.connection.descriptor
                        );
                        undo();
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "vanillacord/server/ForwardingHelper",
                                "injectProfile",
                                "(Ljava/lang/Object;Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;",
                                false
                        );
                        state = 8;
                        return;
                    } else {
                        undo();
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                }

                @Override
                public void visitJumpInsn(int opcode, Label label) {
                    if (state == 4 && opcode == IFEQ) {
                        state = 5;
                        skip = label;
                    } else {
                        undo();
                    }
                    super.visitJumpInsn(opcode, label);
                }

                @Override
                public void visitLabel(Label label) {
                    if (state == 5 && skip == label) {
                        super.mv = mv;
                        state = 6;
                    }
                    super.visitLabel(label);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    undo();
                    super.visitTypeInsn(opcode, type);
                }

                @Override
                public void visitEnd() {
                    if (state != 8 && state != 6) throw new IllegalStateException("Inject failed: 0x0" + state);
                    super.visitEnd();
                }

                private void undo() {
                    byte s;
                    if ((s = state) > 1 && s < 5) {
                        for (Runnable action : undo) action.run();
                        undo.clear();
                        state = 1;
                    } else if (s > 5 && s < 8) {
                        for (Runnable action : undo) action.run();
                        undo.clear();
                        state = 6;
                    }
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
