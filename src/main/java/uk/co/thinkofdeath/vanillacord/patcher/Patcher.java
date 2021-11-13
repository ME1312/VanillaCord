package uk.co.thinkofdeath.vanillacord.patcher;

import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uk.co.thinkofdeath.vanillacord.generator.BungeeHelper;
import uk.co.thinkofdeath.vanillacord.generator.VelocityHelper;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.co.thinkofdeath.vanillacord.library.VanillaUtil.readAll;

public class Patcher {

    public static void main(String[] args) throws Exception {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Args: <input> <output> [secret]");
            System.exit(1);
        }

        File in = new File(args[0]);
        if (!in.isFile()) {
            System.err.println("Cannot find input file: " + args[0]);
            System.exit(1);
        }

        patch(in, new File(args[1]), (args.length == 3 && args[2].length() > 0)?args[2]:null);
    }

    public static void patch(File in, File out, String secret) throws Exception {
        boolean secure = secret != null;

        out.getParentFile().mkdirs();
        if (out.exists()) out.delete();

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(in));
             ZipOutputStream zop = new ZipOutputStream(new FileOutputStream(out))) {

            LinkedHashMap<String, byte[]> classes = new LinkedHashMap<>();

            if (secure) System.out.println("Requested modern IP forwarding");
            System.out.println("Loading server bytecode");

            String handshakePacket = null;
            String loginListener = null;
            String loginPacket = null;
            String serverQuery = null;
            String clientQuery = null;
            String networkManager = null;
            {
                String handshakeListener = null;
                TypeChecker handshakeType = null;

                // So Mojang managed to include the same file
                // multiple times in the server jar. This
                // (not being valid) causes java to (correctly)
                // throw an exception so we need to track what
                // files have been added to a jar so that we
                // don't add them twice
                Set<String> mojangCantEvenJar = new HashSet<>();

                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (mojangCantEvenJar.add(path)) {
                        if (!entry.getName().endsWith(".class")) {
                            if (path.equals("META-INF/MANIFEST.MF")) {
                                zop.putNextEntry(new ZipEntry(path));
                                manifest(zip, zop);
                            } else if (!(path.startsWith("META-INF/") && path.endsWith(".SF"))) {
                                zop.putNextEntry(entry);
                                ByteStreams.copy(zip, zop);
                            }
                            continue;
                        }

                        String name = entry.getName();
                        byte[] clazz = ByteStreams.toByteArray(zip);
                        ClassReader reader = new ClassReader(clazz);
                        TypeChecker typeChecker = new TypeChecker(secure);
                        reader.accept(typeChecker, 0);
                        classes.put(name, clazz);

                        if (typeChecker.isHandshakeListener()) {
                            System.out.println("Found the handshake listener in " + name);
                            handshakeListener = name;
                            handshakeType = typeChecker;
                        } else if (typeChecker.isLoginListener()) {
                            System.out.println("Found the login listener in " + name);
                            loginListener = name;
                        } else if (typeChecker.isServerQuery()) {
                            System.out.println("Found the extended login request in " + name);
                            serverQuery = name;
                        } else if (typeChecker.isClientQuery()) {
                            System.out.println("Found the extended login response in " + name);
                            clientQuery = name;
                        }
                    }
                }

                System.out.println("Patching classes");

            // Intercept the handshake
                byte[] clazz = classes.get(handshakeListener);
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                HandshakeListener hsl = new HandshakeListener(classWriter, handshakeType, secure);
                reader.accept(hsl, 0);
                clazz = classWriter.toByteArray();
                classes.put(handshakeListener, clazz);

                hsl.validate();
                handshakePacket = hsl.getHandshake();
                networkManager = hsl.getNetworkManager();
            }
            // Increase the hostname field size
            if (!secure) {
                byte[] clazz = classes.get(handshakePacket + ".class");
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(new HandshakePacket(classWriter), 0);
                clazz = classWriter.toByteArray();
                classes.put(handshakePacket + ".class", clazz);
            }
            // Inject the profile injector and force offline mode
            {
                byte[] clazz = classes.get(loginListener);
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                LoginListener ll = new LoginListener(classWriter, networkManager, clientQuery, secret);
                reader.accept(ll, 0);
                clazz = classWriter.toByteArray();
                classes.put(loginListener, clazz);

                loginPacket = ll.getPacket() + ".class";

            // Intercept the login process
                if (secure) {
                    clazz = classes.get(loginPacket);
                    reader = new ClassReader(clazz);
                    classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new LoginPacket(classWriter, ll, loginListener), 0);
                    clazz = classWriter.toByteArray();
                    classes.put(loginPacket, clazz);
                }
            }
            // Change the server brand
            {
                byte[] clazz = classes.get("net/minecraft/server/MinecraftServer.class");
                ClassReader classReader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classReader.accept(new MCBrand(classWriter), 0);
                clazz = classWriter.toByteArray();
                classes.put("net/minecraft/server/MinecraftServer.class", clazz);
            }

            System.out.println("Generating helper classes");

            {
                InputStream clazz = Patcher.class.getResourceAsStream("/uk/co/thinkofdeath/vanillacord/helper/BungeeHelper.class");
                LinkedHashMap<String, byte[]> queue = new LinkedHashMap<>();
                ClassReader classReader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classReader.accept(new BungeeHelper(queue, classWriter, networkManager, handshakePacket), 0);
                classes.put("uk/co/thinkofdeath/vanillacord/helper/BungeeHelper.class", classWriter.toByteArray());
                classes.putAll(queue);
                clazz.close();
            }

            if (secure) {
                InputStream clazz = Patcher.class.getResourceAsStream("/uk/co/thinkofdeath/vanillacord/helper/VelocityHelper.class");
                LinkedHashMap<String, byte[]> queue = new LinkedHashMap<>();
                ClassReader classReader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classReader.accept(new VelocityHelper(queue, classWriter, networkManager, loginListener, loginPacket, serverQuery, clientQuery), 0);
                classes.put("uk/co/thinkofdeath/vanillacord/helper/VelocityHelper.class", classWriter.toByteArray());
                classes.putAll(queue);
                clazz.close();

            }

            System.out.println("Exporting server jar");
            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                zop.putNextEntry(new ZipEntry(e.getKey()));
                zop.write(e.getValue());
            }

            InputStream clazz = Patcher.class.getResourceAsStream("/uk/co/thinkofdeath/vanillacord/helper/QuietException.class");
            zop.putNextEntry(new ZipEntry("uk/co/thinkofdeath/vanillacord/helper/QuietException.class"));
            ByteStreams.copy(clazz, zop);
            clazz.close();
        }
    }

    public static void manifest(InputStream in, OutputStream out) throws IOException {
        StringBuilder edited = new StringBuilder();
        String[] original = readAll(new InputStreamReader(in)).replace("\r\n", "\n").replace("\n ", "").split("\n");
        Pattern pattern = Pattern.compile("^(?:Manifest-Version|Main-Class):.*$");
        for (String property : original) {
            Matcher m = pattern.matcher(property);
            if (m.find()) {
                edited.append(m.group());
                edited.append('\n');
            }
        }
        edited.append("Built-By: VanillaCord");
        edited.append('\n');
        out.write(edited.toString().getBytes(UTF_8));
    }
}
