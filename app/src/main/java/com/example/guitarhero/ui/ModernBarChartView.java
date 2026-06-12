package com.example.guitarhero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class ModernBarChartView extends View {
    private int[] values = new int[0];
    private String[] labels = new String[0];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ModernBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] values, String[] labels) {
        this.values = values == null ? new int[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        invalidate();
    }

    public boolean hasData() {
        for (int value : values) if (value > 0) return true;
        return false;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int left = dp(10);
        int right = dp(8);
        int top = dp(16);
        int bottom = dp(34);
        int chartWidth = width - left - right;
        int chartHeight = height - top - bottom;

        paint.setShader(null);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(235, 243, 243));
        for (int i = 0; i < 4; i++) {
            float y = top + chartHeight * i / 3f;
            canvas.drawLine(left, y, width - right, y, paint);
        }

        if (values.length == 0) return;
        int max = 1;
        int maxIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                maxIndex = i;
            }
        }

        float gap = values.length > 8 ? dp(7) : dp(11);
        float barWidth = Math.max(dp(9), (chartWidth - gap * (values.length + 1)) / values.length);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(values.length > 8 ? 10 : 11));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        for (int i = 0; i < values.length; i++) {
            float x = left + gap + i * (barWidth + gap);
            float barHeight = values[i] <= 0 ? dp(4) : chartHeight * values[i] / (float) max;
            float topY = top + chartHeight - barHeight;
            int startColor = i == maxIndex && values[i] > 0 ? Color.rgb(92, 174, 182) : Color.rgb(131, 201, 207);
            int endColor = i == maxIndex && values[i] > 0 ? Color.rgb(177, 225, 228) : Color.rgb(224, 244, 245);
            paint.setShader(new LinearGradient(0, topY, 0, top + chartHeight, startColor, endColor, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(x, topY, x + barWidth, top + chartHeight, dp(9), dp(9), paint);
            paint.setShader(null);
            paint.setColor(Color.rgb(138, 153, 153));
            String label = i < labels.length ? labels[i] : String.valueOf(i + 1);
            canvas.drawText(label, x + barWidth / 2f, height - dp(10), paint);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
