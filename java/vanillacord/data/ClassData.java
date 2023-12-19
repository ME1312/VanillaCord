package vanillacord.data;

import bridge.asm.KnownType;
import bridge.asm.TypeMap;

import java.util.LinkedHashMap;

public class ClassData {
    public final LinkedHashMap<String, FieldData> fields = new LinkedHashMap<>();
    public final LinkedHashMap<String, MethodData> methods = new LinkedHashMap<>();
    public final String signature;
    public final KnownType clazz;
    final TypeMap types;

    ClassData(TypeMap map, KnownType type, String signature) {
        this.types = map;
        this.clazz = type;
        this.signature = signature;
    }
}
