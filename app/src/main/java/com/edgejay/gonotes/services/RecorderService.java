package com.edgejay.gonotes.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.edgejay.gonotes.MainActivity;
import com.edgejay.gonotes.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service {
    private final static String TAG = "RecorderService";
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
    private final int mMoveThreshold = 5;

    private boolean mOpen = false;
    private WindowManager mWindowManager;
    private ImageView mHoverView;
    private Resources mRes;
    private String[] mPokemonNames;

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

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 128, mRes.getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 128, mRes.getDisplayMetrics()),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 0;

        mHoverView = new ImageView(this);
        //mHoverView.setBackgroundColor(Color.RED);
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
                                .scaleX(1.2F)
                                .scaleY(1.2F)
                                .setDuration(150)
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
                                .setDuration(150)
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
        mOpen = !mOpen;
        Log.d(TAG, "open: " + mOpen);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHoverView != null) {
            mWindowManager.removeView(mHoverView);
        }
    }
}
