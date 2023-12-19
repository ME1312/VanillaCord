package vanillacord.server;

import com.mojang.authlib.GameProfile;

public abstract class ForwardingHelper {
    public void parseHandshake(Object connection, Object handshake) {
      //return;
    }

    public boolean initializeTransaction(Object connection, Object hello) {
        return false;
    }

    public boolean completeTransaction(Object connection, Object login, Object response) {
        return false;
    }

    public abstract GameProfile injectProfile(Object connection, String username);

    static RuntimeException exception(String text, Throwable e) {
        if (e instanceof QuietException) {
            return (QuietException) e;
        } else if (e.getCause() instanceof QuietException) {
            return (QuietException) e.getCause();
        } else {
            if (text != null) e = new RuntimeException(text, e);
            e.printStackTrace();
            return new QuietException(e);
        }
    }
}
