package cn.ac.iscas.xlab.droidfacedog.custom_views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import cn.ac.iscas.xlab.droidfacedog.R;

/**
 * Created by lisongting on 2017/7/31.
 */

public class WaveView extends View {

    public static final String TAG = "WaverView";
    public static final String START_TALK = "开启语音对话";
    public static final String STOP_TALK = "关闭语音对话";
    private boolean isWorking = false;
    private int mColor,mLastColor;
    private float mRadius;
    private int mWidth,mHeight;
    private float centerX,centerY;
    private int textWidth,textHeight;
    private float outCircleRadius;
    private int outCircleAlpha = 255;
    private float accelerate;

    private Paint mPaint;
    private Shader shader;
    private ValueAnimator radiusAnimator;
    private ValueAnimator alphaAnimator;
    private ValueAnimator accelerateAnimator;

    public WaveView(Context context) {
        this(context,null);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WaveView, defStyleAttr, 0);
        int n = typedArray.getIndexCount();
        for(int i=0;i<n;i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {
                case R.styleable.WaveView_inner_color:
                    mColor = typedArray.getColor(attr, Color.BLUE);
                    //mLastColor保存在XML文件中定义好的颜色
                    mLastColor = mColor;
                    break;
                default:
                    break;
            }
        }
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        //测量文字的高度，以便居中放置
        Rect bound = new Rect();
        mPaint.getTextBounds(START_TALK, 0, START_TALK.length(), bound);
        textHeight = bound.height();

        typedArray.recycle();
    }

    @Override
    public void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width,height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            //wrap_content的时候，widthMode为MeasureSpec.AT_MOST.
            width = widthSize / 2;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;

        } else {
            height = heightSize / 2;
        }

        //将width和height设置为一样的，形成一个正方形区域
        width = width <= height ? width : height;
        height = width <= height ? width : height;

        mWidth = width;
        mHeight = height;

        centerX = mWidth / 2;
        centerY = mHeight / 2;
        mRadius = (mWidth-getPaddingLeft()-getPaddingRight()) / 2F;

        Log.i(TAG, "PaddingLeft:" + getPaddingLeft() + ",PaddingRight:" + getPaddingRight());
        Log.i(TAG, "onMeasure:" + width + "x" + height);

        shader = new RadialGradient(centerX, centerY, mWidth / 2, mColor,Color.WHITE, Shader.TileMode.CLAMP);
        setMeasuredDimension(mWidth, mHeight);

    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColor);
        mPaint.setAlpha(255);
        mPaint.setShader(shader);
        canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mPaint);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(40);
        mPaint.setShader(null);
        if (!isWorking) {
            canvas.drawText(START_TALK, centerX - mPaint.measureText(START_TALK) / 2, centerY + textHeight / 2, mPaint);
        } else {
            canvas.drawText(STOP_TALK, centerX - mPaint.measureText(STOP_TALK) / 2, centerY + textHeight / 2, mPaint);
        }

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.GREEN);
        mPaint.setAlpha(outCircleAlpha);
        canvas.drawCircle(centerX,centerY,outCircleRadius,mPaint);
        canvas.drawCircle(centerX,centerY,outCircleRadius*accelerate,mPaint);

    }


    public void startAnimation(){
        isWorking = true;
        radiusAnimator = ValueAnimator.ofFloat(mRadius, mRadius * 1.2F);
        radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                outCircleRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        radiusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        radiusAnimator.setDuration(2500);
        radiusAnimator.setInterpolator(new DecelerateInterpolator());
        radiusAnimator.start();

        alphaAnimator = ValueAnimator.ofInt(255, 0);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                outCircleAlpha = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setDuration(2500);
        alphaAnimator.setInterpolator(new AccelerateInterpolator());
        alphaAnimator.start();

        accelerateAnimator = ValueAnimator.ofFloat(1.0F, 1.3F);
        accelerateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                accelerate = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        accelerateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        accelerateAnimator.setDuration(2500);
        accelerateAnimator.setInterpolator(new AccelerateInterpolator());
        accelerateAnimator.start();
        invalidate();
    }

    public void endAnimation(){
        radiusAnimator.end();
        alphaAnimator.end();
        isWorking = false;
        invalidate();
    }

    public boolean isWorking() {
        return isWorking;
    }

    public void setEnable(boolean enable) {
        if (enable) {
            setClickable(true);
            mColor = mLastColor;
            invalidate();
        } else {
            setClickable(false);
            mColor = Color.DKGRAY;

            invalidate();
        }
    }

}
