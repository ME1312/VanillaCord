package uk.co.thinkofdeath.vanillacord;

import org.objectweb.asm.*;

public class HandshakeListener extends ClassVisitor {

    private final TypeChecker typeChecker;
    private final boolean secure;

    private String fieldName;
    private String fieldDesc;

    private String thisName;
    private String handshake;

    public HandshakeListener(ClassVisitor cv, TypeChecker typeChecker, boolean secure) {
        super(Opcodes.ASM9, cv);
        this.typeChecker = typeChecker;
        this.secure = secure;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        thisName = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!desc.contains("Minecraft")) {
            fieldName = name;
            fieldDesc = desc;
        }
        return super.visitField(access, name, desc, signature, value);


    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!secure && name.equals(typeChecker.hsName) && desc.equals(typeChecker.hsDesc)) {
            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                private boolean waitVirt;

                @Override
                public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                    waitVirt = true;
                    super.visitLookupSwitchInsn(dflt, keys, labels);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);

                    if (waitVirt && opcode == Opcodes.INVOKEVIRTUAL) {
                        System.out.println("Hooking");
                        waitVirt = false;

                        mv.visitLabel(new Label());
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitFieldInsn(Opcodes.GETFIELD, thisName, fieldName, fieldDesc);
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "uk/co/thinkofdeath/vanillacord/helper/BungeeHelper",
                                "parseHandshake",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V", false
                        );
                    }
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    public String getHandshake() {
        return Type.getMethodType(typeChecker.hsDesc).getArgumentTypes()[0].getInternalName();
    }

    public String getNetworkManager() {
        return Type.getMethodType(fieldDesc).getInternalName();
    }
}
