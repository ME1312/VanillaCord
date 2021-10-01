package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.Resources;
import org.json.JSONObject;
import uk.co.thinkofdeath.vanillacord.packager.BundleEditor;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Launch {

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Args: <version> [secret]");
            return;
        }

        System.out.println("VanillaCord 1.8");
        System.out.println("Searching versions");

        String version = args[0].toLowerCase();
        String secret = (args.length == 2 && args[1].length() > 0)?args[1]:"";

        File in = new File("in/" + version + ".jar");
        File out = new File("out/" + version + '-' + ((secret.length() > 0)?"velocity":"bungee"));
        in.getParentFile().mkdirs();

        if (!in.exists()) {
            JSONObject mcprofile = null;
            JSONObject mcversionmanifest = new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream(), Charset.forName("UTF-8")))));
            for (int i = 0; i < mcversionmanifest.getJSONArray("versions").length(); i++) {
                if (mcversionmanifest.getJSONArray("versions").getJSONObject(i).getString("id").equals(version.toString())) {
                    mcprofile = new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URL(mcversionmanifest.getJSONArray("versions").getJSONObject(i).getString("url")).openStream(), Charset.forName("UTF-8")))));
                    break;
                }
            } if (mcprofile == null) throw new IllegalArgumentException("Could not find version metadata for " + version);

            System.out.println("Downloading Minecraft Server " + version);
            try (FileOutputStream fin = new FileOutputStream(in)) {
                Resources.copy(new URL(mcprofile.getJSONObject("downloads").getJSONObject("server").getString("url")), fin);

                if (in.length() != mcprofile.getJSONObject("downloads").getJSONObject("server").getLong("size"))
                    throw new IllegalStateException("Downloaded file does not match the profile's expectations: File size: " + in.length() + "!=" + mcprofile.getJSONObject("downloads").getJSONObject("server").getLong("size"));
                String sha1 = sha1(in);
                if (!mcprofile.getJSONObject("downloads").getJSONObject("server").getString("sha1").equals(sha1))
                    throw new IllegalStateException("Downloaded file does not match the profile's expectations: SHA-1 checksum: " + sha1 + "!=" + mcprofile.getJSONObject("downloads").getJSONObject("server").getString("sha1"));
            } catch (Throwable e) {
                in.delete();
                throw e;
            }
        } else {
            System.out.println("Found Minecraft Server " + version);
        }

        if (!BundleEditor.edit(in, out, version, secret)) {
            VCClassLoader loader = new VCClassLoader(new URL[]{Launch.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
            loader.loadClass("uk.co.thinkofdeath.vanillacord.patcher.Patcher").getDeclaredMethod("patch", File.class, File.class, String.class).invoke(null, in, new File(out.getParentFile(), out.getName() + ".jar"), secret);
        }
    }

    private static String sha1(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
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

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
