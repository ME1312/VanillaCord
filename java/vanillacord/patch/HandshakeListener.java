package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import vanillacord.data.FieldData;
import vanillacord.packaging.Package;

import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;
public class HandshakeListener extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {
    private final Package file;
    private final FieldData connection;

    public HandshakeListener(Package file) throws Throwable {
        super(ASM9);
        this.file = file;
        for (FieldData field : file.sources.handshake.owner.fields.values()) {
            if ((field.access & ACC_STATIC) == 0 && !field.descriptor.equals("Lnet/minecraft/server/MinecraftServer;")) {
                this.connection = file.sources.connection = field;
                return;
            }
        }
        throw new ClassNotFoundException("Connection class not found");
    }

    @Override
    public ClassVisitor apply(ClassVisitor visitor) {
        cv = visitor;
        return this;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (file.sources.handshake.name.equals(name) && file.sources.handshake.descriptor.equals(descriptor)) {
            return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                private boolean vanilla = true;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    if (vanilla && opcode == INVOKEVIRTUAL && file.sources.connection.type.type.getInternalName().equals(owner)) {
                        mv.visitLabel(new Label());
                        mv.visitFieldInsn(GETSTATIC,
                                "vanillacord/server/VanillaCord",
                                "helper",
                                "Lvanillacord/server/ForwardingHelper;"
                        );
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD,
                                connection.owner.clazz.type.getInternalName(),
                                connection.name,
                                connection.descriptor
                        );
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "vanillacord/server/ForwardingHelper",
                                "parseHandshake",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                                false
                        );
                        vanilla = false;
                    }
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (vanilla) throw new IllegalStateException("Hook failed");
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
