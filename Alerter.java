package dz.alertecourses;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Notification + son (flux alarme, sonne même en mode silencieux) + vibration + voix. */
public final class Alerter {
    private static final String CHANNEL = "courses";
    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady;
    private int notifId = 100;

    public Alerter(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        NotificationManager nm = this.ctx.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Courses intéressantes",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);          // le son est joué par l'application (flux alarme)
        ch.enableVibration(false);        // idem pour la vibration
        nm.createNotificationChannel(ch);
        tts = new TextToSpeech(this.ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.FRANCE);
                ttsReady = true;
            }
        });
    }

    public void alert(Offer o, String zone) {
        SharedPreferences p = Prefs.get(ctx);
        String dest = o.destination.isEmpty() ? "?" : o.destination;
        String title = o.price + " DA → " + (zone == null || zone.isEmpty() ? dest : capitalize(zone));
        String body = "Client à " + Offer.km(o.pickupKm) + " km · " + o.pickup + "\nVers : " + dest
                + (o.tripKm > 0 ? "\nCourse : " + Offer.km(o.tripKm) + " km" : "")
                + "\n(" + o.format + ")";
        notify(title, body, o.app);

        if (p.getBoolean(Prefs.LOUD, true)) playSound();
        if (p.getBoolean(Prefs.VIBRATE, true)) vibrate();
        if (p.getBoolean(Prefs.VOICE, true)) {
            String where = zone == null || zone.isEmpty() ? "" : ", vers " + capitalize(zone);
            String km = o.pickupKm >= 0 ? ", client à " + Offer.km(o.pickupKm) + " kilomètres" : "";
            final String text = o.format + ". Course à " + o.price + " dinars" + where + km;
            handler.postDelayed(() -> speak(text), p.getBoolean(Prefs.LOUD, true) ? 1500 : 0);
        }
    }

    private void notify(String title, String body, String pkg) {
        Intent open = pkg == null ? null : ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (open == null) open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, notifId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        ctx.getSystemService(NotificationManager.class).notify(notifId++, n);
        if (notifId > 140) notifId = 100;
    }

    private void playSound() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            final Ringtone r = RingtoneManager.getRingtone(ctx, uri);
            if (r == null) return;
            r.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            r.play();
            handler.postDelayed(r::stop, 3000);
        } catch (Exception ignored) { }
    }

    private void vibrate() {
        try {
            Vibrator v = ctx.getSystemService(Vibrator.class);
            if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[]{0, 400, 200, 400, 200, 700}, -1));
        } catch (Exception ignored) { }
    }

    private void speak(String text) {
        if (!ttsReady) return;
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);
        tts.speak(text, TextToSpeech.QUEUE_ADD, params, "c" + System.currentTimeMillis());
    }

    private static String capitalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    public void shutdown() {
        if (tts != null) tts.shutdown();
    }
}
