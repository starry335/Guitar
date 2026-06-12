package com.example.guitarhero.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.guitarhero.model.MaterialItem;

import java.util.ArrayList;
import java.util.List;

public class PracticeDiagramView extends View {
    private static final int INK = Color.rgb(31, 45, 45);
    private static final int MUTED = Color.rgb(138, 153, 153);
    private static final int GRID = Color.rgb(205, 221, 221);
    private static final int PRIMARY = Color.rgb(92, 174, 182);
    private static final int PRIMARY_DARK = Color.rgb(64, 142, 150);
    private static final int SOFT = Color.rgb(234, 247, 248);

    private MaterialItem item;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PracticeDiagramView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public PracticeDiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setMaterial(MaterialItem item) {
        this.item = item;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(250, 253, 253));
        canvas.drawRoundRect(new RectF(0, 0, w, h), dp(20), dp(20), paint);

        if (item == null) return;
        String category = safe(item.category);
        String title = safe(item.title);
        if (category.contains("和弦") || title.contains("和弦") || title.contains("F ")) {
            drawChordDiagram(canvas, w, h, title);
        } else if (category.contains("音阶")) {
            drawScaleDiagram(canvas, w, h, title);
        } else if (category.contains("节奏")) {
            drawRhythmDiagram(canvas, w, h, title);
        } else if (category.contains("右手")) {
            drawPickingDiagram(canvas, w, h, title);
        } else if (category.contains("左手")) {
            drawLeftHandDiagram(canvas, w, h, title);
        } else {
            drawPracticeFlow(canvas, w, h);
        }
    }

    private void drawChordDiagram(Canvas canvas, int w, int h, String title) {
        ChordShape shape = chordFor(title);
        drawTitle(canvas, shape.name + " 按法图", "竖线是弦，横线是品丝；圆点内为左手手指");
        int left = dp(58), top = dp(66), gridW = w - dp(92), gridH = h - dp(120);
        drawChordGrid(canvas, left, top, gridW, gridH, shape.startFret, shape.fretCount);

        for (ChordNote note : shape.notes) {
            if (note.barreToString >= 0) {
                barre(canvas, left, top, gridW, gridH, note.stringIndex, note.barreToString, note.fret - shape.startFret + 1, note.finger);
            } else {
                chordFinger(canvas, left, top, gridW, gridH, note.stringIndex, note.fret - shape.startFret + 1, note.finger, note.fret + "品");
            }
        }
        drawStringNames(canvas, left, top + gridH + dp(18), gridW);
        drawHint(canvas, shape.hint);
    }

    private void drawScaleDiagram(Canvas canvas, int w, int h, String title) {
        boolean pentatonic = title.contains("五声");
        drawTitle(canvas, pentatonic ? "A 小调五声音阶 5 品把位" : "C 大调音阶 2 品把位", "左侧是 6 弦到 1 弦，上方是实际品数");
        int left = dp(50), top = dp(68), gridW = w - dp(80), gridH = h - dp(124);
        int startFret = pentatonic ? 5 : 2;
        int fretCount = 5;
        drawFretboard(canvas, left, top, gridW, gridH, startFret, fretCount);

        ScaleNote[] notes = pentatonic
                ? new ScaleNote[]{
                n(6,5,"1"), n(6,8,"4"), n(5,5,"1"), n(5,7,"3"), n(4,5,"1"), n(4,7,"3"),
                n(3,5,"1"), n(3,7,"3"), n(2,5,"1"), n(2,8,"4"), n(1,5,"1"), n(1,8,"4")}
                : new ScaleNote[]{
                n(5,3,"1"), n(5,5,"3"), n(4,2,"1"), n(4,3,"2"), n(4,5,"4"),
                n(3,2,"1"), n(3,4,"3"), n(3,5,"4"), n(2,3,"1"), n(2,5,"3"), n(1,3,"1")};
        for (int i = 0; i < notes.length; i++) {
            ScaleNote note = notes[i];
            float x = fretCenter(left, gridW, startFret, fretCount, note.fret);
            float y = stringY(top, gridH, note.stringNo);
            scaleDot(canvas, x, y, note.finger, String.valueOf(i + 1));
        }
        drawHint(canvas, pentatonic ? "从 6弦5品 开始，按编号向 1弦8品 走，再原路返回" : "从 5弦3品 开始；先慢速唱名，再保持每个音干净");
    }

    private void drawRhythmDiagram(Canvas canvas, int w, int h, String title) {
        drawTitle(canvas, "扫弦节奏图", "箭头方向是拨片运动，数字是拍子位置");
        String[] beats = title.contains("切分")
                ? new String[]{"1", "&", "2", "&", "3", "&", "4", "&"}
                : title.contains("八分")
                ? new String[]{"1", "&", "2", "&", "3", "&", "4", "&"}
                : new String[]{"1", "2", "3", "4"};
        String[] moves = title.contains("切分")
                ? new String[]{"下", "空", "上", "下", "上", "空", "上", "下"}
                : title.contains("八分")
                ? new String[]{"下", "上", "下", "上", "下", "上", "下", "上"}
                : new String[]{"下", "下", "下", "下"};
        int left = dp(24), right = w - dp(24);
        int y = h / 2 + dp(8);
        float gap = (right - left) / (float) moves.length;
        for (int i = 0; i < moves.length; i++) {
            float cx = left + gap * i + gap / 2f;
            smallLabel(canvas, beats[i], cx, y - dp(54));
            rhythmPill(canvas, cx, y, moves[i]);
        }
        drawHint(canvas, "“空”表示手继续摆动但不碰弦，先数 1 & 2 & 3 & 4 &");
    }

    private void drawPickingDiagram(Canvas canvas, int w, int h, String title) {
        drawTitle(canvas, title.contains("分解") ? "P-i-m-a 指弹分解" : "右手拨弦位置", "P=拇指，i=食指，m=中指，a=无名指");
        int left = dp(54), top = dp(76), gridW = w - dp(108), gridH = h - dp(130);
        drawSixStringLines(canvas, left, top, gridW, gridH);
        String[] labels = title.contains("分解") ? new String[]{"P", "i", "m", "a", "m", "i"} :
                title.contains("闷音") ? new String[]{"掌侧靠桥", "短", "放", "短"} : new String[]{"下拨", "上拨", "下拨", "上拨"};
        int[] strings = title.contains("分解") ? new int[]{5, 3, 2, 1, 2, 3} : new int[]{3, 3, 3, 3};
        for (int i = 0; i < labels.length; i++) {
            float x = left + gridW * (i + 0.5f) / labels.length;
            float y = stringY(top, gridH, strings[i]);
            labeledDot(canvas, x, y, labels[i]);
        }
        drawHint(canvas, title.contains("分解") ? "先按 5弦-3弦-2弦-1弦-2弦-3弦，音量保持一样" : "拨片只穿过目标弦附近，动作越小越稳");
    }

    private void drawLeftHandDiagram(Canvas canvas, int w, int h, String title) {
        drawTitle(canvas, title.contains("击弦") ? "击弦 / 勾弦路线" : title.contains("滑音") ? "滑音路线" : "爬格子 5 品起步", "上方写实际品数，圆点写左手手指");
        int left = dp(50), top = dp(72), gridW = w - dp(82), gridH = h - dp(126);
        drawFretboard(canvas, left, top, gridW, gridH, 5, 4);
        if (title.contains("击弦")) {
            movePoint(canvas, left, top, gridW, gridH, 3, 5, "1", "拨");
            moveArrow(canvas, fretCenter(left, gridW, 5, 4, 5), stringY(top, gridH, 3), fretCenter(left, gridW, 5, 4, 7), stringY(top, gridH, 3));
            movePoint(canvas, left, top, gridW, gridH, 3, 7, "3", "击");
            moveArrow(canvas, fretCenter(left, gridW, 5, 4, 7), stringY(top, gridH, 3), fretCenter(left, gridW, 5, 4, 5), stringY(top, gridH, 3));
            movePoint(canvas, left, top, gridW, gridH, 3, 5, "1", "勾");
            drawHint(canvas, "拨 3弦5品 后，3指击到7品，再勾回5品");
        } else if (title.contains("滑音")) {
            movePoint(canvas, left, top, gridW, gridH, 3, 5, "1", "起");
            moveArrow(canvas, fretCenter(left, gridW, 5, 4, 5), stringY(top, gridH, 3), fretCenter(left, gridW, 5, 4, 7), stringY(top, gridH, 3));
            movePoint(canvas, left, top, gridW, gridH, 3, 7, "1", "到");
            drawHint(canvas, "1指按住 3弦5品 不松手，带着压力滑到7品");
        } else {
            for (int stringNo = 6; stringNo >= 1; stringNo--) {
                for (int fret = 5; fret <= 8; fret++) {
                    movePoint(canvas, left, top, gridW, gridH, stringNo, fret, String.valueOf(fret - 4), "");
                }
            }
            drawHint(canvas, "每根弦按 5-6-7-8 品，对应 1-2-3-4 指；先从6弦到1弦");
        }
    }

    private void drawPracticeFlow(Canvas canvas, int w, int h) {
        drawTitle(canvas, "练习流程", "先慢后快，每次练完记录问题");
        String[] steps = {"调音", "慢速", "纠错", "提速", "记录"};
        float gap = (w - dp(48)) / (float) steps.length;
        int y = h / 2 + dp(8);
        for (int i = 0; i < steps.length; i++) {
            pill(canvas, dp(24) + gap * i + dp(4), y - dp(20), dp(24) + gap * i + gap - dp(4), y + dp(20), steps[i]);
        }
        drawHint(canvas, "不要只追速度：干净、稳定、能重复，才算练会");
    }

    private void drawChordGrid(Canvas canvas, int left, int top, int gridW, int gridH, int startFret, int fretCount) {
        drawFretboard(canvas, left, top, gridW, gridH, startFret, fretCount);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(11));
        paint.setColor(PRIMARY_DARK);
        canvas.drawText(startFret == 1 ? "琴枕" : startFret + "品起", left - dp(8), top + dp(4), paint);
    }

    private void drawFretboard(Canvas canvas, int left, int top, int gridW, int gridH, int startFret, int fretCount) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        for (int i = 0; i < 6; i++) {
            float y = top + gridH * i / 5f;
            paint.setStrokeWidth(dp(i == 0 ? 1 : 1));
            paint.setColor(GRID);
            canvas.drawLine(left, y, left + gridW, y, paint);
        }
        for (int i = 0; i <= fretCount; i++) {
            float x = left + gridW * i / (float) fretCount;
            paint.setStrokeWidth(i == 0 && startFret == 1 ? dp(4) : dp(1));
            paint.setColor(i == 0 && startFret == 1 ? Color.rgb(150, 166, 166) : GRID);
            canvas.drawLine(x, top, x, top + gridH, paint);
            if (i < fretCount) {
                smallLabel(canvas, (startFret + i) + "品", left + gridW * (i + 0.5f) / fretCount, top - dp(12));
            }
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(10));
        paint.setColor(MUTED);
        for (int stringNo = 6; stringNo >= 1; stringNo--) {
            canvas.drawText(stringNo + "弦", left - dp(8), stringY(top, gridH, stringNo) + dp(4), paint);
        }
    }

    private void drawSixStringLines(Canvas canvas, int left, int top, int gridW, int gridH) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(GRID);
        for (int stringNo = 6; stringNo >= 1; stringNo--) {
            float y = stringY(top, gridH, stringNo);
            canvas.drawLine(left, y, left + gridW, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(10));
            paint.setColor(MUTED);
            canvas.drawText(stringNo + "弦", left - dp(8), y + dp(4), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(GRID);
        }
    }

    private void drawStringNames(Canvas canvas, int left, int y, int gridW) {
        String[] names = {"6弦E", "5弦A", "4弦D", "3弦G", "2弦B", "1弦E"};
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(9));
        paint.setColor(MUTED);
        for (int i = 0; i < names.length; i++) {
            canvas.drawText(names[i], left + gridW * i / 5f, y, paint);
        }
    }

    private void chordFinger(Canvas canvas, int left, int top, int gridW, int gridH, int stringIndex, int fretOffset, String finger, String fretLabel) {
        float x = left + gridW * stringIndex / 5f;
        float y = top + gridH * (fretOffset - 0.5f) / 4f;
        dot(canvas, x, y, finger, fretLabel);
    }

    private void barre(Canvas canvas, int left, int top, int gridW, int gridH, int s1, int s2, int fretOffset, String finger) {
        float x1 = left + gridW * s1 / 5f;
        float x2 = left + gridW * s2 / 5f;
        float y = top + gridH * (fretOffset - 0.5f) / 4f;
        paint.setColor(PRIMARY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(18));
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x1, y, x2, y, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(11));
        canvas.drawText(finger + "指横按", (x1 + x2) / 2f, y + dp(4), paint);
    }

    private void dot(Canvas canvas, float x, float y, String main, String sub) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PRIMARY);
        canvas.drawCircle(x, y, dp(14), paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(10));
        canvas.drawText(main, x, y + dp(4), paint);
        paint.setColor(PRIMARY_DARK);
        paint.setTextSize(dp(9));
        canvas.drawText(sub, x, y + dp(27), paint);
    }

    private void scaleDot(Canvas canvas, float x, float y, String finger, String order) {
        dot(canvas, x, y, finger, order);
    }

    private void movePoint(Canvas canvas, int left, int top, int gridW, int gridH, int stringNo, int fret, String finger, String tag) {
        float x = fretCenter(left, gridW, 5, 4, fret);
        float y = stringY(top, gridH, stringNo);
        dot(canvas, x, y, finger, tag.isEmpty() ? fret + "品" : tag);
    }

    private void labeledDot(Canvas canvas, float x, float y, String label) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SOFT);
        canvas.drawRoundRect(new RectF(x - dp(24), y - dp(15), x + dp(24), y + dp(15)), dp(15), dp(15), paint);
        paint.setColor(PRIMARY_DARK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(label.length() > 4 ? 9 : 12));
        canvas.drawText(label, x, y + dp(4), paint);
    }

    private void rhythmPill(Canvas canvas, float cx, int y, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor("空".equals(text) ? Color.rgb(245, 248, 248) : SOFT);
        canvas.drawRoundRect(new RectF(cx - dp(18), y - dp(24), cx + dp(18), y + dp(24)), dp(18), dp(18), paint);
        paint.setColor("空".equals(text) ? MUTED : PRIMARY_DARK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(12));
        canvas.drawText(text, cx, y + dp(5), paint);
    }

    private void pill(Canvas canvas, float l, float t, float r, float b, String text) {
        paint.setColor(SOFT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(l, t, r, b), dp(18), dp(18), paint);
        paint.setColor(PRIMARY_DARK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(12));
        canvas.drawText(text, (l + r) / 2f, (t + b) / 2f + dp(5), paint);
    }

    private void moveArrow(Canvas canvas, float x1, float y1, float x2, float y2) {
        paint.setColor(MUTED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x1, y1, x2, y2, paint);
        float dir = x2 >= x1 ? 1f : -1f;
        canvas.drawLine(x2, y2, x2 - dir * dp(8), y2 - dp(6), paint);
        canvas.drawLine(x2, y2, x2 - dir * dp(8), y2 + dp(6), paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawTitle(Canvas canvas, String text, String sub) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(INK);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(15));
        canvas.drawText(text, dp(18), dp(28), paint);
        paint.setColor(MUTED);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(dp(11));
        canvas.drawText(sub, dp(18), dp(47), paint);
    }

    private void drawHint(Canvas canvas, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(MUTED);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(dp(11));
        canvas.drawText(text, dp(18), getHeight() - dp(16), paint);
    }

    private void smallLabel(Canvas canvas, String text, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(10));
        paint.setColor(MUTED);
        canvas.drawText(text, x, y, paint);
    }

    private float fretCenter(int left, int gridW, int startFret, int fretCount, int fret) {
        int offset = Math.max(0, Math.min(fretCount - 1, fret - startFret));
        return left + gridW * (offset + 0.5f) / fretCount;
    }

    private float stringY(int top, int gridH, int stringNo) {
        int row = Math.max(0, Math.min(5, 6 - stringNo));
        return top + gridH * row / 5f;
    }

    private ScaleNote n(int stringNo, int fret, String finger) {
        return new ScaleNote(stringNo, fret, finger);
    }

    private ChordShape chordFor(String title) {
        if (title.contains("G")) {
            return new ChordShape("G 和弦", 1, "2指5弦2品，3指6弦3品，4指1弦3品；其余空弦自然响")
                    .add(1, 2, "2").add(0, 3, "3").add(5, 3, "4");
        }
        if (title.contains("F")) {
            return new ChordShape("F 和弦", 1, "1指横按1品，2指3弦2品，3指5弦3品，4指4弦3品")
                    .barre(5, 0, 1, "1").add(2, 2, "2").add(1, 3, "3").add(2, 3, "4");
        }
        if (title.contains("Am")) {
            return new ChordShape("Am 和弦", 1, "1指2弦1品，2指4弦2品，3指3弦2品；5弦起扫")
                    .add(4, 1, "1").add(2, 2, "2").add(3, 2, "3");
        }
        return new ChordShape("C 和弦", 1, "1指2弦1品，2指4弦2品，3指5弦3品；从5弦开始拨")
                .add(4, 1, "1").add(2, 2, "2").add(1, 3, "3");
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ScaleNote {
        final int stringNo;
        final int fret;
        final String finger;

        ScaleNote(int stringNo, int fret, String finger) {
            this.stringNo = stringNo;
            this.fret = fret;
            this.finger = finger;
        }
    }

    private static class ChordNote {
        final int stringIndex;
        final int fret;
        final String finger;
        final int barreToString;

        ChordNote(int stringIndex, int fret, String finger, int barreToString) {
            this.stringIndex = stringIndex;
            this.fret = fret;
            this.finger = finger;
            this.barreToString = barreToString;
        }
    }

    private static class ChordShape {
        final String name;
        final int startFret;
        final int fretCount = 4;
        final String hint;
        final List<ChordNote> notes = new ArrayList<>();

        ChordShape(String name, int startFret, String hint) {
            this.name = name;
            this.startFret = startFret;
            this.hint = hint;
        }

        ChordShape add(int stringIndex, int fret, String finger) {
            notes.add(new ChordNote(stringIndex, fret, finger, -1));
            return this;
        }

        ChordShape barre(int fromStringIndex, int toStringIndex, int fret, String finger) {
            notes.add(new ChordNote(fromStringIndex, fret, finger, toStringIndex));
            return this;
        }
    }
}
