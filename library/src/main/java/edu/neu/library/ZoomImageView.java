package edu.neu.library;

import android.animation.Animator.AnimatorListener;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
    /**
     * 父viewpager
     */
    private ViewGroup parent;
    /**
     * thisImageView
     */
    private ImageView thisImageView;
    /**
     * 上一次时的matrix
     */
    private Matrix mSaveMatrix;
    /**
     * 当前的matrix
     */
    private Matrix mCurrentMatrix;
    /**
     * 辅助触摸事件
     */
    private GestureDetector mGestureDetector;
    /**
     * 初始化时的matrix
     */
    private Matrix mInitMatrix;
    /**
     * 矩阵的值
     */
    private float[] values = new float[9];

    // 手势模式
    /**
     * 初始化状态
     */
    private static final int NONE = 0;
    /**
     * 拖动模式，在放大的情况下
     */
    private static final int DRAG = 1;
    /**
     * 缩放模式
     */
    private static final int ZOOM = 2;
    /**
     * 双击模式
     */
    private static final int DOUBLECLICK = 4;
    // 缩放模式 PS：NONE表示初始化
    /**
     * 放大状态
     */
    private static final int ENLARGE = 5;
    /**
     * 缩小状态
     */
    private static final int NARROW = 6;
    /**
     * 最大放大倍数
     */
    public static final float MAX_SCALE = 4f;
    /**
     * 最小缩放倍数
     */
    public static final float MIN_SCALE = 0.5f;
    /**
     * 最小拖拽距离
     */
    public static final float MIN_DRAG = 8f;
    /**
     * 点击放大的倍数
     */
    public static final float DOUBLE_CLICK_SCALE = 2f;
    /**
     * 单击事件的延迟时间
     */
    private static final int DELAY_TIME_TO_ONCLICK = 300;
    /**
     * 最小的滑动速度
     */
    private static final int MIN_SCROLL_SPEED = 1000;
    /**
     * 操作的模式
     */
    private int mode = NONE;
    /**
     * 大小模式
     */
    private int sizeMode = NONE;
    /**
     * 图片的宽度
     */
    private float mImageWidth;
    /**
     * 图片的高度
     */
    private float mImageHeight;
    /**
     * view的宽度
     */
    private float mViewWidth;
    /**
     * view的高度
     */
    private float mViewHeight;
    /**
     * 起始点位置
     */
    private PointF startPoint;
    /**
     * 上一次移动的点
     */
    private PointF lastPoint;
    /**
     * 缩放时的缩放点
     */
    private PointF midPoint;
    /**
     * 上一次移动的x轴的量
     */
    private float lastTranX;
    /**
     * 上一次移动的y轴的量
     */
    private float lastTranY;
    /**
     * 判断是否调用过onDraw方法
     */
    private boolean isOnDraw = false;
    /**
     * 缩放前两点的距离
     */
    private float oldDistance;
    /**
     * 用于完成拖拽或缩放后的动画效果
     */
    private ValueAnimator animator;
    /**
     * 在动画中需要平移的x量
     */
    private float tranX;
    /**
     * 在动画中需要平移的y量
     */
    private float tranY;
    /**
     * 在动画中需要缩放的倍数
     */
    private float scale = 1;
    /**
     * 是否在动画中
     */
    private boolean isAnimating;
    /**
     * 是否在缩小中，缩小时无法中断
     */
    private boolean isNarrowing;
    /**
     * 单击时的回调
     */
    private OnSingleClick mOnSingleClick;
    /**
     * 已经拖拽到最右边
     */
    private boolean inRight;
    /**
     * 已经拖拽到最左边
     */
    private boolean inLeft;
    /**
     * 是否可水平滑动
     */
    private boolean ableTranX;
    /**
     * 是否可竖直滑动
     */
    private boolean ableTranY;
    /**
     * 是否拦截左滑动
     */
    private boolean isInterruptLeft = true;

    /**
     * 是否拦截右滑动
     */
    private boolean isInterruptRight = true;
    /**
     * 是否判断屏蔽滑动事件
     */
    private boolean isJudgeInterrputTouch;
    /**
     * 是否放大或还原后
     */
    private boolean isEnlargeOrRestore;
    /**
     * 是否在after方法后
     */
    private boolean isAfter;
    /**
     * 弹性滑动的X速度
     */
    private float scrollSpeedX;
    /**
     * 弹性滑动的Y速度
     */
    private float scrollSpeedY;
    /**
     * 是否已加载完毕
     */
    private boolean isLoaded;
    private static final String TAG = "fiend";

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * xml创建
     *
     * @param context
     * @param attrs
     */
    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * java创建
     *
     * @param context
     */
    public ZoomImageView(Context context) {
        super(context);
        init();
    }

    /**
     * 初始化操作
     */
    private void init() {
        ZoomListener mListenr = new ZoomListener();
        mGestureDetector = new GestureDetector(getContext(), simpleOnGestureListener);
        setOnTouchListener(mListenr);
        mCurrentMatrix = new Matrix();
        mSaveMatrix = new Matrix();
        mInitMatrix = new Matrix();
        midPoint = new PointF();
        startPoint = new PointF();
        lastPoint = new PointF();
        thisImageView = this;
        parent = getParent() instanceof ViewGroup ? (ViewGroup) getParent() : null;
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.MATRIX);
    }

    /**
     * 初始化图片位置
     */
    private void initImage() {
        // 将图片缩放到宽度与view的宽度相同，平移到view的中心
        float width = getWidth();
        float height = getHeight();
        float[] values = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
        mCurrentMatrix.setValues(values);
        getWidthAndHeight(mCurrentMatrix);
        // 没有获得宽高的时候不进行初始化
        if (mImageHeight == 0 || mImageWidth == 0 || mViewHeight == 0 || mViewWidth == 0) {
            return;
        }
        // 初始化时宽和控件的宽相同
        float scaleX = width / mImageWidth;
        mCurrentMatrix.postScale(scaleX, scaleX);
        getWidthAndHeight(mCurrentMatrix);
        if (mImageHeight <= mViewHeight) {// 只有在缩放后图片的高度小于view的高度才需要向下平移
            // 平移,因为宽已经和view的宽一样，所以只需要向下平移
            float tranY = (height - mImageHeight) / 2;
            mCurrentMatrix.postTranslate(0, tranY);
            mode = NONE;
        } else {
            mode = DRAG;
            ableTranY = true;
        }
        setImageMatrix(mCurrentMatrix);
        mInitMatrix.set(mCurrentMatrix);
        isLoaded = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isOnDraw) {
            init();
            initImage();
            isOnDraw = true;
        }
    }

    /**
     * 重写setImageBitmap方法，获取图片的matrix
     */
    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        isLoaded = false;
        init();
        initImage();
    }

    /**
     * 获取图片的宽高
     */
    private void getWidthAndHeight(Matrix matrix) {
        BitmapDrawable bd = (BitmapDrawable) getDrawable();
        if (bd == null) {
            return;
        }
        Bitmap bm = bd.getBitmap();
        if (bm == null) {
            return;
        }
        float[] values = new float[9];
        matrix.getValues(values);
        // 获取图片的宽和高，
        mImageWidth = bm.getWidth() * values[Matrix.MSCALE_X];
        mImageHeight = bm.getHeight() * values[Matrix.MSCALE_X];
        // 获取view的宽高
        mViewWidth = getWidth();
        mViewHeight = getHeight();

        // 获得拖动参数
        if (mImageWidth > mViewWidth) {
            ableTranX = true;
        } else {
            ableTranX = false;
        }
        if (mImageHeight > mViewHeight) {
            ableTranY = true;
        } else {
            ableTranY = false;
        }
    }

    /**
     * 设置父viewGroup
     *
     * @param viewGroup
     */
    public void setParent(ViewGroup viewGroup) {
        parent = viewGroup;
    }

    /**
     * 滑动，快速拖拽后放手发动
     *
     * @return
     */
    private boolean scroll(float xVelocity, float yVelocity) {
        if (!isLoaded) {
            return false;
        }
        if (Math.abs(xVelocity) >= MIN_SCROLL_SPEED || Math.abs(yVelocity) >= MIN_SCROLL_SPEED) {
            if (Math.abs(xVelocity) > Math.abs(yVelocity)) {
                if (Math.abs(xVelocity) >= MIN_SCROLL_SPEED) {
                    scrollSpeedX = xVelocity;
                    scrollSpeedY = 0;
                    tranX = 0;
                    tranY = 0;
                }
            } else {
                if (Math.abs(yVelocity) >= MIN_SCROLL_SPEED) {
                    scrollSpeedX = 0;
                    scrollSpeedY = yVelocity;
                    tranY = 0;
                    tranX = 0;
                }
            }
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            mSaveMatrix.set(mCurrentMatrix);
            getWidthAndHeight(mCurrentMatrix);
            animator = ValueAnimator.ofFloat(1, 0f);
            animator.setDuration(1000);
            animator.addUpdateListener(mScrollAnimatorListener);
            animator.addListener(listener);
            animator.start();
            isAnimating = true;
            return true;
        }
        return false;
    }

    /**
     * 滑动动画结束
     */
    AnimatorListener listener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            isAnimating = false;
            tranX = 0;
            tranY = 0;
            scale = 1;
            after();// 结束时
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }
    };

    /**
     * 滑动时的动画器
     */
    private AnimatorUpdateListener mScrollAnimatorListener = new AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            Float value = (Float) animation.getAnimatedValue();
            float nowSpeedX = value * scrollSpeedX;
            float nowSpeedY = value * scrollSpeedY;
            tranX = tranX + nowSpeedX * 0.01f;
            tranY = tranY + nowSpeedY * 0.01f;
            Log.d("fiend", "tranX:" + tranX);
            if (!ableTranX) {
                tranX = 0;
            }
            if (!ableTranY) {
                tranY = 0;
            }
            mCurrentMatrix.set(mSaveMatrix);
            mCurrentMatrix.postTranslate(tranX, tranY);
            setImageMatrix(mCurrentMatrix);
            getWidthAndHeight(mCurrentMatrix);
            mCurrentMatrix.getValues(values);
            float mTranX = values[Matrix.MTRANS_X];
            float mTranY = values[Matrix.MTRANS_Y];
            if (scrollSpeedX != 0) {
                if (mTranX > 0 || Math.abs(mTranX) > (mImageWidth - mViewWidth)) {
                    Log.d("fiend", "tranX:" + tranX);
                    Log.d("fiend", "结束");
                    if (animation.isRunning()) {
                        animation.cancel();
                        after();// 结束时
                    }
                }
            }
            if (scrollSpeedY != 0) {
                if (mTranY > 0 || Math.abs(mTranY) > (mImageHeight - mViewHeight)) {
                    if (animation.isRunning()) {
                        animation.cancel();
                        after();// 结束时
                    }
                }
            }
        }

    };

    /**
     * 还原到未缩放和拖拽的大小
     */
    public void restore() {
        if (!isLoaded) {
            return;
        }
        if (sizeMode == ENLARGE) {// 只有在放大的情况下才能还原
            mCurrentMatrix.getValues(values);
            getWidthAndHeight(mCurrentMatrix);
            isEnlargeOrRestore = true;
            // 当前的偏移和缩放倍数
            float mCurrentTranX = values[Matrix.MTRANS_X];
            float mCurrentTranY = values[Matrix.MTRANS_Y];
            float mCurrentScale = values[Matrix.MSCALE_X];
            mInitMatrix.getValues(values);
            // 目标偏移和缩放
            float mTargetTranX = values[Matrix.MTRANS_X];
            float mTargetTranY = values[Matrix.MTRANS_Y];
            float mTargetScale = values[Matrix.MSCALE_X];
            scale = mTargetScale / mCurrentScale;
            tranX = mTargetTranX - mCurrentTranX;
            tranY = mTargetTranY - mCurrentTranY;
            // startAnimator();
            startAnimator(tranX, tranY, scale, new OnAnimatorEnd() {

                @Override
                public void onAnimatorEnd() {
//                    interruptParent();// 返回原大后可以执行viewpager的操作
                }
            });
        }
    }

    /**
     * 放大到2倍
     */
    private void enlarge() {
        if (!isLoaded) {
            return;
        }
        if (mode == NONE) {
            isEnlargeOrRestore = true;
            getWidthAndHeight(mCurrentMatrix);
            scale = DOUBLE_CLICK_SCALE;
            float imageWidth = mImageWidth * DOUBLE_CLICK_SCALE;
            float imageHeight = mImageHeight * DOUBLE_CLICK_SCALE;
            tranX = -imageWidth / 4;
            tranY = -imageHeight / 4;
            // startAnimator();
            startAnimator(tranX, tranY, scale, new OnAnimatorEnd() {

                @Override
                public void onAnimatorEnd() {
//                    notInterruptParent();
                    mode = ENLARGE;
                }
            });
        }
    }

    /**
     * 在缩放后的动画效果
     */
    public void after() {
        if (!isLoaded) {
            return;
        }
        mCurrentMatrix.getValues(values);
        float mTranX = values[Matrix.MTRANS_X];
        float mTranY = values[Matrix.MTRANS_Y];
        float mScale = values[Matrix.MSCALE_X];
        getWidthAndHeight(mCurrentMatrix);
        float imageWidth = mImageWidth;
        float imageHeight = mImageHeight;
        // 对缩放处理
        if (mScale < getNolmalScale()) {
            scale = getNolmalScale() / mScale;
            imageWidth = mImageWidth * scale;
            imageHeight = mImageHeight * scale;
            isNarrowing = true;
        } else {
            scale = 1;
        }
        // 对X轴移动处理
        if (mTranX > 0) {
            tranX = -mTranX;// 反方向
        } else if (Math.abs(mTranX) > imageWidth - mViewWidth) {
            tranX = -(imageWidth - Math.abs(mTranX) - mViewWidth);
        }
        float targetY = 0f;
        // 对Y轴移动处理
        if (imageHeight > mViewHeight) {
            if (mTranY > 0) {
                tranY = -mTranY;// 反方向
            } else if (Math.abs(mTranY) > imageHeight - mViewHeight) {
                tranY = -(imageHeight - Math.abs(mTranY) - mViewHeight);
            }
        } else if (imageHeight < mViewHeight) {
            targetY = mViewHeight / 2 - imageHeight / 2;
            tranY = targetY - mTranY;
        } else {

        }
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        isAfter = true;
        // startAnimator();
        startAnimator(tranX, tranY, scale, new OnAnimatorEnd() {

            @Override
            public void onAnimatorEnd() {
                getSizeMode();
//                if (sizeMode == NONE) {
//                    interruptParent();
//                } else if (sizeMode == ENLARGE) {
//                    if (isJudgeInterrputTouch) {
//                        interruptParent();
//                    } else {
//                        notInterruptParent();
//                    }
//                }
//                isEnlargeOrRestore = false;
            }
        });
    }

    /**
     * 启动动画
     *
     * @param tranX
     * @param tranY
     * @param scale
     * @param end
     */
    private void startAnimator(float tranX, float tranY, float scale, OnAnimatorEnd end) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        mSaveMatrix.set(mCurrentMatrix);
        animator = ValueAnimator.ofFloat(1);
        animator.setDuration(300);
        animator.addUpdateListener(new MyAnimatorUpdateListener(tranX, tranY, scale, end, this));
        animator.start();
        isAnimating = true;
    }

    /**
     * 执行动画效果
     */
    static class MyAnimatorUpdateListener implements AnimatorUpdateListener {
        private float mTranX;
        private float mTranY;
        private float mScale;
        private OnAnimatorEnd end;
        ZoomImageView mZoomImageView;

        public MyAnimatorUpdateListener(float tranX, float tranY, float scale, OnAnimatorEnd end, ZoomImageView zoomImageView) {
            this.mTranX = tranX;
            this.mTranY = tranY;
            this.mScale = scale;
            this.end = end;
            mZoomImageView = zoomImageView;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            Float value = (Float) animation.getAnimatedValue();
            mZoomImageView.mCurrentMatrix.set(mZoomImageView.mSaveMatrix);
            mZoomImageView.getInitPoint(mZoomImageView.midPoint);
            if (mScale != 1) {
                float scale = 1 + (mScale - 1) * value;
                mZoomImageView.mCurrentMatrix.postScale(scale, scale, mZoomImageView.midPoint.x, mZoomImageView.midPoint.y);
            }
            mZoomImageView.mCurrentMatrix.postTranslate(mTranX * value, mTranY * value);
            mZoomImageView.setImageMatrix(mZoomImageView.mCurrentMatrix);
            if (value == 1f) {// 动画结束
                mZoomImageView.isAnimating = false;
                mZoomImageView.isNarrowing = false;
                mZoomImageView.tranX = 0;
                mZoomImageView.tranY = 0;
                mZoomImageView.scale = 1;
                if (end != null) {
                    end.onAnimatorEnd();
                    mZoomImageView = null;
                }
            }
        }

    }

    /**
     * 动画结束时执行
     *
     * @author lihanguang
     */
    public interface OnAnimatorEnd {
        void onAnimatorEnd();
    }

