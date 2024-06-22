package vanillacord.data;

import bridge.asm.KnownType;
import org.objectweb.asm.Type;

public final class MethodData {
    public final ClassData owner;
    public final int access;
    public final String name, descriptor, signature;
    public final KnownType[] arguments, exceptions;
    public final KnownType returns;

    MethodData(ClassData owner, int access, String name, String descriptor, String signature, String[] exceptions) {
        final Type desc = Type.getMethodType(descriptor);
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.arguments = owner.map.load(desc.getArgumentTypes());
        this.returns = owner.map.load(desc.getReturnType());
        this.exceptions = owner.map.loadClass(exceptions);
        this.signature = signature;
    }
}
