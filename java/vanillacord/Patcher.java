package vanillacord;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import vanillacord.packaging.Bundle;
import vanillacord.packaging.FatJar;
import vanillacord.packaging.Package;
import vanillacord.patch.*;
import vanillacord.update.Updater;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Patcher {
    public static final String brand;

    static {
        String build;
        StringBuilder version = new StringBuilder("VanillaCord 2.0");
        try {
            if ((build = new Manifest(Patcher.class.getResourceAsStream("/META-INF/MANIFEST.MF")).getMainAttributes().getValue("Implementation-Version")) != null && build.length() != 0) {
                version.append(' ').append('(').append(build).append(')');
            }
        } catch (IOException e) {}
        brand = version.toString();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Required arguments: <input> <output>");
            System.exit(1);
        }
        System.out.println(Patcher.brand);
        try {
            File in = new File(args[0]);
            if (!in.isFile()) {
                System.err.println("Couldn't find input file: " + args[0]);
                System.exit(1);
            }

            File out = new File(args[1]);
            if (!out.getParentFile().exists()) {
                System.err.println("Couldn't find output location: " + args[1]);
                System.exit(1);
            }

            Patcher.patch(in, out);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void patch(File in, File out) throws Throwable {
        final Package file;
        try (ZipInputStream stream = new ZipInputStream(new FileInputStream(in))) {
            for (ZipEntry entry;;) {
                if ((entry = stream.getNextEntry()) == null) {
                    throw new FileNotFoundException("MANIFEST.MF");
                }

                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    String format = new Manifest(stream).getMainAttributes().getValue("Bundler-Format");
                    stream.close();

                    if (format != null && format.length() != 0) {
                        file = new Bundle(format);
                    } else { // 1.18 > version
                        file = new FatJar();
                    }
                    break;
                }
            }
        }
        try (
                ZipInputStream zis = file.read(in);
                ZipOutputStream zos = file.write(out);
        ) {
            HashMap<String, Function<ClassVisitor, ClassVisitor>> patches = new HashMap<>();
            System.out.println("Patching classes");

            patches.put(file.sources.startup.owner.clazz.type.getInternalName(), new DedicatedServer(file));
            patches.put(file.sources.handshake.owner.clazz.type.getInternalName(), new HandshakeListener(file));
            patches.put(file.sources.handshake.arguments[0].type.getInternalName(), new HandshakePacket());
            patches.put(file.sources.login.owner.clazz.type.getInternalName(), new LoginListener(file));
            patches.put(file.sources.login.arguments[0].type.getInternalName(), new LoginPacket(file));
            if (file.sources.receive != null && file.sources.receive.owner.clazz.extended(file.types.loadClass("java/lang/Record"))) { // 1.20+
                patches.put(file.sources.receive.owner.clazz.type.getInternalName(), new LoginExtension(file));
            }

            String name;
            HashSet<String> unique = new HashSet<>();
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                if (!unique.add(name = entry.getName())) {
                    continue;
                } else if (name.endsWith(".class")) {
                    Function<ClassVisitor, ClassVisitor> patch = patches.get(name.substring(0, name.length() - 6));
                    if (patch != null) {
                        zos.putNextEntry(new ZipEntry(name));
                        ClassReader reader = new ClassReader(zis);
                        ClassWriter writer = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                        reader.accept(patch.apply(writer), 0);
                        zos.write(writer.toByteArray());
                        continue;
                    }
                } else if (name.equals("META-INF/MANIFEST.MF")) {
                    zos.putNextEntry(new ZipEntry(name));
                    manifest(zis, zos);
                    continue;
                } else if (name.startsWith("META-INF/") && name.endsWith(".SF")) {
                    continue;
                }
                zos.putNextEntry(entry);
                copy(zis, zos);
            }

            ClassLoader vccl = Patcher.class.getClassLoader();
            System.out.println("Generating helper classes");

            zos.putNextEntry(new ZipEntry("vanillacord/"));
            zos.putNextEntry(new ZipEntry("vanillacord/server/"));
            zos.putNextEntry(new ZipEntry("vanillacord/server/VanillaCord.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/VanillaCord.class"), zos);

            zos.putNextEntry(new ZipEntry("vanillacord/server/QuietException.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/QuietException.class"), zos);

            zos.putNextEntry(new ZipEntry("vanillacord/server/ForwardingHelper.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/ForwardingHelper.class"), zos);

            Updater updater = new Updater(vccl, file);
            updater.update("vanillacord/server/BungeeHelper.class", zos);

            zos.putNextEntry(new ZipEntry("vanillacord/translation/"));
            vanillacord.translation.PlayerConnection.translate(file, zos);
            vanillacord.translation.HandshakePacket.translate(file, zos);

            if (file.sources.receive != null) { // 1.13+
                updater.update("vanillacord/server/VelocityHelper.class", zos);
                vanillacord.translation.LoginListener.translate(file, zos);
                vanillacord.translation.LoginExtension.translate(file, zos);
                vanillacord.translation.NamespacedKey.translate(file, zos);
            }

            System.out.println("Closing jarfiles");
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] data = new byte[8192];
        for (int i; (i = in.read(data)) != -1;) {
            out.write(data, 0, i);
        }
    }

    private static final Pattern manifest = Pattern.compile("^(?:Manifest-Version|Main-Class|Multi-Release|Bundler-Format):.*$");
    public static void manifest(InputStream in, OutputStream out) throws IOException {
        StringBuilder edited = new StringBuilder();
        BufferedReader original = new BufferedReader(new InputStreamReader(in, UTF_8));
        for (String property; (property = original.readLine()) != null;) {
            Matcher m = Patcher.manifest.matcher(property);
            if (m.find()) {
                edited.append(m.group());
                edited.append('\n');
            }
        }
        edited.append("Built-By: ");
        edited.append(Patcher.brand);
        edited.append('\n');
        out.write(edited.toString().getBytes(UTF_8));
    }
}
