package com.osudroid.ui.v2.game
import ru.nsu.ccfit.zuev.osuplusplus.ResourceManager

import com.edlplan.framework.easing.*
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.beatmap.hitobject.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.*

object FollowPointConnection {


    private const val SPACING = 32

    private const val MAX_PREEMPT = 800


    @JvmStatic
    val pool = Pool {

        // For optimization, we avoid using AnimatedSprite if there's one frame.
        if (ResourceManager.getInstance().isTextureLoaded("followpoint-0")) {
            UIAnimatedSprite("followpoint", true, OsuSkin.get().animationFramerate).also { sprite ->
                sprite.frames.fastForEach {
                    it?.applyFollowPointMaxSize()
                }

                sprite.invalidate(InvalidationFlag.Content)
                sprite.isLoop = false
            }
        } else {
            UISprite(ResourceManager.getInstance().getTexture("followpoint")).also {
                it.textureRegion?.applyFollowPointMaxSize()
                it.invalidate(InvalidationFlag.Content)
            }
        }
    }

    private val expire = OnModifierFinished { fp ->
        updateThread {
            fp.detachSelf()
            fp.reset()

            // Only recycle if the sprite is still registered. If clearAll() already recycled it,
            // freeing it again would put the same instance twice into the pool, and obtaining it
            // twice would crash attachChild() with "pEntity already has a parent!".
            if (activeSprites.remove(fp)) {
                pool.free(fp as UISprite)
            }
        }
    }

    /**
     * Registry of currently attached follow point sprites, so [clearAll] can recycle
     * only follow points instead of every sprite on the scene.
     */
    private val activeSprites = java.util.Collections.synchronizedSet(HashSet<UISprite>())

    /**
     * Detaches and recycles every follow point sprite attached to the given scene.
     * Used on seek/restart where queued modifiers would otherwise be discarded without
     * returning their sprites to the pool.
     */
    @JvmStatic
    fun clearAll(scene: Scene) {
        val snapshot = synchronized(activeSprites) { activeSprites.toList() }

        snapshot.fastForEach { fp ->
            if (!activeSprites.remove(fp)) {
                return@fastForEach
            }

            fp.clearEntityModifiers()
            fp.detachSelf()
            fp.reset()
            pool.free(fp)
        }
    }

    /**
     * Drops every pooled follow point sprite so the next obtain() rebuilds them from
     * the freshly loaded skin textures: the pool factory captures TextureRegions at
     * construction, and pooled sprites keep referencing dead regions after a mid-game
     * skin switch. SliderTickSprite has the same problem, so its pool is drained too.
     * Must be called on the update thread.
     */
    @JvmStatic
    fun refreshTextures() {
        pool.clear()
        SliderTickSprite.pool.clear()
    }

    /**
     * Re-binds the texture of every follow point sprite CURRENTLY attached to the
     * scene: [refreshTextures] only drains the idle pool, but in-flight sprites keep
     * referencing the previous skin's GL texture until their fade-out modifier
     * completes. Re-pulling the region in place leaves their modifiers undisturbed.
     * Must be called on the update thread.
     */
    @JvmStatic
    fun refreshLiveTextures() {
        val snapshot = activeSprites.toList()
        snapshot.fastForEach { fp ->
            if (ResourceManager.getInstance().isTextureLoaded("followpoint-0")) {
                // Animated skin: swap to the frame list the pool factory would use.
                if (fp is UIAnimatedSprite) {
                    fp.setFrames("followpoint", true)
                } else {
                    // Non-animated sprite created before the animated frames existed:
                    // pooled sprites can't change class, so just re-bind the base
                    // region — the next pool cycle replaces it with the right type.
                    fp.textureRegion = ResourceManager.getInstance().getTexture("followpoint")
                }
            } else {
                fp.textureRegion = ResourceManager.getInstance().getTexture("followpoint")
            }
            fp.textureRegion?.applyFollowPointMaxSize()
            fp.invalidate(InvalidationFlag.Content)
        }

        // Re-bind sprites that were freed back into the pool DURING the refresh
        // window (expire modifiers run on the update thread too) so they are never
        // handed out again with the previous skin's region.
        pool.forEach { fp ->
            fp.textureRegion = ResourceManager.getInstance().getTexture("followpoint")
            fp.textureRegion?.applyFollowPointMaxSize()
            fp.invalidate(InvalidationFlag.Content)
        }
    }

