package com.edgejay.gonotes.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.edgejay.gonotes.R;
import com.edgejay.gonotes.views.HoverPanelView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service {
    private final static String TAG = "RecorderService";
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
    private final int mMoveThreshold = 5;
    private final float mHoverViewOrigSize = 64F;
    private final float mHoverViewExpandScale = 1.2F;
    private final int mHoverViewExpandTime = 150;

    private boolean mOpen = false;
    private WindowManager mWindowManager;
    private ImageView mHoverView;
    private Resources mRes;
    private String[] mPokemonNames;

    // for expanded view
    private HoverPanelView mHoverPanelView;

    public RecorderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRes = getResources();
        mPokemonNames = mRes.getStringArray(R.array.pokemon_names);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        final int hoverViewSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mHoverViewOrigSize * mHoverViewExpandScale,
                mRes.getDisplayMetrics());

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
                            Log.d(TAG, "moved");
                        }
                        else {
                            toggleOpen();
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
        final String mPath = Environment.getExternalStorageDirectory().toString() + "/gonotes_screencap.jpg";

        /*
        try {
            MainActivity.rootView.setDrawingCacheEnabled(true);
            Bitmap bm = Bitmap.createBitmap(MainActivity.rootView.getDrawingCache());
            MainActivity.rootView.setDrawingCacheEnabled(false);

            File screenshotFile = new File(mPath);
            FileOutputStream os = new FileOutputStream(screenshotFile);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        */
    }

    private void toggleOpen() {
        if (mOpen) {
            if (mHoverPanelView != null) {
                mWindowManager.removeView(mHoverPanelView);
                mHoverPanelView = null;
            }
        }
        else {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            mHoverPanelView = (HoverPanelView) inflater.inflate(R.layout.layout_recorder_main, null);

            final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

            mWindowManager.addView(mHoverPanelView, layoutParams);
        }

        mOpen = !mOpen;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHoverView != null) {
            mWindowManager.removeView(mHoverView);
        }
    }
}
