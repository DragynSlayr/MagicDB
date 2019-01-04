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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
                    TextView text = new TextView(getContext());
                    text.setText("Would you like to add " + name + "?");
                    text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22.0f);
                    text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    text.setTextColor(getColor(R.color.greenBG));
                    AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Dialog).setTitle("Add " + name).setView(text).setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendPut(id, name);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setLayout(WRAP_CONTENT, WRAP_CONTENT);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Dialog).setTitle("Add " + name).setView(R.layout.dialog).setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText input = ((AlertDialog) dialog).findViewById(R.id.addInput);
                            String text = input.getText().toString();
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
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setLayout(WRAP_CONTENT, WRAP_CONTENT);

                    return true;
                }
            });

            return convertView;
        }

        private void sendPut(String id, String name) {
            sendPut(id, name, 1);
        }

        private void sendPut(final String id, final String name, final int num) {
            toast("Adding " + name);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = new NetworkHandler(NetworkHandler.Command.AddCard, user + ":" + num + ":" + id).getString();
                    if (result.equals("Put Success")) {
                        toast("Added " + name);
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
