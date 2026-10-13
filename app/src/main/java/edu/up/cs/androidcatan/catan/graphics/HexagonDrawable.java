package edu.up.cs.androidcatan.catan.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import java.io.Serializable;

import edu.up.cs.androidcatan.R;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * https://github.com/alexweininger/android-catan
 **/

public class HexagonDrawable extends BoardSurfaceView implements Serializable {

    private static final String TAG = "HexagonDrawable"; // log tag

    // instance variables concerning graphics
    private Context context;
    protected int x, y;
    protected Path hexagonPath;
    protected int[][] points;
    protected int size;
    protected int color;
    private boolean highlight;
    protected int resourceId;

    // instance variables concerning game logic
    protected int hexagonId;
    protected int chitValue;
    protected boolean isRobber, isDesert;

    private static final int[] terrainArt = {
            R.drawable.terrain_brick, R.drawable.terrain_grain,
            R.drawable.terrain_lumber, R.drawable.terrain_ore,
            R.drawable.terrain_wool, R.drawable.terrain_desert
    };

    static int[] resourceDrawables = {R.drawable.brick_icon_25x25, R.drawable.grain_icon_25x25, R.drawable.lumber_icon_25x25, R.drawable.ore_icon_25x25, R.drawable.wool_icon_25x25};
    private static final String[] resourceLabels = {"BRICK", "GRAIN", "LUMBER", "ORE", "WOOL", "DESERT"};

    /**
     * the method for drawing the hexagon shaped tiles on the board
     *
     * @param context    the app context
     * @param x          the x position
     * @param y          the y position
     * @param size       how big the hexagon is
     * @param color      the colour of the tile
     * @param isRobber   true or false for showing the robber or not
     * @param isDesert   true or false for if it is the desert tile or now
     * @param chitValue  the value of the tile
     * @param hexagonId  the id of where it is on the board
     * @param highlight  the selection box around the hexagon
     * @param resourceId which resource it provides
     */
    public HexagonDrawable(Context context, int x, int y, int size, int color, boolean isRobber, boolean isDesert, int chitValue, int hexagonId, boolean highlight, int resourceId) {
        super(context);
        setWillNotDraw(false);
        this.context = context;
        this.x = x;
        this.y = y;
        this.size = size; // size can also be thought of as the radius
        this.color = color;
        this.isDesert = isDesert;
        this.isRobber = isRobber;
        this.chitValue = chitValue;
        this.hexagonId = hexagonId;
        this.highlight = highlight;
        this.resourceId = resourceId;
    }

    // constructors needed by android
    public HexagonDrawable(Context context) {
        super(context);
    }

    public HexagonDrawable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param canvas    Canvas to draw the hexagon upon.
     * @param debugMode Debug mode.
     */
    /**
     * Paint an illustrated hex without changing its original geometry.
     * Roads, settlements and touch hit testing continue to use the same points.
     */
    public void drawHexagon(Canvas canvas, boolean debugMode) {
        points = calculateHexagonPoints(this.x, this.y, this.size);
        Path hexagonPath = createHexagonPath(points);

        final int[] light = {0xFFD99471, 0xFFF8D56B, 0xFF6FA66B,
                             0xFFA8AEB4, 0xFF9ACD7F, 0xFFE8CB91};
        final int[] dark  = {0xFF99583F, 0xFFD4A744, 0xFF36683E,
                             0xFF59656D, 0xFF56834B, 0xFFC69C62};
        int terrain = Math.max(0, Math.min(resourceId, 5));

        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x660C2D2F);
        canvas.save();
        canvas.translate(5f, 9f);
        canvas.drawPath(hexagonPath, shadow);
        canvas.restore();

        Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
        ground.setShader(new LinearGradient(x - size, y - size,
                x + size, y + size, light[terrain], dark[terrain],
                Shader.TileMode.CLAMP));
        canvas.drawPath(hexagonPath, ground);

        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(9f);
        edge.setStrokeJoin(Paint.Join.ROUND);
        edge.setColor(0xFFEBD9A9);
        canvas.drawPath(hexagonPath, edge);
        edge.setStrokeWidth(3f);
        edge.setColor(0x884C3927);
        canvas.drawPath(hexagonPath, edge);

        // Kenney CC0 miniature scenery, scattered deterministically so
        // terrain does not rearrange itself every time the screen redraws.
        Drawable decoration = context.getDrawable(terrainArt[terrain]);
        if (decoration != null) {
            canvas.save();
            canvas.clipPath(hexagonPath);
            for (int i = 0; i < 4; i++) {
                int dx = ((hexagonId * 37 + i * 71) % 178) - 89;
                int dy = ((hexagonId * 23 + i * 43) % 92) - 77;
                int artSize = terrain == 1 ? 54 : 82;
                decoration.setBounds(x + dx - artSize / 2,
                        y + dy - artSize,
                        x + dx + artSize / 2, y + dy + artSize / 3);
                decoration.draw(canvas);
            }
            canvas.restore();
        }

