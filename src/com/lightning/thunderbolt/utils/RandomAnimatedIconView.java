package com.lightning.thunderbolt.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.Random;

public class RandomAnimatedIconView extends AppCompatImageView {

    private final int mAnimType;

    private AnimatorSet mAnimatorSet;
    private ValueAnimator mSingleAnimator;

    public RandomAnimatedIconView(Context context) {
        super(context);
        mAnimType = new Random().nextInt(5);
    }

    public RandomAnimatedIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAnimType = new Random().nextInt(5);
    }

    public RandomAnimatedIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAnimType = new Random().nextInt(5);
    }

    private void buildAndStartAnimation() {
        cancelAll();

        switch (mAnimType) {
            case 0: startSpinAnimation();   break;
            case 1: startPulseAnimation();  break;
            case 2: startFloatAnimation();  break;
            case 3: startWobbleAnimation(); break;
            case 4: startFlipAnimation();   break;
        }
    }

    private void startSpinAnimation() {
        mSingleAnimator = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        mSingleAnimator.setDuration(4000);
        mSingleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSingleAnimator.setInterpolator(new LinearInterpolator());
        mSingleAnimator.start();
    }

    private void startPulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.15f, 1f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(scaleX, scaleY);
        mAnimatorSet.setDuration(2500);
        mAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimatorSet.start();
    }

    private void startFloatAnimation() {
        mSingleAnimator = ObjectAnimator.ofFloat(this, "translationY", 0f, -8f, 0f);
        mSingleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSingleAnimator.setDuration(3000);
        mSingleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSingleAnimator.start();
    }

    private void startWobbleAnimation() {
        mSingleAnimator = ObjectAnimator.ofFloat(this, "rotation", 0f, -15f, 15f, 0f);
        mSingleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSingleAnimator.setDuration(2000);
        mSingleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSingleAnimator.start();
    }

    private void startFlipAnimation() {
        mSingleAnimator = ObjectAnimator.ofFloat(this, "rotationY", 0f, 360f);
        mSingleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSingleAnimator.setDuration(5000);
        mSingleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSingleAnimator.start();
    }

    private void cancelAll() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }
        if (mSingleAnimator != null) {
            mSingleAnimator.cancel();
            mSingleAnimator = null;
        }
        setRotation(0f);
        setRotationY(0f);
        setScaleX(1f);
        setScaleY(1f);
        setTranslationY(0f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == View.VISIBLE) {
            buildAndStartAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAll();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isAttachedToWindow()) {
            if (visibility == View.VISIBLE) {
                buildAndStartAnimation();
            } else {
                cancelAll();
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            buildAndStartAnimation();
        } else {
            cancelAll();
        }
    }
}
