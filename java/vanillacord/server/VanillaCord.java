package vanillacord.server;

import bridge.Unchecked;
import com.mojang.authlib.GameProfile;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("SpellCheckingInspection")
public class VanillaCord {
    public static final ForwardingHelper helper;

    static {
        final File file = new File("vanillacord.txt");
        double version = 0;
        String forwarding = "bungeecord";
        LinkedList<String> seecrets = new LinkedList<>();

        try {
            if (file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8))) {
                    for (String line; (line = reader.readLine()) != null;) {
                        int start, end;
                        if (line.lastIndexOf('#', 0) != 0 && (start = line.indexOf('=')) >= 0) {
                            for (end = start + 1; start != 0 && line.codePointBefore(start) == ' '; --start) {
                                if (end != line.length() && line.codePointAt(end) == ' ') ++end;
                            }
                            switch (line.substring(0, start).toLowerCase(Locale.ROOT)) {
                                case "version":
                                    version = Double.parseDouble(line.substring(end));
                                    break;
                                case "forwarding":
                                    forwarding = line.substring(end);
                                    break;
                                case "seecret":
                                    if (line.length() > end) seecrets.add(line.substring(end));
                                    break;
                            }
                        }
                    }
                }
            }

            if (version < 2) {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), UTF_8)))) {
                    writer.println("# Welcome to the VanillaCord configuration.");
                    writer.println("version = 2.0");
                    writer.println();
                    writer.println("# Here, you can set which forwarding standard to enable.");
                    writer.println("# Valid options: bungeecord, bungeeguard, velocity");
                    writer.println("forwarding = " + forwarding);
                    writer.println();
                    writer.println("# Some forwarding standards require a seecret key to function.");
                    writer.println("# Specify that here. Repeat this line to specify multiple.");
                    if (seecrets.size() == 0) {
                        writer.println("seecret = ");
                    } else {
                        for (String s : seecrets) {
                            writer.print("seecret = ");
                            writer.println(s);
                        }
                    }
                }
            }
        } catch (NumberFormatException | IOException e) {
            System.out.println("Failed accessing the VanillaCord configuration");
            e.printStackTrace(System.out);
        }

        try {
            switch (forwarding.toLowerCase(Locale.ROOT)) {
                case "bungeecord":
                    helper = new BungeeHelper();
                    break;
                case "bungeeguard":
                    helper = new BungeeHelper(seecrets);
                    break;
                case "velocity":
                    helper = (ForwardingHelper) MethodHandles.lookup().findConstructor(VelocityHelper.class, MethodType.methodType(void.class, LinkedList.class)).invoke(seecrets);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown forwarding provider: " + forwarding);
            }
        } catch (NoClassDefFoundError e) {
            throw new UnsupportedOperationException(forwarding.substring(0, 1).toUpperCase(Locale.ROOT) + forwarding.substring(1).toLowerCase(Locale.ROOT) + " forwarding is not available in this version");
        } catch (Throwable e) {
            throw new Unchecked(e);
        }
    }

    private VanillaCord() {
        // this constructor is never called
    }
}
