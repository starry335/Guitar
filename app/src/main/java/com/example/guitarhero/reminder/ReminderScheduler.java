package com.example.guitarhero.reminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.example.guitarhero.MainActivity;
import com.example.guitarhero.R;
import com.example.guitarhero.data.GuitarRepository;

import java.util.Calendar;
import java.util.Locale;

public class ReminderScheduler {
    public static final String ACTION_REMIND = "com.example.guitarhero.ACTION_DAILY_REMINDER";
    public static final String ACTION_BOOT = Intent.ACTION_BOOT_COMPLETED;
    public static final String CHANNEL_ID = "guitar_practice_reminder";
    private static final int REQUEST_REMINDER = 2601;
    private static final int NOTIFICATION_ID = 2602;

    private ReminderScheduler() {}

    public static void scheduleDaily(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = reminderPendingIntent(context);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DATE, 1);
        }

        // setAndAllowWhileIdle works on modern Android without requiring the special exact-alarm setting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(reminderPendingIntent(context));
    }

    public static void rescheduleFromSavedSetting(Context context) {
        GuitarRepository repo = new GuitarRepository(context);
        if (repo.isReminderEnabled()) {
            scheduleDaily(context, repo.getReminderHour(), repo.getReminderMinute());
        } else {
            cancel(context);
        }
    }

    public static void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannel(context, manager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("今天也练一会儿吉他吧")
                .setContentText("打开吉他 Hero，完成你的每日练习。")
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(android.app.Notification.PRIORITY_DEFAULT);
        }
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static String formatTime(int hour, int minute) {
        return String.format(Locale.CHINA, "%02d:%02d", hour, minute);
    }

    private static PendingIntent reminderPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void createChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "练习提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("每日吉他练习提醒");
            manager.createNotificationChannel(channel);
        }
    }
}
