package github.dragynslayr.magicdb;

import android.Manifest;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_CARDS = "MagicDB_Cards";

    private static final int PERM_REQ_ID = 101, PORT = 19615;
    private static final String TAG = "MagicDB_Main", IP = "70.72.212.179";
    private SurfaceView cameraView;
    private CameraSource cameraSource;
    private String scanned = "";
    private Thread searchThread;
    private boolean scanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.surfaceView);
        searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] found = search(scanned);
                Log.d(TAG, "Served: " + Arrays.toString(found));
                if (found.length > 0) {
                    Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                    intent.putExtra(EXTRA_CARDS, found);
                    startActivity(intent);
                }
            }
        });

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

    private String[] search(String needle) {
        ArrayList<String> found = new ArrayList<>();
        Log.d(TAG, "Searching");
        try {
            InetAddress addr = InetAddress.getByName(IP);
            Socket socket = new Socket(addr, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.print(needle);
            out.flush();
            String response = in.readLine();
            if (response != null) {
                String[] cards = response.split("\n");
                Log.d(TAG, "Count: " + cards.length);
                Collections.addAll(found, cards);
            }

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        scanning = true;
        return found.toArray(new String[0]);
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
                    if (items.size() != 0 && scanning) {
                        scanned = items.valueAt(0).getValue().replace('(', '\0').replace(')', '\0').trim();
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
}
