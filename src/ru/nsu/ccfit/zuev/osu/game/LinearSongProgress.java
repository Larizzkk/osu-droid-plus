package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.framework.Color4;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Utils;

public class LinearSongProgress extends GameObject {
    private final Rectangle progressRect;
    private final Rectangle bgRect;
    private float time;
    private float startTime;
    private float passedTime;
    private float initialPassedTime;

    /** Receives the seek target in ms when the user releases the bar. */
    public interface SeekListener {
        void onSeek(float time);
    }

    private SeekListener seekListener;
    private boolean seeking;
    private float seekTargetTime;

    public LinearSongProgress(final Scene scene, final float time, final float startTime, final PointF pos) {
        this(scene, time, startTime, pos, Utils.toRes(300), Utils.toRes(7));
    }

    public LinearSongProgress(final Scene scene, final float time, final float startTime, final PointF pos, float width, float height) {
        this.time = time;
        this.startTime = startTime;

        bgRect = new Rectangle(pos.x, pos.y, width, height) {
            @Override
            public boolean onAreaTouched(final TouchEvent pEvent, final float pLocalX, final float pLocalY) {
                return handleSeekTouch(pEvent);
            }
        };
        bgRect.setColor(0, 0, 0, 0.3f);
        scene.attachChild(bgRect);
        scene.registerTouchArea(bgRect);

        progressRect = new Rectangle(bgRect.getX(), bgRect.getY(), 0,
                bgRect.getHeight());
        progressRect.setColor(153f / 255f, 204f / 255f, 51f / 255f);
        scene.attachChild(progressRect);
    }


    @Override
    public void update(final float dt) {
        if (seeking) {
            return;
        }
        if (passedTime >= startTime) {
            passedTime = Math.min(time, passedTime + dt);
            progressRect.setWidth(bgRect.getWidth() * (passedTime - startTime)
                    / (time - startTime));
        } else {
            passedTime = Math.min(startTime, passedTime + dt);
            progressRect.setWidth(bgRect.getWidth() * (passedTime - initialPassedTime) / (startTime - initialPassedTime));
            if (passedTime >= startTime) {
                progressRect.setColor(1, 1, 150f / 255f);
            }
        }
    }

    public void setTime(float time) {
        this.time = time;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public void setPassedTime(float passedTime) {
        if (seeking) {
            return;
        }
        this.passedTime = passedTime;
    }

    public void setInitialPassedTime(float initialPassedTime) {
        this.initialPassedTime = initialPassedTime;
    }

    public void setProgressRectColor(Color4 color) {
        ComponentsKt.setColor4(progressRect, color);
    }

    public void setProgressRectAlpha(float alpha) {
        this.progressRect.setAlpha(alpha);
    }

    public void setSeekListener(SeekListener listener) {
        this.seekListener = listener;
    }

    public boolean isSeeking() {
        return seeking;
    }

    /**
     * Touch handling for the bar: the preview bar follows the finger while dragging,
     * the actual seek fires once on release. Coordinates are scene-space, so the ratio
     * is computed against the track bounds regardless of which rect is hit.
     */
    private boolean handleSeekTouch(final TouchEvent pEvent) {
        if (time <= 0 || seekListener == null) {
            return false;
        }

        final float ratio = Math.max(0f, Math.min(1f,
            (pEvent.getX() - bgRect.getX()) / bgRect.getWidth()));

        if (pEvent.isActionDown()) {
            seeking = true;
            seekTargetTime = startTime + ratio * (time - startTime);
            applyProgress(seekTargetTime);
            return true;
        }
        if (seeking && pEvent.isActionMove()) {
            seekTargetTime = startTime + ratio * (time - startTime);
            applyProgress(seekTargetTime);
            return true;
        }
        if (seeking && (pEvent.isActionUp() || pEvent.isActionCancel())) {
            seeking = false;
            applyProgress(seekTargetTime);
            seekListener.onSeek(seekTargetTime);
            return true;
        }
        return seeking;
    }

    private void applyProgress(float targetTime) {
        passedTime = targetTime;
        progressRect.setWidth(bgRect.getWidth() * (passedTime - startTime)
                / (time - startTime));
    }
}
