package uk.co.thinkofdeath.vanillacord.packager;

import java.io.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        if (current != null) return true;

        detect(in, out, version, secret);
        if (current != null) {
            current.extract();
            return true;
        } else {
            return false;
        }
    }

    private static void detect(File in, File out, String version, String secret) throws Exception{
        if (current != null) throw new IllegalStateException();

        JarFile jar = new JarFile(in);
        Attributes ja = jar.getManifest().getMainAttributes();
        jar.close();

        if ("net.minecraft.bundler.Main".equals(ja.getValue("Main-Class"))) {
            current = new BEv1(in, out, version, secret);
        }
    }

    public static void main(String[] args) throws Exception {
        if (current == null) {
            if (args.length != 3 && args.length != 4) throw new IllegalArgumentException();
            detect(new File(args[0]), new File(args[1]), args[2], (args.length == 4 && args[3].length() > 0)?args[3]:null);
        }
        current.edit();
    }

    // Abstract methods
    protected abstract void extract() throws Exception;
    protected abstract void edit() throws Exception;
    protected abstract void update() throws Exception;
    protected abstract boolean update(ZipInputStream zip, ZipOutputStream stream, String path, ZipEntry entry) throws Exception;
    protected void close() throws Exception {
        if (current == this) current = null;
    }
}