//    AnimatorUpdateListener mAnimationListener = new AnimatorUpdateListener() {
//
//        @Override
//        public void onAnimationUpdate(ValueAnimator animation) {
//            Float value = (Float) animation.getAnimatedValue();
//            mCurrentMatrix.set(mSaveMatrix);
//            getInitPoint(midPoint);
//            if (scale != 1) {
//                float mScale = 1 + (scale - 1) * value;
//                mCurrentMatrix.postScale(mScale, mScale, midPoint.x, midPoint.y);
//            }
//            mCurrentMatrix.postTranslate(tranX * value, tranY * value);
//            setImageMatrix(mCurrentMatrix);
//            if (value == 1f) {// 动画结束
//                isAnimating = false;
//                isNarrowing = false;
//                tranX = 0;
//                tranY = 0;
//                scale = 1;
//                getSizeMode();
//                if (sizeMode == NONE) {
//                    interruptParent();
//                } else {
//                    if (isJudgeInterrputTouch && !isEnlargeOrRestore) {
//                        interruptParent();
//                    } else {
//                        notInterruptParent();
//                    }
//                }
//                isEnlargeOrRestore = false;
//            }
//        }
//
//    };

    /**
     * 拖动图片,进入拖拽模式，水平必定可拖动，竖直需要判断
     *
     * @param event
     */
    private void dragImage(MotionEvent event) {
        float currentX = event.getX();// 获取当前的x坐标
        float currentY = event.getY();// 获取当前的Y坐标
        float tranX = currentX - startPoint.x;// 移动的x轴距离
        float tranY = currentY - startPoint.y;// 移动的y轴距离
        // 判断是否达到滑动条件
        if (Math.abs(tranX - lastTranX) < MIN_DRAG && Math.abs(tranY - lastTranY) < MIN_DRAG) {
            tranX = lastTranX;
            tranY = lastTranY;
        }
        // 判断是否能够拖动
        if (!ableTranY) {
            tranY = 0;
        }
        if (!ableTranX) {
            tranX = 0;
        }
        mCurrentMatrix.postTranslate(tranX, tranY);
        setImageMatrix(mCurrentMatrix);
        // 判断是否拦截事件，当拖动到边界的时候不拦截
        getWidthAndHeight(mCurrentMatrix);
        mCurrentMatrix.getValues(values);
        float mTranX = values[Matrix.MTRANS_X];
        boolean ableToTurnLeft = mTranX > 3 && isInterruptLeft;
        boolean ableToTurnRight = Math.abs(mTranX) > Math.abs(mImageWidth - mViewWidth) + 3 && isInterruptRight;
        if (ableToTurnLeft || ableToTurnRight) {
            setInterrupt(false);
            isJudgeInterrputTouch = true;
            Log.d("fiend2", "dragImage-interruptParent");
        } else {
//            setInterrupt(true);
            isJudgeInterrputTouch = false;
            Log.d("fiend2", "dragImage-notInterruptParent");
        }

        // 记录上一次移动的位置
        lastTranX = tranX;
        lastTranY = tranY;
    }

    /**
     * 缩放图片
     *
     * @param event
     */
    private void zoomImage(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return;
        }
        Log.d(TAG, "zoomImage");
        float newDistance = spacing(event);// 获取新的距离
        float scale = newDistance / oldDistance;// 缩放比例
        scale = getScaleType(scale);// 获取缩放的类型
        midPoint(midPoint, event);
        mCurrentMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
        setImageMatrix(mCurrentMatrix);
        getSizeMode();// 获得大小模式
    }

    /**
     * 判断是否可以缩放，超过最大缩放值不可缩放
     *
     * @return scale
     */
    private float getScaleType(float scale) {
        float[] values = new float[9];
        mCurrentMatrix.getValues(values);
        float nextScale = scale * values[Matrix.MSCALE_X];
        if (nextScale >= getMaxScale()) {
            return getMaxScale() / values[Matrix.MSCALE_X];
        } else if (nextScale <= getMinScale()) {
            return getMinScale() / values[Matrix.MSCALE_X];
        }
        return scale;
    }

    /**
     * 计算两点的距离
     *
     * @param event
     * @return
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算缩放中点
     *
     * @param point
     * @param event
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * 获取最大的放大倍数，值为初始化后宽度的一倍
     *
     * @return
     */
    private float getMaxScale() {
        float[] values = new float[9];
        mInitMatrix.getValues(values);
        return MAX_SCALE * values[Matrix.MSCALE_X];
    }

    /**
     * 获取最小的放大倍数，值为初始化后宽度的0.5倍
     *
     * @return
     */
    private float getMinScale() {
        float[] values = new float[9];
        mInitMatrix.getValues(values);
        return MIN_SCALE * values[Matrix.MSCALE_X];
    }

    /**
     * 获取初始化时候的缩放倍数
     *
     * @return
     */
    private float getNolmalScale() {
        float[] values = new float[9];
        mInitMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /**
     * 获得大小状态
     *
     * @return
     */
    private void getSizeMode() {
        float[] values = new float[9];
        mCurrentMatrix.getValues(values);
        getWidthAndHeight(mCurrentMatrix);
        float currentScale = values[Matrix.MSCALE_X];
        mInitMatrix.getValues(values);
        float initScale = values[Matrix.MSCALE_X];
        if (currentScale > initScale) {
            sizeMode = ENLARGE;
//			ableTranX = true;
        } else if (currentScale < initScale) {
            sizeMode = NARROW;
//			ableTranX = false;
//			ableTranY = false;
        } else {
            sizeMode = NONE;
//			ableTranX = false;
        }

        // 判断是否可以滑动
        if (mImageHeight > mViewHeight) {
            ableTranY = true;
        } else {
            ableTranY = false;
        }

        if (mImageWidth > mViewWidth) {
            ableTranX = true;
        } else {
            ableTranX = false;
        }
    }

    private void getInitPoint(PointF pointf) {
        float[] values = new float[9];
        mCurrentMatrix.getValues(values);
        pointf.x = values[Matrix.MTRANS_X];
        pointf.y = values[Matrix.MTRANS_Y];
    }

    /**
     * 判断是否在最右边或最左边
     */
    private void isInRightOrLeft() {
        float[] values = new float[9];
        mCurrentMatrix.getValues(values);
        float mTranX = values[Matrix.MTRANS_X];
        getWidthAndHeight(mCurrentMatrix);
        if (mTranX >= 0) {
            inLeft = true;
        } else {
            inLeft = false;
        }
        if (Math.abs(mTranX) > mImageWidth - mViewWidth) {
            inRight = true;
        } else {
            inRight = false;
        }
    }

    /**
     * 设置是否拦截左滑动
     *
     * @param interrupt
     */
    public void setInterruptLeft(boolean interrupt) {
        isInterruptLeft = interrupt;
    }

    /**
     * 设置是否拦截右滑动
     *
     * @param interrupt
     */
    public void setInterruptRight(boolean interrupt) {
        isInterruptRight = interrupt;
    }

    /**
     * 设置单击事件
     *
     * @param onSingleClick
     */
    public void setOnSingleClick(OnSingleClick onSingleClick) {
        mOnSingleClick = onSingleClick;
    }

    /**
     * @param isInterrupt true时父View不拦截事件，false时View可以拦截事件
     */
    private void setInterrupt(boolean isInterrupt) {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(isInterrupt);
        }
    }


    /**
     * 单击监听器
     *
     * @author lihanguang
     */
    public interface OnSingleClick {
        void onClick(View v);
    }

    /**
     * 单击事件的延迟执行
     */
    private Runnable delayOnClick = new Runnable() {

        @Override
        public void run() {
            if (mOnSingleClick != null) {
                mOnSingleClick.onClick(thisImageView);
            }

        }
    };

    /**
     * 触摸事件 处理手势
     *
     * @author lihanguang
     */
    private class ZoomListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    getSizeMode();
                    oldDistance = spacing(event);
                    Log.d(TAG, "oldDistance:" + oldDistance);
                    if (oldDistance > 10f) {
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == ZOOM) {// 缩放模式
                        Log.d(TAG, "mode:" + mode);
                        mCurrentMatrix.set(mSaveMatrix);
                        zoomImage(event);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // 拖拽模式
                    if (mode == DRAG) {
                        after();
                        mode = NONE;
                        // 缩放模式
                    } else if (mode == ZOOM) {
                        after();
                        mode = NONE;
                    } else if (mode == DOUBLECLICK) {
                        mode = NONE;
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerCount() == 2) {
                        after();
                    }
                    break;
                default:
                    break;
            }
            boolean consume = mGestureDetector.onTouchEvent(event);
            return true;
        }

    }

    /**
     * GestureDetector的事件
     *
     * @param e
     * @return
     */
    SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

        /**
         * 第一次按下
         */
        public boolean onDown(MotionEvent e) {
            getSizeMode();
            Log.d(TAG, "mode:" + mode);
            setInterrupt(true);
            if (sizeMode == NONE && !ableTranY) {
                setInterrupt(false);
            }
            if (isAnimating) {
                if (isNarrowing || mode == DOUBLECLICK) {
                    return true;
                } else {
                    animator.cancel();
                }
            }
            mSaveMatrix.set(mCurrentMatrix);
            getSizeMode();
            if (sizeMode == ENLARGE || ableTranY) {// 放大或Y轴可拖动
                mode = DRAG;// 单点进入拖动模式
                startPoint.set(e.getX(), e.getY());
                lastPoint.set(e.getX(), e.getY());
            }
            return true;
        }

        /**
         * 处理拖动
         */
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mode == DRAG) {// 拖动模式
                mCurrentMatrix.set(mSaveMatrix);
                dragImage(e2);
            }
            return true;
        }

        /**
         * 滑动
         */
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            scroll(velocityX, velocityY);
            return true;
        }

        /**
         * 双击事件
         */
        public boolean onDoubleTap(MotionEvent e) {
            if (sizeMode == ENLARGE) {
                restore();
            } else if (sizeMode == NONE) {
                enlarge();
            }
            removeCallbacks(delayOnClick);
            mode = DOUBLECLICK;
            return true;
        }

        /**
         * 单击事件
         */
        public boolean onSingleTapUp(MotionEvent e) {
            postDelayed(delayOnClick, DELAY_TIME_TO_ONCLICK);
            return true;
        }

    };

}
