package github.dragynslayr.magicdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.Objects;

public class ResultActivity extends AppCompatActivity {

    public static final String TAG = "MagicDB_Result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        String[] cards = intent.getStringArrayExtra(MainActivity.EXTRA_CARDS);
        Log.d(TAG, Arrays.toString(cards));
    }
}
