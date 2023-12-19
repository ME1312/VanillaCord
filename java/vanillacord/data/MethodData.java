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
        Type desc = Type.getMethodType(descriptor);
        KnownType[] ex = new KnownType[(exceptions != null)? exceptions.length : 0];
        for (int i = 0; i < ex.length; ++i) {
            ex[i] = owner.types.load(Type.getObjectType(exceptions[i]));
        }
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.arguments = owner.types.load(desc.getArgumentTypes());
        this.returns = owner.types.load(desc.getReturnType());
        this.exceptions = ex;
        this.signature = signature;
    }
}
