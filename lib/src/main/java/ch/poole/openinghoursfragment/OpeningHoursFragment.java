package ch.poole.openinghoursfragment;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.DialogFragment;
import ch.poole.openinghoursfragment.pickers.DateRangePicker;
import ch.poole.openinghoursfragment.pickers.OccurrenceInMonthPicker;
import ch.poole.openinghoursfragment.pickers.RangePicker;
import ch.poole.openinghoursfragment.pickers.SetDateRangeListener;
import ch.poole.openinghoursfragment.pickers.SetRangeListener;
import ch.poole.openinghoursfragment.pickers.SetTimeRangeListener;
import ch.poole.openinghoursfragment.pickers.TimeRangePicker;
import ch.poole.openinghoursfragment.templates.TemplateDatabase;
import ch.poole.openinghoursfragment.templates.TemplateDatabaseHelper;
import ch.poole.openinghoursfragment.templates.TemplateDialog;
import ch.poole.openinghoursfragment.templates.TemplateMangementDialog;
import ch.poole.openinghoursfragment.templates.UpdateTextListener;
import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Event;
import ch.poole.openinghoursparser.Holiday;
import ch.poole.openinghoursparser.Holiday.Type;
import ch.poole.openinghoursparser.I18n;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.RuleModifier.Modifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.TokenMgrError;
import ch.poole.openinghoursparser.VarDate;
import ch.poole.openinghoursparser.VariableTime;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
import ch.poole.openinghoursparser.YearRange;
import ch.poole.android.rangebar.RangeBar;
import ch.poole.android.rangebar.RangeBar.PinTextFormatter;

/**
 * DialogFragment that implements an editor for OpenStreetMap opening_hours tags
 * 
 * @author Simon Poole
 *
 */
public class OpeningHoursFragment extends DialogFragment implements SetDateRangeListener, SetRangeListener, SetTimeRangeListener, UpdateTextListener {
    private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();

    private static final String VALUE_KEY          = "value";
    private static final String ORIGINAL_VALUE_KEY = "original_value";
    private static final String KEY_KEY            = "key";
    private static final String REGION_KEY         = "region";
    private static final String OBJECT_KEY         = "object";
    private static final String STYLE_KEY          = "style";
    private static final String RULE_KEY           = "rule";
    private static final String SHOWTEMPLATES_KEY  = "show_templates";
    private static final String TEXTVALUES_KEY     = "text_values";
    private static final String FRAGMENT_KEY       = "fragment";
    private static final String LOCALE_KEY         = "locale";

    protected static final int OSM_MAX_TAG_LENGTH = 255;

    private Context context = null;

    private LayoutInflater inflater = null;

    /**
     * Saved state
     */
    private ValueWithDescription key;
    private String               region;
    private String               object;
    private String               openingHoursValue;
    private String               originalOpeningHoursValue;
    private int                  styleRes = 0;
    private Locale               locale;

    /**
     * If true we use a call back to the parent fragment
     */
    private boolean useFragmentCallback;

    private List<Rule> rules;

    private AutoCompleteTextView text;
    private LinearLayout         errorMessages;

    private OnSaveListener saveListener = null;

    List<String> weekDays = WeekDay.nameValues();

    List<String> months = Month.nameValues();

    private SQLiteDatabase mDatabase;

    private boolean loadedDefault = false;

    private boolean showTemplates = false;

    private List<ValueWithDescription> textValues;

    private OhTextWatcher   watcher;
    private TextTextWatcher textWatcher;

    private AppCompatButton saveButton;

    private View headerLine;

    /**
     * True if we encountered a parse error
     */
    private boolean parseErrorFound;

    /** record if we are not actually adding a OH value */
    private boolean textMode = false;

    static PinTextFormatter extendedTimeFormater = value -> {
        int minutes = Integer.parseInt(value);
        int tempMinutes = minutes - TimeSpan.MAX_TIME;
        return String.format(Locale.US, "%02d", tempMinutes / 60) + ":" + String.format(Locale.US, "%02d", minutes % 60);
    };

    static PinTextFormatter timeFormater = value -> {
        int minutes = Integer.parseInt(value);
        return String.format(Locale.US, "%02d", minutes / 60) + ":" + String.format(Locale.US, "%02d", minutes % 60);
    };

