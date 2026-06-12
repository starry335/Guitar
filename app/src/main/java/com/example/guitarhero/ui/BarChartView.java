package com.example.guitarhero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BarChartView extends View {
    private int[] values = {0, 0, 0, 0, 0, 0, 0};
    private String[] labels = {"一", "二", "三", "四", "五", "六", "日"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setData(int[] values, String[] labels) {
        this.values = values == null ? new int[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(); int h = getHeight();
        int left = dp(22), right = dp(18), top = dp(22), bottom = dp(42);
        int chartW = w - left - right; int chartH = h - top - bottom;
        int max = 1; for (int v: values) if (v > max) max = v;
        paint.setColor(Color.rgb(221, 239, 239)); paint.setStrokeWidth(dp(1));
        for (int i=0;i<4;i++) {
            float y = top + chartH * i / 3f;
            canvas.drawLine(left, y, w - right, y, paint);
        }
        if (values.length == 0) return;
        float gap = dp(8); float barW = Math.max(dp(8), (chartW - gap * (values.length + 1)) / values.length);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(dp(11));
        for (int i=0;i<values.length;i++) {
            float x = left + gap + i * (barW + gap);
            float bh = chartH * values[i] / (float) max;
            paint.setColor(Color.rgb(58, 188, 194));
            canvas.drawRoundRect(x, top + chartH - bh, x + barW, top + chartH, dp(8), dp(8), paint);
            paint.setColor(Color.rgb(122, 141, 142));
            String label = i < labels.length ? labels[i] : String.valueOf(i + 1);
            canvas.drawText(label, x + barW/2, h - dp(18), paint);
        }
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
