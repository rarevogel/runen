package nl.rarevogel.runen.core;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
public class Shot {
    public static void main(String[] a) throws Exception {
        String out = a[0]; int W = Integer.parseInt(a[1]), H = Integer.parseInt(a[2]); String scene = a[3];
        Map<String,String> m = new HashMap<>();
        Progress.Store st = new Progress.Store() { public String get(String k){return m.get(k);} public void put(String k,String v){m.put(k,v);} public void clear(){m.clear();} };
        Progress p = new Progress(st);
        App app = new App(p, new App.Host() { public void quit(){} public long today(){return 20000;} public void buzz(int ms){} public void play(String n){} });
        Random r = new Random(2);
        // some progress
        if (!scene.equals("fresh")) {
            for (int i = 0; i < 8; i++) for (int k = 0; k < (8 - i) / 2 + 1; k++) p.record(Runes.ELDER.runes.get(i), r.nextFloat() < 0.8f);
            p.checkUnlock(Runes.ELDER); p.checkUnlock(Runes.ELDER);
            p.record(Runes.ELDER.runes.get(1), false);
            p.sessionDone(19998); p.sessionDone(19999); p.sessionDone(20000);
        }
        if (scene.endsWith("J")) { app.setTrack(Runes.YOUNGER); scene = scene.substring(0, scene.length() - 1); }
        if (scene.endsWith("A")) { app.setTrack(Runes.FUTHORC); scene = scene.substring(0, scene.length() - 1); }
        String[] parts = scene.split(":");
        switch (parts[0]) {
            case "home": case "fresh": break;
            case "cards": app.go(App.CARDS); break;
            case "detail": app.detail = Integer.parseInt(parts[1]); app.go(App.CARDS); app.go(App.DETAIL); if (parts.length > 2) app.scroll[App.DETAIL] = Float.parseFloat(parts[2]) * W / 400f; break;
            case "quiz": {
                int type = Integer.parseInt(parts[1]); p.unlockAll(app.track);
                app.quiz = Quiz.make(app.track, p, new Random(5)); app.go(App.QUIZ);
                Quiz q = app.quiz;
                Quiz.Step s = Quiz.question(app.track, p, app.track.runes.get(2), new Random(3));
                for (int t = 0; t < 50 && s.type != type; t++) { p.record(app.track.runes.get(2), true); s = Quiz.question(app.track, p, app.track.runes.get(2), new Random(t)); }
                if (type == Quiz.INTRO) { s = new Quiz.Step(); s.type = Quiz.INTRO; s.rune = app.track.runes.get(9); }
                q.steps.add(0, s); q.pos = 0;
                if (parts.length > 2) {
                    if (type == Quiz.WRITE) { s.typed.append(s.target.substring(0, Math.min(3, s.target.length()))); if (parts[2].equals("a")) q.checkWrite(s, p); }
                    else q.choose(s, parts[2].equals("ok") ? s.answer : (s.answer + 1) % 4, p);
                }
                break;
            }
            case "summary": { app.quiz = Quiz.make(app.track, p, new Random(5)); app.go(App.QUIZ); app.quiz.right = 8; app.quiz.pos = app.quiz.steps.size(); app.finishQuiz(); app.fresh = new ArrayList<>(Runes.ELDER.runes.subList(12, 16)); break; }
            case "write": app.go(App.WRITE); app.typed = parts.length > 1 ? parts[1] : ""; break;
            case "writed": p.dotted = true; app.go(App.WRITE); app.typed = parts.length > 1 ? parts[1] : ""; break;
            case "progress": app.go(App.PROGRESS); if (parts.length > 1) app.scroll[App.PROGRESS] = Float.parseFloat(parts[1]) * W / 400f; break;
            case "info": app.go(App.INFO); break;
            case "stones": app.go(App.STONES); break;
            case "stone": {
                for (Stones.Stone stn : Stones.ALL) if (stn.id.equals(parts[1])) app.stone = stn;
                int k = Integer.parseInt(parts[2]);
                for (int i = 0; i < k && i < app.stone.words.size(); i++) p.reveal(app.stone, i);
                app.go(App.STONES); app.go(App.STONE);
                if (parts.length > 3) app.scroll[App.STONE] = Float.parseFloat(parts[3]) * W / 400f;
                break;
            }
            case "confirm": app.go(App.PROGRESS); app.confirmReset = true; break;
        }
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        J2dGfx g = new J2dGfx(g2);
        app.time = 0.2f;
        app.frame(g, W, H, 0.016f);
        g2.setTransform(new java.awt.geom.AffineTransform()); g2.setClip(null);
        app.frame(g, W, H, 0.016f);
        ImageIO.write(img, "png", new File(out));
    }
}
