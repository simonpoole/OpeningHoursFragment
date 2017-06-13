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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Event;
import ch.poole.openinghoursparser.Holiday;
import ch.poole.openinghoursparser.Holiday.Type;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.DateRange;
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

	private static final String VALUE_KEY = "value";

	private static final String KEY_KEY = "key";
	
	private static final String STYLE_KEY = "style";

	private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();

	private LayoutInflater inflater = null;

	private TextWatcher watcher;
	
	private String key;
	
	private String openingHoursValue;
	
	private int styleRes = 0;
	
	private ArrayList<Rule> rules;
	
	private EditText text;
	
	private OnSaveListener saveListener = null;
	
	private TemplateListener templateListener = null;
	
	List<String> weekDays = WeekDay.nameValues();
	
	List<String> months = Month.nameValues();

	static PinTextFormatter timeFormater = new PinTextFormatter() {
		@Override
		public String getText(String value) {
			int minutes = Integer.valueOf(value);
			return String.format(Locale.US,"%02d", minutes / 60) + ":" + String.format(Locale.US,"%02d", minutes % 60);
		}
	};

	/**
	 */
	static public OpeningHoursFragment newInstance(String key,String value, int style) {
		OpeningHoursFragment f = new OpeningHoursFragment();

		Bundle args = new Bundle();
		args.putSerializable(KEY_KEY, key);
		args.putSerializable(VALUE_KEY, value);
		args.putInt(STYLE_KEY, style);

		f.setArguments(args);
		// f.setShowsDialog(true);

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
        try {
            templateListener = (TemplateListener) activity;
        } catch (ClassCastException e) {
            // optional
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(DEBUG_TAG, "onCreate");
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	  Dialog dialog = super.onCreateDialog(savedInstanceState);

	  // request a window without the title
	  dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	  return dialog;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			Log.d(DEBUG_TAG,"Restoring from saved state");
			key = savedInstanceState.getString(KEY_KEY);
			openingHoursValue = savedInstanceState.getString(VALUE_KEY);
			styleRes = savedInstanceState.getInt(STYLE_KEY);
		} else {
			key = getArguments().getString(KEY_KEY);
			openingHoursValue = getArguments().getString(VALUE_KEY);
			styleRes = getArguments().getInt(STYLE_KEY);
		}
		if (styleRes == 0) {
			styleRes = R.style.AlertDialog_AppCompat_Light; // fallback
		}
		
		Context context =  new ContextThemeWrapper(getActivity(), styleRes);
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		LinearLayout openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours, null);
		

		buildLayout(openingHoursLayout, openingHoursValue);

		// add callbacks for the buttons
		AppCompatButton cancel = (AppCompatButton) openingHoursLayout.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();				
			}});
		
		AppCompatButton save = (AppCompatButton) openingHoursLayout.findViewById(R.id.save);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveListener.save(key,text.getText().toString());	
				dismiss();
			}});
		
		return openingHoursLayout;
	}

	/**
	 * 
	 * @param openingHoursLayout
	 * @param openingHoursValue
	 */
	private void buildLayout(LinearLayout openingHoursLayout, String openingHoursValue) {
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
							OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(text.getText().toString().getBytes()));
							try {
								rules = parser.rules(false);
								buildForm(sv,rules);
								removeHighlight(text);
							} catch (ParseException pex) {
								Log.d(DEBUG_TAG, pex.getMessage());
								highlightParseError(text, pex);
							} catch (TokenMgrError err) {
								// we currently can't do anything reasonable here except ignore
								Log.e(DEBUG_TAG, err.getMessage());
							}
						}
					};
					text.removeCallbacks(rebuild);
					text.postDelayed(rebuild, 500);
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
				buildForm(sv,rules);
				removeHighlight(text);
			} catch (ParseException pex) {
				Log.d(DEBUG_TAG, pex.getMessage());
				highlightParseError(text, pex);
			} catch (TokenMgrError err) {
				// we currently can't do anything reasonable here except ignore 
				Log.e(DEBUG_TAG, err.getMessage());
			}
			
			FloatingActionButton add = (FloatingActionButton) openingHoursLayout.findViewById(R.id.add);
			add.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream("Mo 6:00-20:00".getBytes()));
					List<Rule> rules2 = null;
					try {
						rules2 = parser.rules(false);
					} catch (ParseException pex) {
						Log.e(DEBUG_TAG, pex.getMessage());
					} catch (TokenMgrError err) {
						Log.e(DEBUG_TAG, err.getMessage());
					}
					if (rules2 != null && !rules2.isEmpty()) {
						rules.add(rules2.get(0));
						updateString();
						watcher.afterTextChanged(null); // hack to force rebuild of form
					}
				}
			});
		}
	}
	
	/**
	 * Highlight the position of a parse error
	 * 
	 * @param text he EditTExt the string is displayed in
	 * @param pex the ParseException to use
	 */
	private void highlightParseError(EditText text, ParseException pex) {
		int c = pex.currentToken.next.beginColumn-1; // starts at 1
		int pos = text.getSelectionStart();
		Spannable spannable = new SpannableString(text.getText());
		spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c,Math.min(c+1,spannable.length())), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		text.removeTextChangedListener(watcher); // avoid infinite loop
		text.setText(spannable, TextView.BufferType.SPANNABLE);
		text.setSelection(Math.min(pos,spannable.length()));
		text.addTextChangedListener(watcher);
	}
	
	/**
	 * Remove all parse error highlighting
	 * 
	 * @param text the EditTExt the string is displayed in
	 */
	private void removeHighlight(EditText text) {
		int pos = text.getSelectionStart();
		int prevLen = text.length();
		text.removeTextChangedListener(watcher); // avoid infinite loop
		String t = Util.rulesToOpeningHoursString(rules);
		text.setText(t);
		// text.setText(text.getText().toString());
		text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos,text.length()));
		text.addTextChangedListener(watcher);
	}

	private synchronized void buildForm(ScrollView sv, List<Rule> rules) {
		sv.removeAllViews();
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setPadding(0, 0, 0, dpToPixels(64));
		ll.setOrientation(LinearLayout.VERTICAL);
		// LinearLayout.LayoutParams layoutParams = new
		// LinearLayout.LayoutParams(
		// LinearLayout.LayoutParams.MATCH_PARENT,
		// LinearLayout.LayoutParams.WRAP_CONTENT);
		// layoutParams.setMargins(20, 20, 20, 20);
		// ll.setLayoutParams(layoutParams);
		sv.addView(ll);
		//

		if (false) { // group mode
			List<ArrayList<Rule>> mergeableRules = Util.getMergeableRules(rules);
			for (ArrayList<Rule> ruleList : mergeableRules) {	
				addRules(true, ruleList, ll);
			}
		} else {
			addRules(false, rules, ll);
		}
	}

	private void addRules(boolean groupMode, final List<Rule> rules, LinearLayout ll) {
		// some standard static elements
		TextView dash = new TextView(getActivity());
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.0f);
		dash.setLayoutParams(params);
		dash.setText("-");
		dash.setGravity(Gravity.CENTER_VERTICAL);
		TextView comma = new TextView(getActivity());
		comma.setLayoutParams(params);
		comma.setText(",");
		comma.setGravity(Gravity.CENTER_VERTICAL);
		
		boolean first = true;
		int headerCount = 1;
		for (final Rule r:rules) {
			if (first) { // everything except days and times should be
						 // the same and only needs to be displayed
						 // once in groupMode, in normal mode this is 
				         // always true
				final LinearLayout groupHeader = (LinearLayout) inflater.inflate(R.layout.rule_header, null);
				TextView header = (TextView) groupHeader.findViewById(R.id.header);
				header.setText(getActivity().getString(groupMode ? R.string.group_header : R.string.rule_header, headerCount));
				RadioButton normal = (RadioButton)groupHeader.findViewById(R.id.normal_rule);
				if (!r.isAdditive() && !r.isFallBack()) {
					normal.setChecked(true);
				}
				normal.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						RadioButton rb = (RadioButton)v;
						if (rb.isChecked()) {
							r.setFallBack(false);
							r.setAdditive(false);
							updateString();
							watcher.afterTextChanged(null);
						}
					}
				});
				RadioButton additive = (RadioButton)groupHeader.findViewById(R.id.additive_rule);
				if (r.isAdditive()) {
					additive.setChecked(true);
				}
				additive.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						RadioButton rb = (RadioButton)v;
						if (rb.isChecked()) {
							r.setFallBack(false);
							r.setAdditive(true);
							updateString();
							watcher.afterTextChanged(null);
						}
					}
				});
				RadioButton fallback = (RadioButton)groupHeader.findViewById(R.id.fallback_rule);
				if (r.isFallBack()) {
					fallback.setChecked(true);
				}
				fallback.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						RadioButton rb = (RadioButton)v;
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
				
				if (r.getModifier()==null) {
					MenuItem addModifierAndComment =menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_modifier_comment);
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
								
				MenuItem addHoliday=menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_holiday);
				addHoliday.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<Holiday>holidays = r.getHolidays();
						if (holidays==null) {
							r.setHolidays(new ArrayList<Holiday>());
							holidays = r.getHolidays();
						}
						Holiday holiday = new Holiday();
						holiday.setType(Type.PH);
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
						List<TimeSpan>ts = r.getTimes();
						if (ts==null) {
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
				
				MenuItem addExtendedTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_extended_time);
				addExtendedTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<TimeSpan>ts = r.getTimes();
						if (ts==null) {
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
				
				MenuItem addVarTimeTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_time);
				addVarTimeTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<TimeSpan>ts = r.getTimes();
						if (ts==null) {
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
				
				MenuItem addTimeVarTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.time_variable_time);
				addTimeVarTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<TimeSpan>ts = r.getTimes();
						if (ts==null) {
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
				
				MenuItem addVarTimeVarTimespan = timespanMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_time_variable_time);
				addVarTimeVarTimespan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<TimeSpan>ts = r.getTimes();
						if (ts==null) {
							r.setTimes(new ArrayList<TimeSpan>());
							ts = r.getTimes();
						}
						TimeSpan t = new TimeSpan();
						VariableTime startVt = new VariableTime();
						startVt.setEvent(Event.DAWN);
						t.setStartEvent(startVt);;
						VariableTime endVt = new VariableTime();
						endVt.setEvent(Event.DUSK);
						t.setEndEvent(endVt);
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
						List<WeekDayRange>wd = r.getDays();
						if (wd==null) {
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
							List<Nth>nths = new ArrayList<Nth>();
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
				addDateDateRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<DateRange>mdr = r.getDates();
						if (mdr==null) {
							r.setMonthdays(new ArrayList<DateRange>());
							mdr = r.getDates();
						} 
						DateRange dateRange = new DateRange();
						DateWithOffset dwo = new DateWithOffset();
						dwo.setMonth(Month.JAN);
						dateRange.setStartDate(dwo);
						mdr.add(dateRange);
						updateString();
						watcher.afterTextChanged(null);
						return true;
					}	
				});
				MenuItem addVarDateDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_date);
				addVarDateDateRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<DateRange>mdr = r.getDates();
						if (mdr==null) {
							r.setMonthdays(new ArrayList<DateRange>());
							mdr = r.getDates();
						} 
						DateRange dateRange = new DateRange();
						DateWithOffset dwo = new DateWithOffset();
						dwo.setVarDate(VarDate.EASTER);
						dateRange.setStartDate(dwo);
						mdr.add(dateRange);
						updateString();
						watcher.afterTextChanged(null);
						return true;
					}	
				});
				MenuItem addDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.date_variable_date);
				addDateVarDateRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<DateRange>mdr = r.getDates();
						if (mdr==null) {
							r.setMonthdays(new ArrayList<DateRange>());
							mdr = r.getDates();
						} 
						DateRange dateRange = new DateRange();
						DateWithOffset startDwo = new DateWithOffset();
						startDwo.setMonth(Month.JAN);
						dateRange.setStartDate(startDwo);
						DateWithOffset endDwo = new DateWithOffset();
						endDwo.setVarDate(VarDate.EASTER);
						dateRange.setEndDate(endDwo);
						mdr.add(dateRange);
						updateString();
						watcher.afterTextChanged(null);
						return true;
					}	
				});
				MenuItem addVarDateVarDateRange = dateRangeMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.variable_date_variable_date);
				addVarDateVarDateRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<DateRange>mdr = r.getDates();
						if (mdr==null) {
							r.setMonthdays(new ArrayList<DateRange>());
							mdr = r.getDates();
						} 
						DateRange dateRange = new DateRange();
						DateWithOffset startDwo = new DateWithOffset();
						startDwo.setVarDate(VarDate.EASTER);
						dateRange.setStartDate(startDwo);
						DateWithOffset endDwo = new DateWithOffset();
						endDwo.setVarDate(VarDate.EASTER);
						dateRange.setEndDate(endDwo);
						mdr.add(dateRange);
						updateString();
						watcher.afterTextChanged(null);
						return true;
					}	
				});
				
				MenuItem addYearRange=menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_year_range);
				addYearRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<YearRange>years = r.getYears();
						if (years==null) {
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
				
				MenuItem addWeekRange=menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.add_week_range);
				addYearRange.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						List<WeekRange>weeks = r.getWeeks();
						if (weeks==null) {
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
							typeView.setVisibility(View.VISIBLE);;
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
								Log.e(DEBUG_TAG,"Rule missing from list!");
								return true;
							}
							rules.remove(current);
							rules.add(Math.max(0, current-1), r);
							updateString();
							watcher.afterTextChanged(null);
							return true;
						}
					});
				}
				if (!r.equals(rules.get(rules.size()-1))) {
				MenuItem moveDown = menu.add(R.string.move_down);
				moveDown.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						int current = rules.indexOf(r);
						if (current < 0) { // not found shouldn't happen
							Log.e(DEBUG_TAG,"Rule missing from list!");
							return true;
						}
						int size = rules.size();
						rules.remove(current);
						rules.add(Math.min(size-1, current+1), r);
						updateString();
						watcher.afterTextChanged(null);
						return true;
					}
				});
				}
				ll.addView(groupHeader);
				//comments
				String comment = r.getComment();
				if (comment != null && !"".equals(comment)) {
					LinearLayout intervalLayout = (LinearLayout) inflater.inflate(R.layout.comment, null);
					EditText commentComment = (EditText)intervalLayout.findViewById(R.id.comment);
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
					menu = addStandardMenuItems(twentyFourSeven, new Delete() {
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
						LinearLayout yearLayout = (LinearLayout) inflater.inflate(R.layout.year_range, null);
						final int startYear = yr.getStartYear();
						final TextView startYearView = (TextView)yearLayout.findViewById(R.id.start_year);
						startYearView.setText(Integer.toString(startYear));
						
						final TextView endYearView = (TextView)yearLayout.findViewById(R.id.end_year);
						final int endYear = yr.getEndYear();
						if (endYear > 0) {
							endYearView.setText(Integer.toString(endYear));
						}
					
						EditText yearIntervalEdit = (EditText)yearLayout.findViewById(R.id.interval);
						if (yr.getInterval() > 0) {
							yearIntervalEdit.setText(Integer.toString(yr.getInterval()));
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
						addStandardMenuItems(yearLayout, new Delete() {
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
						
						LinearLayout range = (LinearLayout)yearLayout.findViewById(R.id.range);
						range.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								int currentYear = Calendar.getInstance().get(Calendar.YEAR);
								int rangeEnd = Math.max(currentYear, startYear)+50;
								int tempEndYear = yr.getEndYear();
								if (tempEndYear==YearRange.UNDEFINED_YEAR) {
									tempEndYear = RangePicker.NOTHING_SELECTED;
								} else {
									rangeEnd = Math.max(rangeEnd, tempEndYear+50);
								}
								RangePicker.showDialog(getActivity(), R.string.year_range, YearRange.FIRST_VALID_YEAR, rangeEnd, yr.getStartYear(), tempEndYear,
									new SetRangeListener() {
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
									}
								);
							}
						});
						ll.addView(yearLayout);
					}
				}
				// week list
				final List<WeekRange> weeks = r.getWeeks();
				if (weeks != null && weeks.size() > 0) {
					for (final WeekRange w : weeks) {
						LinearLayout weekLayout = (LinearLayout) inflater.inflate(R.layout.week_range, null);
						final TextView startWeekView = (TextView)weekLayout.findViewById(R.id.start_week);
						final int startWeek = w.getStartWeek();
						startWeekView.setText(Integer.toString(startWeek));

						final TextView endWeekView = (TextView)weekLayout.findViewById(R.id.end_week);
						final int endWeek = w.getEndWeek();
						if (endWeek > 0) {
							endWeekView.setText(Integer.toString(endWeek));
						}
						
						EditText weekIntervalEdit = (EditText)weekLayout.findViewById(R.id.interval);
						if (w.getInterval() > 0) {
							weekIntervalEdit.setText(Integer.toString(w.getInterval()));
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
						addStandardMenuItems(weekLayout, new Delete() {
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
						
						LinearLayout range = (LinearLayout)weekLayout.findViewById(R.id.range);
						range.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								// need to reget values from w
								int tempEndWeek = w.getEndWeek();
								if (tempEndWeek == WeekRange.UNDEFINED_WEEK) {
									tempEndWeek = RangePicker.NOTHING_SELECTED;
								}
								RangePicker.showDialog(getActivity(), R.string.week_range, WeekRange.MIN_WEEK, WeekRange.MAX_WEEK, w.getStartWeek(), tempEndWeek,
									new SetRangeListener() {
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
									}
								);
							}
						});
						ll.addView(weekLayout);
					}
				}
				// date range list
				final List<DateRange> dateRanges = r.getDates();
				if (dateRanges != null && dateRanges.size() > 0) {
					for (final DateRange dateRange : dateRanges) {
						
						// cases to consider
						// date - date
						// vardate - date
						// date - vardate
						// vardate - vardate
						//
						final LinearLayout dateRangeRow = (LinearLayout) inflater.inflate(R.layout.daterange, null);
						setDateRangeValues(dateRange.getStartDate(), dateRange.getEndDate(), dateRangeRow);
						RelativeLayout dateRangeLayout = (RelativeLayout)dateRangeRow.findViewById(R.id.daterange_container);
						dateRangeLayout.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {		
								int tempEndYear = YearRange.UNDEFINED_YEAR;
								Month tempEndMonth = null;
								int tempEndDay = DateWithOffset.UNDEFINED_MONTH_DAY;
								
								final DateWithOffset start = dateRange.getStartDate();
								DateWithOffset end = dateRange.getEndDate();
								
								if (end != null) {
									tempEndYear = end.getYear();
									tempEndMonth = end.getMonth();
									tempEndDay = end.getDay();
								}
								if (start.getVarDate()==null && (end==null || end.getVarDate()==null)) {
									DateRangePicker.showDialog(getActivity(), R.string.date_range, 
											start.getYear(), start.getMonth(), start.getDay(),
											tempEndYear, tempEndMonth, tempEndDay,
											new SetDateRangeListener() {
										private static final long serialVersionUID = 1L;

										@Override
										public void setDateRange(int startYear, Month startMonth, int startDay, VarDate startVarDate, 
												int endYear, Month endMonth, int endDay, VarDate endVarDate) {
											start.setYear(startYear);
											start.setMonth(startMonth);
											start.setDay(startDay);
											DateWithOffset tempDwo = dateRange.getEndDate();
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
											setDateRangeValues(start, dateRange.getEndDate(), finalRow);
											updateString();
										}
									});
								} else if (start.getVarDate()!=null && (end==null || end.getVarDate()==null)) {
									DateRangePicker.showDialog(getActivity(), R.string.date_range, 
											start.getYear(), start.getVarDate(),
											tempEndYear, tempEndMonth, tempEndDay,
											new SetDateRangeListener() {
										private static final long serialVersionUID = 1L;

										@Override
										public void setDateRange(int startYear, Month startMonth, int startDay, VarDate startVarDate, 
												int endYear, Month endMonth, int endDay, VarDate endVarDate) {
											start.setYear(startYear);
											start.setVarDate(startVarDate);
											DateWithOffset tempDwo = dateRange.getEndDate();
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
											setDateRangeValues(start, dateRange.getEndDate(), finalRow);
											updateString();
										}
									});
								} else if (start.getVarDate()==null && (end!=null && end.getVarDate()!=null)) {
									DateRangePicker.showDialog(getActivity(), R.string.date_range, 
											start.getYear(), start.getMonth(), start.getDay(),
											tempEndYear, end.getVarDate(),
											new SetDateRangeListener() {
										private static final long serialVersionUID = 1L;

										@Override
										public void setDateRange(int startYear, Month startMonth, int startDay, VarDate startVarDate, 
												int endYear, Month endMonth, int endDay, VarDate endVarDate) {
											start.setYear(startYear);
											start.setMonth(startMonth);
											start.setDay(startDay);

											dateRange.getEndDate().setYear(endYear);
											dateRange.getEndDate().setVarDate(endVarDate);

											final LinearLayout finalRow = dateRangeRow;  
											setDateRangeValues(start, dateRange.getEndDate(), finalRow);
											updateString();
										}
									});
								} else if (start.getVarDate()!=null && (end!=null && end.getVarDate()!=null)) {
									DateRangePicker.showDialog(getActivity(), R.string.date_range, 
											start.getYear(), start.getVarDate(),
											tempEndYear, end.getVarDate(),
											new SetDateRangeListener() {
										private static final long serialVersionUID = 1L;

										@Override
										public void setDateRange(int startYear, Month startMonth, int startDay, VarDate startVarDate, 
												int endYear, Month endMonth, int endDay, VarDate endVarDate) {
											start.setYear(startYear);
											start.setVarDate(startVarDate);

											dateRange.getEndDate().setYear(endYear);
											dateRange.getEndDate().setVarDate(endVarDate);

											final LinearLayout finalRow = dateRangeRow;  
											setDateRangeValues(start, dateRange.getEndDate(), finalRow);
											updateString();
										}
									});
								}
							}
						});

						// add menus
						if (dateRangeRow != null) {
							addStandardMenuItems(dateRangeRow, new Delete() {
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
							ll.addView(dateRangeRow);
						}
					}
				}
				// Modifier
				final RuleModifier rm = r.getModifier();
				if (rm != null) {
					LinearLayout modifierLayout = (LinearLayout) inflater.inflate(R.layout.modifier, null);
					final Spinner modifier = (Spinner)modifierLayout.findViewById(R.id.modifier);
					setSpinnerInitialValue(modifier, rm.getModifier().toString());
					setSpinnerListener(modifier, new SetValue() {
						@Override
						public void set(String value) {
							rm.setModifier(value);
						}
					});
					EditText modifierComment = (EditText)modifierLayout.findViewById(R.id.comment);
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
					LinearLayout holidayRow = (LinearLayout) inflater.inflate(R.layout.holiday_row, null);
					Spinner holidaysSpinner = (Spinner) holidayRow.findViewById(R.id.holidays);
					setSpinnerInitialEntryValue(holidaysSpinner, hd.getType().name());
					setSpinnerListenerEntryValues(holidaysSpinner, new SetValue() {
						@Override
						public void set(String value) {
							hd.setType(Holiday.Type.valueOf(value));
						}
					});
					addStandardMenuItems(holidayRow,  new Delete() {
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
					if (hd.getOffset() != 0) {
						EditText offset = (EditText) holidayRow.findViewById(R.id.offset);
						offset.setText(Integer.toString(hd.getOffset()));
						setTextWatcher(offset, new SetValue() {
							@Override
							public void set(String value) {
								int offset = 0;
								try {
									offset = Integer.parseInt(value);
								} catch (NumberFormatException nfex){
								}
								hd.setOffset(offset);
							}
						});
					}
					ll.addView(holidayRow);
				}
			}
			// day list
			final List<WeekDayRange> days = r.getDays();
			if (days != null && days.size() > 0) {
				final LinearLayout weekDayRow = (LinearLayout) inflater.inflate(R.layout.weekday_range_row, null);
				final RelativeLayout weekDayContainer = (RelativeLayout) weekDayRow.findViewById(R.id.weekDayContainer);
				
				Menu menu = addStandardMenuItems(weekDayRow, new Delete() {
					@Override
					public void delete() {
						List<WeekDayRange> temp = new ArrayList<WeekDayRange>(days);
						for (WeekDayRange d:temp) {
							if (d.getNths()==null) {
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

				List<WeekDayRange>normal = new ArrayList<WeekDayRange>();
				List<WeekDayRange>withNth = new ArrayList<WeekDayRange>();
				for (WeekDayRange d : days) {
					if (d.getNths()==null || d.getNths().isEmpty()) {
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
						final RelativeLayout weekDayContainerNth = (RelativeLayout) weekDayRowNth.findViewById(R.id.weekDayContainer);
						WeekDay startDay = d.getStartDay();
						List<Nth>nths = d.getNths();
						checkWeekDay(weekDayContainerNth, startDay.toString());
						nthMenuItem.setEnabled(false);
						LinearLayout nthLayout = (LinearLayout) inflater.inflate(R.layout.nth, null);
						RelativeLayout nthContainer = (RelativeLayout) nthLayout.findViewById(R.id.nthContainer);
						for (Nth nth:nths) {
							Log.d(DEBUG_TAG,"adding nth " + nth.toString());
							int startNth = nth.getStartNth();
							int endNth = nth.getEndNth();
							if (endNth < 1) {
								checkNth(nthContainer, startNth);
							} else {
								for (int i=startNth;i<= endNth; i++) {
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
						// FIXME hide when not in use
						EditText offset = (EditText) nthLayout.findViewById(R.id.offset);
						offset.setText(Integer.toString(d.getOffset()));
						setTextWatcher(offset, new SetValue() {
							@Override
							public void set(String value) {
								int offset = 0;
								try {
									offset = Integer.parseInt(value);
								} catch (NumberFormatException nfex){
								}
								d.setOffset(offset);
							}
						});
							
						ll.addView(weekDayRowNth);
					}		
				}
			}

			// times
			List<TimeSpan> times = r.getTimes();
			addTimeSpanUIs(ll, times);
		}
	}

	private void setDateRangeValues(final DateWithOffset start, final DateWithOffset end, LinearLayout dateRangeRow) {
		TextView startYearView = (TextView) dateRangeRow.findViewById(R.id.startYear);
		if (start.getYear() != YearRange.UNDEFINED_YEAR) {
			startYearView.setText(Integer.toString(start.getYear()));
		} else {
			startYearView.setText("");
		}
		TextView startDelimiter = (TextView) dateRangeRow.findViewById(R.id.startMonthDayDelimiter);
		TextView startMonthView = (TextView) dateRangeRow.findViewById(R.id.startMonth);
		TextView startDayView = (TextView) dateRangeRow.findViewById(R.id.startMonthDay);
		if (start.getVarDate()==null) {
			startDelimiter.setVisibility(View.VISIBLE);
			startDayView.setVisibility(View.VISIBLE);
			
			if (start.getMonth() != null) {
				startMonthView.setText(getResources().getStringArray(R.array.months)[start.getMonth().ordinal()]);
			}

			if (start.getDay() != DateWithOffset.UNDEFINED_MONTH_DAY) {
				startDayView.setText(Integer.toString(start.getDay()));
			} else {
				startDayView.setText("");
			}
		} else {
			startDelimiter.setVisibility(View.GONE);
			startDayView.setVisibility(View.GONE);
			startMonthView.setText(getResources().getStringArray(R.array.vardate_values)[start.getVarDate().ordinal()]);
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
			if (end.getVarDate()==null) {
				endDelimiter.setVisibility(View.VISIBLE);
				endDayView.setVisibility(View.VISIBLE);
				
				if (end.getMonth() != null) {
					endMonthView.setText(getResources().getStringArray(R.array.months)[end.getMonth().ordinal()]);
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
				endMonthView.setText(getResources().getStringArray(R.array.vardate_values)[end.getVarDate().ordinal()]);
			}
		}
	}
	
	/**
	 * Check if just one day is in the ranges
	 * 
	 * @param ranges
	 * @return
	 */
	private boolean justOneDay(List<WeekDayRange> ranges) {
		return ranges.size()==1 && ranges.get(0).getEndDay()==null;
	}
	
	private void setTextWatcher(final EditText edit, final SetValue listener) {
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
	
	private void addTimeSpanUIs(LinearLayout ll, final List<TimeSpan> times) {
		if (times != null && times.size() > 0) {
			Log.d(DEBUG_TAG, "#time spans " + times.size());
			for (final TimeSpan ts : times) {
				boolean hasStartEvent = ts.getStartEvent() != null;
				boolean hasEndEvent = ts.getEndEvent() != null;
				boolean extendedTime = ts.getEnd() > 1440;
				boolean hasInterval = ts.getInterval() > 0;
				if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() > 0 && !extendedTime) {
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
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(leftPinValue));
							ts.setEnd(toMins(rightPinValue));
							updateString();
						}});
					Menu menu = addStandardMenuItems(timeRangeRow, new DeleteTimeSpan(times, ts));
					addTickMenus(timeBar, null,  menu);
					ll.addView(timeRangeRow);
				} else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() > 0 && extendedTime) {
					Log.d(DEBUG_TAG, "t-x " + ts.toString());
					LinearLayout timeExtendedRangeRow = (LinearLayout) inflater
							.inflate(R.layout.time_extended_range_row, null);
					RangeBar timeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.timebar);
					RangeBar extendedTimeBar = (RangeBar) timeExtendedRangeRow
							.findViewById(R.id.extendedTimebar);
					int start = ts.getStart();
					int end = ts.getEnd();
					setGranuarlty(timeBar, start, end);
					setGranuarlty(extendedTimeBar, start, end);
					timeBar.setRangePinsByValue(0, start);
					timeBar.setPinTextFormatter(timeFormater);
					timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
						@Override
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(rightPinValue));
							updateString();
						}});
					extendedTimeBar.setRangePinsByValue(1440, end);
					extendedTimeBar.setPinTextFormatter(timeFormater);
					extendedTimeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
						@Override
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setEnd(toMins(rightPinValue));
							updateString();
						}});
					Menu menu = addStandardMenuItems(timeExtendedRangeRow, new DeleteTimeSpan(times, ts));
					addTickMenus(timeBar, extendedTimeBar, menu);
					ll.addView(timeExtendedRangeRow);
				} else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() <= 0) {
					Log.d(DEBUG_TAG, "pot " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					timeBar.setPinTextFormatter(timeFormater);
					int start = ts.getStart();
					timeBar.setConnectingLineEnabled(false);
					setGranuarlty(timeBar, start, 0);
					timeBar.setRangePinsByValue(0, start);
					
					timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
						@Override
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(rightPinValue));
							updateString();
						}});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					endEvent.setVisibility(View.GONE);
					Menu menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					addTickMenus(timeBar, null, menu);
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
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(rightPinValue));
							updateString();
						}});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					setSpinnerInitialValue(endEvent, ts.getEndEvent().getEvent().toString());
					setSpinnerListener(endEvent, new SetValue() {
						@Override
						public void set(String value) {
							ts.getEndEvent().setEvent(value);
						}
					});
					Menu menu =addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					addTickMenus(timeBar, null, menu);
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
						bar.setRangePinsByValue(start,end);
						bar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
							@Override
							public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
									int rightPinIndex, String leftPinValue, String rightPinValue) {
								ts.setEnd(toMins(rightPinValue));
								updateString();
							}});
					} else {
						timeBar.setVisibility(View.GONE);
						extendedTimeBar.setVisibility(View.GONE);
					}
					Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
					setSpinnerInitialValue(startEvent, ts.getStartEvent().getEvent().toString());
					setSpinnerListener(startEvent, new SetValue() {
						@Override
						public void set(String value) {
							ts.getStartEvent().setEvent(value);
						}
					});
					Menu menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					if (timeBar.getVisibility()==View.VISIBLE) {
						addTickMenus(timeBar, null, menu);
					}
					ll.addView(timeEventRow);
				} else if (!ts.isOpenEnded() && hasStartEvent && hasEndEvent) {
					Log.d(DEBUG_TAG, "e-e " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
					final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
					setSpinnerInitialValue(startEvent, ts.getStartEvent().getEvent().toString());
					setSpinnerListener(startEvent, new SetValue() {
						@Override
						public void set(String value) {
							ts.getStartEvent().setEvent(value);
						}
					});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					setSpinnerInitialValue(endEvent, ts.getEndEvent().getEvent().toString());
					setSpinnerListener(endEvent, new SetValue() {
						@Override
						public void set(String value) {
							ts.getEndEvent().setEvent(value);
						}
					});
					addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					ll.addView(timeEventRow);
				} else if (ts.isOpenEnded() && !hasStartEvent) {
					Log.d(DEBUG_TAG, "t- " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					timeBar.setPinTextFormatter(timeFormater);
					int start = ts.getStart();
					setGranuarlty(timeBar, start, 0);
					timeBar.setRangePinsByValue(0, start);
					timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
						@Override
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(rightPinValue));
							updateString();
						}});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					endEvent.setVisibility(View.GONE);
					Menu menu = addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					addTickMenus(timeBar, null, menu);
					ll.addView(timeEventRow);
				} else if (ts.isOpenEnded() && hasStartEvent) {
					Log.d(DEBUG_TAG, "e- " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
					final Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
					setSpinnerInitialValue(startEvent, ts.getStartEvent().getEvent().toString());
					setSpinnerListener(startEvent, new SetValue() {
						@Override
						public void set(String value) {
							ts.getStartEvent().setEvent(value);
						}
					});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					endEvent.setVisibility(View.GONE);
					addStandardMenuItems(timeEventRow, new DeleteTimeSpan(times, ts));
					ll.addView(timeEventRow);
				} else {
					Log.d(DEBUG_TAG, "? " + ts.toString());
					TextView tv = new TextView(getActivity());
					tv.setText(ts.toString());
					ll.addView(tv);
					return;
				}
				if (hasInterval) {
					LinearLayout intervalLayout = (LinearLayout) inflater.inflate(R.layout.interval, null);
					EditText interval = (EditText)intervalLayout.findViewById(R.id.interval);
					interval.setText(Integer.toString(ts.getInterval()));
					setTextWatcher(interval, new SetValue() {
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
					// addStandardMenuItems(intervalLayout, null);
					ll.addView(intervalLayout);
				}
			}
		}
	}
	
	private void changeTicks(final RangeBar timeBar, int interval) {
		int start = toMins(timeBar.getLeftPinValue());
		start = Math.round(((float)start)/interval)*interval;
		int end = toMins(timeBar.getRightPinValue());
		end = Math.round(((float)end)/interval)*interval;
		timeBar.setTickInterval(interval);
		timeBar.setRangePinsByValue(start, end);
		timeBar.setVisibleTickInterval(60/interval);
	}

	private void addTickMenus(final RangeBar timeBar, final RangeBar timeBar2, Menu menu) {

		final MenuItem item15 = menu.add(R.string.ticks_15_minute);
		final MenuItem item5 = menu.add(R.string.ticks_5_minute);
		final MenuItem item1 = menu.add(R.string.ticks_1_minute);

		item15.setOnMenuItemClickListener(new OnMenuItemClickListener(){
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

		item5.setOnMenuItemClickListener(new OnMenuItemClickListener(){
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

		item1.setOnMenuItemClickListener(new OnMenuItemClickListener(){
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
		if (((int)timeBar.getTickInterval()) != 1) {
			item1.setEnabled(true);
		}
		if (((int)timeBar.getTickInterval()) != 5) {
			item5.setEnabled(true);
		}
		if (((int)timeBar.getTickInterval()) != 15) {
			item15.setEnabled(true);
		}
	}

	private void setGranuarlty(RangeBar bar, int start, int end) {
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

	private void setSpinnerInitialValue(Spinner spinner, String value) {
		spinner.setSelection(((ArrayAdapter<String>) spinner.getAdapter())
				.getPosition(value));
	}

	interface SetValue {
		void set(String value);
	}
	
	private void setSpinnerListener(final Spinner spinner, final SetValue listener) {
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				listener.set((String) spinner.getItemAtPosition(position));
				updateString();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	private void setSpinnerInitialEntryValue(Spinner spinner, String value) {
		Resources res = getResources();
		final TypedArray values = res.obtainTypedArray(R.array.holidays_values);
		for (int i=0;i<values.length();i++) {
			if (value.equals(values.getString(i))) {
				spinner.setSelection(i);
				break;
			}
		}
		values.recycle();
	}
	
	private void setSpinnerListenerEntryValues(final Spinner spinner, final SetValue listener) {
		final Resources res = getResources();
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				final TypedArray selectedValues = res.obtainTypedArray(R.array.holidays_values);
				listener.set(selectedValues.getString(position));
				updateString();
				selectedValues.recycle();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	private Menu addStandardMenuItems(LinearLayout row, final Delete listener) {
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
		MenuItemCompat.setShowAsAction(mi,MenuItemCompat.SHOW_AS_ACTION_NEVER);
		return menu;
	}
	
	Runnable updateStringRunnable = new Runnable() {
		@Override
		public void run() {
			int pos = text.getSelectionStart();
			int prevLen = text.length();
			text.removeTextChangedListener(watcher);
			String oh = Util.rulesToOpeningHoursString(rules);
			text.setText(oh);
			text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos,text.length()));
			text.addTextChangedListener(watcher);
		}
	};
	
	/**
	 * Update the actual OH string
	 */
	private void updateString() {
		text.removeCallbacks(updateStringRunnable);
		text.postDelayed(updateStringRunnable,100);
	}
	
	/**
	 * Parse hh:ss and return minutes since midnight
	 * Totally trivial implementation c&p from http://stackoverflow.com/questions/8909075/convert-time-field-hm-into-integer-field-minutes-in-java
	 * @param t
	 * @return
	 */
	private static int toMins(String t) {
		String[] hourMin = t.split(":");
	    int hour = Integer.parseInt(hourMin[0]);
	    int mins = Integer.parseInt(hourMin[1]);
	    int hoursInMins = hour * 60;
	    return hoursInMins + mins;
	}

	private void checkWeekDay(RelativeLayout container, String day) {
		Log.d(DEBUG_TAG, "checking " + day);
		for (int i = 0; i < container.getChildCount(); i++) {
			View v = container.getChildAt(i);
			if ((v instanceof CheckBox || v instanceof AppCompatCheckBox) && ((String) v.getTag()).equals(day)) {
				((CheckBox) v).setChecked(true);
				return;
			}
		}
	}
	
	private void checkMonth(RelativeLayout container, String month) {
		Log.d(DEBUG_TAG, "checking " + month);
		for (int i = 0; i < container.getChildCount(); i++) {
			View v = container.getChildAt(i);
			if ((v instanceof CheckBox || v instanceof AppCompatCheckBox) && ((String) v.getTag()).equals(month)) {
				((CheckBox) v).setChecked(true);
				return;
			}
		}
	}
	
	private void checkNth(RelativeLayout container, int nth) {
		Log.d(DEBUG_TAG, "checking " + nth);
		for (int i = 0; i < container.getChildCount(); i++) {
			View v = container.getChildAt(i);
			if ((v instanceof CheckBox || v instanceof AppCompatCheckBox) && ((String) v.getTag()).equals(Integer.toString(nth))) {
				((CheckBox) v).setChecked(true);
				return;
			}
		}
	}

	private void setWeekDayListeners(final RelativeLayout container, final List<WeekDayRange> days, final List<WeekDayRange> inContainer, final boolean justOne, final MenuItem nthMenuItem) {
		OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (justOne) { // Nth exists	
					if  (isChecked) { 
						WeekDayRange range = days.get(0);
						for (int i = 0; i < container.getChildCount(); i++) {
							final View c = container.getChildAt(i);
							if ((c instanceof CheckBox || c instanceof AppCompatCheckBox) && !c.equals(buttonView)) {
								((CheckBox)c).setChecked(false);
							}
						}
						range.setStartDay((String) buttonView.getTag());
					} else { // hack alert
						for (int i = 0; i < container.getChildCount(); i++) {
							final View c = container.getChildAt(i);
							if ((c instanceof CheckBox || c instanceof AppCompatCheckBox)) {
								if (((CheckBox)c).isChecked()) {
									return;
								}
							} 
						}
						((CheckBox)buttonView).setChecked(true);
					}
				} else {	
					List<WeekDayRange>temp = new ArrayList<WeekDayRange>(days);
					for (WeekDayRange d:temp) {
						if (d.getNths() == null) {
							days.remove(d);
						}
					}
					WeekDayRange range = null;
					for (int i = 0; i < container.getChildCount(); i++) {
						final View c = container.getChildAt(i);
						if (c instanceof CheckBox || c instanceof AppCompatCheckBox) {
							if (((CheckBox)c).isChecked()) {
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
			}};
			
		for (int i = 0; i < container.getChildCount(); i++) {
			final View v = container.getChildAt(i);
			if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
				((CheckBox) v).setOnCheckedChangeListener(listener);
			}
		}
	}

	private void setNthListeners(final RelativeLayout container, final WeekDayRange days) {
		OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				List<Nth> nths = days.getNths();
				if (nths==null) {
					nths = new ArrayList<Nth>();
					days.setNths(nths);
				} else {
					nths.clear();
				}
				Nth range = null;
				for (int i = 0; i < container.getChildCount(); i++) {
					final View c = container.getChildAt(i);
					if (c instanceof CheckBox || c instanceof AppCompatCheckBox) {
						if (((CheckBox)c).isChecked()) {
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
			}};
			
		for (int i = 0; i < container.getChildCount(); i++) {
			final View v = container.getChildAt(i);
			if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
				((CheckBox) v).setOnCheckedChangeListener(listener);
			}
		}
	}

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(DEBUG_TAG, "onSaveInstanceState");
	   	outState.putSerializable(KEY_KEY, key);
    	outState.putSerializable(VALUE_KEY, text.getText().toString());
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
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME)
		// || element.hasTagKey(Tags.KEY_BUILDING));
	}

	/**
	 * Return the view we have our rows in and work around some android
	 * craziness
	 * 
	 * @return
	 */
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
					Log.d(DEBUG_TAG, "Found R.id.openinghours_layoutt");
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
}
