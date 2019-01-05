package github.dragynslayr.magicdb;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Objects;

public class ScanActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED = "MagicDB_Scanned";
    public static final String EXTRA_USER_NAME = "MagicDB_User";
    public static final String EXTRA_CARDS = "MagicDB_Cards";
    public static final String EXTRA_IDS = "MagicDB_IDs";

    private static final String TAG = "MagicDB_Main";
    private static final int PERM_REQ_ID = 101;

    private final float FPS = 2.0f;
    private final long TIME_OUT = 1500;

    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private Thread searchThread;
    private boolean scanning;
    private String scanned, user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_scan);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        scanning = true;
        scanned = "";

        cameraView = findViewById(R.id.surfaceView);
        searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] found = new NetworkHandler(NetworkHandler.Command.Search, scanned).getStringArray();
                String[] cards = new String[found.length];
                String[] ids = new String[found.length];
                for (int i = 0; i < found.length; i++) {
                    String[] parts = found[i].split("\t");
                    ids[i] = parts[0];
                    cards[i] = parts[1];
                }
                if (found.length > 0) {
                    Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                    intent.putExtra(EXTRA_SCANNED, scanned);
                    intent.putExtra(EXTRA_USER_NAME, user);
                    intent.putExtra(EXTRA_CARDS, cards);
                    intent.putExtra(EXTRA_IDS, ids);
                    startActivity(intent);
                } else {
                    scanning = true;
                }
            }
        });

        startCameraSource();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIME_OUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                scanning = true;
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERM_REQ_ID) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(cameraView.getHolder());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Camera Permission not granted")
                        .setMessage("Card scanning will not work without using the camera")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
            }
        }
    }

    private boolean isValidCard(String card) {
        boolean isUpper = !card.toLowerCase().equals(card);
        boolean isLower = !card.toUpperCase().equals(card);
        boolean isLong = card.length() >= 4;
        return (isUpper && isLower && isLong);
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
                    .setRequestedFps(FPS)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraSource.start(cameraView.getHolder());
                        } else {
                            ActivityCompat.requestPermissions(ScanActivity.this, new String[]{Manifest.permission.CAMERA}, PERM_REQ_ID);
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
                    if (scanning && items.size() != 0) {
                        scanned = items.valueAt(0).getValue();
                        scanned = Normalizer.normalize(scanned, Normalizer.Form.NFD);
                        scanned = replaceAll(scanned).replaceAll("[^\\x00-\\x7F]", "").trim();
                        if (isValidCard(scanned)) {
                            Log.d(TAG, "Found: " + scanned);
                            scanning = false;
                            searchThread.start();
                        }
                    }
                }
            });
        }
    }

    private String replaceAll(String s) {
        char[] chars = new char[]{'(', ')', ':', '\n', '\r', '\t'};
        for (char c : chars) {
            s = s.replace(c + "", "");
        }
        return s;
    }
}
