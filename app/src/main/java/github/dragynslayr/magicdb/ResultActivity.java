package github.dragynslayr.magicdb;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
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
import java.util.Objects;

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

        TextView cardName = findViewById(R.id.scannedText);
        cardName.setText(scanned);

        ListView cardsHolder = findViewById(R.id.cardHolder);
        ArrayAdapter<ResultCard> adapter = new ResultCardAdapter(this, cards);
        cardsHolder.setAdapter(adapter);

        /*
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
                        final String name = tv.getText().toString();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String result = new NetworkHandler(NetworkHandler.Command.AddCard, user + ":" + name).getString();
                                if (result.equals("Put Success")) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Added " + name, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    finish();
                                } else {
                                    Log.d(TAG, "Failed put");
                                }
                            }
                        }).start();
                    } else {
                        Log.d(TAG, "TV NULL");
                    }
                }
            });
            cardsHolder.addView(t);
        }
        */
    }

    class ResultCard {
        public String id, name;

        ResultCard(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class ResultCardAdapter extends ArrayAdapter<ResultCard> {

        private ArrayList<ResultCard> cards;

        ResultCardAdapter(Context context, ArrayList<ResultCard> cards) {
            super(context, 0, cards);
            this.cards = cards;
            // sort
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

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = nameText.getText().toString();
                    String id = idText.getText().toString();
                    sendPut(id, name);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final String name = nameText.getText().toString();
                    final String id = idText.getText().toString();

                    final EditText input = new EditText(getApplicationContext());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setText("0");

                    new AlertDialog.Builder(getContext()).setTitle("Specify Quantity").setView(input).setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int num = Integer.parseInt(input.getText().toString());
                            sendPut(id, name, num);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();

                    return true;
                }
            });

            return convertView;
        }

        private void sendPut(String id, String name) {
            sendPut(id, name, 1);
        }

        private void sendPut(final String id, final String name, final int num) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = new NetworkHandler(NetworkHandler.Command.AddCard, user + ":" + num + ":" + id).getString();
                    if (result.equals("Put Success")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Added " + name, Toast.LENGTH_SHORT).show();
                            }
                        });
                        finish();
                    } else {
                        Log.d(TAG, "Failed put");
                    }
                }
            }).start();
        }
    }
}
