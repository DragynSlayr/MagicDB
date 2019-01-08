package github.dragynslayr.magicdb;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ResultActivity extends AppCompatActivity {

    public static final String TAG = "MagicDB_Result";

    private String user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        String scanned = intent.getStringExtra(ScanActivity.EXTRA_SCANNED);
        String[] names = intent.getStringArrayExtra(ScanActivity.EXTRA_CARDS);
        String[] ids = intent.getStringArrayExtra(ScanActivity.EXTRA_IDS);
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        ArrayList<ResultCard> cards = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            cards.add(new ResultCard(ids[i], names[i]));
        }
        cards.sort(new ResultCardComparator());

        TextView cardName = findViewById(R.id.scannedText);
        cardName.setText(scanned);

        ListView cardsHolder = findViewById(R.id.cardHolder);
        ArrayAdapter<ResultCard> adapter = new ResultCardAdapter(this, cards);
        cardsHolder.setAdapter(adapter);
    }

    class ResultCard {
        public String id, name;

        ResultCard(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class ResultCardAdapter extends ArrayAdapter<ResultCard> {

        ResultCardAdapter(Context context, ArrayList<ResultCard> cards) {
            super(context, 0, cards);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ResultCard card = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.card_result, parent, false);
            }

            final TextView nameText = convertView.findViewById(R.id.cardName);
            final TextView idText = convertView.findViewById(R.id.cardId);

            if (card != null) {
                nameText.setText(card.name);
                idText.setText(card.id);
            }

            final String name = nameText.getText().toString();
            final String id = idText.getText().toString();

            convertView.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    showDialog(id, name, true);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDialog(id, name, false);
                    return true;
                }
            });

            return convertView;
        }

        private void showDialog(final String id, final String name, boolean autoFill) {
            View layout = View.inflate(getApplicationContext(), R.layout.dialog, null);
            final EditText text = layout.findViewById(R.id.addInput);
            if (autoFill) {
                text.setText("1");
            }
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            text.postDelayed(new Runnable() {
                @Override
                public void run() {
                    text.requestFocus();
                    Objects.requireNonNull(imm).showSoftInput(text, 0);
                }
            }, 100);

            final AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Dialog).setTitle("Add " + name).setView(layout).setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handlePut(text.getText().toString(), id, name);
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setLayout(WRAP_CONTENT, WRAP_CONTENT);

            text.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            handlePut(text.getText().toString(), id, name);
                            dialog.cancel();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        private void handlePut(String text, String id, String name) {
            if (text.length() > 0) {
                int num = Integer.parseInt(text);
                if (num > 0 && num < 1000) {
                    sendPut(id, name, num);
                } else {
                    toast("Amount must be between 1-999", Toast.LENGTH_LONG);
                }
            } else {
                toast("Amount must be between 1-999", Toast.LENGTH_LONG);
            }
        }

        private void sendPut(final String id, final String name, final int num) {
            toast("Adding " + num + " of " + name);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = new NetworkHandler(NetworkHandler.Command.AddCard, user + ":" + num + ":" + id).getString();
                    if (result.equals("Put Success")) {
                        toast("Added " + num + " of " + name);
                        finish();
                    } else {
                        Log.d(TAG, "Failed put");
                    }
                }
            }).start();
        }

        private void toast(String text) {
            toast(text, Toast.LENGTH_SHORT);
        }

        private void toast(final String text, final int duration) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), text, duration).show();
                }
            });
        }
    }

    class ResultCardComparator implements Comparator<ResultCard> {

        @Override
        public int compare(ResultCard o1, ResultCard o2) {
            return o1.name.compareTo(o2.name);
        }
    }
}
