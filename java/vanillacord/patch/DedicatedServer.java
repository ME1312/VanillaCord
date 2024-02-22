package vanillacord.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import vanillacord.Patcher;
import vanillacord.packaging.Package;

import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class DedicatedServer extends ClassVisitor implements Function<ClassVisitor, ClassVisitor> {
    private final Package file;
    private boolean branded;

    public DedicatedServer(Package file) {
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
        if (file.sources.startup.name.equals(name) && file.sources.startup.descriptor.equals(descriptor)) {
            return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                private byte state = 0;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    if (state == 0 && opcode == INVOKEVIRTUAL && owner.equals("java/lang/Thread") && descriptor.equals("()V")) {
                        state = 1;
                    } else if (state == 3 && opcode == INVOKEINTERFACE && owner.endsWith("/Logger")) {
                        mv.visitFieldInsn(GETSTATIC,
                                "vanillacord/server/VanillaCord",
                                "helper",
                                "Lvanillacord/server/ForwardingHelper;"
                        );
                        mv.visitInsn(POP);
                        state = 4;
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                    if (state == 1 && descriptor.endsWith("/Logger;")) {
                        mv.visitInsn(DUP);
                        mv.visitLdcInsn(Patcher.brand);
                        mv.visitMethodInsn(INVOKEINTERFACE,
                                descriptor.substring(1, descriptor.length() - 1),
                                "info",
                                "(Ljava/lang/String;)V",
                                true
                        );
                        state = 2;
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    super.visitLdcInsn(value);
                    if (state == 2 && "Loading properties".equals(value)) {
                        state = 3;
                    }
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (state != 4) throw new IllegalStateException("Hook failed: 0x0" + state);
                }
            };
        } else if ("getServerModName".equals(name) && "()Ljava/lang/String;".equals(descriptor)) {
            if (!branded) brand(super.visitMethod(access, name, descriptor, signature, exceptions));
            return null;
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (!branded) brand(super.visitMethod(ACC_PUBLIC, "getServerModName", "()Ljava/lang/String;", null, null));
        super.visitEnd();
    }

    private void brand(MethodVisitor mv) {
        mv.visitCode();
        mv.visitLabel(new Label());
        mv.visitLdcInsn(Patcher.brand.substring(0, Patcher.brand.indexOf(' ')));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        branded = true;
    }
}
