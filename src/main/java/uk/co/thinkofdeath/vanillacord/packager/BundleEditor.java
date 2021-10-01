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
        if (current != null) throw new IllegalStateException();

        JarFile jar = new JarFile(in);
        Attributes ja = jar.getManifest().getMainAttributes();
        jar.close();

        if ("net.minecraft.bundler.Main".equals(ja.getValue("Main-Class"))) {
            (current = new BEv1(in, out, version, secret)).extract();
        } else {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        if (current == null) throw new IllegalAccessException();
        current.edit();
    }

    // Abstract methods
    public abstract void extract() throws Exception;
    public abstract void edit() throws Exception;
    public abstract void update() throws Exception;
    public abstract boolean update(ZipInputStream zip, ZipOutputStream stream, String path, ZipEntry entry) throws Exception;
    protected void close() throws Exception {
        if (current == this) current = null;
    }
}
