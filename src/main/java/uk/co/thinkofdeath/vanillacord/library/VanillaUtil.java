package uk.co.thinkofdeath.vanillacord.library;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VanillaUtil {
    private VanillaUtil() {}

    public static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        return digest(file, "SHA-256");
    }

    public static String sha1(File file) throws IOException, NoSuchAlgorithmException {
        return digest(file, "SHA-1");
    }

    private static String digest(File file, String name) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance(name);
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            byte[] digest = sha1.digest();
            StringBuilder output = new StringBuilder();
            for (int i=0; i < digest.length; i++) {
                output.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }
            return output.toString();
        }
    }

    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
