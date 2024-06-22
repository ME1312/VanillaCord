package vanillacord.server;

import java.io.PrintStream;
import java.io.PrintWriter;

public final class QuietException extends RuntimeException {
    QuietException(String text) {
        super(text);
        this.e = null;
    }

    static QuietException notify(String text) {
        System.out.println((text == null)?
                "VanillaCord has disconnected a player because of an unspecified error." :
                "VanillaCord has disconnected a player with the following error message:"
        );
        return new QuietException(text);
    }

    static QuietException show(String text) {
        QuietException e = notify(text);
        System.out.println('\t' + text);
        return e;
    }

    static QuietException show(Throwable e) {
        if (e instanceof QuietException) {
            return (QuietException) e;
        } else {
            e.printStackTrace();
            return new QuietException(e);
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
        } else if (getMessage() != null) {
            return getMessage();
        } else {
            return super.toString();
        }
    }
}
