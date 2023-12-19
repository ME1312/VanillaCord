package vanillacord;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import vanillacord.data.Digest;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public final class Downloader {
    private final Iterator<JSONObject> data;
    private final HashMap<String, JSONObject> versions;

    @SuppressWarnings("unchecked")
    private Downloader() throws IOException {
        data = ((Iterable<JSONObject>) JSON.parseObject(new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")).get("versions")).iterator();
        versions = new HashMap<>();
    }

    public JSONObject find(String version) {
        JSONObject profile;
        if ((profile = versions.get(version)) != null) {
            return profile;
        }
        String id;
        for (Iterator<JSONObject> it = this.data; it.hasNext();) {
            versions.putIfAbsent(id = (profile = it.next()).getString("id"), profile);
            if (version.equals(id)) {
                return profile;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("VanillaCord requires you to specify at least one minecraft version to patch");
            System.exit(1);
        }
        System.out.println(Patcher.brand);
        try {
            System.out.print("Searching for the specified server version");
            if (args.length != 1) System.out.println('s');
            System.out.println();

            Downloader downloads = null;
            File library = new File("in");
            for (int i = 0; i != args.length;) {
                String version = args[i] = args[i++].toLowerCase(Locale.ENGLISH);
                File in = new File(library, version + ".jar");
                if (!in.exists()) {
                    if (downloads == null) downloads = new Downloader();
                    if (downloads.find(version) == null) {
                        System.err.print("Cannot find version metadata online for Minecraft ");
                        System.err.println(version);
                        System.exit(1);
                    }
                }
            }

            for (String version : args) {
                File in = new File(library, version + ".jar");
                File out = new File("out", version + ".jar");

                if (in.exists()) {
                    System.out.print("Using local Minecraft server version ");
                    System.out.println(version);
                } else {
                    final JSONObject location;
                    if (downloads == null) downloads = new Downloader();
                    if ((location = downloads.find(version)) == null) {
                        throw new FileNotFoundException(in.toString());
                    }
                    System.out.print("Downloading Minecraft server version ");
                    System.out.println(version);
                    library.mkdirs();

                    URLConnection connection = new URL(location.getString("url")).openConnection();
                    int length = connection.getContentLength();
                    byte[] digest, data = new byte[length];
                    try (InputStream stream = connection.getInputStream()) {
                        for (int b, i = 0; i < length && (b = stream.read(data, i, length - i)) != -1; i += b);
                    }

                    MessageDigest sha1;
                    (sha1 = MessageDigest.getInstance("SHA-1")).update(data);
                    if (!Digest.isEqual(digest = sha1.digest(), location.getString("sha1"))) {
                        throw new IllegalStateException("Downloaded profile is not as expected: SHA-1 checksum: " + Digest.toHex(digest) + " != " + location.getString("sha1"));
                    }

                    JSONObject profile = (JSONObject) ((JSONObject) JSON.parseObject(data).get("downloads")).get("server");
                    try (FileOutputStream file = new FileOutputStream(in, false); InputStream stream = new URL(profile.getString("url")).openStream()) {
                        data = new byte[8192];
                        for (int i; (i = stream.read(data)) != -1;) {
                            file.write(data, 0, i);
                            sha1.update(data, 0, i);
                        }

                        file.close();
                        if (in.length() != profile.getLong("size")) {
                            throw new IllegalStateException("Downloaded jarfile is not as expected: File size: " + in.length() + " != " + profile.getLong("size"));
                        } else if (!Digest.isEqual(digest = sha1.digest(), profile.getString("sha1"))) {
                            throw new IllegalStateException("Downloaded jarfile is not as expected: SHA-1 checksum: " + Digest.toHex(digest) + " != " + profile.getString("sha1"));
                        }
                    } catch (Throwable e) {
                        in.delete();
                        throw e;
                    }
                }

                out.getParentFile().mkdir();
                Patcher.patch(in, out);
                System.out.println();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
