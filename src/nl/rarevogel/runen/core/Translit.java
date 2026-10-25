package nl.rarevogel.runen.core;

import java.text.Normalizer;

/**
 * Turns modern (Dutch) spelling into runes. Rules, in this order:
 * digraphs first (th, ng, ch, ck, ph, ie, oe, ij/ei/ey), then single letters,
 * and runs of the same rune collapse to one (the rune carvers did not write double letters).
 */
public final class Translit {
    public static final String SEP = "᛫";

    // Elder futhark
    static final String[][] ELDER_DI = {
            {"th", "ᚦ"}, {"ng", "ᛜ"}, {"ch", "ᚲ"}, {"ck", "ᚲ"}, {"ph", "ᚠ"}, {"qu", "ᚲᚹ"},
            {"ie", "ᛁ"}, {"oe", "ᚢ"}, {"ij", "ᛇ"}, {"ei", "ᛇ"}, {"ey", "ᛇ"}};
    static final String ELDER_AZ = "ᚨᛒᚲᛞᛖᚠᚷᚺᛁᛃᚲᛚᛗᚾᛟᛈᚲᚱᛊᛏᚢᚠᚹ?ᛁᛉ";

    // Younger futhark (long-branch runes)
    static final String[][] YOUNGER_DI = {
            {"th", "ᚦ"}, {"ng", "ᚾᚴ"}, {"ch", "ᚴ"}, {"ck", "ᚴ"}, {"ph", "ᚠ"}, {"qu", "ᚴᚢ"},
            {"ie", "ᛁ"}, {"oe", "ᚢ"}, {"ij", "ᛅᛁ"}, {"ei", "ᛅᛁ"}, {"ey", "ᛅᛁ"}};
    static final String YOUNGER_AZ = "ᛅᛒᚴᛏᛁᚠᚴᚼᛁᛁᚴᛚᛘᚾᚬᛒᚴᚱᛋᛏᚢᚠᚢ?ᚢᛋ";

    // Anglo-Frisian futhorc
    static final String[][] FUTHORC_DI = {
            {"th", "ᚦ"}, {"ng", "ᛝ"}, {"ea", "ᛠ"}, {"eo", "ᛇ"}, {"ae", "ᚫ"}, {"eu", "ᛟ"}, {"ch", "ᚳ"}, {"ck", "ᚳ"},
            {"ph", "ᚠ"}, {"qu", "ᚳᚹ"}, {"ie", "ᛁ"}, {"oe", "ᚢ"}, {"ij", "ᛖᛁ"}, {"ei", "ᛖᛁ"}};
    static final String FUTHORC_AZ = "ᚪᛒᚳᛞᛖᚠᚷᚻᛁᛄᚳᛚᛗᚾᚩᛈᚳᚱᛋᛏᚢᚠᚹᛉᚣᛋ";

    public static final int ELDER = 0, YOUNGER = 1, FUTHORC = 2;

    public static String clean(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == 'þ') b.append("th");
            else if (c == 'ð') b.append('d');
            else if (c == 'æ') b.append("ae");
            else if (c == 'ø') b.append('o');
            else if (c == 'ß') b.append("ss");
            else if (Character.getType(c) != Character.NON_SPACING_MARK) b.append(c);
        }
        return b.toString();
    }

    /** Converts one text; words are separated by the runic word divider. */
    public static String toRunes(String text, boolean younger) { return toRunes(text, younger ? YOUNGER : ELDER, false); }

    public static String toRunes(String text, boolean younger, boolean dotted) { return toRunes(text, younger ? YOUNGER : ELDER, dotted); }

    /** Dotted runes (from about 1000 AD): e ᛂ, g ᚵ, d ᛑ, p ᛔ, y ᚤ. Only for the younger futhark. */
    public static final String DOTTED_LATIN = "egdpy", DOTTED_RUNES = "ᛂᚵᛑᛔᚤ";

    public static String toRunes(String text, int mode, boolean dotted) {
        boolean younger = mode == YOUNGER, futhorc = mode == FUTHORC;
        dotted &= younger;
        String s = clean(text);
        String[][] di = futhorc ? FUTHORC_DI : younger ? YOUNGER_DI : ELDER_DI;
        String az = futhorc ? FUTHORC_AZ : younger ? YOUNGER_AZ : ELDER_AZ;
        StringBuilder out = new StringBuilder();
        StringBuilder word = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') { word.append(c); i++; continue; }
            if (c < 'a' || c > 'z') {
                if (word.length() > 0) { if (out.length() > 0) out.append(SEP); out.append(word); word.setLength(0); }
                i++;
                continue;
            }
            String r = null;
            for (String[] d : di) if (s.startsWith(d[0], i)) { r = d[1]; i += 2; break; }
            if (r != null && dotted && r.equals("ᚾᚴ")) r = "ᚾᚵ";
            if (r == null) {
                if (c == 'x' && !futhorc) r = younger ? "ᚴᛋ" : "ᚲᛊ";
                else if (c == 'c' && i + 1 < s.length() && "eiy".indexOf(s.charAt(i + 1)) >= 0) r = younger || futhorc ? "ᛋ" : "ᛊ";
                else if (dotted && DOTTED_LATIN.indexOf(c) >= 0) r = String.valueOf(DOTTED_RUNES.charAt(DOTTED_LATIN.indexOf(c)));
                else r = String.valueOf(az.charAt(c - 'a'));
                i++;
            }
            for (int k = 0; k < r.length(); k++) {
                char rc = r.charAt(k);
                if (word.length() == 0 || word.charAt(word.length() - 1) != rc) word.append(rc);
            }
        }
        if (word.length() > 0) { if (out.length() > 0) out.append(SEP); out.append(word); }
        return out.toString();
    }
}
