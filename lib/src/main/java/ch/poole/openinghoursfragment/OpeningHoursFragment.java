package ch.poole.openinghoursfragment;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Event;
import ch.poole.openinghoursparser.Holiday;
import ch.poole.openinghoursparser.Holiday.Type;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.RuleModifier.Modifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.TokenMgrError;
import ch.poole.openinghoursparser.Util;
import ch.poole.openinghoursparser.VarDate;
import ch.poole.openinghoursparser.VariableTime;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
import ch.poole.openinghoursparser.YearRange;
import ch.poole.rangebar.RangeBar;
import ch.poole.rangebar.RangeBar.OnRangeBarChangeListener;
import ch.poole.rangebar.RangeBar.PinTextFormatter;

public class OpeningHoursFragment extends DialogFragment {

    private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();

    private static final String VALUE_KEY = "value";

    private static final String ORIGINAL_VALUE_KEY = "original_value";

    private static final String KEY_KEY = "key";

    private static final String STYLE_KEY = "style";

    private static final String RULE_KEY = "rule";

    protected static final int OSM_MAX_TAG_LENGTH = 255;

    private Context context = null;

    private LayoutInflater inflater = null;

    private TextWatcher watcher;

    private String key;

    private String openingHoursValue;

    private String originalOpeningHoursValue;

    private int styleRes = 0;

    private ArrayList<Rule> rules;

    private EditText text;

    private OnSaveListener saveListener = null;

    List<String> weekDays = WeekDay.nameValues();

    List<String> months = Month.nameValues();

    private SQLiteDatabase mDatabase;

    private transient boolean loadedDefault = false;

    /**
     * True if we encountered a parse error
     */
    private boolean parseErrorFound;

    static PinTextFormatter timeFormater = new PinTextFormatter() {
        @Override
        public String getText(String value) {
            int minutes = Integer.valueOf(value);
            return String.format(Locale.US, "%02d", minutes / 60) + ":"
                    + String.format(Locale.US, "%02d", minutes % 60);
        }
    };

    /**
     * Create a new OpeningHoursFragment
     * 
     * @param key the key the OH values belongs to
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @return an OpeningHoursFragment
     */
    static public OpeningHoursFragment newInstance(@NonNull String key, @NonNull String value, int style, int rule) {
        OpeningHoursFragment f = new OpeningHoursFragment();

        Bundle args = new Bundle();
        args.putSerializable(KEY_KEY, key);
        args.putSerializable(VALUE_KEY, value);
        args.putInt(STYLE_KEY, style);
        args.putInt(RULE_KEY, rule);

        f.setArguments(args);
        ;

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            saveListener = (OnSaveListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSaveListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        mDatabase = new TemplateDatabaseHelper(getContext()).getReadableDatabase();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int initialRule = -1;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            key = savedInstanceState.getString(KEY_KEY);
            openingHoursValue = savedInstanceState.getString(VALUE_KEY);
            originalOpeningHoursValue = savedInstanceState.getString(ORIGINAL_VALUE_KEY);
            styleRes = savedInstanceState.getInt(STYLE_KEY);
        } else {
            key = getArguments().getString(KEY_KEY);
            openingHoursValue = getArguments().getString(VALUE_KEY);
            originalOpeningHoursValue = openingHoursValue;
            styleRes = getArguments().getInt(STYLE_KEY);
            initialRule = getArguments().getInt(RULE_KEY);
        }
        if (styleRes == 0) {
            styleRes = R.style.AlertDialog_AppCompat_Light; // fallback
        }
        if (openingHoursValue == null || "".equals(openingHoursValue)) {
            openingHoursValue = TemplateDatabase.getDefault(mDatabase);
            loadedDefault = true;
        }

        context = new ContextThemeWrapper(getActivity(), styleRes);
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours, null);

        buildLayout(openingHoursLayout, openingHoursValue, initialRule);

