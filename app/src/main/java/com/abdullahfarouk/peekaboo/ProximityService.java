package com.abdullahfarouk.peekaboo;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;

import static com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS;
import static com.google.android.gms.vision.face.FaceDetector.ALL_LANDMARKS;
import static com.google.android.gms.vision.face.FaceDetector.FAST_MODE;

/**
 * Created by Zaid Zakir
 */
public class ProximityService extends Service {
    static final int IMAGE_WIDTH = 1024;
    static final int IMAGE_HEIGHT = 1024;
    static final int RIGHT_EYE = 0;
    static final int LEFT_EYE = 1;
    static final int AVERAGE_EYE_DISTANCE = 63; // in mm
    float F = 1f;           //focal length
    float sensorX, sensorY; //camera sensor dimensions
    float angleX, angleY;
    Handler handler;
    Toast toast;
    int LAYOUT_FLAG;
    private WindowManager manager;
    private View view;
    private WindowManager.LayoutParams layoutParams;

    public ProximityService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Camera camera = frontCam();
        Camera.Parameters campar = camera.getParameters();
        F = campar.getFocalLength();
        angleX = campar.getHorizontalViewAngle();
        angleY = campar.getVerticalViewAngle();
        sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * F);
        sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * F);
        camera.stopPreview();
        camera.release();
        createCameraSource();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }


        handler = new Handler();
        manager = (WindowManager) getApplicationContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        view = View.inflate(getApplicationContext(), R.layout.custom_toast, null);
        layoutParams = new WindowManager.LayoutParams();

    }


    private Camera frontCam() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            Log.v("CAMID", camIdx + "");
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("FAIL", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    @Override
    public void onDestroy() {

        if (toast != null) {
            toast.cancel();
        }
    }

    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(ALL_CLASSIFICATIONS)
                .setLandmarkType(ALL_LANDMARKS)
                .setMode(FAST_MODE)
                .build();
        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new FaceTracker()));

        CameraSource cameraSource = new CameraSource.Builder(this, detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
        System.out.println(cameraSource.getPreviewSize());

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraSource.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void showStatus(final float message) {

        if (message < 300) {

            final Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
            layoutParams.gravity = Gravity.CENTER;
            layoutParams.type = LAYOUT_FLAG;
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.alpha = 1.0f;
            layoutParams.packageName = getApplicationContext().getPackageName();
            layoutParams.buttonBrightness = 1f;
            layoutParams.windowAnimations = android.R.style.Animation_Dialog;

            Button okBtn = (Button) view.findViewById(R.id.okBtn);
            okBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    manager.removeView(view);
                }
            });
            if (!view.isShown()) {
                System.out.println("cameraaa " + "view is showwwnnnnnn " + view.isShown() + " " + message);
                manager.addView(view, layoutParams);
            }
        }


                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();
        }else {
            if (view.isShown()) {
                System.out.println("cameraaa " + "view is NOT showwwnnnnnn");
            }
        }
}

private class FaceTracker extends Tracker<Face> {


    private FaceTracker() {

    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        PointF leftEyePos = face.getLandmarks().get(LEFT_EYE).getPosition();
        PointF rightEyePos = face.getLandmarks().get(RIGHT_EYE).getPosition();

        float deltaX = Math.abs(leftEyePos.x - rightEyePos.x);
        float deltaY = Math.abs(leftEyePos.y - rightEyePos.y);

        final float distance;
        if (deltaX >= deltaY) {
            distance = F * (AVERAGE_EYE_DISTANCE / sensorX) * (IMAGE_WIDTH / deltaX);
        } else {
            distance = F * (AVERAGE_EYE_DISTANCE / sensorY) * (IMAGE_HEIGHT / deltaY);
        }


        showStatus(distance);
    }

    @Override
    public void onMissing(Detector.Detections<Face> detections) {
        super.onMissing(detections);
        showStatus(99999);
    }

    @Override
    public void onDone() {
        super.onDone();
    }

}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, PeekabooMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, "1")
                .setContentTitle("Peekaboo is Running!")
                .setContentText(input)
                .setSmallIcon(R.drawable.peekaboo_logo_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return Service.START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "1",
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}