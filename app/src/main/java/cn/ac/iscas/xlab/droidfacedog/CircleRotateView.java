package cn.ac.iscas.xlab.droidfacedog;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by lisongting on 2017/7/4.
 */

public class CircleRotateView extends View {

    public static final String TAG = "CircleRotateView";
    private int circleColor,wrapColor;
    private int mWidth,mHeight;
    private Paint mPaint;
    private ValueAnimator radiusAnimator,degreeAnimator;
    private float degree,radius;
    private int strokeWidth;
    
    public CircleRotateView(Context context) {
        this(context,null);
    }

    public CircleRotateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircleRotateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.CircleRotateView, defStyleAttr, 0);
        int n = typedArray.getIndexCount();
        Log.i(TAG, "typedArray Count:" + n);
        for(int i=0;i<n;i++) {
            int attr = typedArray.getIndex(i);
            //在这个中获取自定义的styleable属性
            switch (attr) {
                case R.styleable.CircleRotateView_circle_color:
                    //如果获取不到，则为默认值黑色
                    circleColor = typedArray.getColor(attr, Color.BLUE);
                    break;
                case R.styleable.CircleRotateView_wrap_color:
                    wrapColor = typedArray.getColor(attr, Color.RED);
                    break;
                case R.styleable.CircleRotateView_stroke_width:
                    strokeWidth = typedArray.getDimensionPixelSize(attr, 10);
                    break;
            }
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        degree = 0;
        typedArray.recycle();
    }

    @Override
    public void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        //测量得到的宽度
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //测量的到的高度
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        Log.i(TAG, "MeasureSpec widthSize :" + widthSize + "heightSize:" + heightSize);
        int width,height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = widthSize / 2;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = heightSize / 2;
        }
        //设置外部的宽高
        mWidth = width;
        mHeight = height;
        radius = height / 4;

        Log.i(TAG, "setMeasuredDimension:" + width + "x" + height+",degree:"+degree);
        setMeasuredDimension(width, height);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(mWidth / 2, mHeight / 2);
        mPaint.setColor(circleColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, radius, mPaint);
        canvas.rotate(degree);
        RectF rectF = new RectF(-radius * 5 / 3, -radius * 5 / 3, radius * 5 / 3, radius * 5 / 3);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setColor(wrapColor);
        canvas.drawArc(rectF, 50, 80, false, mPaint);
        canvas.drawArc(rectF, -130, 80, false, mPaint);

    }

    public void startAnimation() {
        radius = mHeight / 4;
        degree = 0;

        radiusAnimator = ValueAnimator.ofFloat(mHeight / 4, mHeight / 8, mHeight / 4);
        radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        radiusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        radiusAnimator.setDuration(1500);
        radiusAnimator.start();

        degreeAnimator = ValueAnimator.ofFloat(0, 360);
        degreeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                degree = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        degreeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        degreeAnimator.setDuration(1500);
        degreeAnimator.start();

    }

    public void endAnimation() {
        degreeAnimator.end();
        radiusAnimator.end();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.i(TAG, "onWindowFocusChanged");
        if (!hasWindowFocus) {
            endAnimation();
        }

    }
}
