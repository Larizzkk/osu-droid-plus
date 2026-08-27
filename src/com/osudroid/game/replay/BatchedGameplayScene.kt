package com.osudroid.game.replay

import com.reco1l.andengine.UIScene
import com.reco1l.andengine.sprite.UISprite
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.sprite.batch.SpriteBatch
import org.anddev.andengine.opengl.texture.ITexture
import javax.microedition.khronos.opengles.GL10

/**
 * Drop-in replacement for UIScene that batch-renders all children during replay.
 *
 * During normal gameplay this is transparent — all children draw normally.
 * During replay, [activateBatching] switches to batched mode where children
 * still update via the normal Entity tree, but rendering is intercepted and
 * drawn through SpriteBatch draw calls grouped by texture.
 *
 * Render order is preserved: we batch CONSECUTIVE sprites with the same texture.
 * A new batch starts whenever the texture changes.
 */
class BatchedGameplayScene : UIScene() {

    companion object {
        private const val BATCH_CAPACITY = 512
        private const val MAX_BATCHES = 32
        private val SCENE_COORDS = FloatArray(2)
    }

    private var batchingActive = false
    private val batchPool = ArrayList<SpriteBatchEntry>(MAX_BATCHES)
    private val activeBatches = ArrayList<SpriteBatchEntry>(MAX_BATCHES)

    // ── Activation ──────────────────────────────────────────

    fun activateBatching() {
        batchingActive = true
    }

    fun deactivateBatching() {
        batchingActive = false
        for (entry in batchPool) {
            entry.batch.setIndex(0)
        }
        activeBatches.clear()
    }

    // ── Override child draw to batch during replay ───────────

    override fun onManagedDrawChildren(gl: GL10, camera: Camera) {
        if (!batchingActive) {
            super.onManagedDrawChildren(gl, camera)
            return
        }
        drawBatched(gl)
    }

    // ── Batched rendering ───────────────────────────────────

    private fun drawBatched(gl: GL10) {
        // Phase 1: Collect all sprites from children, preserving draw order
        val childCount = this.childCount
        for (i in 0 until childCount) {
            val child = this.getChild(i) as? Entity ?: continue
            if (!child.isVisible) continue
            collectChildrenInOrder(child)
        }

        // Phase 2: Submit and draw each batch in order
        for (i in 0 until activeBatches.size) {
            val entry = activeBatches[i]
            if (entry.batch.index > 0) {
                entry.batch.submit()
                // Draw via SpriteBatch's own draw path
                entry.batch.onDraw(gl, null)
            }
        }

        // Phase 3: Reset for next frame
        for (i in 0 until activeBatches.size) {
            activeBatches[i].batch.setIndex(0)
        }
        activeBatches.clear()
    }

    /**
     * Traverse entity tree in DFS order (same as normal draw order).
     * For each visible UISprite, add to the current batch if same texture,
     * or start a new batch if texture changed. This preserves render order.
     */
    private fun collectChildrenInOrder(entity: Entity) {
        val childCount = entity.childCount
        if (childCount == 0) return

        for (i in 0 until childCount) {
            val child = entity.getChild(i) as? Entity ?: continue
            if (!child.isVisible) continue

            if (child is UISprite) {
                addSpriteToBatch(child)
                // Also recurse for nested sprites (CirclePiece > circle, overlay, number)
                collectChildrenInOrder(child)
                continue
            }

            // Non-sprite entities (containers, rectangles, etc.) — recurse
            collectChildrenInOrder(child)
        }
    }

    /**
     * Add a sprite to the batch. If the texture matches the current batch,
     * append to it. Otherwise, flush the current batch and start a new one.
     * This preserves exact render order while batching consecutive same-texture sprites.
     */
    private fun addSpriteToBatch(sprite: UISprite) {
        val texRegion = sprite.textureRegion ?: return
        val tex = texRegion.texture ?: return
        val w = sprite.width
        val h = sprite.height
        if (w <= 0f || h <= 0f) return

        val rotation = sprite.rotation
        val scaleX = sprite.scaleX
        val scaleY = sprite.scaleY

        val texId = System.identityHashCode(tex)

        // Check if we can append to the current batch
        val lastEntry = if (activeBatches.isNotEmpty()) activeBatches.last() else null
        val canAppend = lastEntry != null
            && lastEntry.textureId == texId
            && lastEntry.batch.index < BATCH_CAPACITY

        val entry = if (canAppend) {
            lastEntry!!
        } else {
            val newEntry = getOrCreateBatchEntry(texId, tex)
            activeBatches.add(newEntry)
            newEntry
        }

        // Get world position
        val coords = sprite.convertLocalToSceneCoordinates(0f, 0f, SCENE_COORDS)
        val worldX = coords[0]
        val worldY = coords[1]

        // Draw into batch — preserving the sprite's exact world-space transform
        val batch = entry.batch
        if (rotation == 0f && scaleX == 1f && scaleY == 1f) {
            batch.drawWithoutChecks(texRegion, worldX, worldY, w, h)
        } else if (scaleX == 1f && scaleY == 1f) {
            batch.drawWithoutChecks(texRegion, worldX, worldY, w, h, rotation)
        } else {
            batch.drawWithoutChecks(texRegion, worldX, worldY, w, h, rotation, scaleX, scaleY)
        }
    }

    private fun getOrCreateBatchEntry(texId: Int, tex: ITexture): SpriteBatchEntry {
        // Check if we have a reusable entry in the pool
        for (entry in batchPool) {
            if (!entry.inUse && entry.textureId == texId) {
                entry.inUse = true
                return entry
            }
        }
        // Create new entry
        if (batchPool.size >= MAX_BATCHES) {
            // Overflow: find an unused entry and reuse it
            for (entry in batchPool) {
                if (!entry.inUse) {
                    entry.textureId = texId
                    entry.batch = SpriteBatch(tex, BATCH_CAPACITY)
                    entry.inUse = true
                    return entry
                }
            }
            // Should never reach here with MAX_BATCHES > number of distinct textures
            val fallback = batchPool[0]
            fallback.textureId = texId
            fallback.batch = SpriteBatch(tex, BATCH_CAPACITY)
            fallback.inUse = true
            return fallback
        }
        val entry = SpriteBatchEntry(texId, SpriteBatch(tex, BATCH_CAPACITY))
        entry.inUse = true
        batchPool.add(entry)
        return entry
    }

    /** Internal batch entry with texture tracking and pool support. */
    private class SpriteBatchEntry(
        var textureId: Int,
        var batch: SpriteBatch,
        var inUse: Boolean = false
    )
}
