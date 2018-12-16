package github.dragynslayr.magicdb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private TextView textView;
    private CameraSource cameraSource;

    private static final int PERM_REQ_ID = 101;
    private static final String TAG = "MagicDB_Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.surfaceView);
        textView = findViewById(R.id.text_view);

        startCameraSource();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERM_REQ_ID) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(cameraView.getHolder());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCameraSource() {
        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!recognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), recognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1920, 1080)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(1.0f)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraSource.start(cameraView.getHolder());
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERM_REQ_ID);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            recognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                String s = items.valueAt(0).getValue();
                                s = s.replace('(', '\0');
                                s = s.replace(')', '\0');
                                textView.setText(s);
                            }
                        });
                    }
                }
            });
        }
    }
}
