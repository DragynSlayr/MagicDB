package github.dragynslayr.magicdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import java.util.Objects;

public class MenuActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "MagicDB_User";
    private String user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_menu);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);
    }

    public void scan(View view) {
        Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
        intent.putExtra(EXTRA_USER_NAME, user);
        startActivity(intent);
    }

    public void list(View view) {
        Intent intent = new Intent(getApplicationContext(), ListActivity.class);
        intent.putExtra(EXTRA_USER_NAME, user);
        startActivity(intent);
    }

    public void logout(View view) {
        finish();
    }
}
