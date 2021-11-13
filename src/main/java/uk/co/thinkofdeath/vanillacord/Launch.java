package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.Resources;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.thinkofdeath.vanillacord.library.PatchLoader;
import uk.co.thinkofdeath.vanillacord.packager.BundleEditor;

import java.io.*;
import java.net.URL;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.readAll;
import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.sha1;

public class Launch {

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Args: <version> [secret]");
            System.exit(1);
        }

        System.out.println("VanillaCord 1.8");
        System.out.println("Searching versions");

        String version = args[0].toLowerCase(Locale.ENGLISH);
        String secret = (args.length == 2 && args[1].length() > 0)?args[1]:null;

        File in = new File("in/" + version + ".jar");
        File out = new File("out/" + version + '-' + ((secret != null)?"velocity":"bungee"));
        in.getParentFile().mkdirs();

        try {
            if (!in.exists()) {
                JSONObject profile = null;
                JSONArray profiles = new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream(), UTF_8)))).getJSONArray("versions");
                for (int i = 0; i < profiles.length(); ++i) {
                    if (profiles.getJSONObject(i).getString("id").equals(version)) {
                        profile = new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URL(profiles.getJSONObject(i).getString("url")).openStream(), UTF_8))));
                        break;
                    }
                } if (profile == null) {
                    System.err.println("Cannot find version metadata for " + version);
                    System.exit(1);
                }

                System.out.println("Downloading Minecraft Server " + version);
                try (FileOutputStream fin = new FileOutputStream(in)) {
                    JSONObject download = profile.getJSONObject("downloads").getJSONObject("server");
                    Resources.copy(new URL(download.getString("url")), fin);

                    if (in.length() != download.getLong("size"))
                        throw new IllegalStateException("Downloaded file is not as expected: File size: " + in.length() + "!=" + download.getLong("size"));

                    String sha1 = sha1(in);
                    if (!download.getString("sha1").equals(sha1))
                        throw new IllegalStateException("Downloaded file is not as expected: SHA-1 checksum: " + sha1 + "!=" + download.getString("sha1"));

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
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
