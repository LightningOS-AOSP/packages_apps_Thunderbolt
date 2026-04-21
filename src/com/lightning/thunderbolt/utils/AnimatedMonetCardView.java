package com.lightning.thunderbolt.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;

import com.google.android.material.card.MaterialCardView;

import java.util.Random;

public class AnimatedMonetCardView extends MaterialCardView {

    private ValueAnimator colorAnimator;
    private int currentColor;
    private final Random random = new Random();
    private Runnable animationRunnable;

    public AnimatedMonetCardView(Context context) {
        super(context);
        init();
    }

    public AnimatedMonetCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedMonetCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        currentColor = Color.parseColor("#1A1A1A");

        animationRunnable = new Runnable() {
            @Override
            public void run() {
                float[] hsv = new float[3];
                hsv[0] = random.nextInt(360);
                hsv[1] = 0.4f + random.nextFloat() * 0.2f;
                hsv[2] = 0.2f + random.nextFloat() * 0.2f;

                int nextColor = Color.HSVToColor(hsv);

                if (colorAnimator != null && colorAnimator.isRunning()) {
                    colorAnimator.cancel();
                }

                colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, nextColor);
                colorAnimator.setDuration(1500);
                colorAnimator.addUpdateListener(animator -> {
                    currentColor = (int) animator.getAnimatedValue();
                    setCardBackgroundColor(currentColor);
                });
                colorAnimator.start();

                postDelayed(this, 4000);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode() && animationRunnable != null) {
            postDelayed(animationRunnable, 1000);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(animationRunnable);
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
    }
}
