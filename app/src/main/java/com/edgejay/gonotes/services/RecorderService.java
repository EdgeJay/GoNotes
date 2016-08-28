package com.edgejay.gonotes.services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
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
import android.widget.ImageButton;
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

    }

    private void toggleOpen() {
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
        else {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

            mOptionsView = inflater.inflate(R.layout.layout_recorder_options, null);
            final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

            mWindowManager.addView(mOptionsView, layoutParams);

            ImageButton addEntryButton = (ImageButton) mOptionsView.findViewById(R.id.add_entry_button);
            addEntryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "add entry!");
                }
            });

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
