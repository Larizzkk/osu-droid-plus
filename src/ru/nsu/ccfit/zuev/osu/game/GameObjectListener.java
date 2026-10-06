package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.osudroid.game.Cursor;
import com.reco1l.framework.Color4;
import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.gameplay.GameplayHitSampleInfo;

import java.util.BitSet;
import java.util.List;

public interface GameObjectListener {

    int SLIDER_START = 1, SLIDER_REPEAT = 2, SLIDER_END = 3, SLIDER_TICK = 4;

    void onCircleHit(int id, float accuracy, PointF pos, boolean endCombo, byte forcedScore, Color4 color);

    void onSliderHit(int id, int score, PointF judgementPos,
                     boolean endCombo, Color4 color, int type, boolean incrementCombo);

    void onSliderEnd(int id, int accuracy, BitSet tickSet);

    void onSpinnerStart(int id);

    void onSpinnerHit(int id, int score, boolean endCombo, int totalScore);

    void onSpinnerEnd(int id);

    void addObject(GameObject object);

    void removeObject(GameObject object);

    boolean isObjectHittable(GameObject object);

    Cursor getCursor(int index);

    int getCursorsCount();

    void registerAccuracy(double acc);
    
    void updateAutoBasedPos(float pX, float pY);

    void onTrackingSliders(boolean isTrackingSliders);

    void onUpdatedAutoCursor(float pX, float pY);

    void playHitSamples(List<GameplayHitSampleInfo> samples);

    /**
     * Resolves the combo color of the given object against the CURRENT combo palette
     * (custom colors, beatmap colors, or the skin's forceOverride palette). Used by
     * live objects after a mid-game skin hot-swap, where the palette captured at
     * init() may no longer match the freshly loaded skin.
     */
    Color4 getComboColor(HitObject hitObject);
}