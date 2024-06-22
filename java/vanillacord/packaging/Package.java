package vanillacord.packaging;

import bridge.asm.TypeMap;
import vanillacord.data.Sources;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Function;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class Package {
    public final HashMap<String, Function<byte[], byte[]>> patches = new HashMap<>();
    public final Sources sources = new Sources();
    public final TypeMap types = new TypeMap();
    Package() {}

    public abstract ZipInputStream read(Path path) throws Throwable;
    public abstract ZipOutputStream write(Path path) throws Throwable;
}
