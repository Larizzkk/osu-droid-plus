package ru.nsu.ccfit.zuev.osu;

import static com.acivev.ui.EffectKt.addFireworksWithPeriod;
import static com.acivev.ui.EffectKt.addSnowfallWithPeriod;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import com.acivev.VibratorManager;
import com.edlplan.framework.easing.Easing;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.UIScene;
import com.reco1l.andengine.shape.UIBox;
import com.reco1l.andengine.sprite.UISprite;
import com.osudroid.ui.BannerManager;
import com.osudroid.ui.BannerManager.BannerSprite;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.ui.MainMenu;

import com.osudroid.beatmaplisting.BeatmapListing;
import com.reco1l.andengine.ui.UIConfirmDialog;
import com.reco1l.framework.Color4;
import com.reco1l.osu.ui.HorizontalMessageDialog;
import com.osudroid.beatmaps.timings.EffectControlPoint;
import com.osudroid.beatmaps.timings.TimingControlPoint;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.AccelerationInitializer;
import org.anddev.andengine.entity.particle.initializer.RotationInitializer;
import org.anddev.andengine.entity.particle.initializer.VelocityInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.particle.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseBounceOut;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;
import org.anddev.andengine.util.modifier.ease.EaseQuadIn;
import ru.nsu.ccfit.zuev.osuplusplus.menu.TriangleBackground;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.game.LinearSongProgress;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplusplus.BuildConfig;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class MainScene implements IUpdateHandler {

    // Legacy osu!droid+ main-menu visualiser: 120 bars / 5 arms of 24 bars each,
    // one arm every 72 degrees, all rotating as one star. Values and update order
    // are copied 1:1 from the legacy osuplusplus MainScene.
    private static final int SPECTRUM_BAR_COUNT = 120;
    private static final double SPECTRUM_DECAY_BASE = 0.949999988079071;
    private static final float SPECTRUM_ATTACK_MULTIPLIER = 3f;
    private static final float SPECTRUM_SCALE_DEAD_ZONE = 0.01f;
    private static final float SPECTRUM_BAR_LENGTH = 600f;
    private static final int SPECTRUM_INDEX_CHANGE = 3;
    private static final long SPECTRUM_UPDATE_INTERVAL = 50;

    // osu!stable main-menu dim bars: black strips 86.4 design units tall (768-height
    // design space) at 40% opacity, flush to the top and bottom edges.
    private static final float MENU_DIM_BAR_HEIGHT = 86.4f;
    private static final float MENU_DIM_BAR_ALPHA = 0.4f;

    public LinearSongProgress progressBar;
    public BeatmapInfo beatmapInfo;
    private Context context;
    private Sprite logo, logoOverlay, background, lastBackground;

    // ── Parallax (osu!droid+ "parallaxEnabled"/"parallaxStrength") ──────────
    // The background is slightly overscanned and shifted against the finger, so
    // the shift never exposes an edge. Cost per frame: two setPosition calls.
    private boolean parallaxEnabled = false;
    private float parallaxStrength = 0.1f;
    /** Raw target in [-1..1] on both axes, from the last touch position. */
    private float parallaxTargetX = 0f, parallaxTargetY = 0f;
    /** Smoothed value actually applied, avoids jitter on fast finger moves. */
    private float parallaxOffsetX = 0f, parallaxOffsetY = 0f;
    /** Seconds since the settings were last re-read (prefs are not polled per frame). */
    private float parallaxSettingsAge = 999f;
    private boolean parallaxApplied = false;
    /** The scene background sprite ("menu-background"); owned by SpriteBackground. */
    private Sprite menuBgSprite = null;
    private float menuBgBaseY = 0f;
    private Sprite music_nowplay;
    // Music repeat toggle, 1:1 with the legacy main menu.
    private boolean musicRepeat = false;
    private Sprite musicRepeatBtn;
    private UIScene scene;
    private ChangeableText musicInfoText;
    private final Rectangle[] spectrum = new Rectangle[SPECTRUM_BAR_COUNT];
    /**
     * Orbit point of the visualiser. The whole star lives here instead of at the screen
     * centre, so it travels with the osu!cookie (the menu logo) when the logo slides
     * aside and comes back.
     */
    private Entity spectrumAnchor;
    private final float[] frequencyAmplitudes = new float[SPECTRUM_BAR_COUNT];
    private int spectrumIndexOffset = 0;
    private long lastSpectrumUpdate = 0L;
    /**
     * Visualiser settings snapshot. Config is backed by SharedPreferences, so the
     * keys are re-read twice a second instead of on every frame (this code runs at
     * full frame rate while music plays).
     */
    private float visualizerSettingsAge = 999f;
    private boolean visualizerEnabled = true;
    private int visualizerIntensityPct = 100;
    private int visualizerUpdateRate = (int) SPECTRUM_UPDATE_INTERVAL;
    private boolean visualizerKiaiDim = true;
    private int visualizerRotationSpeed = SPECTRUM_INDEX_CHANGE;
    private int visualizerDecaySpeedPct = 100;
    /** True while the bars are already collapsed, so a disabled visualiser costs nothing. */
    private boolean spectrumCleared = false;
    private LinkedList<TimingControlPoint> timingControlPoints;
    private LinkedList<EffectControlPoint> effectControlPoints;
    private TimingControlPoint currentTimingPoint;
    private EffectControlPoint currentEffectPoint;

    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;

    private final ParticleSystem[] particleSystem = new ParticleSystem[2];

    private boolean musicStarted;
    private BassSoundProvider hitsound;
    private BassSoundProvider heartbeatSound;

    // ── osu!PC (lazer) main-menu effects ────────────────────────────────────
    // MenuSideFlashes: two 200px white boxes hugging the left/right screen edges,
    // pulsed on every beat with the live channel amplitude. Kiai alternates the
    // sides; a normal downbeat flashes both.
    // Legacy osu!droid MenuSideFlashes: two white boxes hugging the screen edges,
    // pulsing with the live channel level on every beat / kiai beat.
    private Rectangle leftFlash, rightFlash;
    private boolean leftFlashActive = false;
    private static final float AMPLITUDE_DEAD_ZONE = 0.25f;
    private static final float ALPHA_MULTIPLIER = (1 - AMPLITUDE_DEAD_ZONE) / 0.55f;
    private static final float KIAI_MULTIPLIER = (1 - AMPLITUDE_DEAD_ZONE * 0.95f) / 0.8f;
    private static final float BOX_FADE_IN_TIME = 65f / 1000f;

    // KiaiFlash: one white full-screen flash on every kiai section entry,
    // fade-in 150ms to 20% alpha, fade-out 400ms (osu!stable KiaiFlash).
    private Rectangle kiaiFlashOverlay;
    private float kiaiFlashAlpha = 0f;
    private boolean kiaiFlashTriggered = false;
    private boolean wasKiai = false;
    private float kiaiFlashTimer = 0f;
    private static final float KIAI_FLASH_PEAK_ALPHA = 0.2f;
    private static final float KIAI_FLASH_FADE_IN_TIME = 0.15f;
    private static final float KIAI_FLASH_FADE_OUT_TIME = 0.4f;
    private static final float KIAI_FLASH_TOTAL_TIME = KIAI_FLASH_FADE_IN_TIME + KIAI_FLASH_FADE_OUT_TIME;

    /** Beat counter since the current timing point, used for flash alternation. */
    private long beatIndex = 0;

    /** osu!droid+ floating triangle/shape background behind the menu UI. */
    private TriangleBackground triangleBg;

    /** Clock and session time, draggable around the bottom of the menu. */
    private ChangeableText clockText;
    private ChangeableText sessionTimeText;
    private long sessionStartTime = System.currentTimeMillis();
    public long lastTouchTime = System.currentTimeMillis();
    private float clockDragOffsetX;
    private float clockDragOffsetY;

    private double bpmLength = 1000;
    private double beatPassTime = 0;
    private boolean doChange = false;
    private boolean doStop = false;
    private long lastHit = 0;
    public boolean isOnExitAnim = false;

    private boolean isMenuShowed = false;
    private boolean doMenuShow = false;
    private float showPassTime = 0;
    private float menuBarX = 0;

    private MainMenu menu;

    public void load(Context context) {
        this.context = context;
        Debug.i("Load: mainMenuLoaded()");
        VibratorManager.INSTANCE.init(context);
        scene = new UIScene();

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");

        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) tex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, tex);
            menuBgSprite = menuBg;
            menuBgBaseY = (Config.getRES_HEIGHT() - height) / 2f;
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new ColorBackground(70 / 255f, 129 / 255f,
                    252 / 255f));
        }
        lastBackground = new Sprite(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), ResourceManager.getInstance().getTexture("emptyavatar"));

        refreshParallaxSettings();
        scene.setOnSceneTouchListener((pScene, pTouchEvent) -> {
            if (!parallaxEnabled) {
                return false;
            }

            // Only the menu background moves: never swallow the event (touch areas
            // and the menu still need it), so always return false.
            if (pTouchEvent.getAction() != TouchEvent.ACTION_UP) {
                float cx = Config.getRES_WIDTH() / 2f;
                float cy = Config.getRES_HEIGHT() / 2f;

                if (cx > 0f && cy > 0f) {
                    parallaxTargetX = Math.max(-1f, Math.min(1f, (pTouchEvent.getX() - cx) / cx));
                    parallaxTargetY = Math.max(-1f, Math.min(1f, (pTouchEvent.getY() - cy) / cy));
                }
            }

            return false;
        });

        if (Config.getBoolean("snowfallEnabled", true)) {
            addSnowfallWithPeriod(scene, context);
        }
        addFireworksWithPeriod(scene, context);

        final TextureRegion logotex = ResourceManager.getInstance().getTexture("logo");
        logo = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex) {
            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (hitsound != null) {
                        hitsound.play();
                    }
                    Debug.i("logo down");
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    Debug.i("logo up");
                    Debug.i("doMenuShow " + doMenuShow + " isMenuShowed " + isMenuShowed + " showPassTime " + showPassTime);
                    if (doMenuShow && isMenuShowed) {
                        showPassTime = 20000;
                    }
                    if (!doMenuShow && !isMenuShowed && logo.getX() == (Config.getRES_WIDTH() - logo.getWidth()) / 2) {
                        doMenuShow = true;
                        showPassTime = 0;
                    }
                    Debug.i("doMenuShow " + doMenuShow + " isMenuShowed " + isMenuShowed + " showPassTime " + showPassTime);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        logoOverlay = new Sprite((float) Config.getRES_WIDTH() / 2 - (float) logotex.getWidth() / 2, (float) Config.getRES_HEIGHT() / 2 - (float) logotex.getHeight() / 2, logotex);
        logoOverlay.setScale(1.07f);
        logoOverlay.setAlpha(0.2f);

        menu = new MainMenu(this);

        UIBox box = new UIBox() {

            {
                Text versionText = new Text(10f, 2f, ResourceManager.getInstance().getFont("smallFont"), "osu!droid " + BuildConfig.VERSION_NAME);
                attachChild(versionText);

                setSize(versionText.getWidth() + 20f, versionText.getHeight() + 4f);
                setPosition(10f, Config.getRES_HEIGHT() - getHeight() - 10f);
                setColor(0f, 0f, 0f, 0.5f); // Black
                setCornerRadius(12f);
            }

            public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
                if (event.isActionUp()) {
                    new HorizontalMessageDialog()
                        .setTitle("About")
                        .setMessage(
                                "<h1>osu!droid</h1>\n" +
                                "<h5>Version " + BuildConfig.VERSION_NAME + "</h5>\n" +
                                "<p>Made by osu!droid team<br>osu! is © peppy 2007-2026</p>\n" +
                                "<br>\n" +
                                "<a href=\"https://osu.ppy.sh\">Visit official osu! website ↗</a>\n" +
                                "<br>\n" +
                                "<br>\n" +
                                "<a href=\"https://osudroid.moe\">Visit official osu!droid website ↗</a>\n" +
                                "<br>\n" +
                                "<br>\n" +
                                "<a href=\"https://discord.gg/nyD92cE\">Join the official Discord server ↗</a>\n",
                            true
                        )
                        .addButton("Changelog", dialog -> {
                            dialog.dismiss();

                            try {
                                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://osudroid.moe/changelog/latest"));
                                context.startActivity(intent);
                            } catch (Exception e) {
                                android.util.Log.e("MainScene", "Failed to load changelog", e);
                            }

                            return null;
                        })
                        .addButton("Close", dialog -> {
                            dialog.dismiss();
                            return null;
                        })
                        .show();
                }
                return true;
            }
        };
        scene.attachChild(box);

        musicRepeat = Config.getBoolean("musicRepeat", false);

        // Texture may be missing (old skins / partially built assets); the button is
        // then skipped instead of crashing the whole menu on a null texture region.
        final TextureRegion repeatTexture = ResourceManager.getInstance().getTexture("music_list");

        if (repeatTexture != null) {
            musicRepeatBtn = new Sprite(Config.getRES_WIDTH() - 50 * 7 + 35,
                    47, 40, 40, repeatTexture) {

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                             final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        setColor(0.7f, 0.7f, 0.7f);
                        return true;
                    }
                    if (pSceneTouchEvent.isActionUp()) {
                        musicRepeat = !musicRepeat;
                        if (musicRepeat) {
                            setColor(0.3f, 0.8f, 1f);
                        } else {
                            setColor(1f, 1f, 1f);
                        }
                        Config.setBoolean("musicRepeat", musicRepeat);

                        if (GlobalManager.getInstance().getSongService() != null) {
                            GlobalManager.getInstance().getSongService().setLoop(musicRepeat);
                        }
                        return true;
                    }
                    return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                            pTouchAreaLocalY);
                }
            };
        }

        if (musicRepeat) {
            if (musicRepeatBtn != null) {
                musicRepeatBtn.setColor(0.3f, 0.8f, 1f);
            }
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().setLoop(true);
            }
        }

        final Sprite music_prev = new Sprite(Config.getRES_WIDTH() - 50 * 6 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_prev")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doChange = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (lastHit == 0) {
                        lastHit = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true;
                        }
                    }
                    lastHit = System.currentTimeMillis();
                    musicControl(MusicOption.PREV);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_play = new Sprite(Config.getRES_WIDTH() - 50 * 5 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_play")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.PLAY);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_pause = new Sprite(Config.getRES_WIDTH() - 50 * 4 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_pause")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.PAUSE);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_stop = new Sprite(Config.getRES_WIDTH() - 50 * 3 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_stop")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doStop = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    musicControl(MusicOption.STOP);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        final Sprite music_next = new Sprite(Config.getRES_WIDTH() - 50 * 2 + 35,
                47, 40, 40, ResourceManager.getInstance().getTexture(
                "music_next")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doChange = true;
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    if (lastHit == 0) {
                        lastHit = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true;
                        }
                    }
                    lastHit = System.currentTimeMillis();
                    musicControl(MusicOption.NEXT);
                    return true;
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
                        pTouchAreaLocalY);
            }
        };

        musicInfoText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", HorizontalAlign.RIGHT, 35);

        final TextureRegion nptex = ResourceManager.getInstance().getTexture("music_np");
        music_nowplay = new Sprite(Utils.toRes(Config.getRES_WIDTH() - 500), 0, (float) (40 * nptex.getWidth()) / nptex.getHeight(), 40, nptex);

        // Legacy visualiser: 120 bars arranged around the osu!cookie (menu logo).
        // The bars are children of an anchor placed at the logo centre, so the star
        // moves and rotates with the cookie instead of staying at the screen centre.
        spectrumAnchor = new Entity((float) Config.getRES_WIDTH() / 2, (float) Config.getRES_HEIGHT() / 2);
        scene.attachChild(spectrumAnchor);

        for (int i = 0; i < SPECTRUM_BAR_COUNT; i++) {
            spectrum[i] = new Rectangle(0, 0, 260, 10);
            spectrum[i].setRotationCenter(0, 5);
            spectrum[i].setScaleCenter(0, 5);
            // 5 overlapping arms (osu! PC star visualiser): 24 bars per arm,
            // arms offset by 72 degrees.
            spectrum[i].setRotation((i % 24) * 15f + (i / 24) * 72f - 220f);
            spectrum[i].setAlpha(0f);

            spectrumAnchor.attachChild(spectrum[i]);
        }

        TextureRegion starRegion = ResourceManager.getInstance().getTexture("star");

        {
            particleSystem[0] = new ParticleSystem(new PointParticleEmitter(-40, (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128, starRegion);
            particleSystem[0].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            particleSystem[0].addParticleInitializer(new VelocityInitializer(150, 430, -480, -520));
            particleSystem[0].addParticleInitializer(new AccelerationInitializer(10, 30));
            particleSystem[0].addParticleInitializer(new RotationInitializer(0.0f, 360.0f));

            particleSystem[0].addParticleModifier(new ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f));
            particleSystem[0].addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f));
            particleSystem[0].addParticleModifier(new ExpireModifier(1.0f));

            particleSystem[0].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[0]);
        }

        {
            particleSystem[1] = new ParticleSystem(new PointParticleEmitter(Config.getRES_WIDTH(), (float) (Config.getRES_HEIGHT() * 3) / 4), 32, 48, 128, starRegion);
            particleSystem[1].setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            particleSystem[1].addParticleInitializer(new VelocityInitializer(-150, -430, -480, -520));
            particleSystem[1].addParticleInitializer(new AccelerationInitializer(-10, 30));
            particleSystem[1].addParticleInitializer(new RotationInitializer(0.0f, 360.0f));

            particleSystem[1].addParticleModifier(new ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f));
            particleSystem[1].addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f));
            particleSystem[1].addParticleModifier(new ExpireModifier(1.0f));

            particleSystem[1].setParticlesSpawnEnabled(false);

            scene.attachChild(particleSystem[1]);
        }

        TextureRegion beatmapDownloaderTex = ResourceManager.getInstance().getTexture("beatmap_downloader");
        Sprite beatmapDownloader = new Sprite(Config.getRES_WIDTH() - beatmapDownloaderTex.getWidth(), (Config.getRES_HEIGHT() - beatmapDownloaderTex.getHeight()) / 2f, beatmapDownloaderTex) {
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    doStop = true;
                    return true;
                }

                if (pSceneTouchEvent.isActionUp()) {
                    setColor(1, 1, 1);
                    new BeatmapListing().show();
                    return true;
                }

                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
            }
        };

        menu.getFirst().setAlpha(0f);
        menu.getSecond().setAlpha(0f);
        menu.getThird().setAlpha(0f);

        logo.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);
        logoOverlay.setPosition((Config.getRES_WIDTH() - logo.getWidth()) / 2, (Config.getRES_HEIGHT() - logo.getHeight()) / 2);

        menu.getSecond().setScale(Config.getRES_WIDTH() / 1024f);
        menu.getFirst().setScale(Config.getRES_WIDTH() / 1024f);
        menu.getThird().setScale(Config.getRES_WIDTH() / 1024f);

        menu.getSecond().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, (Config.getRES_HEIGHT() - menu.getSecond().getHeight()) / 2);
        menu.getFirst().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, menu.getSecond().getY() - menu.getFirst().getHeight() - 40 * Config.getRES_WIDTH() / 1024f);
        menu.getThird().setPosition(logo.getX() + logo.getWidth() - Config.getRES_WIDTH() / 2.5f, menu.getSecond().getY() + menu.getThird().getHeight() + 40 * Config.getRES_WIDTH() / 1024f);

        menuBarX = menu.getFirst().getX();

        scene.attachChild(lastBackground, 0);

        // Stable main-menu dim bars: they belong directly above the background and
        // below every piece of UI (logo, menu buttons, visualiser), so they are
        // inserted right after the background sprite.
        float dimBarHeight = MENU_DIM_BAR_HEIGHT * Config.getRES_HEIGHT() / 768f;

        Rectangle topDimBar = new Rectangle(0, 0, Config.getRES_WIDTH(), dimBarHeight);
        topDimBar.setColor(0f, 0f, 0f, MENU_DIM_BAR_ALPHA);
        scene.attachChild(topDimBar, 1);

        Rectangle bottomDimBar = new Rectangle(
            0,
            Config.getRES_HEIGHT() - dimBarHeight,
            Config.getRES_WIDTH(),
            dimBarHeight
        );
        bottomDimBar.setColor(0f, 0f, 0f, MENU_DIM_BAR_ALPHA);
        scene.attachChild(bottomDimBar, 2);

        scene.attachChild(logo);
        scene.attachChild(logoOverlay);
        scene.attachChild(music_nowplay);
        scene.attachChild(musicInfoText);
        if (musicRepeatBtn != null) {
            scene.attachChild(musicRepeatBtn);
        }
        scene.attachChild(music_prev);
        scene.attachChild(music_play);
        scene.attachChild(music_pause);
        scene.attachChild(music_stop);
        scene.attachChild(music_next);
        scene.attachChild(beatmapDownloader);

        scene.registerTouchArea(logo);
        scene.registerTouchArea(box);
        scene.registerTouchArea(beatmapDownloader);
        if (musicRepeatBtn != null) {
            scene.registerTouchArea(musicRepeatBtn);
        }
        scene.registerTouchArea(music_prev);
        scene.registerTouchArea(music_play);
        scene.registerTouchArea(music_pause);
        scene.registerTouchArea(music_stop);
        scene.registerTouchArea(music_next);

        if (BuildConfig.DEBUG) {
            ResourceManager.getInstance().loadHighQualityAsset("dev-build-overlay", "dev-build-overlay.png");

            UISprite debugOverlay = new UISprite(ResourceManager.getInstance().getTexture("dev-build-overlay"));
            debugOverlay.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT());
            debugOverlay.setOrigin(Anchor.BottomCenter);
            scene.attachChild(debugOverlay);

            Text debugText = new Text(0, 0, ResourceManager.getInstance().getFont("smallFont"), "DEVELOPMENT BUILD");
            debugText.setColor(1f, 237f / 255f, 0f);
            debugText.setPosition((Config.getRES_WIDTH() - debugText.getWidth()) / 2f, Config.getRES_HEIGHT() - debugOverlay.getHeight() - 1f - debugText.getHeight());

            Text debugTextShadow = new Text(0, 0, ResourceManager.getInstance().getFont("smallFont"), "DEVELOPMENT BUILD");
            debugTextShadow.setColor(0f, 0f, 0f, 0.5f);
            debugTextShadow.setPosition((Config.getRES_WIDTH() - debugText.getWidth()) / 2f + 2f, Config.getRES_HEIGHT() - debugOverlay.getHeight() - 1f - debugText.getHeight() + 2f);

            scene.attachChild(debugTextShadow);
            scene.attachChild(debugText);
        }

        progressBar = new LinearSongProgress(scene, 0, 0, new PointF(Utils.toRes(Config.getRES_WIDTH() - 320), Utils.toRes(100)));
        progressBar.setProgressRectColor(new Color4(0.9f, 0.9f, 0.9f));
        progressBar.setProgressRectAlpha(0.8f);
        progressBar.setSeekListener(time -> {
            var songService = GlobalManager.getInstance().getSongService();
            if (songService != null) {
                songService.seekTo((int) time);
            }
        });

        // Kiai flash overlay: full-screen white rectangle, kept at alpha 0 until
        // a kiai section starts.
        kiaiFlashOverlay = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        kiaiFlashOverlay.setColor(1f, 1f, 1f, 0f);
        scene.attachChild(kiaiFlashOverlay);

        initSideFlashes();

        createOnlinePanel(scene);
        scene.registerUpdateHandler(this);

        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false);
        heartbeatSound = ResourceManager.getInstance().loadSound("heartbeat", "sfx/heartbeat.ogg", false);

        // Floating shapes sit above the dim bars (index 1/2) but below every UI
        // element created later; they are deliberately very faint (alpha 0.04-0.12).
        triangleBg = new TriangleBackground();
        scene.attachChild(triangleBg, Math.min(3, scene.getChildCount()));

        createClock(scene);
    }

    /**
     * Clock and session time in the bottom area, exactly like the legacy menu:
     * white text with an invisible drag handle that saves its position.
     */
    private void createClock(UIScene scene) {
        float clockX = Config.getFloat("clockPosX", Config.getRES_WIDTH() / 2f - 30);
        float clockY = Config.getFloat("clockPosY", Config.getRES_HEIGHT() - 55);
        float sessionY = Config.getFloat("sessionPosY", clockY + 24);

        Rectangle clockDragHandle = new Rectangle(clockX - 10, clockY - 10, 140, 52) {
            @Override
            public boolean onAreaTouched(final TouchEvent pEvent, final float pLocalX, final float pLocalY) {
                switch (pEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        lastTouchTime = System.currentTimeMillis();
                        clockDragOffsetX = pEvent.getX() - clockText.getX();
                        clockDragOffsetY = pEvent.getY() - clockText.getY();
                        clockText.setAlpha(0.7f);
                        sessionTimeText.setAlpha(0.7f);
                        return true;
                    case TouchEvent.ACTION_MOVE:
                        float newX = pEvent.getX() - clockDragOffsetX;
                        float newY = pEvent.getY() - clockDragOffsetY;
                        clockText.setPosition(newX, newY);
                        sessionTimeText.setPosition(newX, newY + 24);
                        return true;
                    case TouchEvent.ACTION_UP:
                    case TouchEvent.ACTION_CANCEL:
                        clockText.setAlpha(1f);
                        sessionTimeText.setAlpha(1f);
                        Config.setFloat("clockPosX", clockText.getX());
                        Config.setFloat("clockPosY", clockText.getY());
                        Config.setFloat("sessionPosY", clockText.getY() + 24);
                        return true;
                }
                return super.onAreaTouched(pEvent, pLocalX, pLocalY);
            }
        };
        clockDragHandle.setColor(0, 0, 0);
        clockDragHandle.setAlpha(0f);
        scene.attachChild(clockDragHandle);
        scene.registerTouchArea(clockDragHandle);

        clockText = new ChangeableText(clockX, clockY,
                ResourceManager.getInstance().getFont("font"), "00:00", HorizontalAlign.LEFT, 20);
        clockText.setColor(1f, 1f, 1f);
        clockText.setAlpha(1f);
        scene.attachChild(clockText);

        sessionTimeText = new ChangeableText(clockX, sessionY,
                ResourceManager.getInstance().getFont("font"), "00:00:00", HorizontalAlign.LEFT, 12);
        sessionTimeText.setColor(1f, 1f, 1f);
        sessionTimeText.setAlpha(1f);
        scene.attachChild(sessionTimeText);
    }

    /** Legacy initSideFlashes(): two 200 px white boxes hugging the screen edges. */
    private void initSideFlashes() {
        final float boxWidth = 200f;

        leftFlash = new Rectangle(-boxWidth + 50f, 0, boxWidth, Config.getRES_HEIGHT());
        leftFlash.setColor(1f, 1f, 1f, 0f);
        scene.attachChild(leftFlash, 0);

        rightFlash = new Rectangle(Config.getRES_WIDTH() - 50f, 0, boxWidth, Config.getRES_HEIGHT());
        rightFlash.setColor(1f, 1f, 1f, 0f);
        scene.attachChild(rightFlash, 0);
    }

    /**
     * Legacy flashSide(): fade in over 65 ms to a peak derived from the channel's
     * live audio level, then fade out over one beat with quadratic easing.
     */
    private void flashSide(Rectangle flash, double beatLength, boolean kiai) {
        if (flash == null) {
            return;
        }

        var songService = GlobalManager.getInstance().getSongService();
        float[] levels = songService != null ? songService.getChannelLevel() : null;
        float channelLevel = levels != null
                ? (flash == leftFlash ? levels[0] : levels[1])
                : 0f;

        float multiplier = kiai ? KIAI_MULTIPLIER : ALPHA_MULTIPLIER;
        float targetAlpha = 0.1f + (channelLevel - AMPLITUDE_DEAD_ZONE) / multiplier;
        if (targetAlpha < 0.1f) targetAlpha = 0.1f;
        if (targetAlpha > 1f) targetAlpha = 1f;

        flash.clearEntityModifiers();
        flash.registerEntityModifier(new SequenceEntityModifier(
                new org.anddev.andengine.entity.modifier.AlphaModifier(BOX_FADE_IN_TIME, 0f, targetAlpha),
                new org.anddev.andengine.entity.modifier.AlphaModifier((float) (beatLength / 1000), targetAlpha, 0f,
                        EaseQuadIn.getInstance())
        ));
    }

    /**
     * One white flash per kiai entry (osu!stable KiaiFlash): 150ms fade-in to 20%
     * then 400ms fade-out. Runs independently of the beat pulse.
     */
    private void updateKiaiFlash(float dt, boolean kiai) {
        if (kiaiFlashOverlay == null) {
            return;
        }

        if (kiai && !wasKiai && !kiaiFlashTriggered) {
            kiaiFlashTriggered = true;
            kiaiFlashTimer = 0f;
            kiaiFlashAlpha = 0f;
        }
        if (!kiai && wasKiai) {
            kiaiFlashTriggered = false;
        }
        wasKiai = kiai;

        if (kiaiFlashTriggered) {
            kiaiFlashTimer += dt;

            if (kiaiFlashTimer < KIAI_FLASH_FADE_IN_TIME) {
                kiaiFlashAlpha = (kiaiFlashTimer / KIAI_FLASH_FADE_IN_TIME) * KIAI_FLASH_PEAK_ALPHA;
            } else if (kiaiFlashTimer < KIAI_FLASH_TOTAL_TIME) {
                float progress = (kiaiFlashTimer - KIAI_FLASH_FADE_IN_TIME) / KIAI_FLASH_FADE_OUT_TIME;
                kiaiFlashAlpha = KIAI_FLASH_PEAK_ALPHA * (1f - progress);
            } else {
                kiaiFlashAlpha = 0f;
            }
        } else {
            kiaiFlashAlpha = 0f;
        }

        kiaiFlashOverlay.setAlpha(kiaiFlashAlpha);
    }

    public void loadBannerSprite() {

        if (!Config.isStayOnline()) {
            return;
        }

        BannerSprite sprite = BannerManager.loadBannerSprite();
        if (sprite != null) {
            sprite.setPosition(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            sprite.setOrigin(Anchor.BottomRight);
            scene.attachChild(sprite);
        }
    }

    private void createOnlinePanel(UIScene scene) {
        Config.loadOnlineConfig(context);
        OnlineManager.getInstance().init();

        if (OnlineManager.getInstance().isStayOnline()) {
            Debug.i("Stay online, creating panel");
            OnlineScoring.getInstance().createPanel();
            final OnlinePanel panel = OnlineScoring.getInstance().getPanel();
            panel.setPosition(5, 5);
            scene.attachChild(panel);
            scene.registerTouchArea(panel.rect);
        }

        OnlineScoring.getInstance().login();
    }

    public void reloadOnlinePanel() {
        // IndexOutOfBoundsException 141 fix
        Execution.updateThread(() -> {
            scene.detachChild(OnlineScoring.getInstance().getPanel());
            createOnlinePanel(scene);
        });
    }

    public void musicControl(MusicOption option) {
        if (GlobalManager.getInstance().getSongService() == null || beatmapInfo == null) {
            return;
        }
        switch (option) {
            case PREV: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                }
                currentTimingPoint = null;
                LibraryManager.selectPreviousBeatmapSet();
                loadBeatmapInfo();
                loadTimingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case PLAY: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
                        loadTimingPoints(false);
                        GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getAudioPath(),
                                1f, false, musicRepeat);

                        if (currentTimingPoint != null) {
                            bpmLength = currentTimingPoint.msPerBeat;
                            beatPassTime = 0;
                        }
                    }
                    if (GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED && currentTimingPoint != null) {
                        bpmLength = currentTimingPoint.msPerBeat;
                        int position = GlobalManager.getInstance().getSongService().getPosition();
                        beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                    }

                    GlobalManager.getInstance().getSongService().play();
                    doStop = false;
                }
            }
            break;
            case PAUSE: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
                    GlobalManager.getInstance().getSongService().pause();
                    bpmLength = 1000;
                    beatPassTime = 0;
                }
            }
            break;
            case STOP: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                    bpmLength = 1000;
                    beatPassTime = 0;
                }
            }
            break;
            case NEXT: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING || GlobalManager.getInstance().getSongService().getStatus() == Status.PAUSED) {
                    GlobalManager.getInstance().getSongService().stop();
                }
                LibraryManager.selectNextBeatmapSet();
                currentTimingPoint = null;
                loadBeatmapInfo();
                loadTimingPoints(true);
                doChange = false;
                doStop = false;
            }
            break;
            case SYNC: {
                if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING && currentTimingPoint != null) {
                    int position = GlobalManager.getInstance().getSongService().getPosition();
                    beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                }
            }
        }
    }

    /**
     * Re-reads the parallax preferences. Called on scene load and then at most
     * twice a second from {@link #updateParallax(float)} — SharedPreferences must
     * not be hit every frame.
     */
    private void refreshParallaxSettings() {
        // Config holds the live value (replay panel can flip it mid-game); it is loaded
        // from the same preference, so both surfaces always agree.
        parallaxEnabled = Config.isParallaxEnabled();
        parallaxStrength = Math.max(0f, Math.min(1f, Config.getInt("parallaxStrength", 10) / 100f));
    }

    /**
     * Shifts the overscanned menu/beatmap background against the finger position.
     * Two setPosition calls per frame while enabled and moving, nothing when idle.
     */
    private void updateParallax(float dt) {
        parallaxSettingsAge += dt;
        if (parallaxSettingsAge > 0.5f) {
            parallaxSettingsAge = 0f;
            refreshParallaxSettings();
        }

        if (!parallaxEnabled || parallaxStrength <= 0f) {
            if (parallaxApplied) {
                parallaxApplied = false;
                parallaxOffsetX = 0f;
                parallaxOffsetY = 0f;
                resetParallaxSprite(background, 0f, 0f);
                resetParallaxSprite(lastBackground, 0f, 0f);
                resetParallaxSprite(menuBgSprite, 0f, menuBgBaseY);
            }
            return;
        }

        float lerp = Math.min(1f, dt * 8f);
        parallaxOffsetX += (parallaxTargetX - parallaxOffsetX) * lerp;
        parallaxOffsetY += (parallaxTargetY - parallaxOffsetY) * lerp;

        // 6% overscan is enough for the maximum shift of this strength.
        float scale = 1f + 0.06f * parallaxStrength;
        float halfW = Config.getRES_WIDTH() * (scale - 1f) * 0.5f;
        float halfH = Config.getRES_HEIGHT() * (scale - 1f) * 0.5f;

        float dx = parallaxOffsetX * halfW;
        float dy = parallaxOffsetY * halfH;
        applyParallaxSprite(background, scale, dx, dy, 0f, 0f);
        applyParallaxSprite(lastBackground, scale, dx, dy, 0f, 0f);
        applyParallaxSprite(menuBgSprite, scale, dx, dy, 0f, menuBgBaseY);
        parallaxApplied = true;
    }

    private void applyParallaxSprite(Sprite sprite, float scale, float offsetX, float offsetY,
                                     float baseX, float baseY) {
        if (sprite == null) {
            return;
        }

        if (sprite.getScaleX() != scale) {
            sprite.setScaleCenter(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
            sprite.setScale(scale);
        }

        sprite.setPosition(baseX + offsetX, baseY + offsetY);
    }

    private void resetParallaxSprite(Sprite sprite, float baseX, float baseY) {
        if (sprite == null) {
            return;
        }

        if (sprite.getScaleX() != 1f) {
            sprite.setScaleCenter(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
            sprite.setScale(1f);
        }

        sprite.setPosition(baseX, baseY);
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        updateParallax(pSecondsElapsed);
        beatPassTime += pSecondsElapsed * 1000;
        if (isOnExitAnim) {
            return;
        }

        // Kiai flash runs every frame (not just while playing) so a flash that
        // started right before a pause still fades out instead of sticking.
        updateKiaiFlash(pSecondsElapsed,
                (currentEffectPoint != null && currentEffectPoint.isKiai) || isContinuousKiai);

        if (GlobalManager.getInstance().getSongService() == null || !musicStarted || GlobalManager.getInstance().getSongService().getStatus() == Status.STOPPED) {
            bpmLength = 1000;
        }

        if (doMenuShow && !isMenuShowed) {
            logo.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));
            logoOverlay.registerEntityModifier(new MoveXModifier(0.3f, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, EaseExponentialOut.getInstance()));

            menu.attachButtons();
            menu.showFirstMenu();

            for (var button : menu.getButtons()) {
                button.clearEntityModifiers();
                button.setX(menuBarX - 100);
                button.setAlpha(0f);

                button.beginModifierSequence(sequence -> sequence
                        .moveToX(menuBarX, 0.5f, Easing.OutElastic)
                        .fadeTo(0.9f, 0.5f, Easing.OutCubic));
            }

            isMenuShowed = true;
        }

        if (doMenuShow) {
            if (showPassTime > 10000f) {

                menu.showFirstMenu();

                for (var button : menu.getButtons()) {
                    // Do not allow the button to be pressed while it is disappearing.
                    scene.unregisterTouchArea(button);

                    button.clearEntityModifiers();
                    button.setX(menuBarX);
                    button.setAlpha(0.9f);

                    button.beginModifierSequence(sequence -> sequence
                            .moveToX(menuBarX - 50, 1f, Easing.OutExpo)
                            .fadeOut(1f, Easing.OutExpo)
                            .after(IEntity::detachSelf));
                }

                logo.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2,
                        EaseBounceOut.getInstance()));
                logoOverlay.registerEntityModifier(new MoveXModifier(1f, (float) Config.getRES_WIDTH() / 3 - logo.getWidth() / 2, (float) Config.getRES_WIDTH() / 2 - logo.getWidth() / 2, EaseBounceOut.getInstance()));

                isMenuShowed = false;
                doMenuShow = false;
                showPassTime = 0;
            } else {
                showPassTime += pSecondsElapsed * 1000f;
            }
        }

        if (beatPassTime >= bpmLength) {
            beatPassTime %= bpmLength;

            if (logo != null) {
                logo.registerEntityModifier(new SequenceEntityModifier(new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.9f), 1f, 1.07f),
                        new org.anddev.andengine.entity.modifier.ScaleModifier((float) (bpmLength / 1000 * 0.07f), 1.07f, 1f)));

                if (heartbeatSound != null) {
                    heartbeatSound.play(Config.getInt("heartbeatVolume", 100) / 100f);
                }
            }

            if (currentTimingPoint != null) {
                boolean kiai = (currentEffectPoint != null && currentEffectPoint.isKiai) || isContinuousKiai;

                // Legacy behaviour: kiai alternates the sides every beat, a normal
                // downbeat (beat 0 of the measure) lights both sides.
                if (kiai) {
                    if (beatIndex % 2 == 0) {
                        flashSide(leftFlash, bpmLength, true);
                    } else {
                        flashSide(rightFlash, bpmLength, true);
                    }
                } else if (currentTimingPoint.timeSignature > 0
                        && beatIndex % currentTimingPoint.timeSignature == 0) {
                    flashSide(leftFlash, bpmLength, false);
                    flashSide(rightFlash, bpmLength, false);
                }

                beatIndex++;
            }

            if (triangleBg != null) {
                triangleBg.onBeat();
            }
        }

        if (GlobalManager.getInstance().getSongService() != null) {
            if (!musicStarted) {
                if (currentTimingPoint == null) {
                    return;
                }

                bpmLength = currentTimingPoint.msPerBeat;
                beatPassTime = 0;
                beatIndex = 0;
                progressBar.setStartTime(0);
                GlobalManager.getInstance().getSongService().play();
                GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
//				ToastLogger.showText("BPM: " + 60 / bpmLength * 1000 + " Offset: " + offset, false);
                musicStarted = true;
            }

            if (GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING) {
//                syncPassedTime += pSecondsElapsed * 1000f;
                int position = GlobalManager.getInstance().getSongService().getPosition();
                progressBar.setTime(GlobalManager.getInstance().getSongService().getLength());
                progressBar.setPassedTime(position);
                progressBar.update(pSecondsElapsed * 1000);

                if (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                    while (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                        currentTimingPoint = timingControlPoints.pop();
                    }

                    bpmLength = currentTimingPoint.msPerBeat;
                    beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                }

                // The looping channel jumps back to the start, so rewind the timing
                // and effect point queues, 1:1 with the legacy menu.
                if (musicRepeat && position < 1000 && timingControlPoints.isEmpty()) {
                    try {
                        var beatmap = BeatmapCache.getBeatmap(beatmapInfo, false);
                        timingControlPoints = new LinkedList<>(beatmap.getControlPoints().timing.controlPoints);
                        effectControlPoints = new LinkedList<>(beatmap.getControlPoints().effect.controlPoints);
                        currentTimingPoint = beatmap.getControlPoints().timing.defaultControlPoint;
                        currentEffectPoint = beatmap.getControlPoints().effect.defaultControlPoint;
                        bpmLength = currentTimingPoint.msPerBeat;
                        beatPassTime = (position - currentTimingPoint.time) % bpmLength;
                        beatIndex = 0;
                    } catch (Exception e) {
                        Debug.e("Failed to reload timing points on repeat loop: " + e);
                    }
                }

                if (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                    while (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                        currentEffectPoint = effectControlPoints.pop();
                    }

                    if (!isContinuousKiai && currentEffectPoint.isKiai) {
                        for (ParticleSystem particleSpout : particleSystem) {
                            particleSpout.setParticlesSpawnEnabled(true);
                        }
                        particleBeginTime = position;
                        particleEnabled = true;

                        if (triangleBg != null) {
                            triangleBg.setKiai(true);
                        }
                    }

                    isContinuousKiai = currentEffectPoint.isKiai;
                }

                if (particleEnabled && (position - particleBeginTime > 2000)) {
                    for (ParticleSystem particleSpout : particleSystem) {
                        particleSpout.setParticlesSpawnEnabled(false);
                    }
                    particleEnabled = false;
                }

                // Anchor the visualiser to the osu!cookie for this frame. The logo
                // slides to the left while the menu is open and bounces back on its
                // own, and it rotates during the exit animation.
                if (spectrumAnchor != null && logo != null) {
                    spectrumAnchor.setPosition(
                            logo.getX() + logo.getWidth() / 2f,
                            logo.getY() + logo.getHeight() / 2f);
                    spectrumAnchor.setRotation(logo.getRotation());
                }

                visualizerSettingsAge += pSecondsElapsed;
                if (visualizerSettingsAge >= 0.5f) {
                    visualizerSettingsAge = 0f;
                    visualizerEnabled = Config.getBoolean("visualizerEnabled", true);
                    visualizerIntensityPct = Config.getInt("visualizerIntensity", 100);
                    visualizerUpdateRate = Config.getInt("visualizerUpdateRate", (int) SPECTRUM_UPDATE_INTERVAL);
                    visualizerKiaiDim = Config.getBoolean("visualizerKiaiDim", true);
                    visualizerRotationSpeed = Config.getInt("visualizerRotationSpeed", SPECTRUM_INDEX_CHANGE);
                    visualizerDecaySpeedPct = Config.getInt("visualizerDecaySpeed", 100);
                }

                if (!visualizerEnabled) {
                    if (!spectrumCleared) {
                        spectrumCleared = true;
                        for (Rectangle specRectangle : spectrum) {
                            specRectangle.setWidth(0);
                            specRectangle.setAlpha(0);
                        }
                    }
                } else {
                    spectrumCleared = false;
                    // 1:1 with the legacy osuplusplus MainScene visualiser.
                    float[] fft = GlobalManager.getInstance().getSongService().getSpectrum();
                    long nowMs = System.currentTimeMillis();

                    float visualizerIntensity = visualizerIntensityPct / 100f;

                    if (fft != null
                            && nowMs - lastSpectrumUpdate >= visualizerUpdateRate) {
                        lastSpectrumUpdate = nowMs;

                        boolean isKiaiTime = (currentEffectPoint != null && currentEffectPoint.isKiai) || isContinuousKiai;
                        float kiaiMultiplier = (isKiaiTime || !visualizerKiaiDim) ? 1f : 0.5f;

                        final int windowSize = 240;
                        for (int i = 0, leftBound = 0; i < SPECTRUM_BAR_COUNT; i++) {
                            float peak = 0f;
                            int rightBound = (int) Math.pow(2., (i * 9.) / (windowSize - 1));
                            if (rightBound <= leftBound) rightBound = leftBound + 1;
                            if (rightBound > 511) rightBound = 511;
                            for (; leftBound < rightBound && 1 + leftBound < fft.length; leftBound++) {
                                float value = fft[1 + leftBound];
                                if (value > peak) peak = value;
                            }

                            float targetAmplitude = peak
                                    * SPECTRUM_ATTACK_MULTIPLIER
                                    * kiaiMultiplier
                                    * visualizerIntensity;

                            int idx = (i + spectrumIndexOffset) % SPECTRUM_BAR_COUNT;
                            if (targetAmplitude > frequencyAmplitudes[idx]) {
                                frequencyAmplitudes[idx] = targetAmplitude;
                            }
                        }

                        spectrumIndexOffset = (spectrumIndexOffset + visualizerRotationSpeed) % SPECTRUM_BAR_COUNT;
                    }

                    float decaySpeed = visualizerDecaySpeedPct / 100f;
                    float decayFactor = (float) Math.pow(
                            SPECTRUM_DECAY_BASE,
                            (pSecondsElapsed * 1000f * decaySpeed) / 16.666666666666668);

                    for (int i = 0; i < SPECTRUM_BAR_COUNT; i++) {
                        float scale = frequencyAmplitudes[i] * decayFactor;

                        if (scale < SPECTRUM_SCALE_DEAD_ZONE) {
                            frequencyAmplitudes[i] = 0f;
                            spectrum[i].setWidth(250f);
                            spectrum[i].setAlpha(0f);
                            continue;
                        }

                        frequencyAmplitudes[i] = scale;
                        spectrum[i].setWidth(250f + scale * SPECTRUM_BAR_LENGTH);
                        spectrum[i].setAlpha(Math.max(0f, 0.4f * Math.min(1f, (scale - 0.04f) / 0.08f)));
                    }
                }
            } else {
                if (!spectrumCleared) {
                    spectrumCleared = true;
                    for (Rectangle specRectangle : spectrum) {
                        specRectangle.setWidth(0);
                        specRectangle.setAlpha(0);
                    }
                }

                final var songService = GlobalManager.getInstance().getSongService();

                if (musicRepeat) {
                    // Fallback for players whose loop flag is not honoured by the
                    // audio backend: restart the track manually when it ends.
                    if (!doChange && !doStop && songService != null
                            && songService.getStatus() == Status.STOPPED) {
                        songService.seekTo(1);
                        songService.play();
                    }
                } else if (!doChange && !doStop && songService != null
                        && songService.getPosition() >= songService.getLength()) {
                    musicControl(MusicOption.NEXT);
                }
            }
        }

        // Clock and session time, updated every frame like in the legacy menu.
        if (clockText != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String min = String.valueOf(cal.get(java.util.Calendar.MINUTE));
            if (min.length() < 2) min = "0" + min;
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            String hourStr = String.valueOf(hour);
            if (hourStr.length() < 2) hourStr = "0" + hourStr;
            clockText.setText(hourStr + ":" + min);
        }

        if (sessionTimeText != null) {
            long elapsed = System.currentTimeMillis() - sessionStartTime;
            long sec = (elapsed / 1000) % 60;
            long min = (elapsed / 60000) % 60;
            long hrs = elapsed / 3600000;
            String secStr = String.valueOf(sec);
            if (secStr.length() < 2) secStr = "0" + secStr;
            String minStr = String.valueOf(min);
            if (minStr.length() < 2) minStr = "0" + minStr;
            sessionTimeText.setText(hrs + ":" + minStr + ":" + secStr);
        }
    }

    @Override
    public void reset() {

    }

    public void loadBeatmap() {
        LibraryManager.shuffleLibrary();
        loadBeatmapInfo();
        loadTimingPoints(true);
    }

    public void loadBeatmapInfo() {
        if (LibraryManager.getSizeOfBeatmaps() != 0) {

            beatmapInfo = LibraryManager.getCurrentBeatmapSet().getBeatmap(0);

            if (musicInfoText == null) {
                musicInfoText = new ChangeableText(Utils.toRes(Config.getRES_WIDTH() - 500), Utils.toRes(3),
                        ResourceManager.getInstance().getFont("font"), "None...", HorizontalAlign.RIGHT, 35);
            }

            musicInfoText.setText(beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText(), true);

            try {
                musicInfoText.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - musicInfoText.getWidth()), musicInfoText.getY());
                music_nowplay.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - musicInfoText.getWidth() - 130), 0);
            } catch (NullPointerException e) {
                musicInfoText.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - 200), 5);
                music_nowplay.setPosition(Utils.toRes(Config.getRES_WIDTH() - 500 + 470 - 200 - 130), 0);
            }

            animateNowPlaying();
        }
    }

    /**
     * NOW PLAYING transition: the label and the track title slide in from the
     * right while fading up, so a track change reads as one smooth move.
     */
    private void animateNowPlaying() {
        if (musicInfoText == null || music_nowplay == null) {
            return;
        }

        float slide = 40f;
        float textX = musicInfoText.getX();
        float labelX = music_nowplay.getX();

        musicInfoText.clearEntityModifiers();
        music_nowplay.clearEntityModifiers();

        musicInfoText.setAlpha(0f);
        music_nowplay.setAlpha(0f);
        musicInfoText.setPosition(textX + slide, musicInfoText.getY());
        music_nowplay.setPosition(labelX + slide, music_nowplay.getY());

        musicInfoText.registerEntityModifier(
            new ParallelEntityModifier(
                new org.anddev.andengine.entity.modifier.AlphaModifier(0.35f, 0f, 1f),
                new MoveXModifier(0.35f, textX + slide, textX)
            )
        );
        music_nowplay.registerEntityModifier(
            new ParallelEntityModifier(
                new org.anddev.andengine.entity.modifier.AlphaModifier(0.35f, 0f, 1f),
                new MoveXModifier(0.35f, labelX + slide, labelX)
            )
        );
    }

    public void loadTimingPoints(boolean reloadMusic) {
        if (beatmapInfo == null) {
            return;
        }

        for (ParticleSystem particleSpout : particleSystem) {
            particleSpout.setParticlesSpawnEnabled(false);
        }
        particleEnabled = false;
        GlobalManager.getInstance().setSelectedBeatmap(beatmapInfo);
        if (beatmapInfo.getBackgroundFilename() != null) {
            try {
                final TextureRegion tex = Config.isSafeBeatmapBg() ?
                        ResourceManager.getInstance().getTexture("menu-background") :
                        ResourceManager.getInstance().loadBackground(beatmapInfo.getBackgroundPath());

                if (tex != null) {
                    float height = tex.getHeight();
                    height *= Config.getRES_WIDTH()
                            / (float) tex.getWidth();
                    background = new Sprite(0,
                            (Config.getRES_HEIGHT() - height) / 2, Config
                            .getRES_WIDTH(), height, tex);
                    lastBackground.registerEntityModifier(new org.anddev.andengine.entity.modifier.AlphaModifier(1.5f, 1, 0, new IEntityModifier.IEntityModifierListener() {
                        @Override
                        public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                            scene.attachChild(background, 0);
                        }

                        @Override
                        public void onModifierFinished(IModifier<IEntity> pModifier, final IEntity pItem) {
                            GlobalManager.getInstance().getMainActivity().runOnUpdateThread(pItem::detachSelf);
                        }
                    }));
                    lastBackground = background;
                }
            } catch (Exception e) {
                Debug.e(e.toString());
                lastBackground.setAlpha(0);
            }
        } else {
            lastBackground.setAlpha(0);
        }

        if (reloadMusic) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().preLoad(beatmapInfo.getAudioPath(),
                        1f, false, musicRepeat);
                musicStarted = false;
            } else {
                Log.w("nullpoint", "GlobalManager.getInstance().getSongService() is null while reload music (MainScene.loadTimeingPoints)");
            }
        }

        Arrays.fill(frequencyAmplitudes, 0f);
        spectrumIndexOffset = 0;
        lastSpectrumUpdate = 0L;

        try {
            var beatmap = BeatmapCache.getBeatmap(beatmapInfo, false);

            var timingControlPoints = new LinkedList<>(beatmap.getControlPoints().timing.controlPoints);
            var effectControlPoints = new LinkedList<>(beatmap.getControlPoints().effect.controlPoints);

            // Getting the first timing point is not always accurate - case in point is when the music is not reloaded.
            int position = GlobalManager.getInstance().getSongService() != null ?
                    GlobalManager.getInstance().getSongService().getPosition() : 0;

            TimingControlPoint currentTimingPoint = null;
            EffectControlPoint currentEffectPoint = null;

            while (!timingControlPoints.isEmpty() && position > timingControlPoints.peek().time) {
                currentTimingPoint = timingControlPoints.pop();
            }

            while (!effectControlPoints.isEmpty() && position > effectControlPoints.peek().time) {
                currentEffectPoint = effectControlPoints.pop();
            }

            if (currentTimingPoint == null) {
                currentTimingPoint = beatmap.getControlPoints().timing.defaultControlPoint;
            }

            if (currentEffectPoint == null) {
                currentEffectPoint = beatmap.getControlPoints().effect.defaultControlPoint;
            }

            this.timingControlPoints = timingControlPoints;
            this.effectControlPoints = effectControlPoints;
            this.currentTimingPoint = currentTimingPoint;
            this.currentEffectPoint = currentEffectPoint;

            bpmLength = currentTimingPoint.msPerBeat;
            beatPassTime = (position - currentTimingPoint.time) % bpmLength;
        } catch (IOException | IllegalArgumentException e) {
            Debug.e("Failed to load beatmap for timing points: " + e);
        }
    }

    public void showExitDialog() {
        if (isOnExitAnim) {
            return;
        }

        var exitDialog = new UIConfirmDialog();
        exitDialog.setTitle("Exit");
        exitDialog.setText(context.getString(com.osudroid.resources.R.string.dialog_exit_message));
        exitDialog.setOnConfirm(() -> {
            exit();
            return null;
        });
        exitDialog.show();
    }

    public void exit() {
        if (isOnExitAnim) {
            return;
        }
        isOnExitAnim = true;

        Execution.updateThread(menu::detachButtons);

        //ResourceManager.getInstance().loadSound("seeya", "sfx/seeya.wav", false).play();
        //Allow customize Seeya Sounds from Skins
        BassSoundProvider exitsound = ResourceManager.getInstance().getSound("seeya");
        if (exitsound != null) {
            exitsound.play();
        }

        Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 1.0f);
        bg.registerEntityModifier(new org.anddev.andengine.entity.modifier.AlphaModifier(3.0f, 0, 1));
        scene.attachChild(bg);
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.anddev.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
        ));
        logoOverlay.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                new org.anddev.andengine.entity.modifier.ScaleModifier(3.0f, 1f, 0.8f)
        ));

        if (GlobalManager.getInstance().getSongService() != null) {
            GlobalManager.getInstance().getSongService().stop();
        }

        ScheduledExecutorService taskPool = Executors.newScheduledThreadPool(1);
        taskPool.schedule(new TimerTask() {
            @Override
            public void run() {
                GlobalManager.getInstance().getMainActivity().finish();
            }
        }, 3000, TimeUnit.MILLISECONDS);
    }

    public UIScene getScene() {
        return scene;
    }

    public BeatmapInfo getBeatmapInfo() {
        return beatmapInfo;
    }

    public void setBeatmap(BeatmapInfo beatmapInfo) {

        LibraryManager.findBeatmapSetIndex(beatmapInfo);
        this.beatmapInfo = beatmapInfo;

        loadBeatmapInfo();
        loadTimingPoints(false);
        musicControl(MusicOption.SYNC);
    }

    public void watchReplay(String replayFile) {
        Replay replay = new Replay();

        if (!replay.load(replayFile, false) || replay.replayVersion < 3) {
            return;
        }

        BeatmapInfo beatmap = LibraryManager.findBeatmapByMD5(replay.getMd5());

        if (beatmap == null) {
            return;
        }

        GlobalManager.getInstance().getMainScene().setBeatmap(beatmap);
        StatisticV2 stat = replay.getStat();
        stat.migrateLegacyMods(beatmap.getBeatmapDifficulty());
        stat.calculateModScoreMultiplier(beatmap.getBeatmapDifficulty());

        GlobalManager.getInstance().getSongMenu().select();
        ResourceManager.getInstance().loadBackground(beatmap.getBackgroundPath());
        GlobalManager.getInstance().getSongService().preLoad(beatmap.getAudioPath());
        GlobalManager.getInstance().getSongService().play();

        ScoringScene scorescene = GlobalManager.getInstance().getScoring();
        scorescene.load(stat, null, GlobalManager.getInstance().getSongService(), replayFile, null, beatmap);
        GlobalManager.getInstance().getEngine().setScene(scorescene.getScene());
    }

    public void show() {
        GlobalManager.getInstance().getSongService().setGaming(false);
        GlobalManager.getInstance().getEngine().setScene(getScene());
        if (GlobalManager.getInstance().getSelectedBeatmap() != null) {
            setBeatmap(GlobalManager.getInstance().getSelectedBeatmap());
        }
    }

    public enum MusicOption {PREV, PLAY, PAUSE, STOP, NEXT, SYNC}
}
