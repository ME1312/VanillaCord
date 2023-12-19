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

public class VanillaCord {
    public static final ForwardingHelper helper;

    static {
        final File file = new File("vanillacord.txt");
        double version = 0;
        String forwarding = "bungeecord";
        LinkedList<String> seecret = new LinkedList<>();

        try {
            if (file.isFile()) {
                final Pattern format = Pattern.compile("^\\s*([^#].*?)(\\s*)=");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    for (String value; (value = reader.readLine()) != null;) {
                        final Matcher header = format.matcher(value);
                        if (header.find()) {
                            int i = header.end();
                            whitespace: for (int c, maximum = Math.min(i + header.group(2).length(), value.length()); i < maximum;) {
                                switch (c = value.codePointAt(i)) {
                                    case ' ':
                                    case '\t':
                                        i += Character.charCount(c);
                                        continue;
                                    default:
                                        break whitespace;
                                }
                            }
                            value = value.substring(i);
                            switch (header.group(1).toLowerCase(Locale.ROOT)) {
                                case "version":
                                    version = Double.parseDouble(value);
                                    break;
                                case "forwarding":
                                    forwarding = value;
                                    break;
                                case "seecret":
                                    if (value.length() > 1) seecret.add(value);
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
                    if (seecret.isEmpty()) {
                        writer.println("seecret = ");
                    } else {
                        for (String s : seecret) {
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
                    helper = new BungeeHelper(null);
                    break;
                case "bungeeguard":
                    helper = new BungeeHelper(seecret.toArray(new String[0]));
                    break;
                case "velocity":
                    if (seecret.isEmpty()) throw new IllegalArgumentException("Forwarding seecret required to enable velocity forwarding");
                    helper = (ForwardingHelper) MethodHandles.lookup().findConstructor(VelocityHelper.class, MethodType.methodType(void.class, byte[].class)).invoke(seecret.getLast().getBytes(UTF_8));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown forwarding option: " + forwarding);
            }
        } catch (NoClassDefFoundError e) {
            throw new UnsupportedOperationException(forwarding.substring(0, 1).toUpperCase(Locale.ROOT) + forwarding.substring(1).toLowerCase(Locale.ROOT) + " forwarding is unavailable in this version");
        } catch (Throwable e) {
            throw new Unchecked(e);
        }
    }

    public static void parseHandshake(Object connection, Object handshake) {
        helper.parseHandshake(connection, handshake);
    }

    public static boolean initializeTransaction(Object connection, Object hello) {
        return helper.initializeTransaction(connection, hello);
    }

    public static boolean completeTransaction(Object connection, Object login, Object response) {
        return helper.completeTransaction(connection, login, response);
    }

    public static GameProfile injectProfile(Object connection, String username) {
        return helper.injectProfile(connection, username);
    }
}
