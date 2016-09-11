package com.edgejay.gonotes;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.edgejay.gonotes.services.RecorderService;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_SIGN_IN        = 1000;
    private static final int REQUEST_OVERLAY        = 2000;
    private static final int REQUEST_SCREEN_CAPTURE = 3000;

    private MediaProjectionManager mMediaProjectionManager = null;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create Google sign in options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        else if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Settings.canDrawOverlays(this)) {
                canDoScreenCapture();
            }
            else {
                //TODO display error
            }
        }
        else if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                launchRecorder(resultCode, data);
            }
            else {
                //TODO display error
            }
        }
    }

    public void onLaunchRecorder(View v) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            canDoScreenCapture();
        }
        // from API level 23 onwards, need to seek user permission to draw views on top of other apps
        else {
            canDrawOverlays();
        }

    }

    @TargetApi(23)
    private void canDrawOverlays() {
        // check if user granted app permission to draw over other apps
        if (Settings.canDrawOverlays(this)) {
            canDoScreenCapture();
        }
        else {
            // request for permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        }
    }

    private void canDoScreenCapture() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE);
    }

    private void launchRecorder(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            stopService(new Intent(this, RecorderService.class));

            Intent intent = new Intent(this, RecorderService.class);
            intent.putExtra(RecorderService.MEDIA_PROJECTION_RESULT_CODE, resultCode);
            intent.putExtra(RecorderService.MEDIA_PROJECTION_DATA, data);
            startService(intent);
        }
    }

    public void onCloseRecorder(View v) {
        stopService(new Intent(this, RecorderService.class));
    }

    public void onGoogleSignin(View v) {
        signIn();
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGN_IN);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, result.getSignInAccount().getDisplayName());
        Log.d(TAG, result.getSignInAccount().getEmail());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, String.valueOf(connectionResult));
    }
}
