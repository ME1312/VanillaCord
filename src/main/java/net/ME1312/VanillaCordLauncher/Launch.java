package net.ME1312.VanillaCordLauncher;

import net.ME1312.VanillaCordLauncher.Library.URLOverrideClassLoader;
import net.ME1312.VanillaCordLauncher.Library.Version.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Launch {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Args: <version>");
            return;
        }

        Version launcherversion = new Version(1, 4);
        Version mcversion = new Version(args[0]);
        Version patchversion;
        if (mcversion.compareTo(new Version("1.12")) >= 0) {
            patchversion = new Version("1.12");
        } else {
            patchversion = new Version("1.7.10");
        }

        System.out.println("VanillaCord launcher v" + launcherversion);
        String mcurl = String.format("http://s3.amazonaws.com/Minecraft.Download/versions/%1$s/minecraft_server.%1$s.jar", mcversion);
        String patchurl = String.format("https://raw.githubusercontent.com/ME1312/VanillaCord/%1$s/artifacts/VanillaCord.jar", patchversion);

        File mcfile = new File("in/" + mcversion + ".jar");
        mcfile.getParentFile().mkdirs();
        if (!mcfile.exists()) {
            System.out.println("Downloading Minecraft Server " + mcversion);
            try (FileOutputStream fin = new FileOutputStream(mcfile); ReadableByteChannel rbc = Channels.newChannel(new URL(mcurl).openStream())) {
                fin.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Throwable e) {
                mcfile.delete();
                throw e;
            }
        }

        File patchfile = new File("in/" + patchversion + "-patch.jar");
        patchfile.getParentFile().mkdirs();
        if (!patchfile.exists()) {
            System.out.println("Downloading patches from branch " + patchversion);
            try (FileOutputStream fin = new FileOutputStream(patchfile); ReadableByteChannel rbc = Channels.newChannel(new URL(patchurl).openStream())) {
                fin.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Throwable e) {
                patchfile.delete();
                throw e;
            }
        } else System.out.println("Reusing patches from branch " + patchversion);

        URLOverrideClassLoader loader = new URLOverrideClassLoader(new URL[]{patchfile.toURI().toURL(), mcfile.toURI().toURL()});
        loader.loadClass("uk.co.thinkofdeath.vanillacord.Main").getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
    }
}
