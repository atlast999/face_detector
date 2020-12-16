package com.example.facedetector.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class GraphicsFocusZoneView extends View {

    public static final RectF sInnerRectangle = new RectF(0, 0, 0, 0);

    public static void setInnerRect(RectF rectF) {
        synchronized (sInnerRectangle) {
            sInnerRectangle.top = rectF.top;
            sInnerRectangle.left = rectF.left;
            sInnerRectangle.bottom = rectF.bottom;
            sInnerRectangle.right = rectF.right;
        }
    }

    private Bitmap bitmap;

    public GraphicsFocusZoneView(Context context) {
        super(context);
    }

    public GraphicsFocusZoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GraphicsFocusZoneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (bitmap == null) {
            createFrameLayout();
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    protected void createFrameLayout() {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(bitmap);

        RectF outerRectangle = new RectF(0, 0, getWidth(), getHeight());

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(android.R.color.white));
        paint.setAlpha(180);
        osCanvas.drawRect(outerRectangle, paint);
        float width = getWidth();
        float height = getHeight();
        float left = width / 14;
        float radius = width * 0.95f;
        float top = (height - radius) / 2;
        int margin = 20;
        RectF innerRect = new RectF(left, top - margin,
                width - left, height - top - margin);
        setInnerRect(innerRect);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        osCanvas.drawOval(innerRect, paint);

    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }
}
