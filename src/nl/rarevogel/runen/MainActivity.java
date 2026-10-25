package nl.rarevogel.runen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;

import nl.rarevogel.runen.core.App;
import nl.rarevogel.runen.core.Progress;

public class MainActivity extends Activity implements App.Host {
    AppView view;
    App app;
    Progress progress;
    Vibrator vib;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF161B21);
        getWindow().setNavigationBarColor(0xFF0D1115);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        final SharedPreferences sp = getSharedPreferences("runen", MODE_PRIVATE);
        progress = new Progress(new Progress.Store() {
            public String get(String k) { return sp.getString(k, null); }
            public void put(String k, String v) { sp.edit().putString(k, v).apply(); }
            public void clear() { sp.edit().clear().apply(); }
        });
        app = new App(progress, this);
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Typeface title = Typeface.createFromAsset(getAssets(), "cinzel-bold.ttf");
        Typeface rune = Typeface.createFromAsset(getAssets(), "runic.ttf");
        view = new AppView(this, new AndroidGfx(title, rune));
        setContentView(view);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause() {
        super.onPause();
        progress.save();
        if (player != null) { player.release(); player = null; }
    }

    @Override
    public void onBackPressed() {
        if (!app.back()) super.onBackPressed();
    }

    // ---- App.Host
    public void quit() { finish(); }

    public long today() {
        Calendar c = Calendar.getInstance();
        long local = c.getTimeInMillis() + c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        return local / 86400000L;
    }

    @SuppressWarnings("deprecation")
    public void buzz(int ms) {
        try { if (vib != null && ms > 0) vib.vibrate(ms); } catch (Exception ignored) { }
    }

    android.media.MediaPlayer player;

    public void play(String name) {
        try {
            if (player != null) { player.release(); player = null; }
            android.content.res.AssetFileDescriptor fd = getAssets().openFd("audio/" + name + ".ogg");
            player = new android.media.MediaPlayer();
            player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            player.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                public void onCompletion(android.media.MediaPlayer mp) { mp.release(); if (player == mp) player = null; }
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            if (player != null) { player.release(); player = null; }
        }
    }

    final class AppView extends View {
        final AndroidGfx gfx;
        long last;
        final Runnable tick = new Runnable() { public void run() { invalidate(); } };

        AppView(Context ctx, AndroidGfx gfx) {
            super(ctx);
            this.gfx = gfx;
        }

        @Override
        protected void onDraw(Canvas c) {
            long now = System.nanoTime();
            float dt = last == 0 ? 0.016f : Math.min(0.6f, (now - last) / 1e9f);
            last = now;
            gfx.c = c;
            app.frame(gfx, getWidth(), getHeight(), dt);
            removeCallbacks(tick);
            if (app.busy()) postInvalidateOnAnimation();
            else postDelayed(tick, 500);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            invalidate();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: app.down(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_MOVE: app.move(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_UP: app.up(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_CANCEL: app.up(-1000, -1000); break;
                default: break;
            }
            return true;
        }
    }
}
