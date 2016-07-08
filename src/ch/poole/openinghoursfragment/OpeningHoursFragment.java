package ch.poole.openinghoursfragment;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.appyvet.rangebar.RangeBar;
import com.appyvet.rangebar.RangeBar.OnRangeBarChangeListener;
import com.appyvet.rangebar.RangeBar.PinTextFormatter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import ch.poole.openinghoursparser.Holiday;
import ch.poole.openinghoursparser.MonthDayRange;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.TokenMgrError;
import ch.poole.openinghoursparser.Util;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
import ch.poole.openinghoursparser.YearRange;

public class OpeningHoursFragment extends DialogFragment {

	private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();

	private LayoutInflater inflater = null;

	private TextWatcher watcher;
	
	private String openingHoursValue;
	
	private ArrayList<Rule> rules;
	
	private EditText text;
	
	private OnSaveListener saveListener = null;
	
	private TemplateListener templateListener = null;
	

	List<String> weekDays = Arrays.asList("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su");

	static PinTextFormatter timeFormater = new PinTextFormatter() {
		@Override
		public String getText(String value) {
			int minutes = Integer.valueOf(value);
			return String.format("%02d", minutes / 60) + ":" + String.format("%02d", minutes % 60);
		}
	};

	/**
	 */
	static public OpeningHoursFragment newInstance(String value) {
		OpeningHoursFragment f = new OpeningHoursFragment();

		Bundle args = new Bundle();
		args.putSerializable("value", value);

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

		// Inflate the layout for this fragment
		// this.inflater = inflater;
		Context context =  new ContextThemeWrapper(getActivity(), R.style.Base_AlertDialog_AppCompat_Light);
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		LinearLayout openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours, null);

