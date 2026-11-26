package ru.nsu.ccfit.zuev.osuplusplus.menu;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

import org.anddev.andengine.entity.sprite.batch.ColoredSpriteBatch;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Main-menu audio visualiser, ported from osu!lazer's
 * {@code osu.Game/Screens/Menu/LogoVisualisation}.
 *
 * <p>lazer draws 200 bars around the logo and repeats that ring five times
 * (1000 bars total) with additive blending and 20% white. Drawing those as
 * individual AndEngine sprites would create a thousand scene children, so the
 * same geometry is submitted through one {@link ColoredSpriteBatch}. The
 * texture is a 1x1 white pixel, exactly like lazer's {@code WhitePixel}.</p>
 */
public final class MenuVisualizer extends ColoredSpriteBatch {

    /** Bars in one full rotation, lazer's {@code bars_per_visualiser}. */
    public static final int BARS = 200;
    /** Overlapping rotations, lazer's {@code visualiser_rounds}. */
    private static final int ROUNDS = 5;
    private static final int CAPACITY = BARS * ROUNDS;

    /** lazer's {@code bar_length}. */
    private static final float BAR_LENGTH = 600f;
    /** lazer's {@code amplitude_dead_zone} = 1 / bar_length. */
    private static final float AMPLITUDE_DEAD_ZONE = 1f / BAR_LENGTH;
    /** lazer's menu visualiser alpha: container 0.5 * transparent white 0.2. */
    private static final float ALPHA = 0.1f;
    /** lazer's OsuLogo.SCALE_ADJUST, applied to both logo and visualiser. */
    public static final float LOGO_SCALE_ADJUST = 0.94f;

    private static final float[] COS = new float[BARS];
    private static final float[] SIN = new float[BARS];
    private static final float BAR_WIDTH_FACTOR;

    static {
        for (int i = 0; i < BARS; i++) {
            double angle = Math.toRadians(i * 360.0 / BARS);
            COS[i] = (float) Math.cos(angle);
            SIN[i] = (float) Math.sin(angle);
        }
        // sqrt(2 * (1 - cos(2*pi/bars))) / 2, the tangential width lazer uses.
        BAR_WIDTH_FACTOR = (float) Math.sin(Math.PI / BARS);
    }

    private static TextureRegion whitePixel;

    /** Amplitudes written by the update thread, read by the draw thread. */
    private volatile float[] audioData = new float[BARS];
    private float[] spareData = new float[BARS];
    private float diameter;
    private float barR = 1f;
    private float barG = 1f;
    private float barB = 1f;

    public MenuVisualizer() {
        // SpriteBatch.setIndex() rejects the index exactly equal to capacity,
        // while submit() needs setIndex(CAPACITY) after all bars were drawn.
        super(whitePixel().getTexture(), CAPACITY + 1);
        // lazer: Blending = BlendingParameters.Additive.
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        setIgnoreUpdate(true);
    }

    /** Size of the logo the ring is wrapped around, in screen pixels. */
    public void setLogoSize(float logoSize) {
        this.diameter = logoSize * LOGO_SCALE_ADJUST;
    }

    /** lazer's SeasonalMenuLogoVisualisation uses the ambient seasonal colour. */
    public void setBarColor(float r, float g, float b) {
        barR = r;
        barG = g;
        barB = b;
    }

    /** Snapshot amplitudes from the update thread without allocating. */
    public void setAmplitudes(float[] amplitudes) {
        float[] target = spareData;
        int count = Math.min(BARS, amplitudes.length);
        System.arraycopy(amplitudes, 0, target, 0, count);
        for (int i = count; i < BARS; i++) {
            target[i] = 0f;
        }
        spareData = audioData;
        audioData = target;
    }

    @Override
    protected boolean onUpdateSpriteBatch() {
        setIndex(0);
        resetColors();

        if (diameter <= 0f || !isVisible()) {
            return true;
        }

        final float[] amplitudes = audioData;
        final float radius = diameter * 0.5f;
        final float halfWidth = diameter * BAR_WIDTH_FACTOR * 0.5f;
        int rect = 0;

        for (int round = 0; round < ROUNDS; round++) {
            // 360 / 5 degrees equals 40 bars of the 200-bar ring.
            final int offset = round * (BARS / ROUNDS);

            for (int i = 0; i < BARS && rect < CAPACITY; i++) {
                float amplitude = amplitudes[i];
                if (amplitude < AMPLITUDE_DEAD_ZONE) {
                    continue;
                }
                if (amplitude > 1f) {
                    amplitude = 1f;
                }

                int index = i + offset;
                if (index >= BARS) {
                    index -= BARS;
                }

                float cos = COS[index];
                float sin = SIN[index];

                float centerX = cos * radius;
                float centerY = sin * radius;

                // Tangent half-offset and radial bar length, as in lazer.
                float bottomX = -sin * halfWidth;
                float bottomY = cos * halfWidth;
                float amplitudeX = cos * BAR_LENGTH * amplitude;
                float amplitudeY = sin * BAR_LENGTH * amplitude;

                drawWithoutChecks(whitePixel,
                        centerX - bottomX, centerY - bottomY,
                        centerX - bottomX + amplitudeX, centerY - bottomY + amplitudeY,
                        centerX + bottomX, centerY + bottomY,
                        centerX + bottomX + amplitudeX, centerY + bottomY + amplitudeY);

                putColor(rect++, barR, barG, barB, ALPHA);
            }
        }

        setIndex(rect);
        return true;
    }

    private static synchronized TextureRegion whitePixel() {
        if (whitePixel == null) {
            BitmapTextureAtlas atlas = new BitmapTextureAtlas(1, 1, TextureOptions.NEAREST);
            WhitePixelSource source = new WhitePixelSource();
            TextureRegion region = TextureRegionFactory.createFromSource(atlas, source, 0, 0, false);
            ResourceManager.getInstance().getEngine().getTextureManager().loadTexture(atlas);
            whitePixel = region;
        }
        return whitePixel;
    }

    private static final class WhitePixelSource extends BaseTextureAtlasSource
            implements IBitmapTextureAtlasSource {

        WhitePixelSource() {
            super(0, 0);
        }

        @Override
        public int getWidth() {
            return 1;
        }

        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public Bitmap onLoadBitmap(Config pBitmapConfig) {
            Bitmap bitmap = Bitmap.createBitmap(1, 1, pBitmapConfig);
            bitmap.eraseColor(Color.WHITE);
            return bitmap;
        }

        @Override
        public WhitePixelSource deepCopy() {
            return new WhitePixelSource();
        }
    }
}