        if (highlight) {
            Paint selected = new Paint(Paint.ANTI_ALIAS_FLAG);
            selected.setColor(0xFFFFEE96);
            selected.setStyle(Paint.Style.STROKE);
            selected.setStrokeJoin(Paint.Join.ROUND);
            selected.setStrokeWidth(16f);
            canvas.drawPath(hexagonPath, selected);
        }

        if (!isDesert) {
            // Classic Catan number token: always legible above the scenery.
            float tokenY = y + 49f;
            Paint tokenShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            tokenShadow.setColor(0x77422D1F);
            canvas.drawCircle(x + 3f, tokenY + 6f, 42f, tokenShadow);

            Paint token = new Paint(Paint.ANTI_ALIAS_FLAG);
            token.setColor(0xFFF8F0DC);
            canvas.drawCircle(x, tokenY, 40f, token);
            token.setColor(0xFFBEA879);
            token.setStyle(Paint.Style.STROKE);
            token.setStrokeWidth(4f);
            canvas.drawCircle(x, tokenY, 39f, token);

            Paint numerals = new Paint(Paint.ANTI_ALIAS_FLAG);
            numerals.setTextAlign(Paint.Align.CENTER);
            numerals.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            numerals.setTextSize(57f);
            numerals.setColor((chitValue == 6 || chitValue == 8) ? 0xFFB92F2C : 0xFF473C32);
            canvas.drawText(Integer.toString(chitValue), x, tokenY + 19f, numerals);

            // On a landscape phone the board is scaled down substantially.
            // Put the resource in a high-contrast, labelled emblem rather than
            // a tiny 36px symbol that is difficult to recognise after scaling.
            Paint emblem = new Paint(Paint.ANTI_ALIAS_FLAG);
            emblem.setColor(0xEEFFF5DB);
            canvas.drawRoundRect(x - 69f, y - 164f, x + 69f, y - 13f,
                    24f, 24f, emblem);
            emblem.setStyle(Paint.Style.STROKE);
            emblem.setStrokeWidth(4f);
            emblem.setColor(0xFF4D3F2F);
            canvas.drawRoundRect(x - 69f, y - 164f, x + 69f, y - 13f,
                    24f, 24f, emblem);

            Drawable resource = context.getDrawable(resourceDrawables[terrain]);
            if (resource != null) {
                // 100x100 virtual pixels, nearly three times the previous size.
                resource.setBounds(x - 50, y - 156, x + 50, y - 56);
                resource.draw(canvas);
            }

            Paint tag = new Paint(Paint.ANTI_ALIAS_FLAG);
            tag.setColor(0xFF33483F);
            canvas.drawRoundRect(x - 65f, y - 47f, x + 65f, y - 17f,
                    10f, 10f, tag);
            tag.setColor(Color.WHITE);
            tag.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            tag.setTextAlign(Paint.Align.CENTER);
            tag.setTextSize(23f);
            canvas.drawText(resourceLabels[terrain], x, y - 25f, tag);
        }

        if (isRobber) {
            Drawable robber = context.getDrawable(R.drawable.robber);
            if (robber != null) {
                robber.setBounds(x - 65, y - 103, x + 65, y + 27);
                robber.draw(canvas);
            }
        }

        if (debugMode) {
            Paint debug = new Paint(Paint.ANTI_ALIAS_FLAG);
            debug.setColor(Color.BLACK);
            debug.setTextSize(22f);
            canvas.drawText("id: " + hexagonId, x - 64f, y - 92f, debug);
        }
    }

    /**
     * calculateHexagonPoints() generates an array of points (x, y) for the corners of a hexagon
     *
     * @param x    - x position
     * @param y    - y position
     * @param size - size, measured from center to a corner
     * @return int[][]
     */
    public int[][] calculateHexagonPoints(int x, int y, int size) {
        int[][] points = new int[6][2];

        for (int i = 0; i < 6; i++) {
            double angle_deg = 60 * i - 30;
            double angle_rad = Math.PI / 180 * angle_deg;

            points[i][0] = (int) (x + size * Math.cos(angle_rad));
            points[i][1] = (int) (y + size * Math.sin(angle_rad));
        }
        this.points = points;
        return points;
    }

    /**
     * createHexagonPath() creates a Path object from given hexagon corner x and y values
     *
     * @param corners - 2d array of x and y cords for the corners
     * @return Path
     */
    public Path createHexagonPath(int[][] corners) {
        this.hexagonPath = new Path();
        this.hexagonPath.moveTo(corners[0][0], corners[0][1]);

        for (int i = 1; i < corners.length; i++) {
            this.hexagonPath.lineTo(corners[i][0], corners[i][1]);
        }
        this.hexagonPath.close();
        return this.hexagonPath;
    }

    /**
     * @return Array of the corner points of a hexagon.
     */
    public int[][] getHexagonPoints() {
        return this.points;
    }
}
