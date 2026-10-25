package nl.rarevogel.runen.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nl.rarevogel.runen.core.Runes.Example;
import nl.rarevogel.runen.core.Runes.Rune;
import nl.rarevogel.runen.core.Runes.Track;

/** The whole app, drawn on a canvas: screens, layout, touch handling. */
public final class App {
    public interface Host {
        void quit();
        long today();
        void buzz(int ms);
        /** Plays assets/audio/<name>.ogg if it exists. */
        void play(String name);
    }

    // colours
    static final int BG0 = 0xFF1C232B, BG1 = 0xFF0D1115;
    static final int STONE0 = 0xFF4A525C, STONE1 = 0xFF30363E, STONE_EDGE = 0xFF59626D;
    static final int BONE = 0xFFEDE3CF, MUTED = 0xFFA79F8F, FAINT = 0xFF6E6A62;
    static final int RED = 0xFFD0472F, RED_DARK = 0xFF8E2A1C, GOLD = 0xFFE2AE4C, GREEN = 0xFF6FBF73, BAD = 0xFFE0604A;
    static final int[] BOX_COL = {0xFF4A4F57, 0xFFC0503A, 0xFFD98A3A, 0xFFE2C04C, 0xFF9CCB5E, 0xFF5DBB6A};

    public static final int HOME = 0, CARDS = 1, DETAIL = 2, QUIZ = 3, SUMMARY = 4, WRITE = 5, PROGRESS = 6, INFO = 7, STONES = 8, STONE = 9;

    final Progress p;
    final Host host;
    final Random rnd = new Random();
    public Track track;
    public int screen = HOME;
    final ArrayList<Integer> stack = new ArrayList<>();
    final float[] scroll = new float[10];
    float maxScroll;
    public int detail;
    public Stones.Stone stone;
    public Quiz quiz;
    public String typed = "";
    boolean confirmReset;
    List<Rune> fresh = new ArrayList<>();
    public float time;

    // layout
    int W, H;
    float u, ox, cw;
    Object bgLayer;
    int bgKey;

    // hits
    final ArrayList<float[]> hitR = new ArrayList<>();
    final ArrayList<Runnable> hitA = new ArrayList<>();
    float offY, clipTop, clipBottom;

    // touch
    float downX, downY, lastY, vel;
    boolean pressed, dragging;
    float scrollTop, scrollBottom;

    public App(Progress p, Host host) {
        this.p = p;
        this.host = host;
        track = Runes.track(p.track);
    }

    // ================================================================ navigation

    void go(int s) {
        stack.add(screen);
        screen = s;
        scroll[s] = 0;
    }

    public boolean back() {
        if (confirmReset) { confirmReset = false; return true; }
        if (stack.isEmpty()) return false;
        if (screen == QUIZ || screen == SUMMARY) { p.save(); }
        screen = stack.remove(stack.size() - 1);
        if (screen == QUIZ && quiz != null && quiz.done()) screen = stack.isEmpty() ? HOME : stack.remove(stack.size() - 1);
        return true;
    }

    void home() { stack.clear(); screen = HOME; }

    void setTrack(Track t) { track = t; p.track = t.id; p.save(); }

    void startQuiz() {
        quiz = Quiz.make(track, p, rnd);
        if (screen == SUMMARY) { screen = QUIZ; } else go(QUIZ);
    }

    void finishQuiz() {
        fresh = p.checkUnlock(track);
        p.sessionDone(host.today());
        p.save();
        screen = SUMMARY;
        scroll[SUMMARY] = 0;
    }

    /** True while something moves; otherwise the host may redraw slowly (caret blink only). */
    public boolean busy() { return pressed || Math.abs(vel) > 5; }

    // ================================================================ input

    public void down(float x, float y) {
        downX = x; downY = y; lastY = y; pressed = true; dragging = false; vel = 0;
    }

    public void move(float x, float y) {
        if (!pressed) return;
        if (!dragging && Math.abs(y - downY) > 10 * u && downY >= scrollTop && downY <= scrollBottom) dragging = true;
        if (dragging) {
            float dy = y - lastY;
            scroll[screen] -= dy;
            vel = vel * 0.6f - dy * 0.4f * 60;
        }
        lastY = y;
    }

    public void up(float x, float y) {
        if (pressed && !dragging) tap(x, y);
        pressed = false;
        if (!dragging) vel = 0;
        dragging = false;
    }

