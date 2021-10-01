package uk.co.thinkofdeath.vanillacord.library;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public final class QuietStream extends PrintStream {
    public QuietStream() {
        super(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // This is a quiet stream
            }
        });
    }
}
