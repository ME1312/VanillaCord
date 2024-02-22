package vanillacord.data;

import bridge.asm.KnownType;
import bridge.asm.TypeMap;

import java.util.Iterator;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("signature {");
        if (signature != null) {
            builder.append("\n    ").append(signature).append('\n');
        }
        builder.append("}\nfields {");
        if (fields.size() != 0) {
            for (Iterator<String> it = fields.keySet().iterator(); it.hasNext(); ) {
                builder.append("\n    ").append(it.next());
            }
            builder.append('\n');
        }
        builder.append("}\nmethods {");
        if (methods.size() != 0) {
            for (Iterator<String> it = methods.keySet().iterator(); it.hasNext(); ) {
                builder.append("\n    ").append(it.next());
            }
            builder.append('\n');
        }
        return builder.append('}').toString();
    }
}
