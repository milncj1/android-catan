package edu.up.cs.androidcatan.catan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

import edu.up.cs.androidcatan.catan.graphics.HexagonGrid;

/**
 * Tiny native, offline public-VP trend chart. Each point is a completed round.
 * Opponents' private VP development cards are intentionally NOT plotted.
 * The final total appears separately only in the finished match summary.
 */
public final class CatanCareerGraph extends View {
    private final int[][] rounds;
    private final String[] names;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CatanCareerGraph(Context context, List<int[]> rounds, String[] names) {
        super(context);
        this.names = names.clone();
        this.rounds = new int[rounds.size()][];
        for (int i = 0; i < rounds.size(); i++) this.rounds[i] = rounds.get(i).clone();
        setMinimumHeight(dp(156));
        setContentDescription("Public victory point history across "
                + rounds.size() + " complete rounds");
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(dp(164), MeasureSpec.EXACTLY));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rounds.length < 2 || names.length < 2) {
            paint.setColor(0xFFBBC6C9);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(12));
            canvas.drawText("Score graph appears after two complete rounds.",
                    dp(8), dp(25), paint);
            return;
        }
        final float left = dp(32), top = dp(8), right = getWidth() - dp(15);
        final float bottom = getHeight() - dp(40);
        if (right <= left || bottom <= top) return;
        final RectF plot = new RectF(left, top, right, bottom);
        int high = 10;
        for (int[] row : rounds)
            for (int score : row) high = Math.max(high, score);
        high = ((high + 1) / 2) * 2;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        for (int y = 0; y <= high; y += 2) {
            float h = plot.bottom - (y / (float) high) * plot.height();
            paint.setColor(0x337C9FBA);
            canvas.drawLine(plot.left, h, plot.right, h, paint);
            paint.setColor(0xFFBACAD3);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(10));
            canvas.drawText(Integer.toString(y), dp(4), h + dp(3), paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        for (int p = 0; p < names.length; p++) {
            paint.setColor(HexagonGrid.playerColors[
                    Math.min(p, HexagonGrid.playerColors.length - 1)]);
            paint.setStrokeWidth(dp(2.5f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int r = 1; r < rounds.length; r++) {
                if (p >= rounds[r].length || p >= rounds[r-1].length) continue;
                float x1 = plot.left + (r - 1f) * plot.width() / (rounds.length - 1);
                float x2 = plot.left + r * plot.width() / (rounds.length - 1);
                float y1 = plot.bottom - Math.min(high, rounds[r-1][p]) *
                        plot.height() / high;
                float y2 = plot.bottom - Math.min(high, rounds[r][p]) *
                        plot.height() / high;
                canvas.drawLine(x1, y1, x2, y2, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(10));
        paint.setColor(0xFFCAD8DF);
        canvas.drawText("Round 1", plot.left, getHeight() - dp(24), paint);
        String last = "Round " + rounds.length;
        canvas.drawText(last, right - paint.measureText(last),
                getHeight() - dp(24), paint);
        float x = dp(8), legendY = getHeight() - dp(7);
        paint.setTextSize(dp(10));
        for (int p = 0; p < names.length; p++) {
            paint.setColor(HexagonGrid.playerColors[
                    Math.min(p, HexagonGrid.playerColors.length - 1)]);
            String label = names[p].length() > 11 ? names[p].substring(0, 10) + "…" : names[p];
            canvas.drawText(label, x, legendY, paint);
            x += paint.measureText(label) + dp(12);
            if (x > getWidth() - dp(50)) break; // keep legend unobtrusive
        }
    }
}
