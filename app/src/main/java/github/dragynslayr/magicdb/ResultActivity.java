package github.dragynslayr.magicdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        String scanned = intent.getStringExtra(ScanActivity.EXTRA_SCANNED);
        String[] cards = intent.getStringArrayExtra(ScanActivity.EXTRA_CARDS);

        TextView cardName = findViewById(R.id.scannedText);
        cardName.setText(scanned);

        LinearLayout cardsHolder = findViewById(R.id.cardHolder);
        for (String card : cards) {
            TextView t = new TextView(this);
            t.setText(card);
            t.setTextSize(35.0f);
            t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tv = (TextView) v;
                    if (tv != null) {
                        Log.d(TAG, tv.getText().toString());
                    } else {
                        Log.d(TAG, "TV NULL");
                    }
                }
            });
            cardsHolder.addView(t);
        }
    }

    public void reject(View view) {
        finish();
    }
}
