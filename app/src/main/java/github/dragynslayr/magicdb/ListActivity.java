package github.dragynslayr.magicdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ListActivity extends AppCompatActivity {

    private String user;
    private LinearLayout cardsHolder;
    private ArrayList<CardItem> cardItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        cardsHolder = findViewById(R.id.cardList);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] cards = new NetworkHandler(NetworkHandler.Command.GetList, user).getStringArray();
                if (cards.length == 0) {
                    final TextView t = new TextView(getApplicationContext());
                    t.setText(getString(R.string.listError));
                    t.setTextSize(35.0f);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cardsHolder.addView(t);
                        }
                    });
                } else {
                    HashMap<String, Integer> map = new HashMap<>();
                    for (String card : cards) {
                        Integer num = map.get(card);
                        if (num != null) {
                            map.put(card, num + 1);
                        } else {
                            map.put(card, 1);
                        }
                    }
                    cardItems = new ArrayList<>(map.keySet().size());
                    for (Map.Entry<String, Integer> pair : map.entrySet()) {
                        cardItems.add(new CardItem(pair.getKey(), pair.getValue()));
                    }
                    cardItems.sort(new CardItemComparator());
                    Log.d("MagicDB_List", "cards: " + cardItems.size());
                    for (CardItem card : cardItems) {
                        final TextView t = new TextView(getApplicationContext());
                        @SuppressLint("DefaultLocale") String data = String.format("%s x%d", card.name, card.quantity);
                        t.setText(data);
                        t.setTextSize(35.0f);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cardsHolder.addView(t);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public class CardItem {
        public String name;
        int quantity;

        CardItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        int compare(CardItem other) {
            return name.compareTo(other.name);
        }
    }

    public class CardItemComparator implements Comparator<CardItem> {

        @Override
        public int compare(CardItem o1, CardItem o2) {
            return o1.compare(o2);
        }
    }
}
