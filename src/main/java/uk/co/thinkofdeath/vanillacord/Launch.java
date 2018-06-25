package uk.co.thinkofdeath.vanillacord;

import com.google.common.io.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class Launch {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Args: <version>");
            return;
        }

        System.out.println("VanillaCord branch 1.12");

        String version = args[0];
        String url = String.format("http://s3.amazonaws.com/Minecraft.Download/versions/%1$s/minecraft_server.%1$s.jar", version);

        File in = new File("in/" + version + ".jar");
        in.getParentFile().mkdirs();
        if (!in.exists()) {
            System.out.println("Downloading Minecraft Server " + version);
            try (FileOutputStream fin = new FileOutputStream(in)) {
                Resources.copy(new URL(url), fin);
            }
        }

        URLOverrideClassLoader loader = new URLOverrideClassLoader(new URL[]{Launch.class.getProtectionDomain().getCodeSource().getLocation(), in.toURI().toURL()});
        loader.loadClass("uk.co.thinkofdeath.vanillacord.Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
    }
}
