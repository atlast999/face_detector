package com.example.facedetector.face.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


public class GraphicsFocusZoneView extends View {

    public static final RectF sInnerRectangle = new RectF(0, 0, 0, 0);

    public static void setInnerRect(RectF rect) {
        synchronized (sInnerRectangle) {
            sInnerRectangle.top = rect.top;
            sInnerRectangle.left = rect.left;
            sInnerRectangle.bottom = rect.bottom;
            sInnerRectangle.right = rect.right;
        }
    }

    private boolean childMeasureDirty = true;
    private final Paint cornerPaint;
    private final Path cornerPath;

    public GraphicsFocusZoneView(Context context) {
        super(context);
    }

    public GraphicsFocusZoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GraphicsFocusZoneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        cornerPaint = new Paint();
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
//        cornerPaint.setStrokeWidth(getResources().getDimensionPixelOffset(R.dimen.space_xsmall));
        cornerPaint.setStyle(Paint.Style.STROKE);
//        cornerPaint.setColor(AppResources.INSTANCE.getColor(R.color.yellow700));
        cornerPath = new Path();
        cornerPath.setFillType(Path.FillType.EVEN_ODD);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (childMeasureDirty) {
            measureElements();
        }
        if (childMeasureDirty) {
            return;
        }
        canvas.drawPath(cornerPath, cornerPaint);
    }

    private void measureElements() {
        childMeasureDirty = true;
        cornerPath.reset();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (width == 0 || height == 0) {
            return;
        }
        int spacing = 60;
        if (width < (spacing << 1) || height < (spacing << 1)) {
            return;
        }
        int radius = (Math.min(width, height) >> 1) - spacing;
        int centerX = width >> 1;
        int centerY = height >> 1;
        int top = centerY - radius;
        int left = centerX - radius;
        int bottom = centerY + radius;
        int right = centerX + radius;
        int cornerStrokeLength = radius >> 1; // 25% of the squared-mask

        RectF innerRect = new RectF(left, top, right, bottom);
        setInnerRect(innerRect);

        //CORNER
        int endLeft = left + cornerStrokeLength;
        int endRight = right - cornerStrokeLength;
        int endTop = top + cornerStrokeLength;
        int endBottom = bottom - cornerStrokeLength;

        //TOP-LEFT
        drawCorner(left, top, endLeft, endTop);
        //TOP-RIGHT
        drawCorner(right, top, endRight, endTop);
        //BOTTOM-RIGHT
        drawCorner(right, bottom, endRight, endBottom);
        //BOTTOM-LEFT
        drawCorner(left, bottom, endLeft, endBottom);
        childMeasureDirty = false;
    }

    private void drawCorner(int startX, int startY, int endX, int endY) {
        cornerPath.moveTo(endX, startY);
        cornerPath.lineTo(startX, startY);
        cornerPath.lineTo(startX, endY);
    }
}
