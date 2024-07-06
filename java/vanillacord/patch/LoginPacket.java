package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.packaging.Package;

import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class LoginPacket extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {
    private final Package file;
    public LoginPacket(Package file) throws Throwable {
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
        Type[] args;
        if ((access & ACC_BRIDGE) == 0 && descriptor.endsWith(")V") &&
                (args = Type.getMethodType(descriptor).getArgumentTypes()).length == 1 &&
                file.sources.login.owner.clazz.implemented(file.types.load(args[0]))
        ) {
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
                        mv.visitVarInsn(ALOAD, 1);
                        if (!file.sources.login.owner.clazz.type.equals(args[0])) {
                            mv.visitTypeInsn(CHECKCAST, file.sources.login.owner.clazz.type.getInternalName());
                        }
                        mv.visitFieldInsn(GETFIELD,
                                file.sources.connection.owner.clazz.type.getInternalName(),
                                file.sources.connection.name,
                                file.sources.connection.descriptor
                        );
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "vanillacord/server/ForwardingHelper",
                                "initializeTransaction",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
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
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
