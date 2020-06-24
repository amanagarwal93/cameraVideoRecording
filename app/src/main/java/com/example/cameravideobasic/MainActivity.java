package com.example.cameravideobasic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private Button capture, switchCamera;
    private Context myContext;
    private LinearLayout cameraPreview;
    private TextView timer;
    private boolean cameraFront = false;
    private int count = 3;
    private Thread thread;
    private boolean recording = false;

    private static final int REQUEST_PERMISSION_SETTING = 101;

    private String[] permissionsRequired = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissionsRequired[1]) == PackageManager.PERMISSION_GRANTED) {
            if (!hasCamera(myContext)) {
                Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
                toast.show();
                finish();
            }
            if (mCamera == null) {
                // if the front facing camera does not exist
                if (findFrontFacingCamera() < 0) {
                    Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                    switchCamera.setVisibility(View.GONE);
                }
                mPreview = new CameraPreview(myContext, mCamera);
                cameraPreview.addView(mPreview);

                mCamera = Camera.open(findBackFacingCamera());
                mPreview.refreshCamera(mCamera);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissionsRequired, REQUEST_PERMISSION_SETTING);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_SETTING) {

            if (permissions.length == 0) {
                return;
            }
            boolean allPermissionsGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            }
            if (!allPermissionsGranted) {
                boolean somePermissionsForeverDenied = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        //denied
                        Log.d("denied", permission);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(permissionsRequired, REQUEST_PERMISSION_SETTING);
                        }
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                            //allowed
                            Log.d("allowed", permission);
                        } else {
                            //set to never ask again
                            Log.d("set to never ask again", permission);
                            somePermissionsForeverDenied = true;
                        }
                    }
                }
                if (somePermissionsForeverDenied) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                            .setMessage("You have forcefully denied some of the required permissions " +
                                    "for this action. Please open settings, go to permissions and allow them.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                dialog.dismiss();
                            })
                            .setCancelable(false)
                            .create()
                            .show();
                }
            } else {
                Log.d("TAG", "permissions granted");
                if (!hasCamera(myContext)) {
                    Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
                    toast.show();
                    finish();
                }
                if (mCamera == null) {
                    // if the front facing camera does not exist
                    if (findFrontFacingCamera() < 0) {
                        Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                        switchCamera.setVisibility(View.GONE);
                    }
                    mPreview = new CameraPreview(myContext, mCamera);
                    cameraPreview.addView(mPreview);
                    mCamera = Camera.open(findBackFacingCamera());
                    mPreview.refreshCamera(mCamera);
                }
            }
        }
    }

    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);

        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captrureListener);
        timer = findViewById(R.id.textCountDown);
        switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
        switchCamera.setOnClickListener(switchCameraListener);
    }

    OnClickListener switchCameraListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // get the number of cameras
            if (!recording) {
                int camerasNumber = Camera.getNumberOfCameras();
                if (camerasNumber > 1) {
                    // release the old camera instance
                    // switch camera, from the front and the back and vice versa

                    releaseCamera();
                    chooseCamera();
                } else {
                    Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                    toast.show();
                }
            } else {
                Log.d("TAG", "onClick: " + recording);
            }
        }
    };

    public void chooseCamera() {
        // if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                // open the backFacingCamera
                // set a picture callback
                // refresh the preview

                mCamera = Camera.open(cameraId);
                // mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                // open the backFacingCamera
                // set a picture callback
                // refresh the preview

                mCamera = Camera.open(cameraId);
                // mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        // check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    OnClickListener captrureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording) {
                // stop recording and release camera
                mediaRecorder.stop(); // stop the recording
                releaseMediaRecorder(); // release the MediaRecorder object
                Toast.makeText(MainActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
//                thread.interrupt();
                recording = false;
                Log.d("TAG", "onClick: " + recording);
            } else {
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MainActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }

                /*thread = new Thread() {
                    @Override
                    public void run() {
                        while (!isInterrupted()) {
                            try {
                                Thread.sleep(1000);

                                runOnUiThread(() -> {
                                    // If there are stories, add them to the table
                                    if (count > 0) {
                                        timer.setText(String.valueOf(count));
                                        count--;
                                    } else {
                                        timer.setText("");
                                        count = 0;
                                        try {
                                            mediaRecorder.start();
                                        } catch (final Exception ex) {
                                            // Log.i("---","Exception in thread");
                                        }
                                        recording = true;
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();*/

                try {
                    mediaRecorder.start();
                } catch (final Exception ex) {
                    // Log.i("---","Exception in thread");
                }
                recording = true;
            }
        }
    };

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {

        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        mediaRecorder.setOutputFile("/sdcard/ " + System.currentTimeMillis() + "myvideo.mp4");
        mediaRecorder.setMaxDuration(600000); // Set max duration 60 sec.
        mediaRecorder.setMaxFileSize(50000000); // Set max file size 50M

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}