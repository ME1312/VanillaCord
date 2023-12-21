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
}
