package vanillacord.data;

public final class Digest {
    private Digest() {}

    public static boolean equals(byte[] digest, String target) {
        int c, i, d = digest.length, t = target.length();
        while (d != 0 && t != 0) {
            i = (c = target.charAt(--t)) - ((c >= 'a')? 87 : ((c >= 'A')? 55 : 48));
            if (t != 0) {
                i += ((c = target.charAt(--t)) - ((c >= 'a')? 87 : ((c >= 'A')? 55 : 48))) << 4;
            }
            if (digest[--d] != (byte) i) break;
            if (d == 0 && t == 0) return true;
        }
        return false;
    }

    public static String toHex(byte[] array) {
        StringBuilder hex = new StringBuilder(array.length * 2);
        for (int c, i = 0; i != array.length;) {
            hex.append((char) ((((c = array[i++] & 0xFF) > 0x9F)? 87 : 48) + (c >>> 4)))
                           .append((char) ((((c &= 0x0F) > 0x09)? 87 : 48) + c));
        }
        return hex.toString();
    }
}
