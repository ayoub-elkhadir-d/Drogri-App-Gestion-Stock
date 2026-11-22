package com.octo.drogri.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Custom Zoomable ImageView supporting smooth pinch-to-zoom, double-tap zoom,
 * dragging when zoomed, boundary clamping, and seamless integration with ViewPager2.
 */
public class ZoomImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private float minScale = 1.0f;
    private float maxScale = 5.0f;
    private float currentScale = 1.0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private final PointF lastTouch = new PointF();
    private int viewWidth = 0;
    private int viewHeight = 0;
    private boolean isMatrixInitialized = false;

    public ZoomImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        fitImageToView();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        isMatrixInitialized = false;
        fitImageToView();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        isMatrixInitialized = false;
        fitImageToView();
    }

    public void fitImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null || viewWidth == 0 || viewHeight == 0) return;

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();
        if (dWidth <= 0 || dHeight <= 0) return;

        matrix.reset();

        float scaleX = (float) viewWidth / dWidth;
        float scaleY = (float) viewHeight / dHeight;
        float scale = Math.min(scaleX, scaleY);

        float dx = (viewWidth - (dWidth * scale)) * 0.5f;
        float dy = (viewHeight - (dHeight * scale)) * 0.5f;

        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);

        currentScale = 1.0f;
        setImageMatrix(matrix);
        isMatrixInitialized = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                if (getParent() != null && currentScale > 1.05f) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (scaleDetector.isInProgress()) {
                    break;
                }

                float dx = event.getX() - lastTouch.x;
                float dy = event.getY() - lastTouch.y;

                if (currentScale > 1.05f) {
                    matrix.postTranslate(dx, dy);
                    checkMatrixBounds();
                    setImageMatrix(matrix);

                    // Allow ViewPager swiping only when hitting horizontal edge
                    RectF rect = getImageRect();
                    boolean atLeftEdge = rect.left >= 0 && dx > 0;
                    boolean atRightEdge = rect.right <= viewWidth && dx < 0;

                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(!atLeftEdge && !atRightEdge);
                    }
                } else {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }

                lastTouch.set(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        return true;
    }

    private void checkMatrixBounds() {
        RectF rect = getImageRect();
        float deltaX = 0;
        float deltaY = 0;

        if (rect.width() <= viewWidth) {
            deltaX = (viewWidth - rect.width()) * 0.5f - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
        }

        if (rect.height() <= viewHeight) {
            deltaY = (viewHeight - rect.height()) * 0.5f - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        matrix.postTranslate(deltaX, deltaY);
    }

    private RectF getImageRect() {
        RectF rect = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            if (newScale >= minScale && newScale <= maxScale) {
                currentScale = newScale;
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                checkMatrixBounds();
                setImageMatrix(matrix);
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > 1.2f) {
                fitImageToView();
            } else {
                float targetScale = 2.5f;
                float factor = targetScale / currentScale;
                currentScale = targetScale;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                checkMatrixBounds();
                setImageMatrix(matrix);
            }
            return true;
        }
    }

    // Helper to safely access PointF coordinates
    private static class TouchPointHelper {
        static float getX(PointF p) { return p.x; }
        static float getY(PointF p) { return p.y; }
    }
}
