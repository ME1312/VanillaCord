package vanillacord;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import vanillacord.packaging.Bundle;
import vanillacord.packaging.FatJar;
import vanillacord.packaging.Package;
import vanillacord.patch.*;
import vanillacord.update.Updater;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Patcher {
    public static final String brand;
    private Patcher() {}

    static {
        String build;
        StringBuilder version = new StringBuilder().append("VanillaCord 2.0");
        try {
            if ((build = new Manifest(Patcher.class.getResourceAsStream("/META-INF/MANIFEST.MF")).getMainAttributes().getValue("Implementation-Version")) != null && build.length() != 0) {
                version.append(' ').append('(').append(build).append(')');
            }
        } catch (IOException e) {}
        brand = version.toString();
    }

    @SuppressWarnings("AssignmentUsedAsCondition")
    public static void main(String[] args) {
        System.out.println(Patcher.brand);

        final int length;
        if ((length = args.length) < 2) {
            System.out.println("This entry point requires you to specify at least one input/output file pair");
            System.exit(1);
        }
        if ((length & 1) != 0) {
            System.out.print("Incomplete input/output file pair: ");
            System.out.println(args[length - 1]);
            System.exit(1);
        }
        try {
            FileSystem system = FileSystems.getDefault();
            Path[] paths = new Path[length];
            for (int i = 0; i != length; ++i) {
                if (!Files.exists(paths[i] = system.getPath(args[i]))) {
                    System.err.print("Couldn't find input file: ");
                    System.err.print(args[i]);
                    System.exit(1);
                }

                Path parent;
                if ((parent = (paths[++i] = system.getPath(args[i])).normalize().toAbsolutePath().getParent()) == null || !Files.isDirectory(parent)) {
                    System.err.print("Couldn't find output location: ");
                    System.err.println(args[i]);
                    System.exit(1);
                }
            }

            boolean multiple;
            if (multiple = length != 2) {
                System.out.println();
            }

            for (int i = 0; i != length; ++i) {
                System.out.print("Opening ");
                System.out.println(args[i]);
                Patcher.patch(paths[i], paths[++i]);
                if (multiple) System.out.println();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void patch(Path in, Path out) throws Throwable {
        final Package file;
        try (ZipInputStream stream = new ZipInputStream(Files.newInputStream(in))) {
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
            final byte[] buffer = new byte[8192];
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
                copy(zis, zos, buffer);
            }

            ClassLoader vccl = Patcher.class.getClassLoader();
            System.out.println("Generating helper classes");

            zos.putNextEntry(new ZipEntry("vanillacord/"));
            zos.putNextEntry(new ZipEntry("vanillacord/server/"));
            zos.putNextEntry(new ZipEntry("vanillacord/server/VanillaCord.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/VanillaCord.class"), zos, buffer);

            zos.putNextEntry(new ZipEntry("vanillacord/server/QuietException.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/QuietException.class"), zos, buffer);

            zos.putNextEntry(new ZipEntry("vanillacord/server/ForwardingHelper.class"));
            copy(vccl.getResourceAsStream("vanillacord/server/ForwardingHelper.class"), zos, buffer);

            Updater updater = new Updater(vccl, file);
            updater.update("vanillacord/server/BungeeHelper.class", zos, buffer);

            zos.putNextEntry(new ZipEntry("vanillacord/translation/"));
            vanillacord.translation.PlayerConnection.translate(file, zos);
            vanillacord.translation.HandshakePacket.translate(file, zos);

            if (file.sources.receive != null) { // 1.13+
                updater.update("vanillacord/server/VelocityHelper.class", zos, buffer);
                vanillacord.translation.LoginListener.translate(file, zos);
                vanillacord.translation.LoginExtension.translate(file, zos);
                vanillacord.translation.NamespacedKey.translate(file, zos);
            }

            System.out.println("Closing jarfiles");
        }
    }

    public static void copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        for (int i; (i = in.read(buffer)) != -1;) {
            out.write(buffer, 0, i);
        }
    }

    public static void manifest(InputStream in, OutputStream out) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8));
        for (String line; (line = reader.readLine()) != null;) {
            int delimiter;
            if ((delimiter = line.indexOf(':')) >= 0) {
                switch (line.substring(0, delimiter)) {
                    case "Manifest-Version":
                    case "Main-Class":
                    case "Multi-Release":
                    case "Bundler-Format":
                        out.write(line.getBytes(UTF_8));
                        out.write('\n');
                }
            }
        }
        out.write(new byte[] {'B', 'u', 'i', 'l', 't', '-', 'B', 'y', ':', ' '});
        out.write(Patcher.brand.getBytes(UTF_8));
        out.write('\n');
    }
}
