package org.anddev.andengine.entity.sprite;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.primitive.BaseRectangle;
import org.anddev.andengine.opengl.texture.region.BaseTextureRegion;
import org.anddev.andengine.opengl.texture.region.buffer.TextureRegionBuffer;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.vertex.RectangleVertexBuffer;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:38:53 - 08.03.2010
 */
public abstract class BaseSprite extends BaseRectangle {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final org.anddev.andengine.entity.optimization.TransformOptimizer _transformOpt = null; // unused, just ensures class loaded

	// ===========================================================
	// Fields
	// ===========================================================

	protected final BaseTextureRegion mTextureRegion;

	// ===========================================================

	/**
	 * Inlined hot-path draw for BaseSprite.
	 * Avoids virtual dispatch through Entity→Shape→BaseSprite chain.
	 * Handles: glPushMatrix + translate + [rotate+scale merged] + texture bind + draw + glPopMatrix
	 */
	@Override
	protected void onManagedDraw(final GL10 pGL, final Camera pCamera) {
		pGL.glPushMatrix();
		{
			// Translation
			pGL.glTranslatef(this.mX, this.mY, 0);

			// Optimized rotation+scale (merged when centers equal)
			org.anddev.andengine.entity.optimization.TransformOptimizer.applyRotationAndScale(
				pGL, this.mRotation, this.mScaleX, this.mScaleY,
				this.mRotationCenterX, this.mRotationCenterY,
				this.mScaleCenterX, this.mScaleCenterY);

			// Bind texture
			this.mTextureRegion.onApply(pGL);

			// Init GL state
			GLHelper.setColor(pGL, this.mRed, this.mGreen, this.mBlue, this.mAlpha);
			GLHelper.enableVertexArray(pGL);
			GLHelper.blendFunction(pGL, this.mSourceBlendFunction, this.mDestinationBlendFunction);
			GLHelper.enableTextures(pGL);
			GLHelper.enableTexCoordArray(pGL);

			// Apply vertices (VBO or client pointer)
			if(GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS) {
				final javax.microedition.khronos.opengles.GL11 gl11 = (javax.microedition.khronos.opengles.GL11)pGL;
				this.getVertexBuffer().selectOnHardware(gl11);
				GLHelper.vertexZeroPointer(gl11);
			} else {
				GLHelper.vertexPointer(pGL, this.getVertexBuffer().getFloatBuffer());
			}

			// Draw quad
			pGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

			// Draw children
			if(this.mChildrenVisible && this.mChildren != null) {
				this.onManagedDrawChildren(pGL, pCamera);
			}
		}
		pGL.glPopMatrix();
	}
	// Constructors
	// ===========================================================

	public BaseSprite(final float pX, final float pY, final float pWidth, final float pHeight, final BaseTextureRegion pTextureRegion) {
		super(pX, pY, pWidth, pHeight);

		this.mTextureRegion = pTextureRegion;
		this.initBlendFunction();
	}

	public BaseSprite(final float pX, final float pY, final float pWidth, final float pHeight, final BaseTextureRegion pTextureRegion, final RectangleVertexBuffer pRectangleVertexBuffer) {
		super(pX, pY, pWidth, pHeight, pRectangleVertexBuffer);

		this.mTextureRegion = pTextureRegion;
		this.initBlendFunction();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public BaseTextureRegion getTextureRegion() {
		return this.mTextureRegion;
	}

	public void setFlippedHorizontal(final boolean pFlippedHorizontal) {
		this.mTextureRegion.setFlippedHorizontal(pFlippedHorizontal);
	}

	public void setFlippedVertical(final boolean pFlippedVertical) {
		this.mTextureRegion.setFlippedVertical(pFlippedVertical);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void reset() {
		super.reset();

		this.initBlendFunction();
	}

	@Override
	protected void onInitDraw(final GL10 pGL) {
		super.onInitDraw(pGL);
		GLHelper.enableTextures(pGL);
		GLHelper.enableTexCoordArray(pGL);
	}

	@Override
	protected void doDraw(final GL10 pGL, final Camera pCamera) {
		this.mTextureRegion.onApply(pGL);

		super.doDraw(pGL, pCamera);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		final TextureRegionBuffer textureRegionBuffer = this.mTextureRegion.getTextureBuffer();
		if(textureRegionBuffer.isManaged()) {
			textureRegionBuffer.unloadFromActiveBufferObjectManager();
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void initBlendFunction() {
		if(this.mTextureRegion.getTexture().getTextureOptions().mPreMultipyAlpha) {
			this.setBlendFunction(BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
