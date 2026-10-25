package nl.rarevogel.runen.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nl.rarevogel.runen.core.Runes.Example;
import nl.rarevogel.runen.core.Runes.Rune;
import nl.rarevogel.runen.core.Runes.Track;

/** Builds a practice session of ten steps, weighted towards the runes you know least. */
public final class Quiz {
    public static final int INTRO = 0, SOUND = 1, RUNE = 2, READ = 3, WRITE = 4;
    public static final int LENGTH = 10;

    public static final class Step {
        public int type;
        public Rune rune;
        public Example example;
        public String[] options;     // SOUND: sounds, RUNE: glyphs, READ: names
        public int answer;
        public String target = "";   // WRITE: the rune string to build
        // state while answering
        public int chosen = -1;
        public StringBuilder typed = new StringBuilder();
        public boolean answered, correct;
    }

    public final Track track;
    public final List<Step> steps = new ArrayList<>();
    public int pos, right, wrong;
    public List<Rune> unlockedNow = new ArrayList<>();

    Quiz(Track t) { track = t; }

    public Step current() { return pos < steps.size() ? steps.get(pos) : null; }
    public boolean done() { return pos >= steps.size(); }
    public int questions() { int n = 0; for (Step s : steps) if (s.type != INTRO) n++; return n; }
    public int questionNumber() { int n = 0; for (int i = 0; i <= pos && i < steps.size(); i++) if (steps.get(i).type != INTRO) n++; return n; }

    public static String shortName(Example e) {
        int i = e.latin.indexOf(" (");
        return i > 0 ? e.latin.substring(0, i) : e.latin;
    }

    static boolean usesOnly(String runes, Track t, Progress p) {
        for (int i = 0; i < runes.length(); i++) {
            char c = runes.charAt(i);
            if (Translit.SEP.charAt(0) == c) continue;
            Rune r = Runes.byGlyph(t, c);
            if (r == null || !p.isUnlocked(r)) return false;
        }
        return true;
    }

    public static Quiz make(Track t, Progress p, Random rnd) {
        Quiz q = new Quiz(t);
        int u = p.unlocked(t);
        List<Rune> pool = new ArrayList<>(t.runes.subList(0, u));
        Rune last = null;
        java.util.HashSet<Rune> introduced = new java.util.HashSet<>();
        for (int n = 0; n < LENGTH; n++) {
            // weighted pick, never the same rune twice in a row
            float total = 0;
            float[] w = new float[pool.size()];
            for (int i = 0; i < pool.size(); i++) {
                Rune r = pool.get(i);
                int b = p.box(r);
                w[i] = r == last ? 0 : (6 - b) * (6 - b) + (b == 0 && !introduced.contains(r) ? 20 : 0);
                total += w[i];
            }
            float x = rnd.nextFloat() * total;
            Rune pick = pool.get(0);
            for (int i = 0; i < pool.size(); i++) { x -= w[i]; if (x <= 0 && w[i] > 0) { pick = pool.get(i); break; } }
            last = pick;

            if (!p.seen(pick) && !introduced.contains(pick)) {
                Step in = new Step();
                in.type = INTRO; in.rune = pick;
                q.steps.add(in);
                introduced.add(pick);
            }
            q.steps.add(question(t, p, pick, rnd));
        }
        return q;
    }

    static Step question(Track t, final Progress p, Rune r, Random rnd) {
        int b = p.box(r);
        List<Integer> types = new ArrayList<>();
        types.add(SOUND); types.add(RUNE);
        if (b >= 2) types.add(READ);
        if (b >= 3) { types.add(READ); types.add(WRITE); }
        // examples that only use unlocked runes
        List<Example> readable = new ArrayList<>(), writable = new ArrayList<>();
        for (Example e : r.examples) if (usesOnly(e.runes, t, p)) { readable.add(e); if (!e.authentic && !e.runes.contains(Translit.SEP)) writable.add(e); }
        while (readable.isEmpty() && types.remove(Integer.valueOf(READ))) { }
        if (writable.isEmpty()) types.remove(Integer.valueOf(WRITE));
        int type = types.get(rnd.nextInt(types.size()));

        Step s = new Step();
        s.type = type; s.rune = r;
        if (type == SOUND || type == RUNE) {
            List<Rune> opts = new ArrayList<>();
            opts.add(r);
            List<Rune> others = new ArrayList<>(t.runes);
            java.util.Collections.shuffle(others, rnd);
            // prefer unlocked runes as distractors, avoid identical sounds
            java.util.Collections.sort(others, new java.util.Comparator<Rune>() {
                public int compare(Rune a, Rune c) { return Boolean.compare(!p.isUnlocked(a), !p.isUnlocked(c)); }
            });
            for (Rune o : others) {
                if (opts.size() == 4) break;
                boolean dup = false;
                for (Rune k : opts) if (k.sound.equals(o.sound) || k.glyph.equals(o.glyph)) dup = true;
                if (!dup) opts.add(o);
            }
            java.util.Collections.shuffle(opts, rnd);
            s.options = new String[opts.size()];
            for (int i = 0; i < opts.size(); i++) {
                s.options[i] = type == SOUND ? opts.get(i).sound : opts.get(i).glyph;
                if (opts.get(i) == r) s.answer = i;
            }
        } else if (type == READ) {
            Example e = readable.get(rnd.nextInt(readable.size()));
            s.example = e;
            List<String> opts = new ArrayList<>();
            opts.add(shortName(e));
            List<Example> all = new ArrayList<>();
            for (Rune o : t.runes) for (Example x : o.examples) all.add(x);
            java.util.Collections.shuffle(all, rnd);
            for (Example x : all) {
                if (opts.size() == 4) break;
                String n = shortName(x);
                if (!opts.contains(n)) opts.add(n);
            }
            java.util.Collections.shuffle(opts, rnd);
            s.options = opts.toArray(new String[0]);
            s.answer = opts.indexOf(shortName(e));
        } else {
            Example e = writable.get(rnd.nextInt(writable.size()));
            s.example = e;
            s.target = e.runes;
        }
        return s;
    }

    /** Applies an answer for choice questions. */
    public void choose(Step s, int i, Progress p) {
        if (s.answered) return;
        s.chosen = i;
        s.answered = true;
        s.correct = i == s.answer;
        score(s, p);
    }

    public void checkWrite(Step s, Progress p) {
        if (s.answered) return;
        s.answered = true;
        s.correct = s.typed.toString().equals(s.target);
        score(s, p);
    }

    void score(Step s, Progress p) {
        if (s.correct) right++; else wrong++;
        p.record(s.rune, s.correct);
    }

    public void next() { pos++; }
}
