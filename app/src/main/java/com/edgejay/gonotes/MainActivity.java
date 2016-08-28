package com.edgejay.gonotes;

import android.annotation.TargetApi;
import android.content.Intent;
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

    public static View rootView = null;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_SIGN_IN = 1000;
    private static final int REQUEST_OVERLAY = 2000;
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
                launchRecorder();
            }
        }
    }

    public void onLaunchRecorder(View v) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            launchRecorder();
        }
        // from API level 23 onwards, need to seek user permission to draw views on top of other apps
        else {
            canDrawOverlays();
        }

    }

    @TargetApi(23)
    private void canDrawOverlays() {
        if (Settings.canDrawOverlays(this)) {
            launchRecorder();
        }
        else {
            // request for permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        }
    }

    private void launchRecorder() {
        // used by RecorderService for taking screenshots
        rootView = getWindow().getDecorView().getRootView();

        stopService(new Intent(this, RecorderService.class));
        startService(new Intent(this, RecorderService.class));
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
