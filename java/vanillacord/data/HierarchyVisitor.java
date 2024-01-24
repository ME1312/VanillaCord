package vanillacord.data;

import bridge.asm.HierarchyScanner;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import vanillacord.packaging.Package;

public class HierarchyVisitor extends HierarchyScanner {
    private final Package file;
    private ClassData data;
    private boolean hasTID;

    public HierarchyVisitor(Package file) {
        super(Opcodes.ASM9, file.types);
        this.file = file;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String extended, String[] implemented) {
        super.visit(version, access, name, signature, extended, implemented);
        super.data = this.data = new ClassData(file.types, compile(), signature);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        this.data.fields.put(name, new FieldData(this.data, access, name, descriptor, signature, value));
        if ((access & Opcodes.ACC_STATIC) == 0) {
            if (!hasTID && descriptor.equals("I")) hasTID = true;
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        final MethodData data;
        this.data.methods.put(name + descriptor, data = new MethodData(this.data, access, name, descriptor, signature, exceptions));
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof String) {
                    if ("Server console handler".equals(value)) {
                        System.out.print("Found the dedicated server: ");
                        System.out.println(HierarchyVisitor.super.name);
                        file.sources.startup = data;
                    } else if ("Unexpected hello packet".equals(value)) {
                        System.out.print("Found the login listener: ");
                        System.out.println(HierarchyVisitor.super.name);
                        file.sources.login = data;
                    } else if (hasTID && "Payload may not be larger than 1048576 bytes".equals(value)) {
                        System.out.print("Found a login extension packet: ");
                        System.out.println(HierarchyVisitor.super.name);
                        if (file.sources.send == null) {
                            file.sources.send = data;
                        } else {
                            file.sources.receive = data;
                        }
                    } else if ("multiplayer.disconnect.incompatible".equals(value) || "multiplayer.disconnect.outdated_server".equals(value) || ((String) value).startsWith("Outdated client! Please use")) {
                        System.out.print("Found the handshake listener: ");
                        System.out.println(HierarchyVisitor.super.name);
                        file.sources.handshake = data;
                    }
                }
                super.visitLdcInsn(value);
            }
        };
    }
}
