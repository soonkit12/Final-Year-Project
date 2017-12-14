package com.hetnet.milocsample;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class ScaleImageView extends View {

    // view
    private float contentWidth = 0f;
    private float contentHeight = 0f;
    private Paint paint = null;
    private Vibrator vibrator = null;

    // image
    private Bitmap image = null;
    private Matrix dispMatrix = new Matrix();
    protected float[] transValue = new float[9];  // must sync with dispMatrix
    private float maxScale = 0f;
    private float minScale = 0f;
    private float defMaxScale = 0f;

    // point
    private Bitmap pointImage = null;
    private PointF pointImageCenter = null;
    private ArrayList<PointF> arrPoint = null;
    private Bitmap pointSelectImage = null;
    private PointF pointSelectImageCenter = null;
    private PointF pointSelect = null;

    // operations
    private final float RESOLUTION = 10f;
    private boolean isViewMode = false;
    private boolean isCenterSelect = false;
    private enum OpType {NONE, DRAG_IMAGE, ZOOM_IMAGE, DRAG_POINT_NEW, DRAG_POINT_OLD}
    private OpType opType = OpType.NONE;
    private Matrix refMatrix = new Matrix();
    private PointF refPoint = new PointF();
    private float refDist = 0f;
    private GestureDetector gestureDetector = null;
    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    pointSelect = new PointF(
                            (e.getX() - transValue[Matrix.MTRANS_X]) / transValue[Matrix.MSCALE_X],
                            (e.getY() - transValue[Matrix.MTRANS_Y]) / transValue[Matrix.MSCALE_Y]
                    );
                    boolean isOld = false;
                    for (PointF point : arrPoint) {
                        if (Math.abs(point.x - pointSelect.x) <= RESOLUTION &&
                                Math.abs(point.y - pointSelect.y) <= RESOLUTION) {
                            isOld = true;
                            pointSelect.set(point);
                            arrPoint.remove(point);
                            break;
                        }
                    }
                    refPoint.set(pointSelect);
                    if (!isOld) {
                        opType = OpType.DRAG_POINT_NEW;
                    } else {
                        opType = OpType.DRAG_POINT_OLD;
                    }
                    vibrator.vibrate(100);
                    invalidate();
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    pointSelect = new PointF(
                            (e.getX() - transValue[Matrix.MTRANS_X]) / transValue[Matrix.MSCALE_X],
                            (e.getY() - transValue[Matrix.MTRANS_Y]) / transValue[Matrix.MSCALE_Y]
                    );
                    boolean isOld = false;
                    for (PointF point : arrPoint) {
                        if (Math.abs(point.x - pointSelect.x) <= RESOLUTION &&
                                Math.abs(point.y - pointSelect.y) <= RESOLUTION) {
                            isOld = true;
                            pointSelect.set(point);
                            break;
                        }
                    }
                    if (!isOld) {
                        pointSelect = null;
                    }
                    notifyPointSelected(pointSelect);
                    invalidate();
                    return true;
                }
            };

    public ScaleImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ScaleImageView, defStyle, 0);

        Resources res = getResources();
        pointImage = BitmapFactory.decodeResource(res,
                a.getResourceId(R.styleable.ScaleImageView_normalPointSrc, R.drawable.green_dot));
        pointImageCenter = new PointF(pointImage.getWidth() / 2f, pointImage.getHeight() / 2f);
        pointSelectImage = BitmapFactory.decodeResource(res,
                a.getResourceId(R.styleable.ScaleImageView_selectPointSrc, R.drawable.red_dot));
        pointSelectImageCenter = new PointF(pointSelectImage.getWidth() / 2f, pointSelectImage.getHeight() / 2f);
        defMaxScale = a.getFloat(R.styleable.ScaleImageView_maxScale, 2f);

        a.recycle();

        paint = new Paint();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        arrPoint = new ArrayList<>();

        gestureDetector = new GestureDetector(context, simpleOnGestureListener);
        gestureDetector.setOnDoubleTapListener(simpleOnGestureListener);
        gestureDetector.setIsLongpressEnabled(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        contentWidth = w;
        contentHeight = h;
//        adjustScale();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (image == null) {
            return;
        }

        if (isCenterSelect && (pointSelect != null)) {
            float newScale = transValue[Matrix.MSCALE_X];
            float newCenterX = pointSelect.x * newScale;
            float newCenterY = pointSelect.y * newScale;
            transValue[Matrix.MTRANS_X] =  contentWidth / 2f - newCenterX;
            transValue[Matrix.MTRANS_Y] =  contentHeight / 2f - newCenterY;
            dispMatrix.setValues(transValue);
            adjustImage();
        }

        canvas.drawBitmap(image, dispMatrix, paint);

        for (PointF point : arrPoint) {
            canvas.drawBitmap(pointImage,
                    transValue[Matrix.MTRANS_X] + point.x * transValue[Matrix.MSCALE_X] - pointImageCenter.x,
                    transValue[Matrix.MTRANS_Y] + point.y * transValue[Matrix.MSCALE_Y] - pointImageCenter.y,
                    paint
            );
        }

        if (pointSelect != null) {
            canvas.drawBitmap(pointSelectImage,
                    transValue[Matrix.MTRANS_X] + pointSelect.x * transValue[Matrix.MSCALE_X] - pointSelectImageCenter.x,
                    transValue[Matrix.MTRANS_Y] + pointSelect.y * transValue[Matrix.MSCALE_Y] - pointSelectImageCenter.y,
                    paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // this order not good
//        boolean retVal = dragZoom(event);
//        retVal = retVal || gestureDetector.onTouchEvent(event);
        // this order good
        boolean retVal = false;
        if (!isViewMode)
            retVal = gestureDetector.onTouchEvent(event);
        retVal = retVal || dragZoom(event);
        retVal = retVal || super.onTouchEvent(event);
        return retVal;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
        adjustScale();
//        dispMatrix.postScale(minScale, minScale);
        dispMatrix.getValues(transValue);
        adjustImage();
        invalidate();
    }

    private void adjustScale() {
        if (image == null)
            return;

        minScale = Math.min(contentWidth / image.getWidth(), contentHeight / image.getHeight());
        maxScale = defMaxScale * minScale;
    }

    private void adjustImage() {
        if (image == null)
            return;

        float newScale = transValue[Matrix.MSCALE_X];
        newScale = Math.min(maxScale, newScale);
        newScale = Math.max(minScale, newScale);

        transValue[Matrix.MSCALE_X] = newScale;
        transValue[Matrix.MSCALE_Y] = newScale;

        float newTransX = transValue[Matrix.MTRANS_X];
        float newWidth = image.getWidth() * newScale;
        if (newWidth <= contentWidth) {
            newTransX = (contentWidth - newWidth) / 2f;
        } else {
            newTransX = Math.min(newTransX, 0f);
            newTransX = Math.max(newTransX, contentWidth - newWidth);
        }
        transValue[Matrix.MTRANS_X] = newTransX;

        float newTransY = transValue[Matrix.MTRANS_Y];
        float newHeight = image.getHeight() * newScale;
        if (newHeight <= contentHeight) {
            newTransY = (contentHeight - newHeight) / 2f;
        } else {
            newTransY = Math.min(newTransY, 0f);
            newTransY = Math.max(newTransY, contentHeight - newHeight);
        }
        transValue[Matrix.MTRANS_Y] = newTransY;

        dispMatrix.setValues(transValue);
    }

    private boolean dragZoom(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                refMatrix.set(dispMatrix);
                refPoint.set(event.getX(), event.getY());
                opType = OpType.DRAG_IMAGE;
                setViewPointSelect(false);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (opType == OpType.NONE || opType == OpType.DRAG_IMAGE) {
                    refDist = calcDistance(event);
                    if (refDist > RESOLUTION) {
                        refMatrix.set(dispMatrix);
                        calcMidPoint(refPoint, event);
                        opType = OpType.ZOOM_IMAGE;
                        setViewPointSelect(false);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (opType == OpType.DRAG_IMAGE) {
                    dispMatrix.set(refMatrix);
                    dispMatrix.postTranslate(event.getX() - refPoint.x, event.getY() - refPoint.y);
                    dispMatrix.getValues(transValue);
                    invalidate();
                } else if (opType == OpType.ZOOM_IMAGE) {
                    float newDist = calcDistance(event);
                    if (newDist > RESOLUTION) {
                        dispMatrix.set(refMatrix);
                        float newScale = newDist / refDist;
                        dispMatrix.postScale(newScale, newScale, refPoint.x, refPoint.y);
                        dispMatrix.getValues(transValue);
                        invalidate();
                    }
                } else if (opType == OpType.DRAG_POINT_NEW || opType == OpType.DRAG_POINT_OLD) {
                    pointSelect.set(
                            (event.getX() - transValue[Matrix.MTRANS_X]) / transValue[Matrix.MSCALE_X],
                            (event.getY() - transValue[Matrix.MTRANS_Y]) / transValue[Matrix.MSCALE_Y]
                    );
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (opType == OpType.DRAG_IMAGE || opType == OpType.ZOOM_IMAGE) {
                    adjustImage();
                    invalidate();
                } else if (opType == OpType.DRAG_POINT_NEW) {
                    adjustPointSelect(refPoint);
                    invalidate();
                    notifyPointAdded(pointSelect);
                    notifyPointSelected(pointSelect);
                } else if (opType == OpType.DRAG_POINT_OLD) {
                    adjustPointSelect(refPoint);
                    invalidate();
                    if (refPoint.x != pointSelect.x || refPoint.y != pointSelect.y)
                        notifyPointMoved(refPoint, pointSelect);
                    notifyPointSelected(pointSelect);
                }
                opType = OpType.NONE;
                break;
        }

        return true;
    }

    private float calcDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.hypot(x, y);  // sqrt(x^2 + y^2)
    }

    private void calcMidPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2f, y / 2f);
    }

    private void adjustPointSelect(PointF original) {
        for (PointF point : arrPoint) {
            if (Math.abs(point.x - pointSelect.x) <= RESOLUTION &&
                    Math.abs(point.y - pointSelect.y) <= RESOLUTION) {
                pointSelect.set(original);
                break;
            }
        }
        arrPoint.add(pointSelect);
    }

    public void setPoint(Iterable<PointF> iterable) {
        arrPoint.clear();
        for (PointF point : iterable) {
            arrPoint.add(point);
        }
        invalidate();
    }

    public void setPointSelect(PointF point) {
//        if (point == null)
//            pointSelect = null;
//        else
//            pointSelect = new PointF(point.x, point.y);
        pointSelect = point;
        invalidate();
    }

    public void setViewMode(boolean isViewMode) {
        this.isViewMode = isViewMode;
    }

    public void setViewPointSelect(boolean isCenterSelect) {
        this.isCenterSelect = isCenterSelect;
    }

    public interface IPointObserver {

        void pointSelected(PointF point);

        void pointAdded(PointF point);

        void pointMoved(PointF pointPrev, PointF pointCurr);

    }

    private ArrayList<IPointObserver> listObserver = new ArrayList<>();

    public void addObserver(IPointObserver o) {
        listObserver.add(o);
    }

    public void removeObserver(IPointObserver o) {
        listObserver.remove(o);
    }

    private void notifyPointSelected(PointF point) {
        for (IPointObserver o : listObserver)
            o.pointSelected(point);
    }

    private void notifyPointAdded(PointF point) {
        for (IPointObserver o : listObserver)
            o.pointAdded(point);
    }

    private void notifyPointMoved(PointF pointPrev, PointF pointCurr) {
        for (IPointObserver o : listObserver)
            o.pointMoved(pointPrev, pointCurr);
    }
}
