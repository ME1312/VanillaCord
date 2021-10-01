package uk.co.thinkofdeath.vanillacord.packager;

import com.google.common.io.ByteStreams;
import uk.co.thinkofdeath.vanillacord.Launch;
import uk.co.thinkofdeath.vanillacord.VCClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BEv1 extends BundleEditor {
    protected BEv1(File in, File out, String version, String secret) {
        super(in, out, version, secret);
    }
    protected File server;

    @Override
    public void extract() throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{in.toURI().toURL()});
        System.setProperty("bundlerRepoDir", out.toString());
        System.setProperty("bundlerMainClass", "uk.co.thinkofdeath.vanillacord.packager.BundleEditor");
        loader.loadClass("net.minecraft.bundler.Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[0]);
    }

    @Override
    public void edit() throws Exception {
        File out = null, dir = new File(this.out, "versions/" + version);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    out = file;
                    break;
                }
            }
        }

        if (out == null) {
            System.out.println("Failed extracting " + version + ".jar");
        } else {
            File in = new File(out.getParentFile(), out.getName() + ".tmp");
            Files.move(out.toPath(), in.toPath(), StandardCopyOption.REPLACE_EXISTING);
            VCClassLoader loader = new VCClassLoader(new URL[]{Launch.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
            loader.loadClass("uk.co.thinkofdeath.vanillacord.patcher.Patcher").getDeclaredMethod("patch", File.class, File.class, String.class).invoke(null, in, out, secret);

            server = out;
            update();
        }
    }

    @Override
    public void update() throws Exception {
        Set<String> mojangCantEvenJar = new HashSet<>();
        File out = new File(this.out, version + ".jar");
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

    @Override
    public boolean update(ZipInputStream zip, ZipOutputStream zop, String path, ZipEntry entry) throws Exception {
        if (path.equals("META-INF/versions.list")) {
            boolean space = false;
            StringBuilder edited = new StringBuilder();
            String[] original = readAll(new InputStreamReader(zip)).split("\r?\n");

            for (String line : original) {
                if (space) edited.append('\n');
                space = true;

                String[] properties = line.split("\t");
                if (properties[2].startsWith(version + '/') && properties[2].endsWith(".jar")) {
                    edited.append(sha256(server));
                    edited.append('\t');
                    edited.append(properties[1]);
                    edited.append('\t');
                    edited.append(properties[2]);
                } else {
                    edited.append(line);
                }
            }

            zop.putNextEntry(new ZipEntry(path));
            zop.write(edited.toString().getBytes(StandardCharsets.UTF_8));
        } else if (path.startsWith("META-INF/versions/" + version) && path.endsWith(".jar")) {
            zop.putNextEntry(new ZipEntry(path));
            try (FileInputStream server = new FileInputStream(this.server)) {
                ByteStreams.copy(server, zop);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void close() throws Exception {
        super.close();
    }
}
