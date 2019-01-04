package github.dragynslayr.magicdb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class ListActivity extends AppCompatActivity {

    private String user;
    private Spinner spinner;
    private String[] foundCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        EditText searchBox = findViewById(R.id.searchBox);
        final ListView cardsHolder = findViewById(R.id.cardList);

        spinner = findViewById(R.id.spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort_order_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ListCardAdapter cardAdapter = (ListCardAdapter) cardsHolder.getAdapter();
                if (cardAdapter != null) {
                    cardAdapter.sortAndUpdate();
                }
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
                ListCardAdapter cardAdapter = (ListCardAdapter) cardsHolder.getAdapter();
                cardAdapter.filter = s.toString().toLowerCase();
                cardAdapter.notifyDataSetChanged();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                foundCards = new NetworkHandler(NetworkHandler.Command.GetList, user).getStringArray();
                final ArrayList<ListCard> cards;

                if (foundCards.length == 0) {
                    cards = new ArrayList<>();
                } else {
                    cards = parse(foundCards);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ListCardAdapter adapter = new ListCardAdapter(getApplicationContext(), cards);
                        cardsHolder.setAdapter(adapter);

                        if (cards.size() == 0) {
                            TextView errorText = findViewById(R.id.error);
                            errorText.setVisibility(View.VISIBLE);
                            cardsHolder.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    private ArrayList<ListCard> parse(String[] found) {
        ArrayList<ListCard> cards = new ArrayList<>(found.length);
        for (String s : found) {
            String[] parts = s.split("\t");
            ListCard card = new ListCard(parts[1], parts[0], parts[3], Integer.parseInt(parts[2]), Integer.parseInt(parts[4]));
            cards.add(card);
        }
        return cards;
    }

    private Comparator<ListCard> getComparator() {
        Comparator<ListCard> comparator = new CardNameComparator();
        String order = spinner.getSelectedItem().toString();
        if (order.equals("Cost")) {
            comparator = new CardCostComparator();
        } else if (order.equals("Color")) {
            comparator = new CardColorComparator();
        }
        return comparator;
    }

    class ListCardAdapter extends ArrayAdapter<ListCard> {

        String filter;
        private ArrayList<ListCard> cards;

        ListCardAdapter(Context context, ArrayList<ListCard> cards) {
            super(context, 0, cards);
            this.cards = cards;
            filter = "";
            sortAndUpdate();
        }

        @Nullable
        @Override
        public ListCard getItem(int position) {
            ListCard card = null;
            if (position < cards.size()) {
                card = cards.get(position);
                if (filter.length() > 0) {
                    if (card.name.toLowerCase().contains(filter)) {
                        return card;
                    } else {
                        return null;
                    }
                }
            }
            return card;
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) { // TODO: Simplify / fix this
            ListCard card = getItem(position);
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.card_list, parent, false);

            TextView nameText = convertView.findViewById(R.id.cardName);
            TextView quantityText = convertView.findViewById(R.id.cardQuantity);
            TextView costText = convertView.findViewById(R.id.cardCost);
            TextView idText = convertView.findViewById(R.id.cardId);
            TextView cmcText = convertView.findViewById(R.id.cardCMC);

            if (card != null && nameText != null) { // && card.name != null && nameText != null
                nameText.setText(card.name);
                quantityText.setText(card.quantity + "");
                costText.setText(card.cost);
                idText.setText(card.id);
                cmcText.setText(card.cmc + "");
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_null, parent, false);
                convertView.setVisibility(View.GONE);
            }

            return convertView;
        }

        void sortAndUpdate() {
            cards.sort(getComparator());
            notifyDataSetChanged();
        }
    }

    class ListCard {
        String name, id, cost;
        int quantity, cmc;

        ListCard(String name, String id, String cost, int cmc, int quantity) {
            this.name = name;
            this.id = id;
            this.cost = cost;
            this.cmc = cmc;
            this.quantity = quantity;
        }
    }

    public class CardNameComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    public class CardCostComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            int diff = o1.cmc - o2.cmc;
            if (diff != 0) {
                return diff;
            } else {
                return o1.name.compareTo(o2.name);
            }
        }
    }

    public class CardColorComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) { // TODO: Implement this
            int diff = o1.quantity - o2.quantity;
            if (diff != 0) {
                return diff;
            } else {
                return o1.quantity - o2.quantity;
            }
        }
    }
}
