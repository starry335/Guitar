package com.example.guitarhero.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ReminderScheduler.rescheduleFromSavedSetting(context);
            return;
        }
        if (ReminderScheduler.ACTION_REMIND.equals(action)) {
            ReminderScheduler.showNotification(context);
            ReminderScheduler.rescheduleFromSavedSetting(context);
        }
    }
}
