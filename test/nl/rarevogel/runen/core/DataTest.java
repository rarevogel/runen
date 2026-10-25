package nl.rarevogel.runen.core;
import java.util.*;
public class DataTest {
    public static void main(String[] a) {
        int bad = 0;
        for (Runes.Track t : Runes.TRACKS) {
            System.out.println("== " + t.title + " " + t.size());
            for (Runes.Rune r : t.runes) {
                StringBuilder sb = new StringBuilder();
                for (Runes.Example e : r.examples) {
                    sb.append(e.latin).append("=").append(e.runes).append("  ");
                    if (!e.runes.contains(r.glyph)) { System.out.println("  !! example " + e.latin + " lacks " + r.glyph); bad++; }
                    for (char c : e.runes.toCharArray()) if (c != '᛫' && Runes.byGlyph(t, c) == null) { System.out.println("  !! " + e.latin + " has foreign rune " + c); bad++; }
                }
                System.out.println(r.glyph + " " + r.name + " [" + r.sound + "]  " + sb);
            }
        }
        for (Stones.Stone st : Stones.ALL) {
            StringBuilder sb = new StringBuilder();
            for (Stones.Word w : st.words) { sb.append(w.runes).append(' '); for (char c : w.runes.toCharArray()) if (Runes.byGlyph(st.track, c) == null) { System.out.println("  !! stone " + st.id + " foreign rune " + c); bad++; } }
            System.out.println(st.id + ": " + sb);
        }
        System.out.println(Translit.toRunes("Vogel van Rarevogel, Wijk bij Duurstede 2026", false));
        System.out.println(Translit.toRunes("Vogel van Rarevogel, Wijk bij Duurstede 2026", true));
        // quiz simulation: perfect player unlocks everything
        Map<String,String> m = new HashMap<>();
        Progress.Store st = new Progress.Store() { public String get(String k){return m.get(k);} public void put(String k,String v){m.put(k,v);} public void clear(){m.clear();} };
        for (Runes.Track t : Runes.TRACKS) {
            Progress p = new Progress(st);
            Random rnd = new Random(1);
            int sessions = 0; int[] types = new int[5];
            while (p.mastered(t) < t.size() && sessions < 200) {
                Quiz q = Quiz.make(t, p, rnd);
                for (Quiz.Step s : q.steps) {
                    types[s.type]++;
                    if (s.type == Quiz.INTRO) continue;
                    boolean ok = rnd.nextFloat() < 0.85f;
                    if (s.type == Quiz.WRITE) { if (ok) s.typed.append(s.target); q.checkWrite(s, p); }
                    else { if (s.options.length != 4) { System.out.println("!! options " + s.options.length); bad++; } q.choose(s, ok ? s.answer : (s.answer + 1) % s.options.length, p); }
                }
                p.checkUnlock(t); p.sessionDone(sessions); sessions++;
            }
            p.save(); Progress p2 = new Progress(st);
            System.out.println(t.title + ": sessions to master all " + sessions + " types " + Arrays.toString(types) + " xp " + p.xp + " reload ok " + (p2.xp == p.xp && p2.mastered(t) == p.mastered(t)) + " streak " + p.streak);
        }
        System.out.println(bad == 0 ? "ALL OK" : "PROBLEMS " + bad);
    }
}
