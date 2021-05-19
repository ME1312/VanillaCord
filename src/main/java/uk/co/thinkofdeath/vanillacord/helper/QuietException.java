package uk.co.thinkofdeath.vanillacord.helper;

import java.io.PrintStream;
import java.io.PrintWriter;

public class QuietException extends RuntimeException {
    public QuietException(String text) {
        super(text);
    }

    private Throwable e;
    public QuietException(Throwable e) {
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
