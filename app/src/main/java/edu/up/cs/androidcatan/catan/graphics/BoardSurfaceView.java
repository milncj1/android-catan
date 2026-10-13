package edu.up.cs.androidcatan.catan.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

import java.io.Serializable;

/**
 * @author Alex Weininger
 * @author Andrew Lang
 * @author Daniel Borg
 * @author Niraj Mali
 * https://github.com/alexweininger/android-catan
 **/

public class BoardSurfaceView extends SurfaceView implements Serializable {
    /**
     * External Citation
     * Date: 12/1/2018
     * Problem:
     * We did not know where to even start when it came to creating data structures to represent the board, road adjacency and the hexagons. It was particularly difficult because
     * Resource:
     * https://www.academia.edu/9699475/Settlers_of_Catan_Developing_an_Implementation_of_an_Emerging_Classic_Board_Game_in_Java
     * Solution: We read this entire 50+ page PDF, we did not follow any of the data structures, but this paper gave us confidence in our own data structures, which were already similar to the ones used in the research paper.
     */

    private static final String TAG = "BoardSurfaceView";

    int size; // size of the surface view
    HexagonGrid grid; // hexagon grid for drawing the hex tiles
    private Canvas canvas; // canvas for drawing
    private boolean ready; // ready to draw?
    // The original board uses fixed game coordinates; fit them to the phone
    // without changing the game graph or making taps miss their targets.
    private static final float BOARD_LEFT = -150f;
    private static final float BOARD_TOP = -155f;
    private static final float BOARD_WIDTH = 1980f;
    private static final float BOARD_HEIGHT = 1855f;

    // Camera position is UI-only, never part of the serialised game state.
    // At 1.0 the original complete island remains visible.
    private float userZoom = 1.0f;
    private float cameraOffsetX = 0f;
    private float cameraOffsetY = 0f;
    private static final float MAX_ZOOM = 3.25f;

    public void resetCamera() {
        userZoom = 1.0f;
        cameraOffsetX = cameraOffsetY = 0f;
        invalidate();
    }

    public float getUserZoom() {
        return userZoom;
    }

    public void panBy(float pixelsX, float pixelsY) {
        cameraOffsetX += pixelsX;
        cameraOffsetY += pixelsY;
        constrainCamera();
        invalidate();
    }

    /** Pinch around the fingers' centre, keeping the same map point beneath it. */
    public void zoomBy(float factor, float focalX, float focalY) {
        if (!Float.isFinite(factor) || factor <= 0) return;
        float old = userZoom;
        userZoom = Math.max(1.0f, Math.min(MAX_ZOOM, userZoom * factor));
        float ratio = userZoom / old;
        cameraOffsetX = focalX - viewportLeft()
                - (focalX - viewportLeft() - cameraOffsetX) * ratio;
        cameraOffsetY = focalY - viewportTop()
                - (focalY - viewportTop() - cameraOffsetY) * ratio;
        constrainCamera();
        invalidate();
    }

    private void constrainCamera() {
        float contentWidth = BOARD_WIDTH * viewportScale() * userZoom;
        float contentHeight = BOARD_HEIGHT * viewportScale() * userZoom;
        // Keep the whole viewport covered in whichever dimension is zoomed.
        float maxX = Math.max(0f, (contentWidth - getWidth()) / 2f);
        float maxY = Math.max(0f, (contentHeight - getHeight()) / 2f);
        cameraOffsetX = Math.max(-maxX, Math.min(maxX, cameraOffsetX));
        cameraOffsetY = Math.max(-maxY, Math.min(maxY, cameraOffsetY));
    }

    private float viewportScale() {
        if (getWidth() <= 0 || getHeight() <= 0) return 1f;
        return Math.min(getWidth() / BOARD_WIDTH, getHeight() / BOARD_HEIGHT);
    }

    private float viewportLeft() {
        return (getWidth() - BOARD_WIDTH * viewportScale()) * 0.5f;
    }

    private float viewportTop() {
        return (getHeight() - BOARD_HEIGHT * viewportScale()) * 0.5f;
    }

    public float toBoardX(float screenX) {
        return (screenX - viewportLeft() - cameraOffsetX) / (viewportScale() * userZoom) + BOARD_LEFT;
    }

    public float toBoardY(float screenY) {
        return (screenY - viewportTop() - cameraOffsetY) / (viewportScale() * userZoom) + BOARD_TOP;
    }


    public BoardSurfaceView(Context context) {
        super(context);
        setWillNotDraw(false);
        ready = false;
    }

    public BoardSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    /**
     * draws the board on the screen
     *
     * @param canvas canvas of where to draw
     */
    public void onDraw(Canvas canvas) {

        // SurfaceView otherwise leaves uncovered areas black; always fill the
        // entire screen before fitting the original fixed-coordinate island.
        canvas.drawColor(0xFF175875);

        // check if ready
        if (!ready) {
            Log.e(TAG, "onDraw: not ready");
            return;
        }
        // check if the grid is null
        if (grid == null) {
            Log.e(TAG, "onDraw: grid is null");
            this.invalidate();
        } else {
            // draw the game
            Log.i(TAG, "onDraw: drawing grid");
            canvas.save();
            canvas.translate(viewportLeft() + cameraOffsetX, viewportTop() + cameraOffsetY);
            canvas.scale(viewportScale() * userZoom, viewportScale() * userZoom);
            canvas.translate(-BOARD_LEFT, -BOARD_TOP);
            grid.drawGameBoard(canvas);
            canvas.restore();
        }
    }

    // getters and setters

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public HexagonGrid getGrid() {
        return grid;
    }

    public void setGrid(HexagonGrid grid) {
        this.grid = grid;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    // toString
    @Override
    public String toString() {
        return "BoardSurfaceView{" + "size=" + size + ", grid=" + grid + ", canvas=" + canvas + ", ready=" + ready + '}';
    }
}
