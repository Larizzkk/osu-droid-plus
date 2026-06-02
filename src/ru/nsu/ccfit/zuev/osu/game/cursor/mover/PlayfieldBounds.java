package ru.nsu.ccfit.zuev.osu.game.cursor.mover;

import android.graphics.PointF;
import ru.nsu.ccfit.zuev.osu.Config;

/**
 * Playfield boundaries for cursor movement
 */
public class PlayfieldBounds {
    
    // Standard osu! playfield dimensions
    private static final float PLAYFIELD_WIDTH = 512f;
    private static final float PLAYFIELD_HEIGHT = 384f;
    private static final float OFFSET_X = (Config.getRES_WIDTH() - PLAYFIELD_WIDTH) / 2f;
    private static final float OFFSET_Y = (Config.getRES_HEIGHT() - PLAYFIELD_HEIGHT) / 2f;
    
    /**
     * Clamp position to playfield boundaries
     */
    public static PointF clampToPlayfield(PointF pos) {
        float x = Math.max(OFFSET_X, Math.min(OFFSET_X + PLAYFIELD_WIDTH, pos.x));
        float y = Math.max(OFFSET_Y, Math.min(OFFSET_Y + PLAYFIELD_HEIGHT, pos.y));
        return new PointF(x, y);
    }
    
    /**
     * Get playfield center in real coordinates
     */
    public static PointF getPlayfieldCenter() {
        return new PointF(
            Config.getRES_WIDTH() / 2f,
            Config.getRES_HEIGHT() / 2f
        );
    }
}
