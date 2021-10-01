package uk.co.thinkofdeath.vanillacord.packager;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // Utility methods
    protected static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha256.update(buffer, 0, len);
                len = input.read(buffer);
            }

            byte[] digest = sha256.digest();
            StringBuilder output = new StringBuilder();
            for (int i=0; i < digest.length; i++) {
                output.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }
            return output.toString();
        }
    }

    protected static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
