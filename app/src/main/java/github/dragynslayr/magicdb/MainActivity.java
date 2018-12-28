package github.dragynslayr.magicdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "MagicDB_User";

    private Thread loginThread, registerThread;
    private EditText userEdit, passEdit;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);

        userEdit = findViewById(R.id.username);
        passEdit = findViewById(R.id.password);
        errorText = findViewById(R.id.errorMessage);

        passEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        login(findViewById(R.id.loginButton));
                        return true;
                    }
                }
                return false;
            }
        });

        loginThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendRequest(NetworkHandler.Command.Login, "Login Success");
            }
        });
        registerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendRequest(NetworkHandler.Command.Register, "Registration Success");
            }
        });
    }

    private void sendRequest(NetworkHandler.Command cmd, String expected) {
        String user = userEdit.getText().toString();
        String pass = passEdit.getText().toString();
        String result = new NetworkHandler(cmd, user + ":" + pass).getString();

        if (result.equals(expected)) {
            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
            intent.putExtra(EXTRA_USER_NAME, user);
            startActivity(intent);
        } else {
            flashError(result);
        }
    }

    private void flashError(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorText.setText(result);
                errorText.setVisibility(View.VISIBLE);
            }
        });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorText.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void login(View view) {
        errorText.setVisibility(View.INVISIBLE);
        loginThread.start();
    }

    public void register(View view) {
        errorText.setVisibility(View.INVISIBLE);
        registerThread.start();
    }
}