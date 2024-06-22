package vanillacord;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import vanillacord.data.Digest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @SuppressWarnings("AssignmentUsedAsCondition")
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("VanillaCord requires you to specify at least one minecraft version to patch");
            System.exit(1);
        }
        System.out.println(Patcher.brand);
        try {
            final boolean multiple;
            System.out.print("Searching for the specified server version");
            if (multiple = args.length != 1) System.out.println('s');
            System.out.println();

            Downloader downloads = null;
            FileSystem system;
            Path library = (system = FileSystems.getDefault()).getPath("in");
            for (int i = 0; i != args.length;) {
                String version;
                if (!Files.exists(library.resolve((version = args[i] = args[i++].toLowerCase(Locale.ENGLISH)) + ".jar"))) {
                    if (downloads == null) downloads = new Downloader();
                    if (downloads.find(version) == null) {
                        System.err.print("Cannot find version metadata online for Minecraft ");
                        System.err.println(version);
                        System.exit(1);
                    }
                }
            }

            Path parent = system.getPath("out");
            for (String version : args) {
                Path in = library.resolve(version + ".jar");
                Path out = parent.resolve(version + ".jar");

                if (Files.exists(in)) {
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
                    Files.createDirectories(library);

                    URLConnection connection;
                    int i = 0, length;
                    byte[] digest, data = new byte[length = (connection = new URL(location.getString("url")).openConnection()).getContentLength()];
                    try (InputStream stream = connection.getInputStream()) {
                        for (int b; i < length && (b = stream.read(data, i, length - i)) != -1; i += b);
                    }

                    if (i != length) {
                        throw new IllegalStateException("Downloaded profile is not as expected: File size: " + i + " != " + length);
                    }

                    MessageDigest sha1;
                    (sha1 = MessageDigest.getInstance("SHA-1")).update(data);
                    if (!Digest.equals(digest = sha1.digest(), location.getString("sha1"))) {
                        throw new IllegalStateException("Downloaded profile is not as expected: SHA-1 checksum: " + Digest.toHex(digest) + " != " + location.getString("sha1"));
                    }

                    JSONObject profile;
                    try (
                            OutputStream file = Files.newOutputStream(in);
                            InputStream stream = new URL((profile = (JSONObject) ((JSONObject) JSON.parseObject(data).get("downloads")).get("server")).getString("url")).openStream()
                    ) {
                        data = new byte[8192];
                        while ((i = stream.read(data)) != -1) {
                            file.write(data, 0, i);
                            sha1.update(data, 0, i);
                        }

                        file.close();
                        if (Files.size(in) != profile.getLong("size")) {
                            throw new IllegalStateException("Downloaded jarfile is not as expected: File size: " + Files.size(in) + " != " + profile.getLong("size"));
                        }
                        if (!Digest.equals(digest = sha1.digest(), profile.getString("sha1"))) {
                            throw new IllegalStateException("Downloaded jarfile is not as expected: SHA-1 checksum: " + Digest.toHex(digest) + " != " + profile.getString("sha1"));
                        }
                    } catch (Throwable e) {
                        Files.deleteIfExists(in);
                        throw e;
                    }
                }

                Files.createDirectories(parent);
                Patcher.patch(in, out);
                if (multiple) System.out.println();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
