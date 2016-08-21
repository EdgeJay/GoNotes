package com.edgejay.gonotes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.edgejay.gonotes.services.RecorderService;

public class MainActivity extends AppCompatActivity {

    public static View rootView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onLaunchRecorder(View v) {
        // used by RecorderService for taking screenshots
        rootView = getWindow().getDecorView().getRootView();

        stopService(new Intent(this, RecorderService.class));
        startService(new Intent(this, RecorderService.class));
    }

    public void onCloseRecorder(View v) {
        stopService(new Intent(this, RecorderService.class));
    }
}
