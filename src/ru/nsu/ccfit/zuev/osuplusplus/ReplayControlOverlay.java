package ru.nsu.ccfit.zuev.osuplusplus;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.text.ChangeableText;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;

public class ReplayControlOverlay extends Entity {

    private Rectangle speedBtnBg;
    private ChangeableText speedLabel;
    private Rectangle timeBg;
    private ChangeableText timeText;
    private Rectangle seekBarBg;
    private Rectangle seekBarProgress;
    private Rectangle seekBarHandle;
    private float currentSpeed = 1f;
    private float totalDuration = 0;
    private boolean visible = false;
    private float seekTarget = 0;

    private OnSpeedChangeListener speedListener;
    private OnSeekListener seekListener;

    public interface OnSpeedChangeListener { void onSpeedChanged(float speed); }
    public interface OnSeekListener { void onSeek(float timeSeconds); }

    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};

    public ReplayControlOverlay() { setVisible(false); }

    public void build() {
        float w = Config.getRES_WIDTH();
        float h = Config.getRES_HEIGHT();

        // Speed button - top-left, small
        float btnSize = 56;
        float margin = 8;
        speedBtnBg = new Rectangle(margin, h - btnSize - margin - 100, btnSize, btnSize);
        speedBtnBg.setColor(0, 0, 0, 0.5f);
        attachChild(speedBtnBg);

        speedLabel = new ChangeableText(margin + 8, h - btnSize - margin - 100 + 15,
            ResourceManager.getInstance().getFont("font"), "1.0x", 10);
        speedLabel.setColor(1, 1, 0);
        attachChild(speedLabel);

        // Time display - bottom center
        timeBg = new Rectangle(60, h - 100, w - 120, 28);
        timeBg.setColor(0, 0, 0, 0.4f);
        attachChild(timeBg);

        timeText = new ChangeableText(70, h - 97,
            ResourceManager.getInstance().getFont("smallFont"), "0:00 / 0:00", 24);
        timeText.setColor(0.9f, 0.9f, 0.9f);
        attachChild(timeText);

        // Seek bar
        float seekY = h - 65;
        float seekH = 14;
        seekBarBg = new Rectangle(20, seekY, w - 40, seekH);
        seekBarBg.setColor(0.2f, 0.2f, 0.2f, 0.6f);
        attachChild(seekBarBg);

        seekBarProgress = new Rectangle(20, seekY, 0, seekH);
        seekBarProgress.setColor(93f/255f, 169f/255f, 233f/255f, 0.9f);
        attachChild(seekBarProgress);

        seekBarHandle = new Rectangle(20, seekY - 4, 4, seekH + 8);
        seekBarHandle.setColor(1, 1, 1, 1);
        attachChild(seekBarHandle);
    }

    public void toggle() { visible = !visible; setVisible(visible); }
    public void show() { visible = true; setVisible(true); }
    public void hide() { visible = false; setVisible(false); }
    public boolean isVisible() { return visible; }
    public void setTotalDuration(float s) { totalDuration = s; }

    public void updateTime(float currentSeconds) {
        if (timeText != null)
            timeText.setText(formatTime(currentSeconds) + " / " + formatTime(totalDuration));
        if (totalDuration > 0) {
            float frac = currentSeconds / totalDuration;
            float w = seekBarBg.getWidth();
            float x = seekBarBg.getX();
            float pw = w * Math.min(1, Math.max(0, frac));
            seekBarProgress.setWidth(pw);
            seekBarHandle.setPosition(x + pw - 2, seekBarHandle.getY());
        }
    }

    public void setSpeed(float speed) {
        currentSpeed = speed;
        if (speedLabel != null) speedLabel.setText(String.format("%.1fx", speed));
    }

    private String formatTime(float seconds) {
        int s = Math.max(0, (int) seconds);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    public boolean handleTouch(float tx, float ty) {
        if (!visible) return false;
        float w = Config.getRES_WIDTH();
        float h = Config.getRES_HEIGHT();

        // Speed button
        float btnSize = 56;
        float margin = 8;
        float btnX = margin;
        float btnY = h - btnSize - margin - 100;
        if (tx >= btnX && tx <= btnX + btnSize && ty >= btnY && ty <= btnY + btnSize) {
            cycleSpeed();
            return true;
        }

        // Seek bar
        float seekY = h - 65;
        float seekH = 14;
        if (ty >= seekY - 10 && ty <= seekY + seekH + 10 && tx >= 20 && tx <= w - 20) {
            float frac = (tx - 20) / (w - 40);
            frac = Math.max(0, Math.min(1, frac));
            if (seekListener != null) seekListener.onSeek(frac * totalDuration);
            return true;
        }

        return false;
    }

    private void cycleSpeed() {
        int idx = -1;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - currentSpeed) < 0.01f) { idx = i; break; }
        }
        currentSpeed = SPEEDS[(idx + 1) % SPEEDS.length];
        if (speedLabel != null) speedLabel.setText(String.format("%.1fx", currentSpeed));
        if (speedListener != null) speedListener.onSpeedChanged(currentSpeed);
    }

    public void setOnSpeedChangeListener(OnSpeedChangeListener l) { speedListener = l; }
    public void setOnSeekListener(OnSeekListener l) { seekListener = l; }
}
