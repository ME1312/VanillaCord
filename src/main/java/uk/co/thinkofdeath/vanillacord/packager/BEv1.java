package uk.co.thinkofdeath.vanillacord.packager;

import com.google.common.io.ByteStreams;
import uk.co.thinkofdeath.vanillacord.library.PatchLoader;
import uk.co.thinkofdeath.vanillacord.library.QuietStream;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.*;

public class BEv1 extends BundleEditor {
    protected String serverFolder = version;
    protected File serverFile;

    protected BEv1(File in, File out, String version, String secret) {
        super(in, out, version, secret);
    }

    @Override
    public void extract() throws Exception {
        System.out.println("Running the self-extracting server bundle");

        if (runProcess(new ProcessBuilder(
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                "-DbundlerRepoDir=" + out,
                "-DbundlerMainClass=uk.co.thinkofdeath.vanillacord.packager.BundleEditor",
                "-Dvc.launch=net.minecraft.bundler.Main",
                "-Dvc.debug=" + Boolean.getBoolean("vc.debug"),
                "-cp", new File(BundleEditor.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                getClass().getCanonicalName(), in.toString(), out.toString(), version, (secret != null)?secret:""
        )) == 0) {
            detect();
            update();
        }
    }
    public static void main(String[] args) throws Exception {
        String main;
        if ((main = System.getProperty("vc.launch", "")).length() == 0) throw new IllegalAccessException();
        if (args.length != 3 && args.length != 4) throw new IllegalArgumentException();

        PrintStream out = System.out;
        if (!Boolean.getBoolean("vc.debug")) System.setOut(new QuietStream());

        URLClassLoader loader = new URLClassLoader(new URL[]{new File(args[0]).toURI().toURL()});
        loader.loadClass(main).getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        System.setOut(out);
    }

    protected void detect() {
        File dir = new File(out, "versions/" + version);
        boolean found = dir.isDirectory();

        if (!found && (dir = dir.getParentFile()).isDirectory()) {
            for (File folder : dir.listFiles()) {
                if (folder.isDirectory()) {
                    found = true;
                    dir = folder;
                    serverFolder = folder.getName();
                    break;
                }
            }
        }

        if (found) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    serverFile = file;
                    break;
                }
            }
        }
    }

    @Override
    protected void edit() throws Exception {
        detect();

        if (serverFile != null) try {
            File in = new File(serverFile.getParentFile(), serverFile.getName() + ".tmp");
            Files.move(serverFile.toPath(), in.toPath(), StandardCopyOption.REPLACE_EXISTING);
            PatchLoader loader = new PatchLoader(new URL[]{BundleEditor.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
            loader.loadClass("uk.co.thinkofdeath.vanillacord.patcher.Patcher").getDeclaredMethod("patch", File.class, File.class, String.class).invoke(null, in, serverFile, secret);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        } else {
            System.out.println("Cannot locate server file, giving up");
            System.exit(1);
        }
    }

    @Override
    protected boolean update(ZipInputStream zip, ZipOutputStream zop, String path, ZipEntry entry) throws Exception {
        if (path.equals("META-INF/versions.list")) {
            boolean space = false;
            StringBuilder edited = new StringBuilder();
            String[] original = readAll(new InputStreamReader(zip)).split("\r?\n");

            for (String line : original) {
                if (space) edited.append('\n');
                space = true;

                String[] properties = line.split("\t", 3);
                if (properties[1].equals(serverFolder)) {
                    edited.append(sha256(serverFile));
                    edited.append('\t');
                    edited.append(properties[1]);
                    edited.append('\t');
                    edited.append(properties[2]);
                } else {
                    edited.append(line);
                }
            }

            zop.putNextEntry(new ZipEntry(path));
            zop.write(edited.toString().getBytes(UTF_8));
        } else if (path.startsWith("META-INF/versions/" + serverFolder + '/') && path.endsWith(".jar")) {
            zop.putNextEntry(new ZipEntry(path));
            try (FileInputStream server = new FileInputStream(serverFile)) {
                ByteStreams.copy(server, zop);
            }
        } else {
            return false;
        }
        return true;
    }
}
