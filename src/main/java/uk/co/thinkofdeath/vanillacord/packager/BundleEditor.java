package uk.co.thinkofdeath.vanillacord.packager;

import com.google.common.io.ByteStreams;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.deleteDirectory;

public abstract class BundleEditor {
    private static BundleEditor current;
    protected final File in, out;
    protected final String version, secret;

    protected BundleEditor(File in, File out, String version, String secret) {
        this.in = in;
        this.out = out;
        this.version = version;
        this.secret = secret;
    }

    public static boolean edit(File in, File out, String version, String secret) throws Exception {
        if (current != null) throw new IllegalStateException("Edit already in progress!");

        detect(in, out, version, secret);
        if (current != null) {
            current.extract();
            return true;
        } else {
            return false;
        }
    }

    private static void detect(File in, File out, String version, String secret) throws Exception {
        JarFile jar = new JarFile(in);
        Attributes ja = jar.getManifest().getMainAttributes();
        jar.close();

        if ("net.minecraft.bundler.Main".equals(ja.getValue("Main-Class"))) {
            current = new BEv1(in, out, version, secret);
        }
    }

    public static void main(String[] args) {
        try {
            if (current == null) {
                if (args.length != 3 && args.length != 4) throw new IllegalArgumentException();
                detect(new File(args[0]), new File(args[1]), args[2], (args.length == 4 && args[3].length() > 0)?args[3]:null);
            }
            current.edit();
        } catch (Throwable e) {
            e.printStackTrace();
            if (current != null) try {
                current.close();
            } catch (Throwable e2) {}
            System.exit(1);
        }
    }

    protected abstract void extract() throws Exception;
    protected abstract void edit() throws Exception;
    protected abstract boolean update(ZipInputStream zip, ZipOutputStream stream, String path, ZipEntry entry) throws Exception;

    protected void update() throws Exception {
        Set<String> mojangCantEvenJar = new HashSet<>();
        File out = new File(this.out.getParentFile(), this.out.getName() + ".jar");
        if (out.exists()) out.delete();
        try (
                ZipInputStream zip = new ZipInputStream(new FileInputStream(in));
                ZipOutputStream zop = new ZipOutputStream(new FileOutputStream(out, false));
        ) {
            System.out.println("Repackaging server bundle");
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (mojangCantEvenJar.add(entry.getName()) && !update(zip, zop, entry.getName(), entry)) {
                    zop.putNextEntry(new ZipEntry(entry.getName()));
                    ByteStreams.copy(zip, zop);
                }
            }
        }

        close();
    }

    protected void close() throws Exception {
        if (out.isDirectory() && !Boolean.getBoolean("vc.debug")) deleteDirectory(out);
        if (current == this) current = null;
    }
}
