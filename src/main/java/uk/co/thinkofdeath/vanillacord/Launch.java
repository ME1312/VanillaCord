package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.Resources;
import org.json.JSONObject;
import uk.co.thinkofdeath.vanillacord.library.PatchLoader;
import uk.co.thinkofdeath.vanillacord.packager.BundleEditor;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.readAll;
import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.sha1;

public class Launch {

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Args: <version> [secret]");
            return;
        }

        System.out.println("VanillaCord 1.8");
        System.out.println("Searching versions");

        String version = args[0].toLowerCase(Locale.ENGLISH);
        String secret = (args.length == 2 && args[1].length() > 0)?args[1]:null;

        File in = new File("in/" + version + ".jar");
        File out = new File("out/" + version + '-' + ((secret != null)?"velocity":"bungee"));
        in.getParentFile().mkdirs();

        if (!in.exists()) {
            JSONObject mcprofile = null;
            JSONObject mcversionmanifest = new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream(), Charset.forName("UTF-8")))));
            for (int i = 0; i < mcversionmanifest.getJSONArray("versions").length(); i++) {
                if (mcversionmanifest.getJSONArray("versions").getJSONObject(i).getString("id").equals(version)) {
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
            PatchLoader loader = new PatchLoader(new URL[]{Launch.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
            loader.loadClass("uk.co.thinkofdeath.vanillacord.patcher.Patcher").getDeclaredMethod("patch", File.class, File.class, String.class).invoke(null, in, new File(out.getParentFile(), out.getName() + ".jar"), secret);
        }
    }
}