    /**
     * Create a new OpeningHoursFragment with callback to an activity
     * 
     * @param key the key the OH values belongs to
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstance(@NonNull String key, @NonNull String value, int style, int rule) {
        return newInstance(key, value, style, rule, false);
    }

    /**
     * Create a new OpeningHoursFragment with callback to an activity
     * 
     * @param key the key the OH values belongs to
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstance(@NonNull String key, @NonNull String value, int style, int rule, boolean showTemplates) {
        return newInstance(new ValueWithDescription(key, null), null, null, value, style, rule, showTemplates, null, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to an activity
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstance(@NonNull ValueWithDescription key, @NonNull String value, int style, int rule, boolean showTemplates,
            @Nullable ArrayList<ValueWithDescription> textValues) {
        return newInstance(key, null, null, value, style, rule, showTemplates, textValues, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to an activity
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param region the current region
     * @param object the object in question (typically the main osm tag)
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstance(@NonNull ValueWithDescription key, String region, String object, @NonNull String value, int style, int rule,
            boolean showTemplates, @Nullable ArrayList<ValueWithDescription> textValues) {
        return newInstance(key, region, object, value, style, rule, showTemplates, textValues, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to an activity
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param region the current region
     * @param object the object in question (typically the main osm tag)
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @param locale if not null use a different Locale than the default for parser error messages
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstance(@NonNull ValueWithDescription key, String region, String object, @NonNull String value, int style, int rule,
            boolean showTemplates, @Nullable ArrayList<ValueWithDescription> textValues, @Nullable Locale locale) {
        OpeningHoursFragment f = new OpeningHoursFragment();

        Bundle args = new Bundle();
        args.putSerializable(KEY_KEY, key);
        args.putString(REGION_KEY, region);
        args.putString(OBJECT_KEY, object);
        args.putSerializable(VALUE_KEY, value);
        args.putInt(STYLE_KEY, style);
        args.putInt(RULE_KEY, rule);
        args.putBoolean(SHOWTEMPLATES_KEY, showTemplates);
        args.putBoolean(FRAGMENT_KEY, false);
        args.putSerializable(TEXTVALUES_KEY, textValues);
        args.putSerializable(LOCALE_KEY, locale);

        f.setArguments(args);

        return f;
    }

    /**
     * Create a new OpeningHoursFragment with callback to a fragment
     * 
     * @param key the key the OH values belongs to
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstanceForFragment(@NonNull String key, @NonNull String value, int style, int rule) {
        return newInstanceForFragment(key, value, style, rule, false);
    }

    /**
     * Create a new OpeningHoursFragment with callback to a fragment
     * 
     * @param key the key the OH values belongs to
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstanceForFragment(@NonNull String key, @NonNull String value, int style, int rule, boolean showTemplates) {
        return newInstanceForFragment(new ValueWithDescription(key, null), null, null, value, style, rule, showTemplates, null, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to a fragment
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstanceForFragment(@NonNull ValueWithDescription key, @NonNull String value, int style, int rule,
            boolean showTemplates, @Nullable ArrayList<ValueWithDescription> textValues) {
        return newInstanceForFragment(key, null, null, value, style, rule, showTemplates, textValues, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to a fragment
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param region the current region
     * @param object the object in question (typically the main osm tag)
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstanceForFragment(@NonNull ValueWithDescription key, String region, String object, @NonNull String value, int style,
            int rule, boolean showTemplates, @Nullable ArrayList<ValueWithDescription> textValues) {
        return newInstanceForFragment(key, region, object, value, style, rule, showTemplates, textValues, null);
    }

    /**
     * Create a new OpeningHoursFragment with callback to a fragment
     * 
     * @param key the key the OH values belongs to in an ValueWithDescription object
     * @param region the current region
     * @param object the object in question (typically the main osm tag)
     * @param value the OH value
     * @param style resource id for the Android style to use
     * @param rule rule to scroll to or -1 (currently ignored)
     * @param showTemplates if value is empty show the template selector instead of using a default when true
     * @param textValues for tags that can contain both OH and other values a list of possible non-OH values, or null
     * @param locale if not null use a different Locale than the default for parser error messages
     * @return an OpeningHoursFragment
     */
    public static OpeningHoursFragment newInstanceForFragment(@NonNull ValueWithDescription key, String region, String object, @NonNull String value, int style,
            int rule, boolean showTemplates, @Nullable ArrayList<ValueWithDescription> textValues, @Nullable Locale locale) {
        OpeningHoursFragment f = new OpeningHoursFragment();

        Bundle args = new Bundle();
        args.putSerializable(KEY_KEY, key);
        args.putString(REGION_KEY, region);
        args.putString(OBJECT_KEY, object);
        args.putSerializable(VALUE_KEY, value);
        args.putInt(STYLE_KEY, style);
        args.putInt(RULE_KEY, rule);
        args.putBoolean(SHOWTEMPLATES_KEY, showTemplates);
        args.putBoolean(FRAGMENT_KEY, true);
        args.putSerializable(TEXTVALUES_KEY, textValues);
        args.putSerializable(LOCALE_KEY, locale);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        if (!useFragmentCallback) {
            try {
                saveListener = (OnSaveListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnSaveListener");
            }
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

    @SuppressWarnings("unchecked")
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int initialRule = -1;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            key = (ValueWithDescription) savedInstanceState.getSerializable(KEY_KEY);
            region = savedInstanceState.getString(REGION_KEY);
            object = savedInstanceState.getString(OBJECT_KEY);
            openingHoursValue = savedInstanceState.getString(VALUE_KEY);
            originalOpeningHoursValue = savedInstanceState.getString(ORIGINAL_VALUE_KEY);
            styleRes = savedInstanceState.getInt(STYLE_KEY);
            useFragmentCallback = savedInstanceState.getBoolean(FRAGMENT_KEY);
            textValues = (List<ValueWithDescription>) savedInstanceState.getSerializable(TEXTVALUES_KEY);
            locale = (Locale) savedInstanceState.getSerializable(LOCALE_KEY);
        } else {
            key = (ValueWithDescription) getArguments().getSerializable(KEY_KEY);
            region = getArguments().getString(REGION_KEY);
            object = getArguments().getString(OBJECT_KEY);
            openingHoursValue = getArguments().getString(VALUE_KEY);
            originalOpeningHoursValue = openingHoursValue;
            styleRes = getArguments().getInt(STYLE_KEY);
            initialRule = getArguments().getInt(RULE_KEY);
            showTemplates = getArguments().getBoolean(SHOWTEMPLATES_KEY);
            useFragmentCallback = getArguments().getBoolean(FRAGMENT_KEY);
            textValues = (List<ValueWithDescription>) getArguments().getSerializable(TEXTVALUES_KEY);
            locale = (Locale) getArguments().getSerializable(LOCALE_KEY);
        }
        if (styleRes == 0) {
            styleRes = R.style.AlertDialog_AppCompat_Light; // fallback
        }
        if (openingHoursValue == null || "".equals(openingHoursValue)) {
            if (!showTemplates) {
                loadDefault();
                loadedDefault = openingHoursValue != null;
            }
        }

        context = new ContextThemeWrapper(getActivity(), styleRes);
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final LinearLayout openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours, null);

        final ScrollView sv = (ScrollView) openingHoursLayout.findViewById(R.id.openinghours_view);
        watcher = new OhTextWatcher(sv);
        textWatcher = new TextTextWatcher();

        // set parser locale singleton
        I18n.setLocale(locale != null ? locale : Locale.getDefault());

        // check if this is a mixed value tag
        final LinearLayout modeContainer = (LinearLayout) openingHoursLayout.findViewById(R.id.modeContainer);
        headerLine = openingHoursLayout.findViewById(R.id.headerLine);
        errorMessages = (LinearLayout) openingHoursLayout.findViewById(R.id.openinghours_error_messages);
        if (textValues != null) {
            final RadioGroup modeGroup = (RadioGroup) openingHoursLayout.findViewById(R.id.modeGroup);
            final RadioButton useOH = (RadioButton) modeGroup.findViewById(R.id.use_oh);
            final RadioButton useText = (RadioButton) modeGroup.findViewById(R.id.use_text);
            if (textValues.contains(new ValueWithDescription(openingHoursValue, null)) || openingHoursValue == null || "".equals(openingHoursValue)) {
                useText.setChecked(true);
                textMode = true;
                headerLine.setVisibility(View.VISIBLE);
            } else {
                useOH.setChecked(true);
                headerLine.setVisibility(View.GONE);
            }
            modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                openingHoursValue = text.getText().toString();
                text.removeTextChangedListener(watcher);
                text.removeTextChangedListener(textWatcher);
                text.removeCallbacks(updateStringRunnable);
                removeHighlight(text);
                errorMessages.removeAllViews();
                buildLayout(openingHoursLayout, openingHoursValue, -1);
            });
            modeContainer.setVisibility(View.VISIBLE);
        } else {
            modeContainer.setVisibility(View.GONE);
            headerLine.setVisibility(View.GONE);
        }
        buildLayout(openingHoursLayout, openingHoursValue == null ? "" : openingHoursValue, initialRule);

        // add callbacks for the buttons
        AppCompatButton cancel = (AppCompatButton) openingHoursLayout.findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> dismiss());

        if (useFragmentCallback) {
            saveListener = (OnSaveListener) getParentFragment();
        }

        saveButton = (AppCompatButton) openingHoursLayout.findViewById(R.id.save);
        enableSaveButton(openingHoursValue);
        saveButton.setOnClickListener(v -> {
            saveListener.save(key.getValue(), text.getText().toString());
            dismiss();
        });

        return openingHoursLayout;
    }

    /**
     * Try to locate a reasonable default value There probably is a more elegant way to do this
     */
    private void loadDefault() {
        openingHoursValue = TemplateDatabase.getDefault(mDatabase, key.getValue(), region, object);
        if (openingHoursValue == null) {
            openingHoursValue = TemplateDatabase.getDefault(mDatabase, key.getValue(), null, object);
            if (openingHoursValue == null) {
                openingHoursValue = TemplateDatabase.getDefault(mDatabase, key.getValue(), null, null);
                if (openingHoursValue == null) {
                    // didn't find a more specific default try general default now
                    openingHoursValue = TemplateDatabase.getDefault(mDatabase, null, null, null);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    /**
     * Enable the save button if the text has changed
     * 
     * @author simon
     *
     */
    private class TextTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
            enableSaveButton(text.getText().toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // empty
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // empty
        }
    }

    /**
     * Re-parses and rebuilds the form if the text is changed by typing
     * 
     * @author simon
     *
     */
    private class OhTextWatcher implements TextWatcher {
        final ScrollView scrollView;

        /**
         * Construct a new instance
         * 
         * @param scrollView the ScrollView holding the bits that we will want to update
         */
        OhTextWatcher(@NonNull ScrollView scrollView) {
            this.scrollView = scrollView;
        }

        @Override
        public void afterTextChanged(Editable s) {
            Runnable rebuild = () -> {
                OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(text.getText().toString().getBytes()));
                try {
                    rules = parser.rules(false);
                    buildForm(scrollView, rules);
                    removeHighlight(text);
                    errorMessages.removeAllViews();
                } catch (OpeningHoursParseException pex) {
                    Log.d(DEBUG_TAG, pex.getMessage());
                    highlightParseError(text, pex);
                    errorMessages.removeAllViews();
                    for (OpeningHoursParseException ex : pex.getExceptions()) {
                        TextView message = new TextView(getContext());
                        message.setSingleLine();
                        message.setText(ex.getMessage());
                        message.setTextColor(ContextCompat.getColor(getContext(), R.color.error_text));
                        final int column = ex.getColumn() + 1;
                        message.setOnClickListener(v -> text.setSelection(column, Math.min(column + 1, message.length())));
                        errorMessages.addView(message);
                    }
                } catch (TokenMgrError err) {
                    // we currently can't do anything reasonable here except ignore
                    Log.e(DEBUG_TAG, err.getMessage());
                }
                enableSaveButton(text.getText().toString());
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
            // Empty
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Empty
        }
    }

    private OnClickListener autocompleteOnClick = v -> {
        if (v.hasFocus()) {
            ((AutoCompleteTextView) v).showDropDown();
        }
    };

    /**
     * Build the parts of the layout that only need to be done once
     * 
     * @param openingHoursLayout the layout
     * @param openingHoursValue the OH value
     * @param initialRule index of the rule to scroll to, currently ignored
     * @return a ScrollView
     */
    private ScrollView buildLayout(final @NonNull LinearLayout openingHoursLayout, @NonNull String openingHoursValue, final int initialRule) {
        text = (AutoCompleteTextView) openingHoursLayout.findViewById(R.id.openinghours_string_edit);
        String keyDescription = key.getDescription();
        if (keyDescription != null && !"".equals(keyDescription)) {
            text.setHint(keyDescription);
        }
        final ScrollView sv = (ScrollView) openingHoursLayout.findViewById(R.id.openinghours_view);
        if (text != null && sv != null) {
            sv.removeAllViews();

            final FloatingActionButton fab = (FloatingActionButton) openingHoursLayout.findViewById(R.id.more);

            // non-OH support
            if (textValues != null) {
                final RadioGroup modeGroup = (RadioGroup) openingHoursLayout.findViewById(R.id.modeGroup);
                final RadioButton useText = (RadioButton) modeGroup.findViewById(R.id.use_text);
                if (useText.isChecked()) {
                    text.removeTextChangedListener(watcher);
                    text.removeCallbacks(updateStringRunnable);
                    ValueArrayAdapter adapter = new ValueArrayAdapter(getContext(), android.R.layout.simple_spinner_item, textValues);
                    text.setAdapter(adapter);
                    text.setOnClickListener(autocompleteOnClick);
                    text.setOnItemClickListener((parent, view, position, id) -> {
                        Object o = parent.getItemAtPosition(position);
                        if (o instanceof ValueWithDescription) {
                            text.setText(((ValueWithDescription) o).getValue());
                        } else if (o instanceof String) {
                            text.setText((String) o);
                        }
                    });

                    text.setText(openingHoursValue);
                    text.removeTextChangedListener(textWatcher);
                    text.addTextChangedListener(textWatcher);
                    fab.setVisibility(View.GONE);
                    headerLine.setVisibility(View.VISIBLE);
                    textMode = true;
                    return sv;
                } else {
                    text.setAdapter(null);
                    text.setOnClickListener(null);
                    textMode = false;
                    if ("".equals(openingHoursValue)) {
                        if (!showTemplates) {
                            loadDefault();
                            if (!"".equals(openingHoursValue)) {
                                ch.poole.openinghoursfragment.Util.toastTop(getActivity(), getString(R.string.loaded_default));
                            }
                        } else {
                            showTemplates = false;
                            TemplateMangementDialog.showDialog(this, false, key, null, null, text.getText().toString());
                        }
                    }
                    headerLine.setVisibility(View.GONE);
                }
            }
            text.setText(openingHoursValue);
            text.removeTextChangedListener(textWatcher);
            text.removeTextChangedListener(watcher);
            text.addTextChangedListener(watcher);
            fab.setVisibility(View.VISIBLE);

            OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHoursValue.getBytes()));
            try {
                rules = parser.rules(false);
                buildForm(sv, rules);
                removeHighlight(text);
            } catch (OpeningHoursParseException pex) {
                Log.d(DEBUG_TAG, pex.getMessage());
                highlightParseError(text, pex);
            } catch (TokenMgrError err) {
                // we currently can't do anything reasonable here except ignore
                Log.e(DEBUG_TAG, err.getMessage());
            }

            class AddRuleListener implements OnMenuItemClickListener {
                String ruleString;

                /**
                 * Construct a new listener for creating Rules
                 * 
                 * @param rule a String containing a rule
                 */
                AddRuleListener(@NonNull String rule) {
                    ruleString = rule;
                }

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(ruleString.getBytes()));
                    List<Rule> rules2 = null;
                    try {
                        rules2 = parser.rules(false);
                    } catch (ParseException pex) {
                        Log.e(DEBUG_TAG, pex.getMessage());
                    } catch (TokenMgrError err) {
                        Log.e(DEBUG_TAG, err.getMessage());
                    }
                    if (rules2 != null && !rules2.isEmpty()) {
                        if (rules == null || hasParseError()) { // if there was an unparseable string it needs to be
                                                                // fixed first
                            ch.poole.openinghoursfragment.Util.toastTop(getActivity(), R.string.would_overwrite_invalid_value);
                            return true;
                        }
                        rules.add(rules2.get(0));
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                        // scroll to bottom
                        text.postDelayed(() -> ch.poole.openinghoursfragment.Util.scrollToRow(sv, null, false, false), 200);
                    }
                    return true;
                }
            }

            fab.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, fab);

                // menu items for adding rules
                MenuItem addRule = popup.getMenu().add(R.string.add_rule);
                addRule.setOnMenuItemClickListener(new AddRuleListener("Mo 6:00-20:00"));
                MenuItem addRulePH = popup.getMenu().add(R.string.add_rule_closed_on_holidays);
                addRulePH.setOnMenuItemClickListener(new AddRuleListener("PH closed"));
                MenuItem addRule247 = popup.getMenu().add(R.string.add_rule_247);
                addRule247.setOnMenuItemClickListener(new AddRuleListener("24/7"));

                MenuItem loadTemplate = popup.getMenu().add(R.string.load_template);
                loadTemplate.setOnMenuItemClickListener(item -> {
                    TemplateMangementDialog.showDialog(OpeningHoursFragment.this, false, key, region, object, text.getText().toString());
                    return true;
                });
                MenuItem saveTemplate = popup.getMenu().add(R.string.save_to_template);
                saveTemplate.setOnMenuItemClickListener(item -> {
                    TemplateDialog.showDialog(OpeningHoursFragment.this, text.getText().toString(), key, false, -1);
                    return true;
                });
                MenuItem manageTemplate = popup.getMenu().add(R.string.manage_templates);
                manageTemplate.setOnMenuItemClickListener(item -> {
                    TemplateMangementDialog.showDialog(OpeningHoursFragment.this, true, key, region, object, text.getText().toString());
                    return true;
                });
                MenuItem refresh = popup.getMenu().add(R.string.refresh);
                refresh.setOnMenuItemClickListener(item -> {
                    updateString();
                    watcher.afterTextChanged(null); // hack to force rebuild of form
                    return true;
                });
                MenuItem clear = popup.getMenu().add(R.string.clear);
                clear.setOnMenuItemClickListener(item -> {
                    if (rules != null) { // FIXME should likely disable the entry if there is actually nothing to clear
                        rules.clear();
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    } else {
                        text.setText("");
                        watcher.afterTextChanged(null);
                    }
                    return true;
                });
                popup.show();// showing popup menu
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
     * 
     * @param text he EditText the string is displayed in
     * @param pex the ParseException to use
     */
    private void highlightParseError(@NonNull EditText text, @NonNull OpeningHoursParseException ohpex) {
        parseErrorFound = true;
        Spannable spannable = new SpannableString(text.getText());
        boolean first = true;
        int pos = 0;
        for (OpeningHoursParseException pex : ohpex.getExceptions()) {
            int c = pex.currentToken.next.beginColumn - 1; // starts at 1
            spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c, Math.min(c + 1, spannable.length())), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (first) {
                pos = c;
                first = false;
            }
        }
        text.removeTextChangedListener(watcher); // avoid infinite loop
        text.setText(spannable, TextView.BufferType.SPANNABLE);
        text.setSelection(pos, Math.min(pos + 1, spannable.length()));
        text.addTextChangedListener(watcher);
    }

    /**
     * Remove all parse error highlighting
     * 
     * Side effect sets parserErrorFound to false
     * 
     * @param text the EditText the string is displayed in
     */
    private void removeHighlight(@NonNull EditText text) {
        parseErrorFound = false;
        int pos = text.getSelectionStart();
        int prevLen = text.length();
        text.removeTextChangedListener(watcher); // avoid infinite loop
        if (rules != null) {
            String t = ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules);
            text.setText(t);
        }
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
                header.setText(getActivity().getString(groupMode ? R.string.group_header : R.string.rule_header, headerCount));
                RadioButton normal = (RadioButton) groupHeader.findViewById(R.id.normal_rule);
                if (!r.isAdditive() && !r.isFallBack()) {
                    normal.setChecked(true);
                }
                normal.setOnClickListener(v -> {
                    RadioButton rb = (RadioButton) v;
                    if (rb.isChecked()) {
                        r.setFallBack(false);
                        r.setAdditive(false);
                        updateString();
                        watcher.afterTextChanged(null);
                    }
                });
                RadioButton additive = (RadioButton) groupHeader.findViewById(R.id.additive_rule);
                if (r.isAdditive()) {
                    additive.setChecked(true);
                }
                additive.setOnClickListener(v -> {
                    RadioButton rb = (RadioButton) v;
                    if (rb.isChecked()) {
                        r.setFallBack(false);
                        r.setAdditive(true);
                        updateString();
                        watcher.afterTextChanged(null);
                    }
                });
                RadioButton fallback = (RadioButton) groupHeader.findViewById(R.id.fallback_rule);
                if (r.isFallBack()) {
                    fallback.setChecked(true);
                }
                fallback.setOnClickListener(v -> {
                    RadioButton rb = (RadioButton) v;
                    if (rb.isChecked()) {
                        r.setFallBack(true);
                        r.setAdditive(false);
                        rules.remove(r); // move to last position
                        rules.add(r);
                        updateString();
                        watcher.afterTextChanged(null);
                    }
                });

                // add menu items for rules
                Menu menu = addStandardMenuItems(groupHeader, () -> {
                    rules.remove(r);
                    updateString();
                    watcher.afterTextChanged(null); // hack to force rebuild of form
                });

                if (r.getModifier() == null) {
                    MenuItem addModifierAndComment = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_modifier_comment);
                    addModifierAndComment.setOnMenuItemClickListener(item -> {
                        RuleModifier modifier = new RuleModifier();
                        modifier.setModifier(Modifier.CLOSED);
                        r.setModifier(modifier);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    });
                }

                MenuItem addHoliday = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_holiday);
                addHoliday.setOnMenuItemClickListener(item -> {
                    List<Holiday> holidays = r.getHolidays();
                    if (holidays == null) {
                        r.setHolidays(new ArrayList<>());
                        holidays = r.getHolidays();
                    }
                    Holiday holiday = new Holiday();
                    holiday.setType(Type.PH);
                    holidays.add(holiday);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                SubMenu timespanMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.timespan_menu);
                class SetTimeSpanListener implements OnMenuItemClickListener {
                    private final int   start;
                    private final Event startEvent;
                    private final int   end;
                    private final Event endEvent;

                    /**
                     * Create a listener that sets the time
                     * 
                     * @param start start time
                     * @param startEvent start event or null
                     * @param end end time
                     * @param endEvent end event or null
                     */
                    SetTimeSpanListener(int start, @Nullable Event startEvent, int end, @Nullable Event endEvent) {
                        this.start = start;
                        this.startEvent = startEvent;
                        this.end = end;
                        this.endEvent = endEvent;
                    }

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        List<TimeSpan> ts = r.getTimes();
                        if (ts == null) {
                            r.setTimes(new ArrayList<>());
                            ts = r.getTimes();
                        }
                        TimeSpan t = new TimeSpan();
                        if (startEvent == null) {
                            t.setStart(start);
                        } else {
                            VariableTime vt = new VariableTime();
                            vt.setEvent(startEvent);
                            t.setStartEvent(vt);
                        }
                        if (endEvent == null && end > 0) {
                            t.setEnd(end);
                        } else if (endEvent != null) {
                            VariableTime vt = new VariableTime();
                            vt.setEvent(endEvent);
                            t.setEndEvent(vt);
                        }
                        ts.add(t);
                        updateString();
                        watcher.afterTextChanged(null);
                        return true;
                    }
                }

                MenuItem addTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_time);
                addTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(360, null, 1440, null));

                MenuItem addExtendedTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_extended_time);
                addExtendedTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(0, null, 2880, null));

                MenuItem addVarTimeTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_time);
                addVarTimeTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(0, Event.DAWN, 1440, null));

                MenuItem addVarTimeExtendedTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_extended_time);
                addVarTimeExtendedTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(0, Event.DAWN, 2880, null));

                MenuItem addTimeVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_variable_time);
                addTimeVarTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(360, null, 2880, Event.DUSK));

                MenuItem addVarTimeVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_variable_time);
                addVarTimeVarTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(360, Event.DAWN, 2880, Event.DUSK));

                MenuItem addTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time);
                addTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(360, null, -1, null));

                MenuItem addTimeOpenEndTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_open_end);
                addTimeOpenEndTimespan.setOnMenuItemClickListener(item -> {
                    List<TimeSpan> ts = r.getTimes();
                    if (ts == null) {
                        r.setTimes(new ArrayList<>());
                        ts = r.getTimes();
                    }
                    TimeSpan t = new TimeSpan();
                    t.setStart(360);
                    t.setOpenEnded(true);
                    ts.add(t);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                MenuItem addVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time);
                addVarTimeTimespan.setOnMenuItemClickListener(new SetTimeSpanListener(360, Event.DAWN, -1, null));

                MenuItem addVarTimeOpenEndTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_open_end);
                addVarTimeOpenEndTimespan.setOnMenuItemClickListener(item -> {
                    List<TimeSpan> ts = r.getTimes();
                    if (ts == null) {
                        r.setTimes(new ArrayList<>());
                        ts = r.getTimes();
                    }
                    TimeSpan t = new TimeSpan();
                    VariableTime startVt = new VariableTime();
                    startVt.setEvent(Event.DAWN);
                    t.setStartEvent(startVt);
                    t.setOpenEnded(true);
                    ts.add(t);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                MenuItem addWeekdayRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.week_day_range);
                addWeekdayRange.setOnMenuItemClickListener(item -> {
                    List<WeekDayRange> wd = r.getDays();
                    if (wd == null) {
                        r.setDays(new ArrayList<>());
                        wd = r.getDays();
                    }
                    WeekDayRange d = new WeekDayRange();
                    if (wd.isEmpty()) {
                        d.setStartDay("Mo");
                        // d.setEndDay("Su"); a single day is better from an UI pov
                    } else {
                        // add a single day with nth
                        d.setStartDay("Mo");
                        List<Nth> nths = new ArrayList<>();
                        Nth nth = new Nth();
                        nth.setStartNth(1);
                        nths.add(nth);
                        d.setNths(nths);
                    }
                    wd.add(d);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                SubMenu dateRangeMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.daterange_menu);
                MenuItem addDateDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_date);
                addDateDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset dwo = new DateWithOffset();
                    dwo.setMonth(Month.JAN);
                    dateRange.setStartDate(dwo);
                }));
                MenuItem addVarDateDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_date);
                addVarDateDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset dwo = new DateWithOffset();
                    dwo.setVarDate(VarDate.EASTER);
                    dateRange.setStartDate(dwo);
                }));
                MenuItem addDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_variable_date);
                addDateVarDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addVarDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_variable_date);
                addVarDateVarDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceOccurrenceRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_occurrence);
                addOccurrenceOccurrenceRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.FEB);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_date);
                addOccurrenceDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.JAN);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addDateOccurrenceRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_occurrence);
                addDateOccurrenceRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.FEB);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_variable_date);
                addOccurrenceVarDateRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addVarDateOccurrenceRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_occurrence);
                addVarDateOccurrenceRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.JAN);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));

                MenuItem addDateOpenEndRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_open_end);
                addDateOpenEndRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setOpenEnded(true);
                    dateRange.setStartDate(startDwo);
                }));
                MenuItem addVarDateOpenEndRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_open_end);
                addVarDateOpenEndRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    startDwo.setOpenEnded(true);
                    dateRange.setStartDate(startDwo);
                }));
                MenuItem addOccurrenceOpenEndRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_open_end);
                addOccurrenceOpenEndRange.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    startDwo.setOpenEnded(true);
                    dateRange.setStartDate(startDwo);
                }));

                // the same with offsets
                SubMenu dateRangeOffsetMenu = dateRangeMenu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.with_offsets);
                MenuItem addDateDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_date);
                addDateDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset dwo = new DateWithOffset();
                    dwo.setMonth(Month.JAN);
                    dwo.setDay(1);
                    dwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(dwo);
                }));
                MenuItem addVarDateDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_date);
                addVarDateDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset dwo = new DateWithOffset();
                    dwo.setVarDate(VarDate.EASTER);
                    dwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(dwo);
                }));
                MenuItem addDateVarDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_variable_date);
                addDateVarDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setDay(1);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addVarDateVarDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_variable_date);
                addVarDateVarDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceOccurrenceRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_occurrence);
                addOccurrenceOccurrenceRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.FEB);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_date);
                addOccurrenceDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.JAN);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addDateOccurrenceRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_occurrence);
                addDateOccurrenceRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.FEB);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addOccurrenceVarDateRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_variable_date);
                addOccurrenceVarDateRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setVarDate(VarDate.EASTER);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addVarDateOccurrenceRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_occurrence);
                addVarDateOccurrenceRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    dateRange.setStartDate(startDwo);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    DateWithOffset endDwo = new DateWithOffset();
                    endDwo.setMonth(Month.JAN);
                    endDwo.setNth(WeekDay.MO, 1);
                    dateRange.setEndDate(endDwo);
                }));
                MenuItem addDateOpenEndRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_open_end);
                addDateOpenEndRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setDay(1);
                    startDwo.setOpenEnded(true);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                }));
                MenuItem addVarDateOpenEndRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_open_end);
                addVarDateOpenEndRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setVarDate(VarDate.EASTER);
                    startDwo.setOpenEnded(true);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                }));
                MenuItem addOccurrenceOpenEndRangeWithOffsets = dateRangeOffsetMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.occurrence_open_end);
                addOccurrenceOpenEndRangeWithOffsets.setOnMenuItemClickListener(new DateRangeMenuListener(r, (DateRange dateRange) -> {
                    DateWithOffset startDwo = new DateWithOffset();
                    startDwo.setMonth(Month.JAN);
                    startDwo.setNth(WeekDay.MO, 1);
                    startDwo.setOpenEnded(true);
                    startDwo.setWeekDayOffset(WeekDay.MO);
                    dateRange.setStartDate(startDwo);
                }));

                MenuItem addYearRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_year_range);
                addYearRange.setOnMenuItemClickListener(item -> {
                    List<YearRange> years = r.getYears();
                    if (years == null) {
                        r.setYears(new ArrayList<>());
                        years = r.getYears();
                    }
                    YearRange yearRange = new YearRange();
                    yearRange.setStartYear(Calendar.getInstance().get(Calendar.YEAR));
                    years.add(yearRange);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                MenuItem addWeekRange = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_week_range);
                addWeekRange.setOnMenuItemClickListener(item -> {
                    List<WeekRange> weeks = r.getWeeks();
                    if (weeks == null) {
                        r.setWeeks(new ArrayList<>());
                        weeks = r.getWeeks();
                    }
                    WeekRange weekRange = new WeekRange();
                    weekRange.setStartWeek(1);
                    weeks.add(weekRange);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                MenuItem duplicateRule = menu.add(R.string.duplicate_rule);
                duplicateRule.setOnMenuItemClickListener(item -> {
                    Rule duplicate = r.copy();
                    int current = rules.indexOf(r);
                    if (current < 0) { // not found shouldn't happen
                        Log.e(DEBUG_TAG, "Rule missing from list!");
                        return true;
                    }
                    rules.add(Math.max(0, current + 1), duplicate);
                    updateString();
                    watcher.afterTextChanged(null);
                    return true;
                });

                if (!r.equals(rules.get(0))) { // don't show this for the first rule as it is meaningless
                    final View typeView = groupHeader.findViewById(R.id.rule_type_group);
                    MenuItem showRuleType = menu.add(R.string.rule_type);
                    showRuleType.setOnMenuItemClickListener(item -> {
                        typeView.setVisibility(View.VISIBLE);
                        groupHeader.invalidate();
                        return true;
                    });
                    MenuItem moveUp = menu.add(R.string.move_up);
                    moveUp.setOnMenuItemClickListener(item -> {
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
                    });
                }
                if (!r.equals(rules.get(rules.size() - 1))) {
                    MenuItem moveDown = menu.add(R.string.move_down);
                    moveDown.setOnMenuItemClickListener(item -> {
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
                    });
                }
                ll.addView(groupHeader);
                // comments
                String comment = r.getComment();
                if (comment != null && !"".equals(comment)) {
                    LinearLayout intervalLayout = (LinearLayout) inflater.inflate(R.layout.comment, null);
                    EditText commentComment = (EditText) intervalLayout.findViewById(R.id.comment);
                    commentComment.setText(comment);
                    setTextWatcher(commentComment, r::setComment);
                    addStandardMenuItems(intervalLayout, null);
                    ll.addView(intervalLayout);
                }
                if (r.isTwentyfourseven()) {
                    Log.d(DEBUG_TAG, "24/7 " + r.toString());
                    LinearLayout twentyFourSeven = (LinearLayout) inflater.inflate(R.layout.twentyfourseven, null);
                    addStandardMenuItems(twentyFourSeven, () -> {
                        r.setTwentyfourseven(false);
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    });
                    ll.addView(twentyFourSeven);
                }
                // year range list
                final List<YearRange> years = r.getYears();
                if (years != null && !years.isEmpty()) {
                    for (final YearRange yr : years) {
                        addYearRangeUI(ll, r, years, yr);
                    }
                }
                // week range list
                final List<WeekRange> weeks = r.getWeeks();
                if (weeks != null && !weeks.isEmpty()) {
                    for (final WeekRange w : weeks) {
                        addWeekRangeUI(ll, r, weeks, w);
                    }
                }
                // date range list
                final List<DateRange> dateRanges = r.getDates();
                if (dateRanges != null && !dateRanges.isEmpty()) {
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
            if (holidays != null && !holidays.isEmpty()) {
                for (final Holiday hd : holidays) {
                    addHolidayUI(ll, r, holidays, hd);
                }
            }
            // week day list
            final List<WeekDayRange> days = r.getDays();
            if (days != null && !days.isEmpty()) {
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
                Util.setSpinnerInitialEntryValue(getResources(), R.array.modifier_values, modifierSpinner,
                        rm.getModifier() == null ? "" : rm.getModifier().toString());
                setSpinnerListenerEntryValues(R.array.modifier_values, modifierSpinner, rm::setModifier);
                EditText modifierComment = (EditText) modifierLayout.findViewById(R.id.comment);
                modifierComment.setText(rm.getComment());
                setTextWatcher(modifierComment, rm::setComment);
                addStandardMenuItems(modifierLayout, () -> {
                    r.setModifier(null);
                    updateString();
                    watcher.afterTextChanged(null); // hack to force rebuild of form
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
        final Rule              r;

        /**
         * Construct a new listener for date range menu entries
         * 
         * @param r the Rule
         * @param datesAdder call back to add the date
         */
        DateRangeMenuListener(@NonNull Rule r, @NonNull DateMenuInterface datesAdder) {
            this.r = r;
            this.datesAdder = datesAdder;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            List<DateRange> mdr = r.getDates();
            if (mdr == null) {
                r.setDates(new ArrayList<>());
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

    /**
     * Create the week day ui
     * 
     * @param ll the LinearLayout container
     * @param days a list of WeekDayRanges
     */
    private void addWeekDayUI(@NonNull LinearLayout ll, @NonNull final List<WeekDayRange> days) {
        final LinearLayout weekDayRow = (LinearLayout) inflater.inflate(R.layout.weekday_range_row, null);
        final RelativeLayout weekDayContainer = (RelativeLayout) weekDayRow.findViewById(R.id.weekDayContainer);

        Menu menu = addStandardMenuItems(weekDayRow, () -> {
            List<WeekDayRange> temp = new ArrayList<>(days);
            for (WeekDayRange d : temp) {
                if (d.getNths() == null) {
                    days.remove(d);
                }
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        });
        // menu item will be enabled/disabled depending on number of days etc.
        MenuItem nthMenuItem = menu.add(R.string.occurrence_in_month);
        nthMenuItem.setOnMenuItemClickListener(item -> {
            LinearLayout nthLayout = (LinearLayout) inflater.inflate(R.layout.nth, null);
            RelativeLayout nthContainer = (RelativeLayout) nthLayout.findViewById(R.id.nthContainer);
            setNthListeners(nthContainer, days.get(0));
            weekDayRow.addView(nthLayout);
            item.setEnabled(false);
            return true;
        });

        List<WeekDayRange> normal = new ArrayList<>();
        List<WeekDayRange> withNth = new ArrayList<>();
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
                    if (endIndex < startIndex) { // handle wrap around spec by splitting in two
                        Log.d(DEBUG_TAG, "wraparound weekday spec");
                        for (int i = startIndex; i <= WeekDay.SU.ordinal(); i++) {
                            checkWeekDay(weekDayContainer, weekDays.get(i));
                        }
                        startIndex = WeekDay.MO.ordinal();
                    }
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
                final RelativeLayout weekDayContainerNth = (RelativeLayout) weekDayRowNth.findViewById(R.id.weekDayContainer);
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
                List<WeekDayRange> inContainer = new ArrayList<>();
                inContainer.add(d);
                setWeekDayListeners(weekDayContainerNth, days, inContainer, true, nthMenuItem);
                setNthListeners(nthContainer, d);
                weekDayRowNth.addView(nthLayout);
                menu = addStandardMenuItems(weekDayRowNth, () -> {
                    days.remove(d);
                    updateString();
                    watcher.afterTextChanged(null); // hack to force rebuild of form
                });

                final View offsetContainer = weekDayRowNth.findViewById(R.id.offset_container);
                int offsetValue = d.getOffset();
                setOffsetVisibility(offsetValue, menu, offsetContainer);

                EditText offset = (EditText) nthLayout.findViewById(R.id.offset);
                offset.setText(Integer.toString(offsetValue));
                setTextWatcher(offset, value -> {
                    int o = 0;
                    try {
                        o = Integer.parseInt(value);
                    } catch (NumberFormatException nfex) {
                        // Empty
                    }
                    d.setOffset(o);
                });

                ll.addView(weekDayRowNth);
            }
        }
    }

    SetDateRangeListener realDateRangeListener = null;

    @Override
    public void setDateRange(int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear, Month endMonth,
            WeekDay endWeekday, int endDay, VarDate endVarDate) {
        if (realDateRangeListener != null) {
            realDateRangeListener.setDateRange(startYear, startMonth, startWeekday, startDay, startVarDate, endYear, endMonth, endWeekday, endDay, endVarDate);
        }
    }

    /**
     * Add the DateRange UI
     * 
     * @param ll the LinearLayout container
     * @param r the Rule
     * @param dateRanges a List of DateRanges to display holding the range we want to display
     * @param dateRange the DateRange we want to display
     */
    private void addDateRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<DateRange> dateRanges,
            @NonNull final DateRange dateRange) {
        // cases to consider
        // date - date
        // vardate - date
        // date - vardate
        // vardate - vardate
        // date open ended
        // vardate open ended
        //
        // occurrence - occurrence
        // date - occurrence
        // occurrence - date
        // occurrence open ended
        //
        // and the same with offsets
        final DateWithOffset startDate = dateRange.getStartDate();
        final DateWithOffset endDate = dateRange.getEndDate();

        boolean hasStartOffset = startDate.getWeekDayOffset() != null || startDate.getDayOffset() != 0;
        boolean hasEndOffset = endDate != null && (endDate.getWeekDayOffset() != null || endDate.getDayOffset() != 0);
        boolean hasStartOccurrence = startDate.getNthWeekDay() != null;
        boolean hasEndOccurrence = endDate != null && endDate.getNthWeekDay() != null;

        int layoutRes = 0;
        if (hasStartOffset || hasEndOffset) {
            layoutRes = R.layout.daterange_with_offset_row;
        } else if (hasStartOccurrence || hasEndOccurrence) {
            layoutRes = R.layout.daterange_occurrence_row;
        } else {
            layoutRes = R.layout.daterange_row;
        }
        final LinearLayout dateRangeRow = (LinearLayout) inflater.inflate(layoutRes, null);

        // add menus
        final Menu menu = addStandardMenuItems(dateRangeRow, () -> {
            dateRanges.remove(dateRange);
            if (dateRanges.isEmpty()) {
                r.setDates(null);
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        });

        if (hasStartOffset || hasEndOffset || hasStartOccurrence || hasEndOccurrence) {
            LinearLayout startDateLayout = (LinearLayout) dateRangeRow.findViewById(R.id.startDate);
            LinearLayout endDateLayout = (LinearLayout) dateRangeRow.findViewById(R.id.endDate);
            setDateRangeValues(startDate, endDate, dateRangeRow, menu);
            startDateLayout.setOnClickListener(v -> {
                final DateWithOffset start = startDate;
                if (start.getVarDate() == null && start.getNthWeekDay() == null) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        start.setYear(startYear);
                        start.setMonth(startMonth);
                        start.setDay(startDay);
                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(start, null, finalRow, menu);
                        updateString();
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, start.getYear(), start.getMonth(), start.getDay());
                } else if (start.getVarDate() != null && start.getNthWeekDay() == null) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        start.setYear(startYear);
                        start.setVarDate(startVarDate);
                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(start, null, finalRow, menu);
                        updateString();
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, start.getYear(), start.getVarDate());
                } else if (start.getNthWeekDay() != null) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        start.setYear(startYear);
                        start.setMonth(startMonth);
                        start.setNth(startWeekday, startDay);
                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(start, null, finalRow, menu);
                        updateString();
                    };
                    OccurrenceInMonthPicker.showDialog(OpeningHoursFragment.this, R.string.date_weekday_occurrence, start.getYear(), start.getMonth(),
                            start.getNthWeekDay(), start.getNth());
                } else {
                    Log.e(DEBUG_TAG, "Unsupported date " + start);
                }
            });
            endDateLayout.setOnClickListener(v -> {
                if (endDate == null || (endDate.getVarDate() == null && endDate.getNthWeekDay() == null)) {
                    final DateWithOffset end;
                    if (endDate == null) {
                        end = new DateWithOffset();
                        end.setMonth(Month.JAN);
                    } else {
                        end = endDate;
                    }
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        // we are only using the first NPV thats why we need to use the startXXX values
                        // here
                        DateWithOffset tempDwo = endDate;
                        if (tempDwo == null
                                && (startYear != YearRange.UNDEFINED_YEAR || startMonth != null || startDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
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
                            text.postDelayed(() -> watcher.afterTextChanged(null), 100);
                        }
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, end.getYear(), end.getMonth(), end.getDay());
                } else if (endDate.getVarDate() != null && endDate.getNthWeekDay() == null) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        // we are only using the first NPV thats why we need to use the startXXX values
                        // here
                        endDate.setYear(startYear);
                        endDate.setVarDate(startVarDate);
                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(startDate, endDate, finalRow, menu);
                        updateString();
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, endDate.getYear(), endDate.getVarDate());
                } else if (endDate.getNthWeekDay() != null) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        endDate.setYear(startYear);
                        endDate.setMonth(startMonth);
                        endDate.setNth(startWeekday, startDay);
                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(startDate, endDate, finalRow, menu);
                        updateString();
                    };
                    OccurrenceInMonthPicker.showDialog(OpeningHoursFragment.this, R.string.date_weekday_occurrence, endDate.getYear(), endDate.getMonth(),
                            endDate.getNthWeekDay(), endDate.getNth());
                } else {
                    Log.e(DEBUG_TAG, "Unsupported date " + endDate);
                }
            });
        } else {
            setDateRangeValues(startDate, endDate, dateRangeRow, menu);
            RelativeLayout dateRangeLayout = (RelativeLayout) dateRangeRow.findViewById(R.id.daterange_container);
            dateRangeLayout.setOnClickListener(v -> {
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
                        realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                                Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                            start.setYear(startYear);
                            start.setMonth(startMonth);
                            start.setDay(startDay);
                            final LinearLayout finalRow = dateRangeRow;
                            setDateRangeValues(start, null, finalRow, menu);
                            updateString();
                        };
                        DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, start.getYear(), start.getMonth(), start.getDay());
                    } else {
                        realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                                Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                            start.setYear(startYear);
                            start.setMonth(startMonth);
                            start.setDay(startDay);
                            DateWithOffset tempDwo = endDate;
                            if (tempDwo == null && (endYear != YearRange.UNDEFINED_YEAR || endMonth != null || endDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
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
                                // force endDate to be set
                                text.postDelayed(() -> watcher.afterTextChanged(null), 100);
                            }
                        };
                        DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date_range, start.getYear(), start.getMonth(), start.getDay(),
                                tempEndYear, tempEndMonth, tempEndDay);
                    }
                } else if (start.getVarDate() != null && (end == null || end.getVarDate() == null)) {
                    if (start.isOpenEnded()) {
                        realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                                Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                            start.setYear(startYear);
                            start.setVarDate(startVarDate);
                            final LinearLayout finalRow = dateRangeRow;
                            setDateRangeValues(start, null, finalRow, menu);
                            updateString();
                        };
                        DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date, start.getYear(), start.getVarDate());
                    } else {
                        realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                                Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                            start.setYear(startYear);
                            start.setVarDate(startVarDate);
                            DateWithOffset tempDwo = endDate;
                            if (tempDwo == null && (endYear != YearRange.UNDEFINED_YEAR || endMonth != null || endDay != DateWithOffset.UNDEFINED_MONTH_DAY)) {
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
                                // force endDate to be set
                                text.postDelayed(() -> watcher.afterTextChanged(null), 100);
                            }
                        };
                        DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date_range, start.getYear(), start.getVarDate(), tempEndYear,
                                tempEndMonth, tempEndDay);
                    }
                } else if (start.getVarDate() == null && (end != null && end.getVarDate() != null)) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        start.setYear(startYear);
                        start.setMonth(startMonth);
                        start.setDay(startDay);

                        endDate.setYear(endYear);
                        endDate.setVarDate(endVarDate);

                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(start, endDate, finalRow, menu);
                        updateString();
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date_range, start.getYear(), start.getMonth(), start.getDay(), tempEndYear,
                            end.getVarDate());
                } else if (start.getVarDate() != null && (end != null && end.getVarDate() != null)) {
                    realDateRangeListener = (int startYear, Month startMonth, WeekDay startWeekday, int startDay, VarDate startVarDate, int endYear,
                            Month endMonth, WeekDay endWeekday, int endDay, VarDate endVarDate) -> {
                        start.setYear(startYear);
                        start.setVarDate(startVarDate);

                        endDate.setYear(endYear);
                        endDate.setVarDate(endVarDate);

                        final LinearLayout finalRow = dateRangeRow;
                        setDateRangeValues(start, endDate, finalRow, menu);
                        updateString();
                    };
                    DateRangePicker.showDialog(OpeningHoursFragment.this, R.string.date_range, start.getYear(), start.getVarDate(), tempEndYear,
                            end.getVarDate());
                }
            });
        }
        ll.addView(dateRangeRow);
    }

    private void setDateRangeValues(@NonNull final DateWithOffset start, @Nullable final DateWithOffset end, @NonNull LinearLayout dateRangeRow,
            @NonNull Menu menu) {
        TextView startYearView = (TextView) dateRangeRow.findViewById(R.id.startYear);
        if (start.getYear() != YearRange.UNDEFINED_YEAR) {
            startYearView.setText(Integer.toString(start.getYear()));
        } else {
            startYearView.setText("");
        }
        TextView startDelimiter = (TextView) dateRangeRow.findViewById(R.id.startMonthDayDelimiter);
        TextView startMonthView = (TextView) dateRangeRow.findViewById(R.id.startMonth);
        TextView startWeekdayView = (TextView) dateRangeRow.findViewById(R.id.startWeekday);
        TextView startDayView = (TextView) dateRangeRow.findViewById(R.id.startMonthDay);
        if (start.getVarDate() == null && start.getNthWeekDay() == null) {
            startDelimiter.setVisibility(View.VISIBLE);
            startDayView.setVisibility(View.VISIBLE);
            if (startWeekdayView != null) {
                startWeekdayView.setVisibility(View.GONE);
            }
            if (start.getMonth() != null) {
                startMonthView.setText(getResources().getStringArray(R.array.months_entries)[start.getMonth().ordinal()]);
            }

            if (start.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                startDayView.setText(Integer.toString(start.getDay()));
            } else {
                startDayView.setText("");
            }
        } else if (start.getVarDate() != null && start.getNthWeekDay() == null) {
            startDelimiter.setVisibility(View.GONE);
            startDayView.setVisibility(View.GONE);
            if (startWeekdayView != null) {
                startWeekdayView.setVisibility(View.GONE);
            }
            startMonthView.setText(getResources().getStringArray(R.array.vardate_entries)[start.getVarDate().ordinal()]);
        } else if (start.getNthWeekDay() != null) {
            startDelimiter.setVisibility(View.VISIBLE);
            startDayView.setVisibility(View.VISIBLE);
            startWeekdayView.setVisibility(View.VISIBLE);
            if (start.getMonth() != null) {
                startMonthView.setText(getResources().getStringArray(R.array.months_entries)[start.getMonth().ordinal()]);
            }
            if (start.getNthWeekDay() != null) {
                startWeekdayView.setText(getResources().getStringArray(R.array.weekdays_entries)[start.getNthWeekDay().ordinal()]);
            }
            if (start.getNth() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                startDayView.setText("[" + Integer.toString(start.getNth()) + "]");
            } else {
                startDayView.setText("");
            }
        } else {
            Log.e(DEBUG_TAG, "Unsupported date " + start);
        }
        // offset stuff
        RelativeLayout startWeekDayContainer = (RelativeLayout) dateRangeRow.findViewById(R.id.startWeekDayContainer);
        if (startWeekDayContainer != null) {
            checkWeekDay(startWeekDayContainer, start.getWeekDayOffset() != null ? start.getWeekDayOffset().toString() : null);
            setWeekDayListeners(startWeekDayContainer, start);
            final Spinner offsetTypeSpinner = (Spinner) dateRangeRow.findViewById(R.id.startOffsetType);
            Util.setSpinnerInitialEntryValue(getResources(), R.array.offset_type_values, offsetTypeSpinner, start.isWeekDayOffsetPositive() ? "+" : "-");
            setSpinnerListenerEntryValues(R.array.offset_type_values, offsetTypeSpinner, value -> start.setWeekDayOffsetPositive("+".equals(value)));
        }
        final LinearLayout startOffsetContainer = (LinearLayout) dateRangeRow.findViewById(R.id.start_offset_container);
        if (startOffsetContainer != null) {
            EditText offset = (EditText) startOffsetContainer.findViewById(R.id.start_offset);
            setTextWatcher(offset, value -> {
                int o = 0;
                try {
                    o = Integer.parseInt(value);
                } catch (NumberFormatException nfex) {
                    // Empty
                }
                start.setDayOffset(o);
            });
            if (start.getDayOffset() != 0) {
                offset.setText(Integer.toString(start.getDayOffset()));
                startOffsetContainer.setVisibility(View.VISIBLE);
            } else {
                startOffsetContainer.setVisibility(View.GONE);
                if (!itemExistsInMenu(menu, R.string.show_start_offset)) {
                    MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_start_offset);
                    showOffset.setOnMenuItemClickListener(item -> {
                        startOffsetContainer.setVisibility(View.VISIBLE);
                        return true;
                    });
                }
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
            TextView endWeekdayView = (TextView) dateRangeRow.findViewById(R.id.endWeekday);
            TextView endDayView = (TextView) dateRangeRow.findViewById(R.id.endMonthDay);
            if (end.getVarDate() == null && end.getNthWeekDay() == null) {
                endDelimiter.setVisibility(View.VISIBLE);
                endDayView.setVisibility(View.VISIBLE);
                if (endWeekdayView != null) {
                    endWeekdayView.setVisibility(View.GONE);
                }

                if (end.getMonth() != null) {
                    endMonthView.setText(getResources().getStringArray(R.array.months_entries)[end.getMonth().ordinal()]);
                } else {
                    endMonthView.setText("");
                }
                if (end.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                    endDayView.setText(Integer.toString(end.getDay()));
                } else {
                    endDayView.setText("");
                }
            } else if (end.getVarDate() != null && end.getNthWeekDay() == null) {
                endDelimiter.setVisibility(View.GONE);
                endDayView.setVisibility(View.GONE);
                if (endWeekdayView != null) {
                    endWeekdayView.setVisibility(View.GONE);
                }
                endMonthView.setText(getResources().getStringArray(R.array.vardate_entries)[end.getVarDate().ordinal()]);
            } else if (end.getNthWeekDay() != null) {
                endDelimiter.setVisibility(View.VISIBLE);
                endDayView.setVisibility(View.VISIBLE);
                endWeekdayView.setVisibility(View.VISIBLE);
                if (end.getMonth() != null) {
                    endMonthView.setText(getResources().getStringArray(R.array.months_entries)[end.getMonth().ordinal()]);
                }
                if (end.getNthWeekDay() != null) {
                    endWeekdayView.setText(getResources().getStringArray(R.array.weekdays_entries)[end.getNthWeekDay().ordinal()]);
                }
                if (end.getNth() != DateWithOffset.UNDEFINED_MONTH_DAY) {
                    endDayView.setText("[" + Integer.toString(end.getNth()) + "]");
                } else {
                    endDayView.setText("");
                }
            } else {
                Log.e(DEBUG_TAG, "Unsupported date " + end);
            }
            Log.d(DEBUG_TAG, "month " + end.getMonth() + " day " + end.getDay() + " var date " + end.getVarDate());
            if ((end.getMonth() != null && end.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) || end.getVarDate() != null || end.getNthWeekDay() != null) {
                // offset stuff
                setEnableEndDateOffsets(dateRangeRow, true);
                RelativeLayout endWeekDayContainer = (RelativeLayout) dateRangeRow.findViewById(R.id.endWeekDayContainer);
                if (endWeekDayContainer != null) {
                    checkWeekDay(endWeekDayContainer, end.getWeekDayOffset() != null ? end.getWeekDayOffset().toString() : null);
                    setWeekDayListeners(endWeekDayContainer, end);
                    final Spinner offsetTypeSpinner = (Spinner) dateRangeRow.findViewById(R.id.endOffsetType);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.offset_type_values, offsetTypeSpinner, end.isWeekDayOffsetPositive() ? "+" : "-");
                    setSpinnerListenerEntryValues(R.array.offset_type_values, offsetTypeSpinner, value -> {
                        end.setWeekDayOffsetPositive("+".equals(value));
                        updateString();
                    });
                }
                final LinearLayout endOffsetContainer = (LinearLayout) dateRangeRow.findViewById(R.id.end_offset_container);
                if (endOffsetContainer != null) {
                    EditText offset = (EditText) endOffsetContainer.findViewById(R.id.end_offset);
                    setTextWatcher(offset, value -> {
                        int o = 0;
                        try {
                            o = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        end.setDayOffset(o);
                        updateString();
                    });
                    if (end.getDayOffset() != 0) {
                        offset.setText(Integer.toString(end.getDayOffset()));
                        endOffsetContainer.setVisibility(View.VISIBLE);
                    } else {
                        endOffsetContainer.setVisibility(View.GONE);
                        if (!itemExistsInMenu(menu, R.string.show_end_offset)) {
                            MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_end_offset);
                            showOffset.setOnMenuItemClickListener(item -> {
                                endOffsetContainer.setVisibility(View.VISIBLE);
                                return true;
                            });
                        }
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

    /**
     * Check if a string resourse is already in use in a specific menu
     * 
     * @param menu menu to check
     * @param stringRes string resource we are interested in
     * @return true if found
     */
    private boolean itemExistsInMenu(Menu menu, int stringRes) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            if (mi.getTitle().equals(getString(stringRes))) {
                return true;
            }
        }
        return false;
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

    private void addHolidayUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<Holiday> holidays, @NonNull final Holiday hd) {
        LinearLayout holidayRow = (LinearLayout) inflater.inflate(R.layout.holiday_row, null);
        Spinner holidaysSpinner = (Spinner) holidayRow.findViewById(R.id.holidays);
        Util.setSpinnerInitialEntryValue(getResources(), R.array.holidays_values, holidaysSpinner, hd.getType().name());
        setSpinnerListenerEntryValues(R.array.holidays_values, holidaysSpinner, value -> hd.setType(Holiday.Type.valueOf(value)));
        Menu menu = addStandardMenuItems(holidayRow, () -> {
            holidays.remove(hd);
            if (holidays.isEmpty()) {
                r.setHolidays(null);
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        });

        final CheckBox useAsWeekDay = (CheckBox) holidayRow.findViewById(R.id.checkBoxUseAsWeekDay);
        useAsWeekDay.setChecked(hd.getUseAsWeekDay());
        useAsWeekDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (Holiday h : holidays) {
                h.setUseAsWeekDay(isChecked);
            }
            updateString();
            text.postDelayed(() -> watcher.afterTextChanged(null), 100);
        });

        final View useAsWeekDayContainer = holidayRow.findViewById(R.id.useAsWeekDay_container);
        if (!hd.getUseAsWeekDay()) {
            useAsWeekDayContainer.setVisibility(View.VISIBLE);
        } else {
            useAsWeekDayContainer.setVisibility(View.GONE);
            MenuItem showUse = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_useAsWeekDay);
            showUse.setOnMenuItemClickListener(item -> {
                useAsWeekDayContainer.setVisibility(View.VISIBLE);
                return true;
            });
        }

        final View offsetContainer = holidayRow.findViewById(R.id.offset_container);

        int offsetValue = hd.getOffset();
        setOffsetVisibility(offsetValue, menu, offsetContainer);

        EditText offset = (EditText) holidayRow.findViewById(R.id.offset);
        offset.setText(Integer.toString(offsetValue));
        setTextWatcher(offset, value -> {
            int o = 0;
            try {
                o = Integer.parseInt(value);
            } catch (NumberFormatException nfex) {
                // Empty
            }
            hd.setOffset(o);
        });
        ll.addView(holidayRow);
    }

    /**
     * Show or hide the offset field and if hidden add a menu entry
     * 
     * @param offset the current offset value
     * @param menu the menu to add an entry to
     * @param offsetContainer the container view
     */
    private void setOffsetVisibility(final int offset, @NonNull Menu menu, @NonNull final View offsetContainer) {
        if (offset != 0) {
            offsetContainer.setVisibility(View.VISIBLE);
        } else {
            offsetContainer.setVisibility(View.GONE);
            MenuItem showOffset = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_offset);
            showOffset.setOnMenuItemClickListener(item -> {
                offsetContainer.setVisibility(View.VISIBLE);
                return true;
            });
        }
    }

    private void addYearRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<YearRange> years, @NonNull final YearRange yr) {
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
        setTextWatcher(yearIntervalEdit, value -> {
            int interval = 0;
            try {
                interval = Integer.parseInt(value);
            } catch (NumberFormatException nfex) {
                // Empty
            }
            yr.setInterval(interval);
        });
        Menu menu = addStandardMenuItems(yearLayout, () -> {
            years.remove(yr);
            if (years.isEmpty()) {
                r.setYears(null);
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        });
        addShowIntervalItem(intervalContainer, menu);

        LinearLayout range = (LinearLayout) yearLayout.findViewById(R.id.range);
        range.setOnClickListener(v -> {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int rangeEnd = Math.max(currentYear, startYear) + 50;
            int tempEndYear = yr.getEndYear();
            if (tempEndYear == YearRange.UNDEFINED_YEAR) {
                tempEndYear = RangePicker.NOTHING_SELECTED;
            } else {
                rangeEnd = Math.max(rangeEnd, tempEndYear + 50);
            }
            realSetRangeListener = (int start, int end) -> {
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
            };
            RangePicker.showDialog(OpeningHoursFragment.this, R.string.year_range, YearRange.FIRST_VALID_YEAR, rangeEnd, yr.getStartYear(), tempEndYear);
        });
        ll.addView(yearLayout);
    }

    /**
     * Add a menu item to show the interval field
     * 
     * @param intervalContainer the container view
     * @param menu the menu we want to add an item to
     */
    private void addShowIntervalItem(@NonNull final View intervalContainer, @NonNull Menu menu) {
        MenuItem showInterval = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_interval);
        showInterval.setOnMenuItemClickListener(item -> {
            intervalContainer.setVisibility(View.VISIBLE);
            return true;
        });
    }

    SetRangeListener realSetRangeListener = null;

    @Override
    public void setRange(int start, int end) {
        if (realSetRangeListener != null) {
            realSetRangeListener.setRange(start, end);
        }
    }

    private void addWeekRangeUI(@NonNull LinearLayout ll, @NonNull final Rule r, @NonNull final List<WeekRange> weeks, @NonNull final WeekRange w) {
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
        setTextWatcher(weekIntervalEdit, value -> {
            int interval = 0;
            try {
                interval = Integer.parseInt(value);
            } catch (NumberFormatException nfex) {
                // Empty
            }
            w.setInterval(interval);
        });
        Menu menu = addStandardMenuItems(weekLayout, () -> {
            weeks.remove(w);
            if (weeks.isEmpty()) {
                r.setWeeks(null);
            }
            updateString();
            watcher.afterTextChanged(null); // hack to force rebuild of form
        });
        addShowIntervalItem(intervalContainer, menu);

        LinearLayout range = (LinearLayout) weekLayout.findViewById(R.id.range);
        range.setOnClickListener(v -> {
            // need to reget values from w
            int tempEndWeek = w.getEndWeek();
            if (tempEndWeek == WeekRange.UNDEFINED_WEEK) {
                tempEndWeek = RangePicker.NOTHING_SELECTED;
            }
            realSetRangeListener = (int start, int end) -> {
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
            };
            RangePicker.showDialog(OpeningHoursFragment.this, R.string.week_range, WeekRange.MIN_WEEK, WeekRange.MAX_WEEK, w.getStartWeek(), tempEndWeek);
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
                // Empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Empty
            }

            @Override
            public void afterTextChanged(Editable s) {
                listener.set(s.toString());
                updateString();
            }
        });
    }

    private class DeleteTimeSpan implements Delete {
        final List<TimeSpan> times;
        final TimeSpan       ts;

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
        if (times != null && !times.isEmpty()) {
            Log.d(DEBUG_TAG, "#time spans " + times.size());
            Menu menu = null;
            for (final TimeSpan ts : times) {
                boolean hasStartEvent = ts.getStartEvent() != null;
                boolean hasEndEvent = ts.getEndEvent() != null;
                final boolean extendedTime = ts.getEnd() > 1440;
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
                    timeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setStart((int) (leftPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        ts.setEnd((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    menu = addStandardMenuItems(timeRangeRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts, timeBar);
                    addTimeSpanMenus(timeBar, null, menu);

                    ll.addView(timeRangeRow);
                } else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() >= 0 && extendedTime) {
                    Log.d(DEBUG_TAG, "t-x " + ts.toString());
                    LinearLayout timeExtendedRangeRow = (LinearLayout) inflater.inflate(R.layout.time_extended_range_row, null);
                    RangeBar timeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.timebar);
                    RangeBar extendedTimeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.extendedTimebar);
                    int start = ts.getStart();
                    int end = ts.getEnd();
                    setGranuarlty(timeBar, start, end);
                    setGranuarlty(extendedTimeBar, start, end);
                    timeBar.setRangePinsByValue(0, start);
                    timeBar.setPinTextFormatter(timeFormater);
                    timeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setStart((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    extendedTimeBar.setRangePinsByValue(1440, end);
                    extendedTimeBar.setPinTextFormatter(extendedTimeFormater);
                    extendedTimeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setEnd((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    menu = addStandardMenuItems(timeExtendedRangeRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts, timeBar);
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
                    timeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setStart((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts, timeBar);
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
                    timeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setStart((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.events_values, endEvent, ts.getEndEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, endEvent, value -> ts.getEndEvent().setEvent(value));
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts, timeBar);
                    addTimeSpanMenus(timeBar, null, menu);

                    final View offsetContainer = timeEventRow.findViewById(R.id.offset_container);
                    int offsetValue = ts.getEndEvent().getOffset();
                    setOffsetVisibility(offsetValue, menu, offsetContainer);

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.offset);
                    offset.setText(Integer.toString(offsetValue));
                    setTextWatcher(offset, value -> {
                        int o = 0;
                        try {
                            o = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        ts.getEndEvent().setOffset(o);
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
                        bar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                            ts.setEnd((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                            updateString();
                        });
                    } else {
                        timeBar.setVisibility(View.GONE);
                        extendedTimeBar.setVisibility(View.GONE);
                    }
                    Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.events_values, startEvent, ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, value -> ts.getStartEvent().setEvent(value));
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    if (timeBar.getVisibility() == View.VISIBLE) {
                        addTimePickerMenu(menu, ts, timeBar);
                        addTimeSpanMenus(timeBar, null, menu);
                    }
                    final View offsetContainer = timeEventRow.findViewById(R.id.offset_container);
                    int offsetValue = ts.getStartEvent().getOffset();
                    setOffsetVisibility(offsetValue, menu, offsetContainer);

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.offset);
                    offset.setText(Integer.toString(offsetValue));
                    setTextWatcher(offset, value -> {
                        int o = 0;
                        try {
                            o = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        ts.getStartEvent().setOffset(o);
                    });
                    ll.addView(timeEventRow);
                } else if (!ts.isOpenEnded() && hasStartEvent && hasEndEvent) {
                    Log.d(DEBUG_TAG, "e-e " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
                    final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.events_values, startEvent, ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, value -> ts.getStartEvent().setEvent(value));
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.events_values, endEvent, ts.getEndEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, endEvent, value -> ts.getEndEvent().setEvent(value));
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
                        showOffset.setOnMenuItemClickListener(item -> {
                            startOffsetContainer.setVisibility(View.VISIBLE);
                            endOffsetContainer.setVisibility(View.VISIBLE);
                            return true;
                        });
                    }

                    EditText startOffset = (EditText) startOffsetContainer.findViewById(R.id.start_offset);
                    startOffset.setText(Integer.toString(ts.getStartEvent().getOffset()));
                    setTextWatcher(startOffset, value -> {
                        int offset = 0;
                        try {
                            offset = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        ts.getStartEvent().setOffset(offset);
                    });
                    EditText endOffset = (EditText) endOffsetContainer.findViewById(R.id.end_offset);
                    endOffset.setText(Integer.toString(ts.getEndEvent().getOffset()));
                    setTextWatcher(endOffset, value -> {
                        int offset = 0;
                        try {
                            offset = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        ts.getEndEvent().setOffset(offset);
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
                    timeBar.setOnRangeBarChangeListener((rangeBar, leftPinIndex, rightPinIndex, leftPinValue, rightPinValue) -> {
                        ts.setStart((int) (rightPinIndex * rangeBar.getTickInterval() + rangeBar.getTickStart()));
                        updateString();
                    });
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    addTimePickerMenu(menu, ts, timeBar);
                    addTimeSpanMenus(timeBar, null, menu);
                    ll.addView(timeEventRow);
                } else if (ts.isOpenEnded() && hasStartEvent) {
                    Log.d(DEBUG_TAG, "e- " + ts.toString());
                    LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
                    final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
                    Util.setSpinnerInitialEntryValue(getResources(), R.array.events_values, startEvent, ts.getStartEvent().getEvent().toString());
                    setSpinnerListenerEntryValues(R.array.events_values, startEvent, value -> ts.getStartEvent().setEvent(value));
                    Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
                    endEvent.setVisibility(View.GONE);
                    View endOffsetContainer = timeEventRow.findViewById(R.id.end_offset_container);
                    endOffsetContainer.setVisibility(View.GONE);
                    menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
                    final View offsetContainer = timeEventRow.findViewById(R.id.start_offset_container);
                    int offsetValue = ts.getStartEvent().getOffset();
                    setOffsetVisibility(offsetValue, menu, offsetContainer);

                    EditText offset = (EditText) offsetContainer.findViewById(R.id.start_offset);
                    offset.setText(Integer.toString(offsetValue));
                    setTextWatcher(offset, value -> {
                        int o = 0;
                        try {
                            o = Integer.parseInt(value);
                        } catch (NumberFormatException nfex) {
                            // Empty
                        }
                        ts.getStartEvent().setOffset(o);
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
                setTextWatcher(intervalEdit, value -> {
                    int interval = 0;
                    try {
                        interval = Integer.parseInt(value);
                    } catch (NumberFormatException nfex) {
                        // Empty
                    }
                    ts.setInterval(interval);
                });
                ll.addView(intervalLayout);
                if (hasInterval) {
                    intervalLayout.setVisibility(View.VISIBLE);
                } else {
                    intervalLayout.setVisibility(View.GONE);
                    MenuItem showInterval = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.show_interval);
                    showInterval.setOnMenuItemClickListener(item -> {
                        intervalLayout.setVisibility(View.VISIBLE);
                        return true;
                    });
                }
            }
        }
    }

    SetTimeRangeListener realSetTimeRangeListener = null;

    @Override
    public void setTimeRange(int startHour, int startMinute, int endHour, int endMinute) {
        if (realSetTimeRangeListener != null) {
            realSetTimeRangeListener.setTimeRange(startHour, startMinute, endHour, endMinute);
        }
    }

    /**
     * Add a menu item that displays a number picker for times
     * 
     * @param menu Menu to and the item to
     * @param ts TimeSpan to display and modify
     * @param timeBar an optional RangeBar we are associated with
     */
    private void addTimePickerMenu(@NonNull Menu menu, @NonNull final TimeSpan ts, @Nullable final RangeBar timeBar) {
        final MenuItem timePickerMenu = menu.add(R.string.display_time_picker);
        final OnMenuItemClickListener listener = item -> {
            int start = ts.getStart();
            int end = ts.getEnd();
            int interval = timeBar != null ? (int) timeBar.getTickInterval() : 1;
            if (ts.getStartEvent() == null && ts.getEndEvent() == null && end >= 0) { // t-t, t-x
                realSetTimeRangeListener = (int startHour, int startMinute, int endHour, int endMinute) -> {
                        ts.setStart(startHour * 60 + startMinute);
                        ts.setEnd(endHour * 60 + endMinute);
                        updateString();
                        watcher.afterTextChanged(null);
                };
                TimeRangePicker.showDialog(OpeningHoursFragment.this, R.string.time, start / 60, start % 60, end / 60, end % 60, interval);
            } else if (ts.getStartEvent() == null && (ts.getEndEvent() != null || end < 0)) { // t, t-, t-e
                realSetTimeRangeListener = (int startHour, int startMinute, int endHour, int endMinute) -> {
                        ts.setStart(startHour * 60 + startMinute);
                        updateString();
                        watcher.afterTextChanged(null);
                };
                TimeRangePicker.showDialog(OpeningHoursFragment.this, R.string.time, start / 60, start % 60, interval);
            } else if (ts.getStartEvent() != null && ts.getEndEvent() == null || end >= 0) { // e-t
                realSetTimeRangeListener =(int startHour, int startMinute, int endHour, int endMinute) -> {
                        ts.setEnd(startHour * 60 + startMinute);
                        updateString();
                        watcher.afterTextChanged(null);
                };
                TimeRangePicker.showDialog(OpeningHoursFragment.this, R.string.time, start / 60, start % 60, interval);
            }
            return true;
        };
        timePickerMenu.setOnMenuItemClickListener(listener);
        if (timeBar != null) {
            // lazy hack
            timeBar.setOnClickListener(v -> listener.onMenuItemClick(null));
        }
    }

    /**
     * Change the tick interval on a RangeBar
     * 
     * @param timeBar the RangeBar to modify
     * @param interval the new interval
     */
    private void changeTicks(@NonNull final RangeBar timeBar, int interval) {
        double tickInterval = timeBar.getTickInterval();
        float startTick = timeBar.getTickStart();
        int start = (int) (timeBar.getLeftIndex() * tickInterval + startTick);
        start = Math.round(((float) start) / interval) * interval;
        int end = (int) (timeBar.getRightIndex() * tickInterval + startTick);
        end = Math.round(((float) end) / interval) * interval;
        timeBar.setTickInterval(interval);
        timeBar.setRangePinsByValue(start, end);
        timeBar.setVisibleTickInterval(60 / interval);
    }

    private void changeStart(@NonNull final RangeBar timeBar, float newTickStart) {
        double tickInterval = timeBar.getTickInterval();
        float startTick = timeBar.getTickStart();
        int start = (int) (timeBar.getLeftIndex() * tickInterval + startTick);
        int end = (int) (timeBar.getRightIndex() * tickInterval + startTick);
        timeBar.setTickStart(newTickStart);
        timeBar.setRangePinsByValue(start, end);
    }

    private void addTimeSpanMenus(@NonNull final RangeBar timeBar, @Nullable final RangeBar timeBar2, @NonNull Menu menu) {

        final MenuItem item15 = menu.add(R.string.ticks_15_minute);
        final MenuItem item5 = menu.add(R.string.ticks_5_minute);
        final MenuItem item1 = menu.add(R.string.ticks_1_minute);

        item15.setOnMenuItemClickListener(item -> {
            changeTicks(timeBar, 15);
            if (timeBar2 != null) {
                changeTicks(timeBar2, 15);
            }
            item15.setEnabled(false);
            item5.setEnabled(true);
            item1.setEnabled(true);
            return true;
        });

        item5.setOnMenuItemClickListener(item -> {
            changeTicks(timeBar, 5);
            if (timeBar2 != null) {
                changeTicks(timeBar2, 5);
            }
            item15.setEnabled(true);
            item5.setEnabled(false);
            item1.setEnabled(true);
            return true;
        });

        item1.setOnMenuItemClickListener(item -> {
            changeTicks(timeBar, 1);
            if (timeBar2 != null) {
                changeTicks(timeBar2, 1);
            }
            item15.setEnabled(true);
            item5.setEnabled(true);
            item1.setEnabled(false);
            return true;
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
            expand.setOnMenuItemClickListener(item -> {
                changeStart(timeBar, 0f);
                if (timeBar2 != null) {
                    changeStart(timeBar2, 0f);
                }
                expand.setEnabled(false);
                return true;
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

    private void setSpinnerListenerEntryValues(final int valuesId, @NonNull final Spinner spinner, @NonNull final SetValue listener) {
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
                // Empty
            }
        });
    }

    /**
     * Add the standard menu entries for a Rule
     * 
     * @param row the layout containing the Rule UI
     * @param listener the listener for the deleting the rule
     * @return the created Menu
     */
    private Menu addStandardMenuItems(@NonNull LinearLayout row, @Nullable final Delete listener) {
        ActionMenuView amv = (ActionMenuView) row.findViewById(R.id.menu);
        Menu menu = amv.getMenu();
        amv.setOnLongClickListener(v -> true);
        MenuItem mi = menu.add(Menu.NONE, Menu.NONE, Menu.CATEGORY_SECONDARY, R.string.Delete);
        mi.setOnMenuItemClickListener(item -> {
            if (listener != null) {
                listener.delete();
            }
            return true;
        });
        MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_NEVER);
        return menu;
    }

    /**
     * Runnable that updates the OH string
     */
    Runnable updateStringRunnable = () -> {
        if (rules != null) {
            int pos = text.getSelectionStart();
            int prevLen = text.length();
            text.removeTextChangedListener(watcher);
            String oh = ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules);
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
     * Set a check mark for the day specified
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
            if ((v instanceof CheckBox || v instanceof AppCompatCheckBox) && ((String) v.getTag()).equals(Integer.toString(nth))) {
                ((CheckBox) v).setChecked(true);
                return;
            }
        }
    }

    private void setWeekDayListeners(@NonNull final RelativeLayout container, @NonNull final List<WeekDayRange> days,
            @NonNull final List<WeekDayRange> inContainer, final boolean justOne, @NonNull final MenuItem nthMenuItem) {
        OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (justOne) { // Nth exists
                if (isChecked) {
                    WeekDayRange range = inContainer.get(0);
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
                List<WeekDayRange> temp = new ArrayList<>(days);
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
        };

        for (int i = 0; i < container.getChildCount(); i++) {
            final View v = container.getChildAt(i);
            if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener(listener);
            }
        }
    }

    private void setWeekDayListeners(@NonNull final RelativeLayout container, @NonNull final DateWithOffset dwo) {
        OnCheckedChangeListener listener = (buttonView, isChecked) -> {
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
        };

        for (int i = 0; i < container.getChildCount(); i++) {
            final View v = container.getChildAt(i);
            if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener(listener);
            }
        }
    }

    private void setNthListeners(@NonNull final RelativeLayout container, @NonNull final WeekDayRange days) {
        OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            List<Nth> nths = days.getNths();
            if (nths == null) {
                nths = new ArrayList<>();
                days.setNths(nths);
            } else {
                nths.clear();
            }
            Nth range = null;
            for (int i = 0; i < container.getChildCount(); i++) {
                final View c = container.getChildAt(i);
                if (c instanceof CheckBox || c instanceof AppCompatCheckBox) {
                    if (((CheckBox) c).isChecked()) {
                        int nth = Integer.parseInt((String) c.getTag());
                        if (range == null) {
                            range = new Nth();
                            range.setStartNth(nth);
                            nths.add(range);
                        } else { // don't mix pos and neg valus
                            if (Integer.signum(nth) != Integer.signum(range.getStartNth())) {
                                range = new Nth();
                                range.setStartNth(nth);
                                nths.add(range);
                            } else {
                                range.setEndNth(nth);
                            }
                        }
                    } else {
                        range = null;
                    }
                }
            }
            updateString();
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
        outState.putString(REGION_KEY, region);
        outState.putString(OBJECT_KEY, object);
        outState.putSerializable(VALUE_KEY, text.getText().toString());
        outState.putSerializable(ORIGINAL_VALUE_KEY, originalOpeningHoursValue);
        outState.putInt(STYLE_KEY, styleRes);
        outState.putSerializable(LOCALE_KEY, locale);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        if (loadedDefault) {
            Log.d(DEBUG_TAG, "Show toast");
            ch.poole.openinghoursfragment.Util.toastTop(getActivity(), getString(R.string.loaded_default));
        }
        if ((openingHoursValue == null || "".equals(openingHoursValue)) && showTemplates && !textMode) {
            showTemplates = false;
            TemplateMangementDialog.showDialog(this, false, key, region, object, text.getText().toString());
        }
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

    /**
     * Convert density independent pixels to screen pixels
     * 
     * @param dp the dip value
     * @return the actual pixels
     */
    private int dpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    /**
     * Enable / disable the save button depending on if the current value is the same as the original one
     * 
     * @param oh oh value to test against
     */
    private void enableSaveButton(@Nullable String oh) {
        if (saveButton != null) {
            saveButton.setEnabled(
                    originalOpeningHoursValue == null || (!originalOpeningHoursValue.equals(oh) && (oh == null || oh.length() <= OSM_MAX_TAG_LENGTH)));
        }
    }

    @Override
    public void updateText(String newText) {
        text.removeTextChangedListener(watcher);
        text.setText(newText);
        text.addTextChangedListener(watcher);
        watcher.afterTextChanged(null);
    }
}
