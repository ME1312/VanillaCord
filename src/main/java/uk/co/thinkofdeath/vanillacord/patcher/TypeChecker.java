package uk.co.thinkofdeath.vanillacord.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TypeChecker extends ClassVisitor {

    private final boolean secure;

    private int fCount = 0;
    private boolean hasTID = false;

    private boolean handshakeListener;
    private boolean loginListener;
    private boolean cbQuery, sbQuery;
    String hsName;
    String hsDesc;

    public TypeChecker(boolean secure) {
        super(Opcodes.ASM9);
        this.secure = secure;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (secure && (access & Opcodes.ACC_STATIC) == 0) {
            if (!hasTID && descriptor.equals("I")) hasTID = true;
            ++fCount;
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        return new MethodVisitor(api) {

            @Override
            public void visitLdcInsn(Object cst) {
                if (cst instanceof String) {
                    if (hasTID && "Payload may not be larger than 1048576 bytes".equals(cst)) {
                        cbQuery = fCount == 2;
                        sbQuery = fCount == 3;
                    }
                    if ("multiplayer.disconnect.incompatible".equals(cst) || "multiplayer.disconnect.outdated_server".equals(cst) || ((String) cst).startsWith("Outdated client! Please use")) {
                        handshakeListener = true;
                        hsName = name;
                        hsDesc = desc;
                    }
                    if ("Unexpected hello packet".equals(cst)) {
                        loginListener = true;
                    }
                }
            }
        };
    }

    public boolean isHandshakeListener() {
        return handshakeListener;
    }

    public boolean isLoginListener() {
        return loginListener;
    }

    public boolean isClientQuery() {
        return cbQuery;
    }

    public boolean isServerQuery() {
        return sbQuery;
    }
}
