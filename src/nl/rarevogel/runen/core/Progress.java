package nl.rarevogel.runen.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.rarevogel.runen.core.Runes.Rune;
import nl.rarevogel.runen.core.Runes.Track;

/**
 * Learning progress with Leitner boxes: 0 = never practised, 1..5 = how well a rune is known.
 * Runes unlock four at a time once the unlocked ones are reasonably known.
 */
public final class Progress {
    public interface Store {
        String get(String key);
        void put(String key, String value);
        void clear();
    }

    public static final int MAX_BOX = 5, MASTERED = 4, START_UNLOCKED = 4, UNLOCK_STEP = 4;

    final Store store;
    final HashMap<String, int[]> runes = new HashMap<>();   // key -> {box, right, wrong}
    final HashMap<String, Integer> unlocked = new HashMap<>();
    final HashMap<String, boolean[]> stones = new HashMap<>();
    public int xp, right, wrong, sessions, streak, bestStreak;
    public long lastDay = -1;
    public String track = "O";
    public boolean dotted;

    public Progress(Store store) { this.store = store; load(); }

    int[] rec(Rune r) {
        int[] a = runes.get(r.key());
        if (a == null) { a = new int[3]; runes.put(r.key(), a); }
        return a;
    }

    public int box(Rune r) { return rec(r)[0]; }
    public int rightOf(Rune r) { return rec(r)[1]; }
    public int wrongOf(Rune r) { return rec(r)[2]; }
    public boolean seen(Rune r) { int[] a = rec(r); return a[1] + a[2] > 0; }

    public int unlocked(Track t) {
        Integer u = unlocked.get(t.id);
        return Math.min(t.size(), u == null ? START_UNLOCKED : u);
    }

    public boolean isUnlocked(Rune r) { return r.index < unlocked(r.track); }

    public int mastered(Track t) {
        int n = 0;
        for (Rune r : t.runes) if (box(r) >= MASTERED) n++;
        return n;
    }

    /** 0..1: average box over all runes of the track. */
    public float score(Track t) {
        float s = 0;
        for (Rune r : t.runes) s += box(r);
        return s / (t.size() * (float) MAX_BOX);
    }

    public void record(Rune r, boolean ok) {
        int[] a = rec(r);
        if (ok) { a[0] = Math.min(MAX_BOX, a[0] + 1); a[1]++; right++; xp += 10; }
        else { a[0] = Math.max(1, a[0] - 1); a[2]++; wrong++; }
    }

    /** Unlocks the next runes when every unlocked rune has reached box 2. Returns the new ones. */
    public List<Rune> checkUnlock(Track t) {
        List<Rune> fresh = new ArrayList<>();
        int u = unlocked(t);
        if (u >= t.size()) return fresh;
        for (int i = 0; i < u; i++) if (box(t.runes.get(i)) < 2) return fresh;
        int nu = Math.min(t.size(), u + UNLOCK_STEP);
        for (int i = u; i < nu; i++) fresh.add(t.runes.get(i));
        unlocked.put(t.id, nu);
        return fresh;
    }

    public void unlockAll(Track t) { unlocked.put(t.id, t.size()); }

    // ---------------------------------------------------------------- inscriptions

    boolean[] words(Stones.Stone s) {
        boolean[] b = stones.get(s.id);
        if (b == null || b.length != s.words.size()) { b = new boolean[s.words.size()]; stones.put(s.id, b); }
        return b;
    }

    public boolean revealed(Stones.Stone s, int i) { return words(s)[i]; }

    /** Reveals one word; returns true the first time, and gives 5 points for it. */
    public boolean reveal(Stones.Stone s, int i) {
        boolean[] b = words(s);
        if (b[i]) return false;
        b[i] = true;
        xp += 5;
        return true;
    }

    public void revealAll(Stones.Stone s) { java.util.Arrays.fill(words(s), true); }

    public int revealedCount(Stones.Stone s) { int n = 0; for (boolean x : words(s)) if (x) n++; return n; }

    public boolean read(Stones.Stone s) { return revealedCount(s) == s.words.size(); }

    public int stonesRead() { int n = 0; for (Stones.Stone s : Stones.ALL) if (read(s)) n++; return n; }

    /** Called after a finished practice session; today is the epoch day. */
    public void sessionDone(long today) {
        sessions++;
        if (lastDay == today) { /* same day */ }
        else if (lastDay == today - 1) streak++;
        else streak = 1;
        lastDay = today;
        if (streak > bestStreak) bestStreak = streak;
    }

    /** Streak shown on the home screen: 0 once a day has been skipped. */
    public int currentStreak(long today) { return lastDay >= today - 1 ? streak : 0; }

    // ---------------------------------------------------------------- persistence

    void load() {
        runes.clear(); unlocked.clear(); stones.clear();
        for (Stones.Stone st : Stones.ALL) {
            String v = store.get("st_" + st.id);
            if (v == null) continue;
            boolean[] b = new boolean[st.words.size()];
            for (int i = 0; i < b.length && i < v.length(); i++) b[i] = v.charAt(i) == '1';
            stones.put(st.id, b);
        }
        String s = store.get("runes");
        if (s != null && s.length() > 0) {
            for (String part : s.split(";")) {
                String[] kv = part.split("=");
                if (kv.length != 2) continue;
                String[] n = kv[1].split(",");
                if (n.length != 3) continue;
                try { runes.put(kv[0], new int[]{Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2])}); }
                catch (NumberFormatException ignored) { }
            }
        }
        for (Track t : Runes.TRACKS) {
            String u = store.get("unlocked" + t.id);
            if (u != null) try { unlocked.put(t.id, Integer.parseInt(u)); } catch (NumberFormatException ignored) { }
        }
        xp = num("xp"); right = num("right"); wrong = num("wrong"); sessions = num("sessions");
        streak = num("streak"); bestStreak = num("best");
        String d = store.get("lastDay");
        lastDay = d == null ? -1 : Long.parseLong(d);
        dotted = "1".equals(store.get("dotted"));
        String tr = store.get("track");
        if (tr != null) track = tr;
    }

    int num(String k) {
        String v = store.get(k);
        try { return v == null ? 0 : Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    public void save() {
        StringBuilder b = new StringBuilder();
        for (java.util.Map.Entry<String, int[]> e : runes.entrySet()) {
            int[] a = e.getValue();
            if (a[0] == 0 && a[1] == 0 && a[2] == 0) continue;
            if (b.length() > 0) b.append(';');
            b.append(e.getKey()).append('=').append(a[0]).append(',').append(a[1]).append(',').append(a[2]);
        }
        store.put("runes", b.toString());
        for (java.util.Map.Entry<String, Integer> e : unlocked.entrySet()) store.put("unlocked" + e.getKey(), e.getValue().toString());
        for (java.util.Map.Entry<String, boolean[]> e : stones.entrySet()) {
            StringBuilder v = new StringBuilder();
            for (boolean x : e.getValue()) v.append(x ? '1' : '0');
            store.put("st_" + e.getKey(), v.toString());
        }
        store.put("xp", "" + xp); store.put("right", "" + right); store.put("wrong", "" + wrong);
        store.put("sessions", "" + sessions); store.put("streak", "" + streak); store.put("best", "" + bestStreak);
        store.put("lastDay", "" + lastDay); store.put("track", track); store.put("dotted", dotted ? "1" : "0");
    }

    public void reset() {
        String tr = track;
        boolean dt = dotted;
        store.clear();
        load();
        track = tr;
        dotted = dt;
        save();
    }
}
