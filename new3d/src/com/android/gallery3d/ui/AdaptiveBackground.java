package com.android.gallery3d.ui;

import com.android.gallery3d.anim.FloatAnimation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

public class AdaptiveBackground extends GLView {

    private static final int BACKGROUND_WIDTH = 128;
    private static final int BACKGROUND_HEIGHT = 64;
    private static final int FILTERED_COLOR = 0xffaaaaaa;
    private static final int ANIMATION_DURATION = 500;

    private MixedTexture mMixedTexture;
    private final Paint mPaint;
    private Bitmap mPendingBitmap;
    private final FloatAnimation mAnimation =
            new FloatAnimation(0, 1, ANIMATION_DURATION);

    public AdaptiveBackground() {
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setColorFilter(new LightingColorFilter(FILTERED_COLOR, 0));
        mPaint = paint;
    }

    public Bitmap getAdaptiveBitmap(Bitmap bitmap) {
        Bitmap target = Bitmap.createBitmap(
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = 0;
        int top = 0;
        if (width * BACKGROUND_HEIGHT > height * BACKGROUND_WIDTH) {
            float scale = (float) BACKGROUND_HEIGHT / height;
            canvas.scale(scale, scale);
            left = (BACKGROUND_WIDTH - (int) (width * scale + 0.5)) / 2;
        } else {
            float scale = (float) BACKGROUND_WIDTH / width;
            canvas.scale(scale, scale);
            top = (BACKGROUND_HEIGHT - (int) (height * scale + 0.5)) / 2;
        }
        canvas.drawBitmap(bitmap, left, top, mPaint);
        BoxBlurFilter.apply(target,
                BoxBlurFilter.MODE_REPEAT, BoxBlurFilter.MODE_CLAMP);
        return target;
    }

    private void startTransition(Bitmap bitmap) {
        BitmapTexture texture = new BitmapTexture(bitmap);
        if (mMixedTexture == null) {
            mMixedTexture = new MixedTexture(texture);
        } else {
            mMixedTexture.setNewDestination(texture);
        }
        mMixedTexture.setMixtureRatio(0);
        mAnimation.start();
        invalidate();
    }

    public void setImage(Bitmap bitmap) {
        if (mAnimation.isActive()) {
            mPendingBitmap = bitmap;
        } else {
            startTransition(bitmap);
        }
    }

    public void setScrollPosition(int position) {
        if (mScrollX == position) return;
        mScrollX = position;
        invalidate();
    }

    @Override
    protected void render(GLCanvas canvas) {
        if (mMixedTexture == null) return;

        if (mAnimation.calculate(canvas.currentAnimationTimeMillis())) {
            mMixedTexture.setMixtureRatio(mAnimation.get());
            invalidate();
        } else if (mPendingBitmap != null) {
            startTransition(mPendingBitmap);
            mPendingBitmap = null;
        }

        int height = getHeight();
        float scale = (float) height / BACKGROUND_HEIGHT;
        int width = (int) (BACKGROUND_WIDTH * scale + 0.5f);
        int scroll = mScrollX;
        int start = (scroll / width) * width;

        MixedTexture mixed = mMixedTexture;
        for (int i = start, n = scroll + getWidth(); i < n; i += width) {
            mMixedTexture.draw(canvas, i - scroll, 0, width, height);
        }
    }
}