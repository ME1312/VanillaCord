package vanillacord.packaging;

import org.objectweb.asm.ClassReader;
import vanillacord.data.SourceScanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FatJar extends Package {

    @Override
    public ZipInputStream read(Path path) throws Throwable {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(path))) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                if (entry.getName().endsWith(".class")) {
                    new ClassReader(zis).accept(new SourceScanner(this), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                }
            }
        }
        return new ZipInputStream(Files.newInputStream(path));
    }

    @Override
    public ZipOutputStream write(Path path) throws Throwable {
        return new ZipOutputStream(Files.newOutputStream(path));
    }
}
