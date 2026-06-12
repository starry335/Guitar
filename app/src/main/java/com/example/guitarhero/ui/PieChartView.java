package com.example.guitarhero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {
    private int[] values = new int[0];
    private String centerText = "暂无";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette = new int[]{
            Color.rgb(58, 188, 194),
            Color.rgb(125, 221, 224),
            Color.rgb(153, 207, 202),
            Color.rgb(188, 226, 224),
            Color.rgb(118, 156, 160),
            Color.rgb(219, 240, 239)
    };

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] values, int totalMinutes) {
        this.values = values == null ? new int[0] : values;
        this.centerText = totalMinutes <= 0 ? "暂无" : formatMinutes(totalMinutes);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - dp(38);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        RectF oval = new RectF(left, top, left + size, top + size);

        int total = 0;
        for (int v : values) total += Math.max(0, v);

        paint.setStyle(Paint.Style.FILL);
        if (total <= 0) {
            paint.setColor(Color.rgb(238, 248, 248));
            canvas.drawCircle(w / 2f, h / 2f, size / 2f, paint);
        } else {
            float start = -90f;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = 360f * values[i] / total;
                paint.setColor(palette[i % palette.length]);
                canvas.drawArc(oval, start, sweep, true, paint);
                start += sweep;
            }
        }

        paint.setColor(Color.WHITE);
        canvas.drawCircle(w / 2f, h / 2f, size * 0.28f, paint);

        paint.setColor(Color.rgb(31, 45, 45));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(16));
        canvas.drawText(centerText, w / 2f, h / 2f + dp(6), paint);
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + "分钟";
        int h = minutes / 60;
        int m = minutes % 60;
        return h + "小时" + (m == 0 ? "" : m + "分");
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
