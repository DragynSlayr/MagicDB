package github.dragynslayr.magicdb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Overlay extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private String scanned;
    private boolean foundText;
    private DisplayMetrics dimensions;

    public Overlay(Context context) {
        super(context);
        init();
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setZOrderOnTop(true);
        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        dimensions = getResources().getDisplayMetrics();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setWillNotDraw(false);
        draw(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void update(String scanned) {
        this.scanned = scanned;
        foundText = true;
        invalidate();
    }

    public void reset() {
        scanned = "";
        foundText = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        draw(holder);
    }

    private void draw(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        if (foundText) {
            paint.setColor(Color.GREEN);
        } else {
            paint.setColor(Color.RED);
        }
        paint.setStrokeWidth(10);

        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;

        int left = (int) (width * 0.1);
        int right = width - left;
        int top = (int) (height * 0.075);
        int bottom = height - top;

        Rect rect = new Rect(left, top, right, bottom);
        canvas.drawRect(rect, paint);

        Paint textPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setStrokeWidth(2);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(45);
        int xPos = (int) ((width / 2) - (textPaint.measureText(scanned) / 2));
        canvas.drawText(scanned, xPos, 150, textPaint);

        holder.unlockCanvasAndPost(canvas);
    }
}
