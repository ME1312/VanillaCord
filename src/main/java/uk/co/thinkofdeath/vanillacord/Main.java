package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Args: <version>");
            return;
        }

        String version = args[0];

        String url = String.format("http://s3.amazonaws.com/Minecraft.Download/versions/%1$s/minecraft_server.%1$s.jar", version);

        File in = new File("in/" + version + ".jar");
        in.getParentFile().mkdirs();
        if (!in.exists()) {
            System.out.println("Downloading");
            try (FileOutputStream fin = new FileOutputStream(in)) {
                Resources.copy(new URL(url), fin);
            }
        }
        addURL(in.toURI().toURL());

        File out = new File("out/" + version + "-bungee.jar");
        out.getParentFile().mkdirs();
        if (out.exists()) out.delete();

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(in));
             ZipOutputStream zop = new ZipOutputStream(new FileOutputStream(out));
             InputStream helper = Main.class.getResourceAsStream("/uk/co/thinkofdeath/vanillacord/util/BungeeHelper.class")) {

            HashMap<String, byte[]> classes = new HashMap<>();

            System.out.println("Loading");

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".class")) {
                    zop.putNextEntry(entry);
                    ByteStreams.copy(zip, zop);
                    continue;
                }
                byte[] clazz = ByteStreams.toByteArray(zip);
                classes.put(entry.getName(), clazz);
            }

            String handshakePacket = null;
            String loginListener = null;
            String networkManager = null;

            for (Map.Entry<String, byte[]> e : new HashMap<>(classes).entrySet()) {
                byte[] clazz = e.getValue();
                ClassReader reader = new ClassReader(clazz);
                TypeChecker typeChecker = new TypeChecker();
                reader.accept(typeChecker, 0);

                if (typeChecker.isHandshakeListener()) {
                    System.out.println("Found the handshake listener in " + e.getKey());

                    reader = new ClassReader(clazz);
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    HandshakeListener hsl = new HandshakeListener(classWriter, typeChecker);
                    reader.accept(hsl, ClassReader.EXPAND_FRAMES);
                    clazz = classWriter.toByteArray();

                    handshakePacket = hsl.getHandshake();
                    networkManager = hsl.getNetworkManager();
                } else if (typeChecker.isLoginListener()) {
                    System.out.println("Found the login listener in " + e.getKey());
                    loginListener = e.getKey();
                }
                classes.put(e.getKey(), clazz);
            }
            // Increase the hostname field size
            {
                byte[] clazz = classes.get(handshakePacket + ".class");
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(new HandshakePacket(classWriter), ClassReader.EXPAND_FRAMES);
                clazz = classWriter.toByteArray();
                classes.put(handshakePacket + ".class", clazz);
            }
            // Inject the profile injector
            {
                byte[] clazz = classes.get(loginListener);
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(new LoginListener(classWriter, networkManager), ClassReader.EXPAND_FRAMES);
                clazz = classWriter.toByteArray();
                classes.put(loginListener, clazz);
            }

            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                zop.putNextEntry(new ZipEntry(e.getKey()));
                zop.write(e.getValue());
            }

            System.out.println("Adding helper");
            zop.putNextEntry(new ZipEntry("uk/co/thinkofdeath/vanillacord/util/BungeeHelper.class"));
            ByteStreams.copy(helper, zop);
        }
    }


    public static void addURL(URL u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
}
