package com.edgejay.gonotes.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.edgejay.gonotes.R;
import com.edgejay.gonotes.views.HoverPanelView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * How to take screenshot: http://binwaheed.blogspot.sg/2015/03/how-to-correctly-take-screenshot-using.html
 */
public class RecorderService extends Service {
    public static final String MEDIA_PROJECTION_RESULT_CODE = "RecorderService.MEDIA_PROJECTION_RESULT_CODE";
    public static final String MEDIA_PROJECTION_DATA = "RecorderService.MEDIA_PROJECTION_DATA";
    private final static String TAG = "RecorderService";

    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);

    private final int mMoveThreshold = 5;
    private final float mHoverViewOrigSize = 64F;
    private final float mHoverViewExpandScale = 1.2F;
    private final int mHoverViewExpandTime = 150;

    private boolean mOpen = false;
    private boolean mPaused = false;
    private WindowManager mWindowManager;
    private ImageView mHoverView;
    private Resources mRes;
    private String[] mPokemonNames;

    private MediaProjectionManager mMediaProjectionManager = null;
    private MediaProjection mMediaProjection = null;
    private int mMediaProjectionResultCode;
    private Intent mMediaProjectionData;
    private ImageReader mImageReader;

    // for expanded view
    //private HoverPanelView mHoverPanelView;
    private View mOptionsView;

    public RecorderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not used
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMediaProjectionResultCode = intent.getIntExtra(MEDIA_PROJECTION_RESULT_CODE, -1);
        mMediaProjectionData = intent.getParcelableExtra(MEDIA_PROJECTION_DATA);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRes = getResources();
        mPokemonNames = mRes.getStringArray(R.array.pokemon_names);

        // window manager for managing views drawn over other apps
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // for capturing screen content
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mMediaProjectionResultCode, mMediaProjectionData);

        Point winSize = new Point();
        mWindowManager.getDefaultDisplay().getSize(winSize);
        mImageReader = ImageReader.newInstance(winSize.x, winSize.y, ImageFormat.RGB_565, 2);

        // prepare to create and add "Pokeball" to screen
        final int hoverViewSize = calculatePixelValue(mHoverViewOrigSize * mHoverViewExpandScale);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                hoverViewSize,
                hoverViewSize,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 0;

        mHoverView = new ImageView(this);
        mHoverView.setScaleType(ImageView.ScaleType.CENTER);
        mHoverView.setImageResource(R.mipmap.ic_pokeball);
        mHoverView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        mHoverView.animate().cancel();
                        mHoverView.animate()
                                .scaleX(mHoverViewExpandScale)
                                .scaleY(mHoverViewExpandScale)
                                .setDuration(mHoverViewExpandTime)
                                .setInterpolator(new FastOutSlowInInterpolator());
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (Math.abs(layoutParams.x - initialX) >= mMoveThreshold ||
                            Math.abs(layoutParams.y - initialY) >= mMoveThreshold) {
                            //Log.d(TAG, "moved");
                        }
                        else {
                            toggleOpen(layoutParams);
                        }

                        mHoverView.animate().cancel();
                        mHoverView.animate()
                                .scaleX(1.0F)
                                .scaleY(1.0F)
                                .setDuration(mHoverViewExpandTime)
                                .setInterpolator(new FastOutSlowInInterpolator());

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mHoverView, layoutParams);

                        if (mOpen) {
                            toggleOpen(null);
                        }
                        return true;
                }
                return false;
            }
        });

        mWindowManager.addView(mHoverView, layoutParams);

        mScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                findCapturedPokemon();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void findCapturedPokemon() {
        // take screenshot

    }

    private void toggleOpen(@Nullable final WindowManager.LayoutParams layoutParams) {
        // if already in "open" state
        if (mOpen) {

            if (mOptionsView != null) {
                mWindowManager.removeView(mOptionsView);
                mOptionsView = null;
            }

            /*
            if (mHoverPanelView != null) {
                mWindowManager.removeView(mHoverPanelView);
                mHoverPanelView = null;
            }
            */
        }
        // if in "closed" state
        else {
            if (layoutParams != null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

                final Point winSize = new Point();
                mWindowManager.getDefaultDisplay().getSize(winSize);

                // shift hover view to top right corner
                layoutParams.x = winSize.x - calculatePixelValue(mHoverViewOrigSize + 10);
                layoutParams.y = (winSize.y / 2) - calculatePixelValue(mHoverViewOrigSize / 2);
                //mWindowManager.updateViewLayout(mHoverView, layoutParams);
                mWindowManager.removeView(mHoverView);

                mOptionsView = inflater.inflate(R.layout.layout_recorder_options, null);
                final WindowManager.LayoutParams optionsLayoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                optionsLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
                optionsLayoutParams.x = layoutParams.x - calculatePixelValue(110F);
                optionsLayoutParams.y = layoutParams.y + calculatePixelValue(10F);

                mWindowManager.addView(mOptionsView, optionsLayoutParams);
                mWindowManager.addView(mHoverView, layoutParams);

                ImageButton pauseButton = (ImageButton) mOptionsView.findViewById(R.id.pause_button);
                pauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePause((ImageButton) v);
                    }
                });
            }

            /*
            mHoverPanelView = (HoverPanelView) inflater.inflate(R.layout.layout_recorder_main, null);

            final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

            mWindowManager.addView(mHoverPanelView, layoutParams);
            */
        }

        mOpen = !mOpen;
    }

    private void togglePause(ImageButton pauseButton) {
        if (mPaused) {
            pauseButton.setImageResource(R.mipmap.ic_pause_circle_outline);
        }
        else {
            pauseButton.setImageResource(R.mipmap.ic_pause_circle_fill);
        }

        mPaused = !mPaused;
    }

    /**
     * Returns actual pixel value based on screen density
     *
     * @param value
     * @return
     */
    private int calculatePixelValue(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, mRes.getDisplayMetrics()
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHoverView != null) {
            mWindowManager.removeView(mHoverView);
        }

        if (mOptionsView != null) {
            mWindowManager.removeView(mOptionsView);
        }
    }
}
