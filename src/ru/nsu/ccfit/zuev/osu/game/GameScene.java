package ru.nsu.ccfit.zuev.osu.game;

import static kotlinx.coroutines.JobKt.ensureActive;

import android.graphics.PointF;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import com.acivev.VibratorManager;
import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.support.ProxySprite;
import com.edlplan.framework.support.osb.StoryboardSprite;
import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.DatabaseManager;
import com.osudroid.game.Cursor;
import com.osudroid.game.CursorEvent;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.multiplayer.api.RoomAPI;
import com.osudroid.resources.R;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.ui.v2.game.FollowPointConnection;
import com.osudroid.ui.v2.game.SliderTickSprite;
import com.osudroid.ui.v2.hud.GameplayHUD;
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard;
import com.osudroid.ui.v2.hud.elements.HUDPPCounter;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.Cameras;
import com.reco1l.andengine.UIEngine;
import com.reco1l.andengine.UIScene;
import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.andengine.modifier.Modifiers;
import com.reco1l.andengine.shape.PaintStyle;
import com.reco1l.andengine.shape.UIBox;
import com.reco1l.andengine.sprite.UIAnimatedSprite;
import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.sprite.UIVideoSprite;
import com.reco1l.framework.Color4;
import com.rian.osu.GameMode;
import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.beatmap.ComboColor;
import com.rian.osu.beatmap.DroidPlayableBeatmap;
import com.rian.osu.beatmap.HitWindow;
import com.rian.osu.beatmap.constants.BeatmapCountdown;
import com.rian.osu.beatmap.hitobject.HitCircle;
import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.Spinner;
import com.rian.osu.beatmap.sections.BeatmapDifficulty;
import com.rian.osu.beatmap.timings.BreakPeriod;
import com.rian.osu.beatmap.timings.EffectControlPoint;
import com.rian.osu.beatmap.timings.TimingControlPoint;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes;
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes;
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes;
import com.rian.osu.difficulty.calculator.DroidPerformanceCalculationParameters;
import com.rian.osu.difficulty.calculator.PerformanceCalculationParameters;
import com.rian.osu.difficulty.calculator.StandardPerformanceCalculationParameters;
import com.rian.osu.gameplay.GameplayHitSampleInfo;
import com.rian.osu.math.Interpolation;
import com.rian.osu.mods.*;
import com.rian.osu.utils.ModHashMap;
import com.rian.osu.utils.ModUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.microedition.khronos.opengles.GL10;
import kotlin.Unit;
import kotlin.random.Random;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.util.FrameLimiter;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.EntityBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.effect.Metronome;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.SecurityUtils;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.game.GameplayHitCircle;
import ru.nsu.ccfit.zuev.osu.game.GameplaySlider;
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner;
import ru.nsu.ccfit.zuev.osu.game.cursor.AutoplayStyle;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;
import ru.nsu.ccfit.zuev.osuplusplus.MainActivity;
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main.AutoCursor;
import ru.nsu.ccfit.zuev.osuplusplus.game.cursor.main.CursorEntity;
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameScene implements GameObjectListener, IOnSceneTouchListener {

    // FailingLayer constants, ported from osu!(lazer) HUD/FailingLayer.cs.
    private static final float LOW_HEALTH_MAX_ALPHA = 0.4f;
    private static final float LOW_HEALTH_THRESHOLD = 0.20f;
    // Lerp speed per second; matches the original's elapsedMs * 0.01 lerp (~0.15 per frame at 60fps).
    private static final float LOW_HEALTH_LERP_SPEED = 10f;

    public static final int CursorCount = 10;
    private final int maximumActiveCursorCount = CursorCount;
    private final UIEngine engine;
    private Cursor[] cursors = new Cursor[CursorCount];
    private ru.nsu.ccfit.zuev.osuplusplus.ScreenShake screenShake;
    public String audioFilePath = null;
    private com.osudroid.game.replay.ReplaySettingsPanel replayPanel;
    private UIScene scene;
    private UIScene bgScene, mgScene, fgScene;
    private Scene oldScene;
    private UIBox sceneBorder;
    private Shape beatmapBackground;
    private Beatmap parsedBeatmap;
    private DroidPlayableBeatmap playableBeatmap;
    private BeatmapInfo lastBeatmapInfo;
    private ScoringScene scoringScene;
    private TimingControlPoint[] timingControlPoints;
    private int timingControlPointIndex;
    private EffectControlPoint[] effectControlPoints;
    private int effectControlPointIndex;
    private TimingControlPoint activeTimingPoint;
    private EffectControlPoint activeEffectPoint;
    private int lastObjectId = -1;
    private float leadOut = 0;
    private HitObject[] objects;
    private int objectIndex;
    private ArrayList<Color4> comboColors;
    private boolean comboWasMissed = false;
    private boolean comboWas100 = false;
    private ArrayList<GameObject> activeObjects;
    private ArrayList<GameObject> expiredObjects;
    /** Identity-based set of objects already force-expired (seek) or expired (update) this frame. */
    private final Set<GameObject> processedExpiredObjects = Collections.newSetFromMap(new IdentityHashMap<>());
    private GameObject judgeableObject;
    private BreakPeriod[] breakPeriods;
    private int breakPeriodIndex;
    private Metronome metronome;
    private float scale;
    public StatisticV2 stat;
    private boolean gameStarted;
    private float totalOffset;
    private int totalLength = Integer.MAX_VALUE;
    private boolean paused;
    private UISprite skipBtn;
    private float skipTime;
    private boolean musicStarted;
    private double distToNextObject;
    private CursorEntity[] cursorSprites;
    public AutoCursor autoCursor;
    private FlashLightEntity flashlightSprite;
    private int mainCursorId = -1;
    private Replay replay;
    private boolean replaying;
    private String replayFilePath;
    public boolean autoExportReplay = false;
    public String autoExportOutputPath = null;

    public float offsetSum;
    public int offsetRegs;
    private Rectangle dimRectangle = null;

    // Parallax effect
    private float bgBaseX = 0f;
    private float bgBaseY = 0f;
    private float parallaxPosX = 0f;
    private float parallaxPosY = 0f;
    private float parallaxScale = 0f;
    private float parallaxLastTime = 0f;
    /** Mirrors the state the background was last built with (replay panel toggle). */
    private boolean parallaxApplied = false;
    private ComboBurst comboBurst;
    private int failcount = 0;
    private int postSeekFrameCount = 0; // Suppress hitsounds after seek

    // Last point written into the replay per pointer, in track space + gameplay ms.
    // MOVE events are only recorded at least 1 osu!px apart (or 33ms apart), otherwise
    // Replay.MoveArray.checkNewPoint collapses the dense sub-pixel samples into a
    // single point and the recorded path is lost.
    private final float[] replayRecLastX = new float[CursorCount];
    private final float[] replayRecLastY = new float[CursorCount];
    private final int[] replayRecLastTime = new int[CursorCount];
    private final boolean[] replayRecWasDown = new boolean[CursorCount];

    // Replay playback pause: gameplay time is frozen while the scene (HUD, panel)
    // keeps updating, mirroring osu-droid's stopped gameplayClock.
    private boolean replayPlaybackPaused = false;
    private boolean replayPlaybackWasPlaying = false;
    /**
     * User-requested playback rate from the replay settings panel (upstream:
     * ReplayPlaybackRate.rate). Combined with the mod rate every frame the same way
     * osu-droid does: currentSpeedMultiplier = modRate * replaySettingsRate.
     */
    private float replaySettingsRate = 1f;
    private Color4 sliderBorderColor;
    private SliderPath[] sliderPaths = null;
    private LinePath[] sliderRenderPaths = null;
    private int sliderIndex = 0;

    // Kiai effects
    private int particleBeginTime = 0;
    private boolean particleEnabled = false;
    private boolean isContinuousKiai = false;
    private final org.anddev.andengine.entity.particle.ParticleSystem[] particleSystem =
        new org.anddev.andengine.entity.particle.ParticleSystem[2];
    private org.anddev.andengine.entity.primitive.Rectangle kiaiFlashOverlay;
    private float kiaiFlashAlpha = 0f;
    private boolean kiaiFlashTriggered = false;
    private boolean wasKiaiFlash = false;
    private float kiaiFlashTimer = 0f;

    // FailingLayer from osu!(lazer): fullscreen red overlay shown while health is low.
    private Rectangle lowHealthOverlay;
    private float lowHealthAlpha = 0f;

    // Stable letterbox-in-breaks bars (black strips flush to the top/bottom edge).
    private Rectangle letterboxTop;
    private Rectangle letterboxBottom;
    private ru.nsu.ccfit.zuev.osuplusplus.menu.TriangleBackground triangleBg;
    private UISprite unrankedSprite;
    private final ArrayList<IModApplicableToTrackRate> rateAdjustingMods =
        new ArrayList<>();

    @Nullable
    private Job storyboardLoadingJob;

    private StoryboardSprite storyboardSprite;

    // GPU replay renderer — bypasses per-entity draw path during replay playback
    @Nullable
    private ProxySprite storyboardOverlayProxy;

    public HitWindow hitWindow;
    private ModHashMap lastMods;

    private final AtomicInteger loadingRequestId = new AtomicInteger(0);
    private CompletableFuture<?> loadingPipeline;
    private Job gameLoadingJob;

    private PerformanceCalculationParameters performanceCalculationParameters;
    private TimedDifficultyAttributes<DroidDifficultyAttributes>[] droidTimedDifficultyAttributes;
    private TimedDifficultyAttributes<StandardDifficultyAttributes>[] standardTimedDifficultyAttributes;

    // Game

    /**
     * Whether the game is over.
     */
    private boolean isGameOver = false;

    /**
     * The break time animator.
     */
    private BreakAnimator breakAnimator;

    /**
     * The countdown animator.
     */
    public Countdown countdownAnimator;

    /**
     * Whether the game is ready to start.
     */
    public boolean isReadyToStart = false;

    // UI

    /**
     * The gameplay HUD
     */
    public GameplayHUD hud;

    /**
     * Whether the HUD editor mode is enabled.
     */
    public boolean isHUDEditorMode = false;

    /**
     * Whether the game started in HUD editor mode.
     */
    public boolean startedFromHUDEditor = false;

    // Timing

    /**
     * The time at which the last frame was rendered with respect to {@link SystemClock#uptimeMillis()}.
     * <br>
     * If 0, a frame has not been rendered yet.
     */
    private long previousFrameTime;

    /**
     * The start time of the first object in seconds.
     */
    public float firstObjectStartTime;

    /**
     * The end time of the last object in seconds.
     */
    public float lastObjectEndTime;

    /**
     * The initial {@link #elapsedTime} value when the game started, in seconds.
     */
    public float initialElapsedTime = 0;

    /**
     * The time passed since the game has started, in seconds.
     */
    public float elapsedTime = 0;

    // Video support

    /**
     * The current video loading {@link Job}.
     */
    @Nullable
    private Job videoLoadingJob;

    /**
     * Whether video is enabled.
     */
    private boolean videoEnabled;

    /**The video sprite*/
    private UIVideoSprite video;

    /**Video offset aka video start time in seconds*/
    private float videoOffset;

    /**Whether the video has started*/
    private boolean videoStarted;

    // Multiplayer

    /**Indicates the last time that the user pressed the back button, used to reset {@code backPressCount}*/
    private float lastBackPressTime = -1f;

    /**Indicates that the player has failed and the score shouldn't be submitted*/
    public boolean hasFailed = false;

    /**Indicates that the player has requested skip*/
    private boolean isSkipRequested = false;

    /**Real time elapsed in milliseconds since the game has started*/
    private long realTimeElapsed = 0;

    /**Real time elapsed in milliseconds since the latest statistic data was sent*/
    private long statisticDataTimeElapsed = 0;

    /**Last score data chunk sent to server, used to determine if the data was changed.*/
    private ScoreBoardItem lastScoreSent = null;

    public GameScene(final UIEngine engine) {
        this.engine = engine;
        scene = createMainScene();
        bgScene = new UIScene();
        fgScene = new UIScene();
        mgScene = new UIScene();
        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);
    }

    public void setScoringScene(final ScoringScene sc) {
        scoringScene = sc;
    }

    public void setOldScene(final Scene oscene) {
        oldScene = oscene;
    }

    private void loadBackground() {
        if (dimRectangle != null) {
            dimRectangle.detachSelf();
            dimRectangle = null;
        }

        if (sceneBorder != null) {
            sceneBorder.detachSelf();
            sceneBorder = null;
        }

        if (storyboardSprite != null) {
            storyboardSprite.detachSelf();
            storyboardSprite = null;
        }

        if (video != null) {
            video.release();
            video.detachSelf();
            video = null;
        }

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        TextureRegion textureRegion =
            Config.isSafeBeatmapBg() ||
            playableBeatmap.getEvents().backgroundFilename == null
                ? ResourceManager.getInstance().getTexture("menu-background")
                : ResourceManager.getInstance().getTextureIfLoaded(
                      "::background"
                  );

        if (textureRegion == null) {
            Rectangle rectangle = new Rectangle(
                0f,
                0f,
                Config.getRES_WIDTH(),
                Config.getRES_HEIGHT()
            );

            Color4 backgroundColor = playableBeatmap
                .getEvents()
                .getBackgroundColor();
            if (backgroundColor == null) {
                backgroundColor = new Color4(0, 0, 0);
            }
            ComponentsKt.setColor4(rectangle, backgroundColor);

            beatmapBackground = rectangle;
        } else {
            beatmapBackground = new Sprite(
                0,
                0,
                textureRegion.getWidth(),
                textureRegion.getHeight(),
                textureRegion
            );
        }
    }

    public void loadStoryboard(BeatmapInfo beatmapInfo) {
        if (storyboardSprite != null) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;
        boolean isStoryboardEnabled =
            brightness > 0.02f && Config.getBoolean("enableStoryboard", false);

        if (!isStoryboardEnabled) {
            cancelStoryboardLoading();
            return;
        }

        if (
            storyboardLoadingJob != null && !storyboardLoadingJob.isCompleted()
        ) {
            return;
        }

        storyboardLoadingJob = Execution.async(scope -> {
            StoryboardSprite storyboardSprite = this.storyboardSprite;
            this.storyboardSprite = null;

            if (storyboardSprite != null) {
                storyboardSprite.detachSelf();
            } else {
                storyboardSprite = new StoryboardSprite(
                    Config.getRES_WIDTH(),
                    Config.getRES_HEIGHT()
                );
                ensureActive(scope.getCoroutineContext());
            }

            ProxySprite storyboardOverlayProxy = this.storyboardOverlayProxy;
            this.storyboardOverlayProxy = null;

            if (storyboardOverlayProxy != null) {
                storyboardOverlayProxy.detachSelf();
            } else {
                storyboardSprite.setOverlayDrawProxy(
                    (storyboardOverlayProxy = new ProxySprite(
                        Config.getRES_WIDTH(),
                        Config.getRES_HEIGHT()
                    ))
                );
                ensureActive(scope.getCoroutineContext());
            }

            storyboardSprite.setTransparentBackground(
                videoEnabled && video != null
            );
            storyboardSprite.loadStoryboard(beatmapInfo.getPath());
            ensureActive(scope.getCoroutineContext());

            this.storyboardSprite = storyboardSprite;
            this.storyboardOverlayProxy = storyboardOverlayProxy;

            // The storyboard may only load after gameplay is started, in which case we must apply it immediately.
            Execution.updateThread(this::applyBackground);

            storyboardLoadingJob = null;
        });
    }

    private void cancelStoryboardLoading() {
        if (storyboardLoadingJob != null) {
            storyboardLoadingJob.cancel(
                new CancellationException("Storyboard loading job cancelled")
            );
            storyboardLoadingJob = null;
        }
    }

    public void loadVideo(BeatmapInfo beatmapInfo) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || video != null) {
            return;
        }

        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;
        var videoFilename = playableBeatmap.getEvents().videoFilename;
        videoEnabled =
            brightness > 0.02f &&
            Config.getBoolean("enableVideo", false) &&
            videoFilename != null;

        if (!videoEnabled) {
            cancelVideoLoading();
            return;
        }

        if (videoLoadingJob != null && !videoLoadingJob.isCompleted()) {
            return;
        }

        videoLoadingJob = Execution.async(scope -> {
            try {
                var video = new UIVideoSprite(
                    beatmapInfo.getAbsoluteSetDirectory() + "/" + videoFilename,
                    engine
                );
                video.setAlpha(0f);

                ensureActive(scope.getCoroutineContext());

                this.video = video;

                // The video may only load after gameplay is started, in which case we must apply it immediately.
                Execution.updateThread(this::applyBackground);
            } catch (Exception e) {
                video = null;
                Log.e("GameScene", "Error while loading video background.", e);
            }

            videoLoadingJob = null;
        });
    }

    private void cancelVideoLoading() {
        if (videoLoadingJob != null) {
            videoLoadingJob.cancel(
                new CancellationException("Video loading job cancelled")
            );
            videoLoadingJob = null;
        }
    }

    private void applyBackground() {
        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        float brightness = Config.getInt("bgbrightness", 25) / 100f;
        // Track the parallax state the background was built with, so the replay
        // panel's toggle can detect redundant rebuilds.
        parallaxApplied = Config.isParallaxEnabled();

        boolean isStoryboardEnabled =
            brightness > 0.02f && Config.getBoolean("enableStoryboard", false);
        float playfieldSize = Config.getPlayfieldSize();

        var storyboardSprite = this.storyboardSprite;
        var storyboardOverlayProxy = this.storyboardOverlayProxy;
        var video = this.video;
        var sceneBorder = this.sceneBorder;

        if (
            sceneBorder == null &&
            Config.isDisplayPlayfieldBorder() &&
            playfieldSize < 1f
        ) {
            sceneBorder = new UIBox() {
                {
                    setAnchor(Anchor.Center);
                    setOrigin(Anchor.Center);
                    setPaintStyle(PaintStyle.Outline);
                    setLineWidth(5f);
                    setColor(1f, 1f, 1f);
                    setAlpha(Interpolation.linear(0.2f, 0.8f, brightness));
                    setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
                }
            };

            scene.attachChild(sceneBorder, 0);
            this.sceneBorder = sceneBorder;
        }

        var background =
            videoEnabled && video != null ? video : beatmapBackground;

        if (dimRectangle != null) {
            dimRectangle.detachSelf();
        } else {
            dimRectangle = new Rectangle(0f, 0f, 0f, 0f);
        }

        dimRectangle.setSize(background.getWidth(), background.getHeight());
        dimRectangle.setColor(0f, 0f, 0f, 1f - brightness);
        background.attachChild(dimRectangle);

        if (breakAnimator != null) {
            breakAnimator.setDimRectangle(dimRectangle);
        }

        var factor = Config.isKeepBackgroundAspectRatio()
            ? Config.getRES_HEIGHT() / background.getHeight()
            : Config.getRES_WIDTH() / background.getWidth();

        // When parallax is enabled, add extra scale so edges don't show
        if (Config.isParallaxEnabled()) {
            factor *= 1.1f;
        }

        background.setScale(factor);
        background.setPosition(
            (Config.getRES_WIDTH() - background.getWidth()) / 2f,
            (Config.getRES_HEIGHT() - background.getHeight()) / 2f
        );
        bgBaseX = background.getX();
        bgBaseY = background.getY();
        parallaxLastTime = elapsedTime;
        scene.setBackground(new EntityBackground(background));

        if (storyboardSprite != null) {
            if (isStoryboardEnabled) {
                storyboardSprite.setTransparentBackground(
                    videoEnabled && video != null
                );
                storyboardSprite.setBrightness(brightness);

                if (!storyboardSprite.hasParent()) {
                    scene.attachChild(storyboardSprite, 0);
                }
            } else {
                storyboardSprite.detachSelf();
            }
        }

        if (storyboardOverlayProxy != null) {
            if (isStoryboardEnabled) {
                if (!storyboardOverlayProxy.hasParent()) {
                    scene.attachChild(
                        storyboardOverlayProxy,
                        scene.getChildIndex(fgScene)
                    );
                }
            } else {
                storyboardOverlayProxy.detachSelf();
            }
        }
    }

    private boolean loadGame(
        final BeatmapInfo beatmapInfo,
        final String rFile,
        final ModHashMap mods,
        @Nullable CoroutineScope scope
    ) {
        if (
            !SecurityUtils.verifyFileIntegrity(
                GlobalManager.getInstance().getMainActivity()
            )
        ) {
            ToastLogger.showText(
                com.osudroid.resources.R.string.file_integrity_tampered,
                true
            );
            return false;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFilePath =
                Config.getCachePath() +
                "/" +
                MD5Calculator.getStringMD5(rFile) +
                ".odr";
            Debug.i("ReplayFile = " + replayFilePath);
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFilePath)) {
                ToastLogger.showText(
                    com.osudroid.resources.R.string.replay_cantdownload,
                    true
                );
                return false;
            }
        } else this.replayFilePath = rFile;

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        boolean shouldParseBeatmap =
            parsedBeatmap == null ||
            !parsedBeatmap.getMd5().equals(beatmapInfo.getMD5());

        if (shouldParseBeatmap) {
            try {
                parsedBeatmap = BeatmapCache.getBeatmap(
                    beatmapInfo,
                    true,
                    GameMode.Droid,
                    scope
                );
            } catch (IOException | IllegalArgumentException e) {
                Debug.e("startGame: " + e.getMessage());
                ToastLogger.showText(e.getMessage(), true);
                return false;
            }
        }

        if (parsedBeatmap == null) {
            return false;
        }

        if (!parsedBeatmap.getMd5().equals(beatmapInfo.getMD5())) {
            ToastLogger.showText(
                com.osudroid.resources.R.string.file_integrity_tampered,
                true
            );
            return false;
        }

        if (parsedBeatmap.getHitObjects().objects.isEmpty()) {
            ToastLogger.showText("Empty Beatmap", true);
            return false;
        }

        // Ensure that only relevant mods are applied.
        mods.values().removeIf(m -> !m.isRelevant());

        boolean differentPlayableBeatmap =
            shouldParseBeatmap || lastMods == null || !lastMods.equals(mods);

        var playableBeatmap = differentPlayableBeatmap
            ? parsedBeatmap.createDroidPlayableBeatmap(mods.values())
            : this.playableBeatmap;

        this.playableBeatmap = playableBeatmap;

        // Set beatmap info for Lua plugins
        try {
            com.osudroid.plugin.GameState.setBeatmapInfo(
                beatmapInfo.getTitle() != null ? beatmapInfo.getTitle() : "",
                beatmapInfo.getArtist() != null ? beatmapInfo.getArtist() : "",
                beatmapInfo.getVersion() != null ? beatmapInfo.getVersion() : "",
                beatmapInfo.getMD5() != null ? beatmapInfo.getMD5() : ""
            );
            com.osudroid.plugin.GameState.setActiveMods(mods.serializeMods());
        } catch (Exception ignored) {}

        // Load backgrounds early to minimize waiting time.
        loadBackground();
        loadStoryboard(beatmapInfo);
        loadVideo(beatmapInfo);

        rateAdjustingMods.clear();

        for (var mod : mods.values()) {
            if (mod instanceof IModApplicableToTrackRate rateMod) {
                rateAdjustingMods.add(rateMod);
            }
        }

        // TODO skin manager
        BeatmapSkinManager.getInstance().loadBeatmapSkin(
            playableBeatmap.getBeatmapsetPath()
        );

        var breaks = playableBeatmap.getEvents().breaks;

        if (
            shouldParseBeatmap ||
            breakPeriods == null ||
            breakPeriods.length != breaks.size()
        ) {
            breakPeriods = new BreakPeriod[breaks.size()];
            System.arraycopy(
                breaks.toArray(),
                0,
                breakPeriods,
                0,
                breakPeriods.length
            );
        }

        try {
            var musicFile = new File(beatmapInfo.getAudioPath());

            if (!musicFile.exists()) {
                throw new FileNotFoundException(musicFile.getPath());
            }

            audioFilePath = musicFile.getPath();
        } catch (final Exception e) {
            Debug.e("Load Music: " + e.getMessage());
            ToastLogger.showText(e.getMessage(), true);
            return false;
        }

        var hitObjects = playableBeatmap.getHitObjects().objects;
        var firstObject = hitObjects.get(0);
        scale = firstObject.getScreenSpaceGameplayScale();

        GameHelper.setOverallDifficulty(playableBeatmap.getDifficulty().od);
        GameHelper.setHealthDrain(playableBeatmap.getDifficulty().hp);
        GameHelper.setSpeedMultiplier(
            ModUtils.calculateRateWithMods(
                mods.values(),
                Double.NEGATIVE_INFINITY
            )
        );

        GameHelper.setOriginalTimePreempt(
            (float) BeatmapDifficulty.difficultyRange(
                playableBeatmap.getDifficulty().getAR(),
                HitObject.PREEMPT_MAX,
                HitObject.PREEMPT_MID,
                HitObject.PREEMPT_MIN
            )
        );

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GlobalManager.getInstance()
            .getSongService()
            .preLoad(
                audioFilePath,
                GameHelper.getSpeedMultiplier(),
                GameHelper.getSpeedMultiplier() != 1f &&
                    (Config.isShiftPitchInRateChange() ||
                        mods.contains(ModNightCore.class) ||
                        mods.contains(ModOldNightCore.class))
            );

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        totalLength = GlobalManager.getInstance().getSongService().getLength();
        judgeableObject = null;
        breakPeriodIndex = 0;
        objectIndex = 0;
        lastObjectId = -1;
        hitWindow = playableBeatmap.getHitWindow();
        videoStarted = false;
        videoOffset = playableBeatmap.getEvents().videoStartTime / 1000f;

        if (
            shouldParseBeatmap ||
            objects == null ||
            objects.length != hitObjects.size()
        ) {
            objects = new HitObject[hitObjects.size()];
        }

        if (differentPlayableBeatmap || objects[0] == null) {
            System.arraycopy(
                hitObjects.toArray(),
                0,
                objects,
                0,
                objects.length
            );
        }

        firstObjectStartTime = (float) firstObject.startTime / 1000;
        lastObjectEndTime =
            (float) objects[objects.length - 1].getEndTime() / 1000;

        int estimatedMaxActiveObjects = Math.max(
            10,
            estimateMaximumActiveObjects()
        );

        if (activeObjects != null) {
            activeObjects.clear();
            activeObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            activeObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        if (expiredObjects != null) {
            expiredObjects.clear();
            expiredObjects.ensureCapacity(estimatedMaxActiveObjects);
        } else {
            expiredObjects = new ArrayList<>(estimatedMaxActiveObjects);
        }

        float firstObjectTimePreempt = (float) firstObject.timePreempt / 1000;
        float skipTargetTime =
            firstObjectStartTime - Math.max(2f, firstObjectTimePreempt);

        elapsedTime = Math.min(0, skipTargetTime);
        skipTime = skipTargetTime - 1;

        // Some beatmaps specify a current lead-in time, which overrides the default lead-in time above.
        float leadIn = playableBeatmap.getGeneral().audioLeadIn / 1000f;
        if (leadIn > 0) {
            elapsedTime = Math.min(elapsedTime, firstObjectStartTime - leadIn);
        }

        // Ensure the video has time to start.
        // Even when video is not activated, apply offset anyway to ensure that everyone in multiplayer starts at the
        // same time regardless of the setting.
        elapsedTime = Math.min(elapsedTime, videoOffset);

        loadSkinDrivenColors();

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var timingControlPointManager = playableBeatmap
            .getControlPoints()
            .timing;
        var effectControlPointManager = playableBeatmap
            .getControlPoints()
            .effect;

        if (shouldParseBeatmap || timingControlPoints == null) {
            timingControlPoints =
                new TimingControlPoint[timingControlPointManager.controlPoints.size()];
            System.arraycopy(
                timingControlPointManager.controlPoints.toArray(),
                0,
                timingControlPoints,
                0,
                timingControlPoints.length
            );
        }

        if (shouldParseBeatmap || effectControlPoints == null) {
            effectControlPoints =
                new EffectControlPoint[effectControlPointManager.controlPoints.size()];
            System.arraycopy(
                effectControlPointManager.controlPoints.toArray(),
                0,
                effectControlPoints,
                0,
                effectControlPoints.length
            );
        }

        activeTimingPoint =
            timingControlPoints.length > 0
                ? timingControlPoints[0]
                : timingControlPointManager.defaultControlPoint;
        activeEffectPoint =
            effectControlPoints.length > 0
                ? effectControlPoints[0]
                : effectControlPointManager.defaultControlPoint;
        timingControlPointIndex = 0;
        effectControlPointIndex = 0;

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(0);
        GameHelper.setSamplesMatchPlaybackRate(
            playableBeatmap.getGeneral().samplesMatchPlaybackRate
        );

        GameObjectPool.getInstance().purge();

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        FollowPointConnection.getPool().renew(16);
        SliderTickSprite.getPool().renew(16);

        // TODO replay
        offsetSum = 0;
        offsetRegs = 0;

        replaying = false;

        // Reset the replay panel's playback rate so a rate chosen in a previous replay
        // session does not leak into normal play (or the next replay).
        replaySettingsRate = 1f;

        // Reset the centralized replay movement recorder (see recordReplayMovements):
        // stale per-pointer state from a previous session would corrupt the density
        // filter and the down/up pairing of the next recording.
        java.util.Arrays.fill(replayRecLastX, 0f);
        java.util.Arrays.fill(replayRecLastY, 0f);
        java.util.Arrays.fill(replayRecLastTime, 0);
        java.util.Arrays.fill(replayRecWasDown, false);

        replay = new Replay(true);
        replay.setObjectCount(hitObjects.size());
        replay.setBeatmap(
            beatmapInfo.getFullBeatmapsetName(),
            beatmapInfo.getFullBeatmapName(),
            playableBeatmap.getMd5()
        );

        if (replayFilePath != null) {
            // Replay decoding may be dependent on the used mods, so we must do this.
            var replayStat = new StatisticV2();
            replayStat.setMod(mods);
            replay.setStat(replayStat);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            replaying = replay.load(replayFilePath, true);

            // In older versions, replay uploads are separated from scores, which means that they may not be uploaded
            // for reasons independent of score uploads (e.g., network failure). When this happens, replays may be very
            // off such that it causes gameplay to appear very wrong (e.g., a score has Hard Rock/Mirror mod while its
            // replay does not). While this can theoretically happen to any score data (not just mods), checking for
            // mods for the time being is enough to dislodge major inconsistencies in gameplay.
            // Difficulty Adjust rewrites its settings' default values when the beatmap is
            // applied (applyFromBeatmap), so the live mod instances in `mods` never compare
            // equal to the ones freshly read from the replay file. Canonicalizing both sides
            // through the same serialization filter makes the comparison independent of that
            // mutation (and of irrelevant mods that the filter drops on both sides).
            boolean modsMatch = false;

            if (replaying) {
                var replayMods = ModUtils.deserializeMods(replay.getStat().getMod().serializeMods());
                var expectedMods = ModUtils.deserializeMods(mods.serializeMods());
                modsMatch = replayMods.equals(expectedMods);
            }

            if (!modsMatch) {
                ToastLogger.showText(
                    com.osudroid.resources.R.string.replay_invalid,
                    true
                );
                return false;
            }
            GameHelper.setReplayVersion(replay.replayVersion);

            // Replays recorded by older builds may contain no cursor movements at all,
            // which renders no cursor during playback — tell the user why.
            {
                int totalMovements = 0;
                for (int i = 0; i < replay.cursorMoves.size(); i++) {
                    totalMovements += replay.cursorMoves.get(i).size;
                }

                if (totalMovements == 0) {
                    ToastLogger.showText("Replay contains no cursor data", false);
                }
            }

        } else if (mods.contains(ModAutoplay.class)) {
            replay = null;
        }

        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        GameObjectPool.getInstance().preload();

        if (
            isHUDEditorMode ||
            OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class)
        ) {
            // Calculate timed difficulty attributes
            switch (Config.getDifficultyAlgorithm()) {
                case droid, drpp, rxpp -> {
                    performanceCalculationParameters =
                        new DroidPerformanceCalculationParameters();

                    if (
                        droidTimedDifficultyAttributes == null ||
                        differentPlayableBeatmap
                    ) {
                        droidTimedDifficultyAttributes =
                            BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(
                                playableBeatmap,
                                scope
                            );
                    }
                }
                case standard -> {
                    performanceCalculationParameters =
                        new StandardPerformanceCalculationParameters();

                    if (
                        standardTimedDifficultyAttributes == null ||
                        differentPlayableBeatmap
                    ) {
                        standardTimedDifficultyAttributes =
                            BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                                parsedBeatmap,
                                mods.values(),
                                scope
                            );
                    }
                }
            }
        }

        sliderIndex = 0;

        if (
            sliderPaths == null ||
            sliderRenderPaths == null ||
            differentPlayableBeatmap
        ) {
            calculateAllSliderPaths(scope);
        }

        lastMods = mods;
        lastBeatmapInfo = beatmapInfo;

        // Resetting variables before starting the game.
        Multiplayer.finalData = null;
        hasFailed = false;
        lastBackPressTime = -1f;
        isSkipRequested = false;
        realTimeElapsed = 0;
        statisticDataTimeElapsed = 0;
        leadOut = 0;
        musicStarted = false;
        lastScoreSent = null;
        isGameOver = false;

        paused = false;
        gameStarted = false;
        return true;
    }

    public Scene getScene() {
        return scene;
    }

    public void restartGame() {
        startGame(null, null, null);
    }

    public void startGame(
        BeatmapInfo beatmapInfo,
        String replayFile,
        ModHashMap mods
    ) {
        startGame(beatmapInfo, replayFile, mods, false);
    }

    public void startGame(
        BeatmapInfo beatmapInfo,
        String replayFile,
        ModHashMap mods,
        boolean isHUDEditor
    ) {
        isReadyToStart = false;
        isHUDEditorMode = isHUDEditor;
        startedFromHUDEditor = isHUDEditor;
        resetPlayfieldSizeScale();

        scene = createMainScene();
        bgScene = new UIScene();
        mgScene = new UIScene();
        mgScene.setClipToBounds(true);
        fgScene = new UIScene();
        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);
        scene.setBackground(new ColorBackground(0, 0, 0));
        bgScene.setBackgroundEnabled(false);
        mgScene.setBackgroundEnabled(false);
        fgScene.setBackgroundEnabled(false);
        failcount = 0;
        mainCursorId = -1;

        final String rfile =
            beatmapInfo != null ? replayFile : this.replayFilePath;
        final int requestId = loadingRequestId.incrementAndGet();

        BeatmapInfo beatmapToUse =
            beatmapInfo != null ? beatmapInfo : lastBeatmapInfo;
        boolean isRestart =
            beatmapInfo == null && replayFile == null && mods == null;
        ModHashMap modsToUse;

        if (isHUDEditor) {
            modsToUse = new ModHashMap();
            modsToUse.put(ModAutoplay.class);
        } else if (mods != null) {
            modsToUse = mods.deepCopy();
        } else if (lastMods != null) {
            modsToUse = lastMods;
        } else {
            modsToUse = new ModHashMap();
        }

        GameLoaderScene scene = new GameLoaderScene(
            this,
            beatmapToUse,
            modsToUse,
            isRestart
        );
        engine.setScene(scene);

        ResourceManager.getInstance().getSound("failsound").stop();

        var pipeline = cancelLoading(false)
            .thenCompose(ignored ->
                DifficultyCalculationManager.stopCalculation()
            )
            .thenRun(() -> {
                if (requestId != loadingRequestId.get()) {
                    return;
                }

                gameLoadingJob = Execution.async(scope -> {
                    boolean succeeded = false;
                    boolean cancelled = false;

                    try {
                        if (requestId != loadingRequestId.get()) {
                            return;
                        }

                        succeeded = loadGame(
                            beatmapToUse,
                            rfile,
                            modsToUse,
                            scope
                        );

                        if (succeeded && requestId == loadingRequestId.get()) {
                            prepareScene();
                        }
                    } catch (CancellationException e) {
                        cancelled = true;
                        throw e;
                    } finally {
                        if (requestId == loadingRequestId.get()) {
                            if (!succeeded && !cancelled) {
                                quit();
                            }

                            gameLoadingJob = null;
                        }
                    }
                });
            });

        loadingPipeline = pipeline;

        pipeline.whenComplete((ignored, error) -> {
            if (loadingPipeline == pipeline) {
                loadingPipeline = null;
            }
        });
    }

    public CompletableFuture<Unit> cancelLoading() {
        return cancelLoading(true);
    }

    private CompletableFuture<Unit> cancelLoading(
        boolean invalidatePendingStart
    ) {
        // Do not cancel loading in multiplayer.
        if (Multiplayer.isMultiplayer) {
            return CompletableFuture.completedFuture(Unit.INSTANCE);
        }

        if (invalidatePendingStart) {
            loadingRequestId.incrementAndGet();
        }

        var gameLoadingJob = this.gameLoadingJob;
        var storyboardLoadingJob = this.storyboardLoadingJob;
        var videoLoadingJob = this.videoLoadingJob;
        var loadingPipeline = this.loadingPipeline;

        this.gameLoadingJob = null;
        this.storyboardLoadingJob = null;
        this.videoLoadingJob = null;

        var jobCancellation = Execution.stopAsync(gameLoadingJob)
            .thenCompose(ignored -> Execution.stopAsync(storyboardLoadingJob))
            .thenCompose(ignored -> Execution.stopAsync(videoLoadingJob));

        var pipelineDrain =
            loadingPipeline != null
                ? loadingPipeline.exceptionally(error -> null)
                : CompletableFuture.completedFuture(Unit.INSTANCE);

        return CompletableFuture.allOf(
            jobCancellation,
            pipelineDrain
        ).thenApply(ignored -> Unit.INSTANCE);
    }

    private void prepareScene() {
        scene.setOnSceneTouchListener(this);

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        var metadata = playableBeatmap.getMetadata();
        android.util.Log.d("GameScene", "User is in GameScene | Song: " + metadata.artist + " - " + metadata.title + " [" + metadata.version + "] | Map: " + lastBeatmapInfo.getPath());

        if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
            android.util.Log.d("GameScene", "Multiplayer room players: " + Multiplayer.room.getPlayerCount() + " | Room: " + Multiplayer.room.getName());
        }

        stat = new StatisticV2();
        stat.setMod(lastMods);
        stat.migrateLegacyMods(parsedBeatmap.getDifficulty());
        stat.calculateModScoreMultiplier(parsedBeatmap);
        stat.canFail =
            !stat.getMod().contains(ModNoFail.class) &&
            !stat.getMod().contains(ModAutopilot.class) &&
            !stat.getMod().contains(ModAutoplay.class);

        float difficultyScoreMultiplier =
            1 +
            Math.min(parsedBeatmap.getDifficulty().od, 10) / 10f +
            Math.min(parsedBeatmap.getDifficulty().hp, 10) / 10f;

        // The maximum CS of osu!droid mapped to osu!standard is ~17.62.
        difficultyScoreMultiplier +=
            (Math.min(parsedBeatmap.getDifficulty().gameplayCS, 17.62f) - 3) /
            4f;

        stat.setDiffModifier(difficultyScoreMultiplier);
        stat.setBeatmapNoteCount(objects.length);
        stat.setBeatmapMaxCombo(parsedBeatmap.getMaxCombo());

        GameHelper.setHardRock(lastMods.ofType(ModHardRock.class));
        GameHelper.setDoubleTime(lastMods.ofType(ModDoubleTime.class));
        GameHelper.setNightCore(
            lastMods.contains(ModNightCore.class)
                ? lastMods.ofType(ModNightCore.class)
                : lastMods.ofType(ModOldNightCore.class)
        );
        GameHelper.setHalfTime(lastMods.ofType(ModHalfTime.class));
        GameHelper.setHidden(lastMods.ofType(ModHidden.class));
        GameHelper.setTraceable(lastMods.ofType(ModTraceable.class));
        GameHelper.setFlashlight(lastMods.ofType(ModFlashlight.class));
        GameHelper.setRelax(lastMods.ofType(ModRelax.class));
        GameHelper.setAutopilot(lastMods.ofType(ModAutopilot.class));
        GameHelper.setAutoplay(lastMods.ofType(ModAutoplay.class));
        GameHelper.setSuddenDeath(lastMods.ofType(ModSuddenDeath.class));
        GameHelper.setPerfect(lastMods.ofType(ModPerfect.class));
        GameHelper.setSynesthesia(lastMods.ofType(ModSynesthesia.class));
        GameHelper.setScoreV2(lastMods.ofType(ModScoreV2.class));
        GameHelper.setEasy(lastMods.ofType(ModEasy.class));
        GameHelper.setMuted(lastMods.ofType(ModMuted.class));
        GameHelper.setFreezeFrame(lastMods.ofType(ModFreezeFrame.class));
        GameHelper.setApproachDifferent(
            lastMods.ofType(ModApproachDifferent.class)
        );
        GameHelper.setGravity(lastMods.ofType(ModGravity.class));

        int cursorCount = GameHelper.isRelax()
            ? 1
            : replaying && replay != null
              ? replay.cursorMoves.size()
              : CursorCount;

        cursors = new Cursor[cursorCount];

        for (int i = 0; i < cursorCount; i++) {
            cursors[i] = new Cursor();
        }

        comboWas100 = false;
        comboWasMissed = false;
        previousFrameTime = 0;

        metronome = null;
        if (
            (Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore()) ||
            (GameHelper.isMuted() &&
                GameHelper.getMuted().isEnableMetronome()) ||
            Config.getMetronomeSwitch() == 2
        ) {
            metronome = new Metronome();
        }

        distToNextObject = 0;

        // TODO passive objects
        // Create cursor entities regardless of particles setting; trail is created conditionally inside CursorEntity
        // The preference is read live (not the loadConfig cache): the settings toggle
        // applies to the next game without an app restart.
        if (
            (replaying || Config.getBoolean("showcursor", false)) &&
            !GameHelper.isAutoplay() &&
            !GameHelper.isAutopilot()
        ) {
            cursorSprites = new CursorEntity[cursorCount];
            for (int i = 0; i < cursorCount; i++) {
                cursorSprites[i] = new CursorEntity();
                cursorSprites[i].attachToScene(fgScene);
            }
        } else {
            cursorSprites = null;
        }

        // Initialize plugin system for ALL gameplay sessions (not just autoplay)
        com.osudroid.plugin.PluginManager.getInstance().initGameplay(
            ru.nsu.ccfit.zuev.osuplusplus.GlobalManager.getInstance().getMainActivity(),
            fgScene
        );

        // Sync beatmap info for plugins
        com.osudroid.plugin.GameState.setBeatmapInfo(
            playableBeatmap.getMetadata().title,
            playableBeatmap.getMetadata().artist,
            playableBeatmap.getMetadata().version,
            playableBeatmap.getMd5()
        );
        com.osudroid.plugin.GameState.setAR((float) playableBeatmap.getDifficulty().getAR());
        com.osudroid.plugin.GameState.setCS((float) playableBeatmap.getDifficulty().gameplayCS);
        com.osudroid.plugin.GameState.setOD((float) playableBeatmap.getDifficulty().od);
        com.osudroid.plugin.GameState.setHP((float) playableBeatmap.getDifficulty().hp);
        com.osudroid.plugin.GameState.setSpeedMultiplier((float) GameHelper.getSpeedMultiplier());
        com.osudroid.plugin.GameState.setMaxCombo(stat.getScoreMaxCombo());
        com.osudroid.plugin.GameState.setObjectCount(playableBeatmap.getHitObjects().objects.size());
        com.osudroid.plugin.GameState.setActiveMods(com.osudroid.plugin.GameState.getActiveMods());

        // Report presence: playing
        if (lastBeatmapInfo != null) {
            ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance().reportPresence(
                "playing",
                (lastBeatmapInfo.getArtist() != null ? lastBeatmapInfo.getArtist() : "Unknown") +
                " - " + (lastBeatmapInfo.getTitle() != null ? lastBeatmapInfo.getTitle() : "Unknown") +
                " [" + (lastBeatmapInfo.getVersion() != null ? lastBeatmapInfo.getVersion() : "Unknown") + "]"
            );
        }

        // Dispatch beatmap loaded event
        com.osudroid.plugin.PluginManager.getInstance().dispatchBeatmapLoaded(
            playableBeatmap.getMetadata().title,
            playableBeatmap.getMetadata().artist,
            playableBeatmap.getMetadata().version,
            playableBeatmap.getHitObjects().objects.size()
        );

        // Refresh per-session mover settings cache (hot-path reads are cached).
        ru.nsu.ccfit.zuev.osu.game.cursor.mover.MoverSettings.clearCache();

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor = new AutoCursor();
            var aoHitObjects = playableBeatmap.getHitObjects().objects;
            if (!aoHitObjects.isEmpty()) {
                var aoFirst = aoHitObjects.get(0);
                autoCursor.setDifficulty(
                    (float) aoFirst.timePreempt,
                    GameHelper.getSpeedMultiplier(),
                    (float) aoFirst.getScreenSpaceGameplayRadius()
                );
            }
            autoCursor.attachToScene(fgScene);

            // Initialize precomputed movement queue like danser-go's GenericScheduler.Init()
            autoCursor.initQueue(
                playableBeatmap.getHitObjects().objects.toArray(new com.rian.osu.beatmap.hitobject.HitObject[0]),
                this
            );
        }

        final var countdown = playableBeatmap.getGeneral().countdown;
        if (Config.isCorovans() && countdown != BeatmapCountdown.NoCountdown) {
            float cdSpeed = countdown.speed;
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH;
            if (
                cdSpeed != 0 &&
                firstObjectStartTime - elapsedTime >=
                    cdSpeed * Countdown.COUNTDOWN_LENGTH
            ) {
                countdownAnimator = new Countdown(
                    bgScene,
                    cdSpeed,
                    0,
                    firstObjectStartTime - elapsedTime
                );
            }
        }

        if (Config.isComboburst()) {
            comboBurst = new ComboBurst(
                Config.getRES_WIDTH(),
                Config.getRES_HEIGHT()
            );
            comboBurst.attachAll(bgScene);
        }

        initializeParticleSystems();

        // Initialize screen shake
        screenShake = new ru.nsu.ccfit.zuev.osuplusplus.ScreenShake(
            engine.getCamera()
        );

        // Initialize kiai flash overlay
        kiaiFlashOverlay = new org.anddev.andengine.entity.primitive.Rectangle(
            0,
            0,
            Config.getRES_WIDTH(),
            Config.getRES_HEIGHT()
        );
        kiaiFlashOverlay.setColor(1f, 1f, 1f, 0f);
        kiaiFlashOverlay.setVisible(false);
        fgScene.attachChild(kiaiFlashOverlay);

        // FailingLayer: fullscreen red overlay while health is low.
        lowHealthOverlay = new Rectangle(
            0,
            0,
            Config.getRES_WIDTH(),
            Config.getRES_HEIGHT()
        );
        lowHealthOverlay.setColor(1f, 0f, 0f, 0f);
        lowHealthOverlay.setVisible(false);
        fgScene.attachChild(lowHealthOverlay);
        // Reset the alpha left over from the previous play.
        lowHealthAlpha = 0f;
        lowHealthOverlay.setAlpha(0f);

        // Stable letterbox-in-breaks bars: same geometry as the main menu dim bars
        // (86.4 design units tall at 768-height), transparent until a break runs.
        float letterboxBarHeight = 86.4f * Config.getRES_HEIGHT() / 768f;
        letterboxTop = new Rectangle(
            0,
            0,
            Config.getRES_WIDTH(),
            letterboxBarHeight
        );
        letterboxTop.setColor(0f, 0f, 0f, 0f);
        fgScene.attachChild(letterboxTop);
        letterboxBottom = new Rectangle(
            0,
            Config.getRES_HEIGHT() - letterboxBarHeight,
            Config.getRES_WIDTH(),
            letterboxBarHeight
        );
        letterboxBottom.setColor(0f, 0f, 0f, 0f);
        fgScene.attachChild(letterboxBottom);

        // Triangle background
        triangleBg =
            new ru.nsu.ccfit.zuev.osuplusplus.menu.TriangleBackground();
        bgScene.attachChild(triangleBg);

        var position = new PointF(Config.getRES_WIDTH() - 130, 130);
        float timeOffset = 0;

        for (var mod : lastMods.values()) {
            if (!mod.isUserPlayable()) {
                continue;
            }

            var icon = new ModIcon(mod);
            icon.setPosition(position.x, position.y);
            icon.setOrigin(Anchor.Center);
            icon.setSize(68, 66);
            icon.setScale(scale);
            icon.registerEntityModifier(
                Modifiers.sequence(
                    IEntity::detachSelf,
                    Modifiers.scale(0.25f, 1.2f, 1f),
                    Modifiers.delay(2f - timeOffset),
                    Modifiers.parallel(
                        Modifiers.fadeOut(0.5f),
                        Modifiers.scale(0.5f, 1f, 1.5f)
                    )
                )
            );

            fgScene.attachChild(icon);

            position.x -= 25f;
            timeOffset += 0.25f;
        }

        boolean hasUnrankedMod = SmartIterator.wrap(
            lastMods.values().iterator()
        )
            .applyFilter(m -> !m.isRanked())
            .hasNext();
        if (hasUnrankedMod || Config.isRemoveSliderLock()) {
            unrankedSprite = new UISprite(
                ResourceManager.getInstance().getTexture("play-unranked")
            );
            unrankedSprite.setAnchor(Anchor.TopCenter);
            unrankedSprite.setOrigin(Anchor.Center);
            unrankedSprite.setPosition(0, 80);
            fgScene.attachChild(unrankedSprite);
        }

        if (GameHelper.isFlashlight()) {
            flashlightSprite = new FlashLightEntity(GameHelper.getFlashlight());
            fgScene.attachChild(flashlightSprite, 0);
        }

        // HUD should be to the last so we ensure everything is initialized and ready to be used by
        // the HUD elements in their constructors.
        hud = new GameplayHUD();

        if (!replaying && !GameHelper.isAutoplay()) {
            // Since block areas are saved in device pixels, we need to map them to scaled pixels.
            final var displayMetrics = new DisplayMetrics();
            GlobalManager.getInstance()
                .getMainActivity()
                .getWindowManager()
                .getDefaultDisplay()
                .getRealMetrics(displayMetrics);

            final float ratio =
                (float) Config.getRES_WIDTH() / displayMetrics.widthPixels;

            for (final var area : DatabaseManager.getBlockAreaTable().getAll()) {
                // Attach the block area to the HUD so that it does not get scaled with the playfield.
                var areaBox = new UIBox() {
                    {
                        setCornerRadius(2f);
                        setColor(30f / 255f, 30f / 255f, 41f / 255f);
                        setAlpha(0.15f);

                        setPosition(
                            Interpolation.linear(0f, area.getX(), ratio),
                            Interpolation.linear(0f, area.getY(), ratio)
                        );

                        setSize(
                            Interpolation.linear(0f, area.getWidth(), ratio),
                            Interpolation.linear(0f, area.getHeight(), ratio)
                        );
                    }

                    @Override
                    public boolean onAreaTouched(
                        TouchEvent event,
                        float localX,
                        float localY
                    ) {
                        if (event.isActionDown()) {
                            int id = event.getPointerID();

                            if (id >= 0 && id < getCursorsCount()) {
                                cursors[id].mouseBlocked = true;
                            }
                        }

                        return false;
                    }
                };

                hud.attachChild(areaBox);
            }
        }

        hud.setEditMode(isHUDEditorMode);
        hud.setSkinData(OsuSkin.get().getHUDSkinData());

        // Initialize replay control overlay
        if (replaying || GameHelper.isAutoplay()) {
            if (!Config.isHideReplaySettingsPanel()) {
                replayPanel = new com.osudroid.game.replay.ReplaySettingsPanel();
                ru.nsu.ccfit.zuev.osu.ReplayControlBridge.wireReplayPanel(
                    replayPanel,
                    this
                );
                // The movement tab only affects the AUTOPLAY cursor; hide it when
                // watching a replay (the cursor belongs to the recording).
                replayPanel.setMovementTabVisible(!replaying);
                hud.attachChild(replayPanel);
            }
        }

        skipBtn = null;
        if (skipTime > 1) {
            float paddingBottom = Multiplayer.isConnected()
                ? Multiplayer.roomScene.getChat().getButtonHeight()
                : 0f;

            skipBtn = new UIAnimatedSprite(
                "play-skip",
                true,
                OsuSkin.get().getAnimationFramerate()
            );
            skipBtn.setOrigin(Anchor.BottomRight);
            skipBtn.setPosition(
                Config.getRES_WIDTH(),
                Config.getRES_HEIGHT() - paddingBottom
            );
            skipBtn.setAlpha(0.7f);
            hud.attachChild(skipBtn);
        }

        String playname = Config.getOnlineUsername();

        if (GameHelper.isAutoplay() || replaying) {
            playname = replaying
                ? GlobalManager.getInstance()
                      .getScoring()
                      .getReplayStat()
                      .getPlayerName()
                : "osu!";

            if (!Config.isHideReplayMarquee()) {
                var replayText = new ChangeableText(
                    0,
                    0,
                    ResourceManager.getInstance().getFont("font"),
                    "",
                    1000
                );
                replayText.setText(
                    "Watching " +
                        playname +
                        " play " +
                        metadata.artist +
                        " - " +
                        metadata.title +
                        " [" +
                        metadata.version +
                        "]"
                );
                replayText.registerEntityModifier(
                    new LoopEntityModifier(
                        new MoveXModifier(
                            40f,
                            Config.getRES_WIDTH() + 5,
                            -replayText.getWidth() - 5
                        )
                    )
                );
                replayText.setPosition(0, 140);
                replayText.setAlpha(0.7f);
                hud.attachChild(replayText, 0);
            }
        } else if (
            Multiplayer.room != null && Multiplayer.room.isTeamVersus()
        ) {
            //noinspection DataFlowIssue
            playname = Multiplayer.player.getTeam().toString();
        }
        stat.setPlayerName(playname);

        breakAnimator = new BreakAnimator(fgScene, stat, hud);

        // Activate batched rendering during replay playback

        if (Multiplayer.isMultiplayer) {
            RoomAPI.INSTANCE.notifyBeatmapLoaded();
        } else {
            isReadyToStart = true;
        }
    }

    /**
     * Starts gameplay. This is used by the game loader once all necessary preprocessing is done.
     */
    public void start() {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        totalOffset = Config.getOffset();

        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(
            lastBeatmapInfo.getSetDirectory()
        );
        if (props != null) {
            totalOffset += props.getOffset();
        }

        totalOffset /= 1000;

        // Ensure user-defined offset has time to be applied.
        var firstObjectTimePreempt =
            (float) playableBeatmap.getHitObjects().objects.get(0).timePreempt /
            1000;
        elapsedTime = Math.min(
            elapsedTime,
            firstObjectStartTime -
                firstObjectTimePreempt -
                getRateAdjustedOffset()
        );
        initialElapsedTime = elapsedTime;

        if (skipTime <= 1 && Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        applyPlayfieldSizeScale();
        applyBackground();

        if (!isHUDEditorMode && !Config.isShowScoreboard()) {
            hud.detachChild(e -> e instanceof HUDLeaderboard);
        }

        if (
            !isHUDEditorMode &&
            !replaying &&
            !GameHelper.isAutoplay() &&
            !GameHelper.isAutopilot()
        ) {
            // Enable historical event processing and raw pointer for sub-frame precision.
            // In decoupled mode (target FPS > display rate), always enable for lowest latency.
            boolean useHighPrecision = Config.isHighPrecisionInput()
                || FrameLimiter.getInstance().getTargetFps() > FrameLimiter.getInstance().getDisplayRefreshRate();

            var touchOptions = new TouchOptions();
            touchOptions.setRunOnUpdateThread(true);
            touchOptions.setProcessHistoricalEvents(useHighPrecision);
            touchOptions.setUseRawPointer(useHighPrecision);

            var touchController = engine.getTouchController();
            touchController.applyTouchOptions(touchOptions);
            touchController.resetRawPointers();

            // Stale samples from a previous session (or pre-start touches) must not
            // leak into gameplay as phantom DOWN/MOVE events.
            var directInputView = GlobalManager.getInstance().getMainActivity().getDirectInputSurface();
            if (directInputView != null) {
                directInputView.clearPointerSamples();
            }
            // Input sampling is fully covered by the SPSC sample queue + raw pointers;
            // there is deliberately no Choreographer-based vsync polling here.
        }

        // Disable screen dimming
        engine.getEngineOptions().setWakeLockOptions(WakeLockOptions.SCREEN_ON);
        GlobalManager.getInstance()
            .getMainActivity()
            .runOnUiThread(() ->
                GlobalManager.getInstance().getMainActivity().reapplyWakeLock()
            );

        engine.setScene(scene);
        engine.getOverlay().attachChild(hud, 0);

        if (isHUDEditorMode) {
            ToastLogger.showText(R.string.hudEditor_back_for_menu, false);
        }
    }

    public Color4 getComboColor(HitObject hitObject) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap != null && GameHelper.isSynesthesia()) {
            return ModSynesthesia.getColorFor(
                playableBeatmap
                    .getControlPoints()
                    .getClosestBeatDivisor(hitObject.startTime)
            );
        }

        return comboColors.get(
            hitObject.getComboIndexWithOffsets() % comboColors.size()
        );
    }

    /**
     * Loads the combo colors and slider border color from the beatmap skin, the
     * user's custom colors, or the forceOverride colors of the current skin — in
     * that priority order. Extracted from the map-loading path so a MID-GAME skin
     * hot-swap can re-run it: {@link OsuSkin#get()}'s comboColor list and border
     * color are replaced by loadSkin(), and gameplay objects spawned afterwards
     * must pick up the new skin's palette, not the one captured at map load.
     */
    private void loadSkinDrivenColors() {
        var playableBeatmap = this.playableBeatmap;

        sliderBorderColor = BeatmapSkinManager.getInstance().getSliderColor();
        if (playableBeatmap != null && playableBeatmap.getColors().getSliderBorderColor() != null) {
            sliderBorderColor = playableBeatmap
                .getColors()
                .getSliderBorderColor();
        }

        if (OsuSkin.get().isForceOverrideSliderBorderColor()) {
            sliderBorderColor = OsuSkin.get().getSliderBorderColor();
        }

        comboColors = new ArrayList<>();
        if (playableBeatmap != null) {
            for (ComboColor comboColor : playableBeatmap.getColors().comboColors) {
                comboColors.add(comboColor.getColor());
            }
        }

        if (comboColors.isEmpty() || Config.isUseCustomComboColors()) {
            comboColors.clear();
            comboColors.addAll(Arrays.asList(Config.getComboColors()));
        }
        if (OsuSkin.get().isForceOverrideComboColor()) {
            comboColors.clear();
            comboColors.addAll(OsuSkin.get().getComboColor());
        }
    }

    private void update(final float dt) {
        if (!isReadyToStart) {
            return;
        }

        elapsedTime += dt;
        previousFrameTime = SystemClock.uptimeMillis();

        // Dispatch game update to Lua plugins
        com.osudroid.plugin.GameState.setElapsedTime(elapsedTime);
        com.osudroid.plugin.PluginManager.getInstance().dispatchGameUpdate(dt, elapsedTime);

        // Update cursor position for plugins
        updatePluginCursor();
        com.osudroid.plugin.PluginManager.getInstance().dispatchCursorUpdate(
            com.osudroid.plugin.GameState.getCursorX(),
            com.osudroid.plugin.GameState.getCursorY(),
            (long) (elapsedTime * 1000)
        );

        // Sync scoring state for plugins
        try {
            com.osudroid.plugin.GameState.setAccuracy((float) stat.getAccuracy());
            com.osudroid.plugin.GameState.setScore(stat.getTotalScore());
            com.osudroid.plugin.GameState.setCombo(stat.getCombo());
            com.osudroid.plugin.GameState.setMaxCombo(stat.getScoreMaxCombo());
            com.osudroid.plugin.GameState.setCurrentHp((float) stat.getHp());
            com.osudroid.plugin.GameState.setObjectsHit(stat.getNotesHit());
            com.osudroid.plugin.GameState.setMisses(stat.getMisses());
            com.osudroid.plugin.GameState.setIsKiai(GameHelper.isKiai());
            com.osudroid.plugin.GameState.setIsFlashlight(GameHelper.isFlashlight());
            com.osudroid.plugin.GameState.setIsRelax(GameHelper.isRelax());
            com.osudroid.plugin.GameState.setIsAutoplay(GameHelper.isAutoplay());
            // isSliderTracking is set by onTrackingSliders()
            com.osudroid.plugin.GameState.setActiveObjectCount(activeObjects != null ? activeObjects.size() : 0);
        } catch (Exception ignored) {}

        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (Multiplayer.isMultiplayer) {
            long mSecElapsed = (long) ((dt / GameHelper.getSpeedMultiplier()) *
                1000);
            realTimeElapsed += mSecElapsed;
            statisticDataTimeElapsed += mSecElapsed;

            // Sending statistics data every 3000ms if data was changed
            if (statisticDataTimeElapsed > 3000) {
                statisticDataTimeElapsed %= 3000;

                if (Multiplayer.isConnected()) {
                    var liveScore = stat.toBoardItem();

                    if (!Objects.equals(liveScore, lastScoreSent)) {
                        lastScoreSent = liveScore;
                        Execution.async(() ->
                            Execution.runSafe(() ->
                                RoomAPI.submitLiveScore(lastScoreSent.toJson())
                            )
                        );
                    }
                }
            }
        }

        final float mSecPassed = elapsedTime * 1000;

        if (!isGameOver) {
            // Match osu-droid: the effective rate is the mod rate multiplied by the
            // replay panel's playback rate.
            float currentSpeedMultiplier =
                getRateAt(mSecPassed) * replaySettingsRate;

            if (currentSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(currentSpeedMultiplier);

                var songService = GlobalManager.getInstance().getSongService();
                if (songService != null) {
                    // Upstream: mod rate goes to tempo (setSpeed), the panel rate goes to
                    // pitchRate. Both combine inside BASS to the effective playback speed.
                    songService.setSpeed(getRateAt(mSecPassed));
                    songService.setPitchRate(replaySettingsRate);
                }
            }
        }

        if (storyboardSprite != null && storyboardSprite.hasParent()) {
            storyboardSprite.updateTime(mSecPassed);
        }

        if (replaying) {
            int cIndex;
            for (int i = 0; i < replay.cursorIndex.length; i++) {
                if (replay.cursorMoves.size() <= i) {
                    break;
                }

                cIndex = replay.cursorIndex[i];
                Replay.ReplayMovement movement = null;

                // Emulating moves
                while (
                    cIndex < replay.cursorMoves.get(i).size &&
                    (movement = replay.cursorMoves
                        .get(i)
                        .movements[cIndex]).getTime() <=
                        (elapsedTime + dt / 4) * 1000
                ) {
                    var event = CursorEvent.obtain();

                    event.systemTime = movement.getTime();
                    event.trackTime = movement.getTime();
                    event.offset = 0;
                    event.isRealInput = false;
                    event.position.set(movement.getX(), movement.getY());

                    if (movement.getTouchType() == TouchType.DOWN) {
                        event.action = TouchEvent.ACTION_DOWN;
                        replay.lastMoveIndex[i] = -1;
                        hud.onGameplayTouchDown(movement.getTime() / 1000f);
                    } else if (movement.getTouchType() == TouchType.MOVE) {
                        event.action = TouchEvent.ACTION_MOVE;
                        replay.lastMoveIndex[i] = cIndex;
                    } else {
                        event.action = TouchEvent.ACTION_UP;
                    }
                    cursors[i].addEvent(event);
                    replay.cursorIndex[i]++;
                    cIndex++;
                }
                // Interpolating cursor movements
                if (
                    movement != null &&
                    movement.getTouchType() == TouchType.MOVE &&
                    replay.lastMoveIndex[i] >= 0
                ) {
                    final int lIndex = replay.lastMoveIndex[i];
                    final Replay.ReplayMovement lastMovement =
                        replay.cursorMoves.get(i).movements[lIndex];
                    int movementTime = movement.getTime();
                    int lastMovementTime = lastMovement.getTime();
                    int duration = lastMovementTime - movementTime;

                    if (duration == 0) {
                        continue;
                    }

                    float t = (mSecPassed - movementTime) / duration;

                    var event = CursorEvent.obtain();

                    // We don't exactly need systemTime to be accurate here since it's not used for anything important
                    // in replays.
                    event.systemTime = (long) mSecPassed;
                    event.trackTime = (long) mSecPassed;
                    event.offset = 0;
                    event.isRealInput = false;
                    event.action = TouchEvent.ACTION_MOVE;
                    event.position.set(
                        lastMovement.getX() * t + movement.getX() * (1 - t),
                        lastMovement.getY() * t + movement.getY() * (1 - t)
                    );

                    cursors[i].addEvent(event);
                }
            }
        }

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.update(dt);
            autoCursor.updateMovement(dt, elapsedTime);
        } else if (cursorSprites != null) {
            for (int i = 0; i < cursorSprites.length; i++) {
                var sprite = cursorSprites[i];
                var cursor = cursors[i];
                var latestEvent = cursor.getLatestEvent();

                // Replay path: update the sprite position/visibility before sprite.update(dt),
                // so the trail never lags a frame behind the cursor.
                if (replaying && latestEvent != null) {
                    // UP events carry no position (stored as (0,0) on disk) — keep the last
                    // real position and only hide the cursor, otherwise it would jump to
                    // the top-left corner on global replays.
                    if (!latestEvent.isActionUp()) {
                        sprite.setPosition(
                            latestEvent.position.x,
                            latestEvent.position.y
                        );
                        sprite.setShowing(true);

                        // Recorded press: the trail must restart from the new tap instead of
                        // interpolating from the previous gesture's lift point.
                        if (latestEvent.isActionDown()) {
                            sprite.onCursorPress();
                        }
                    } else {
                        sprite.setShowing(false);
                    }
                    // Enable trail immediately during replay (bypass 1s delay)
                    sprite.setForceTrailEnabled(true);
                } else if (!replaying) {
                    sprite.setShowing(cursor.isMouseDown());
                }

                sprite.update(dt);

                if (cursor.getLatestEvent(TouchEvent.ACTION_DOWN) != null) {
                    sprite.click();
                }
            }
        }

        if (GameHelper.isFlashlight()) {
            if (!GameHelper.isAutoplay() && !GameHelper.isAutopilot()) {
                // Check if the main cursor is still valid.
                if (mainCursorId >= 0 && !cursors[mainCursorId].isMouseDown()) {
                    mainCursorId = -1;
                }

                // If no cursor is valid, check for the latest pressed cursor.
                if (mainCursorId < 0) {
                    int index = -1;
                    CursorEvent latestDownEvent = null;

                    for (int i = 0; i < cursors.length; ++i) {
                        var c = cursors[i];
                        var latestCursorDownEvent = c.getLatestEvent(
                            TouchEvent.ACTION_DOWN
                        );

                        if (latestCursorDownEvent == null) {
                            continue;
                        }

                        if (
                            latestDownEvent == null ||
                            latestDownEvent.systemTime <
                                latestCursorDownEvent.systemTime
                        ) {
                            latestDownEvent = latestCursorDownEvent;
                            index = i;
                        }
                    }

                    if (latestDownEvent != null) {
                        mainCursorId = index;
                    }
                }

                if (mainCursorId != -1) {
                    var cursor = cursors[mainCursorId];
                    var latestNonUpEvent = cursor.getLatestEvent(
                        TouchEvent.ACTION_DOWN,
                        TouchEvent.ACTION_MOVE
                    );

                    if (latestNonUpEvent != null) {
                        flashlightSprite.onMouseMove(
                            latestNonUpEvent.position.x,
                            latestNonUpEvent.position.y
                        );
                    }
                }
            }

            flashlightSprite.onUpdate(stat.getCombo());
        }

        while (timingControlPointIndex + 1 < timingControlPoints.length) {
            var nextTimingPoint = timingControlPoints[
                timingControlPointIndex + 1
            ];

            if (nextTimingPoint.time > mSecPassed) {
                break;
            }

            activeTimingPoint = nextTimingPoint;
            ++timingControlPointIndex;
        }

        while (effectControlPointIndex + 1 < effectControlPoints.length) {
            var nextEffectPoint = effectControlPoints[
                effectControlPointIndex + 1
            ];

            if (nextEffectPoint.time > mSecPassed) {
                break;
            }

            activeEffectPoint = nextEffectPoint;
            ++effectControlPointIndex;
        }

        GameHelper.setBeatLength(activeTimingPoint.msPerBeat / 1000);
        GameHelper.setKiai(activeEffectPoint.isKiai);
        GameHelper.setCurrentBeatTime(
            Math.max(0, elapsedTime - activeTimingPoint.time / 1000) %
                GameHelper.getBeatLength()
        );

        updateKiaiEffects();
        updateKiaiFlash(dt);
        updateLowHealthOverlay(dt);

        if (screenShake != null) screenShake.update(dt);

        if (Config.isParallaxEnabled() && beatmapBackground != null) {
            float cursorNX = 0f, cursorNY = 0f;
            if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                if (autoCursor != null) {
                    cursorNX = (autoCursor.getX() / Config.getRES_WIDTH() - 0.5f) * 2f;
                    cursorNY = (autoCursor.getY() / Config.getRES_HEIGHT() - 0.5f) * 2f;
                }
            } else if (cursorSprites != null) {
                for (var s : cursorSprites) {
                    if (s.getX() > 0) {
                        cursorNX = (s.getX() / Config.getRES_WIDTH() - 0.5f) * 2f;
                        cursorNY = (s.getY() / Config.getRES_HEIGHT() - 0.5f) * 2f;
                        break;
                    }
                }
            } else if (mainCursorId >= 0 && mainCursorId < cursors.length) {
                var latest = cursors[mainCursorId].getLatestEvent(TouchEvent.ACTION_DOWN, TouchEvent.ACTION_MOVE);
                if (latest != null) {
                    cursorNX = (latest.position.x / Config.getRES_WIDTH() - 0.5f) * 2f;
                    cursorNY = (latest.position.y / Config.getRES_HEIGHT() - 0.5f) * 2f;
                }
            }
            cursorNX = Math.max(-1f, Math.min(1f, cursorNX));
            cursorNY = Math.max(-1f, Math.min(1f, cursorNY));
            float targetPX = cursorNX * 0.1f * Config.getRES_WIDTH() * 0.5f;
            float targetPY = cursorNY * 0.1f * Config.getRES_HEIGHT() * 0.5f;
            float delta = Math.abs(elapsedTime - parallaxLastTime) * 1000f;
            float p = (float) Math.pow(0.5, delta / 100.0);
            parallaxPosX = (float) (targetPX * (1 - p) + p * parallaxPosX);
            parallaxPosY = (float) (targetPY * (1 - p) + p * parallaxPosY);
            parallaxLastTime = elapsedTime;
            beatmapBackground.setPosition(bgBaseX + parallaxPosX, bgBaseY + parallaxPosY);
        }

        // Update replay seek position (panel is attached for both replay and autoplay).
        if (
            replayPanel != null &&
            objects != null &&
            objects.length > 0
        ) {
            ru.nsu.ccfit.zuev.osu.ReplayControlBridge.updateSeekPosition(
                replayPanel,
                elapsedTime,
                (float) objects[0].startTime / 1000,
                (float) objects[objects.length - 1].getEndTime() / 1000
            );
        }

        if (!isGameOver) {
            if (breakPeriodIndex < breakPeriods.length) {
                if (
                    !breakAnimator.isBreak() &&
                    breakPeriods[breakPeriodIndex].startTime / 1000 <=
                        elapsedTime
                ) {
                    var period = breakPeriods[breakPeriodIndex++];

                    gameStarted = false;
                    breakAnimator.init(period.getDuration() / 1000);
                    if (GameHelper.isFlashlight()) {
                        flashlightSprite.onBreak(true);
                    }

                    if (Multiplayer.isConnected()) Multiplayer.roomScene
                        .getChat()
                        .show();

                    hud.onBreakStateChange(true);
                }
            }

            if (breakAnimator.isOver()) {
                // Ensure the multiplayer chat is dismissed if it's still shown
                if (Multiplayer.isConnected()) {
                    Multiplayer.roomScene.getChat().hide();
                }

                gameStarted = true;
                hud.onBreakStateChange(false);

                if (GameHelper.isFlashlight()) {
                    flashlightSprite.onBreak(false);
                }
            }
        }

        if (
            objectIndex >= objects.length &&
            activeObjects.isEmpty() &&
            GameHelper.isFlashlight()
        ) {
            flashlightSprite.onBreak(true);
        }

        updateLetterbox(elapsedTime);

        if (gameStarted) {
            double rate = 0.375;
            if (
                playableBeatmap.getDifficulty().hp > 0 && distToNextObject > 0
            ) {
                rate =
                    1 +
                    playableBeatmap.getDifficulty().hp / (2 * distToNextObject);
            }
            stat.changeHp((float) -rate * 0.01f * dt);

            if (stat.getHp() <= 0 && stat.canFail) {
                if (GameHelper.isEasy() && failcount < 3) {
                    failcount++;
                    stat.changeHp(1f);
                } else {
                    if (Multiplayer.isMultiplayer) {
                        if (!hasFailed) {
                            ToastLogger.showText(
                                "You failed but you can continue playing.",
                                false
                            );
                        }
                        hasFailed = true;
                    } else {
                        gameover();
                        return;
                    }
                }
            }
        }

        if (comboBurst != null) {
            if (stat.getCombo() == 0) {
                comboBurst.breakCombo();
            } else {
                comboBurst.checkAndShow(stat.getCombo());
            }
        }

        // Clearing expired objects. onExpire() detaches visuals, stops looping samples, releases pooled
        // resources and returns the object to its pool. The identity set guards against double-expiry when a
        // seek force-expired an object that was still queued in expiredObjects.
        processedExpiredObjects.clear();

        if (!expiredObjects.isEmpty()) {
            for (int i = 0, size = expiredObjects.size(); i < size; i++) {
                var obj = expiredObjects.get(i);

                if (processedExpiredObjects.add(obj)) {
                    obj.onExpire();
                }
            }

            activeObjects.removeAll(expiredObjects);
            expiredObjects.clear();
        }

        updatePassiveObjects(dt);
        updateActiveObjects(dt);

        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.moveToObject(
                activeObjects.isEmpty() ? null : activeObjects.get(0),
                elapsedTime,
                this,
                activeObjects
            );
        }

        if (videoEnabled && video != null && elapsedTime >= videoOffset) {
            if (!videoStarted) {
                video.play();
                // Some devices do not support custom playback speed for whatever reason.
                try {
                    video.setPlaybackSpeed(GameHelper.getSpeedMultiplier());
                } catch (Exception e) {
                    Log.e(
                        "GameScene",
                        "Failed to change video playback speed.",
                        e
                    );
                    ToastLogger.showText(
                        com.osudroid.resources.R.string.message_video_custom_speed_unsupported,
                        false
                    );
                }
                videoStarted = true;
            }

            if (video.getAlpha() < 1.0f) video.setAlpha(
                Math.min(video.getAlpha() + 0.03f, 1.0f)
            );
        }

        if (elapsedTime >= getRateAdjustedOffset() && !musicStarted) {
            musicStarted = true;

            Execution.updateThread(() -> {
                // Start the music in the next update tick to ensure the most minimum time difference between the music
                // start and the game start.
                var songService = GlobalManager.getInstance().getSongService();

                songService.play();
                songService.setVolume(Config.getBgmVolume());
            });
        }

        boolean shouldBePunished = false;

        while (objectIndex < objects.length) {
            var obj = objects[objectIndex];

            // The casts can be simplified, but it is necessary to prevent floating point errors (see how
            // GameplayHitCircle and GameplaySlider track their passed time, where startTime and timePreempt
            // are cast and converted to seconds individually).
            if (
                elapsedTime <
                (float) obj.startTime / 1000 - (float) obj.timePreempt / 1000
            ) {
                break;
            }

            gameStarted = true;
            ++objectIndex;

            if (unrankedSprite != null) {
                unrankedSprite.registerEntityModifier(
                    Modifiers.sequence(
                        IEntity::detachSelf,
                        Modifiers.delay(1.5f - elapsedTime),
                        Modifiers.parallel(
                            Modifiers.scale(0.5f, 1, 1.5f),
                            Modifiers.fadeOut(0.5f)
                        )
                    )
                );

                // Make it null to avoid multiple entity modifier registration
                unrankedSprite = null;
            }

            if (obj.startTime > totalLength) {
                shouldBePunished = true;
                break;
            }

            final var nextObj =
                objectIndex < objects.length ? objects[objectIndex] : null;

            distToNextObject =
                nextObj != null
                    ? Math.max(
                          nextObj.startTime - obj.startTime,
                          activeTimingPoint.msPerBeat / 2
                      ) / 1000
                    : 0;

            hud.onHitObjectLifetimeStart(obj);

            final Color4 comboColor = getComboColor(obj);

            if (obj instanceof HitCircle parsedCircle) {
                final var gameplayCircle =
                    GameObjectPool.getInstance().getCircle();

                gameplayCircle.init(
                    this,
                    mgScene,
                    parsedCircle,
                    elapsedTime,
                    comboColor
                );
                addObject(gameplayCircle);

                if (GameHelper.isAutoplay()) {
                    gameplayCircle.setAutoPlay();
                }

                gameplayCircle.setId(++lastObjectId);

                if (replaying) {
                    gameplayCircle.setReplayData(
                        replay.objectData[gameplayCircle.getId()]
                    );
                }
            } else if (obj instanceof Spinner parsedSpinner) {
                final float rps =
                    2 + (2 * playableBeatmap.getDifficulty().od) / 10f;
                final var gameplaySpinner =
                    GameObjectPool.getInstance().getSpinner();

                gameplaySpinner.init(this, bgScene, parsedSpinner, rps, stat);
                addObject(gameplaySpinner);

                if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    gameplaySpinner.setAutoPlay();
                }

                gameplaySpinner.setId(++lastObjectId);
                if (replaying) {
                    gameplaySpinner.setReplayData(
                        replay.objectData[gameplaySpinner.getId()]
                    );
                }
            } else if (obj instanceof Slider parsedSlider) {
                final var gameplaySlider =
                    GameObjectPool.getInstance().getSlider();

                gameplaySlider.init(
                    this,
                    mgScene,
                    stat,
                    parsedSlider,
                    playableBeatmap.getControlPoints(),
                    elapsedTime,
                    comboColor,
                    sliderBorderColor,
                    getSliderPath(sliderIndex),
                    getSliderRenderPath(sliderIndex)
                );

                ++sliderIndex;
                addObject(gameplaySlider);

                if (GameHelper.isAutoplay()) {
                    gameplaySlider.setAutoPlay();
                }

                gameplaySlider.setId(++lastObjectId);
                if (replaying) {
                    gameplaySlider.setReplayData(
                        replay.objectData[gameplaySlider.getId()]
                    );
                    if (
                        gameplaySlider.getReplayData().tickSet == null
                    ) gameplaySlider.getReplayData().tickSet = new BitSet();
                }
            }

            if (
                !(obj instanceof Spinner) &&
                nextObj != null &&
                !(nextObj instanceof Spinner) &&
                !obj.isLastInCombo()
            ) {
                FollowPointConnection.addConnection(
                    bgScene,
                    elapsedTime,
                    obj,
                    nextObj
                );
            }
        }

        var mutedMod = GameHelper.getMuted();

        // 节拍器
        if (metronome != null) {
            metronome.update(elapsedTime, activeTimingPoint);

            if (mutedMod != null) {
                metronome.setVolume(1 - mutedMod.volumeAt(stat.getCombo()));
            }
        }

        if (musicStarted && mutedMod != null) {
            GlobalManager.getInstance()
                .getSongService()
                .setVolume(
                    Config.getBgmVolume() * mutedMod.volumeAt(stat.getCombo())
                );
        }

        if (
            shouldBePunished ||
            (!isGameOver &&
                objectIndex >= objects.length &&
                activeObjects.isEmpty() &&
                leadOut > 2)
        ) {
            // Reset the game to continue the HUD editor session.
            if (startedFromHUDEditor && isHUDEditorMode) {
                elapsedTime = initialElapsedTime;
                loadGame(lastBeatmapInfo, null, lastMods, null);
                stat.reset();
                skip(true);
                return;
            }

            scene = createMainScene();
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            timingControlPoints = null;
            effectControlPoints = null;
            objects = null;
            activeObjects.clear();
            expiredObjects.clear();
            breakPeriods = null;
            cursorSprites = null;
            if (particleSystem != null) {
                for (var particleSpout : particleSystem) {
                    if (particleSpout != null) {
                        particleSpout.setParticlesSpawnEnabled(false);
                        particleSpout.clearUpdateHandlers();
                        particleSpout.detachSelf();
                    }
                }
            }
            if (kiaiFlashOverlay != null) {
                kiaiFlashOverlay.detachSelf();
                kiaiFlashOverlay = null;
            }

            if (lowHealthOverlay != null) {
                lowHealthOverlay.detachSelf();
                lowHealthOverlay = null;
            }
            lowHealthAlpha = 0f;

            this.playableBeatmap = null;
            performanceCalculationParameters = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
            String replayPath = null;
            stat.setTime(System.currentTimeMillis());
            if (replay != null && !replaying) {
                String ctime = String.valueOf(System.currentTimeMillis());
                replayPath =
                    Config.getCorePath() +
                    "Scores/" +
                    MD5Calculator.getStringMD5(
                        lastBeatmapInfo.getFilename() + ctime
                    ) +
                    ctime.substring(0, Math.min(3, ctime.length())) +
                    ".odr";
                replay.setStat(stat);
                replay.save(replayPath);
            }
            resetPlayfieldSizeScale();
            cancelStoryboardLoading();
            cancelVideoLoading();


        if (scoringScene != null && !startedFromHUDEditor) {
            ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance().reportPresence("online", null);
            if (replaying) scoringScene.load(
                    scoringScene.getReplayStat(),
                    null,
                    GlobalManager.getInstance().getSongService(),
                    replayPath,
                    null,
                    lastBeatmapInfo
                );
                else {
                    if (stat.getMod().contains(ModAutoplay.class)) {
                        stat.setPlayerName("osu!");
                    }

                    if (Multiplayer.isConnected()) {
                        Multiplayer.log(
                            "Match ended, moving to results scene."
                        );
                        Multiplayer.roomScene.getChat().show();

                        Execution.async(() ->
                            Execution.runSafe(() ->
                                RoomAPI.submitFinalScore(stat.toJson())
                            )
                        );

                        ToastLogger.showText(
                            "Loading room statistics...",
                            false
                        );
                    }
                    scoringScene.load(
                        stat,
                        lastBeatmapInfo,
                        GlobalManager.getInstance().getSongService(),
                        replayPath,
                        parsedBeatmap.getMd5(),
                        null
                    );
                }
                GlobalManager.getInstance().getSongService().setVolume(0.2f);
                engine.setScene(scoringScene.getScene());
            } else {
                engine.setScene(oldScene);
            }

            // Dispatch beatmap finished event to plugins
            try {
                String grade = stat.getMark();
                com.osudroid.plugin.PluginManager.getInstance().dispatchBeatmapFinished(
                    stat.getTotalScore(),
                    stat.getScoreMaxCombo(),
                    (float) stat.getAccuracy(),
                    grade
                );
            } catch (Exception ignored) {}

            // Clean up plugin system for this gameplay session
            com.osudroid.plugin.PluginManager.getInstance().destroyGameplay();

            // Resume difficulty calculation.
            DifficultyCalculationManager.calculateDifficulties();

            // Disable historical event processing for more efficient ACTION_MOVE reports. Frequent reports are not
            // relevant outside gameplay.
            var touchOptions = new TouchOptions();
            touchOptions.setRunOnUpdateThread(true);
            touchOptions.setProcessHistoricalEvents(false);
            touchOptions.setUseRawPointer(false);
            engine.getTouchController().applyTouchOptions(touchOptions);

            engine
                .getEngineOptions()
                .setWakeLockOptions(WakeLockOptions.SCREEN_ON);
            GlobalManager.getInstance()
                .getMainActivity()
                .runOnUiThread(() ->
                    GlobalManager.getInstance()
                        .getMainActivity()
                        .reapplyWakeLock()
                );

            if (video != null) {
                video.release();
                video = null;
                videoStarted = false;
            }

            parsedBeatmap = null;
        } else if (objectIndex >= objects.length && activeObjects.isEmpty()) {
            gameStarted = false;
            leadOut += dt;
        }

        if (elapsedTime > skipTime - 1f && skipBtn != null) {
            if (Multiplayer.isConnected()) {
                Multiplayer.roomScene.getChat().hide();
            }
            skipBtn.detachSelf();
            skipBtn = null;
        } else if (skipBtn != null) {
            for (int i = 0; i < cursors.length; ++i) {
                // This hit test uses cursor events, which for autoplay are the synthetic
                // autoplay cursor (the only "finger" there is). During replay playback real
                // touches are handled directly in onSceneTouchEvent (the replay gate), so
                // they never enter cursors[] and cannot skip from here.
                CursorEvent latestDownEvent = cursors[i].getLatestEvent(
                      TouchEvent.ACTION_DOWN,
                      TouchEvent.ACTION_MOVE
                  );

                if (
                    latestDownEvent != null &&
                    Utils.squaredDistance(
                        latestDownEvent.position.x,
                        latestDownEvent.position.y,
                        Config.getRES_WIDTH(),
                        Config.getRES_HEIGHT()
                    ) <
                        250 * 250
                ) {
                    if (Multiplayer.isConnected()) {
                        if (!isSkipRequested) {
                            isSkipRequested = true;
                            ResourceManager.getInstance()
                                .getSound("menuhit")
                                .play();
                            skipBtn.setVisible(false);

                            Execution.async(RoomAPI.INSTANCE::requestSkip);
                            ToastLogger.showText("Skip requested", false);
                        }
                        return;
                    }
                    if (skipBtn != null) {
                        skipBtn.detachSelf();
                        skipBtn = null;
                    }
                    skip();
                    return;
                }
            }
        }
    }

    private void updateActiveObjects(float deltaTime) {
        judgeableObject = searchJudgeableObject(0);

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);
            obj.update(deltaTime);

            if (Config.isRemoveSliderLock() && obj.isStartHit()) {
                judgeableObject = searchJudgeableObject(i + 1);
            }
        }
    }



    private void updatePassiveObjects(float deltaTime) {
        hud.onGameplayUpdate(this, deltaTime);

        breakAnimator.update(deltaTime);

        if (countdownAnimator != null) {
            countdownAnimator.update(deltaTime);
        }
    }

    @Nullable
    private GameObject searchJudgeableObject(int startIndex) {
        if (!Config.isRemoveSliderLock()) {
            return activeObjects.isEmpty() ? null : activeObjects.get(0);
        }

        for (int i = startIndex, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);

            if (!obj.isStartHit()) {
                return obj;
            }
        }

        return null;
    }

    public void skip() {
        skip(false);
    }

    public void skip(boolean force) {
        if (Multiplayer.isConnected()) {
            Multiplayer.roomScene.getChat().hide();
        }

        if (elapsedTime > skipTime - 1f && !force) {
            return;
        }

        ResourceManager.getInstance().getSound("menuhit").play();

        float difference = skipTime - elapsedTime;
        elapsedTime = skipTime;

        double elapsedTimeMs = Math.ceil(elapsedTime * 1000);

        // Seek times may be negative in forced skips, which are not supported by music and video.
        int musicSeekTime = Math.max(
            0,
            (int) (elapsedTimeMs -
                totalOffset * getRateAt(elapsedTimeMs) * 1000)
        );
        int videoSeekTime = Math.max(
            0,
            (int) (elapsedTimeMs - videoOffset * 1000)
        );

        Execution.updateThread(() -> {
            updatePassiveObjects(difference);

            var songService = GlobalManager.getInstance().getSongService();

            // Mirror seekReplay: never un-pause music the user explicitly paused via
            // the replay panel — the skip would otherwise blast audio while paused.
            if (elapsedTime >= getRateAdjustedOffset() && !musicStarted && !replayPlaybackPaused) {
                songService.play();
                songService.setVolume(Config.getBgmVolume());
                musicStarted = true;
            }

            songService.seekTo(musicSeekTime);

            if (videoEnabled && video != null) {
                video.seekTo(videoSeekTime);
            }

            if (skipBtn != null) {
                skipBtn.detachSelf();
                skipBtn = null;
            }
        });
    }

    /**
     * Pauses replay/autoplay playback honestly: gameplay time stops advancing (dt = 0),
     * music, video and looping samples are stopped. Mirrors osu-droid's
     * gameplayClock.stop() + stopLoopingSamples() + video.pause().
     */
    public void pauseReplayPlayback() {
        if (replayPlaybackPaused) {
            return;
        }

        replayPlaybackPaused = true;

        stopLoopingSamples();

        var songService = GlobalManager.getInstance().getSongService();

        if (songService != null && songService.getStatus() == Status.PLAYING) {
            songService.pause();
            replayPlaybackWasPlaying = true;
        } else {
            replayPlaybackWasPlaying = false;
        }

        if (video != null && videoStarted) {
            video.pause();
        }
    }

    /**
     * Resumes replay/autoplay playback after {@link #pauseReplayPlayback()}.
     */
    public void resumeReplayPlayback() {
        if (!replayPlaybackPaused) {
            return;
        }

        replayPlaybackPaused = false;

        playLoopingSamples();

        var songService = GlobalManager.getInstance().getSongService();

        if (songService != null && replayPlaybackWasPlaying && elapsedTime >= getRateAdjustedOffset()) {
            songService.play();
            songService.setVolume(Config.getBgmVolume());
        }

        if (video != null && videoStarted && elapsedTime >= videoOffset) {
            video.play();
        }
    }

    public boolean isReplayPlaybackPaused() {
        return replayPlaybackPaused;
    }

    /**
     * Called when the user changes background brightness via the replay visual settings panel.
     */
    public void onReplayBrightnessChanged(float brightness) {
        if (breakAnimator != null) {
            breakAnimator.setDimBrightness(brightness);

            // Match upstream: during a break the BreakAnimator owns the dim layer.
            if (!breakAnimator.isBreak() && dimRectangle != null) {
                dimRectangle.setAlpha(1f - brightness);
            }
        }
    }

    /**
     * Called when the user toggles parallax via the replay visual settings panel.
     * The background is rebuilt so the parallax scale factor (1.1x) applies immediately.
     */
    public void onReplayParallaxChanged(boolean enabled) {
        if (parallaxApplied == enabled) {
            return;
        }
        parallaxApplied = enabled;
        Config.setParallaxEnabled(enabled);
        applyBackground();
    }

    /**
     * Called after a mid-replay skin hot-swap finished reloading resources on a
     * background thread. Refreshes sprites whose textures were captured at
     * construction (cursor entities). Gameplay objects re-pull textures in their
     * init() as they spawn from the pool.
     */
    public void onReplaySkinChanged() {
        int refreshed = 0;
        if (cursorSprites != null) {
            for (var sprite : cursorSprites) {
                if (sprite != null) {
                    sprite.refreshSkinTextures();
                    refreshed++;
                }
            }
        }
        if (autoCursor != null) {
            autoCursor.refreshSkinTextures();
            refreshed++;
        }

        // Pooled gameplay objects capture TextureRegions in their constructor, so a
        // hot-swapped skin leaves the pool full of sprites bound to unloaded GL
        // textures. purge() drops them; the next spawn rebuilds from the new skin.
        GameObjectPool.getInstance().purge();
        // Follow points / slider ticks live in their own pools with the same problem.
        FollowPointConnection.refreshTextures();
        // In-flight follow points (attached to the scene, modifiers running) need
        // their region re-bound in place.
        FollowPointConnection.refreshLiveTextures();
        // Same for slider ticks, which are scene grandchildren.
        SliderTickSprite.refreshLiveTickTextures(scene);

        // Live objects keep the previous skin's TextureRegion until refreshed in place.
        // Combo colors are re-derived as well since the skin's palette changed with the swap.
        loadSkinDrivenColors();
        int liveRefreshed = 0;
        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            var obj = activeObjects.get(i);
            if (obj instanceof GameplayHitCircle circle) {
                circle.refreshSkinTextures();
                liveRefreshed++;
            } else if (obj instanceof GameplaySlider slider) {
                slider.refreshSkinTextures();
                liveRefreshed++;
            } else if (obj instanceof GameplaySpinner spinner) {
                spinner.refreshSkinTextures();
                liveRefreshed++;
            }
        }

        // HUD elements (score/combo/accuracy fonts, health bar sprites) capture skinned
        // textures at construction — rebuild them from the new skin. Runs on the update
        // thread (this whole callback is invoked there).
        if (hud != null) {
            hud.onSkinChanged();
        }

        // The skip button is an ANIMATABLE (play-skip-*) UIAnimatedSprite captured at
        // scene build; re-pull its frames from the new skin.
        if (skipBtn instanceof UIAnimatedSprite animatedSkip) {
            animatedSkip.setFrames("play-skip", true);
        }
    }

    /**
     * Called when the user changes the autoplay movement style (or one of its
     * sub-settings) via the visual settings panel during autoplay. The cursor
     * queue is re-baked with the new mover and the cursor resumes from the
     * current gameplay time.
     *
     * @param styleValue the new autoplayStyle value (null/empty = keep the current
     *                   style — only sub-settings were edited).
     */
    public void onReplayMovementStyleChanged(String styleValue) {
        if (autoCursor == null) {
            return;
        }

        AutoplayStyle style = (styleValue == null || styleValue.isEmpty())
            ? null
            : AutoplayStyle.fromValue(styleValue);

        // Re-baking the queue touches the trail + cursor position → update thread.
        com.osudroid.utils.Execution.updateThread(() ->
            autoCursor.applyStyleLive(
                style,
                playableBeatmap != null
                    ? playableBeatmap.getHitObjects().objects.toArray(new com.rian.osu.beatmap.hitobject.HitObject[0])
                    : null,
                this,
                elapsedTime
            )
        );
    }

    /**
     * Seek the replay/autoplay to a specific time (in seconds).
     * Resets game state so objects reappear on backward seek
     * and replay cursor events play correctly on forward seek.
     */
    public void seekReplay(float targetTimeSeconds) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || objects == null) {
            return;
        }
        // Safety: don't seek if timing points aren't initialized yet
        if (
            timingControlPoints == null || timingControlPoints.length == 0
        ) return;
        if (
            effectControlPoints == null || effectControlPoints.length == 0
        ) return;

        float clampedTime = Math.max(
            0,
            Math.min(
                targetTimeSeconds,
                (float) (objects[objects.length - 1].getEndTime() / 1000)
            )
        );

        float oldTime = elapsedTime;
        elapsedTime = clampedTime;
        float targetMs = clampedTime * 1000;

        // Seek music (also recompute the rate at the seek target, see osu-droid).
        double elapsedTimeMs = Math.ceil(targetMs);
        int musicSeekTime = Math.max(
            0,
            (int) (elapsedTimeMs -
                totalOffset * getRateAt(elapsedTimeMs) * 1000)
        );

        var songService = GlobalManager.getInstance().getSongService();
        if (songService != null) {
            // Do not un-pause music the user explicitly paused via the replay panel.
            if (elapsedTime >= getRateAdjustedOffset() && !musicStarted && !replayPlaybackPaused) {
                songService.play();
                songService.setVolume(Config.getBgmVolume());
                musicStarted = true;
            }
            songService.seekTo(musicSeekTime);
        }

        // Seek video.
        if (videoEnabled && video != null) {
            video.seekTo(Math.max(0, (int) ((clampedTime - videoOffset) * 1000)));
        }

        // ---- Reset game state so objects reappear ----

        // Force-expire all active objects (detach from scene and return to pool).
        for (int i = 0, size = activeObjects.size(); i < size; ++i) {
            var obj = activeObjects.get(i);

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire();
            }
        }

        // Also expire objects that were queued for cleanup but not yet processed.
        for (int i = 0, size = expiredObjects.size(); i < size; ++i) {
            var obj = expiredObjects.get(i);

            if (processedExpiredObjects.add(obj)) {
                obj.onExpire();
            }
        }

        activeObjects.clear();
        expiredObjects.clear();
        processedExpiredObjects.clear();
        judgeableObject = null;

        // Detach any lingering hit effects from the gameplay scene.
        for (int i = mgScene.getChildCount() - 1; i >= 0; --i) {
            var child = mgScene.getChild(i);

            // Hit effects are pooled; their fade-out callback schedules putEffect(), but on a hard detach the
            // callback is lost. Finishing the modifiers first lets GameEffect's own completion handler return
            // the wrapper to the pool rather than orphaning it.
            if (child instanceof com.reco1l.andengine.sprite.UISprite sprite) {
                sprite.finishModifiers();
            }

            child.detachSelf();
        }

        // Remove follow points spawned for objects before the seek point.
        FollowPointConnection.clearAll(bgScene);

        // Reset spawn counters.
        objectIndex = 0;
        sliderIndex = 0;
        lastObjectId = -1;
        gameStarted = false;
        leadOut = 0;
        comboWasMissed = false;
        comboWas100 = false;
        failcount = 0;
        isGameOver = false;
        hasFailed = false;

        // Rewind the auto cursor's precomputed queue to the seek target: the queue
        // only advances forward, so a backward seek without re-indexing left the
        // cursor at the old (future) segment's start position until gameplay caught
        // up. seekTo also restores spinning (at the correct angle) and marks a trail
        // discontinuity across the jump.
        if (autoCursor != null) {
            autoCursor.clearEntityModifiers();
            autoCursor.seekTo(elapsedTime);
        }

        // Clear pending cursor events.
        for (int i = 0; i < cursors.length; ++i) {
            var cursor = cursors[i];

            if (cursor != null) {
                cursor.reset(SystemClock.uptimeMillis(), 0);
                cursor.latestProcessedDownEventIndex = 0;
                cursor.latestProcessedEventIndex = 0;
            }

            // Reset trails: otherwise the trail stretches across the seek jump
            // (points from the old position interpolate to the new one).
            if (cursorSprites != null && cursorSprites[i] != null) {
                cursorSprites[i].resetTrail();
            }
        }

        // Advance timing and effect control points to the target time.
        timingControlPointIndex = 0;
        effectControlPointIndex = 0;

        while (
            timingControlPointIndex + 1 < timingControlPoints.length &&
            timingControlPoints[timingControlPointIndex + 1].time <= targetMs
        ) {
            timingControlPointIndex++;
        }

        while (
            effectControlPointIndex + 1 < effectControlPoints.length &&
            effectControlPoints[effectControlPointIndex + 1].time <= targetMs
        ) {
            effectControlPointIndex++;
        }

        activeTimingPoint = timingControlPoints.length > 0 ?
            timingControlPoints[timingControlPointIndex] :
            playableBeatmap.getControlPoints().timing.defaultControlPoint;

        activeEffectPoint = effectControlPoints.length > 0 ?
            effectControlPoints[effectControlPointIndex] :
            playableBeatmap.getControlPoints().effect.defaultControlPoint;

        // Advance break period index past fully elapsed breaks.
        breakPeriodIndex = 0;

        if (breakPeriods != null) {
            while (
                breakPeriodIndex < breakPeriods.length &&
                breakPeriods[breakPeriodIndex].endTime <= targetMs
            ) {
                breakPeriodIndex++;
            }
        }

        // Reset replay cursor movement indices.
        if (replaying && replay != null && replay.lastMoveIndex != null) {
            Arrays.fill(replay.cursorIndex, 0);
            Arrays.fill(replay.lastMoveIndex, -1);
        }

        // Reconstruct scoring state up to the seek target.
        stat.reset();
        reconstructStatAtTime(targetMs);

        // Reset any in-progress break animation, then re-initialize if the seek target is inside a break.
        breakAnimator.reset();

        if (breakPeriods != null && breakPeriodIndex < breakPeriods.length) {
            var bp = breakPeriods[breakPeriodIndex];

            if (bp.startTime <= targetMs && targetMs < bp.endTime) {
                gameStarted = false;
                float totalDuration = bp.getDuration() / 1000f;
                float breakElapsedTime = (float) ((targetMs - bp.startTime) / 1000.0);
                breakAnimator.init(totalDuration, breakElapsedTime);
                breakPeriodIndex++;
                hud.onBreakStateChange(true);
            } else {
                hud.onBreakStateChange(false);
            }
        } else {
            hud.onBreakStateChange(false);
        }

        // Recreate the skip button when seeking back into the lead-in: update() removes
        // it past skipTime and never restores it, but the replay-gate touch check needs
        // it present to allow skipping.
        if (elapsedTime < skipTime - 1f && skipBtn == null) {
            float paddingBottom = Multiplayer.isConnected()
                ? Multiplayer.roomScene.getChat().getButtonHeight()
                : 0f;

            skipBtn = new UIAnimatedSprite(
                "play-skip",
                true,
                OsuSkin.get().getAnimationFramerate()
            );
            skipBtn.setOrigin(Anchor.BottomRight);
            skipBtn.setPosition(
                Config.getRES_WIDTH(),
                Config.getRES_HEIGHT() - paddingBottom
            );
            skipBtn.setAlpha(0.7f);
            hud.attachChild(skipBtn);
        }

        hud.onNoteHit(stat);
        hud.onSeek();

        // Replay all touch-down events up to the seek target so HUD elements
        // that depend on touch history can reconstruct their state correctly.
        if (GameHelper.isAutoplay()) {
            for (int i = 0; i < objects.length; ++i) {
                float tapTime = (float) objects[i].startTime / 1000f;

                if (tapTime > clampedTime) {
                    break;
                }

                hud.onGameplayTouchDown(tapTime);
            }
        } else if (replaying) {
            int cursorCount = replay.cursorMoves.size();

            for (int i = 0; i < cursorCount; ++i) {
                var moveArray = replay.cursorMoves.get(i);

                for (int j = 0; j < moveArray.size; ++j) {
                    var movement = moveArray.movements[j];

                    float tapTime = movement.getTime() / 1000f;

                    if (tapTime > clampedTime) {
                        break;
                    }

                    if (movement.getTouchType() == TouchType.DOWN) {
                        hud.onGameplayTouchDown(tapTime);
                    }
                }
            }
        }

        updatePPValue(objectIndex - 1);

        // For variable-rate mods (WindUp/WindDown), the rate at the seek target may differ from
        // the current rate. The music seek above already accounted for the target rate, so sync
        // the engine-side multiplier too. Matches upstream: modRate is scaled by the panel rate.
        {
            float modRate = getRateAt(targetMs);
            float targetSpeedMultiplier = modRate * replaySettingsRate;

            if (targetSpeedMultiplier != GameHelper.getSpeedMultiplier()) {
                GameHelper.setSpeedMultiplier(targetSpeedMultiplier);

                if (songService != null) {
                    songService.setSpeed(modRate);
                    songService.setPitchRate(replaySettingsRate);
                }
            }
        }

        // Suppress hitsounds for objects judged on the seek target frame (they were already
        // reconstructed into the stat). Objects are spawned in the same frame as the seek, but
        // updated in the next frame, so the flag must survive 2 update frames.
        postSeekFrameCount = 2;

        android.util.Log.d(
            "GameScene",
            "Seek replay: " +
                oldTime +
                "s → " +
                elapsedTime +
                "s (music: " +
                musicSeekTime +
                "ms)"
        );
    }

    private void reconstructStatAtTime(float targetMs) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null || objects == null || objects.length == 0) {
            return;
        }

        var difficulty = playableBeatmap.getDifficulty();
        int localTimingIdx = 0;
        int localBreakIdx = 0;
        var objectData = replaying ? replay.objectData : null;

        // How far continuous HP drain has been applied, and which object-to-object rate segment
        // it's currently within ([objects[rateSegIdx].startTime - timePreempt,
        // objects[rateSegIdx + 1].startTime - timePreempt)). Both advance with elapsed
        // time and is independent of which object's own judgement has been applied yet.
        double drainCursorMs = objects[0].startTime - objects[0].timePreempt;
        int rateSegIdx = 0;

        for (int i = 0; i < objects.length; i++) {
            var obj = objects[i];

            double judgementTimeMs = getJudgementTimeMs(i, obj, objectData);

            if (judgementTimeMs > targetMs) {
                break;
            }

            double drainTargetMs = Math.min(judgementTimeMs, targetMs);

            while (drainCursorMs < drainTargetMs && rateSegIdx < objects.length - 1) {
                var segFromObj = objects[rateSegIdx];
                var segToObj = objects[rateSegIdx + 1];

                // Advance local timing point index to this segment's own "to" object.
                while (
                    localTimingIdx + 1 < timingControlPoints.length &&
                    timingControlPoints[localTimingIdx + 1].time <= segToObj.startTime
                ) {
                    localTimingIdx++;
                }

                double msPerBeat = timingControlPoints[localTimingIdx].msPerBeat;
                double distToNextObject = Math.max(segToObj.startTime - segFromObj.startTime, msPerBeat / 2) / 1000;

                double drainRate = difficulty.hp > 0 && distToNextObject > 0
                    ? 1 + difficulty.hp / (2 * distToNextObject)
                    : 0.375;

                double segEndMs = segToObj.startTime - segToObj.timePreempt;
                double stopAtMs = Math.min(segEndMs, drainTargetMs);

                if (stopAtMs > drainCursorMs) {
                    // Advance past breaks that fully precede the current segment.
                    if (breakPeriods != null) {
                        while (
                            localBreakIdx < breakPeriods.length &&
                            breakPeriods[localBreakIdx].endTime <= drainCursorMs
                        ) {
                            localBreakIdx++;
                        }
                    }

                    double effectiveSecs = calculateEffectiveDrainDuration(drainCursorMs, stopAtMs, localBreakIdx);

                    // Apply drain incrementally so that a large drain section can consume multiple Easy lives.
                    // A one-shot stat.changeHp clamps at 0 and loses the excess, causing at most one Easy revive per drain
                    // section regardless of how deep HP would have gone.
                    float remainingDrain = (float) (drainRate * 0.01 * effectiveSecs);

                    while (remainingDrain > 0) {
                        float currentHp = stat.getHp();

                        if (remainingDrain < currentHp) {
                            stat.changeHp(-remainingDrain);
                            break;
                        }

                        remainingDrain -= currentHp;
                        stat.changeHp(-currentHp);

                        if (!stat.canFail) {
                            break;
                        }

                        if (GameHelper.isEasy() && failcount < 3) {
                            failcount++;
                            stat.changeHp(1f);
                        } else {
                            return;
                        }
                    }

                    drainCursorMs = stopAtMs;
                }

                if (drainCursorMs >= segEndMs) {
                    rateSegIdx++;
                } else {
                    // Capped short of this segment's own end by drainTargetMs - stop crossing
                    // further for now. The remainder of this same segment continues once a
                    // later object's own judgement time lets drain proceed past it.
                    break;
                }
            }

            if (!gameStarted) {
                gameStarted = true;
            }

            var data = (objectData != null && i < objectData.length) ? objectData[i] : null;

            boolean endCombo = obj.isLastInCombo();

            if (obj instanceof HitCircle) {
                applyCircleResult(data, endCombo);
            } else if (obj instanceof Slider slider) {
                sliderIndex++;
                applySliderResult(slider, data, endCombo);
            } else if (obj instanceof Spinner parsedSpinner) {
                applySpinnerResult(parsedSpinner, data, endCombo);
            }

            lastObjectId = i;
            objectIndex = i + 1;
        }
    }

    private double getJudgementTimeMs(int idx, HitObject obj, @Nullable Replay.ReplayObjectData[] objectData) {
        if (obj instanceof Slider || obj instanceof Spinner) {
            return obj.getEndTime();
        }

        // Circle: judged when hit (or miss window expires).
        if (objectData != null && idx < objectData.length) {
            var data = objectData[idx];

            if (data != null && data.result != ResultType.MISS.getId()) {
                return obj.startTime + Math.abs(data.accuracy);
            }
        }

        double mehWindow = obj.hitWindow != null ? obj.hitWindow.getMehWindow() : HitWindow.MISS_WINDOW;

        return obj.startTime + mehWindow;
    }

    private void reconstructHitOffset(double accSeconds) {
        if (Math.abs(accSeconds) <= hitWindow.getMehWindow() / 1000) {
            stat.addHitOffset(accSeconds);
        }
    }

    private void applyCircleResult(@Nullable Replay.ReplayObjectData data, boolean endCombo) {
        byte result = data != null ? data.result : ResultType.HIT300.getId();

        if (result == ResultType.MISS.getId()) {
            comboWasMissed = true;
            stat.registerHit(0, false, false);
        } else {
            reconstructHitOffset(data != null ? data.accuracy / 1000.0 : 0.0);

            if (result == ResultType.HIT50.getId()) {
                stat.registerHit(50, false, false);
                comboWas100 = true;
            } else if (result == ResultType.HIT100.getId()) {
                comboWas100 = true;
                stat.registerHit(100, endCombo && !comboWasMissed, false);
            } else {
                if (endCombo && !comboWasMissed) {
                    stat.registerHit(300, true, !comboWas100);
                } else {
                    stat.registerHit(300, false, false);
                }
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }
    }

    private void applySliderResult(Slider slider, @Nullable Replay.ReplayObjectData data, boolean endCombo) {
        byte result = data != null ? data.result : ResultType.HIT300.getId();

        if (result == ResultType.MISS.getId()) {
            comboWasMissed = true;
            stat.registerHit(0, false, false);

            if (endCombo) {
                comboWas100 = false;
                comboWasMissed = false;
            }

            return;
        }

        // Slider head: HIT300 implies all ticks were hit; autoplay (data==null) always hits.
        // For HIT50/HIT100, reconstruct whether the head was actually within the hit window,
        // mirroring GameplaySlider.onSliderHeadHit.
        double accSeconds = data != null ? data.accuracy / 1000.0 : 0.0;
        boolean headHit;

        if (data == null || result == ResultType.HIT300.getId()) {
            headHit = true;
        } else {
            int replayVersion = GameHelper.getReplayVersion();
            double mehWindowSecs = hitWindow.getMehWindow() / 1000.0;
            double sliderDurationSecs = slider.getDuration() / 1000.0;
            double lateHitThreshold = replayVersion <= 7 ? Math.min(mehWindowSecs, sliderDurationSecs) : mehWindowSecs;

            if (replayVersion >= 6 || mehWindowSecs <= sliderDurationSecs) {
                headHit = -mehWindowSecs <= accSeconds && accSeconds <= lateHitThreshold;
            } else {
                headHit = accSeconds <= sliderDurationSecs;
            }
        }

        if (headHit) {
            reconstructHitOffset(accSeconds);
            stat.registerHit(30, false, false);
            stat.addSliderHeadHit();
        }

        // Ticks and repeats from tickSet.
        var nested = slider.getNestedHitObjects();

        // Skip head (index 0) and tail (last index).
        for (int i = 1, end = nested.size() - 1; i < end; i++) {
            boolean wasHit = data == null || (data.tickSet != null && data.tickSet.get(i - 1));

            if (!wasHit) {
                stat.registerHit(0, true, false);
                continue;
            }

            var nestedObj = nested.get(i);

            if (nestedObj instanceof com.rian.osu.beatmap.hitobject.sliderobject.SliderTick) {
                stat.registerHit(10, false, false);
                stat.addSliderTickHit();
            } else if (nestedObj instanceof com.rian.osu.beatmap.hitobject.sliderobject.SliderRepeat) {
                stat.registerHit(30, false, false);
                stat.addSliderRepeatHit();
            }
        }

        // Slider tail: combo is only awarded when the player was tracking at the endpoint.
        // Unlike circles, the result score reflects ticks hit, not timing accuracy.
        boolean tailTracked = data == null || (data.tickSet != null && data.tickSet.get(nested.size() - 2));
        byte tailResult = data != null ? data.result : ResultType.HIT300.getId();

        if (tailResult == ResultType.HIT50.getId()) {
            comboWas100 = true;
            stat.registerHit(50, false, false, tailTracked);
        } else if (tailResult == ResultType.HIT100.getId()) {
            comboWas100 = true;
            stat.registerHit(100, endCombo && !comboWasMissed, false, tailTracked);
        } else {
            if (endCombo && !comboWasMissed) {
                stat.registerHit(300, true, !comboWas100, tailTracked);
            } else {
                stat.registerHit(300, false, false, tailTracked);
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (tailTracked) {
            stat.addSliderEndHit();
        }
    }

    private void applySpinnerResult(Spinner spinner, @Nullable Replay.ReplayObjectData data, boolean endCombo) {
        float duration = (float) spinner.getDuration() / 1000;
        float needRotations = (2 + 2 * playableBeatmap.getDifficulty().od / 10f) * duration;

        if (duration < 0.05f) {
            needRotations = 0.1f;
        }

        int preClear;
        int bonus;

        if (data != null) {
            // data.accuracy = totalSpins * 4 + resultCode, where totalSpins = fullRotations (pre-clear, 100 pts each)
            // + (bonusScoreCounter - 1) (bonus, 1000 pts each). Split by needRotations to award the correct amounts.
            int totalSpins = (data.accuracy & 0xFFFF) >> 2;
            preClear = Math.min(totalSpins, (int) Math.ceil(needRotations) - 1);
            bonus = totalSpins - preClear;
        } else {
            // Autoplay always clears the spinner. Reconstruct the pre-clear (100 pts each) and bonus (1000 pts each)
            // rotation split from the spinner's parameters.
            // ceil(needRotations) - 1 rotations are pre-clear; bonus rotations begin at ceil(needRotations) total.
            float totalRotations = 5f * duration;

            preClear = (int) Math.ceil(needRotations) - 1;
            bonus = Math.max(0, (int) totalRotations - (int) Math.ceil(needRotations) + 1);
        }

        for (int s = 0; s < preClear; s++) {
            stat.registerSpinnerHit();
        }

        for (int s = 0; s < bonus; s++) {
            stat.registerHit(1000, false, false);
        }

        applyCircleResult(data, endCombo);
    }

    /**
     * Returns the effective drain duration in seconds for the segment [startMs, endMs], subtracting any time that
     * falls within a break period. {@code startBreakIdx} should be the first break period index whose end time is
     * >= startMs (caller advances this monotonically as segments progress forward in time).
     */
    private double calculateEffectiveDrainDuration(double startMs, double endMs, int startBreakIdx) {
        if (startMs >= endMs) {
            return 0;
        }

        double total = endMs - startMs;

        if (breakPeriods != null) {
            for (int i = startBreakIdx; i < breakPeriods.length; ++i) {
                var bp = breakPeriods[i];

                if (bp.startTime >= endMs) {
                    break;
                }

                double overlap = Math.min(bp.endTime, endMs) - Math.max(bp.startTime, startMs);

                if (overlap > 0) {
                    total -= overlap;
                }
            }
        }

        return Math.max(0, total) / 1000;
    }

    private void onExit() {
        Execution.updateThread(() -> {
            BeatmapSkinManager.setSkinEnabled(false);
            GameObjectPool.getInstance().purge();
            stopLoopingSamples();
            if (activeObjects != null) {
                activeObjects.clear();
            }
            if (expiredObjects != null) {
                expiredObjects.clear();
            }
            breakPeriods = null;
            objects = null;
            timingControlPoints = null;
            effectControlPoints = null;
            parsedBeatmap = null;
            playableBeatmap = null;
            if (cursorSprites != null) {
                for (var cs : cursorSprites) {
                    if (cs != null) cs.cleanupTrail();
                }
            }
            cursorSprites = null;
            // Detach + trail cleanup: otherwise the AutoCursor leaks its GL buffers and
            // stays referenced by the old scene until the next gameplay session.
            if (autoCursor != null) {
                autoCursor.cleanupTrail();
                autoCursor.detachSelf();
                autoCursor = null;
            }
            replay = null;
            replayPanel = null;
            lastMods = null;
            performanceCalculationParameters = null;
            droidTimedDifficultyAttributes = null;
            standardTimedDifficultyAttributes = null;
            sliderPaths = null;
            sliderRenderPaths = null;
        });

        cancelStoryboardLoading();
        cancelVideoLoading();

        float mSecPassed = elapsedTime * 1000;
        var selectedBeatmap = GlobalManager.getInstance().getSelectedBeatmap();
        var songService = GlobalManager.getInstance().getSongService();
        var songMenu = GlobalManager.getInstance().getSongMenu();

        if (songService != null && selectedBeatmap != null) {
            // osu!stable restarts the song back to preview time when the player is in the last 10 seconds *or* 2% of the beatmap.
            boolean continuePreview =
                mSecPassed < totalLength - 10000 &&
                mSecPassed / totalLength < 0.98f;
            int previewTime = continuePreview
                ? songService.getPosition()
                : selectedBeatmap.getPreviewTime();

            songMenu.playMusic(selectedBeatmap.getAudioPath(), previewTime);

            if (continuePreview) {
                songMenu.startMusicVolumeAnimation(0.3f);
            }
        }

        if (replaying) {
            replayFilePath = null;
        }
    }

    public void quit() {
        // Disable historical event processing for more efficient ACTION_MOVE reports, since frequent reports are
        // not that relevant outside gameplay.
        var touchOptions = new TouchOptions();
        touchOptions.setRunOnUpdateThread(true);
        touchOptions.setProcessHistoricalEvents(false);
        touchOptions.setUseRawPointer(false);

        var touchController = engine.getTouchController();
        touchController.applyTouchOptions(touchOptions);
        touchController.resetRawPointers();

        // Drop queued samples so they can't be consumed by the next session.
        {
            var directInputView = GlobalManager.getInstance().getMainActivity().getDirectInputSurface();
            if (directInputView != null) {
                directInputView.clearPointerSamples();
            }
        }

        engine.getEngineOptions().setWakeLockOptions(WakeLockOptions.SCREEN_ON);
        GlobalManager.getInstance()
            .getMainActivity()
            .runOnUiThread(() ->
                GlobalManager.getInstance().getMainActivity().reapplyWakeLock()
            );

        if (storyboardSprite != null) {
            storyboardSprite.detachSelf();
            storyboardOverlayProxy.detachSelf();
            storyboardSprite.releaseStoryboard();
            storyboardOverlayProxy.setDrawProxy(null);
            storyboardSprite = null;
        }

        if (video != null) {
            video.release();
            video = null;
            videoStarted = false;
        }

        if (sceneBorder != null) {
            sceneBorder.detachSelf();
            sceneBorder = null;
        }

        onExit();
        resetPlayfieldSizeScale();
        scene = createMainScene();

        if (Multiplayer.isMultiplayer) {
            Multiplayer.roomScene.show();
            return;
        }
        ResourceManager.getInstance().getSound("failsound").stop();
        engine.setScene(oldScene);

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties();
    }

    public void reset() {}

    //CB打击处理
    private String registerHit(
        final int objectId,
        final int score,
        final boolean endCombo
    ) {
        return registerHit(objectId, score, endCombo, true);
    }

    private String registerHit(
        final int objectId,
        final int score,
        final boolean endCombo,
        final boolean incrementCombo
    ) {
        if (isGameOver) {
            return "hit0";
        }

        boolean writeReplay = objectId != -1 && replay != null && !replaying;
        if (score == 0) {
            if (stat.getCombo() > 30) {
                var sound = ResourceManager.getInstance().getCustomSound(
                    "combobreak",
                    1
                );
                if (sound != null) {
                    sound.play();
                }
            }
            comboWasMissed = true;
            stat.registerHit(0, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.MISS);
            if (GameHelper.isPerfect()) {
                gameover();

                if (!Multiplayer.isMultiplayer) restartGame();
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            if (objectId != -1) {
                updatePPValue(objectId);
            }
            return "hit0";
        }

        String scoreName = "hit300";
        if (score == 50) {
            stat.registerHit(50, false, false, incrementCombo);
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT50);
            scoreName = "hit50";
            comboWas100 = true;
            if (GameHelper.isPerfect()) {
                gameover();

                if (!Multiplayer.isMultiplayer) restartGame();
            }
        } else if (score == 100) {
            comboWas100 = true;
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT100);
            if (endCombo && !comboWasMissed) {
                stat.registerHit(100, true, false, incrementCombo);
                scoreName = "hit100k";
            } else {
                stat.registerHit(100, false, false, incrementCombo);
                scoreName = "hit100";
            }
            if (GameHelper.isPerfect()) {
                gameover();
                if (!Multiplayer.isMultiplayer) restartGame();
            }
        } else if (score == 300) {
            if (writeReplay) replay.addObjectScore(objectId, ResultType.HIT300);
            if (endCombo && !comboWasMissed) {
                if (!comboWas100) {
                    stat.registerHit(300, true, true, incrementCombo);
                    scoreName = "hit300g";
                } else {
                    stat.registerHit(300, true, false, incrementCombo);
                    scoreName = "hit300k";
                }
            } else {
                stat.registerHit(300, false, false, incrementCombo);
                scoreName = "hit300";
            }
        }

        if (endCombo) {
            comboWas100 = false;
            comboWasMissed = false;
        }

        if (objectId != -1) {
            updatePPValue(objectId);
        }

        return scoreName;
    }

    public void onCircleHit(
        int id,
        final float acc,
        final PointF pos,
        final boolean endCombo,
        byte forcedScore,
        Color4 color
    ) {
        var playableBeatmap = this.playableBeatmap;

        if (playableBeatmap == null) {
            return;
        }

        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown(
                (float) parsedBeatmap
                    .getHitObjects()
                    .objects.get(id)
                    .startTime / 1000
            );
        }

        float accuracy = Math.abs(acc);

        // Screen shake on hit (gentle for accurate, stronger for shaky hits)
        if (screenShake != null && accuracy < 0.05f) {
            screenShake.shake(1.5f, 0.08f);
        } else if (screenShake != null) {
            screenShake.shake(0.5f, 0.05f);
        }

        boolean writeReplay = replay != null && !replaying;
        if (writeReplay) {
            short sacc = (short) (acc * 1000);
            replay.addObjectResult(id, sacc, null);
        }
        if (
            GameHelper.isFlashlight() &&
            !GameHelper.isAutoplay() &&
            !GameHelper.isAutopilot()
        ) {
            int nearestCursorId = getNearestCursorId(pos.x, pos.y);
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId;
                var latestNonUpEvent = cursors[mainCursorId].getLatestEvent(
                    TouchEvent.ACTION_DOWN,
                    TouchEvent.ACTION_MOVE
                );

                if (latestNonUpEvent != null) {
                    flashlightSprite.onMouseMove(
                        latestNonUpEvent.position.x,
                        latestNonUpEvent.position.y
                    );
                }
            }
        }
        VibratorManager.INSTANCE.circleVibration();



        if (
            accuracy > playableBeatmap.getHitWindow().getMehWindow() / 1000 ||
            forcedScore == ResultType.MISS.getId()
        ) {
            createHitEffect(pos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        String scoreName;
        if (
            forcedScore == ResultType.HIT300.getId() ||
            (forcedScore == 0 &&
                accuracy <=
                    playableBeatmap.getHitWindow().getGreatWindow() / 1000)
        ) {
            scoreName = registerHit(id, 300, endCombo);
        } else if (
            forcedScore == ResultType.HIT100.getId() ||
            (forcedScore == 0 &&
                accuracy <= playableBeatmap.getHitWindow().getOkWindow() / 1000)
        ) {
            scoreName = registerHit(id, 100, endCombo);
        } else {
            scoreName = registerHit(id, 50, endCombo);
        }

        createBurstEffect(pos, color);
        createHitEffect(pos, scoreName, color);

        hud.onNoteHit(stat);

        // Dispatch to Lua plugins (READ ONLY — score already calculated)
        com.osudroid.plugin.PluginManager.getInstance().dispatchCircleHit(
            id, accuracy, pos.x, pos.y, endCombo, scoreName.equals("hit0") ? 0 :
            scoreName.equals("hit50") ? 50 : scoreName.equals("hit100") ? 100 : 300
        );
    }

    public void onSliderReverse(PointF pos, float ang, Color4 color) {
        createBurstEffectSliderReverse(pos, ang, color);
    }

    public void onSliderHit(
        int id,
        final int score,
        final PointF judgementPos,
        final boolean endCombo,
        Color4 color,
        int type,
        boolean incrementCombo
    ) {
        if (
            GameHelper.isFlashlight() &&
            !GameHelper.isAutoplay() &&
            !GameHelper.isAutopilot()
        ) {
            int nearestCursorId = getNearestCursorId(
                judgementPos.x,
                judgementPos.y
            );
            if (nearestCursorId >= 0) {
                mainCursorId = nearestCursorId;
                var latestNonUpEvent = cursors[mainCursorId].getLatestEvent(
                    TouchEvent.ACTION_DOWN,
                    TouchEvent.ACTION_MOVE
                );

                if (latestNonUpEvent != null) {
                    flashlightSprite.onMouseMove(
                        latestNonUpEvent.position.x,
                        latestNonUpEvent.position.y
                    );
                }
            }
        }

        VibratorManager.INSTANCE.sliderVibration();

        // Whole slider was missed.
        if (score == 0) {
            createHitEffect(judgementPos, "hit0", color);
            registerHit(id, 0, endCombo);
            return;
        }

        // Nested object was missed.
        if (score == -1) {
            if (stat.getCombo() > 30) {
                var sound = ResourceManager.getInstance().getCustomSound(
                    "combobreak",
                    1
                );
                if (sound != null) {
                    sound.play();
                }
            }
            if (GameHelper.isSuddenDeath()) {
                stat.changeHp(-1.0f);
                gameover();
            }
            stat.registerHit(0, true, false);
            return;
        }

        String scoreName = "hit0";

        switch (type) {
            case GameObjectListener.SLIDER_START:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderHeadHit();
                    createBurstEffectSliderStart(judgementPos, color);
                    if (GameHelper.isAutoplay()) {
                        hud.onGameplayTouchDown(
                            (float) parsedBeatmap
                                .getHitObjects()
                                .objects.get(id)
                                .startTime / 1000
                        );
                    }
                }
                break;
            case GameObjectListener.SLIDER_REPEAT:
                if (incrementCombo) {
                    scoreName = "sliderpoint30";
                    stat.registerHit(30, false, false);
                    stat.addSliderRepeatHit();
                }
                break;
            case GameObjectListener.SLIDER_TICK:
                if (incrementCombo) {
                    scoreName = "sliderpoint10";
                    stat.registerHit(10, false, false);
                    stat.addSliderTickHit();
                }
                break;
            case GameObjectListener.SLIDER_END:
                // Slider end hit is tied to the final result of the slider.
                scoreName = registerHit(id, score, endCombo, incrementCombo);

                if (incrementCombo) {
                    stat.addSliderEndHit();
                    createBurstEffectSliderEnd(judgementPos, color);
                }
                break;
        }

        createHitEffect(judgementPos, scoreName, color);

        hud.onNoteHit(stat);

        // Dispatch to Lua plugins
        com.osudroid.plugin.PluginManager.getInstance().dispatchSliderHit(
            id, type, judgementPos.x, judgementPos.y, endCombo
        );
    }

    @Override
    public void onSpinnerStart(int id) {
        if (GameHelper.isAutoplay()) {
            autoCursor.click();
            hud.onGameplayTouchDown(
                (float) parsedBeatmap
                    .getHitObjects()
                    .objects.get(id)
                    .startTime / 1000
            );
        }
        com.osudroid.plugin.PluginManager.getInstance().dispatchSpinnerStart(id);
    }

    public void onSpinnerEnd(int id) {
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.onSliderEnd();
        }
        com.osudroid.plugin.PluginManager.getInstance().dispatchSpinnerEnd(id);
    }

    public void onSpinnerHit(
        int id,
        final int score,
        final boolean endCombo,
        int totalScore
    ) {
        if (score == 1000) {
            stat.registerHit(score, false, false);
            return;
        }

        if (replay != null && !replaying) {
            short acc = (short) (totalScore * 4);
            switch (score) {
                case 300:
                    acc += 3;
                    break;
                case 100:
                    acc += 2;
                    break;
                case 50:
                    acc += 1;
                    break;
            }
            replay.addObjectResult(id, acc, null);
        }

        final PointF pos = new PointF(
            (float) Config.getRES_WIDTH() / 2,
            (float) Config.getRES_HEIGHT() / 2
        );

        VibratorManager.INSTANCE.spinnerVibration();

        if (score == 0) {
            final GameEffect effect = GameObjectPool.getInstance().getEffect(
                "hit0"
            );
            effect.init(
                scene,
                pos,
                scale,
                Modifiers.sequence(
                    Modifiers.fadeIn(0.15f),
                    Modifiers.delay(0.35f),
                    Modifiers.fadeOut(0.25f)
                )
            );
            registerHit(id, 0, endCombo);
            com.osudroid.plugin.PluginManager.getInstance().dispatchSpinnerHit(id, 0);
            return;
        }

        String scoreName = switch (score) {
            case 300 -> registerHit(id, 300, endCombo);
            case 100 -> registerHit(id, 100, endCombo);
            case 50 -> registerHit(id, 50, endCombo);
            default -> "hit0";
        };

        createHitEffect(pos, scoreName, null);

        hud.onNoteHit(stat);

        // Dispatch to Lua plugins
        com.osudroid.plugin.PluginManager.getInstance().dispatchSpinnerHit(
            id, score == 300 ? 300 : score == 100 ? 100 : score == 50 ? 50 : 0
        );
    }

    @Override
    public void playHitSamples(List<GameplayHitSampleInfo> samples) {
        float volume = 1;
        var muted = GameHelper.getMuted();

        if (muted != null && muted.affectsHitSounds()) {
            volume = muted.volumeAt(stat.getCombo());
        }

        for (int i = 0, size = samples.size(); i < size; ++i) {
            var sample = samples.get(i);
            sample.setVolume(volume);
            sample.play();
        }
    }

    private void playLoopingSamples() {
        if (activeObjects == null) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).playLoopingSamples();
        }
    }

    private void stopLoopingSamples() {
        if (activeObjects == null) {
            return;
        }

        for (int i = 0, size = activeObjects.size(); i < size; i++) {
            activeObjects.get(i).stopLoopingSamples();
        }
    }

    public void addObject(final GameObject object) {
        activeObjects.add(object);
    }

    public void removeObject(final GameObject object) {
        expiredObjects.add(object);
    }

    @Override
    public boolean isObjectHittable(GameObject object) {
        // When notelock is disabled (lazer-style), allow hitting any active object
        if (Config.getBoolean("removeSliderLock", false)) {
            return true;
        }
        return object == judgeableObject;
    }

    @Override
    public Cursor getCursor(int index) {
        return cursors[index];
    }

    public boolean onSceneTouchEvent(
        final Scene pScene,
        final TouchEvent event
    ) {
        float offset =
            previousFrameTime > 0
                ? (event.getMotionEvent().getEventTime() - previousFrameTime) *
                  GameHelper.getSpeedMultiplier()
                : 0;
        int eventTime = (int) (elapsedTime * 1000 + offset);

        if (paused || isGameOver) {
            return false;
        }

        var id = event.getPointerID();
        if (id < 0 || id >= getCursorsCount()) {
            return false;
        }

        // During replay playback real touches must not control the game: the watched
        // cursor belongs to the recorded player. The only allowed interaction is the
        // skip button while it is visible; everything else is swallowed.
        if (replaying) {
            if (skipBtn != null && event.isActionDown()) {
                float touchX = event.getX();
                float touchY = event.getY();
                float dx = touchX - Config.getRES_WIDTH();
                float dy = touchY - Config.getRES_HEIGHT();
                if (dx * dx + dy * dy < 250f * 250f) {
                    skip();
                }
            }
            // Swallow everything (DOWN/MOVE/UP/CANCEL).
            return true;
        }

        // When raw pointers are active, the high-precision path in onManagedUpdate
        // handles DOWN and MOVE events at the game's update rate (up to 900 Hz).
        // We only need the normal path for UP events (which the raw pointer path
        // doesn't generate) and for the max-active-cursor limit check.
        var touchCtrl = engine.getTouchController();
        boolean rawPointersActive = touchCtrl != null &&
            touchCtrl.isUseRawPointers() &&
            !GameHelper.isAutoplay() && !GameHelper.isAutopilot();

        if (rawPointersActive && !event.isActionUp() && !event.isActionCancel() && !event.isActionOutside()) {
            // DOWN and MOVE are handled by the raw pointer path — skip duplicates.
            // But still enforce the max-active-cursor limit on DOWN.
            if (event.isActionDown()) {
                var cursor = cursors[id];
                if (cursor.mouseBlocked) {
                    cursor.mouseBlocked = false;
                    return true;
                }
                int activeCursorCount = 0;
                for (int i = 0; i < cursors.length; ++i) {
                    if (activeCursorCount >= maximumActiveCursorCount) break;
                    if (cursors[i].isMouseDown()) ++activeCursorCount;
                }
                if (activeCursorCount >= maximumActiveCursorCount) {
                    return false;
                }
                // Inform HUD of touch down timing (no cursor event — raw path handles it)
                if (!GameHelper.isAutoplay()) {
                    hud.onGameplayTouchDown(eventTime / 1000f);
                }
            }
            return true;
        }

        var cursor = cursors[id];

        if (event.isActionDown()) {
            if (cursor.mouseBlocked) {
                cursor.mouseBlocked = false;
                return true;
            }

            int activeCursorCount = 0;

            for (int i = 0; i < cursors.length; ++i) {
                if (activeCursorCount >= maximumActiveCursorCount) {
                    break;
                }

                if (cursors[i].isMouseDown()) {
                    ++activeCursorCount;
                }
            }

            if (activeCursorCount >= maximumActiveCursorCount) {
                return false;
            }
        }

        var sprite =
            !GameHelper.isAutoplay() &&
            !GameHelper.isAutopilot() &&
            !replaying &&
            cursorSprites != null
                ? cursorSprites[id]
                : null;

        var cursorEvent = CursorEvent.obtain(event);

        cursorEvent.trackTime = elapsedTime * 1000;
        cursorEvent.offset = offset;
        cursorEvent.isRealInput = true;

        if (sprite != null) {
            sprite.setPosition(cursorEvent.position.x, cursorEvent.position.y);
        }

        if (event.isActionDown()) {
            if (sprite != null) {
                sprite.setShowing(true);
                // Fresh press: restart the trail from the new position (no interpolation
                // across the lift→press gap, regardless of distance).
                sprite.onCursorPress();
            }

            if (!GameHelper.isAutoplay()) {
                hud.onGameplayTouchDown(eventTime / 1000f);
            }

            cursor.addEvent(cursorEvent);

            // Replay recording is centralized in recordReplayMovements().

            com.osudroid.plugin.GameState.setKeyState(id == 0 ? "m1" : "m2", true);
        } else if (event.isActionMove()) {
            if (sprite != null) {
                sprite.setShowing(true);
            }

            cursor.addEvent(cursorEvent);
        } else if (event.isActionUp()) {
            com.osudroid.plugin.GameState.setKeyState(id == 0 ? "m1" : "m2", false);

            if (sprite != null) {
                sprite.setShowing(false);
            }

            cursor.addEvent(cursorEvent);
        } else if (event.isActionCancel() || event.isActionOutside()) {
            removeAllCursors();
        } else {
            return false;
        }
        return true;
    }

    /**
     * Records this tick's cursor events into the replay. Runs over the same events that drive
     * gameplay, so the replay matches what happened on screen regardless of the input path.
     * MOVE events keep the ≥1 osu!px / ≥33ms density rule (see the fields above).
     */
    private void recordReplayMovements() {
        var currentReplay = this.replay;

        if (currentReplay == null || replaying || isGameOver) {
            return;
        }

        for (int i = 0; i < cursors.length && i < currentReplay.cursorMoves.size(); ++i) {
            var cursor = cursors[i];
            var events = cursor.events;
            int size = events.size();

            for (int j = 0; j < size; ++j) {
                var ev = events.get(j);

                // Drop (0,0) press/move artifacts of secondary pointers (multi-touch
                // coordinate glitch); UP carries no position and is unaffected.
                if (!ev.isActionUp() && ev.position.x == 0f && ev.position.y == 0f) {
                    continue;
                }

                // Safety net: some producers fill `position` but leave `trackPosition`
                // at its pool default (0,0). Never record blind zeros — derive the
                // track-space coordinates from the screen-space position instead.
                if (!ev.isActionUp() &&
                    ev.trackPosition.x == 0f && ev.trackPosition.y == 0f &&
                    !(ev.position.x == 0f && ev.position.y == 0f)) {
                    PointF converted = Utils.realToTrackCoords(
                        new PointF(ev.position.x, ev.position.y)
                    );
                    ev.trackPosition.set(converted);
                }

                int eventTime = (int) (ev.trackTime + ev.offset);

                if (ev.isActionDown()) {
                    if (replayRecWasDown[i]) {
                        // Duplicate DOWN inside an already-recorded press (input glitch or
                        // state desync). Never write a second DOWN: paired with the single UP
                        // it would corrupt playback (cursor re-pressed without release).
                        // Keep the position as a MOVE so taps without drag still record it.
                        recordReplayMove(currentReplay, i, eventTime, ev.trackPosition);
                        continue;
                    }

                    currentReplay.addPress(eventTime, ev.trackPosition, i);
                    replayRecWasDown[i] = true;
                    replayRecLastX[i] = ev.trackPosition.x;
                    replayRecLastY[i] = ev.trackPosition.y;
                    replayRecLastTime[i] = eventTime;
                } else if (ev.isActionMove()) {
                    recordReplayMove(currentReplay, i, eventTime, ev.trackPosition);
                } else if (ev.isActionUp() && replayRecWasDown[i]) {
                    // An UP that was already recorded directly by removeAllCursors()
                    // has its pressed flag cleared there, so no duplicate is written.
                    currentReplay.addUp(eventTime, i);
                    replayRecWasDown[i] = false;
                    replayRecLastTime[i] = eventTime;
                }
            }
        }
    }

    /**
     * Writes one MOVE event into the replay honoring the density rule:
     * ≥1 osu!px of movement since the last recorded point, or ≥33ms since it
     * (slow drags must not collapse into the interpolation base point).
     * Shared by the DOWN→MOVE demotion path and the regular MOVE path.
     */
    private void recordReplayMove(Replay currentReplay, int pointer, int eventTime, PointF trackPos) {
        float moveDx = trackPos.x - replayRecLastX[pointer];
        float moveDy = trackPos.y - replayRecLastY[pointer];

        if (moveDx * moveDx + moveDy * moveDy >= 1f ||
            eventTime - replayRecLastTime[pointer] >= 33) {
            currentReplay.addMove(eventTime, trackPos, pointer);
            replayRecLastX[pointer] = trackPos.x;
            replayRecLastY[pointer] = trackPos.y;
            replayRecLastTime[pointer] = eventTime;
        }
    }

    private void removeAllCursors() {
        long currentTime = SystemClock.uptimeMillis();
        float offset =
            previousFrameTime > 0
                ? (currentTime - previousFrameTime) *
                  GameHelper.getSpeedMultiplier()
                : 0;
        float time = elapsedTime * 1000 + offset;

        for (int i = 0; i < cursors.length; ++i) {
            var cursor = cursors[i];

            if (cursor.isMouseDown()) {
                var upEvent = CursorEvent.obtain();

                upEvent.systemTime = currentTime;
                upEvent.trackTime = time;
                upEvent.action = TouchEvent.ACTION_UP;
                upEvent.isRealInput = true;

                cursor.addEvent(upEvent);

                if (replay != null) {
                    // Recorded directly (a pause/cancel can end the tick before the
                    // centralized recorder runs); clear the recorder's pressed flag so
                    // the same UP event isn't written into the replay twice.
                    replay.addUp((int) time, i);
                    replayRecWasDown[i] = false;
                }
            }

            if (cursorSprites != null) {
                cursorSprites[i].setShowing(false);
            }
        }
    }

    public void pause() {
        if (paused) {
            return;
        }

        if (isHUDEditorMode) {
            hud.onBackPress();
            return;
        }

        if (Multiplayer.isMultiplayer) {
            // Setting a delay of 300ms minimum for the player to tap back button again.
            if (
                lastBackPressTime > 0 &&
                realTimeElapsed - lastBackPressTime >
                    Math.max(300, Config.getBackButtonPressTime() * 1.5f)
            ) {
                // Room being null can happen when the player disconnects from socket while playing
                if (Multiplayer.isConnected()) Execution.async(() ->
                    Execution.runSafe(() ->
                        RoomAPI.submitFinalScore(stat.toJson())
                    )
                );

                Multiplayer.log("Player left the match.");
                quit();
                return;
            }

            lastBackPressTime = realTimeElapsed;
            ToastLogger.showText("Tap twice to exit to room.", false);
            return;
        }

        if (isGameOver) {
            // Finishing the game over animation now.
            GlobalManager.getInstance()
                .getSongService()
                .setFrequencyForcefully(101);
            return;
        }

        if (video != null && videoStarted) {
            video.pause();
        }

        Execution.updateThread(this::stopLoopingSamples);

        if (
            !GameHelper.isAutoplay() && !GameHelper.isAutopilot() && !replaying
        ) {
            removeAllCursors();
        }

        if (
            GlobalManager.getInstance().getSongService() != null &&
            GlobalManager.getInstance().getSongService().getStatus() ==
                Status.PLAYING
        ) {
            GlobalManager.getInstance().getSongService().pause();
        }
        paused = true;
        com.osudroid.plugin.GameState.setPaused(true);
        com.osudroid.plugin.PluginPauseHandler.dispatchPause();
        scene.setIgnoreUpdate(true);

        final PauseMenu menu = new PauseMenu(engine, this, false);
        UIEngine.getCurrent()
            .getOverlay()
            .setChildScene(menu.getScene(), false, true, true);
    }

    public void gameover() {
        if (isGameOver) {
            return;
        }
        isGameOver = true;

        if (!replaying) {
            removeAllCursors();
        }

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected()) {
                Multiplayer.log("Player has lost, moving to room scene.");
                Execution.async(() ->
                    Execution.runSafe(() ->
                        RoomAPI.submitFinalScore(stat.toJson())
                    )
                );
            }
            quit();
            return;
        }

        stopLoopingSamples();
        SongService songService = GlobalManager.getInstance().getSongService();

        if (GameHelper.isPerfect()) {
            if (video != null) {
                video.pause();
            }
            songService.pause();
            paused = true;
            scene.setIgnoreUpdate(true);
            return;
        }

        ResourceManager.getInstance().getSound("failsound").play();
        gameStarted = false;

        float initialFrequency = songService.getFrequency();

        // Locally saving the scenes references to avoid unexpected behavior when the scene is changed.
        UIScene scene = this.scene;
        UIScene mgScene = this.mgScene;
        UIScene bgScene = this.bgScene;

        // Wind down animation for failing based on osu!stable behavior.
        engine.registerUpdateHandler(
            new IUpdateHandler() {
                private float elapsedTime;

                private void applyEffectToScene(Scene scene) {
                    if (scene.getAlpha() > 0) {
                        scene.setAlpha(Math.max(0, scene.getAlpha() - 0.007f));
                    }

                    for (int i = 0; i < scene.getChildCount(); i++) {
                        IEntity entity = scene.getChild(i);

                        entity.setPosition(
                            entity.getX(),
                            entity.getY() < 0f
                                ? entity.getY() * 0.6f
                                : entity.getY() * 1.01f
                        );

                        if (entity.getRotation() == 0) {
                            entity.setRotation(
                                entity.getRotation() +
                                    ((float) Random.Default.nextDouble(
                                        -0.02,
                                        0.02
                                    ) *
                                        180) /
                                        FMath.Pi
                            );
                        } else if (entity.getRotation() > 0) {
                            entity.setRotation(
                                entity.getRotation() + (0.01f * 180) / FMath.Pi
                            );
                        } else {
                            entity.setRotation(
                                entity.getRotation() - (0.01f * 180) / FMath.Pi
                            );
                        }
                    }
                }

                @Override
                public void onUpdate(float pSecondsElapsed) {
                    // Ensure this update handler is removed under unexpected circumstances.
                    if (engine.getScene() != scene) {
                        engine.unregisterUpdateHandler(this);
                        return;
                    }

                    elapsedTime += pSecondsElapsed;

                    // In osu!stable, the update is capped to 60 FPS. This means in higher framerates, the animations
                    // need to be slowed down to match 60 FPS.
                    float sixtyFPS = 1 / 60f;

                    if (elapsedTime < sixtyFPS) {
                        return;
                    }

                    elapsedTime -= sixtyFPS;

                    if (songService.getFrequency() > 101) {
                        applyEffectToScene(mgScene);
                        applyEffectToScene(bgScene);

                        float decreasedFrequency = Math.max(
                            101,
                            songService.getFrequency() - 300
                        );
                        float decreasedSpeed =
                            GameHelper.getSpeedMultiplier() *
                            (1 -
                                (initialFrequency - decreasedFrequency) /
                                    initialFrequency);

                        if (videoEnabled && video != null) {
                            // In some devices this can throw an exception, unfortunately there's no
                            // documentation that explains how to avoid that scenario. Thanks Google.
                            try {
                                video.setPlaybackSpeed(decreasedSpeed);
                            } catch (Exception e) {
                                Log.e(
                                    "GameScene",
                                    "Failed to change video playback speed during game over animation.",
                                    e
                                );
                            }
                        }

                        songService.setFrequencyForcefully(decreasedFrequency);
                    } else {
                        if (videoEnabled && video != null) {
                            video.pause();
                        }

                        // Ensure music frequency is reset back to what it was.
                        songService.setFrequencyForcefully(initialFrequency);

                        if (songService.getStatus() == Status.PLAYING) {
                            songService.pause();
                        }

                        paused = true;

                        scene.setIgnoreUpdate(true);
                        engine.unregisterUpdateHandler(this);

                        PauseMenu menu = new PauseMenu(
                            engine,
                            GameScene.this,
                            true
                        );
                        UIEngine.getCurrent()
                            .getOverlay()
                            .setChildScene(menu.getScene(), false, true, true);
                    }
                }

                @Override
                public void reset() {}
            }
        );
    }

    public void resume() {
        if (!paused) {
            return;
        }

        scene.setIgnoreUpdate(false);
        UIEngine.getCurrent().getOverlay().getChildScene().back();
        paused = false;
        com.osudroid.plugin.GameState.setPaused(false);
        com.osudroid.plugin.PluginPauseHandler.dispatchResume();

        if (
            stat.getHp() <= 0 &&
            !stat.getMod().contains(ModNoFail.class) &&
            !stat.getMod().contains(ModRelax.class) &&
            !stat.getMod().contains(ModAutopilot.class)
        ) {
            quit();
            return;
        }

        if (video != null && videoStarted) {
            video.play();
        }

        // Match upstream: un-pausing the pause menu must not resume playback that the user
        // explicitly paused through the replay panel.
        if (
            GlobalManager.getInstance().getSongService() != null &&
            GlobalManager.getInstance().getSongService().getStatus() !=
                Status.PLAYING &&
            elapsedTime > 0 &&
            !isReplayPlaybackPaused()
        ) {
            GlobalManager.getInstance().getSongService().play();
            GlobalManager.getInstance()
                .getSongService()
                .setVolume(Config.getBgmVolume());
            totalLength = GlobalManager.getInstance()
                .getSongService()
                .getLength();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    private void createHitEffect(
        final PointF pos,
        final String name,
        Color4 color
    ) {
        var effect = GameObjectPool.getInstance().getEffect(name);
        var isAnimated =
            effect.hit instanceof UIAnimatedSprite animatedHit &&
            animatedHit.getFrames().length > 1;

        // Reference https://github.com/ppy/osu/blob/ebf637bd3c33f1c886f6bfc81aa9ea2132c9e0d2/osu.Game/Skinning/LegacyJudgementPieceOld.cs

        var fadeInLength = 0.12f;
        var fadeOutLength = 0.6f;
        var fadeOutDelay = 0.5f;

        var fadeSequence = Modifiers.sequence(
            Modifiers.fadeIn(fadeInLength),
            Modifiers.delay(fadeOutDelay),
            Modifiers.fadeOut(fadeOutLength)
        );

        if (name.equals("hit0")) {
            var rotation = (float) Random.Default.nextDouble(8.6 * 2) - 8.6f;

            if (isAnimated) {
                // Legacy judgements don't play any transforms if they are an animation.
                effect.init(mgScene, pos, scale, fadeSequence);
            } else {
                effect.init(
                    mgScene,
                    pos,
                    scale * 1.6f,
                    fadeSequence,
                    Modifiers.scale(
                        0.1f,
                        scale * 1.6f,
                        scale,
                        null,
                        Easing.InQuad
                    ),
                    Modifiers.translateY(
                        fadeOutDelay + fadeOutLength,
                        -5f,
                        80f,
                        null,
                        Easing.InQuad
                    ),
                    Modifiers.sequence(
                        Modifiers.rotation(fadeInLength, 0, rotation),
                        Modifiers.rotation(
                            fadeOutDelay + fadeOutLength - fadeInLength,
                            rotation,
                            rotation * 2,
                            null,
                            Easing.InQuad
                        )
                    )
                );
            }

            return;
        }

        if (
            Config.isHitLighting() &&
            !name.equals("sliderpoint10") &&
            !name.equals("sliderpoint30") &&
            ResourceManager.getInstance().getTexture("lighting") != null
        ) {
            // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88

            var light = GameObjectPool.getInstance().getEffect("lighting");
            light.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA);
            light.setColor(color);
            light.init(
                bgScene,
                pos,
                scale * 0.8f,
                Modifiers.scale(
                    0.6f,
                    scale * 0.8f,
                    scale * 1.2f,
                    null,
                    Easing.OutQuad
                ),
                Modifiers.sequence(
                    Modifiers.fadeIn(0.2f),
                    Modifiers.delay(0.2f),
                    Modifiers.fadeOut(1f)
                )
            );
        }

        // Legacy judgements don't play any transforms if they are an animation.
        if (isAnimated) {
            effect.init(mgScene, pos, scale, fadeSequence);
        } else {
            effect.init(
                mgScene,
                pos,
                scale * 0.6f,
                fadeSequence,
                Modifiers.sequence(
                    Modifiers.scale(
                        fadeInLength * 0.8f,
                        scale * 0.6f,
                        scale * 1.1f
                    ),
                    Modifiers.delay(fadeInLength * 0.2f),
                    Modifiers.scale(
                        fadeInLength * 0.2f,
                        scale * 1.1f,
                        scale * 0.9f
                    ),

                    // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                    // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                    // second half of the transform.
                    Modifiers.scale(fadeInLength * 0.2f, scale * 0.95f, scale)
                )
            );
        }
    }

    private void applyBurstEffect(GameEffect effect, PointF pos) {
        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        var fadeDuration = 0.24f;

        effect.init(
            mgScene,
            pos,
            scale,
            Modifiers.scale(
                fadeDuration,
                scale,
                scale * 1.4f,
                null,
                Easing.OutQuad
            ),
            Modifiers.fadeOut(fadeDuration)
        );
    }

    private void createBurstEffect(final PointF pos, final Color4 color) {
        if (
            !Config.isBurstEffects() ||
            (GameHelper.getHidden() != null &&
                !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
            GameHelper.isTraceable()
        ) return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect(
            "hitcircle"
        );
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect(
            "hitcircleoverlay"
        );
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderStart(
        final PointF pos,
        final Color4 color
    ) {
        if (
            !Config.isBurstEffects() ||
            (GameHelper.getHidden() != null &&
                !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
            GameHelper.isTraceable()
        ) return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect(
            "sliderstartcircle"
        );
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect(
            "sliderstartcircleoverlay"
        );
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderEnd(
        final PointF pos,
        final Color4 color
    ) {
        if (
            !Config.isBurstEffects() ||
            (GameHelper.getHidden() != null &&
                !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
            GameHelper.isTraceable()
        ) return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect(
            "sliderendcircle"
        );
        applyBurstEffect(burst1, pos);
        burst1.setColor(color);

        final GameEffect burst2 = GameObjectPool.getInstance().getEffect(
            "sliderendcircleoverlay"
        );
        applyBurstEffect(burst2, pos);
    }

    private void createBurstEffectSliderReverse(
        final PointF pos,
        float ang,
        final Color4 color
    ) {
        if (
            !Config.isBurstEffects() ||
            (GameHelper.getHidden() != null &&
                !GameHelper.getHidden().isOnlyFadeApproachCircles()) ||
            GameHelper.isTraceable()
        ) return;

        final GameEffect burst1 = GameObjectPool.getInstance().getEffect(
            "reversearrow"
        );
        burst1.hit.setRotation(ang);
        applyBurstEffect(burst1, pos);
    }

    public int getCursorsCount() {
        return cursors.length;
    }

    public void registerAccuracy(final double acc) {
        offsetSum += (float) acc;
        offsetRegs++;

        stat.addHitOffset(acc);

        if (replaying) {
            scoringScene.getReplayStat().addHitOffset(acc);
        }

        hud.onAccuracyRegister((float) acc);
    }

    public void onSliderEnd(int id, int accuracy, BitSet tickSet) {
        onTrackingSliders(false);
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.onSliderEnd();
        }
        if (replay != null && !replaying) {
            short acc = (short) accuracy;
            replay.addObjectResult(id, acc, (BitSet) tickSet.clone());
        }
        com.osudroid.plugin.PluginManager.getInstance().dispatchSliderEnd(id, accuracy / 100f);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        com.osudroid.plugin.GameState.setIsSliderTracking(isTrackingSliders);
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.onSliderTracking();
        }
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onTrackingSliders(isTrackingSliders);
        }
    }

    public void onUpdatedAutoCursor(float pX, float pY) {
        if (GameHelper.isFlashlight()) {
            flashlightSprite.onMouseMove(pX, pY);
        }
    }

    public void updateAutoBasedPos(float pX, float pY) {
        if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
            autoCursor.followSlider(pX, pY);
        }
    }

    private int getNearestCursorId(float pX, float pY) {
        float nearestDistance = Float.POSITIVE_INFINITY;
        int id = -1;

        for (int i = 0; i < cursors.length; ++i) {
            var latestEvent = cursors[i].getLatestEvent(
                TouchEvent.ACTION_DOWN,
                TouchEvent.ACTION_MOVE
            );

            if (latestEvent != null) {
                float distance = Utils.squaredDistance(
                    pX,
                    pY,
                    latestEvent.position.x,
                    latestEvent.position.y
                );

                if (distance < nearestDistance) {
                    id = i;
                    nearestDistance = distance;
                }
            }
        }

        return id;
    }

    private void calculateAllSliderPaths(@Nullable final CoroutineScope scope) {
        if (scope != null) {
            ensureActive(scope.getCoroutineContext());
        }

        var playableBeatmap = this.playableBeatmap;

        if (
            playableBeatmap == null ||
            playableBeatmap.getHitObjects().getSliderCount() == 0
        ) {
            return;
        }

        var sliderPaths = new SliderPath[playableBeatmap
            .getHitObjects()
            .getSliderCount()];
        var sliderRenderPaths = new LinePath[playableBeatmap
            .getHitObjects()
            .getSliderCount()];
        int index = 0;

        for (var obj : playableBeatmap.getHitObjects().objects) {
            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            if (!(obj instanceof Slider slider)) {
                continue;
            }

            sliderPaths[index] = GameHelper.convertSliderPath(slider, scope);

            if (scope != null) {
                ensureActive(scope.getCoroutineContext());
            }

            sliderRenderPaths[index] = GameHelper.convertSliderPath(
                sliderPaths[index],
                scope
            );
            ++index;
        }

        this.sliderPaths = sliderPaths;
        this.sliderRenderPaths = sliderRenderPaths;
    }

    private SliderPath getSliderPath(int index) {
        if (sliderPaths != null && index < sliderPaths.length && index >= 0) {
            return sliderPaths[index];
        } else {
            return null;
        }
    }

    private LinePath getSliderRenderPath(int index) {
        if (
            sliderRenderPaths != null &&
            index < sliderRenderPaths.length &&
            index >= 0
        ) {
            return sliderRenderPaths[index];
        } else {
            return null;
        }
    }

    public boolean getReplaying() {
        return replaying;
    }

    public float getReplayDurationSec() {
        if (totalLength < Integer.MAX_VALUE) {
            return totalLength / 1000f;
        }
        // Fallback: use the last hit object's time
        if (objects != null && objects.length > 0) {
            return (float) objects[objects.length - 1].startTime / 1000f + 3f;
        }
        return 120f; // Default 2 minutes
    }

    public @Nullable DroidPlayableBeatmap getPlayableBeatmap() {
        return playableBeatmap;
    }

    public boolean saveFailedReplay() {
        stat.setTime(System.currentTimeMillis());
        if (replay != null && !replaying) {
            //write misses to replay
            for (GameObject obj : activeObjects) {
                stat.registerHit(0, false, false);
                replay.addObjectScore(obj.getId(), ResultType.MISS);
            }
            while (objectIndex < objects.length) {
                ++objectIndex;
                stat.registerHit(0, false, false);
                replay.addObjectScore(++lastObjectId, ResultType.MISS);
            }

            var currentTime = String.valueOf(System.currentTimeMillis());
            var odrFilename =
                MD5Calculator.getStringMD5(
                    lastBeatmapInfo.getFilename() + currentTime
                ) +
                currentTime.substring(0, Math.min(3, currentTime.length())) +
                ".odr";

            replayFilePath = Config.getScorePath() + odrFilename;
            replay.setStat(stat);
            replay.save(replayFilePath);

            if (
                stat.getTotalScoreWithMultiplier() > 0 &&
                !stat.getMod().contains(ModAutoplay.class)
            ) {
                stat.setReplayFilename(odrFilename);
                stat.setBeatmapMD5(lastBeatmapInfo.getMD5());

                try {
                    DatabaseManager.getScoreInfoTable().insertScore(
                        stat.toScoreInfo()
                    );
                } catch (Exception e) {
                    Log.e("GameScene", "Failed to save score to database", e);
                }
            }

            ToastLogger.showText(
                StringTable.get(
                    com.osudroid.resources.R.string.message_save_replay_successful
                ),
                true
            );
            replayFilePath = null;
            return true;
        } else {
            ToastLogger.showText(
                StringTable.get(
                    com.osudroid.resources.R.string.message_save_replay_failed
                ),
                true
            );
            return false;
        }
    }

    private void updatePPValue(int objectId) {
        if (
            Config.isHideInGameUI() ||
            (!isHUDEditorMode &&
                !OsuSkin.get().getHUDSkinData().hasElement(HUDPPCounter.class))
        ) {
            return;
        }

        double pp = switch (Config.getDifficultyAlgorithm()) {
            case droid, drpp, rxpp -> getDroidPPAt(objectId);
            case standard -> getStandardPPAt(objectId);
        };

        stat.setPP(pp);
    }

    private double getDroidPPAt(int objectId) {
        var playableBeatmap = this.playableBeatmap;

        if (
            playableBeatmap == null ||
            droidTimedDifficultyAttributes == null ||
            performanceCalculationParameters == null ||
            objectId < 0 ||
            objectId >= droidTimedDifficultyAttributes.length
        ) {
            return 0;
        }

        var timedAttributes = droidTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(playableBeatmap, stat);

        return BeatmapDifficultyCalculator.calculateDroidPerformance(
            timedAttributes.attributes,
            (DroidPerformanceCalculationParameters) performanceCalculationParameters
        ).total;
    }

    private double getStandardPPAt(int objectId) {
        var playableBeatmap = this.playableBeatmap;

        if (
            playableBeatmap == null ||
            standardTimedDifficultyAttributes == null ||
            performanceCalculationParameters == null ||
            objectId < 0 ||
            objectId >= standardTimedDifficultyAttributes.length
        ) {
            return 0;
        }

        var timedAttributes = standardTimedDifficultyAttributes[objectId];

        performanceCalculationParameters.populate(playableBeatmap, stat);

        return BeatmapDifficultyCalculator.calculateStandardPerformance(
            timedAttributes.attributes,
            (StandardPerformanceCalculationParameters) performanceCalculationParameters
        ).total;
    }

    private UIScene createMainScene() {
        return new UIScene() {
            // Reused buffer to avoid allocations.
            private final float[] fastPathSurfaceCoords = new float[2];

            // Reused buffer for coordinate conversion (avoids new float[] per tick).
            private final float[] tmpSurfaceCoords = new float[2];

            // Stable fallback cache per pointer (surface space).
            private final boolean[] fastPathHasStableSnapshot =
                new boolean[CursorCount];
            private final float[] fastPathLastStableX = new float[CursorCount];
            private final float[] fastPathLastStableY = new float[CursorCount];

            // Deduplication: last event timestamp per pointer.
            // Raw pointer data only changes at the OS touch rate (~120 Hz).
            // We only create a CursorEvent when the MotionEvent timestamp changes,
            // avoiding ~7 redundant events per 120 Hz cycle at 900 Hz game rate.
            // The position guard matters on 240Hz+ digitizers: several real samples
            // can share the same uptimeMillis millisecond and would be lost by a
            // time-only dedup (visible as input steps during fast flicks).
            private final long[] rawLastEventTime = new long[CursorCount];
            private final float[] rawLastSampleX = new float[CursorCount];
            private final float[] rawLastSampleY = new float[CursorCount];

            // Reset deduplication state each tick.
            private boolean isInterpolating;

            // Scratch buffers for the raw-pointer sample drain, allocated once and
            // reused every tick to avoid GC pressure on the update thread.
            // sampleCoords: [0]=x [1]=y [2]=unused [3]=down(1/0)
            // rawSampleTime holds the sample timestamp; it must be a long, a float
            // loses all sub-~32ms precision for uptimeMillis values.
            private final float[] sampleCoords = new float[4];
            private final long[] rawSampleTime = new long[1];
            private final int[] rawSampleAction = new int[1];
            private float[] sceneCoords = new float[2];

            @Override
            protected void onManagedDraw(GL10 pGL, Camera pCamera) {
                if (!isGameOver) {
                    applyRawPointerFastPath(pCamera);
                }

                // Render gameplay FIRST, THEN trigger export
                // so glReadPixels captures the fully rendered frame.
                super.onManagedDraw(pGL, pCamera);
            }

            @Override
            protected void onManagedUpdate(float secElapsed) {
                // ── High-precision input: feed raw pointer data as cursor events ──
                // This gives game objects the LATEST touch position for hit detection,
                // bypassing the 1-frame queue latency of the normal touch event pipeline.
                if (
                    !isGameOver && !paused &&
                    engine.getTouchController() != null &&
                    engine.getTouchController().isUseRawPointers() &&
                    !replaying &&
                    !GameHelper.isAutoplay() &&
                    !GameHelper.isAutopilot()
                ) {
                    var touchController = engine.getTouchController();
                    var gameCamera = engine.getCamera();
                    var cap = touchController.getRawPointerCapacity();
                    var directInputView = GlobalManager.getInstance().getMainActivity().getDirectInputSurface();

                    for (int pi = 0; pi < Math.min(cap, cursors.length); pi++) {
                        var cursor = cursors[pi];
                        if (cursor == null) continue;

                        // Process samples even for pointers the controller already considers
                        // up: the final UP sample (down=0) arrives in the same batch that
                        // cleared the raw-down flag, and the UP CursorEvent must still be
                        // generated (otherwise the cursor stays in a pressed state forever).
                        boolean pointerMarkedDown = touchController.isRawPointerDown(pi);
                        boolean hasQueuedSamples =
                            directInputView != null && directInputView.getQueuedSampleCount(pi) > 0;

                        if (!pointerMarkedDown && !hasQueuedSamples) continue;

                        // ── Drain ALL queued samples in chronological order. ──
                        // Every historical+current sample of each batched MotionEvent is
                        // queued, so consuming the complete path preserves intermediate
                        // positions during fast flicks (hit detection follows the finger
                        // exactly and sliders don't "skip").
                        while (directInputView != null && directInputView.popPointerSample(pi, sampleCoords, rawSampleTime, rawSampleAction)) {
                            float sx = sampleCoords[0];
                            float sy = sampleCoords[1];
                            long sampleTime = rawSampleTime[0];
                            boolean sampleDown = sampleCoords[3] > 0f;

                            // Classify strictly from the action recorded at queue time:
                            // inferring it from cursor state would resurrect a lifted finger
                            // whenever a MOVE batch still listed the pointer (phantom DOWNs
                            // while streaming with a second finger).

                            // Dedup: identical timestamp AND position to the last
                            // consumed sample — a true duplicate, skip. Same-ms samples
                            // with movement are KEPT (high-Hz digitizers).
                            if (sampleTime == rawLastEventTime[pi]
                                && sx == rawLastSampleX[pi]
                                && sy == rawLastSampleY[pi]) {
                                continue;
                            }
                            rawLastEventTime[pi] = sampleTime;
                            rawLastSampleX[pi] = sx;
                            rawLastSampleY[pi] = sy;

                            // Some devices back-fill the historical track of a just-added
                            // pointer with (0,0); a real finger can never be at the exact
                            // top-left surface corner. UP carries no position — unaffected.
                            if (sampleDown && sx == 0f && sy == 0f) {
                                continue;
                            }

                            // Compute sub-frame offset from the sample timestamp vs the last
                            // game frame boundary, same as onSceneTouchEvent.
                            double frameOffset =
                                previousFrameTime > 0
                                    ? (sampleTime - previousFrameTime) *
                                      GameHelper.getSpeedMultiplier()
                                    : 0;

                            // Create a cursor event from the queued sample; the UP sample is
                            // queued like any other so the lift is consumed in-order.
                            var ev = CursorEvent.obtain();
                            ev.systemTime = sampleTime;
                            ev.trackTime = elapsedTime * 1000;
                            ev.action = rawSampleAction[0] == android.view.MotionEvent.ACTION_DOWN
                                ? TouchEvent.ACTION_DOWN
                                : rawSampleAction[0] == android.view.MotionEvent.ACTION_UP
                                    ? TouchEvent.ACTION_UP
                                    : TouchEvent.ACTION_MOVE;
                            ev.offset = frameOffset;
                            ev.isRealInput = true;

                            // Convert surface coords to scene coords
                            tmpSurfaceCoords[0] = sx;
                            tmpSurfaceCoords[1] = sy;
                            sceneCoords = Cameras.convertSurfaceToSceneCoordinates(
                                gameCamera,
                                tmpSurfaceCoords
                            );
                            ev.position.x = Math.max(
                                0,
                                Math.min(sceneCoords[0], Config.getRES_WIDTH())
                            );
                            ev.position.y = Math.max(
                                0,
                                Math.min(sceneCoords[1], Config.getRES_HEIGHT())
                            );
                            ev.trackPosition.x = sceneCoords[0];
                            ev.trackPosition.y = sceneCoords[1];
                            if (GameHelper.isHardRock()) {
                                ev.trackPosition.y -=
                                    Config.getRES_HEIGHT() / 2f;
                                ev.trackPosition.y *= -1;
                                ev.trackPosition.y +=
                                    Config.getRES_HEIGHT() / 2f;
                            }

                            ev.trackPosition.x -=
                                (Config.getRES_WIDTH() -
                                    Constants.MAP_ACTUAL_WIDTH) /
                                2f;
                            ev.trackPosition.y -=
                                (Config.getRES_HEIGHT() -
                                    Constants.MAP_ACTUAL_HEIGHT) /
                                2f;
                            ev.trackPosition.x *=
                                Constants.MAP_WIDTH /
                                Constants.MAP_ACTUAL_WIDTH;
                            ev.trackPosition.y *=
                                Constants.MAP_HEIGHT /
                                Constants.MAP_ACTUAL_HEIGHT;

                            cursor.addEvent(ev);

                            // Keep the sprite position in sync on the update thread (mirrors
                            // the replay branch): the trail reads the entity position inside
                            // sprite.update(dt) later this tick, and the draw-thread fast path
                            // may not have observed the new tap yet. UP carries only the lift
                            // position; the cursor is hidden there, so the last position stays.
                            if (ev.action != TouchEvent.ACTION_UP && cursorSprites != null && cursorSprites[pi] != null) {
                                cursorSprites[pi].setPosition(ev.position.x, ev.position.y);

                                // Fresh press: restart the trail instead of interpolating
                                // across the re-press gap (see markDiscontinuity).
                                if (ev.action == TouchEvent.ACTION_DOWN) {
                                    cursorSprites[pi].onCursorPress();
                                }
                            }

                            if (!sampleDown) {
                                // Finger lifted: hide the sprite immediately (mirrors the
                                // UP branch of onSceneTouchEvent).
                                if (cursorSprites != null && cursorSprites[pi] != null) {
                                    cursorSprites[pi].setShowing(false);
                                }
                            }

                            // Replay recording is centralized in recordReplayMovements()
                            // (end of the tick, over cursors[i].events) — see its docs.
                        }
                    }
                } else {
                    // Raw path inactive (replays, pause, game over, mods): drain ALL slots
                    // so queued samples can't flood gameplay as a burst of stale events
                    // when the raw path re-enables.
                    var directInputView = GlobalManager.getInstance().getMainActivity().getDirectInputSurface();

                    if (directInputView != null) {
                        directInputView.clearPointerSamples();
                    }
                }

                var songService = GlobalManager.getInstance().getSongService();
                float speedMultiplier = GameHelper.getSpeedMultiplier();
                float dt = secElapsed * speedMultiplier;

                // Replay playback pause: gameplay time freezes while the scene (HUD,
                // replay panel) keeps updating, mirroring osu-droid's stopped gameplayClock.
                if (replayPlaybackPaused) {
                    dt = 0;
                } else if (songService.getStatus() == Status.PLAYING) {
                    // BASS may report the wrong position. When that happens, `dt` will either be negative or more than the
                    // actual progressed time. To prevent that situation from happening, we keep `dt` between thresholds.
                    // They serve as a buffer zone to allow audio and gameplay time to synchronize in cases where one is
                    // behind or ahead of the other.
                    // See https://github.com/ppy/osu/issues/26879 for more information.
                    float minDt = dt / 2;
                    float maxDt = dt * 2;

                    float audioElapsedTime =
                        (float) songService.getPositionPrecise() / 1000;
                    float gameElapsedTime =
                        elapsedTime - getRateAdjustedOffset();
                    float timeDifference = gameElapsedTime - audioElapsedTime;

                    // In some cases, the audio can be behind the gameplay time so far it would cause gameplay to
                    // completely desynchronize. In that case, we do not let gameplay progress at all until the audio
                    // catches up.
                    if (timeDifference <= 0.1f * speedMultiplier) {
                        float minimumSynchronizationTime =
                            (Config.getMinimumGameplaySynchronizationTime() *
                                speedMultiplier) /
                            1000;
                        // Sync gameplay with audio if the difference is too large.
                        if (
                            !isGameOver &&
                            timeDifference >= minimumSynchronizationTime
                        ) {
                            if (minimumSynchronizationTime > 0) {
                                Log.i(
                                    "GameScene",
                                    "Synchronizing gameplay time with audio time at " +
                                        audioElapsedTime +
                                        "s audio time and " +
                                        gameElapsedTime +
                                        "s gameplay time. Difference: " +
                                        timeDifference +
                                        "s"
                                );
                            }
                            dt = -timeDifference;
                        }

                        dt = FMath.clamp(dt, minDt, maxDt);
                    } else {
                        dt = 0;
                    }
                } else if (!musicStarted) {
                    float realElapsedTime = elapsedTime + dt;
                    float targetElapsedTime = Math.min(
                        realElapsedTime,
                        getRateAdjustedOffset()
                    );
                    float newElapsedTime;

                    if (isInterpolating) {
                        newElapsedTime = Interpolation.dampContinuously(
                            realElapsedTime,
                            targetElapsedTime,
                            0.08f,
                            secElapsed
                        );

                        // If the difference is more than ~2 frames at 60 FPS, snap to the target time.
                        if (
                            Math.abs(targetElapsedTime - newElapsedTime) >
                            (1f / 60f) * 2f * speedMultiplier
                        ) {
                            newElapsedTime = targetElapsedTime;
                            isInterpolating = false;
                        }
                    } else {
                        newElapsedTime = targetElapsedTime;

                        // Only interpolate on second frame onwards.
                        if (previousFrameTime > 0) {
                            isInterpolating = true;
                        }
                    }

                    dt = Math.max(0, newElapsedTime - elapsedTime);
                }

                update(dt);

                // Record this tick's cursor events into the replay BEFORE they are
                // cleared by Cursor.reset below (see recordReplayMovements).
                recordReplayMovements();

                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < cursors.length; ++i) {
                    cursors[i].reset(previousFrameTime, elapsedTime * 1000);
                }

                super.onManagedUpdate(dt);
            }

            private void applyRawPointerFastPath(final Camera camera) {
                var touchController = engine.getTouchController();

                if (
                    touchController == null ||
                    !touchController.isUseRawPointers()
                ) {
                    return;
                }

                if (
                    paused || replaying ||
                    GameHelper.isAutoplay() || GameHelper.isAutopilot()
                ) {
                    return;
                }

                var sprites = cursorSprites;

                if (sprites == null) {
                    return;
                }

                // Use update thread's active state to determine visibility of the sprites to respect the maximum
                // active cursor limitation.
                int count = Math.min(
                    Math.min(getCursorsCount(), sprites.length),
                    touchController.getRawPointerCapacity()
                );
                int updatePathActiveCount = 0;

                for (int i = 0; i < count; ++i) {
                    var sprite = sprites[i];

                    if (sprite == null) {
                        continue;
                    }

                    // Use the hardware touch state directly instead of
                    // cursor.isMouseDown(), which relies on event processing
                    // and can lag behind the actual finger state.
                    boolean isFingerDown =
                        touchController.isRawPointerDown(i);

                    if (!isFingerDown) {
                        sprite.setShowing(false);
                        fastPathHasStableSnapshot[i] = false;
                        continue;
                    }

                    sprite.setShowing(true);

                    if (updatePathActiveCount >= maximumActiveCursorCount) {
                        continue;
                    }

                    ++updatePathActiveCount;

                    boolean readOk = tryReadRawPointer(i);
                    if (readOk) {
                        fastPathLastStableX[i] = fastPathSurfaceCoords[0];
                        fastPathLastStableY[i] = fastPathSurfaceCoords[1];
                        fastPathHasStableSnapshot[i] = true;
                    } else if (fastPathHasStableSnapshot[i]) {
                        // Revert to latest stable coordinates if read fails.
                        fastPathSurfaceCoords[0] = fastPathLastStableX[i];
                        fastPathSurfaceCoords[1] = fastPathLastStableY[i];
                    } else {
                        // No stable sample yet. Keep update thread position.
                        continue;
                    }

                    // Per underlying implementation, this is thread-safe since the camera is never rotated (thus the
                    // shared array is never used). When this is not the case, this must be revisited.
                    float[] sceneCoords =
                        Cameras.convertSurfaceToSceneCoordinates(
                            camera,
                            fastPathSurfaceCoords
                        );

                    sprite.setPosition(sceneCoords[0], sceneCoords[1]);
                }
            }

            private boolean tryReadRawPointer(int pointerId) {
                var touchController = engine.getTouchController();

                if (touchController == null) {
                    return false;
                }

                for (int attempt = 0; attempt < 2; ++attempt) {
                    int versionBefore = touchController.getRawPointerVersion(
                        pointerId
                    );

                    // An odd version means the main thread is updating this pointer, so we wait.
                    if ((versionBefore & 1) != 0) {
                        continue;
                    }

                    float x = touchController.getRawPointerSurfaceX(pointerId);
                    float y = touchController.getRawPointerSurfaceY(pointerId);

                    int versionAfter = touchController.getRawPointerVersion(
                        pointerId
                    );

                    if (
                        versionBefore == versionAfter && (versionAfter & 1) == 0
                    ) {
                        // Successfully read a consistent snapshot.
                        fastPathSurfaceCoords[0] = x;
                        fastPathSurfaceCoords[1] = y;
                        return true;
                    }

                }

                return false;
            }
        };
    }

    private void applyPlayfieldSizeScale() {
        // IMPORTANT: This MUST be called only when the game scene is displayed, otherwise it will scale the currently
        // displayed scene (the one before gameplay starts), which is not what we want.
        if (
            !(GlobalManager.getInstance().getCamera() instanceof
                SmoothCamera camera)
        ) {
            return;
        }

        float playfieldSize = Config.getPlayfieldSize();
        float playfieldHorizontalPosition =
            Config.getPlayfieldHorizontalPosition();
        float playfieldVerticalPosition = Config.getPlayfieldVerticalPosition();

        camera.setZoomFactorDirect(playfieldSize);

        camera.setCenterDirect(
            Config.getRES_WIDTH() *
                Interpolation.linear(
                    Interpolation.linear(0f, 0.5f, playfieldSize),
                    Interpolation.linear(1f, 0.5f, playfieldSize),
                    1 - playfieldHorizontalPosition
                ),
            Config.getRES_HEIGHT() *
                Interpolation.linear(
                    Interpolation.linear(0f, 0.5f, playfieldSize),
                    Interpolation.linear(1f, 0.5f, playfieldSize),
                    1 - playfieldVerticalPosition
                )
        );


    }

    private void resetPlayfieldSizeScale() {
        if (
            !(GlobalManager.getInstance().getCamera() instanceof
                SmoothCamera camera)
        ) {
            return;
        }

        camera.setZoomFactorDirect(1f);
        camera.setCenterDirect(
            Config.getRES_WIDTH() / 2f,
            Config.getRES_HEIGHT() / 2f
        );

        mgScene.setRotation(0f);
        mgScene.setScale(1f);
    }

    private int estimateMaximumActiveObjects() {
        if (objects == null) {
            return 0;
        }

        // Estimate the maximum number of simultaneously active objects to pre-size the lists and minimize
        // array reallocations.
        var lifetimeEnds = new PriorityQueue<Double>(
            Math.max(1, objects.length / 4)
        );
        int estimatedMaxActiveObjects = 0;

        for (var object : objects) {
            double lifetimeStart = object.startTime - object.timePreempt;

            // Remove all objects that have expired by the time this object's lifetime starts.
            while (
                !lifetimeEnds.isEmpty() && lifetimeEnds.peek() <= lifetimeStart
            ) {
                lifetimeEnds.poll();
            }

            double lifetimeEnd;

            if (object instanceof HitCircle) {
                var hitWindow = object.hitWindow;
                lifetimeEnd =
                    object.startTime +
                    (hitWindow != null ? hitWindow.getMehWindow() : 0);
            } else if (object instanceof Slider slider) {
                lifetimeEnd = Math.max(
                    object.startTime +
                        slider.getHead().hitWindow.getMehWindow(),
                    object.getEndTime()
                );
            } else {
                lifetimeEnd = object.getEndTime();
            }

            lifetimeEnds.add(lifetimeEnd);
            estimatedMaxActiveObjects = Math.max(
                estimatedMaxActiveObjects,
                lifetimeEnds.size()
            );
        }

        return estimatedMaxActiveObjects;
    }

    private float getRateAt(double time) {
        return ModUtils.calculateRateWithTrackRateMods(rateAdjustingMods, time);
    }

    /**
     * Called when the user changes the playback rate in the replay settings panel.
     * The rate is applied by the update loop (modRate * replaySettingsRate), mirroring
     * upstream's gameplayClock.setRate() path.
     */
    public void onReplayRateChanged(float rate) {
        replaySettingsRate = rate;
    }

    public float getReplaySettingsRate() {
        return replaySettingsRate;
    }

    private float getRateAdjustedOffset() {
        return totalOffset * GameHelper.getSpeedMultiplier();
    }

    private void initializeParticleSystems() {
        // Check if kiai particles are enabled
        if (
            !ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean(
                "kiaiParticles",
                true
            )
        ) {
            android.util.Log.d(
                "GameScene",
                "Kiai particles disabled in settings"
            );
            return;
        }

        try {
            var starRegion =
                ru.nsu.ccfit.zuev.osuplusplus.ResourceManager.getInstance().getTexture(
                    "sliderscorepoint"
                );
            if (starRegion == null) {
                // Fallback: try to load sliderscorepoint from assets
                starRegion =
                    ru.nsu.ccfit.zuev.osuplusplus.ResourceManager.getInstance().loadTexture(
                        "sliderscorepoint",
                        "gfx/sliderscorepoint.png",
                        false
                    );
            }
            if (starRegion == null) {
                android.util.Log.w(
                    "GameScene",
                    "Sliderscorepoint texture not found for particle systems"
                );
                return;
            }

            // Left particle system
            particleSystem[0] =
                new org.anddev.andengine.entity.particle.ParticleSystem(
                    new org.anddev.andengine.entity.particle.emitter.PointParticleEmitter(
                        -40,
                        (float) (Config.getRES_HEIGHT() * 3) / 4
                    ),
                    32,
                    48,
                    128,
                    starRegion
                );
            particleSystem[0].setBlendFunction(
                javax.microedition.khronos.opengles.GL10.GL_SRC_ALPHA,
                javax.microedition.khronos.opengles.GL10.GL_ONE_MINUS_SRC_ALPHA
            );

            particleSystem[0].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.VelocityInitializer(
                    150,
                    430,
                    -480,
                    -520
                )
            );
            particleSystem[0].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.AccelerationInitializer(
                    10,
                    30
                )
            );
            particleSystem[0].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.RotationInitializer(
                    0.0f,
                    360.0f
                )
            );

            particleSystem[0].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.ScaleModifier(
                    0.5f,
                    2.0f,
                    0.0f,
                    1.0f
                )
            );
            particleSystem[0].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.AlphaModifier(
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            );
            particleSystem[0].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.ExpireModifier(
                    1.0f
                )
            );

            particleSystem[0].setParticlesSpawnEnabled(false);
            bgScene.attachChild(particleSystem[0]);

            // Right particle system
            particleSystem[1] =
                new org.anddev.andengine.entity.particle.ParticleSystem(
                    new org.anddev.andengine.entity.particle.emitter.PointParticleEmitter(
                        Config.getRES_WIDTH(),
                        (float) (Config.getRES_HEIGHT() * 3) / 4
                    ),
                    32,
                    48,
                    128,
                    starRegion
                );
            particleSystem[1].setBlendFunction(
                javax.microedition.khronos.opengles.GL10.GL_SRC_ALPHA,
                javax.microedition.khronos.opengles.GL10.GL_ONE_MINUS_SRC_ALPHA
            );

            particleSystem[1].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.VelocityInitializer(
                    -150,
                    -430,
                    -480,
                    -520
                )
            );
            particleSystem[1].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.AccelerationInitializer(
                    -10,
                    30
                )
            );
            particleSystem[1].addParticleInitializer(
                new org.anddev.andengine.entity.particle.initializer.RotationInitializer(
                    0.0f,
                    360.0f
                )
            );

            particleSystem[1].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.ScaleModifier(
                    0.5f,
                    2.0f,
                    0.0f,
                    1.0f
                )
            );
            particleSystem[1].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.AlphaModifier(
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            );
            particleSystem[1].addParticleModifier(
                new org.anddev.andengine.entity.particle.modifier.ExpireModifier(
                    1.0f
                )
            );

            particleSystem[1].setParticlesSpawnEnabled(false);
            bgScene.attachChild(particleSystem[1]);
        } catch (Exception e) {
            android.util.Log.e(
                "GameScene",
                "Failed to initialize particle systems: " + e.getMessage()
            );
        }
    }

    private void updateKiaiEffects() {
        try {
            boolean kiaiParticlesEnabled =
                ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean(
                    "kiaiParticles",
                    true
                );

            if (
                kiaiParticlesEnabled &&
                !isContinuousKiai &&
                activeEffectPoint.isKiai &&
                particleSystem[0] != null &&
                particleSystem[1] != null
            ) {
                for (var particleSpout : particleSystem) {
                    particleSpout.setParticlesSpawnEnabled(true);
                }
                particleBeginTime = (int) (elapsedTime * 1000);
                particleEnabled = true;
            }

            // Triangle kiai boost on kiai start
            if (!isContinuousKiai && activeEffectPoint.isKiai) {
                if (triangleBg != null) triangleBg.setKiai(true);
                if (screenShake != null) screenShake.shake(2f, 0.15f);
            }

            isContinuousKiai = activeEffectPoint.isKiai;

            if (
                kiaiParticlesEnabled &&
                particleEnabled &&
                elapsedTime * 1000 - particleBeginTime > 2000
            ) {
                for (var particleSpout : particleSystem) {
                    if (particleSpout != null) {
                        particleSpout.setParticlesSpawnEnabled(false);
                    }
                }
                particleEnabled = false;
            }
        } catch (Exception e) {
            android.util.Log.e(
                "GameScene",
                "Error updating kiai effects: " + e.getMessage()
            );
        }
    }

    /**
     * Updates the stable letterbox-in-breaks bars: black strips shown while the
     * current break runs, only when the map's {@code LetterboxInBreaks} is set.
     *
     * <p>Alpha follows the opsu reference (Game.java:543): fade in over the first
     * 500ms of the break and out over its last 500ms, up to 0.4, and only for
     * breaks of at least 4 seconds. Driven by elapsed time so pause, seek and
     * break-end stay correct without extra events.
     */
    private void updateLetterbox(float elapsedTime) {
        if (letterboxTop == null || letterboxBottom == null) {
            return;
        }

        float alpha = 0f;
        boolean tracked =
            playableBeatmap != null &&
            playableBeatmap.getGeneral().letterboxInBreaks &&
            breakPeriods != null &&
            breakPeriodIndex > 0 &&
            breakPeriodIndex <= breakPeriods.length;

        if (tracked) {
            var period = breakPeriods[breakPeriodIndex - 1];
            float timeMs = elapsedTime * 1000f;

            if (
                period.getDuration() >= 4000f &&
                timeMs >= period.startTime &&
                timeMs <= period.endTime
            ) {
                float fadeIn = Math.min(500f, timeMs - period.startTime);
                float fadeOut = Math.min(500f, period.endTime - timeMs);
                alpha = 0.4f * Math.min(fadeIn, fadeOut) / 500f;
            }
        }

        letterboxTop.setAlpha(alpha);
        letterboxBottom.setAlpha(alpha);
    }

    private void updateKiaiFlash(float dt) {
        if (kiaiFlashOverlay == null) return;

        try {
            boolean kiaiFlashEnabled =
                ru.nsu.ccfit.zuev.osuplusplus.Config.getBoolean(
                    "kiaiFlash",
                    true
                );
            if (!kiaiFlashEnabled) {
                kiaiFlashOverlay.setVisible(false);
                kiaiFlashOverlay.setAlpha(0f);
                return;
            }

            boolean isKiai =
                activeEffectPoint != null && activeEffectPoint.isKiai;

            // ONE flash per kiai section: trigger only when kiai STARTS
            if (isKiai && !wasKiaiFlash && !kiaiFlashTriggered) {
                kiaiFlashTriggered = true;
                kiaiFlashTimer = 0f;
                kiaiFlashAlpha = 0f;
                kiaiFlashOverlay.setColor(1f, 1f, 1f);
                kiaiFlashOverlay.setVisible(true);
            }

            // Reset for next kiai section
            if (!isKiai) {
                kiaiFlashTriggered = false;
            }

            wasKiaiFlash = isKiai;

            // Fade in then fade out — one shot
            if (kiaiFlashTriggered && kiaiFlashTimer < 0.55f) {
                kiaiFlashTimer += dt;

                if (kiaiFlashTimer < 0.15f) {
                    kiaiFlashAlpha = (kiaiFlashTimer / 0.15f) * 0.2f;
                } else {
                    float fadeOut = (kiaiFlashTimer - 0.15f) / 0.4f;
                    kiaiFlashAlpha = (1f - fadeOut) * 0.2f;
                }

                kiaiFlashOverlay.setAlpha(kiaiFlashAlpha);
            } else {
                kiaiFlashOverlay.setVisible(false);
                kiaiFlashOverlay.setAlpha(0f);
            }
        } catch (Exception e) {
            android.util.Log.e(
                "GameScene",
                "Error updating kiai flash: " + e.getMessage()
            );
        }
    }

    /**
     * Updates the FailingLayer (ported from osu!(lazer) HUD/FailingLayer.cs): a fullscreen red overlay
     * whose alpha rises as health falls below {@link #LOW_HEALTH_THRESHOLD}, and drops back as it recovers.
     * Only shown when failing is possible; replays, autoplay and multiplayer never see it.
     */
    private void updateLowHealthOverlay(float dt) {
        if (lowHealthOverlay == null) {
            return;
        }

        try {
            boolean showLayer =
                gameStarted &&
                stat != null && stat.canFail &&
                !GameHelper.isAutoplay() &&
                !GameHelper.isAutopilot() &&
                !replaying &&
                !Multiplayer.isMultiplayer;

            float target = 0f;

            if (showLayer) {
                target = Math.max(
                    0f,
                    Math.min(
                        LOW_HEALTH_MAX_ALPHA *
                            (1f - stat.getHp() / LOW_HEALTH_THRESHOLD),
                        LOW_HEALTH_MAX_ALPHA
                    )
                );
            }

            // Exponential approach to the target alpha, frame-rate independent.
            float t = 1f - (float) Math.exp(-dt * LOW_HEALTH_LERP_SPEED);
            lowHealthAlpha += (target - lowHealthAlpha) * t;

            if (lowHealthAlpha < 0.002f) {
                lowHealthAlpha = 0f;
            }

            boolean visible = lowHealthAlpha > 0f;

            lowHealthOverlay.setVisible(visible);

            if (visible) {
                lowHealthOverlay.setAlpha(lowHealthAlpha);
            }
        } catch (Exception e) {
            android.util.Log.e(
                "GameScene",
                "Error updating low health overlay: " + e.getMessage()
            );
        }
    }

    /**
     * Update cursor position in GameState for Lua plugins.
     * Finds the raw pixel position of the cursor regardless of input method.
     */
    private void updatePluginCursor() {
        try {
            float cx = -1, cy = -1;
            if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                if (autoCursor != null) {
                    cx = autoCursor.getX();
                    cy = autoCursor.getY();
                }
            } else if (cursorSprites != null) {
                for (var s : cursorSprites) {
                    if (s.getX() > 0) {
                        cx = s.getX();
                        cy = s.getY();
                        break;
                    }
                }
            } else if (mainCursorId >= 0 && mainCursorId < cursors.length) {
                var latest = cursors[mainCursorId].getLatestEvent(TouchEvent.ACTION_DOWN, TouchEvent.ACTION_MOVE);
                if (latest != null) {
                    cx = latest.position.x;
                    cy = latest.position.y;
                }
            }
            if (cx >= 0 && cy >= 0) {
                com.osudroid.plugin.GameState.setCursorPosition(cx, cy);
            }
        } catch (Exception ignored) {}
    }
}
