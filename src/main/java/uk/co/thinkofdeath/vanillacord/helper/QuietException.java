package uk.co.thinkofdeath.vanillacord.helper;

import java.io.PrintStream;
import java.io.PrintWriter;

public class QuietException extends RuntimeException {
    QuietException(String text) {
        super(text);
        this.e = null;
        if (text == null) {
            System.out.println("VanillaCord has disconnected a player because of an unspecified error.");
        } else {
            System.out.println("VanillaCord has disconnected a player with the following error message:");
            System.out.println('\t' + text);
        }
    }

    private final Throwable e;
    QuietException(Throwable e) {
        this.e = e;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        // This is a quiet exception
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        // This is a quiet exception
    }

    @Override
    public String toString() {
        if (e != null) {
            return e.toString();
        } else {
            return super.toString();
        }
    }
}
