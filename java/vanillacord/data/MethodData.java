package vanillacord.data;

import bridge.asm.KnownType;
import org.objectweb.asm.Type;

public class MethodData {
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
        this.arguments = owner.types.load(desc.getArgumentTypes());
        this.returns = owner.types.load(desc.getReturnType());
        this.exceptions = owner.types.loadClass(exceptions);
        this.signature = signature;
    }
}
