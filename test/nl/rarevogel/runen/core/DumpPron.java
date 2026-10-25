package nl.rarevogel.runen.core;
/** Prints name<TAB>phonemes<TAB>speed for every audio clip the app needs. */
public class DumpPron {
    public static void main(String[] a) {
        for (Runes.Track t : Runes.TRACKS) for (Runes.Rune r : t.runes) System.out.println(r.audio() + "\t" + r.pron + "\t120");
        for (Stones.Stone s : Stones.ALL) {
            if (!s.audio) continue;
            System.out.println("s_" + s.id + "\t" + s.pron() + "\t135");
            for (int i = 0; i < s.words.size(); i++) if (s.words.get(i).pron.length() > 0) System.out.println("s_" + s.id + "_" + i + "\t" + s.words.get(i).pron + "\t120");
        }
    }
}
