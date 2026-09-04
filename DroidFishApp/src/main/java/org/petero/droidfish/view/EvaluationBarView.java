package org.petero.droidfish.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

/**
 * Animated horizontal evaluation bar for chess engine analysis.
 * Displays winning probability bar and numerical evaluation score.
 */
public class EvaluationBarView extends View {
    private boolean hasScore = false;
    private int whiteScore = 0; // centipawns
    private boolean isMate = false;
    private int mateMoves = 0;
    private boolean flipped = false;

    private float currentRatio = 0.5f;
    private float targetRatio = 0.5f;
    private ValueAnimator animator = null;

    private Paint whitePaint;
    private Paint blackPaint;
    private Paint borderPaint;
    private Paint textDarkPaint;
    private Paint textWhitePaint;

    private final Path clipPath = new Path();
    private final RectF boundsRect = new RectF();

    public EvaluationBarView(Context context) {
        this(context, null);
    }

    public EvaluationBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EvaluationBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setColor(0xFFF2F2F2);

        blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackPaint.setStyle(Paint.Style.FILL);
        blackPaint.setColor(0xFF262626);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        float strokeW = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, getResources().getDisplayMetrics());
        borderPaint.setStrokeWidth(strokeW);
        borderPaint.setColor(0x33808080);

        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11.0f, getResources().getDisplayMetrics());

        textDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textDarkPaint.setColor(0xFF1E1E1E);
        textDarkPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textDarkPaint.setTextSize(textSize);

        textWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textWhitePaint.setColor(0xFFFFFFFF);
        textWhitePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textWhitePaint.setTextSize(textSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(defaultHeight, heightSize);
        } else {
            height = defaultHeight;
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    public void setEvaluation(int whiteScore, boolean isMate, int mateMoves, boolean hasScore, boolean flipped) {
        this.whiteScore = whiteScore;
        this.isMate = isMate;
        this.mateMoves = mateMoves;
        this.hasScore = hasScore;
        this.flipped = flipped;

        float newTarget = calculateTargetRatio();
        if (Math.abs(newTarget - targetRatio) > 0.001f) {
            targetRatio = newTarget;
            if (isAttachedToWindow() && getVisibility() == VISIBLE) {
                if (animator != null) {
                    animator.cancel();
                }
                animator = ValueAnimator.ofFloat(currentRatio, targetRatio);
                animator.setDuration(250);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(animation -> {
                    currentRatio = (float) animation.getAnimatedValue();
                    invalidate();
                });
                animator.start();
            } else {
                currentRatio = targetRatio;
                invalidate();
            }
        } else {
            invalidate();
        }
    }

    public void resetEvaluation() {
        if (animator != null) {
            animator.cancel();
        }
        hasScore = false;
        whiteScore = 0;
        isMate = false;
        mateMoves = 0;
        currentRatio = 0.5f;
        targetRatio = 0.5f;
        invalidate();
    }

    public void setFlipped(boolean flipped) {
        if (this.flipped != flipped) {
            this.flipped = flipped;
            invalidate();
        }
    }

    private float calculateTargetRatio() {
        if (!hasScore) {
            return 0.5f;
        }
        if (isMate) {
            return mateMoves > 0 ? 1.0f : 0.0f;
        }
        // Sigmoid winning probability: P = 1 / (1 + exp(-0.00368208 * whiteScore))
        double p = 1.0 / (1.0 + Math.exp(-0.00368208 * whiteScore));
        if (p < 0.05) p = 0.05;
        if (p > 0.95) p = 0.95;
        return (float) p;
    }

    private String getScoreText() {
        if (!hasScore) {
            return "";
        }
        if (isMate) {
            return (mateMoves > 0) ? ("+M" + mateMoves) : ("-M" + (-mateMoves));
        }
        double pawns = whiteScore / 100.0;
        if (Math.abs(pawns) < 0.005) {
            return "0.00";
        }
        return String.format(Locale.US, "%+.2f", pawns);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cornerRadius = h / 2.0f;
        boundsRect.set(1.0f, 1.0f, w - 1.0f, h - 1.0f);

        clipPath.reset();
        clipPath.addRoundRect(boundsRect, cornerRadius, cornerRadius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);

        // Draw White and Black bars
        if (!flipped) {
            // Normal: White on left, Black on right
            float whiteWidth = w * currentRatio;
            canvas.drawRect(0, 0, whiteWidth, h, whitePaint);
            canvas.drawRect(whiteWidth, 0, w, h, blackPaint);
        } else {
            // Flipped: Black on left, White on right
            float blackWidth = w * (1.0f - currentRatio);
            canvas.drawRect(0, 0, blackWidth, h, blackPaint);
            canvas.drawRect(blackWidth, 0, w, h, whitePaint);
        }

        String text = getScoreText();
        if (!text.isEmpty()) {
            Paint.FontMetrics fm = textDarkPaint.getFontMetrics();
            float textY = (h - fm.bottom - fm.top) / 2.0f;
            float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

            boolean whiteWinning = (isMate && mateMoves > 0) || (!isMate && whiteScore >= 0);
            if (!flipped) {
                // White on left, Black on right
                if (whiteWinning) {
                    textDarkPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(text, padding, textY, textDarkPaint);
                } else {
                    textWhitePaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(text, w - padding, textY, textWhitePaint);
                }
            } else {
                // Black on left, White on right
                if (whiteWinning) {
                    textDarkPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(text, w - padding, textY, textDarkPaint);
                } else {
                    textWhitePaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(text, padding, textY, textWhitePaint);
                }
            }
        }

        canvas.restore();

        // Draw outer rounded border outline
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, borderPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
