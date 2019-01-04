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
import android.widget.Toast;

import java.util.ArrayList;
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
                    }
                });
            }
        });
        listThread.start();
    }

    private void filterCards(String filter, ListView cardsHolder) {
        ListCardAdapter cardAdapter = (ListCardAdapter) cardsHolder.getAdapter();
        if (filter.length() > 0) {
            ArrayList<ListCard> cards = new ArrayList<>();
            for (ListCard card : allCards) {
                if (card.name.toLowerCase().contains(filter)) {
                    cards.add(card);
                }
            }
            cardAdapter.clear();
            cardAdapter.addAll(cards);
        } else {
            cardAdapter.clear();
            cardAdapter.addAll(allCards);
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
            ListCard card = new ListCard(parts[1], parts[0], parts[3], Integer.parseInt(parts[2]), Integer.parseInt(parts[4]));
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
            TextView costText = convertView.findViewById(R.id.cardCost);
            TextView idText = convertView.findViewById(R.id.cardId);
            TextView cmcText = convertView.findViewById(R.id.cardCMC);

            nameText.setText(card.name);
            quantityText.setText(card.quantity + "");
            costText.setText(card.cost);
            idText.setText(card.id);
            cmcText.setText(card.cmc + "");

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    View layout = View.inflate(getApplicationContext(), R.layout.dialog, null);
                    EditText text = layout.findViewById(R.id.addInput);
                    text.setHint("1-" + card.quantity);
                    text.setHintTextColor(getColor(R.color.greenBG));
                    AlertDialog dialog = new AlertDialog.Builder(ListActivity.this, R.style.Dialog).setTitle("Remove " + card.name).setView(layout).setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText input = ((AlertDialog) dialog).findViewById(R.id.addInput);
                            String text = input.getText().toString();
                            String errorString = "Amount must be between 1-" + card.quantity;
                            if (text.length() > 0) {
                                final int num = Integer.parseInt(text);
                                if (num > 0 && num <= card.quantity) {
                                    toast("Removing " + card.name);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String result = new NetworkHandler(NetworkHandler.Command.RemoveCard, user + ":" + card.id + ":" + num).getString();
                                            if (result.equals("Remove Success")) {
                                                toast("Removed " + card.name);
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

    public class CardQuantityComparator implements Comparator<ListCard> {

        @Override
        public int compare(ListCard o1, ListCard o2) {
            int diff = o1.quantity - o2.quantity;
            if (diff != 0) {
                return diff;
            } else {
                return o1.quantity - o2.quantity;
            }
        }
    }
}
