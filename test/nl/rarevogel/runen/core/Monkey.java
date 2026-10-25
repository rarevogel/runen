package nl.rarevogel.runen.core;
import java.awt.image.BufferedImage;
import java.util.*;
/** Random taps, drags and back presses across all screens to find crashes. */
public class Monkey {
    public static void main(String[] a) {
        Map<String,String> m = new HashMap<>();
        Progress.Store st = new Progress.Store() { public String get(String k){return m.get(k);} public void put(String k,String v){m.put(k,v);} public void clear(){m.clear();} };
        Progress p = new Progress(st);
        final long[] day = {20000};
        App app = new App(p, new App.Host() { public void quit(){} public long today(){return day[0];} public void buzz(int ms){} public void play(String n){} });
        int W = 720, H = 1520;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        J2dGfx g = new J2dGfx(g2);
        Random r = new Random(11);
        int[] seen = new int[10];
        int n = Integer.parseInt(a[0]);
        for (int i = 0; i < n; i++) {
            g2.setTransform(new java.awt.geom.AffineTransform()); g2.setClip(null);
            app.frame(g, W, H, 0.05f);
            seen[app.screen]++;
            int act = r.nextInt(100);
            // in a quiz, answer sensibly most of the time so progress moves
            if (app.screen == App.QUIZ && app.quiz != null && !app.quiz.done() && act < 60) {
                Quiz.Step s = app.quiz.current();
                if (s.type == Quiz.INTRO) app.quiz.next();
                else if (!s.answered) { if (s.type == Quiz.WRITE) { if (r.nextBoolean()) s.typed.append(s.target); app.quiz.checkWrite(s, p); } else app.quiz.choose(s, r.nextFloat() < 0.85f ? s.answer : r.nextInt(s.options.length), p); }
                else { app.quiz.next(); if (app.quiz.done()) app.finishQuiz(); }
                continue;
            }
            if (act < 80) { float x = r.nextFloat() * W, y = r.nextFloat() * H; app.down(x, y); app.up(x, y); }
            else if (act < 93) { float x = r.nextFloat() * W, y = r.nextFloat() * H, dy = (r.nextFloat() - 0.5f) * H; app.down(x, y); for (int k = 1; k <= 5; k++) app.move(x, y + dy * k / 5); app.up(x, y + dy); }
            else if (act < 99) { if (!app.back()) app.screen = App.HOME; }
            else day[0]++;
        }
        System.out.println("screens visited " + Arrays.toString(seen) + " xp " + p.xp + " sessions " + p.sessions + " unlocked O " + p.unlocked(Runes.ELDER) + " J " + p.unlocked(Runes.YOUNGER) + " best streak " + p.bestStreak);
    }
}
