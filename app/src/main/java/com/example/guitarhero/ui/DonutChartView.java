package com.example.guitarhero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class DonutChartView extends View {
    private int[] values = new int[0];
    private int totalMinutes = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette = new int[]{
            Color.rgb(109, 189, 196),
            Color.rgb(134, 164, 174),
            Color.rgb(139, 198, 174),
            Color.rgb(176, 169, 199),
            Color.rgb(210, 228, 226)
    };

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] values, int totalMinutes) {
        this.values = values == null ? new int[0] : values;
        this.totalMinutes = Math.max(0, totalMinutes);
        invalidate();
    }

    public int colorForIndex(int index) {
        return palette[index % palette.length];
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - dp(34);
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        RectF oval = new RectF(left, top, left + size, top + size);
        float stroke = dp(22);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(238, 248, 249));
        canvas.drawArc(oval, -90, 360, false, paint);

        int total = 0;
        for (int value : values) total += Math.max(0, value);
        if (total > 0) {
            float start = -90f;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = Math.max(3f, 360f * values[i] / total);
                paint.setColor(colorForIndex(i));
                canvas.drawArc(oval, start, sweep - 2f, false, paint);
                start += sweep;
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.rgb(138, 153, 153));
        paint.setTextSize(dp(12));
        canvas.drawText("总计", width / 2f, height / 2f - dp(8), paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setColor(Color.rgb(31, 45, 45));
        paint.setTextSize(dp(17));
        canvas.drawText(formatMinutes(totalMinutes), width / 2f, height / 2f + dp(18), paint);
    }

    private String formatMinutes(int minutes) {
        if (minutes <= 0) return "0分钟";
        if (minutes < 60) return minutes + "分钟";
        int h = minutes / 60;
        int m = minutes % 60;
        return h + "小时" + (m == 0 ? "" : m + "分");
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
