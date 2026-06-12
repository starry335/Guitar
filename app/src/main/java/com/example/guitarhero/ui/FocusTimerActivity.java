package com.example.guitarhero.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.guitarhero.R;

import java.util.Locale;

public class FocusTimerActivity extends Activity {
    private TextView countdownText;
    private TextView digitMinTens, digitMinOnes, digitSecTens, digitSecOnes;
    private CountDownTimer timer;
    private long remainingMs;
    private boolean running = true;
    private int targetMinutes;
    private long itemId;
    private String dateKey;
    private int lastShownSeconds = -1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setupSystemBars();
        setContentView(R.layout.activity_focus_timer);

        countdownText = findViewById(R.id.countdownText);
        digitMinTens = findViewById(R.id.digitMinTens);
        digitMinOnes = findViewById(R.id.digitMinOnes);
        digitSecTens = findViewById(R.id.digitSecTens);
        digitSecOnes = findViewById(R.id.digitSecOnes);

        TextView title = findViewById(R.id.timerTitle);
        TextView subTitle = findViewById(R.id.timerSubTitle);
        Button pause = findViewById(R.id.pauseButton);
        Button end = findViewById(R.id.endButton);

        String name = getIntent().getStringExtra("name");
        if (name == null || name.trim().isEmpty()) name = "吉他练习";
        targetMinutes = getIntent().getIntExtra("minutes", 25);
        itemId = getIntent().getLongExtra("itemId", -1);
        dateKey = getIntent().getStringExtra("dateKey");

        title.setText("正在练习 · " + name);
        subTitle.setText(targetMinutes + " 分钟专注倒计时");
        remainingMs = targetMinutes * 60_000L;
        render(remainingMs);
        startTimer();

        pause.setOnClickListener(v -> {
            if (running) {
                if (timer != null) timer.cancel();
                running = false;
                pause.setText("继续");
            } else {
                running = true;
                pause.setText("暂停");
                startTimer();
            }
        });
        end.setOnClickListener(v -> finishWithMinutes());
    }

    private void setupSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#0D2023"));
        window.setNavigationBarColor(Color.parseColor("#0D2023"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void startTimer() {
        timer = new CountDownTimer(remainingMs, 1000) {
            @Override public void onTick(long ms) {
                remainingMs = ms;
                render(ms);
            }
            @Override public void onFinish() {
                remainingMs = 0;
                render(0);
                finishWithMinutes();
            }
        }.start();
    }

    private void render(long ms) {
        int total = (int) Math.max(0, ms / 1000);
        int minutes = total / 60;
        int seconds = total % 60;
        String value = String.format(Locale.CHINA, "%02d:%02d", minutes, seconds);
        countdownText.setText(value);

        if (total != lastShownSeconds) {
            lastShownSeconds = total;
            setDigit(digitMinTens, minutes / 10);
            setDigit(digitMinOnes, minutes % 10);
            setDigit(digitSecTens, seconds / 10);
            setDigit(digitSecOnes, seconds % 10);
        }
    }

    private void setDigit(TextView tv, int value) {
        String next = String.valueOf(value);
        if (next.contentEquals(tv.getText())) return;
        tv.animate().rotationX(90f).alpha(0.35f).setDuration(120).withEndAction(() -> {
            tv.setText(next);
            tv.setRotationX(-90f);
            tv.animate().rotationX(0f).alpha(1f).setDuration(160).start();
        }).start();
    }

    private void finishWithMinutes() {
        if (timer != null) timer.cancel();
        int used = Math.max(1, targetMinutes - (int)(remainingMs / 60_000L));
        getSharedPreferences("timer_result", MODE_PRIVATE)
                .edit()
                .putLong("itemId", itemId)
                .putString("dateKey", dateKey)
                .putInt("minutes", used)
                .apply();
        finish();
    }

    @Override public void onBackPressed() { }
}