        // add callbacks for the buttons
        AppCompatButton cancel = (AppCompatButton) openingHoursLayout.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        saveButton = (AppCompatButton) openingHoursLayout.findViewById(R.id.save);
        enableSaveButton(openingHoursValue);
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveListener.save(key, text.getText().toString());
                dismiss();
            }
        });

        return openingHoursLayout;
    }

    /**
     * Build the parts of the layout that only need to be done once
     * 
     * @param openingHoursLayout the layout
     * @param openingHoursValue the OH value
     * @param initialRule index of the rule to scroll to, currently ignored
     */
    private ScrollView buildLayout(LinearLayout openingHoursLayout, String openingHoursValue, final int initialRule) {
        text = (EditText) openingHoursLayout.findViewById(R.id.openinghours_string_edit);
        final ScrollView sv = (ScrollView) openingHoursLayout.findViewById(R.id.openinghours_view);
        if (text != null && sv != null) {
            text.setText(openingHoursValue);
            sv.removeAllViews();

            watcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    Runnable rebuild = new Runnable() {
                        @Override
                        public void run() {
                            OpeningHoursParser parser = new OpeningHoursParser(
                                    new ByteArrayInputStream(text.getText().toString().getBytes()));
                            try {
                                rules = parser.rules(false);
                                buildForm(sv, rules);
                                removeHighlight(text);
                            } catch (ParseException pex) {
                                Log.d(DEBUG_TAG, pex.getMessage());
                                highlightParseError(text, pex);
                            } catch (TokenMgrError err) {
                                // we currently can't do anything reasonable here except ignore
                                Log.e(DEBUG_TAG, err.getMessage());
                            }
                            enableSaveButton(text.getText().toString());
                        }
                    };
                    text.removeCallbacks(rebuild);
                    if (s != null) {
                        text.postDelayed(rebuild, 500);
                    } else {
                        text.postDelayed(rebuild, 100); // a direct post currently doesn't work
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            };
            text.addTextChangedListener(watcher);

            OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHoursValue.getBytes()));
            try {
                rules = parser.rules(false);
                buildForm(sv, rules);
                removeHighlight(text);
            } catch (ParseException pex) {
                Log.d(DEBUG_TAG, pex.getMessage());
                highlightParseError(text, pex);
            } catch (TokenMgrError err) {
                // we currently can't do anything reasonable here except ignore
                Log.e(DEBUG_TAG, err.getMessage());
            }

            class AddRuleListener implements OnMenuItemClickListener {
                String ruleString;

                AddRuleListener(String rule) {
                    ruleString = rule;
                }

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(ruleString.getBytes()));
                    List<Rule> rules2 = null;
                    if (parser != null) {
                        try {
                            rules2 = parser.rules(false);
                        } catch (ParseException pex) {
                            Log.e(DEBUG_TAG, pex.getMessage());
                        } catch (TokenMgrError err) {
                            Log.e(DEBUG_TAG, err.getMessage());
                        }
                        if (rules2 != null && !rules2.isEmpty()) {
                            if (rules==null || hasParseError()) { // if there was an unparseable string it needs to be fixed first 
                                ch.poole.openinghoursfragment.Util.toastTop(getActivity(), R.string.would_overwrite_invalid_value);
                                return true;
                            }
                            rules.add(rules2.get(0));
                            updateString();
                            watcher.afterTextChanged(null); // hack to force rebuild of form
                            text.postDelayed(new Runnable() { // use post to ensure view has been rebuilt
                                @Override
                                public void run() {
                                    ch.poole.openinghoursfragment.Util.scrollToRow(sv, null, false, false); // scroll to
                                    // bottom
                                }
                            }, 200);

                        }
                    } 
                    return true;
                }
            }

            final FloatingActionButton fab = (FloatingActionButton) openingHoursLayout.findViewById(R.id.add);
            fab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu popup = new PopupMenu(context, fab);

                    // menu items for adding rules
                    MenuItem addRule = popup.getMenu().add(R.string.add_rule);
                    addRule.setOnMenuItemClickListener(new AddRuleListener("Mo 6:00-20:00"));
                    MenuItem addRulePH = popup.getMenu().add(R.string.add_rule_closed_on_holidays);
                    addRulePH.setOnMenuItemClickListener(new AddRuleListener("PH closed"));
                    MenuItem addRule247 = popup.getMenu().add(R.string.add_rule_247);
                    addRule247.setOnMenuItemClickListener(new AddRuleListener("24/7"));

                    MenuItem loadTemplate = popup.getMenu().add(R.string.load_template);
                    loadTemplate.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            loadOrManageTemplate(context, false);
                            return true;
                        }
                    });
                    MenuItem saveTemplate = popup.getMenu().add(R.string.save_to_template);
                    saveTemplate.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            showTemplateDialog(new TemplateDatabaseHelper(context).getWritableDatabase(), false, -1);
                            return true;
                        }
                    });
                    MenuItem manageTemplate = popup.getMenu().add(R.string.manage_template);
                    manageTemplate.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            loadOrManageTemplate(context, true);
                            return true;
                        }
                    });
                    MenuItem refresh = popup.getMenu().add(R.string.refresh);
                    refresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            updateString();
                            watcher.afterTextChanged(null); // hack to force rebuild of form
                            return true;
                        }
                    });
                    popup.show();// showing popup menu
                }
            });
        }
        return sv;
    }

    /**
     * Check if we have an parser error
     * 
     * @return true if there was a parser error
     */
    public boolean hasParseError() {
        return parseErrorFound;
    }

    /**
     * Highlight the position of a parse error
     * 
     * Side effect sets parserErrorFound to true
     * @param text he EditText the string is displayed in
     * @param pex the ParseException to use
     */
    private void highlightParseError(EditText text, ParseException pex) {
        parseErrorFound = true;
        int c = pex.currentToken.next.beginColumn - 1; // starts at 1
        int pos = text.getSelectionStart();
        Spannable spannable = new SpannableString(text.getText());
        spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c, Math.min(c + 1, spannable.length())),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.removeTextChangedListener(watcher); // avoid infinite loop
        text.setText(spannable, TextView.BufferType.SPANNABLE);
        text.setSelection(Math.min(pos, spannable.length()));
        text.addTextChangedListener(watcher);
    }

    /**
     * Remove all parse error highlighting
     * 
     * Side effect sets parserErrorFound to false
     * @param text the EditText the string is displayed in
     */
    private void removeHighlight(EditText text) {
        parseErrorFound = false;
        int pos = text.getSelectionStart();
        int prevLen = text.length();
        text.removeTextChangedListener(watcher); // avoid infinite loop
        String t = Util.rulesToOpeningHoursString(rules);
        text.setText(t);
        // text.setText(text.getText().toString());
        text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos, text.length()));
        text.addTextChangedListener(watcher);
    }

    /**
     * (Re-)Build the contents of the scroll view
     * 
     * @param sv the ScrollView
     * @param rules List of Rules to display
     */
    private synchronized void buildForm(@NonNull ScrollView sv, @NonNull List<Rule> rules) {
        sv.removeAllViews();
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setPadding(0, 0, 0, dpToPixels(64));
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);
        addRules(false, rules, ll);
    }

    /**
     * Loop over the list of rules adding views and menus for the entries
     * 
     * @param groupMode use groupMode, currently ignored
     * @param rules List of Rules to display
     * @param ll layout to add the wules to
     */
    private void addRules(boolean groupMode, @NonNull final List<Rule> rules, @NonNull LinearLayout ll) {
        boolean first = true;
        int headerCount = 1;
        for (final Rule r : rules) {
            if (first) { // everything except days and times should be
                         // the same and only needs to be displayed
                         // once in groupMode, in normal mode this is
                         // always true
                final LinearLayout groupHeader = (LinearLayout) inflater.inflate(R.layout.rule_header, null);
                TextView header = (TextView) groupHeader.findViewById(R.id.header);
                header.setText(
                        getActivity().getString(groupMode ? R.string.group_header : R.string.rule_header, headerCount));
                RadioButton normal = (RadioButton) groupHeader.findViewById(R.id.normal_rule);
                if (!r.isAdditive() && !r.isFallBack()) {
                    normal.setChecked(true);
                }
                normal.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RadioButton rb = (RadioButton) v;
                        if (rb.isChecked()) {
                            r.setFallBack(false);
                            r.setAdditive(false);
                            updateString();
                            watcher.afterTextChanged(null);
                        }
                    }
                });
                RadioButton additive = (RadioButton) groupHeader.findViewById(R.id.additive_rule);
                if (r.isAdditive()) {
                    additive.setChecked(true);
                }
                additive.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RadioButton rb = (RadioButton) v;
                        if (rb.isChecked()) {
                            r.setFallBack(false);
                            r.setAdditive(true);
                            updateString();
                            watcher.afterTextChanged(null);
                        }
                    }
                });
                RadioButton fallback = (RadioButton) groupHeader.findViewById(R.id.fallback_rule);
                if (r.isFallBack()) {
                    fallback.setChecked(true);
                }
                fallback.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RadioButton rb = (RadioButton) v;
                        if (rb.isChecked()) {
                            r.setFallBack(true);
                            r.setAdditive(false);
                            rules.remove(r); // move to last position
                            rules.add(r);
                            updateString();
                            watcher.afterTextChanged(null);
                        }
                    }
                });

                // add menu items for rules
                Menu menu = addStandardMenuItems(groupHeader, new Delete() {
                    @Override
                    public void delete() {
                        rules.remove(r);
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    }
                });

                if (r.getModifier() == null) {
                    MenuItem addModifierAndComment = menu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                            R.string.add_modifier_comment);
                    addModifierAndComment.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            RuleModifier modifier = new RuleModifier();
                            modifier.setModifier(Modifier.CLOSED);
                            r.setModifier(modifier);
                            updateString();
                            watcher.afterTextChanged(null);
                            return true;
                        }
                    });
                }

                MenuItem addHoliday = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_holiday);
                addHoliday.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<Holiday> holidays = r.getHolidays();
                        if (holidays == null) {
                            r.setHolidays(new ArrayList<Holiday>());
                            holidays = r.getHolidays();
                        }
                        Holiday holiday = new Holiday();
                        holiday.setType(Type.PH);
                        holidays.add(holiday);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                SubMenu timespanMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.timespan_menu);
                MenuItem addTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_time);
                addTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        t.setStart(360);
                        t.setEnd(1440);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addExtendedTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.time_extended_time);
                addExtendedTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        t.setStart(0);
                        t.setEnd(2880);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addVarTimeTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_time_time);
                addVarTimeTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        VariableTime vt = new VariableTime();
                        vt.setEvent(Event.DAWN);
                        t.setStartEvent(vt);
                        t.setEnd(1440);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addVarTimeExtendedTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_time_extended_time);
                addVarTimeExtendedTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        VariableTime vt = new VariableTime();
                        vt.setEvent(Event.DAWN);
                        t.setStartEvent(vt);
                        t.setEnd(2880);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addTimeVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.time_variable_time);
                addTimeVarTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        t.setStart(360);
                        VariableTime vt = new VariableTime();
                        vt.setEvent(Event.DUSK);
                        t.setEndEvent(vt);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addVarTimeVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_time_variable_time);
                addVarTimeVarTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        VariableTime startVt = new VariableTime();
                        startVt.setEvent(Event.DAWN);
                        t.setStartEvent(startVt);
                        ;
                        VariableTime endVt = new VariableTime();
                        endVt.setEvent(Event.DUSK);
                        t.setEndEvent(endVt);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time);
                addTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        t.setStart(360);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addTimeOpenEndTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.time_open_end);
                addTimeOpenEndTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        t.setStart(360);
                        t.setOpenEnded(true);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time);
                addVarTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        VariableTime startVt = new VariableTime();
                        startVt.setEvent(Event.DAWN);
                        t.setStartEvent(startVt);
                        ;
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addVarTimeOpenEndTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_time_open_end);
                addVarTimeOpenEndTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<TimeSpan>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        VariableTime startVt = new VariableTime();
                        startVt.setEvent(Event.DAWN);
                        t.setStartEvent(startVt);
                        ;
                        t.setOpenEnded(true);
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addWeekdayRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.week_day_range);
                addWeekdayRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<WeekDayRange> wd = r.getDays();
                        if (wd == null) {
                            r.setDays(new ArrayList<WeekDayRange>());
                            wd = r.getDays();
                        }
                        WeekDayRange d = new WeekDayRange();
                        if (wd.isEmpty()) {
                            d.setStartDay("Mo");
                            // d.setEndDay("Su"); a single day is better from an UI pov
                        } else {
                            // add a single day with nth
                            d.setStartDay("Mo");
                            List<Nth> nths = new ArrayList<Nth>();
                            Nth nth = new Nth();
                            nth.setStartNth(1);
                            nths.add(nth);
                            d.setNths(nths);
                        }
                        wd.add(d);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                SubMenu dateRangeMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.daterange_menu);
                MenuItem addDateDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_date);
                addDateDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset dwo = new DateWithOffset();
                        dwo.setMonth(Month.JAN);
                        dateRange.setStartDate(dwo);
                    }
                }));
                MenuItem addVarDateDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_date);
                addVarDateDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset dwo = new DateWithOffset();
                        dwo.setVarDate(VarDate.EASTER);
                        dateRange.setStartDate(dwo);
                    }
                }));
                MenuItem addDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.date_variable_date);
                addDateVarDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset startDwo = new DateWithOffset();
                        startDwo.setMonth(Month.JAN);
                        dateRange.setStartDate(startDwo);
                        DateWithOffset endDwo = new DateWithOffset();
                        endDwo.setVarDate(VarDate.EASTER);
                        dateRange.setEndDate(endDwo);
                    }
                }));
                MenuItem addVarDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_variable_date);
                addVarDateVarDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset startDwo = new DateWithOffset();
                        startDwo.setVarDate(VarDate.EASTER);
                        dateRange.setStartDate(startDwo);
                        DateWithOffset endDwo = new DateWithOffset();
                        endDwo.setVarDate(VarDate.EASTER);
                        dateRange.setEndDate(endDwo);
                    }
                }));
                MenuItem addDateOpenEndRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.date_open_end);
                addDateOpenEndRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset startDwo = new DateWithOffset();
                        startDwo.setMonth(Month.JAN);
                        startDwo.setOpenEnded(true);
                        dateRange.setStartDate(startDwo);
                    }
                }));
                MenuItem addVarDateOpenEndRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_open_end);
                addVarDateOpenEndRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                    @Override
                    public void addDates(DateRange dateRange) {
                        DateWithOffset startDwo = new DateWithOffset();
                        startDwo.setVarDate(VarDate.EASTER);
                        startDwo.setOpenEnded(true);
                        dateRange.setStartDate(startDwo);
                    }
                }));

                // the same with offsets
                SubMenu dateRangeOffsetMenu = dateRangeMenu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.with_offsets);
                MenuItem addDateDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.date_date);
                addDateDateRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset dwo = new DateWithOffset();
                                dwo.setMonth(Month.JAN);
                                dwo.setDay(1);
                                dwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(dwo);
                            }
                        }));
                MenuItem addVarDateDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_date);
                addVarDateDateRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset dwo = new DateWithOffset();
                                dwo.setVarDate(VarDate.EASTER);
                                dwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(dwo);
                            }
                        }));
                MenuItem addDateVarDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.date_variable_date);
                addDateVarDateRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset startDwo = new DateWithOffset();
                                startDwo.setMonth(Month.JAN);
                                startDwo.setDay(1);
                                startDwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(startDwo);
                                DateWithOffset endDwo = new DateWithOffset();
                                endDwo.setVarDate(VarDate.EASTER);
                                dateRange.setEndDate(endDwo);
                            }
                        }));
                MenuItem addVarDateVarDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_variable_date);
                addVarDateVarDateRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset startDwo = new DateWithOffset();
                                startDwo.setVarDate(VarDate.EASTER);
                                startDwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(startDwo);
                                DateWithOffset endDwo = new DateWithOffset();
                                endDwo.setVarDate(VarDate.EASTER);
                                dateRange.setEndDate(endDwo);
                            }
                        }));
                MenuItem addDateOpenEndRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.date_open_end);
                addDateOpenEndRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset startDwo = new DateWithOffset();
                                startDwo.setMonth(Month.JAN);
                                startDwo.setDay(1);
                                startDwo.setOpenEnded(true);
                                startDwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(startDwo);
                            }
                        }));
                MenuItem addVarDateOpenEndRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.variable_date_open_end);
                addVarDateOpenEndRangeWithOffsets
                        .setOnMenuItemClickListener(new DateRangeMenuListener(r, new DateMenuInterface() {
                            @Override
                            public void addDates(DateRange dateRange) {
                                DateWithOffset startDwo = new DateWithOffset();
                                startDwo.setVarDate(VarDate.EASTER);
                                startDwo.setOpenEnded(true);
                                startDwo.setWeekDayOffset(WeekDay.MO);
                                dateRange.setStartDate(startDwo);
                            }
                        }));

                MenuItem addYearRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_year_range);
                addYearRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<YearRange> years = r.getYears();
                        if (years == null) {
                            r.setYears(new ArrayList<YearRange>());
                            years = r.getYears();
                        }
                        YearRange yearRange = new YearRange();
                        yearRange.setStartYear(2000);
                        years.add(yearRange);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                MenuItem addWeekRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_week_range);
                addWeekRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<WeekRange> weeks = r.getWeeks();
                        if (weeks == null) {
                            r.setWeeks(new ArrayList<WeekRange>());
                            weeks = r.getWeeks();
                        }
                        WeekRange weekRange = new WeekRange();
                        weekRange.setStartWeek(1);
                        weeks.add(weekRange);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                });

                if (!r.equals(rules.get(0))) { // don't show this for the first rule as it is meaningless
                    final View typeView = groupHeader.findViewById(R.id.rule_type_group);
                    MenuItem showRuleType = menu.add(R.string.rule_type);
                    showRuleType.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            typeView.setVisibility(View.VISIBLE);
                            ;
                            groupHeader.invalidate();
                            return true;
                        }
                    });
                    MenuItem moveUp = menu.add(R.string.move_up);
                    moveUp.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int current = rules.indexOf(r);
                            if (current < 0) { // not found shouldn't happen
                                Log.e(DEBUG_TAG, "Rule missing from list!");
                                return true;
                            }
                            rules.remove(current);
                            rules.add(Math.max(0, current - 1), r);
                            updateString();
                            watcher.afterTextChanged(null);
                            return true;
                        }
                    });
                }
                if (!r.equals(rules.get(rules.size() - 1))) {
                    MenuItem moveDown = menu.add(R.string.move_down);
                    moveDown.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int current = rules.indexOf(r);
                            if (current < 0) { // not found shouldn't happen
                                Log.e(DEBUG_TAG, "Rule missing from list!");
                                return true;
                            }
                            int size = rules.size();
                            rules.remove(current);
                            rules.add(Math.min(size - 1, current + 1), r);
                            updateString();
                            watcher.afterTextChanged(null);
                            return true;
                        }
                    });
                }
                ll.addView(groupHeader);
                // comments
                String comment = r.getComment();
                if (comment != null && !"".equals(comment)) {
                    LinearLayout intervalLayout = (LinearLayout) inflater.inflate(R.layout.comment, null);
                    EditText commentComment = (EditText) intervalLayout.findViewById(R.id.comment);
                    commentComment.setText(comment);
                    setTextWatcher(commentComment, new SetValue() {
                        @Override
                        public void set(String value) {
                            r.setComment(value);
                        }
                    });
                    addStandardMenuItems(intervalLayout, null);
                    ll.addView(intervalLayout);
                }
                if (r.isTwentyfourseven()) {
                    Log.d(DEBUG_TAG, "24/7 " + r.toString());
                    LinearLayout twentyFourSeven = (LinearLayout) inflater.inflate(R.layout.twentyfourseven, null);
                    addStandardMenuItems(twentyFourSeven, new Delete() {
                        @Override
                        public void delete() {
                            r.setTwentyfourseven(false);
                            updateString();
                            watcher.afterTextChanged(null); // hack to force rebuild of form
                        }
                    });
                    ll.addView(twentyFourSeven);
                }
                // year range list
                final List<YearRange> years = r.getYears();
                if (years != null && years.size() > 0) {
                    for (final YearRange yr : years) {
                        addYearRangeUI(ll, r, years, yr);
                    }
                }
                // week range list
                final List<WeekRange> weeks = r.getWeeks();
                if (weeks != null && weeks.size() > 0) {
                    for (final WeekRange w : weeks) {
                        addWeekRangeUI(ll, r, weeks, w);
                    }
                }
                // date range list
                final List<DateRange> dateRanges = r.getDates();
                if (dateRanges != null && dateRanges.size() > 0) {
                    for (final DateRange dateRange : dateRanges) {
                        addDateRangeUI(ll, r, dateRanges, dateRange);
                    }
                }

                // only used in group mode
                if (groupMode) {
                    first = false;
                }
                headerCount++;
            }

            // days and times will be different per rule
            // are supposedly pseudo days
            // holiday list
            final List<Holiday> holidays = r.getHolidays();
            if (holidays != null && holidays.size() > 0) {
                for (final Holiday hd : holidays) {
                    addHolidayUI(ll, r, holidays, hd);
                }
            }
            // week day list
            final List<WeekDayRange> days = r.getDays();
            if (days != null && days.size() > 0) {
                addWeekDayUI(ll, days);
            }

            // times
            List<TimeSpan> times = r.getTimes();
            addTimeSpanUIs(ll, times);

            // Modifier
            final RuleModifier rm = r.getModifier();
            if (rm != null) {
                LinearLayout modifierLayout = (LinearLayout) inflater.inflate(R.layout.modifier, null);
                final Spinner modifierSpinner = (Spinner) modifierLayout.findViewById(R.id.modifier);
                setSpinnerInitialEntryValue(R.array.modifier_values, modifierSpinner, rm.getModifier().toString());
                setSpinnerListenerEntryValues(R.array.modifier_values, modifierSpinner, new SetValue() {
                    @Override
                    public void set(String value) {
                        rm.setModifier(value);
                    }
                });
                EditText modifierComment = (EditText) modifierLayout.findViewById(R.id.comment);
                modifierComment.setText(rm.getComment());
                setTextWatcher(modifierComment, new SetValue() {
                    @Override
                    public void set(String value) {
                        rm.setComment(value);
                    }
                });
                addStandardMenuItems(modifierLayout, new Delete() {
                    @Override
                    public void delete() {
                        r.setModifier(null);
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    }
                });
                ll.addView(modifierLayout);
            }
        }
    }

    /**
     * Helper class to reduce code duplication when adding date range menu items
     * 
     * @author simon
     *
     */
    class DateRangeMenuListener implements OnMenuItemClickListener {
        final DateMenuInterface datesAdder;
        final Rule r;

        DateRangeMenuListener(@NonNull Rule r, @NonNull DateMenuInterface datesAdder) {
            this.r = r;
            this.datesAdder = datesAdder;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            List<DateRange> mdr = r.getDates();
            if (mdr == null) {
                r.setMonthdays(new ArrayList<DateRange>());
                mdr = r.getDates();
            }
            DateRange dateRange = new DateRange();
            datesAdder.addDates(dateRange);
            mdr.add(dateRange);
            updateString();
            watcher.afterTextChanged(null);
            return true;
        }
    }

    private void addWeekDayUI(@NonNull LinearLayout ll, @NonNull final List<WeekDayRange> days) {
        final LinearLayout weekDayRow = (LinearLayout) inflater.inflate(R.layout.weekday_range_row, null);
        final RelativeLayout weekDayContainer = (RelativeLayout) weekDayRow.findViewById(R.id.weekDayContainer);

        Menu menu = addStandardMenuItems(weekDayRow, new Delete() {
            @Override
            public void delete() {
                List<WeekDayRange> temp = new ArrayList<WeekDayRange>(days);
                for (WeekDayRange d : temp) {
                    if (d.getNths() == null) {
                        days.remove(d);
                    }
                }
                updateString();
                watcher.afterTextChanged(null); // hack to force rebuild of form
            }
        });
        // menu item will be enabled/disabled depending on number of days etc.
        MenuItem nthMenuItem = menu.add(R.string.occurrence_in_month);
        nthMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                LinearLayout nthLayout = (LinearLayout) inflater.inflate(R.layout.nth, null);
                RelativeLayout nthContainer = (RelativeLayout) nthLayout.findViewById(R.id.nthContainer);
                setNthListeners(nthContainer, days.get(0));
                weekDayRow.addView(nthLayout);
                item.setEnabled(false);
                return true;
            }
        });

        List<WeekDayRange> normal = new ArrayList<WeekDayRange>();
        List<WeekDayRange> withNth = new ArrayList<WeekDayRange>();
        for (WeekDayRange d : days) {
            if (d.getNths() == null || d.getNths().isEmpty()) {
                normal.add(d);
            } else {
                withNth.add(d);
            }
        }

        if (!normal.isEmpty()) {
            for (WeekDayRange d : normal) {
                WeekDay startDay = d.getStartDay();
                WeekDay endDay = d.getEndDay();
                if (endDay == null) {
                    checkWeekDay(weekDayContainer, startDay.toString());
                } else {
                    int startIndex = startDay.ordinal();
                    int endIndex = endDay.ordinal();
                    Log.d(DEBUG_TAG, "startDay " + startDay + " " + startIndex + " endDay " + endDay + " " + endIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        checkWeekDay(weekDayContainer, weekDays.get(i));
                    }
                }
            }
            setWeekDayListeners(weekDayContainer, days, normal, false, nthMenuItem);
            nthMenuItem.setEnabled(justOneDay(normal));
            ll.addView(weekDayRow);
        }

        if (!withNth.isEmpty()) {
            for (final WeekDayRange d : withNth) {
                final LinearLayout weekDayRowNth = (LinearLayout) inflater.inflate(R.layout.weekday_range_row, null);
                final RelativeLayout weekDayContainerNth = (RelativeLayout) weekDayRowNth
                        .findViewById(R.id.weekDayContainer);
                WeekDay startDay = d.getStartDay();
                List<Nth> nths = d.getNths();
                checkWeekDay(weekDayContainerNth, startDay.toString());
                nthMenuItem.setEnabled(false);
                LinearLayout nthLayout = (LinearLayout) inflater.inflate(R.layout.nth, null);
                RelativeLayout nthContainer = (RelativeLayout) nthLayout.findViewById(R.id.nthContainer);
                for (Nth nth : nths) {
                    Log.d(DEBUG_TAG, "adding nth " + nth.toString());
                    int startNth = nth.getStartNth();
                    int endNth = nth.getEndNth();
                    if (endNth < 1) {
                        checkNth(nthContainer, startNth);
                    } else {
                        for (int i = startNth; i <= endNth; i++) {
                            checkNth(nthContainer, i);
                        }
                    }
                }
                setWeekDayListeners(weekDayContainerNth, days, withNth, true, nthMenuItem);
                setNthListeners(nthContainer, d);
                weekDayRowNth.addView(nthLayout);
                menu = addStandardMenuItems(weekDayRowNth, new Delete() {
                    @Override
                    public void delete() {
                        days.remove(d);
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    }
                });

                final View offsetContainer = weekDayRowNth.findViewById(R.id.offset_container);
                if (d.getOffset() != 0) {
                    offsetContainer.setVisibility(View.VISIBLE);
                } else {
                    offsetContainer.setVisibility(View.GONE);
                    MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
                    showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            offsetContainer.setVisibility(View.VISIBLE);
                            return true;
                        }
                    });
                }

                EditText offset = (EditText) nthLayout.findViewById(R.id.offset);
                offset.setText(Integer.toString(d.getOffset()));
                setTextWatcher(offset, new SetValue() {
                    @Override
                    public void set(String value) {
                        int offset = 0;
                        try {
                            offset = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                        }
                        d.setOffset(offset);
                    }
                });

                ll.addView(weekDayRowNth);
            }
        }
    }

    private void addDateRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r,
            @NonNull final List<DateRange> dateRanges, final DateRange dateRange) {
        // cases to consider
        // date - date
        // vardate - date
        // date - vardate
        // vardate - vardate
        // date open ended
        // vardate open ended
        //
        // and the same with offsets
        final DateWithOffset startDate = dateRange.getStartDate();
        final DateWithOffset endDate = dateRange.getEndDate();

        boolean hasStartOffset = startDate.getWeekDayOffset() != null || startDate.getDayOffset() != 0;
        boolean hasEndOffset = endDate != null && (endDate.getWeekDayOffset() != null || startDate.getDayOffset() != 0);
        final LinearLayout dateRangeRow = (LinearLayout) inflater.inflate(
                hasStartOffset || hasEndOffset ? R.layout.daterange_with_offset_row : R.layout.daterange_row, null);

        // add menus
        final Menu menu = addStandardMenuItems(dateRangeRow, new Delete() {
            @Override
            public void delete() {
                dateRanges.remove(dateRange);
                if (dateRanges.isEmpty()) {
                    r.setMonthdays(null);
                }
                updateString();
                watcher.afterTextChanged(null); // hack to force rebuild of form
            }
        });

        if (hasStartOffset || hasEndOffset) {
            LinearLayout startDateLayout = (LinearLayout) dateRangeRow.findViewById(R.id.startDate);
            LinearLayout endDateLayout = (LinearLayout) dateRangeRow.findViewById(R.id.endDate);
            setDateRangeValues(startDate, endDate, dateRangeRow, menu);
            startDateLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final DateWithOffset start = startDate;
                    if (start.getVarDate() == null) {
                        DateRangePicker.showDialog(getActivity(), R.string.date, start.getYear(), start.getMonth(),
                                start.getDay(), new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        start.setYear(startYear);
                                        start.setMonth(startMonth);
                                        start.setDay(startDay);
                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(start, null, finalRow, menu);
                                        updateString();
                                    }
                                });
                    } else {
                        DateRangePicker.showDialog(getActivity(), R.string.date, start.getYear(), start.getVarDate(),
                                new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        start.setYear(startYear);
                                        start.setVarDate(startVarDate);
                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(start, null, finalRow, menu);
                                        updateString();
                                    }
                                });
                    }
                }
            });
            endDateLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (endDate == null || endDate.getVarDate() == null) {
                        final DateWithOffset end;
                        if (endDate == null) {
                            end = new DateWithOffset();
                            end.setMonth(Month.JAN);
                        } else {
                            end = endDate;
                        }
                        DateRangePicker.showDialog(getActivity(), R.string.date, end.getYear(), end.getMonth(),
                                end.getDay(), new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        // we are only using the first NPV thats why we need to use the startXXX values
                                        // here
                                        DateWithOffset tempDwo = endDate;
                                        if (tempDwo == null
                                                && (startYear != YearRange.UNDEFINED_YEAR || startMonth != null
                                                        || startDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
                                            tempDwo = new DateWithOffset();
                                            dateRange.setEndDate(tempDwo);
                                        }
                                        if (tempDwo != null) {
                                            tempDwo.setYear(startYear);
                                            tempDwo.setMonth(startMonth);
                                            tempDwo.setDay(startDay);
                                        }
                                        end.setYear(startYear);
                                        end.setMonth(startMonth);
                                        end.setDay(startDay);
                                        Log.d(DEBUG_TAG, "y " + startYear + " m " + startMonth + " d" + startDay);
                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(startDate, tempDwo, finalRow, menu);
                                        updateString();
                                        if (endDate == null) {
                                            text.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    watcher.afterTextChanged(null); // force endDate to be set
                                                }
                                            }, 100);
                                        }
                                    }
                                });
                    } else {
                        DateRangePicker.showDialog(getActivity(), R.string.date, endDate.getYear(),
                                endDate.getVarDate(), new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        // we are only using the first NPV thats why we need to use the startXXX values
                                        // here
                                        endDate.setYear(startYear);
                                        endDate.setVarDate(startVarDate);
                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(startDate, endDate, finalRow, menu);
                                        updateString();
                                    }
                                });
                    }
                }
            });
        } else {
            setDateRangeValues(startDate, endDate, dateRangeRow, menu);
            RelativeLayout dateRangeLayout = (RelativeLayout) dateRangeRow.findViewById(R.id.daterange_container);
            dateRangeLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int tempEndYear = YearRange.UNDEFINED_YEAR;
                    Month tempEndMonth = null;
                    int tempEndDay = DateWithOffset.UNDEFINED_MONTH_DAY;

                    final DateWithOffset start = startDate;
                    DateWithOffset end = endDate;

                    if (end != null) {
                        tempEndYear = end.getYear();
                        tempEndMonth = end.getMonth();
                        tempEndDay = end.getDay();
                    }
                    if (start.getVarDate() == null && (end == null || end.getVarDate() == null)) {
                        if (start.isOpenEnded()) {
                            DateRangePicker.showDialog(getActivity(), R.string.date, start.getYear(), start.getMonth(),
                                    start.getDay(), new SetDateRangeListener() {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void setDateRange(int startYear, Month startMonth, int startDay,
                                                VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                                VarDate endVarDate) {
                                            start.setYear(startYear);
                                            start.setMonth(startMonth);
                                            start.setDay(startDay);
                                            final LinearLayout finalRow = dateRangeRow;
                                            setDateRangeValues(start, null, finalRow, menu);
                                            updateString();
                                        }
                                    });
                        } else {
                            DateRangePicker.showDialog(getActivity(), R.string.date_range, start.getYear(),
                                    start.getMonth(), start.getDay(), tempEndYear, tempEndMonth, tempEndDay,
                                    new SetDateRangeListener() {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void setDateRange(int startYear, Month startMonth, int startDay,
                                                VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                                VarDate endVarDate) {
                                            start.setYear(startYear);
                                            start.setMonth(startMonth);
                                            start.setDay(startDay);
                                            DateWithOffset tempDwo = endDate;
                                            if (tempDwo == null
                                                    && (endYear != YearRange.UNDEFINED_YEAR || endMonth != null
                                                            || endDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
                                                tempDwo = new DateWithOffset();
                                                dateRange.setEndDate(tempDwo);
                                            }
                                            if (tempDwo != null) {
                                                tempDwo.setYear(endYear);
                                                tempDwo.setMonth(endMonth);
                                                tempDwo.setDay(endDay);
                                            }
                                            final LinearLayout finalRow = dateRangeRow;
                                            setDateRangeValues(start, tempDwo, finalRow, menu);
                                            updateString();
                                            if (endDate == null) {
                                                text.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        watcher.afterTextChanged(null); // force endDate to be set
                                                    }
                                                }, 100);
                                            }
                                        }
                                    });
                        }
                    } else if (start.getVarDate() != null && (end == null || end.getVarDate() == null)) {
                        if (start.isOpenEnded()) {
                            DateRangePicker.showDialog(getActivity(), R.string.date, start.getYear(),
                                    start.getVarDate(), new SetDateRangeListener() {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void setDateRange(int startYear, Month startMonth, int startDay,
                                                VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                                VarDate endVarDate) {
                                            start.setYear(startYear);
                                            start.setVarDate(startVarDate);
                                            final LinearLayout finalRow = dateRangeRow;
                                            setDateRangeValues(start, null, finalRow, menu);
                                            updateString();
                                        }
                                    });
                        } else {
                            DateRangePicker.showDialog(getActivity(), R.string.date_range, start.getYear(),
                                    start.getVarDate(), tempEndYear, tempEndMonth, tempEndDay,
                                    new SetDateRangeListener() {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        public void setDateRange(int startYear, Month startMonth, int startDay,
                                                VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                                VarDate endVarDate) {
                                            start.setYear(startYear);
                                            start.setVarDate(startVarDate);
                                            DateWithOffset tempDwo = endDate;
                                            if (tempDwo == null
                                                    && (endYear != YearRange.UNDEFINED_YEAR || endMonth != null
                                                            || endDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
                                                tempDwo = new DateWithOffset();
                                                dateRange.setEndDate(tempDwo);
                                            }
                                            if (tempDwo != null) {
                                                tempDwo.setYear(endYear);
                                                tempDwo.setMonth(endMonth);
                                                tempDwo.setDay(endDay);
                                            }
                                            final LinearLayout finalRow = dateRangeRow;
                                            setDateRangeValues(start, tempDwo, finalRow, menu);
                                            updateString();
                                            if (endDate == null) {
                                                text.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        watcher.afterTextChanged(null); // force endDate to be set
                                                    }
                                                }, 100);
                                            }
                                        }
                                    });
                        }
                    } else if (start.getVarDate() == null && (end != null && end.getVarDate() != null)) {
                        DateRangePicker.showDialog(getActivity(), R.string.date_range, start.getYear(),
                                start.getMonth(), start.getDay(), tempEndYear, end.getVarDate(),
                                new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        start.setYear(startYear);
                                        start.setMonth(startMonth);
                                        start.setDay(startDay);

                                        endDate.setYear(endYear);
                                        endDate.setVarDate(endVarDate);

                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(start, endDate, finalRow, menu);
                                        updateString();
                                    }
                                });
                    } else if (start.getVarDate() != null && (end != null && end.getVarDate() != null)) {
                        DateRangePicker.showDialog(getActivity(), R.string.date_range, start.getYear(),
                                start.getVarDate(), tempEndYear, end.getVarDate(), new SetDateRangeListener() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void setDateRange(int startYear, Month startMonth, int startDay,
                                            VarDate startVarDate, int endYear, Month endMonth, int endDay,
                                            VarDate endVarDate) {
                                        start.setYear(startYear);
                                        start.setVarDate(startVarDate);

                                        endDate.setYear(endYear);
                                        endDate.setVarDate(endVarDate);

                                        final LinearLayout finalRow = dateRangeRow;
                                        setDateRangeValues(start, endDate, finalRow, menu);
                                        updateString();
                                    }
                                });
                    }
                }
            });
        }
        ll.addView(dateRangeRow);
    }

    private void setDateRangeValues(@NonNull final DateWithOffset start, @Nullable final DateWithOffset end,
            @NonNull LinearLayout dateRangeRow, @NonNull Menu menu) {
        TextView startYearView = (TextView) dateRangeRow.findViewById(R.id.startYear);
        if (start.getYear() != YearRange.UNDEFINED_YEAR) {
            startYearView.setText(Integer.toString(start.getYear()));
        } else {
            startYearView.setText("");
        }
        TextView startDelimiter = (TextView) dateRangeRow.findViewById(R.id.startMonthDayDelimiter);
        TextView startMonthView = (TextView) dateRangeRow.findViewById(R.id.startMonth);
        TextView startDayView = (TextView) dateRangeRow.findViewById(R.id.startMonthDay);
        if (start.getVarDate() == null) {
            startDelimiter.setVisibility(View.VISIBLE);
            startDayView.setVisibility(View.VISIBLE);

            if (start.getMonth() != null) {
                startMonthView
                        .setText(getResources().getStringArray(R.array.months_entries)[start.getMonth().ordinal()]);
            }

            if (start.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                startDayView.setText(Integer.toString(start.getDay()));
            } else {
                startDayView.setText("");
            }
        } else {
            startDelimiter.setVisibility(View.GONE);
            startDayView.setVisibility(View.GONE);
            startMonthView
                    .setText(getResources().getStringArray(R.array.vardate_entries)[start.getVarDate().ordinal()]);
        }
        // offset stuff
        RelativeLayout startWeekDayContainer = (RelativeLayout) dateRangeRow.findViewById(R.id.startWeekDayContainer);
        if (startWeekDayContainer != null) {
            checkWeekDay(startWeekDayContainer,
                    start.getWeekDayOffset() != null ? start.getWeekDayOffset().toString() : null);
            setWeekDayListeners(startWeekDayContainer, start);
            final Spinner offsetTypeSpinner = (Spinner) dateRangeRow.findViewById(R.id.startOffsetType);
            setSpinnerInitialEntryValue(R.array.offset_type_values, offsetTypeSpinner,
                    start.isWeekDayOffsetPositive() ? "+" : "-");
            setSpinnerListenerEntryValues(R.array.offset_type_values, offsetTypeSpinner, new SetValue() {
                @Override
                public void set(String value) {
                    start.setWeekDayOffsetPositive("+".equals(value));
                }
            });
        }
        final LinearLayout startOffsetContainer = (LinearLayout) dateRangeRow.findViewById(R.id.start_offset_container);
        if (startOffsetContainer != null) {
            EditText offset = (EditText) startOffsetContainer.findViewById(R.id.start_offset);
            setTextWatcher(offset, new SetValue() {
                @Override
                public void set(String value) {
                    int offset = 0;
                    try {
                        offset = Integer.parseInt(value);
                    } catch (NumberFormatException nfex) {
                    }
                    start.setDayOffset(offset);
                }
            });
            if (start.getDayOffset() != 0) {
                offset.setText(Integer.toString(start.getDayOffset()));
                startOffsetContainer.setVisibility(View.VISIBLE);
            } else {
                startOffsetContainer.setVisibility(View.GONE);
                MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_start_offset);
                showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startOffsetContainer.setVisibility(View.VISIBLE);
                        return true;
                    }
                });
            }
        }
        if (end != null) {
            TextView yearView = (TextView) dateRangeRow.findViewById(R.id.endYear);
            if (end.getYear() != YearRange.UNDEFINED_YEAR) {
                yearView.setText(Integer.toString(end.getYear()));
            } else {
                yearView.setText("");
            }
            TextView endDelimiter = (TextView) dateRangeRow.findViewById(R.id.endMonthDayDelimiter);
            TextView endMonthView = (TextView) dateRangeRow.findViewById(R.id.endMonth);
            TextView endDayView = (TextView) dateRangeRow.findViewById(R.id.endMonthDay);
            if (end.getVarDate() == null) {
                endDelimiter.setVisibility(View.VISIBLE);
                endDayView.setVisibility(View.VISIBLE);

                if (end.getMonth() != null) {
                    endMonthView
                            .setText(getResources().getStringArray(R.array.months_entries)[end.getMonth().ordinal()]);
                } else {
                    endMonthView.setText("");
                }
                if (end.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                    endDayView.setText(Integer.toString(end.getDay()));
                } else {
                    endDayView.setText("");
                }
            } else {
                endDelimiter.setVisibility(View.GONE);
                endDayView.setVisibility(View.GONE);
                endMonthView
                        .setText(getResources().getStringArray(R.array.vardate_entries)[end.getVarDate().ordinal()]);
            }
            Log.d(DEBUG_TAG, "month " + end.getMonth() + " day " + end.getDay() + " var date " + end.getVarDate());
            if ((end.getMonth() != null && end.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY)
                    || end.getVarDate() != null) {
                // offset stuff
                setEnableEndDateOffsets(dateRangeRow, true);
                RelativeLayout endWeekDayContainer = (RelativeLayout) dateRangeRow
                        .findViewById(R.id.endWeekDayContainer);
                if (endWeekDayContainer != null) {
                    checkWeekDay(endWeekDayContainer,
                            end.getWeekDayOffset() != null ? end.getWeekDayOffset().toString() : null);
                    setWeekDayListeners(endWeekDayContainer, end);
                    final Spinner offsetTypeSpinner = (Spinner) dateRangeRow.findViewById(R.id.endOffsetType);
                    setSpinnerInitialEntryValue(R.array.offset_type_values, offsetTypeSpinner,
                            end.isWeekDayOffsetPositive() ? "+" : "-");
                    setSpinnerListenerEntryValues(R.array.offset_type_values, offsetTypeSpinner, new SetValue() {
                        @Override
                        public void set(String value) {
                            end.setWeekDayOffsetPositive("+".equals(value));
                            updateString();
                        }
                    });
                }
                final LinearLayout endOffsetContainer = (LinearLayout) dateRangeRow
                        .findViewById(R.id.end_offset_container);
                if (endOffsetContainer != null) {
                    EditText offset = (EditText) endOffsetContainer.findViewById(R.id.end_offset);
                    setTextWatcher(offset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            end.setDayOffset(offset);
                            updateString();
                        }
                    });
                    if (end.getDayOffset() > 0) {
                        offset.setText(Integer.toString(end.getDayOffset()));
                        endOffsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        endOffsetContainer.setVisibility(View.GONE);
                        MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_end_offset);
                        showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                endOffsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            }
                        });
                    }
                }
            } else {
                setEnableEndDateOffsets(dateRangeRow, false);
            }
        } else {
            if (start.isOpenEnded()) {
                View endDate = dateRangeRow.findViewById(R.id.endDate);
                endDate.setVisibility(View.GONE);
                View to = dateRangeRow.findViewById(R.id.to);
                to.setVisibility(View.GONE);
                setVisEndDateOffsets(dateRangeRow, View.GONE);
            } else {
                setEnableEndDateOffsets(dateRangeRow, false);
            }
        }
    }

    private void setVisEndDateOffsets(@NonNull LinearLayout dateRangeRow, int vis) {
        View endWeekDayContainer = dateRangeRow.findViewById(R.id.endWeekDayContainer);
        if (endWeekDayContainer != null) {
            endWeekDayContainer.setVisibility(vis);
        }
        View endOffsetType = dateRangeRow.findViewById(R.id.endOffsetType);
        if (endOffsetType != null) {
            endOffsetType.setVisibility(vis);
        }
        View endOffsetContainer = dateRangeRow.findViewById(R.id.end_offset_container);
        if (endOffsetContainer != null) {
            endOffsetContainer.setVisibility(vis);
        }
    }

    private void setEnableEndDateOffsets(@NonNull LinearLayout dateRangeRow, boolean enable) {
        setEnableChlldren(R.id.endWeekDayContainer, dateRangeRow, enable);
        View endOffsetType = dateRangeRow.findViewById(R.id.endOffsetType);
        if (endOffsetType != null) {
            endOffsetType.setEnabled(enable);
        }
        setEnableChlldren(R.id.end_offset_container, dateRangeRow, enable);
    }

    private void setEnableChlldren(int res, @NonNull LinearLayout dateRangeRow, boolean enable) {
        ViewGroup container = (ViewGroup) dateRangeRow.findViewById(res);
        if (container != null) {
            int children = container.getChildCount();
            for (int i = 0; i < children; i++) {
                container.getChildAt(i).setEnabled(enable);
            }
        }
    }

    private void addHolidayUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<Holiday> holidays,
            @NonNull final Holiday hd) {
        LinearLayout holidayRow = (LinearLayout) inflater.inflate(R.layout.holiday_row, null);
        Spinner holidaysSpinner = (Spinner) holidayRow.findViewById(R.id.holidays);
        setSpinnerInitialEntryValue(R.array.holidays_values, holidaysSpinner, hd.getType().name());
        setSpinnerListenerEntryValues(R.array.holidays_values, holidaysSpinner, new SetValue() {
            @Override
            public void set(String value) {
                hd.setType(Holiday.Type.valueOf(value));
            }
        });
        Menu menu = addStandardMenuItems(holidayRow, new Delete() {
            @Override
            public void delete() {
                holidays.remove(hd);
                if (holidays.isEmpty()) {
                    r.setHolidays(null);
                }
                updateString();
                watcher.afterTextChanged(null); // hack to force rebuild of form
            }
        });

        final CheckBox useAsWeekDay = (CheckBox) holidayRow.findViewById(R.id.checkBoxUseAsWeekDay);
        useAsWeekDay.setChecked(hd.getUseAsWeekDay());
        useAsWeekDay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (Holiday h : holidays) {
                    h.setUseAsWeekDay(isChecked);
                }
                updateString();
                text.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        watcher.afterTextChanged(null);
                    }
                }, 100);
            }
        });

        final View useAsWeekDayContainer = holidayRow.findViewById(R.id.useAsWeekDay_container);
        if (!hd.getUseAsWeekDay()) {
            useAsWeekDayContainer.setVisibility(View.VISIBLE);
        } else {
            useAsWeekDayContainer.setVisibility(View.GONE);
            MenuItem showUse = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_useAsWeekDay);
            showUse.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    useAsWeekDayContainer.setVisibility(View.VISIBLE);
                    return true;
                }
            });
        }

        final View offsetContainer = holidayRow.findViewById(R.id.offset_container);
        if (hd.getOffset() != 0) {
            offsetContainer.setVisibility(View.VISIBLE);
        } else {
            offsetContainer.setVisibility(View.GONE);
            MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
            showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    offsetContainer.setVisibility(View.VISIBLE);
                    return true;
                }
            });
        }

        EditText offset = (EditText) holidayRow.findViewById(R.id.offset);
        offset.setText(Integer.toString(hd.getOffset()));
        setTextWatcher(offset, new SetValue() {
            @Override
            public void set(String value) {
                int offset = 0;
                try {
                    offset = Integer.parseInt(value);
                } catch (NumberFormatException nfex) {
                }
                hd.setOffset(offset);
            }
        });
        ll.addView(holidayRow);
    }

    private void addYearRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<YearRange> years,
            @NonNull final YearRange yr) {
        LinearLayout yearLayout = (LinearLayout) inflater.inflate(R.layout.year_range, null);
        final int startYear = yr.getStartYear();
        final TextView startYearView = (TextView) yearLayout.findViewById(R.id.start_year);
        startYearView.setText(Integer.toString(startYear));

        final TextView endYearView = (TextView) yearLayout.findViewById(R.id.end_year);
        final int endYear = yr.getEndYear();
        if (endYear > 0) {
            endYearView.setText(Integer.toString(endYear));
        }
        final View intervalContainer = yearLayout.findViewById(R.id.interval_container);
        EditText yearIntervalEdit = (EditText) yearLayout.findViewById(R.id.interval);
        if (yr.getInterval() > 0) {
            intervalContainer.setVisibility(View.VISIBLE);
            yearIntervalEdit.setText(Integer.toString(yr.getInterval()));
        } else {
            intervalContainer.setVisibility(View.GONE);
        }
        setTextWatcher(yearIntervalEdit, new SetValue() {
            @Override
            public void set(String value) {
                int interval = 0;
                try {
                    interval = Integer.parseInt(value);
                } catch (NumberFormatException nfex) {
                }
                yr.setInterval(interval);
            }
        });
        Menu menu = addStandardMenuItems(yearLayout, new Delete() {
            @Override
            public void delete() {
                years.remove(yr);
                if (years.isEmpty()) {
                    r.setYears(null);
                }
                updateString();
                watcher.afterTextChanged(null); // hack to force rebuild of form
            }
        });
        MenuItem showInterval = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_interval);
        showInterval.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                intervalContainer.setVisibility(View.VISIBLE);
                return true;
            }
        });

        LinearLayout range = (LinearLayout) yearLayout.findViewById(R.id.range);
        range.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                int rangeEnd = Math.max(currentYear, startYear) + 50;
                int tempEndYear = yr.getEndYear();
                if (tempEndYear == YearRange.UNDEFINED_YEAR) {
                    tempEndYear = RangePicker.NOTHING_SELECTED;
                } else {
                    rangeEnd = Math.max(rangeEnd, tempEndYear + 50);
                }
                RangePicker.showDialog(getActivity(), R.string.year_range, YearRange.FIRST_VALID_YEAR, rangeEnd,
                        yr.getStartYear(), tempEndYear, new SetRangeListener() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void setRange(int start, int end) {
                                yr.setStartYear(start);
                                startYearView.setText(Integer.toString(start));
                                if (end != RangePicker.NOTHING_SELECTED) {
                                    endYearView.setText(Integer.toString(end));
                                    yr.setEndYear(end);
                                } else {
                                    endYearView.setText("");
                                    yr.setEndYear(YearRange.UNDEFINED_YEAR);
                                }
                                updateString();
                            }
                        });
            }
        });
        ll.addView(yearLayout);
    }

    private void addWeekRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<WeekRange> weeks,
            @NonNull final WeekRange w) {
        LinearLayout weekLayout = (LinearLayout) inflater.inflate(R.layout.week_range, null);
        final TextView startWeekView = (TextView) weekLayout.findViewById(R.id.start_week);
        final int startWeek = w.getStartWeek();
        startWeekView.setText(Integer.toString(startWeek));

        final TextView endWeekView = (TextView) weekLayout.findViewById(R.id.end_week);
        final int endWeek = w.getEndWeek();
        if (endWeek > 0) {
            endWeekView.setText(Integer.toString(endWeek));
        }

        final View intervalContainer = weekLayout.findViewById(R.id.interval_container);
        EditText weekIntervalEdit = (EditText) weekLayout.findViewById(R.id.interval);
        if (w.getInterval() > 0) {
            intervalContainer.setVisibility(View.VISIBLE);
            weekIntervalEdit.setText(Integer.toString(w.getInterval()));
        } else {
            intervalContainer.setVisibility(View.GONE);
        }
        setTextWatcher(weekIntervalEdit, new SetValue() {
            @Override
            public void set(String value) {
                int interval = 0;
                try {
                    interval = Integer.parseInt(value);
                } catch (NumberFormatException nfex) {
                }
                w.setInterval(interval);
            }
        });
        Menu menu = addStandardMenuItems(weekLayout, new Delete() {
            @Override
            public void delete() {
                weeks.remove(w);
                if (weeks.isEmpty()) {
                    r.setWeeks(null);
                }
                updateString();
                watcher.afterTextChanged(null); // hack to force rebuild of form
            }
        });
        MenuItem showInterval = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_interval);
        showInterval.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                intervalContainer.setVisibility(View.VISIBLE);
                return true;
            }
        });

        LinearLayout range = (LinearLayout) weekLayout.findViewById(R.id.range);
        range.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // need to reget values from w
                int tempEndWeek = w.getEndWeek();
                if (tempEndWeek == WeekRange.UNDEFINED_WEEK) {
                    tempEndWeek = RangePicker.NOTHING_SELECTED;
                }
                RangePicker.showDialog(getActivity(), R.string.week_range, WeekRange.MIN_WEEK, WeekRange.MAX_WEEK,
                        w.getStartWeek(), tempEndWeek, new SetRangeListener() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void setRange(int start, int end) {
                                w.setStartWeek(start);
                                startWeekView.setText(Integer.toString(start));
                                if (end != RangePicker.NOTHING_SELECTED) {
                                    endWeekView.setText(Integer.toString(end));
                                    w.setEndWeek(end);
                                } else {
                                    endWeekView.setText("");
                                    w.setEndWeek(WeekRange.UNDEFINED_WEEK);
                                }
                                updateString();
                            }
                        });
            }
        });
        ll.addView(weekLayout);
    }

    /**
     * Check if just one day is in the ranges
     * 
     * @param ranges WeekDayRange to check
     * @return return true if just one range and that only contains one day
     */
    private boolean justOneDay(@NonNull List<WeekDayRange> ranges) {
        return ranges.size() == 1 && ranges.get(0).getEndDay() == null;
    }

    /**
     * Set our standard text watcher and a listener on an EditText
     * 
     * @param edit the EditText
     * @param listener listener to call when afterTextChanged is called
     */
    private void setTextWatcher(@NonNull final EditText edit, @NonNull final SetValue listener) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                listener.set(s.toString());
                updateString();
            }
        });
    }

    /**
     * Interface for call backs that delete a Rule
     * 
     * @author simon
     *
     */
    interface Delete {
        void delete();
    }

    private class DeleteTimeSpan implements Delete {
        final List<TimeSpan> times;
        final TimeSpan ts;

        DeleteTimeSpan(final List<TimeSpan> times, final TimeSpan ts) {
            this.times = times;
            this.ts = ts;
        }

        @Override
        public void delete() {
            times.remove(ts);
            if (times.isEmpty()) {
                // set to null?
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        }
    }

    private void addTimeSpanUIs(@NonNull final LinearLayout ll, @Nullable final List<TimeSpan> times) {
        if (times != null && times.size() > 0) {
            Log.d(DEBUG_TAG, "#time spans " + times.size());
            Menu menu = null;
            for (final TimeSpan ts : times) {
                boolean hasStartEvent = ts.getStartEvent() != null;
                boolean hasEndEvent = ts.getEndEvent() != null;
                boolean extendedTime = ts.getEnd() > 1440;
                boolean hasInterval = ts.getInterval() > 0;
                if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() >= 0 && !extendedTime) {
                    Log.d(DEBUG_TAG, "t-t " + ts.toString());
                    LinearLayout timeRangeRow = (LinearLayout) inflater.inflate(R.layout.time_range_row, null);
                    RangeBar timeBar = (RangeBar) timeRangeRow.findViewById(R.id.timebar);
                    int start = ts.getStart();
                    int end = ts.getEnd();
                    if (start >= 360) {
                        timeBar.setTickStart(360);
                    }
                    setGranuarlty(timeBar, start, end);
                    timeBar.setRangePinsByValue(start, end);
                    timeBar.setPinTextFormatter(timeFormater);
                    timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setStart(toMins(leftPinValue));
                            ts.setEnd(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    menu = addStandardMenuItems(timeRangeRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts);
                    addTimeSpanMenus(timeBar, null, menu);

                    ll.addView(timeRangeRow);
                } else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() >= 0 && extendedTime) {
                    Log.d(DEBUG_TAG, "t-x " + ts.toString());
                    LinearLayout timeExtendedRangeRow = (LinearLayout) inflater
                            .inflate(R.layout.time_extended_range_row, null);
                    RangeBar timeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.timebar);
                    RangeBar extendedTimeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.extendedTimebar);
                    int start = ts.getStart();
                    int end = ts.getEnd();
                    setGranuarlty(timeBar, start, end);
                    setGranuarlty(extendedTimeBar, start, end);
                    timeBar.setRangePinsByValue(0, start);
                    timeBar.setPinTextFormatter(timeFormater);
                    timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setStart(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    extendedTimeBar.setRangePinsByValue(1440, end);
                    extendedTimeBar.setPinTextFormatter(timeFormater);
                    extendedTimeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setEnd(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    menu = addStandardMenuItems(timeExtendedRangeRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts);
                    addTimeSpanMenus(timeBar, extendedTimeBar, menu);
                    ll.addView(timeExtendedRangeRow);
                } else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() < 0) {
                    Log.d(DEBUG_TAG, "pot " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_row, null);
                    RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
                    timeBar.setPinTextFormatter(timeFormater);
                    int start = ts.getStart();
                    timeBar.setConnectingLineEnabled(false);
                    setGranuarlty(timeBar, start, 0);
                    timeBar.setRangePinsByValue(0, start);

                    timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setStart(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts);
                    addTimeSpanMenus(timeBar, null, menu);
                    ll.addView(timeEventRow);
                } else if (!ts.isOpenEnded() && !hasStartEvent && hasEndEvent) {
                    Log.d(DEBUG_TAG, "t-e " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
                    RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
                    timeBar.setPinTextFormatter(timeFormater);
                    int start = ts.getStart();
                    setGranuarlty(timeBar, start, 0);
                    timeBar.setRangePinsByValue(0, start);
                    timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setStart(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    setSpinnerInitialEntryValue(R.array.events_values, endEvent,
                            ts.getEndEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, endEvent, new SetValue() {
                        @Override
                        public void set(String value) {
                            ts.getEndEvent().setEvent(value);
                        }
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts);
                    addTimeSpanMenus(timeBar, null, menu);

                    final View offsetContainer = timeEventRow.findViewById(R.id.offset_container);
                    if (ts.getEndEvent().getOffset() != 0) {
                        offsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        offsetContainer.setVisibility(View.GONE);
                        MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
                        showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                offsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            }
                        });
                    }

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.offset);
                    offset.setText(Integer.toString(ts.getEndEvent().getOffset()));
                    setTextWatcher(offset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            ts.getEndEvent().setOffset(offset);
                        }
                    });
                    ll.addView(timeEventRow);
                } else if (!ts.isOpenEnded() && hasStartEvent && !hasEndEvent) {
                    Log.d(DEBUG_TAG, "e-t " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_start_event_row, null);
                    RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
                    RangeBar extendedTimeBar = (RangeBar) timeEventRow.findViewById(R.id.extendedTimebar);
                    int end = ts.getEnd();
                    if (end != TimeSpan.UNDEFINED_TIME) {
                        int start = 0;
                        RangeBar bar = timeBar;
                        if (extendedTime) {
                            timeBar.setVisibility(View.GONE);
                            bar = extendedTimeBar;
                            start = 1440;
                        } else {
                            extendedTimeBar.setVisibility(View.GONE);
                            if (end >= 360) {
                                bar.setTickStart(360);
                                start = 360;
                            }
                        }
                        bar.setPinTextFormatter(timeFormater);
                        setGranuarlty(bar, 0, end);
                        bar.setRangePinsByValue(start, end);
                        bar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                            @Override
                            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                    String leftPinValue, String rightPinValue) {
                                ts.setEnd(toMins(rightPinValue));
                                updateString();
                            }
                        });
                    } else {
                        timeBar.setVisibility(View.GONE);
                        extendedTimeBar.setVisibility(View.GONE);
                    }
                    Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    setSpinnerInitialEntryValue(R.array.events_values, startEvent,
                            ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, new SetValue() {
                        @Override
                        public void set(String value) {
                            ts.getStartEvent().setEvent(value);
                        }
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    if (timeBar.getVisibility() == View.VISIBLE) {
                        addTimePickerMenu(menu, ts);
                        addTimeSpanMenus(timeBar, null, menu);
                    }
                    final View offsetContainer = timeEventRow.findViewById(R.id.offset_container);
                    if (ts.getStartEvent().getOffset() != 0) {
                        offsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        offsetContainer.setVisibility(View.GONE);
                        MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
                        showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                offsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            }
                        });
                    }

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.offset);
                    offset.setText(Integer.toString(ts.getStartEvent().getOffset()));
                    setTextWatcher(offset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            ts.getStartEvent().setOffset(offset);
                        }
                    });
                    ll.addView(timeEventRow);
                } else if (!ts.isOpenEnded() && hasStartEvent && hasEndEvent) {
                    Log.d(DEBUG_TAG, "e-e " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
                    final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    setSpinnerInitialEntryValue(R.array.events_values, startEvent,
                            ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, new SetValue() {
                        @Override
                        public void set(String value) {
                            ts.getStartEvent().setEvent(value);
                        }
                    });
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    setSpinnerInitialEntryValue(R.array.events_values, endEvent,
                            ts.getEndEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, endEvent, new SetValue() {
                        @Override
                        public void set(String value) {
                            ts.getEndEvent().setEvent(value);
                        }
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));

                    final View startOffsetContainer = timeEventRow.findViewById(R.id.start_offset_container);
                    final View endOffsetContainer = timeEventRow.findViewById(R.id.end_offset_container);
                    if (ts.getStartEvent().getOffset() != 0 || ts.getEndEvent().getOffset() != 0) {
                        startOffsetContainer.setVisibility(View.VISIBLE);
                        endOffsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        startOffsetContainer.setVisibility(View.GONE);
                        endOffsetContainer.setVisibility(View.GONE);
                        MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
                        showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                startOffsetContainer.setVisibility(View.VISIBLE);
                                endOffsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            }
                        });
                    }

                    EditText startOffset = (EditText) startOffsetContainer.findViewById(R.id.start_offset);
                    startOffset.setText(Integer.toString(ts.getStartEvent().getOffset()));
                    setTextWatcher(startOffset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            ts.getStartEvent().setOffset(offset);
                        }
                    });
                    EditText endOffset = (EditText) endOffsetContainer.findViewById(R.id.end_offset);
                    endOffset.setText(Integer.toString(ts.getEndEvent().getOffset()));
                    setTextWatcher(endOffset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            ts.getEndEvent().setOffset(offset);
                        }
                    });
                    ll.addView(timeEventRow);
                } else if (ts.isOpenEnded() && !hasStartEvent) {
                    Log.d(DEBUG_TAG, "t- " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_row, null);
                    RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
                    timeBar.setPinTextFormatter(timeFormater);
                    int start = ts.getStart();
                    setGranuarlty(timeBar, start, 0);
                    timeBar.setRangePinsByValue(0, start);
                    timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                String leftPinValue, String rightPinValue) {
                            ts.setStart(toMins(rightPinValue));
                            updateString();
                        }
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts);
                    addTimeSpanMenus(timeBar, null, menu);
                    ll.addView(timeEventRow);
                } else if (ts.isOpenEnded() && hasStartEvent) {
                    Log.d(DEBUG_TAG, "e- " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
                    final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    setSpinnerInitialEntryValue(R.array.events_values, startEvent,
                            ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, new SetValue() {
                        @Override
                        public void set(String value) {
                            ts.getStartEvent().setEvent(value);
                        }
                    });
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    endEvent.setVisibility(View.GONE);
                    View endOffsetContainer = timeEventRow.findViewById(R.id.end_offset_container);
                    endOffsetContainer.setVisibility(View.GONE);
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    final View offsetContainer = timeEventRow.findViewById(R.id.start_offset_container);
                    if (ts.getStartEvent().getOffset() != 0) {
                        offsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        offsetContainer.setVisibility(View.GONE);
                        MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
                        showOffset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                offsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            }
                        });
                    }

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.start_offset);
                    offset.setText(Integer.toString(ts.getStartEvent().getOffset()));
                    setTextWatcher(offset, new SetValue() {
                        @Override
                        public void set(String value) {
                            int offset = 0;
                            try {
                                offset = Integer.parseInt(value);
                            } catch (NumberFormatException nfex) {
                            }
                            ts.getStartEvent().setOffset(offset);
                        }
                    });
                    ll.addView(timeEventRow);
                } else {
                    Log.d(DEBUG_TAG, "? " + ts.toString());
                    TextView tv = new TextView(getActivity());
                    tv.setText(ts.toString());
                    ll.addView(tv);
                    return;
                }
                final LinearLayout intervalLayout = (LinearLayout) inflater.inflate(R.layout.interval, null);
                EditText intervalEdit = (EditText) intervalLayout.findViewById(R.id.interval);
                intervalEdit.setText(Integer.toString(ts.getInterval()));
                setTextWatcher(intervalEdit, new SetValue() {
                    @Override
                    public void set(String value) {
                        int interval = 0;
                        try {
                            interval = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                        }
                        ts.setInterval(interval);
                    }
                });
                ll.addView(intervalLayout);
                if (hasInterval) {
                    intervalLayout.setVisibility(View.VISIBLE);
                } else {
                    intervalLayout.setVisibility(View.GONE);
                    MenuItem showInterval = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_interval);
                    showInterval.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            intervalLayout.setVisibility(View.VISIBLE);
                            return true;
                        }
                    });
                }
            }
        }
    }

    /**
     * Add a menu item that displays a number picker for times
     * 
     * @param menu Menu to and the item to
     * @param ts TimeSpan to display and modify
     */
    private void addTimePickerMenu(Menu menu, final TimeSpan ts) {
        final MenuItem timePickerMenu = menu.add(R.string.display_time_picker);
        timePickerMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int start = ts.getStart();
                int end = ts.getEnd();
                if (ts.getStartEvent() == null && ts.getEndEvent() == null && end >= 0) { // t-t, t-x
                    TimeRangePicker.showDialog(getActivity(), R.string.time, start / 60, start % 60, end / 60, end % 60,
                            new SetTimeRangeListener() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void setTimeRange(int startHour, int startMinute, int endHour, int endMinute) {
                                    ts.setStart(startHour * 60 + startMinute);
                                    ts.setEnd(endHour * 60 + endMinute);
                                    updateString();
                                    watcher.afterTextChanged(null);
                                }
                            });
                } else if (ts.getStartEvent() == null && (ts.getEndEvent() != null || end < 0)) { // t, t-, t-e
                    TimeRangePicker.showDialog(getActivity(), R.string.time, start / 60, start % 60,
                            new SetTimeRangeListener() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void setTimeRange(int startHour, int startMinute, int endHour, int endMinute) {
                                    ts.setStart(startHour * 60 + startMinute);
                                    updateString();
                                    watcher.afterTextChanged(null);
                                }
                            });
                } else if (ts.getStartEvent() != null && ts.getEndEvent() == null || end >= 0) { // e-t
                    TimeRangePicker.showDialog(getActivity(), R.string.time, start / 60, start % 60,
                            new SetTimeRangeListener() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void setTimeRange(int startHour, int startMinute, int endHour, int endMinute) {
                                    ts.setEnd(startHour * 60 + startMinute);
                                    updateString();
                                    watcher.afterTextChanged(null);
                                }
                            });
                }
                return true;
            }
        });
    }

    private void changeTicks(@NonNull final RangeBar timeBar, int interval) {
        int start = toMins(timeBar.getLeftPinValue());
        start = Math.round(((float) start) / interval) * interval;
        int end = toMins(timeBar.getRightPinValue());
        end = Math.round(((float) end) / interval) * interval;
        timeBar.setTickInterval(interval);
        timeBar.setRangePinsByValue(start, end);
        timeBar.setVisibleTickInterval(60 / interval);
    }

    private void changeStart(@NonNull final RangeBar timeBar, float newTickStart) {
        int interval = (int) timeBar.getTickInterval();
        int start = toMins(timeBar.getLeftPinValue());
        start = Math.round(((float) start) / interval) * interval;
        int end = toMins(timeBar.getRightPinValue());
        end = Math.round(((float) end) / interval) * interval;
        timeBar.setTickStart(newTickStart);
        timeBar.setRangePinsByValue(start, end);
    }

    private void addTimeSpanMenus(@NonNull final RangeBar timeBar, @Nullable final RangeBar timeBar2,
            @NonNull Menu menu) {

        final MenuItem item15 = menu.add(R.string.ticks_15_minute);
        final MenuItem item5 = menu.add(R.string.ticks_5_minute);
        final MenuItem item1 = menu.add(R.string.ticks_1_minute);

        item15.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                changeTicks(timeBar, 15);
                if (timeBar2 != null) {
                    changeTicks(timeBar2, 15);
                }
                item15.setEnabled(false);
                item5.setEnabled(true);
                item1.setEnabled(true);
                return true;
            }
        });

        item5.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                changeTicks(timeBar, 5);
                if (timeBar2 != null) {
                    changeTicks(timeBar2, 5);
                }
                item15.setEnabled(true);
                item5.setEnabled(false);
                item1.setEnabled(true);
                return true;
            }
        });

        item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                changeTicks(timeBar, 1);
                if (timeBar2 != null) {
                    changeTicks(timeBar2, 1);
                }
                item15.setEnabled(true);
                item5.setEnabled(true);
                item1.setEnabled(false);
                return true;
            }
        });
        item1.setEnabled(false);
        item5.setEnabled(false);
        item15.setEnabled(false);
        if (((int) timeBar.getTickInterval()) != 1) {
            item1.setEnabled(true);
        }
        if (((int) timeBar.getTickInterval()) != 5) {
            item5.setEnabled(true);
        }
        if (((int) timeBar.getTickInterval()) != 15) {
            item15.setEnabled(true);
        }

        if ((int) timeBar.getTickStart() > 0 || (timeBar2 != null && (int) timeBar2.getTickStart() > 0)) {
            final MenuItem expand = menu.add(R.string.start_at_midnight);
            expand.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    changeStart(timeBar, 0f);
                    if (timeBar2 != null) {
                        changeStart(timeBar2, 0f);
                    }
                    expand.setEnabled(false);
                    return true;
                }
            });
        }
    }

    private void setGranuarlty(@NonNull RangeBar bar, int start, int end) {
        if ((start % 5 > 0) || (end % 5 > 0)) { // need
            // 1 minute
            // granularity
            bar.setTickInterval(1);
            bar.setVisibleTickInterval(60);
        } else if ((start % 15 > 0) || (end % 15 > 0)) {
            // 5 minute
            // granularity
            bar.setTickInterval(5);
            bar.setVisibleTickInterval(12);
        } else {
            // 15 minute
            // granularity
            bar.setTickInterval(15);
            bar.setVisibleTickInterval(4);
        }
    }

    interface SetValue {
        void set(String value);
    }

    private void setSpinnerInitialEntryValue(int valuesId, @NonNull Spinner spinner, String value) {
        Resources res = getResources();
        final TypedArray values = res.obtainTypedArray(valuesId);
        for (int i = 0; i < values.length(); i++) {
            if (value.equals(values.getString(i))) {
                spinner.setSelection(i);
                break;
            }
        }
        values.recycle();
    }

    private void setSpinnerListenerEntryValues(final int valuesId, @NonNull final Spinner spinner,
            @NonNull final SetValue listener) {
        final Resources res = getResources();

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final TypedArray selectedValues = res.obtainTypedArray(valuesId);
                listener.set(selectedValues.getString(position));
                updateString();
                selectedValues.recycle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Add the standard menu entries for a Rule
     * 
     * @param row the layout containing the Rule UI
     * @param listener the listerner for the deleting the rule
     * @return the created Menu
     */
    private Menu addStandardMenuItems(@NonNull LinearLayout row, @Nullable final Delete listener) {
        ActionMenuView amv = (ActionMenuView) row.findViewById(R.id.menu);
        Menu menu = amv.getMenu();
        MenuItem mi = menu.add(Menu.NONE, Menu.NONE, Menu.CATEGORY_SECONDARY, R.string.Delete);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (listener != null) {
                    listener.delete();
                }
                return true;
            }
        });
        MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        return menu;
    }

    /**
     * Runnable that updates the OH string
     */
    Runnable updateStringRunnable = new Runnable() {
        @Override
        public void run() {
            int pos = text.getSelectionStart();
            int prevLen = text.length();
            text.removeTextChangedListener(watcher);
            String oh = Util.rulesToOpeningHoursString(rules);
            text.setText(oh);
            text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos, text.length()));
            text.addTextChangedListener(watcher);
            enableSaveButton(oh);
            final int len = oh.length();
            if (len > OSM_MAX_TAG_LENGTH) {
                Log.d(DEBUG_TAG, "string too long");
                ch.poole.openinghoursfragment.Util.toastTop(getActivity(), getString(R.string.value_too_long, len));
            }
        }
    };

    /**
     * Update the actual OH string
     */
    private void updateString() {
        text.removeCallbacks(updateStringRunnable);
        text.postDelayed(updateStringRunnable, 100);
    }

    /**
     * Parse hh:mm and return minutes since midnight
     * 
     * Totally trivial implementation c&p from
     * http://stackoverflow.com/questions/8909075/convert-time-field-hm-into-integer-field-minutes-in-java FIXME what
     * happens when this throws an Exception?
     * 
     * @param t hh:mm format string
     * @return number of minutes
     */
    private static int toMins(String t) {
        String[] hourMin = t.split(":");
        int hour = Integer.parseInt(hourMin[0]);
        int mins = Integer.parseInt(hourMin[1]);
        int hoursInMins = hour * 60;
        return hoursInMins + mins;
    }

    /**
     * Set a check mark for the say specified
     * 
     * @param container layout containing the checkboxes
     * @param day two letter day as a string
     */
    private void checkWeekDay(@NonNull RelativeLayout container, @Nullable String day) {
        Log.d(DEBUG_TAG, "checking " + day);
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if ((v instanceof CheckBox || v instanceof AppCompatCheckBox) && ((String) v.getTag()).equals(day)) {
                ((CheckBox) v).setChecked(true);
                return;
            }
        }
    }

    private void checkNth(@NonNull RelativeLayout container, int nth) {
        Log.d(DEBUG_TAG, "checking " + nth);
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if ((v instanceof CheckBox || v instanceof AppCompatCheckBox)
                    && ((String) v.getTag()).equals(Integer.toString(nth))) {
                ((CheckBox) v).setChecked(true);
                return;
            }
        }
    }

    private void setWeekDayListeners(@NonNull final RelativeLayout container, @NonNull final List<WeekDayRange> days,
            @NonNull final List<WeekDayRange> inContainer, final boolean justOne, @NonNull final MenuItem nthMenuItem) {
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (justOne) { // Nth exists
                    if (isChecked) {
                        WeekDayRange range = days.get(0);
                        for (int i = 0; i < container.getChildCount(); i++) {
                            final View c = container.getChildAt(i);
                            if ((c instanceof CheckBox || c instanceof AppCompatCheckBox) && !c.equals(buttonView)) {
                                ((CheckBox) c).setChecked(false);
                            }
                        }
                        range.setStartDay((String) buttonView.getTag());
                    } else { // hack alert
                        for (int i = 0; i < container.getChildCount(); i++) {
                            final View c = container.getChildAt(i);
                            if ((c instanceof CheckBox || c instanceof AppCompatCheckBox)) {
                                if (((CheckBox) c).isChecked()) {
                                    return;
                                }
                            }
                        }
                        ((CheckBox) buttonView).setChecked(true);
                    }
                } else {
                    List<WeekDayRange> temp = new ArrayList<WeekDayRange>(days);
                    for (WeekDayRange d : temp) {
                        if (d.getNths() == null) {
                            days.remove(d);
                        }
                    }
                    WeekDayRange range = null;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View c = container.getChildAt(i);
                        if (c instanceof CheckBox || c instanceof AppCompatCheckBox) {
                            if (((CheckBox) c).isChecked()) {
                                if (range == null) {
                                    range = new WeekDayRange();
                                    range.setStartDay((String) c.getTag());
                                    days.add(range);
                                } else {
                                    range.setEndDay((String) c.getTag());
                                }
                            } else {
                                range = null;
                            }
                        }
                    }
                    nthMenuItem.setEnabled(justOneDay(days));
                }
                updateString();
            }
        };

        for (int i = 0; i < container.getChildCount(); i++) {
            final View v = container.getChildAt(i);
            if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener(listener);
            }
        }
    }

    private void setWeekDayListeners(@NonNull final RelativeLayout container, @NonNull final DateWithOffset dwo) {
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View c = container.getChildAt(i);
                        if ((c instanceof CheckBox || c instanceof AppCompatCheckBox) && !c.equals(buttonView)) {
                            ((CheckBox) c).setChecked(false);
                        }
                    }
                    dwo.setWeekDayOffset((String) buttonView.getTag());
                } else { // hack alert
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View c = container.getChildAt(i);
                        if ((c instanceof CheckBox || c instanceof AppCompatCheckBox)) {
                            if (((CheckBox) c).isChecked()) {
                                return;
                            }
                        }
                    }
                    ((CheckBox) buttonView).setChecked(false);
                    dwo.setWeekDayOffset((WeekDay) null);
                }
                updateString();
            }
        };

        for (int i = 0; i < container.getChildCount(); i++) {
            final View v = container.getChildAt(i);
            if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener(listener);
            }
        }
    }

    private void setNthListeners(@NonNull final RelativeLayout container, @NonNull final WeekDayRange days) {
        OnCheckedChangeListener listener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                List<Nth> nths = days.getNths();
                if (nths == null) {
                    nths = new ArrayList<Nth>();
                    days.setNths(nths);
                } else {
                    nths.clear();
                }
                Nth range = null;
                for (int i = 0; i < container.getChildCount(); i++) {
                    final View c = container.getChildAt(i);
                    if (c instanceof CheckBox || c instanceof AppCompatCheckBox) {
                        if (((CheckBox) c).isChecked()) {
                            if (range == null) {
                                range = new Nth();
                                range.setStartNth(Integer.parseInt((String) c.getTag()));
                                nths.add(range);
                            } else {
                                range.setEndNth(Integer.parseInt((String) c.getTag()));
                            }
                        } else {
                            range = null;
                        }
                    }
                }
                updateString();
            }
        };

        for (int i = 0; i < container.getChildCount(); i++) {
            final View v = container.getChildAt(i);
            if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener(listener);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // super.onSaveInstanceState(outState); avoid trying to save views, this is a hack
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putSerializable(KEY_KEY, key);
        outState.putSerializable(VALUE_KEY, text.getText().toString());
        outState.putSerializable(ORIGINAL_VALUE_KEY, originalOpeningHoursValue);
        outState.putInt(STYLE_KEY, styleRes);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(DEBUG_TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_TAG, "onDestroy");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        if (loadedDefault) {
            Log.d(DEBUG_TAG, "Show toast");
            ch.poole.openinghoursfragment.Util.toastTop(getActivity(), getString(R.string.loaded_default));
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return our layout view or null if it can't be found
     */
    @Nullable
    public View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.openinghours_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.openinghours_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.openinghours_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.openinghours_layout");
                }
                return v;
            }
        } else {
            Log.d(DEBUG_TAG, "got null view in getView");
        }
        return null;
    }

    private int dpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    /**
     * Enable / disable the save button depending on if the current value is the same as the original one
     * 
     * @param oh oh value to test against
     */
    private void enableSaveButton(String oh) {
        saveButton.setEnabled(originalOpeningHoursValue == null
                || (!originalOpeningHoursValue.equals(oh) && oh.length() <= OSM_MAX_TAG_LENGTH));
    }

    /**
     * Template database related methods and fields
     */
    private Cursor templateCursor;
    private TemplateAdapter templateAdapter;
    private AlertDialog templateDialog;

    private AppCompatButton saveButton;

    /**
     * Show a list of the templates in the database, selection will either load a template or start the edit dialog on
     * it
     * 
     * @param context Android context
     * @param manage if true the template editor will be started otherwise the template will replace the current OH
     *            value
     */
    void loadOrManageTemplate(Context context, boolean manage) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View templateView = (View) inflater.inflate(R.layout.template_list, null);
        alertDialog.setTitle(manage ? R.string.manage_templates_title : R.string.load_templates_title);
        alertDialog.setView(templateView);
        ListView lv = (ListView) templateView.findViewById(R.id.listView1);
        final SQLiteDatabase writableDb = new TemplateDatabaseHelper(context).getWritableDatabase();
        templateCursor = writableDb.rawQuery(TemplateDatabase.QUERY_ALL, null);
        templateAdapter = new TemplateAdapter(writableDb, context, templateCursor, manage);
        lv.setAdapter(templateAdapter);
        alertDialog.setNegativeButton(R.string.Cancel, null);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                templateCursor.close();
                writableDb.close();
            }
        });
        templateDialog = alertDialog.show();
    }

    private class TemplateAdapter extends CursorAdapter {
        final SQLiteDatabase db;
        final boolean manage;

        public TemplateAdapter(final SQLiteDatabase db, Context context, Cursor cursor, boolean manage) {
            super(context, cursor, 0);
            this.db = db;
            this.manage = manage;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            View view = LayoutInflater.from(context).inflate(R.layout.template_list_item, parent, false);
            return view;
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            Log.d(DEBUG_TAG, "bindView");
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            Log.d(DEBUG_TAG, "bindView id " + id);
            boolean isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(TemplateDatabase.DEFAULT_FIELD)) == 1;
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.NAME_FIELD));
            TextView nameView = (TextView) view.findViewById(R.id.name);
            nameView.setText(isDefault ? getActivity().getString(R.string.is_default, name) : name);
            final String template = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.TEMPLATE_FIELD));
            if (manage) {
                nameView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer id = (Integer) view.getTag();
                        showTemplateDialog(db, true, id != null ? id.intValue() : -1);
                    }
                });
            } else {
                nameView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        text.removeTextChangedListener(watcher);
                        text.setText(template);
                        text.addTextChangedListener(watcher);
                        watcher.afterTextChanged(null);
                        templateDialog.dismiss();
                    }
                });
            }
        }
    }

    /**
     * Show a dialog for editing and saving a template
     * 
     * @param db a writable instance of the template database
     * @param existing true if this is not a new template
     * @param id the rowid of the template in the database or -1 if not saved yet
     */
    private void showTemplateDialog(@NonNull final SQLiteDatabase db, final boolean existing, final int id) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View templateView = (View) inflater.inflate(R.layout.template_item, null);
        alertDialog.setView(templateView);
        final CheckBox defaultCheck = (CheckBox) templateView.findViewById(R.id.is_default);
        final EditText nameEdit = (EditText) templateView.findViewById(R.id.template_name);
        String template = null;
        if (existing) {
            Cursor cursor = db.rawQuery(TemplateDatabase.QUERY_BY_ROWID, new String[] { Integer.toString(id) });
            if (cursor.moveToFirst()) {
                boolean isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(TemplateDatabase.DEFAULT_FIELD)) == 1;
                defaultCheck.setChecked(isDefault);
                String name = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.NAME_FIELD));
                template = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.TEMPLATE_FIELD));
                nameEdit.setText(name);
            } else {
                Log.e(DEBUG_TAG, "template id " + Integer.toString(id) + " not found");
            }
            cursor.close();

            alertDialog.setTitle(R.string.edit_template);
            alertDialog.setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(DEBUG_TAG, "deleting template " + Integer.toString(id));
                    TemplateDatabase.delete(db, id);
                    newCursor(db);
                }
            });
        } else {
            alertDialog.setTitle(R.string.save_template);
        }
        alertDialog.setNegativeButton(R.string.Cancel, null);

        final String finalTemplate = template;
        alertDialog.setPositiveButton(R.string.Save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!existing) {
                    TemplateDatabase.add(db, nameEdit.getText().toString(), defaultCheck.isChecked(),
                            text.getText().toString());
                    db.close();
                } else {
                    final String current = text.getText().toString();
                    if (!current.equals(finalTemplate)) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle(R.string.update_template);
                        alertDialog.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TemplateDatabase.update(db, id, nameEdit.getText().toString(), defaultCheck.isChecked(),
                                        current);
                                newCursor(db);
                            }
                        });
                        alertDialog.setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TemplateDatabase.update(db, id, nameEdit.getText().toString(), defaultCheck.isChecked(),
                                        finalTemplate);
                                newCursor(db);
                            }
                        });
                        alertDialog.show();
                    } else {
                        TemplateDatabase.update(db, id, nameEdit.getText().toString(), defaultCheck.isChecked(),
                                current);
                        newCursor(db);
                    }
                }
            }
        });
        alertDialog.show();
    }

    /**
     * Replace the current cursor for the template database
     * 
     * @param db the template database
     */
    private void newCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = db.rawQuery(TemplateDatabase.QUERY_ALL, null);
        Cursor oldCursor = templateAdapter.swapCursor(newCursor);
        oldCursor.close();
        templateAdapter.notifyDataSetChanged();
    }
}
