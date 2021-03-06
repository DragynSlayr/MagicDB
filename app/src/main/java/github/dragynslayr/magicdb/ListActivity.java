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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ListActivity extends AppCompatActivity {

    private final String TAG = "MagicDB_List";

    private String user;
    private Spinner spinner;
    private ArrayList<ListCard> cards, allCards;
    private Thread listThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        user = intent.getStringExtra(MainActivity.EXTRA_USER_NAME);

        final EditText searchBox = findViewById(R.id.searchBox);
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
                filterCards(s.toString().toLowerCase(), cardsHolder);
            }
        });

        listThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] foundCards = new NetworkHandler(NetworkHandler.Command.GetList, user).getStringArray();
                if (foundCards.length == 0) {
                    cards = new ArrayList<>();
                } else {
                    cards = parse(foundCards);
                }

                allCards = new ArrayList<>();
                allCards.addAll(cards);

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

                        filterCards(searchBox.getText().toString().toLowerCase(), cardsHolder);
                    }
                });
            }
        });
        listThread.start();
    }

    @SuppressLint("SetTextI18n")
    private void filterCards(String filter, ListView cardsHolder) {
        ListCardAdapter cardAdapter = (ListCardAdapter) cardsHolder.getAdapter();
        TextView countText = findViewById(R.id.cardCount);
        int visible = 0, total = 0;
        for (ListCard card : allCards) {
            total += card.quantity;
        }
        if (filter.length() > 0) {
            ArrayList<ListCard> cards = new ArrayList<>();
            for (ListCard card : allCards) {
                if (card.name.toLowerCase().contains(filter)) {
                    cards.add(card);
                    visible += card.quantity;
                }
            }
            cardAdapter.clear();
            cardAdapter.addAll(cards);
        } else {
            visible = total;
            cardAdapter.clear();
            cardAdapter.addAll(allCards);
        }
        if (visible == total) {
            countText.setText("All " + total + " cards found");
        } else {
            countText.setText(visible + "/" + total + " cards visible");
        }
        cardAdapter.sortAndUpdate();
        TextView errorText = findViewById(R.id.error);
        if (cardAdapter.getCount() == 0) {
            errorText.setVisibility(View.VISIBLE);
            cardsHolder.setVisibility(View.GONE);
        } else {
            errorText.setVisibility(View.GONE);
            cardsHolder.setVisibility(View.VISIBLE);
        }
    }

    private ArrayList<ListCard> parse(String[] found) {
        ArrayList<ListCard> cards = new ArrayList<>(found.length);
        for (String s : found) {
            String[] parts = s.split("\t");
            ListCard card = new ListCard(parts[1], parts[0], parts[3], Float.parseFloat(parts[2]), Integer.parseInt(parts[4]));
            cards.add(card);
        }
        return cards;
    }

    private Comparator<ListCard> getComparator() {
        String order = spinner.getSelectedItem().toString();
        switch (order) {
            case "Cost":
                return new CardCostComparator();
            case "Color":
                return new CardColorComparator();
            case "Quantity":
                return new CardQuantityComparator();
            default:
                return new CardNameComparator();
        }
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

    class ListCardAdapter extends ArrayAdapter<ListCard> {

        private ArrayList<ListCard> cards;

        ListCardAdapter(Context context, ArrayList<ListCard> cards) {
            super(context, 0, cards);
            this.cards = cards;
            sortAndUpdate();
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ListCard card = Objects.requireNonNull(getItem(position));
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.card_list, parent, false);
            }

            TextView nameText = convertView.findViewById(R.id.cardName);
            TextView quantityText = convertView.findViewById(R.id.cardQuantity);
            TextView idText = convertView.findViewById(R.id.cardId);
            TextView cmcText = convertView.findViewById(R.id.cardCMC);
            LinearLayout costLayout = convertView.findViewById(R.id.cardCost);

            nameText.setText(card.name);
            quantityText.setText("x" + card.quantity);
            idText.setText(card.id);
            cmcText.setText(card.cmc + "");

            costLayout.removeAllViews();
            for (String s : card.costs) {
                ImageView iv = new ImageView(getContext());
                String name = "ic_" + s.replaceAll("[^\\x00-\\x7F]", "").toLowerCase();
                iv.setImageResource(getResources().getIdentifier(name, "drawable", getPackageName()));
                iv.setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, 1.0f));
                costLayout.addView(iv);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "ID: " + card.id);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    View layout = View.inflate(getApplicationContext(), R.layout.dialog, null);
                    final EditText text = layout.findViewById(R.id.addInput);
                    text.setHint("1-" + card.quantity);
                    text.setHintTextColor(getColor(R.color.greenBG));
                    final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    text.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            text.requestFocus();
                            Objects.requireNonNull(imm).showSoftInput(text, 0);
                        }
                    }, 100);

                    final AlertDialog dialog = new AlertDialog.Builder(ListActivity.this, R.style.Dialog).setTitle("Remove " + card.name).setView(layout).setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handleRemove(text.getText().toString(), card);
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
                                    handleRemove(text.getText().toString(), card);
                                    dialog.cancel();
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                    return true;
                }
            });

            return convertView;
        }

        private void handleRemove(String text, final ListCard card) {
            String errorString = "Amount must be between 1-" + card.quantity;
            if (text.length() > 0) {
                final int num = Integer.parseInt(text);
                if (num > 0 && num <= card.quantity) {
                    toast("Removing " + num + " of " + card.name);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String result = new NetworkHandler(NetworkHandler.Command.RemoveCard, user + ":" + card.id + ":" + num).getString();
                            if (result.equals("Remove Success")) {
                                toast("Removed " + num + " of " + card.name);
                                listThread.start();
                            } else {
                                Log.d(TAG, "Failed remove");
                            }
                        }
                    }).start();
                } else {
                    toast(errorString, Toast.LENGTH_LONG);
                }
            } else {
                toast(errorString, Toast.LENGTH_LONG);
            }
        }

        void sortAndUpdate() {
            cards.sort(getComparator());
            notifyDataSetChanged();
        }
    }

    class CardMana {

        private int[] mana = new int[6];
        private int x = 0, normal = 0;

        CardMana(String mcs) {
            String[] manas = mcs.replace("}", " ").replace("{", "").split(" ");
            for (String s : manas) {
                try {
                    normal += Integer.parseInt(s);
                } catch (NumberFormatException nfe) {
                    switch (s) {
                        case "W":
                            mana[0]++;
                            break;
                        case "U":
                            mana[1]++;
                            break;
                        case "B":
                            mana[2]++;
                            break;
                        case "R":
                            mana[3]++;
                            break;
                        case "G":
                            mana[4]++;
                            break;
                        case "C":
                            mana[5]++;
                            break;
                        case "X":
                            x++;
                            break;
                    }
                }
            }
        }

        int compare(CardMana other, ListCard a, ListCard b) {
            for (int i = mana.length - 1; i > -1; i--) {
                int manaDiff = mana[i] - other.mana[i];
                if (manaDiff != 0) {
                    return manaDiff;
                }
            }

            int xDiff = x - other.x;
            if (xDiff != 0) {
                return xDiff;
            }

            float cmcDiff = a.cmc - b.cmc;
            if (cmcDiff != 0) {
                return (int) cmcDiff;
            }

            int normalDiff = normal - other.normal;
            if (normalDiff != 0) {
                return normalDiff;
            }

            return a.compareName(b);
        }
    }

    class ListCard {

        String name, id, cost;
        int quantity;
        float cmc;
        CardMana mana;
        ArrayList<String> costs;

        ListCard(String name, String id, String cost, float cmc, int quantity) {
            this.name = name;
            this.id = id;
            this.cost = cost;
            this.cmc = cmc;
            this.quantity = quantity;
            mana = new CardMana(cost);
            parseCost(cost);
        }

        void parseCost(String cost) {
            String[] manas = cost.replace("}", " ").replace("{", "").split(" ");
            for (int i = 0; i < manas.length; i++) {
                manas[i] = manas[i].replace("/", "");
            }
            costs = new ArrayList<>(manas.length);
            Collections.addAll(costs, manas);
        }

        int compareName(ListCard other) {
            return name.compareTo(other.name);
        }

        int compareCost(ListCard other) {
            float diff = cmc - other.cmc;
            return (diff != 0) ? (int) diff : compareName(other);
        }

        int compareColor(ListCard other) {
            return mana.compare(other.mana, this, other);
        }

        int compareQuantity(ListCard other) {
            int diff = quantity - other.quantity;
            return (diff != 0) ? diff : compareName(other);
        }
    }

    public class CardNameComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            return o1.compareName(o2);
        }
    }

    public class CardCostComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            return o1.compareCost(o2);
        }
    }

    public class CardColorComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            return o1.compareColor(o2);
        }
    }

    public class CardQuantityComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            return o1.compareQuantity(o2);
        }
    }
}