    private fun TextureRegion.applyFollowPointMaxSize() {
        // Reference: https://github.com/ppy/osu/blob/0811de728e4205a45e485d53ccdaf19a937c6033/osu.Game.Rulesets.Osu/Skinning/Legacy/OsuLegacySkinTransformer.cs#L95-L97
        val newWidth = min(width, HitObject.OBJECT_RADIUS.toInt() * 2)
        val newHeight = min(height, HitObject.OBJECT_RADIUS.toInt())

        if (width != newWidth || height != newHeight) {

            // Crop the texture from the center.
            setTexturePosition(width / 2 - newWidth / 2, height / 2 - newHeight / 2)

            width = newWidth
            height = newHeight
        }
    }

    @JvmStatic
    fun addConnection(scene: Scene, secPassed: Float, start: HitObject, end: HitObject) {

        // Gravity mod: hide follow points since objects slide in from different directions
        if (ru.nsu.ccfit.zuev.osu.game.GameHelper.isGravity()) return

        // Reference: https://github.com/ppy/osu/blob/7bc8908ca9c026fed1d831eb6e58df7624a8d614/osu.Game.Rulesets.Osu/Objects/Drawables/Connections/FollowPointConnection.cs

        val scale = start.screenSpaceGameplayScale
        val startTime = (start.endTime / 1000f).toFloat()

        val startPosition = start.screenSpaceGameplayStackedEndPosition
        val endPosition = end.screenSpaceGameplayStackedPosition

        val distanceX = endPosition.x - startPosition.x
        val distanceY = endPosition.y - startPosition.y
        val rotation = atan2(distanceY, distanceX) * (180f / Math.PI).toFloat()

        val endFadeInTime = end.timeFadeIn.toFloat() / 1000f
        val duration = (end.startTime - start.endTime).toFloat() / 1000f

        // Preempt time can go below 800ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear preempt function.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        val preempt = min(MAX_PREEMPT.toFloat(), start.timePreempt.toFloat()) * min(1.0, start.timePreempt / HitObject.PREEMPT_MIN).toFloat() / 1000f

        // Since the unit of spacing is in osu!pixels, we cannot directly port the reference code. As such, we need to
        // approach it with another method. We use the distance between the start and end positions in osu!pixels to
        // determine the amount of sprites to spawn, and then map them into gameplay positions in pixels.
        val osuPixelsStartPosition = start.difficultyStackedEndPosition
        val osuPixelsEndPosition = end.difficultyStackedPosition

        val osuPixelsDistance = osuPixelsEndPosition.getDistance(osuPixelsStartPosition).toInt()

        var d = (SPACING * 1.5f).toInt()
        while (d < osuPixelsDistance - SPACING) {

            val fraction = d.toFloat() / osuPixelsDistance

            val pointStartX = startPosition.x + distanceX * (fraction - 0.1f)
            val pointStartY = startPosition.y + distanceY * (fraction - 0.1f)

            val pointEndX = startPosition.x + distanceX * fraction
            val pointEndY = startPosition.y + distanceY * fraction

            val fadeOutTime = startTime + fraction * duration
            val fadeInTime = fadeOutTime - preempt

            val fp = pool.obtain()

            activeSprites.add(fp)

            fp.clearEntityModifiers()

            // Defensive: a pooled sprite must never carry a stale parent, otherwise attachChild()
            // throws "pEntity already has a parent!". detachSelf() is a no-op without a parent.
            fp.detachSelf()

            fp.setPosition(pointStartX, pointStartY)
            fp.setScale(1.5f * scale)
            fp.origin = Anchor.Center
            fp.rotation = rotation
            fp.alpha = 0f

            fp.registerEntityModifier(
                Modifiers.sequence(expire,
                    Modifiers.delay(fadeInTime - secPassed),
                    Modifiers.parallel(null,
                        Modifiers.fadeIn(endFadeInTime),
                        Modifiers.scale(endFadeInTime, 1.5f * scale, scale, null, Easing.OutQuad),
                        Modifiers.move(endFadeInTime, pointStartX, pointEndX, pointStartY, pointEndY, null, Easing.OutQuad),
                        Modifiers.sequence(null,
                            Modifiers.delay(fadeOutTime - fadeInTime),
                            Modifiers.fadeOut(endFadeInTime)
                        )
                    )
                )
            )

            scene.attachChild(fp, 0)
            d += SPACING
        }
    }
}
