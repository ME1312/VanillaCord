package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.Resources;
import uk.co.thinkofdeath.vanillacord.util.version.Version;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class Launch {

    public static void main(String[] args) throws Exception {
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

        if (new Version(version).compareTo(new Version("1.12")) >= 0) {
            URLOverrideClassLoader loader = new URLOverrideClassLoader(new URL[]{Launch.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
            loader.loadClass("uk.co.thinkofdeath.vanillacord.Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        } else {
            File tmp = File.createTempFile("VanillaCord-", ".jar");
            tmp.deleteOnExit();
            tmp.delete();
            extract("/uk/co/thinkofdeath/vanillacord/OldVersion.jar", tmp.toString());
            ArrayList<String> arguments = new ArrayList<String>();
            arguments.add(String.valueOf(System.getProperty("java.home")) + File.separator + "bin" + File.separator + "java");
            arguments.add("-jar");
            arguments.add(tmp.getPath());
            arguments.add(version);
            ProcessBuilder builder = new ProcessBuilder(arguments);
            builder.directory(new File(System.getProperty("user.dir")));
            final Process process = builder.start();
            new Thread(() -> {
                try {
                    String line;
                    BufferedReader obr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while (process.isAlive() && (line = obr.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            String line;
            BufferedReader obr = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (process.isAlive() && (line = obr.readLine()) != null) {
                System.out.println(line);
            }
            Thread.sleep(250);
            System.exit(process.exitValue());
        }
    }

    public static void extract(String resource, String destination) {
        InputStream resStreamIn = Launch.class.getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