		// if (savedInstanceState != null) {
		// Log.d(DEBUG_TAG,"Restoring from saved state");
		// parents = (HashMap<Long, String>)
		// savedInstanceState.getSerializable("PARENTS");
		// } else if (savedParents != null ) {
		// Log.d(DEBUG_TAG,"Restoring from instance variable");
		// parents = savedParents;
		// } else {
		openingHoursValue = getArguments().getString("value");
		// }
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
				saveListener.save(text.getText().toString());	
				dismiss();
			}});
		
		return openingHoursLayout;
	}

	/**
	 * 
	 * @param openingHoursLayout
	 * @param openingHoursValue2
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
		}
	}
	
	/**
	 * Highlight the position of a parse error
	 * @param text
	 * @param pex
	 */
	private void highlightParseError(EditText text, ParseException pex) {
		int c = pex.currentToken.next.beginColumn-1; // starts at 1
		int pos = text.getSelectionStart();
		Spannable spannable = new SpannableString(text.getText());
		spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c,Math.min(c+1,spannable.length()-1)), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		text.removeTextChangedListener(watcher); // avoid infinite loop
		text.setText(spannable, TextView.BufferType.SPANNABLE);
		text.setSelection(Math.min(pos,spannable.length()));
		text.addTextChangedListener(watcher);
	}
	
	/**
	 * Remove all highlighting
	 * @param text
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

	private synchronized void buildForm(ScrollView sv, ArrayList<Rule> rules) {

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
		//


		ArrayList<ArrayList<Rule>> mergeableRules = Util.getMergeableRules(rules);
		int n = 1;
		for (ArrayList<Rule> ruleList : mergeableRules) {
			boolean first = true;
			for (Rule r : ruleList) {
				if (first) { // everything except days and times should be
					// the same and only needs to be displayed
					// once
					LinearLayout groupHeader = (LinearLayout) inflater.inflate(R.layout.rule_header, null);
					TextView header = (TextView) groupHeader.findViewById(R.id.header);
					header.setText(getActivity().getString(R.string.group_header, n));
					addStandardMenuItems(groupHeader);
					ll.addView(groupHeader);
					String comment = r.getComment();
					if (comment != null && comment.length() > 0) {
						TextView tv = new TextView(getActivity());
						tv.setText(comment);
						ll.addView(tv);
					}
					// year range list
					ArrayList<YearRange> years = r.getYears();
					LinearLayout yearLayout = new LinearLayout(getActivity());
					yearLayout.setOrientation(LinearLayout.HORIZONTAL);
					;
					ll.addView(yearLayout);
					if (years != null && years.size() > 0) {
						for (YearRange yr : years) {
							// NumberPicker np1 =
							// getYearPicker(yr.getStartYear());
							EditText np1 = new EditText(getActivity());
							np1.setText(Integer.toString(yr.getStartYear()));
							int endYear = yr.getEndYear();
							if (endYear < 0) {
								endYear = yr.getStartYear();
							}
							// NumberPicker np2 = getYearPicker(endYear);
							EditText np2 = new EditText(getActivity());
							np2.setText(Integer.toString(endYear));
							yearLayout.addView(np1);
							yearLayout.addView(dash);
							yearLayout.addView(np2);
							if (years.get(years.size() - 1) != yr) {
								yearLayout.addView(comma);
							}
						}
					}
					// week list
					ArrayList<WeekRange> weeks = r.getWeeks();
					if (weeks != null && weeks.size() > 0) {
						StringBuffer b = new StringBuffer();
						for (WeekRange wr : weeks) {
							b.append(wr.toString());
							if (weeks.get(weeks.size() - 1) != wr) {
								b.append(",");
							}
						}
						TextView tv = new TextView(getActivity());
						tv.setText(b.toString());
						ll.addView(tv);
					}
					// month day list
					ArrayList<MonthDayRange> monthdays = r.getMonthdays();
					if (monthdays != null && monthdays.size() > 0) {
						StringBuffer b = new StringBuffer();
						for (MonthDayRange md : monthdays) {
							b.append(md.toString());
							if (monthdays.get(monthdays.size() - 1) != md) {
								b.append(",");
							}
						}
						TextView tv = new TextView(getActivity());
						tv.setText(b.toString());
						ll.addView(tv);
					}
					// modifier
					RuleModifier modifier = r.getModifier();
					if (modifier != null && modifier.getModifier() != null && modifier.getModifier().length() > 0) {
						TextView tv = new TextView(getActivity());
						tv.setText(modifier.getModifier());
						ll.addView(tv);
					}
					if (modifier != null && modifier.getComment() != null && modifier.getComment().length() > 0) {
						TextView tv = new TextView(getActivity());
						tv.setText(modifier.getComment());
						ll.addView(tv);
					}
					first = false;
					n++;
				}

				// days and times will be different per rule
				// are supposedly pseudo days
				// holiday list
				ArrayList<Holiday> holidays = r.getHolidays();
				if (holidays != null && holidays.size() > 0) {
					for (Holiday hd : holidays) {
						LinearLayout holidayRow = (LinearLayout) inflater.inflate(R.layout.holiday_row, null);
						TextView tv = (TextView) holidayRow.findViewById(R.id.holiday);
						if (hd.getType()==Holiday.Type.PH) {
							tv.setText(R.string.public_holidays);
							tv.setTag(Holiday.Type.PH);
						} else {
							tv.setText(R.string.school_holidays);
							tv.setTag(Holiday.Type.SH);
						}
						ll.addView(holidayRow);
					}
				}
				// day list
				ArrayList<WeekDayRange> days = r.getDays();
				if (days != null && days.size() > 0) {
					LinearLayout weekDayRow = (LinearLayout) inflater.inflate(R.layout.weekday_range_row, null);
					RelativeLayout checkBoxContainer = (RelativeLayout) weekDayRow
							.findViewById(R.id.checkBoxContainer);
					for (WeekDayRange d : days) {
						String startDay = d.getStartDay();
						String endDay = d.getEndDay();
						if (endDay == null) {
							checkWeekDay(checkBoxContainer, startDay);
						} else {
							int startIndex = weekDays.indexOf(startDay);
							int endIndex = weekDays.indexOf(endDay);
							Log.d(DEBUG_TAG, "startDay " + startDay + " " + startIndex + " endDay " + endDay + " " + endIndex);
							for (int i = startIndex; i <= endIndex; i++) {
								checkWeekDay(checkBoxContainer, weekDays.get(i));
							}
						}
					}
					setWeekDayListeners(checkBoxContainer, days);
					addStandardMenuItems(weekDayRow);
					ll.addView(weekDayRow);
				}
				// times
				ArrayList<TimeSpan> times = r.getTimes();
				addTimeSpanUIs(ll, times);
			}
		}
	}
	
	private void addTimeSpanUIs(LinearLayout ll, ArrayList<TimeSpan> times) {
		if (times != null && times.size() > 0) {
			Log.d(DEBUG_TAG, "#time spans " + times.size());
			for (final TimeSpan ts : times) {
				boolean hasStartEvent = ts.getStartEvent() != null;
				boolean hasEndEvent = ts.getEndEvent() != null;
				boolean extendedTime = ts.getEnd() > 1440;
				if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() > 0
						&& !extendedTime) {
					Log.d(DEBUG_TAG, "t-t " + ts.toString());
					LinearLayout timeRangeRow = (LinearLayout) inflater.inflate(R.layout.time_range_row, null);
					RangeBar timeBar = (RangeBar) timeRangeRow.findViewById(R.id.timebar);
					int start = ts.getStart();
					int end = ts.getEnd();
					if (start >= 360) {
						timeBar.setTickStart(360);
					}
					if ((start % 5 > 0) || (end % 5 > 0)) { // need
						// minute
						// granularity
						timeBar.setTickInterval(1);
						timeBar.setVisibleTickInterval(60);
					}
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
					addStandardMenuItems(timeRangeRow);
					ll.addView(timeRangeRow);
				} else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() > 0
						&& extendedTime) {
					Log.d(DEBUG_TAG, "t-x " + ts.toString());
					LinearLayout timeExtendedRangeRow = (LinearLayout) inflater
							.inflate(R.layout.time_extended_range_row, null);
					RangeBar timeBar = (RangeBar) timeExtendedRangeRow.findViewById(R.id.timebar);
					RangeBar extendedTimeBar = (RangeBar) timeExtendedRangeRow
							.findViewById(R.id.extendedTimebar);
					int start = ts.getStart();
					int end = ts.getEnd();
					if ((start % 5 > 0) || (end % 5 > 0)) { // need
						// minute
						// granularity
						timeBar.setTickInterval(1);
						timeBar.setVisibleTickInterval(60);
						extendedTimeBar.setTickInterval(1);
						extendedTimeBar.setVisibleTickInterval(60);
					}
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
					addStandardMenuItems(timeExtendedRangeRow);
					ll.addView(timeExtendedRangeRow);
				} else if (!ts.isOpenEnded() && !hasStartEvent && !hasEndEvent && ts.getEnd() <= 0) {
					Log.d(DEBUG_TAG, "pot " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					timeBar.setPinTextFormatter(timeFormater);
					int start = ts.getStart();
					timeBar.setConnectingLineEnabled(false);
					if (start % 5 > 0) { // need minute granularity
						timeBar.setTickInterval(1);
						timeBar.setVisibleTickInterval(60);
					}
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
					addStandardMenuItems(timeEventRow);
					ll.addView(timeEventRow);
				} else if (!ts.isOpenEnded() && !hasStartEvent && hasEndEvent) {
					Log.d(DEBUG_TAG, "t-e " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					timeBar.setPinTextFormatter(timeFormater);
					int start = ts.getStart();
					if (start % 5 > 0) { // need minute granularity
						timeBar.setTickInterval(1);
						timeBar.setVisibleTickInterval(60);
					}
					timeBar.setRangePinsByValue(0, start);
					timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
						@Override
						public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
								int rightPinIndex, String leftPinValue, String rightPinValue) {
							ts.setStart(toMins(rightPinValue));
							updateString();
						}});
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					endEvent.setSelection(((ArrayAdapter<String>) endEvent.getAdapter())
							.getPosition(ts.getEndEvent().getEvent()));
					addStandardMenuItems(timeEventRow);
					ll.addView(timeEventRow);
				} else if (!ts.isOpenEnded() && hasStartEvent && !hasEndEvent) {
					Log.d(DEBUG_TAG, "e-t " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_start_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					int end = ts.getEnd();
					if (end > 0) {
						timeBar.setPinTextFormatter(timeFormater);
						if (end >= 360) {
							timeBar.setTickStart(360);
						}
						if (end % 5 > 0) { // need minute granularity
							timeBar.setTickInterval(1);
							timeBar.setVisibleTickInterval(60);
						}
						timeBar.setRangePinsByValue(0, end);
						timeBar.setOnRangeBarChangeListener(new OnRangeBarChangeListener() {
							@Override
							public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
									int rightPinIndex, String leftPinValue, String rightPinValue) {
								ts.setEnd(toMins(rightPinValue));
								updateString();
							}});
					} else {
						timeBar.setVisibility(View.GONE);
					}
					Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.startEvent);
					startEvent.setSelection(((ArrayAdapter<String>) startEvent.getAdapter())
							.getPosition(ts.getStartEvent().getEvent()));
					addStandardMenuItems(timeEventRow);
					ll.addView(timeEventRow);
				} else if (!ts.isOpenEnded() && hasStartEvent && hasEndEvent) {
					Log.d(DEBUG_TAG, "e-e " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_event_row, null);
					Spinner startEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					startEvent.setSelection(((ArrayAdapter<String>) startEvent.getAdapter())
							.getPosition(ts.getStartEvent().getEvent()));
					Spinner endEvent = (Spinner) timeEventRow.findViewById(R.id.endEvent);
					endEvent.setSelection(((ArrayAdapter<String>) endEvent.getAdapter())
							.getPosition(ts.getEndEvent().getEvent()));
					addStandardMenuItems(timeEventRow);
					ll.addView(timeEventRow);
				} else if (ts.isOpenEnded() && !hasStartEvent) {
					Log.d(DEBUG_TAG, "t- " + ts.toString());
					LinearLayout timeEventRow = (LinearLayout) inflater.inflate(R.layout.time_end_event_row, null);
					RangeBar timeBar = (RangeBar) timeEventRow.findViewById(R.id.timebar);
					timeBar.setPinTextFormatter(timeFormater);
					int start = ts.getStart();
					if (start % 5 > 0) { // need minute granularity
						timeBar.setTickInterval(1);
						timeBar.setVisibleTickInterval(60);
					}
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
					addStandardMenuItems(timeEventRow);
					ll.addView(timeEventRow);
				} else {
					Log.d(DEBUG_TAG, "? " + ts.toString());
					TextView tv = new TextView(getActivity());
					tv.setText(ts.toString());
					ll.addView(tv);
				}
			}
		}
	}
	
	private Menu addStandardMenuItems(LinearLayout row) {
		ActionMenuView amv = (ActionMenuView) row.findViewById(R.id.menu);
		Menu menu = amv.getMenu();
		MenuItem mi = menu.add("Delete");
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
	
	private void setWeekDayListeners(final RelativeLayout container, final ArrayList<WeekDayRange> days) {
		OnCheckedChangeListener listener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				days.clear();
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
				updateString();
			}};
			
		for (int i = 0; i < container.getChildCount(); i++) {
			final View v = container.getChildAt(i);
			if (v instanceof CheckBox || v instanceof AppCompatCheckBox) {
				((CheckBox) v).setOnCheckedChangeListener(listener);
			}
		}
	}


	@SuppressLint("NewApi")
	NumberPicker getYearPicker(int year) {
		NumberPicker np = new NumberPicker(getActivity());
		np.setMinValue(1900);
		np.setMaxValue(3000);
		np.setValue(year);
		np.setFormatter(new NumberPicker.Formatter() {
			@Override
			public String format(int value) {
				return String.format(Locale.US,"%04d", value);
			}
		});
		return np;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(DEBUG_TAG, "onSaveInstanceState");
		// outState.putSerializable("PARENTS", savedParents);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(DEBUG_TAG, "onPause");
		// savedParents = getParentRelationMap();
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
