package uk.co.thinkofdeath.vanillacord.helper;

import java.io.PrintStream;
import java.io.PrintWriter;

public class QuietException extends RuntimeException {
    public QuietException() {}
    public QuietException(String text) {
        super('\n' + text);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        // This is a quiet exception
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        // This is a quiet exception
    }
}