    void tap(float x, float y) {
        for (int i = hitR.size() - 1; i >= 0; i--) {
            float[] r = hitR.get(i);
            if (x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3] && y >= r[4] && y <= r[5]) {
                hitA.get(i).run();
                return;
            }
        }
    }

    void hit(float x, float y, float w, float h, Runnable a) {
        hitR.add(new float[]{x, y + offY, w, h, clipTop, clipBottom});
        hitA.add(a);
    }

    boolean isPressed(float x, float y, float w, float h) {
        if (!pressed || dragging) return false;
        float yy = y + offY;
        return downX >= x && downX <= x + w && downY >= yy && downY <= yy + h && downY >= clipTop && downY <= clipBottom;
    }

    // ================================================================ frame

    public void frame(Gfx g, int W, int H, float dt) {
        this.W = W; this.H = H;
        time += dt;
        cw = Math.min(W, H * 0.62f);
        u = cw / 400f;
        ox = (W - cw) / 2f;
        hitR.clear(); hitA.clear();
        offY = 0; clipTop = 0; clipBottom = H;
        scrollTop = 0; scrollBottom = 0;

        // fling
        if (!pressed && Math.abs(vel) > 5) {
            scroll[screen] += vel * dt;
            vel *= (float) Math.pow(0.05, dt);
        }

        background(g);
        switch (screen) {
            case HOME: home(g); break;
            case CARDS: cards(g); break;
            case DETAIL: detail(g); break;
            case QUIZ: quiz(g); break;
            case SUMMARY: summary(g); break;
            case WRITE: write(g); break;
            case PROGRESS: progress(g); break;
            case STONES: stones(g); break;
            case STONE: stone(g); break;
            default: info(g); break;
        }
        if (confirmReset) confirmDialog(g);
    }

    // ================================================================ drawing helpers

    void background(Gfx g) {
        int key = W * 10000 + H;
        if (bgLayer == null || bgKey != key) {
            bgKey = key;
            final int fw = W, fh = H;
            bgLayer = g.layer(Math.max(1, W / 2), Math.max(1, H / 2), new Gfx.Painter() {
                public void paint(Gfx gg) {
                    float w = fw / 2f, h = fh / 2f;
                    gg.linear(0, 0, 0, h, BG0, BG1);
                    gg.fillRect(0, 0, w, h);
                    gg.radial(w * 0.5f, h * 0.12f, w * 0.9f, 0x30E2AE4C, 0x00000000);
                    gg.fillRect(0, 0, w, h);
                    Random r = new Random(4);
                    for (int i = 0; i < (int) (w * h / 90); i++) {
                        gg.color(r.nextBoolean() ? 0x0CFFFFFF : 0x14000000);
                        gg.fillCircle(r.nextFloat() * w, r.nextFloat() * h, 0.6f + r.nextFloat() * 1.6f);
                    }
                }
            });
        }
        g.drawLayer(bgLayer, 0, 0, W, H);
    }

    float X(float ux) { return ox + ux * u; }

    void stone(Gfx g, float x, float y, float w, float h, float r) {
        g.color(0x55000000);
        g.fillRoundRect(x + 1.5f * u, y + 3 * u, w, h, r);
        g.linear(0, y, 0, y + h, STONE0, STONE1);
        g.fillRoundRect(x, y, w, h, r);
        g.color(0x22FFFFFF);
        g.strokeRoundRect(x + 0.5f * u, y + 0.5f * u, w - u, h - u, r, 1.2f * u);
    }

    void runes(Gfx g, String s, float x, float y, float size, int align, int col) {
        g.color(0xAA000000);
        g.text(s, x + size * 0.02f, y + size * 0.035f, size, align, Gfx.RUNE);
        g.color(col);
        g.text(s, x, y, size, align, Gfx.RUNE);
    }

    /** Baseline for runes centred vertically on cy. */
    float runeBase(float cy, float size) { return cy + size * 0.36f; }

    List<String> wrap(Gfx g, String s, float size, int font, float maxW) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            String line = "";
            for (String w : para.split(" ")) {
                String t = line.isEmpty() ? w : line + " " + w;
                if (g.textWidth(t, size, font) > maxW && !line.isEmpty()) { out.add(line); line = w; }
                else line = t;
            }
            out.add(line);
        }
        return out;
    }

    /** Draws wrapped text and returns the height used. */
    float para(Gfx g, String s, float x, float y, float size, int font, int col, float maxW) {
        g.color(col);
        float lh = size * 1.38f;
        List<String> lines = wrap(g, s, size, font, maxW);
        for (int i = 0; i < lines.size(); i++) g.text(lines.get(i), x, y + size + i * lh, size, -1, font);
        return lines.size() * lh;
    }

    static final int PRIMARY = 0, STONEBTN = 1, GHOST = 2, GOLDBTN = 3;

    void button(Gfx g, float x, float y, float w, float h, String label, int kind, Runnable a) {
        boolean pr = isPressed(x, y, w, h);
        float r = Math.min(h * 0.32f, 16 * u);
        if (kind == PRIMARY) {
            g.color(0x66000000); g.fillRoundRect(x + 1.5f * u, y + 3 * u, w, h, r);
            g.linear(0, y, 0, y + h, pr ? 0xFFB13A27 : 0xFFDB5236, pr ? 0xFF6E2014 : 0xFF9A2E1E);
            g.fillRoundRect(x, y, w, h, r);
            g.color(0x40FFFFFF); g.strokeRoundRect(x + u, y + u, w - 2 * u, h - 2 * u, r, 1.2f * u);
        } else if (kind == GOLDBTN) {
            g.color(0x66000000); g.fillRoundRect(x + 1.5f * u, y + 3 * u, w, h, r);
            g.linear(0, y, 0, y + h, pr ? 0xFFC0902F : 0xFFEDBE5E, pr ? 0xFF7A5512 : 0xFFA9771E);
            g.fillRoundRect(x, y, w, h, r);
        } else if (kind == STONEBTN) {
            stone(g, x, y, w, h, r);
            if (pr) { g.color(0x33000000); g.fillRoundRect(x, y, w, h, r); }
        } else {
            g.color(pr ? 0x33FFFFFF : 0x18FFFFFF); g.fillRoundRect(x, y, w, h, r);
            g.color(0x44FFFFFF); g.strokeRoundRect(x, y, w, h, r, 1.2f * u);
        }
        if (label != null) {
            float fs = Math.min(h * 0.38f, 20 * u);
            g.color(kind == GOLDBTN ? 0xFF2A1A05 : BONE);
            g.text(label, x + w / 2, y + h / 2 + fs * 0.36f, fs, 0, Gfx.BOLD);
        }
        if (a != null) hit(x, y, w, h, a);
    }

    /** Fixed header with optional back button; returns its height. */
    float header(Gfx g, String title, String sub) {
        float h = 66 * u;
        g.linear(0, 0, 0, h, 0xF0161B21, 0xE0161B21);
        g.fillRect(0, 0, W, h);
        g.color(0x55E2AE4C);
        g.fillRect(0, h - 1.2f * u, W, 1.2f * u);
        float bx = X(10), by = 13 * u, bs = 40 * u;
        boolean pr = isPressed(bx, by, bs, bs);
        g.color(pr ? 0x33FFFFFF : 0x14FFFFFF);
        g.fillCircle(bx + bs / 2, by + bs / 2, bs / 2);
        g.color(BONE);
        float cx = bx + bs / 2, cy = by + bs / 2;
        g.strokePoly(new float[]{cx + 4 * u, cy - 9 * u, cx - 5 * u, cy, cx + 4 * u, cy + 9 * u}, false, 3 * u);
        hit(bx - 6 * u, by - 8 * u, bs + 12 * u, bs + 16 * u, new Runnable() { public void run() { back(); } });
        g.color(GOLD);
        float ts = sub == null ? 24 * u : 21 * u;
        float maxW = X(390) - X(62);
        if (g.textWidth(title, ts, Gfx.TITLE) > maxW) ts *= maxW / g.textWidth(title, ts, Gfx.TITLE);
        g.text(title, X(62), sub == null ? 41 * u : 32 * u, ts, -1, Gfx.TITLE);
        if (sub != null) { g.color(MUTED); g.text(sub, X(62), 52 * u, 13 * u, -1, Gfx.SANS); }
        return h;
    }

    void beginScroll(Gfx g, float top, float bottom) {
        scrollTop = top; scrollBottom = bottom;
        clipTop = top; clipBottom = bottom;
        g.save();
        g.clip(0, top, W, bottom - top);
        offY = top - scroll[screen];
        g.translate(0, offY);
    }

    void endScroll(Gfx g, float contentH) {
        g.restore();
        maxScroll = Math.max(0, contentH - (scrollBottom - scrollTop));
        if (scroll[screen] < 0) { scroll[screen] = 0; if (!pressed) vel = 0; }
        if (scroll[screen] > maxScroll) { scroll[screen] = maxScroll; if (!pressed) vel = 0; }
        offY = 0; clipTop = 0; clipBottom = H;
    }

    void tabs(Gfx g, float x, float y, float w, float h) {
        g.color(0x40000000);
        g.fillRoundRect(x, y, w, h, h / 2);
        int n = Runes.TRACKS.length;
        float tw = w / n;
        for (int i = 0; i < n; i++) {
            final Track t = Runes.TRACKS[i];
            float tx = x + i * tw;
            boolean on = t == track;
            if (on) {
                g.linear(0, y, 0, y + h, 0xFFEDBE5E, 0xFFB98423);
                g.fillRoundRect(tx + 3 * u, y + 3 * u, tw - 6 * u, h - 6 * u, (h - 6 * u) / 2);
            }
            g.color(on ? 0xFF2A1A05 : MUTED);
            g.text(t.tab, tx + tw / 2, y + h / 2 + 5.5f * u, 15 * u, 0, Gfx.BOLD);
            hit(tx, y, tw, h, new Runnable() { public void run() { setTrack(t); } });
        }
    }

    void ring(Gfx g, float cx, float cy, float r, float frac, float w) {
        g.color(0x40000000);
        g.strokeCircle(cx, cy, r, w);
        if (frac > 0.001f) {
            g.color(GOLD);
            g.arc(cx, cy, r, -90, 360 * Math.min(1, frac), w);
        }
    }

    void pips(Gfx g, float cx, float y, int box, float s) {
        for (int i = 0; i < Progress.MAX_BOX; i++) {
            float x = cx + (i - 2) * s * 2.6f;
            g.color(i < box ? BOX_COL[Math.max(1, box)] : 0x40FFFFFF);
            g.fillCircle(x, y, s);
        }
    }

    void lock(Gfx g, float cx, float cy, float s) {
        g.color(0xCCB0A890);
        g.strokeCircle(cx, cy - s * 0.55f, s * 0.42f, s * 0.18f);
        g.fillRoundRect(cx - s * 0.6f, cy - s * 0.4f, s * 1.2f, s, s * 0.2f);
    }

    // ================================================================ HOME

    void home(Gfx g) {
        beginScroll(g, 0, H);
        float y = 0;
        // title with runes behind it
        g.color(0x16FFFFFF);
        g.text("ᚱᚢᚾᛟ", W / 2f, 98 * u, 96 * u, 0, Gfx.RUNE);
        g.color(0x66000000);
        g.text("RUNEN", W / 2f + 2 * u, 82 * u, 54 * u, 0, Gfx.TITLE);
        g.color(GOLD);
        g.text("RUNEN", W / 2f, 80 * u, 54 * u, 0, Gfx.TITLE);
        g.color(MUTED);
        g.text("Leer het schrift van de vikingen", W / 2f, 108 * u, 15 * u, 0, Gfx.SANS);

        // chips
        long today = host.today();
        int st = p.currentStreak(today);
        float cy = 126 * u, chH = 36 * u, chW = 172 * u;
        chip(g, X(20), cy, chW, chH, 0, st == 1 ? "1 dag op rij" : st + " dagen op rij");
        chip(g, X(208), cy, chW, chH, 1, p.xp + " punten");

        tabs(g, X(20), 178 * u, 360 * u, 46 * u);

        // progress card
        float py = 238 * u;
        stone(g, X(20), py, 360 * u, 150 * u, 16 * u);
        float frac = p.score(track);
        ring(g, X(78), py + 75 * u, 40 * u, frac, 9 * u);
        g.color(BONE);
        g.text(Math.round(frac * 100) + "%", X(78), py + 82 * u, 20 * u, 0, Gfx.BOLD);
        g.color(BONE);
        g.text(track.title, X(136), py + 34 * u, 20 * u, -1, Gfx.TITLE);
        g.color(MUTED);
        g.text(track.subtitle, X(136), py + 54 * u, 13 * u, -1, Gfx.SANS);
        g.color(BONE);
        g.text(p.unlocked(track) + " van " + track.size() + " runen ontgrendeld", X(136), py + 80 * u, 14 * u, -1, Gfx.SANS);
        g.text(p.mastered(track) + " runen beheerst", X(136), py + 100 * u, 14 * u, -1, Gfx.SANS);
        // mini rune strip
        int n = track.size();
        float sx = X(136), sw = 230 * u, step = Math.min(18 * u, sw / (n / 2f + 0.5f));
        for (int i = 0; i < n; i++) {
            Rune r = track.runes.get(i);
            int row = i < (n + 1) / 2 ? 0 : 1, col = row == 0 ? i : i - (n + 1) / 2;
            float rx = sx + col * step + step / 2, ry = py + 118 * u + row * 18 * u;
            int c = !p.isUnlocked(r) ? 0x40FFFFFF : BOX_COL[p.box(r)];
            g.color(c);
            g.text(r.glyph, rx, ry + 5 * u, 15 * u, 0, Gfx.RUNE);
        }

        // practise button
        float by = 404 * u;
        button(g, X(20), by, 360 * u, 78 * u, null, PRIMARY, new Runnable() { public void run() { startQuiz(); } });
        g.color(BONE);
        g.text("Oefenen", X(200), by + 38 * u, 26 * u, 0, Gfx.TITLE);
        g.color(0xDDEDE3CF);
        g.text(Quiz.LENGTH + " vragen · nieuwe runen komen vanzelf", X(200), by + 61 * u, 13 * u, 0, Gfx.SANS);

        // read real inscriptions
        float sy0 = 496 * u;
        button(g, X(20), sy0, 360 * u, 84 * u, null, STONEBTN, new Runnable() { public void run() { go(STONES); } });
        runes(g, "ᛋᛏᛅᛁᚾ", X(36), runeBase(sy0 + 42 * u, 30 * u), 30 * u, -1, RED);
        g.color(BONE);
        g.text("Runenstenen lezen", X(150), sy0 + 36 * u, 17 * u, -1, Gfx.BOLD);
        g.color(MUTED);
        g.text(p.stonesRead() + " van " + Stones.ALL.size() + " inscripties gelezen", X(150), sy0 + 58 * u, 12.5f * u, -1, Gfx.SANS);

        // tiles
        String[][] tiles = {{"Runenkaarten", "uitleg en voorbeelden", "ᚱ"}, {"Schrijf je naam", "van letters naar runen", "ᚹ"},
                {"Voortgang", "wat kun je al?", "ᛏ"}, {"Over runen", "geschiedenis en uitleg", "ᛟ"}};
        final int[] dest = {CARDS, WRITE, PROGRESS, INFO};
        for (int i = 0; i < 4; i++) {
            float tx = X(20 + (i % 2) * 186), ty = 594 * u + (i / 2) * 136 * u;
            final int d = dest[i];
            button(g, tx, ty, 174 * u, 124 * u, null, STONEBTN, new Runnable() { public void run() { if (d == DETAIL) detail = 0; go(d); } });
            runes(g, tiles[i][2], tx + 30 * u, ty + 50 * u, 44 * u, 0, RED);
            g.color(BONE);
            g.text(tiles[i][0], tx + 14 * u, ty + 88 * u, 16 * u, -1, Gfx.BOLD);
            g.color(MUTED);
            g.text(tiles[i][1], tx + 14 * u, ty + 108 * u, 12 * u, -1, Gfx.SANS);
        }
        g.color(FAINT);
        g.text("ᚠᚢᚦᚨᚱᚲ ᚷᚹ ᚺᚾᛁᛃᛇᛈᛉᛊ ᛏᛒᛖᛗᛚᛜᛞᛟ", W / 2f, 894 * u, 16 * u, 0, Gfx.RUNE);
        endScroll(g, 918 * u);
    }

    void chip(Gfx g, float x, float y, float w, float h, int icon, String s) {
        g.color(0x30000000);
        g.fillRoundRect(x, y, w, h, h / 2);
        float ix = x + h / 2 + 2 * u, iy = y + h / 2;
        if (icon == 0) { // flame
            g.linear(0, iy - 10 * u, 0, iy + 9 * u, 0xFFFFD25A, 0xFFE0502A);
            g.fillPoly(new float[]{ix, iy - 11 * u, ix + 7 * u, iy + 1 * u, ix + 5 * u, iy + 8 * u, ix, iy + 10 * u, ix - 5 * u, iy + 8 * u, ix - 7 * u, iy + 1 * u, ix - 2 * u, iy - 4 * u});
        } else { // coin with a rune
            g.linear(0, iy - 9 * u, 0, iy + 9 * u, 0xFFF3C969, 0xFFB98423);
            g.fillCircle(ix, iy, 9 * u);
            g.color(0xFF6B4A12);
            g.text("ᚠ", ix, iy + 4.5f * u, 13 * u, 0, Gfx.RUNE);
        }
        g.color(BONE);
        g.text(s, x + h + 6 * u, y + h / 2 + 5 * u, 14 * u, -1, Gfx.BOLD);
    }

    // ================================================================ CARDS

    void cards(Gfx g) {
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 14 * u;
        float tw = 82.5f * u, th = 104 * u, gap = 10 * u;
        for (int a = 0; a < track.aettNames.length; a++) {
            g.color(GOLD);
            g.text(track.aettNames[a], X(20), y + 22 * u, 19 * u, -1, Gfx.TITLE);
            g.color(0x44E2AE4C);
            g.fillRect(X(20) + g.textWidth(track.aettNames[a], 19 * u, Gfx.TITLE) + 10 * u, y + 15 * u, 200 * u, 1.2f * u);
            y += 34 * u;
            int start = track.aettStart[a], end = a + 1 < track.aettStart.length ? track.aettStart[a + 1] : track.size();
            for (int i = start; i < end; i++) {
                int k = i - start;
                float tx = X(20) + (k % 4) * (tw + gap), ty = y + (k / 4) * (th + gap);
                final Rune r = track.runes.get(i);
                boolean unlocked = p.isUnlocked(r);
                button(g, tx, ty, tw, th, null, STONEBTN, new Runnable() { public void run() { detail = r.index; go(DETAIL); } });
                runes(g, r.glyph, tx + tw / 2, runeBase(ty + 40 * u, 46 * u), 46 * u, 0, unlocked ? RED : 0xFF7A5148);
                g.color(unlocked ? BONE : MUTED);
                g.text(r.sound.split(" ")[0].replace(",", ""), tx + tw / 2, ty + 82 * u, 15 * u, 0, Gfx.BOLD);
                if (unlocked) pips(g, tx + tw / 2, ty + 94 * u, p.box(r), 2.4f * u);
                else lock(g, tx + tw - 13 * u, ty + 16 * u, 9 * u);
            }
            y += ((end - start + 3) / 4) * (th + gap) + 14 * u;
        }
        g.color(MUTED);
        y += para(g, "Tik op een rune voor uitleg en voorbeelden. Runen met een slotje komen vanzelf aan bod in de oefenrondes; je kunt ze hier alvast bekijken.",
                X(20), y, 13 * u, Gfx.SANS, MUTED, 360 * u);
        endScroll(g, y + 30 * u);
        header(g, "Runenkaarten", track.title);
    }

    // ================================================================ DETAIL

    void detail(Gfx g) {
        final Rune r = track.runes.get(detail);
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 16 * u;
        // runestone slab
        float sx = X(110), sw = 180 * u, sh = 210 * u;
        g.color(0x66000000);
        g.fillRoundRect(sx + 3 * u, y + 5 * u, sw, sh, 70 * u);
        g.linear(sx, y, sx + sw, y + sh, 0xFF5A626C, 0xFF2C3238);
        g.fillRoundRect(sx, y, sw, sh, 70 * u);
        g.color(0x30FFFFFF);
        g.strokeRoundRect(sx + 2 * u, y + 2 * u, sw - 4 * u, sh - 4 * u, 68 * u, 1.5f * u);
        runes(g, r.glyph, X(200), runeBase(y + sh * 0.5f, 150 * u), 150 * u, 0, RED);
        y += sh + 34 * u;
        g.color(BONE);
        g.text(r.name, X(200), y, 32 * u, 0, Gfx.TITLE);
        speaker(g, X(200) + g.textWidth(r.name, 32 * u, Gfx.TITLE) / 2 + 26 * u, y - 11 * u, r.audio());
        y += 18 * u;
        // sound + meaning
        float chw = 170 * u;
        stone(g, X(20), y, chw, 58 * u, 12 * u);
        stone(g, X(210), y, chw, 58 * u, 12 * u);
        g.color(MUTED);
        g.text("klank", X(34), y + 20 * u, 12 * u, -1, Gfx.SANS);
        g.text("betekenis", X(224), y + 20 * u, 12 * u, -1, Gfx.SANS);
        g.color(BONE);
        g.text(r.sound, X(34), y + 44 * u, 19 * u, -1, Gfx.BOLD);
        List<String> ml = wrap(g, r.meaning, 15 * u, Gfx.BOLD, 145 * u);
        if (ml.size() == 1) g.text(r.meaning, X(224), y + 44 * u, 15 * u, -1, Gfx.BOLD);
        else { g.text(ml.get(0), X(224), y + 38 * u, 13 * u, -1, Gfx.BOLD); g.text(ml.get(1), X(224), y + 52 * u, 13 * u, -1, Gfx.BOLD); }
        y += 74 * u;

        // memory aid
        g.color(GOLD);
        g.text("Ezelsbruggetje", X(20), y + 16 * u, 17 * u, -1, Gfx.TITLE);
        y += 26 * u;
        y += para(g, r.memo, X(20), y, 15 * u, Gfx.SANS, BONE, 360 * u) + 14 * u;

        // examples
        g.color(GOLD);
        g.text(r.examples.size() == 1 ? "Voorbeeld" : "Voorbeelden", X(20), y + 16 * u, 17 * u, -1, Gfx.TITLE);
        y += 28 * u;
        for (Example e : r.examples) {
            List<String> nl = wrap(g, e.note, 13 * u, Gfx.SANS, 330 * u);
            float eh = 96 * u + nl.size() * 18 * u + (e.authentic ? 0 : 0);
            stone(g, X(20), y, 360 * u, eh, 14 * u);
            float rs = Math.min(40 * u, 330 * u / Math.max(1, e.runes.length() * 0.62f));
            runes(g, e.runes, X(35), runeBase(y + 32 * u, rs), rs, -1, RED);
            g.color(BONE);
            g.text(e.latin, X(35), y + 78 * u, 18 * u, -1, Gfx.BOLD);
            if (e.authentic) {
                float bw = g.textWidth("echte runensteen", 11 * u, Gfx.BOLD) + 16 * u;
                g.color(0x40E2AE4C);
                g.fillRoundRect(X(365) - bw, y + 12 * u, bw, 22 * u, 11 * u);
                g.color(GOLD);
                g.text("echte runensteen", X(365) - bw / 2, y + 27 * u, 11 * u, 0, Gfx.BOLD);
            }
            g.color(MUTED);
            for (int i = 0; i < nl.size(); i++) g.text(nl.get(i), X(35), y + 100 * u + i * 18 * u, 13 * u, -1, Gfx.SANS);
            y += eh + 12 * u;
        }

        // status
        y += 6 * u;
        String status;
        if (!p.isUnlocked(r)) status = "Deze rune is nog niet ontgrendeld. Hij komt vanzelf aan bod als je de vorige runen goed kent.";
        else if (!p.seen(r)) status = "Ontgrendeld, maar nog niet geoefend.";
        else status = "Geoefend: " + p.rightOf(r) + " keer goed, " + p.wrongOf(r) + " keer fout.";
        stone(g, X(20), y, 360 * u, 74 * u, 12 * u);
        g.color(MUTED);
        g.text("Jouw kennis van deze rune", X(34), y + 22 * u, 12 * u, -1, Gfx.SANS);
        pips(g, X(338), y + 17 * u, p.box(r), 3.5f * u);
        List<String> sl = wrap(g, status, 13 * u, Gfx.SANS, 330 * u);
        g.color(BONE);
        for (int i = 0; i < sl.size() && i < 2; i++) g.text(sl.get(i), X(34), y + 44 * u + i * 18 * u, 13 * u, -1, Gfx.SANS);
        y += 90 * u;

        // prev / next
        final int n = track.size();
        button(g, X(20), y, 174 * u, 52 * u, "‹ Vorige", GHOST, new Runnable() { public void run() { detail = (detail + n - 1) % n; scroll[DETAIL] = 0; } });
        button(g, X(206), y, 174 * u, 52 * u, "Volgende ›", GHOST, new Runnable() { public void run() { detail = (detail + 1) % n; scroll[DETAIL] = 0; } });
        y += 70 * u;
        endScroll(g, y);
        header(g, r.glyph + "  " + r.name, track.title + " · rune " + (r.index + 1) + " van " + n);
    }

    // ================================================================ QUIZ

    void quiz(Gfx g) {
        final Quiz q = quiz;
        if (q == null || q.done()) { finishQuiz(); summary(g); return; }
        final Quiz.Step s = q.current();
        // header with close and progress
        float hh = 66 * u;
        g.color(0xF0161B21); g.fillRect(0, 0, W, hh);
        float bx = X(10), by = 13 * u, bs = 40 * u;
        g.color(isPressed(bx, by, bs, bs) ? 0x33FFFFFF : 0x14FFFFFF);
        g.fillCircle(bx + bs / 2, by + bs / 2, bs / 2);
        g.color(BONE);
        float cx = bx + bs / 2, cy = by + bs / 2, k = 7 * u;
        g.line(cx - k, cy - k, cx + k, cy + k, 3 * u);
        g.line(cx + k, cy - k, cx - k, cy + k, 3 * u);
        hit(bx - 6 * u, by - 8 * u, bs + 12 * u, bs + 16 * u, new Runnable() { public void run() { back(); } });
        float pw = 270 * u, px = X(64);
        g.color(0x40000000); g.fillRoundRect(px, cy - 6 * u, pw - 40 * u, 12 * u, 6 * u);
        float done = (q.questionNumber() - (s.answered ? 0 : 1)) / (float) q.questions();
        if (done > 0) { g.linear(px, 0, px + pw, 0, 0xFFB98423, GOLD); g.fillRoundRect(px, cy - 6 * u, Math.max(12 * u, (pw - 40 * u) * done), 12 * u, 6 * u); }
        g.color(BONE);
        g.text(q.right + " goed", X(386), cy + 6 * u, 14 * u, 1, Gfx.BOLD);

        float y = hh + 22 * u;
        if (s.type == Quiz.INTRO) { intro(g, q, s, y); return; }

        String prompt;
        switch (s.type) {
            case Quiz.SOUND: prompt = "Welke klank hoort bij deze rune?"; break;
            case Quiz.RUNE: prompt = "Welke rune is de " + s.rune.sound.split(",")[0].split(" ")[0] + "?"; break;
            case Quiz.READ: prompt = "Lees deze naam"; break;
            default: prompt = "Schrijf deze naam in runen"; break;
        }
        g.color(MUTED);
        g.text("Vraag " + q.questionNumber() + " van " + q.questions(), W / 2f, y, 13 * u, 0, Gfx.SANS);
        g.color(BONE);
        g.text(prompt, W / 2f, y + 28 * u, 20 * u, 0, Gfx.BOLD);
        y += 46 * u;

        // stimulus
        float sh = 170 * u;
        stone(g, X(40), y, 320 * u, sh, 20 * u);
        if (s.type == Quiz.SOUND) {
            runes(g, s.rune.glyph, W / 2f, runeBase(y + sh / 2, 120 * u), 120 * u, 0, RED);
        } else if (s.type == Quiz.RUNE) {
            g.color(BONE);
            String snd = s.rune.sound.split(",")[0].split(" ")[0];
            g.text(snd, W / 2f, y + sh / 2 + 24 * u, 68 * u, 0, Gfx.BOLD);
        } else if (s.type == Quiz.READ) {
            float rs = Math.min(72 * u, 290 * u / Math.max(1, s.example.runes.length() * 0.6f));
            runes(g, s.example.runes, W / 2f, runeBase(y + sh / 2, rs), rs, 0, RED);
        } else {
            g.color(BONE);
            g.text(Quiz.shortName(s.example), W / 2f, y + 62 * u, 34 * u, 0, Gfx.TITLE);
            // slot with typed runes
            g.color(0x50000000);
            g.fillRoundRect(X(60), y + 88 * u, 280 * u, 62 * u, 12 * u);
            String t = s.typed.toString();
            float rs = Math.min(44 * u, 260 * u / Math.max(1, t.length() * 0.62f));
            if (t.length() > 0) runes(g, t, W / 2f, runeBase(y + 119 * u, rs), rs, 0, s.answered ? (s.correct ? GREEN : BAD) : RED);
            else { g.color(FAINT); g.text("tik de runen hieronder", W / 2f, y + 124 * u, 14 * u, 0, Gfx.SANS); }
            if (!s.answered && t.length() > 0 && ((int) (time * 2) & 1) == 0) {
                float tw = t.length() > 0 ? g.textWidth(t, rs, Gfx.RUNE) : 0;
                g.color(GOLD);
                g.fillRect(W / 2f + tw / 2 + 3 * u, y + 100 * u, 2.5f * u, 38 * u);
            }
        }
        y += sh + 22 * u;

        if (s.type == Quiz.WRITE) writeKeyboard(g, q, s, y);
        else {
            float ow = 174 * u, oh = 82 * u;
            for (int i = 0; i < s.options.length; i++) {
                final int idx = i;
                float tx = X(20 + (i % 2) * 186), ty = y + (i / 2) * (oh + 12 * u);
                boolean pr = isPressed(tx, ty, ow, oh) && !s.answered;
                int fill0 = STONE0, fill1 = STONE1;
                if (s.answered && i == s.answer) { fill0 = 0xFF4F8F55; fill1 = 0xFF2F6237; }
                else if (s.answered && i == s.chosen) { fill0 = 0xFFA4473A; fill1 = 0xFF6E2A20; }
                g.color(0x55000000); g.fillRoundRect(tx + 1.5f * u, ty + 3 * u, ow, oh, 14 * u);
                g.linear(0, ty, 0, ty + oh, fill0, fill1);
                g.fillRoundRect(tx, ty, ow, oh, 14 * u);
                if (pr) { g.color(0x33000000); g.fillRoundRect(tx, ty, ow, oh, 14 * u); }
                String o = s.options[i];
                if (s.type == Quiz.RUNE) runes(g, o, tx + ow / 2, runeBase(ty + oh / 2, 54 * u), 54 * u, 0, s.answered && i == s.answer ? BONE : RED);
                else {
                    float fs = Math.min(26 * u, 160 * u / Math.max(1, g.textWidth(o, 1, Gfx.BOLD)));
                    g.color(BONE);
                    g.text(o, tx + ow / 2, ty + oh / 2 + fs * 0.36f, fs, 0, Gfx.BOLD);
                }
                if (!s.answered) hit(tx, ty, ow, oh, new Runnable() { public void run() { q.choose(s, idx, p); host.buzz(s.correct ? 15 : 40); } });
            }
        }
        if (s.answered) feedback(g, q, s);
    }

    void writeKeyboard(Gfx g, final Quiz q, final Quiz.Step s, float y) {
        int n = p.unlocked(track);
        int cols = 6;
        float kw = (360 * u - (cols - 1) * 8 * u) / cols, kh = 50 * u;
        for (int i = 0; i < n; i++) {
            final Rune r = track.runes.get(i);
            float tx = X(20) + (i % cols) * (kw + 8 * u), ty = y + (i / cols) * (kh + 8 * u);
            button(g, tx, ty, kw, kh, null, STONEBTN, s.answered ? null : new Runnable() { public void run() { if (s.typed.length() < 14) s.typed.append(r.glyph); } });
            runes(g, r.glyph, tx + kw / 2, runeBase(ty + kh / 2, 30 * u), 30 * u, 0, RED);
        }
        float ay = y + ((n + cols - 1) / cols) * (kh + 8 * u) + 4 * u;
        if (!s.answered) {
            button(g, X(20), ay, 110 * u, 54 * u, "Wis", GHOST, new Runnable() { public void run() { if (s.typed.length() > 0) s.typed.setLength(s.typed.length() - 1); } });
            button(g, X(142), ay, 238 * u, 54 * u, "Controleer", GOLDBTN, new Runnable() { public void run() { q.checkWrite(s, p); host.buzz(s.correct ? 15 : 40); } });
        }
    }

    void feedback(Gfx g, final Quiz q, Quiz.Step s) {
        float fh = 168 * u, fy = H - fh;
        g.color(0x88000000);
        g.fillRect(0, fy - 6 * u, W, 6 * u);
        g.linear(0, fy, 0, H, s.correct ? 0xFF22402A : 0xFF4A231D, s.correct ? 0xFF14261A : 0xFF2A1410);
        g.fillRect(0, fy, W, fh);
        g.color(s.correct ? GREEN : BAD);
        g.text(s.correct ? "Goed zo!" : "Helaas", X(24), fy + 34 * u, 24 * u, -1, Gfx.TITLE);
        String expl;
        Rune r = s.rune;
        if (s.type == Quiz.READ || s.type == Quiz.WRITE) expl = s.example.runes + "  =  " + Quiz.shortName(s.example);
        else expl = r.glyph + "  =  " + r.name + ", klank " + r.sound;
        float rs = 22 * u;
        g.color(BONE);
        if (s.type == Quiz.READ || s.type == Quiz.WRITE) {
            runes(g, s.example.runes, X(24), fy + 68 * u, rs, -1, RED);
            float w = g.textWidth(s.example.runes, rs, Gfx.RUNE);
            g.color(BONE);
            g.text("=  " + Quiz.shortName(s.example), X(24) + w + 10 * u, fy + 67 * u, 17 * u, -1, Gfx.BOLD);
            g.color(MUTED);
            g.text(s.example.note.length() > 52 ? s.example.note.substring(0, 50) + "…" : s.example.note, X(24), fy + 90 * u, 13 * u, -1, Gfx.SANS);
        } else {
            runes(g, r.glyph, X(24), fy + 70 * u, 28 * u, -1, RED);
            g.color(BONE);
            g.text("=  " + r.name + " · klank " + r.sound, X(54), fy + 66 * u, 17 * u, -1, Gfx.BOLD);
            g.color(MUTED);
            g.text("betekenis: " + r.meaning, X(24), fy + 90 * u, 13 * u, -1, Gfx.SANS);
        }
        button(g, X(20), fy + 104 * u, 360 * u, 54 * u, q.pos + 1 >= q.steps.size() ? "Afronden" : "Verder", GOLDBTN, new Runnable() {
            public void run() { q.next(); if (q.done()) finishQuiz(); }
        });
    }

    void intro(Gfx g, final Quiz q, Quiz.Step s, float y) {
        Rune r = s.rune;
        g.color(GOLD);
        g.text("Nieuwe rune!", W / 2f, y + 10 * u, 26 * u, 0, Gfx.TITLE);
        y += 26 * u;
        float sh = 150 * u;
        stone(g, X(110), y, 180 * u, sh, 40 * u);
        runes(g, r.glyph, W / 2f, runeBase(y + sh / 2, 110 * u), 110 * u, 0, RED);
        y += sh + 34 * u;
        g.color(BONE);
        g.text(r.name + "  ·  " + r.sound, W / 2f, y, 26 * u, 0, Gfx.TITLE);
        speaker(g, W / 2f + g.textWidth(r.name + "  ·  " + r.sound, 26 * u, Gfx.TITLE) / 2 + 24 * u, y - 9 * u, r.audio());
        g.color(MUTED);
        g.text("betekenis: " + r.meaning, W / 2f, y + 26 * u, 14 * u, 0, Gfx.SANS);
        y += 44 * u;
        y += para(g, r.memo, X(30), y, 15 * u, Gfx.SANS, BONE, 340 * u) + 12 * u;
        if (!r.examples.isEmpty()) {
            Example e = r.examples.get(0);
            stone(g, X(20), y, 360 * u, 84 * u, 14 * u);
            float rs = Math.min(36 * u, 200 * u / Math.max(1, e.runes.length() * 0.62f));
            runes(g, e.runes, X(34), runeBase(y + 30 * u, rs), rs, -1, RED);
            g.color(BONE);
            g.text(e.latin, X(366), y + 36 * u, 17 * u, 1, Gfx.BOLD);
            g.color(MUTED);
            String note = e.note.length() > 50 ? e.note.substring(0, 48) + "…" : e.note;
            g.text(note, X(34), y + 68 * u, 12.5f * u, -1, Gfx.SANS);
            y += 96 * u;
        }
        float by = Math.max(y + 8 * u, H - 80 * u);
        button(g, X(20), Math.min(by, H - 70 * u), 360 * u, 58 * u, "Begrepen", GOLDBTN, new Runnable() { public void run() { q.next(); } });
    }

    // ================================================================ SUMMARY

    void summary(Gfx g) {
        final Quiz q = quiz;
        beginScroll(g, 0, H);
        float y = 70 * u;
        g.color(GOLD);
        g.text("Ronde klaar!", W / 2f, y, 36 * u, 0, Gfx.TITLE);
        y += 30 * u;
        int qs = q == null ? 0 : q.questions(), ok = q == null ? 0 : q.right;
        stone(g, X(40), y, 320 * u, 170 * u, 20 * u);
        ring(g, W / 2f, y + 85 * u, 56 * u, qs == 0 ? 0 : ok / (float) qs, 11 * u);
        g.color(BONE);
        g.text(ok + "/" + qs, W / 2f, y + 96 * u, 32 * u, 0, Gfx.TITLE);
        y += 196 * u;
        g.color(BONE);
        g.text("+" + ok * 10 + " punten", W / 2f, y, 20 * u, 0, Gfx.BOLD);
        y += 26 * u;
        g.color(MUTED);
        int st = p.currentStreak(host.today());
        g.text(st <= 1 ? "Kom morgen terug voor een reeks!" : st + " dagen op rij geoefend", W / 2f, y, 14 * u, 0, Gfx.SANS);
        y += 30 * u;
        String verdict = qs == 0 ? "" : ok == qs ? "Foutloos! Odin is trots op je." : ok >= qs * 0.7 ? "Mooi werk, je runenkennis groeit." : "Oefening baart kunst. De zwakke runen komen vaker terug.";
        y += para(g, verdict, X(40), y - 14 * u, 15 * u, Gfx.SANS, BONE, 320 * u);
        y += 16 * u;
        if (!fresh.isEmpty()) {
            float bh = 128 * u;
            stone(g, X(20), y, 360 * u, bh, 16 * u);
            g.color(GOLD);
            g.text(fresh.size() == 1 ? "Nieuwe rune ontgrendeld!" : "Nieuwe runen ontgrendeld!", W / 2f, y + 30 * u, 18 * u, 0, Gfx.TITLE);
            float step = 70 * u, sx = W / 2f - (fresh.size() - 1) * step / 2;
            for (int i = 0; i < fresh.size(); i++) {
                Rune r = fresh.get(i);
                runes(g, r.glyph, sx + i * step, runeBase(y + 72 * u, 48 * u), 48 * u, 0, RED);
                g.color(BONE);
                g.text(r.sound.split(",")[0], sx + i * step, y + 116 * u, 13 * u, 0, Gfx.BOLD);
            }
            y += bh + 16 * u;
        } else if (p.unlocked(track) < track.size()) {
            y += para(g, "Ken je alle runen tot nu toe minstens redelijk (2 bolletjes), dan komen er nieuwe bij.", X(40), y, 13 * u, Gfx.SANS, MUTED, 320 * u) + 12 * u;
        }
        button(g, X(20), y, 360 * u, 64 * u, "Nog een ronde", PRIMARY, new Runnable() { public void run() { startQuiz(); } });
        y += 76 * u;
        if (!fresh.isEmpty()) {
            final Rune first = fresh.get(0);
            button(g, X(20), y, 360 * u, 54 * u, "Bekijk de nieuwe runen", STONEBTN, new Runnable() { public void run() { detail = first.index; screen = DETAIL; scroll[DETAIL] = 0; stack.clear(); stack.add(HOME); stack.add(CARDS); } });
            y += 66 * u;
        }
        button(g, X(20), y, 360 * u, 54 * u, "Naar het begin", GHOST, new Runnable() { public void run() { home(); } });
        y += 70 * u;
        endScroll(g, y);
    }

    // ================================================================ WRITE (transliterator)

    static final String[] KEYS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};

    void write(Gfx g) {
        float top = 66 * u;
        // keyboard height
        float kh = 46 * u, kg = 7 * u;
        float kbH = 4 * kh + 5 * kg + 6 * u;
        float kbY = H - kbH;
        beginScroll(g, top, kbY);
        float y = 14 * u;
        tabs(g, X(20), y, 360 * u, 42 * u);
        y += 56 * u;
        if (track.younger) {
            // switch for the dotted runes
            float sh0 = 58 * u;
            boolean on = p.dotted;
            button(g, X(20), y, 360 * u, sh0, null, STONEBTN, new Runnable() { public void run() { p.dotted = !p.dotted; p.save(); } });
            g.color(BONE);
            g.text("Gestippelde runen", X(34), y + 24 * u, 16 * u, -1, Gfx.BOLD);
            g.color(MUTED);
            g.text("e ᛂ  g ᚵ  d ᛑ  p ᛔ  y ᚤ  · vanaf ca. 1000", X(34), y + 45 * u, 13 * u, -1, Gfx.SANS);
            float tx = X(326), ty = y + sh0 / 2;
            g.color(on ? 0xFFB98423 : 0x50000000);
            g.fillRoundRect(tx - 22 * u, ty - 13 * u, 44 * u, 26 * u, 13 * u);
            g.color(on ? BONE : MUTED);
            g.fillCircle(on ? tx + 9 * u : tx - 9 * u, ty, 10 * u);
            y += sh0 + 14 * u;
        }
        // input box
        g.color(0x40000000);
        g.fillRoundRect(X(20), y, 360 * u, 54 * u, 12 * u);
        g.color(0x55E2AE4C);
        g.strokeRoundRect(X(20), y, 360 * u, 54 * u, 12 * u, 1.4f * u);
        String shown = typed;
        float fs = 22 * u;
        while (g.textWidth(shown, fs, Gfx.BOLD) > 330 * u && shown.length() > 1) shown = shown.substring(1);
        if (typed.isEmpty()) { g.color(FAINT); g.text("Typ je naam...", X(34), y + 35 * u, 18 * u, -1, Gfx.SANS); }
        else { g.color(BONE); g.text(shown, X(34), y + 35 * u, fs, -1, Gfx.BOLD); }
        if (((int) (time * 2) & 1) == 0) {
            float cx = X(34) + (typed.isEmpty() ? 0 : g.textWidth(shown, fs, Gfx.BOLD)) + 2 * u;
            g.color(GOLD); g.fillRect(cx, y + 14 * u, 2.5f * u, 26 * u);
        }
        y += 70 * u;

        String rr = Translit.toRunes(typed, track.mode, p.dotted);
        // runes on a stone, wrapped per word
        List<String> lines = new ArrayList<>();
        float rs = 46 * u;
        if (!rr.isEmpty()) {
            String line = "";
            for (String w : rr.split(Translit.SEP)) {
                String t = line.isEmpty() ? w : line + Translit.SEP + w;
                if (g.textWidth(t, rs, Gfx.RUNE) > 320 * u && !line.isEmpty()) { lines.add(line); line = w; } else line = t;
            }
            lines.add(line);
            for (int i = 0; i < lines.size(); i++) {
                while (g.textWidth(lines.get(i), rs, Gfx.RUNE) > 320 * u && rs > 16 * u) rs *= 0.9f;
            }
        }
        float sh = Math.max(110 * u, 40 * u + lines.size() * rs * 1.25f);
        stone(g, X(20), y, 360 * u, sh, 16 * u);
        if (lines.isEmpty()) {
            g.color(MUTED);
            g.text("Hier verschijnt je naam in runen", W / 2f, y + sh / 2 + 5 * u, 15 * u, 0, Gfx.SANS);
        } else {
            float ly = y + (sh - lines.size() * rs * 1.25f) / 2 + rs * 0.62f;
            for (String l : lines) { runes(g, l, W / 2f, runeBase(ly, rs), rs, 0, RED); ly += rs * 1.25f; }
        }
        y += sh + 16 * u;

        // per-rune breakdown
        if (!rr.isEmpty()) {
            g.color(GOLD);
            g.text("Rune voor rune", X(20), y + 16 * u, 16 * u, -1, Gfx.TITLE);
            y += 28 * u;
            float tw = 52 * u, th = 66 * u, gap = 6 * u;
            int col = 0;
            for (int i = 0; i < rr.length(); i++) {
                char c = rr.charAt(i);
                if (c == Translit.SEP.charAt(0)) continue;
                float tx = X(20) + col * (tw + gap);
                stone(g, tx, y, tw, th, 10 * u);
                runes(g, String.valueOf(c), tx + tw / 2, runeBase(y + 26 * u, 30 * u), 30 * u, 0, RED);
                g.color(BONE);
                String snd = Runes.soundLabel(track, c);
                if (Translit.DOTTED_RUNES.indexOf(c) >= 0) { g.color(GOLD); }
                g.text(snd, tx + tw / 2, y + 58 * u, 12 * u, 0, Gfx.BOLD);
                col++;
                if (col == 6) { col = 0; y += th + gap; }
            }
            if (col > 0) y += th + gap;
            y += 8 * u;
        }
        y += para(g, track.mode == Translit.FUTHORC
                        ? "Zo zetten we om: th wordt ᚦ, ng wordt ᛝ, ea wordt ᛠ, eo wordt ᛇ, ae wordt ᚫ en eu wordt ᛟ. Een c, ch, k en q worden ᚳ, een x wordt ᛉ en een y wordt ᚣ. Dubbele letters schrijf je één keer."
                        : track.younger
                        ? (p.dotted ? "Met gestippelde runen krijgen e, g, d, p en y een eigen teken (goud gemarkeerd). Zo deden runenmakers het vanaf ongeveer het jaar 1000. Dubbele letters schrijf je één keer."
                                    : "De jongere futhark had maar 16 tekens: één rune staat vaak voor meerdere klanken, zoals ᛁ voor i, e en j, ᚢ voor u, o, v en w, en ᛏ voor t en d. Dubbele letters schrijf je één keer.")
                        : "Zo zetten we om: th wordt ᚦ, ng wordt ᛜ, ei en ij worden ᛇ, ie wordt ᛁ en oe wordt ᚢ. Een c, ch, k en q worden ᚲ, een v wordt ᚠ. Dubbele letters schrijf je één keer, zoals de runenmakers deden.",
                X(20), y, 13 * u, Gfx.SANS, MUTED, 360 * u);
        y += 20 * u;
        endScroll(g, y);

        // keyboard
        g.color(0xF0101418);
        g.fillRect(0, kbY, W, kbH);
        g.color(0x55E2AE4C);
        g.fillRect(0, kbY, W, 1.2f * u);
        float kw = (360 * u - 9 * 4 * u) / 10;
        for (int row = 0; row < 3; row++) {
            String keys = KEYS[row];
            float rowW = keys.length() * kw + (keys.length() - 1) * 4 * u;
            float sx = W / 2f - rowW / 2;
            float ky = kbY + kg + 6 * u + row * (kh + kg);
            for (int i = 0; i < keys.length(); i++) {
                final char c = keys.charAt(i);
                key(g, sx + i * (kw + 4 * u), ky, kw, kh, String.valueOf(c).toUpperCase(), new Runnable() { public void run() { if (typed.length() < 40) typed += c; } });
            }
        }
        float ky = kbY + kg + 6 * u + 3 * (kh + kg);
        key(g, X(20), ky, 80 * u, kh, "Wis", new Runnable() { public void run() { typed = ""; } });
        key(g, X(106), ky, 188 * u, kh, "spatie", new Runnable() { public void run() { if (typed.length() < 40 && !typed.endsWith(" ") && !typed.isEmpty()) typed += " "; } });
        key(g, X(300), ky, 80 * u, kh, "←", new Runnable() { public void run() { if (!typed.isEmpty()) typed = typed.substring(0, typed.length() - 1); } });

        header(g, "Schrijf je naam", "van letters naar runen");
    }

    void key(Gfx g, float x, float y, float w, float h, String label, Runnable a) {
        boolean pr = isPressed(x, y, w, h);
        g.color(0x66000000);
        g.fillRoundRect(x, y + 2 * u, w, h, 8 * u);
        g.linear(0, y, 0, y + h, pr ? 0xFF2E343B : 0xFF4A525C, pr ? 0xFF22272D : 0xFF353B43);
        g.fillRoundRect(x, y, w, h, 8 * u);
        g.color(BONE);
        float fs = label.length() > 1 ? 15 * u : 19 * u;
        g.text(label, x + w / 2, y + h / 2 + fs * 0.36f, fs, 0, Gfx.BOLD);
        hit(x, y, w, h, a);
    }

    // ================================================================ PROGRESS

    void progress(Gfx g) {
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 16 * u;
        long today = host.today();
        int total = p.right + p.wrong;
        String[][] stats = {
                {"" + p.xp, "punten"}, {"" + p.currentStreak(today), "dagen op rij"}, {"" + p.bestStreak, "langste reeks"},
                {"" + p.sessions, "rondes"}, {total == 0 ? "-" : Math.round(100f * p.right / total) + "%", "goed"}, {p.stonesRead() + "/" + Stones.ALL.size(), "stenen gelezen"}};
        float tw = 113 * u, th = 76 * u;
        for (int i = 0; i < stats.length; i++) {
            float tx = X(20) + (i % 3) * (tw + 10.5f * u), ty = y + (i / 3) * (th + 10 * u);
            stone(g, tx, ty, tw, th, 12 * u);
            g.color(GOLD);
            g.text(stats[i][0], tx + tw / 2, ty + 38 * u, 26 * u, 0, Gfx.TITLE);
            g.color(MUTED);
            g.text(stats[i][1], tx + tw / 2, ty + 60 * u, 12 * u, 0, Gfx.SANS);
        }
        y += 2 * (th + 10 * u) + 12 * u;

        for (final Track t : Runes.TRACKS) {
            int n = t.size();
            int cols = 8;
            float cs = (330 * u) / cols;
            float ch = 130 * u + ((n + cols - 1) / cols) * cs * 1.15f;
            stone(g, X(20), y, 360 * u, ch, 16 * u);
            g.color(BONE);
            g.text(t.title, X(36), y + 32 * u, 20 * u, -1, Gfx.TITLE);
            g.color(MUTED);
            g.text(p.mastered(t) + " beheerst · " + p.unlocked(t) + " ontgrendeld · " + n + " totaal", X(36), y + 54 * u, 13 * u, -1, Gfx.SANS);
            ring(g, X(346), y + 38 * u, 20 * u, p.score(t), 6 * u);
            float gy = y + 70 * u;
            for (int i = 0; i < n; i++) {
                Rune r = t.runes.get(i);
                float cx = X(35) + (i % cols) * cs + cs / 2, cy = gy + (i / cols) * cs * 1.15f + cs / 2;
                int b = p.box(r);
                boolean un = p.isUnlocked(r);
                g.color(un ? (BOX_COL[b] & 0x00FFFFFF) | 0x55000000 : 0x18FFFFFF);
                g.fillRoundRect(cx - cs * 0.42f, cy - cs * 0.48f, cs * 0.84f, cs * 0.96f, 6 * u);
                g.color(un ? BONE : 0x55FFFFFF);
                g.text(r.glyph, cx, runeBase(cy, 24 * u), 24 * u, 0, Gfx.RUNE);
            }
            float by = gy + ((n + cols - 1) / cols) * cs * 1.15f + 8 * u;
            if (p.unlocked(t) < n) {
                button(g, X(36), by, 328 * u, 40 * u, "Alle runen vrijgeven", GHOST, new Runnable() { public void run() { p.unlockAll(t); p.save(); } });
            } else {
                g.color(MUTED);
                g.text("Alle runen zijn vrijgegeven.", X(200), by + 25 * u, 13 * u, 0, Gfx.SANS);
            }
            y += ch + 16 * u;
        }

        // legend
        String[] lab = {"nieuw", "zwak", "matig", "redelijk", "goed", "beheerst"};
        float lx = X(20);
        for (int i = 0; i < 6; i++) {
            float w = 20 * u + g.textWidth(lab[i], 12 * u, Gfx.SANS);
            if (lx + w > X(380)) { lx = X(20); y += 24 * u; }
            g.color(BOX_COL[i]);
            g.fillCircle(lx + 6 * u, y + 8 * u, 5 * u);
            g.color(MUTED);
            g.text(lab[i], lx + 15 * u, y + 13 * u, 12 * u, -1, Gfx.SANS);
            lx += w + 12 * u;
        }
        y += 36 * u;
        y += para(g, "Elke rune zit in een van vijf vakjes. Een goed antwoord schuift hem een vakje op, een fout antwoord een vakje terug. Runen in de lage vakjes komen vaker terug in de oefenrondes.",
                X(20), y, 13 * u, Gfx.SANS, MUTED, 360 * u) + 16 * u;
        button(g, X(20), y, 360 * u, 50 * u, "Voortgang wissen", GHOST, new Runnable() { public void run() { confirmReset = true; } });
        y += 70 * u;
        endScroll(g, y);
        header(g, "Voortgang", null);
    }

    void confirmDialog(Gfx g) {
        hitR.clear(); hitA.clear();
        g.color(0xCC000000);
        g.fillRect(0, 0, W, H);
        float dy = H / 2f - 110 * u;
        stone(g, X(30), dy, 340 * u, 220 * u, 20 * u);
        g.color(BONE);
        g.text("Alles wissen?", W / 2f, dy + 44 * u, 24 * u, 0, Gfx.TITLE);
        para(g, "Je punten, reeks en de kennis van alle runen gaan verloren.", X(50), dy + 60 * u, 14 * u, Gfx.SANS, MUTED, 300 * u);
        button(g, X(46), dy + 140 * u, 146 * u, 54 * u, "Annuleren", GHOST, new Runnable() { public void run() { confirmReset = false; } });
        button(g, X(208), dy + 140 * u, 146 * u, 54 * u, "Wissen", PRIMARY, new Runnable() { public void run() { p.reset(); confirmReset = false; } });
        // swallow taps elsewhere
        hitR.add(0, new float[]{0, 0, W, H, 0, H});
        hitA.add(0, new Runnable() { public void run() { } });
    }

    // ================================================================ audio button

    void speaker(Gfx g, float cx, float cy, final String name) {
        float r = 15 * u;
        boolean pr = isPressed(cx - r, cy - r, 2 * r, 2 * r);
        g.color(pr ? 0x55E2AE4C : 0x30E2AE4C);
        g.fillCircle(cx, cy, r);
        g.color(GOLD);
        float k = r * 0.42f;
        g.fillPoly(new float[]{cx - k * 1.5f, cy - k * 0.5f, cx - k * 0.7f, cy - k * 0.5f, cx + k * 0.2f, cy - k * 1.3f,
                cx + k * 0.2f, cy + k * 1.3f, cx - k * 0.7f, cy + k * 0.5f, cx - k * 1.5f, cy + k * 0.5f});
        g.arc(cx + k * 0.3f, cy, k * 0.9f, -45, 90, 1.6f * u);
        g.arc(cx + k * 0.3f, cy, k * 1.6f, -45, 90, 1.6f * u);
        hit(cx - r - 6 * u, cy - r - 6 * u, 2 * r + 12 * u, 2 * r + 12 * u, new Runnable() { public void run() { host.play(name); } });
    }

    // ================================================================ STONES (list of inscriptions)

    void stones(Gfx g) {
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 12 * u;
        y += para(g, "Lees echte runeninscripties, woord voor woord. Probeer eerst zelf te ontcijferen wat er staat en tik dan op een woord voor de oplossing.",
                X(20), y, 14 * u, Gfx.SANS, BONE, 360 * u) + 10 * u;
        for (Track t : Runes.TRACKS) {
            g.color(GOLD);
            g.text(t.title, X(20), y + 22 * u, 19 * u, -1, Gfx.TITLE);
            y += 34 * u;
            for (final Stones.Stone st : Stones.of(t)) {
                float ch = 92 * u;
                button(g, X(20), y, 360 * u, ch, null, STONEBTN, new Runnable() { public void run() { stone = st; go(STONE); } });
                String preview = st.words.get(0).runes;
                float rs = Math.min(28 * u, 90 * u / Math.max(1, preview.length() * 0.6f));
                runes(g, preview, X(34), runeBase(y + 46 * u, rs), rs, -1, RED);
                g.color(BONE);
                float tfs = Math.min(15.5f * u, 15.5f * u * 228 * u / Math.max(1, g.textWidth(st.title, 15.5f * u, Gfx.BOLD)));
                g.text(st.title, X(140), y + 32 * u, tfs, -1, Gfx.BOLD);
                g.color(MUTED);
                g.text(st.place.length() > 34 ? st.place.substring(0, 32) + "…" : st.place, X(140), y + 52 * u, 12 * u, -1, Gfx.SANS);
                int done = p.revealedCount(st), n = st.words.size();
                g.color(done == n ? GREEN : MUTED);
                g.text(st.date + "  ·  " + (done == n ? "gelezen" : done + " van " + n + " woorden"), X(140), y + 72 * u, 12 * u, -1, done == n ? Gfx.BOLD : Gfx.SANS);
                y += ch + 10 * u;
            }
            y += 8 * u;
        }
        endScroll(g, y + 20 * u);
        header(g, "Runenstenen", "echte inscripties lezen");
    }

    // ================================================================ STONE (read one inscription)

    void stone(Gfx g) {
        final Stones.Stone st = stone;
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 14 * u;
        y += para(g, st.intro, X(20), y, 14 * u, Gfx.SANS, BONE, 360 * u) + 12 * u;

        // the inscription itself, carved on a stone
        List<String> lines = new ArrayList<>();
        float rs = 30 * u;
        String line = "";
        for (Stones.Word w : st.words) {
            String t = line.isEmpty() ? w.runes : line + Translit.SEP + w.runes;
            if (g.textWidth(t, rs, Gfx.RUNE) > 320 * u && !line.isEmpty()) { lines.add(line); line = w.runes; } else line = t;
        }
        lines.add(line);
        float sh = 30 * u + lines.size() * rs * 1.3f;
        g.color(0x66000000);
        g.fillRoundRect(X(20) + 3 * u, y + 5 * u, 360 * u, sh, 24 * u);
        g.linear(X(20), y, X(380), y + sh, 0xFF5A626C, 0xFF2C3238);
        g.fillRoundRect(X(20), y, 360 * u, sh, 24 * u);
        float ly = y + 15 * u + rs * 0.65f;
        for (String l : lines) { runes(g, l, W / 2f, runeBase(ly, rs), rs, 0, RED); ly += rs * 1.3f; }
        y += sh + 14 * u;

        if (st.audio) {
            button(g, X(20), y, 174 * u, 46 * u, "Beluister", GHOST, new Runnable() { public void run() { host.play("s_" + st.id); } });
            button(g, X(206), y, 174 * u, 46 * u, "Toon alles", GHOST, new Runnable() { public void run() { p.revealAll(st); p.save(); } });
        } else {
            button(g, X(20), y, 360 * u, 46 * u, "Toon alles", GHOST, new Runnable() { public void run() { p.revealAll(st); p.save(); } });
        }
        y += 60 * u;

        // word by word
        for (int i = 0; i < st.words.size(); i++) {
            final Stones.Word w = st.words.get(i);
            final int idx = i;
            boolean open = p.revealed(st, i);
            float wrs = Math.min(28 * u, 130 * u / Math.max(1, w.runes.length() * 0.6f));
            List<String> gl = open ? wrap(g, w.gloss, 14 * u, Gfx.SANS, 150 * u) : new ArrayList<String>();
            float rh = open ? Math.max(70 * u, 52 * u + gl.size() * 18 * u + (w.norm.length() > 0 && !w.norm.equalsIgnoreCase(w.translit) ? 16 * u : 0)) : 58 * u;
            button(g, X(20), y, 360 * u, rh, null, STONEBTN, open ? null : new Runnable() {
                public void run() { if (p.reveal(st, idx)) { p.save(); if (st.audio && w.pron.length() > 0) host.play("s_" + st.id + "_" + idx); } }
            });
            runes(g, w.runes, X(34), runeBase(y + Math.min(rh, 70 * u) / 2, wrs), wrs, -1, RED);
            if (!open) {
                g.color(FAINT);
                g.text("tik om te lezen", X(366), y + rh / 2 + 5 * u, 13 * u, 1, Gfx.SANS);
            } else {
                float tx = X(180);
                g.color(GOLD);
                g.text(w.translit, tx, y + 26 * u, 17 * u, -1, Gfx.BOLD);
                float gy = y + 46 * u;
                if (w.norm.length() > 0 && !w.norm.equalsIgnoreCase(w.translit)) {
                    g.color(MUTED);
                    g.text("= " + w.norm, tx, gy, 12.5f * u, -1, Gfx.SANS);
                    gy += 16 * u;
                }
                g.color(BONE);
                for (String gline : gl) { g.text(gline, tx, gy + 2 * u, 14 * u, -1, Gfx.SANS); gy += 18 * u; }
                if (st.audio && w.pron.length() > 0) speaker(g, X(352), y + 22 * u, "s_" + st.id + "_" + i);
            }
            y += rh + 8 * u;
        }

        // translation
        y += 8 * u;
        g.color(GOLD);
        g.text("Vertaling", X(20), y + 18 * u, 18 * u, -1, Gfx.TITLE);
        y += 30 * u;
        if (p.read(st)) {
            y += para(g, st.translation, X(20), y, 15 * u, Gfx.SANS, BONE, 360 * u) + 10 * u;
        } else {
            y += para(g, "Lees eerst alle woorden; dan verschijnt hier de vertaling van de hele tekst.", X(20), y, 13.5f * u, Gfx.SANS, MUTED, 360 * u) + 10 * u;
        }
        endScroll(g, y + 30 * u);
        header(g, st.title, st.place.length() > 36 ? st.date : st.place + " · " + st.date);
    }

    // ================================================================ INFO

    static final String[][] INFO_TEXT = {
            {"Wat zijn runen?", "Runen zijn de letters van de Germaanse volken. Ze werden gebruikt vanaf ongeveer 150 na Christus, tot ver in de middeleeuwen. Je kerfde ze in hout, been, metaal en steen. Daarom bestaan ze uit rechte lijnen en hebben ze bijna geen horizontale strepen: die zouden in de nerf van het hout verdwijnen."},
            {"Futhark", "Het runenalfabet heet de futhark, naar de eerste zes tekens: ᚠ ᚢ ᚦ ᚨ ᚱ ᚲ (f, u, th, a, r, k). De runen zijn verdeeld in drie groepen, de ættir. Elke groep is genoemd naar de eerste rune: Freys ætt, Hagals ætt en Tyrs ætt."},
            {"Oude futhark", "De oudste reeks heeft 24 runen en werd gebruikt van ongeveer 150 tot 750. De namen van deze runen kennen we niet uit de tijd zelf: ze zijn gereconstrueerd uit latere runengedichten. Daarom zie je soms andere schrijfwijzen, zoals kenaz in plaats van kaunan."},
            {"Jongere futhark: de vikingrunen", "In de vikingtijd (ongeveer 800 tot 1100) schreven de Scandinaviërs met maar 16 runen. Eén rune stond dan voor meerdere klanken: ᚢ voor u, o, v en w, ᛏ voor t en d, ᚴ voor k en g. De runenstenen van Jelling, waarop koning Harald Blauwtand trots vertelt dat hij Denemarken en Noorwegen won en de Denen christen maakte, zijn in deze runen geschreven. Deze app gebruikt de 'langtakrunen'; er bestond ook een snellere variant met korte takjes."},
            {"Gestippelde runen", "Met maar 16 runen was niet altijd duidelijk wat er stond: ᛁ kon i, e of j zijn. Rond het jaar 1000 begonnen runenmakers daarom puntjes in runen te zetten: ᛂ werd de e, ᚵ de g, ᛑ de d, ᛔ de p en ᚤ de y. In de middeleeuwen groeide dit uit tot een volledig runenalfabet. Bij 'Schrijf je naam' kun je deze runen aanzetten."},
            {"Runen in Nederland", "Ook in Nederland zijn runen gevonden. Het bekendst zijn de Friese runen, zoals op een taxusstaafje uit Westeremden in Groningen (8e eeuw). Bij Tiel vond men de schede van Bergakker, met een van de oudste runenteksten in een Frankische taal, uit de 5e eeuw."},
            {"Schrijfrichting", "Runen werden meestal van links naar rechts geschreven, maar ook van rechts naar links, of afwisselend heen en terug. Woorden werden vaak gescheiden door een of meer puntjes, zoals ᛫."},
            {"Hoe we omzetten", "De voorbeeldnamen in deze app zijn omgezet volgens vaste regels. Oude futhark: th wordt ᚦ, ng wordt ᛜ, ei en ij worden ᛇ, ie wordt ᛁ, oe wordt ᚢ, c/ch/k/q worden ᚲ en v wordt ᚠ. Jongere futhark: dezelfde regels, maar met de 16 vikingrunen. Dubbele letters schrijven we één keer. Voorbeelden met het label 'echte runensteen' staan precies zo op een historische steen."},
            {"Uitspraak", "Bij de runennamen en de inscripties hoor je een computerstem. Niemand weet precies hoe deze talen klonken: de uitspraak is een reconstructie, gebaseerd op wat taalkundigen ervan weten. De oude futhark klinkt als Oergermaans, de vikingrunen als Oudnoors en de futhorc als Oudengels."},
            {"Lettertypes", "Runen: Noto Sans Runic. Titels: Cinzel. Beide vrij te gebruiken onder de SIL Open Font License."},
    };

    void info(Gfx g) {
        float top = 66 * u;
        beginScroll(g, top, H);
        float y = 10 * u;
        for (String[] sec : INFO_TEXT) {
            g.color(GOLD);
            g.text(sec[0], X(20), y + 24 * u, 19 * u, -1, Gfx.TITLE);
            y += 34 * u;
            y += para(g, sec[1], X(20), y, 14.5f * u, Gfx.SANS, BONE, 360 * u) + 14 * u;
        }
        y += 20 * u;
        endScroll(g, y);
        header(g, "Over runen", null);
    }
}
