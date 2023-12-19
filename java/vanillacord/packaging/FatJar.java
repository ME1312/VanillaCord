package vanillacord.packaging;

import org.objectweb.asm.ClassReader;
import vanillacord.data.HierarchyVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FatJar extends Package {

    @Override
    public ZipInputStream read(File file) throws Throwable {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                if (entry.getName().endsWith(".class")) {
                    new ClassReader(zis).accept(new HierarchyVisitor(this), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                }
            }
        }
        return new ZipInputStream(new FileInputStream(file));
    }

    @Override
    public ZipOutputStream write(File file) throws Throwable {
        return new ZipOutputStream(new FileOutputStream(file));
    }
}
