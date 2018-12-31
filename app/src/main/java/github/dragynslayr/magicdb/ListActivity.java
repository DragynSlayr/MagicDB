package github.dragynslayr.magicdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ListActivity extends AppCompatActivity {

    private String user;
    private Spinner spinner;
    private EditText searchBox;
    private String[] foundCards;
    private CardItem[] received, inView;
    private LinearLayout cardsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        received = new CardItem[0];

        searchBox = findViewById(R.id.searchBox);
        cardsHolder = findViewById(R.id.cardList);

        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort_order_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterItems(s.toString().toLowerCase());
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                foundCards = new NetworkHandler(NetworkHandler.Command.GetList, user).getStringArray();
                Arrays.sort(foundCards);
                if (foundCards.length == 0) {
                    addToView(new CardItem[0]);
                } else {
                    ArrayList<CardItem> cardItems = flattenCards(foundCards);
                    cardItems.sort(getComparator());
                    received = cardItems.toArray(new CardItem[0]);
                    addToView(received);
                }
            }
        }).start();
    }

    private void addToView(final CardItem[] cards) {
        inView = cards;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardsHolder.removeAllViews();
                if (cards.length == 0) {
                    cardsHolder.addView(createTextView(getString(R.string.listError)));
                } else {
                    for (CardItem card : cards) {
                        cardsHolder.addView(createTextView(card.name + " x" + card.quantity));
                    }
                }
            }
        });
    }

    private TextView createTextView(String text) {
        TextView t = new TextView(getApplicationContext());
        t.setText(text);
        t.setTextSize(35.0f);
        return t;
    }

    private ArrayList<CardItem> flattenCards(String[] foundCards) {
        HashMap<String, Integer> map = new HashMap<>();
        for (String card : foundCards) {
            Integer num = map.get(card);
            if (num != null) {
                map.put(card, num + 1);
            } else {
                map.put(card, 1);
            }
        }
        ArrayList<CardItem> cardItems = new ArrayList<>(map.keySet().size());
        for (Map.Entry<String, Integer> pair : map.entrySet()) {
            cardItems.add(new CardItem(pair.getKey(), pair.getValue()));
        }
        return cardItems;
    }

    private Comparator<CardItem> getComparator() {
        Comparator<CardItem> comparator = new CardNameComparator();
        String order = spinner.getSelectedItem().toString();
        if (order.equals("Cost")) {
            comparator = new CardCostComparator();
        } else if (order.equals("Color")) {
            comparator = new CardColorComparator();
        }
        return comparator;
    }

    private void updateList() {
        ArrayList<CardItem> cardItems = new ArrayList<>();
        Collections.addAll(cardItems, inView);
        cardItems.sort(getComparator());
        addToView(cardItems.toArray(new CardItem[0]));
    }

    private void filterItems(String filter) {
        if (filter.length() == 0) {
            addToView(received);
        } else {
            ArrayList<String> filtered = new ArrayList<>(foundCards.length);
            for (String s : foundCards) {
                if (s.toLowerCase().contains(filter)) {
                    filtered.add(s);
                }
            }
            ArrayList<CardItem> cardItems = flattenCards(filtered.toArray(new String[0]));
            cardItems.sort(getComparator());
            addToView(cardItems.toArray(new CardItem[0]));
        }
    }

    public class CardItem {
        public String name;
        int quantity;

        CardItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    public class CardNameComparator implements Comparator<CardItem> {

        @Override
        public int compare(CardItem o1, CardItem o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    public class CardCostComparator implements Comparator<CardItem> {

        @Override
        public int compare(CardItem o1, CardItem o2) { // TODO: Implement this
            int diff = o1.quantity - o2.quantity;
            if (diff != 0) {
                return diff;
            } else {
                return o1.quantity - o2.quantity;
            }
        }
    }

    public class CardColorComparator implements Comparator<CardItem> {

        @Override
        public int compare(CardItem o1, CardItem o2) { // TODO: Implement this
            int diff = o1.quantity - o2.quantity;
            if (diff != 0) {
                return diff;
            } else {
                return o1.quantity - o2.quantity;
            }
        }
    }
}
