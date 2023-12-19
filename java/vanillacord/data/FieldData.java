package vanillacord.data;

import bridge.asm.KnownType;
import org.objectweb.asm.Type;

public class FieldData {
    public final ClassData owner;
    public final int access;
    public final KnownType type;
    public final String name, descriptor, signature;
    public final Object value;

    public FieldData(ClassData owner, int access, String name, String descriptor, String signature, Object value) {
        this.owner = owner;
        this.access = access;
        this.type = owner.types.load(Type.getType(descriptor));
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.value = value;
    }
}
