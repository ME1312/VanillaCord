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
                private byte state = 0;

                public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                    super.visitLookupSwitchInsn(dflt, keys, labels);
                    if (state == 0) state = 1;
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    if (state == 1 && opcode == INVOKEVIRTUAL) {
                        mv.visitLabel(new Label());
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD,
                                connection.owner.clazz.type.getInternalName(),
                                connection.name,
                                connection.descriptor
                        );
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "vanillacord/server/VanillaCord",
                                "parseHandshake",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                                false
                        );
                        state = 2;
                    }
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (state != 2) throw new IllegalStateException("Hook failed: 0x0" + state);
                }
            };
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
