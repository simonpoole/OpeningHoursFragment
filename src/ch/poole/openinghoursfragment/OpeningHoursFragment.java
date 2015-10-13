package ch.poole.openinghoursfragment;

import ch.poole.openinghoursparser.Holiday;
import ch.poole.openinghoursparser.MonthDayRange;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.Util;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
import ch.poole.openinghoursparser.YearRange;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


public class OpeningHoursFragment extends SherlockDialogFragment {
	
	private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();
	
	private LayoutInflater inflater = null;
	
	private String openingHoursValue;
	
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
//        try {
//            mListener = (OnPresetSelectedListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		LinearLayout openingHoursLayout = null;


    	// Inflate the layout for this fragment
    	this.inflater = inflater;
    	openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours,null);

//    	if (savedInstanceState != null) {
//    		Log.d(DEBUG_TAG,"Restoring from saved state");
//    		parents = (HashMap<Long, String>) savedInstanceState.getSerializable("PARENTS");
//    	} else if (savedParents != null ) {
//    		Log.d(DEBUG_TAG,"Restoring from instance variable");
//    		parents = savedParents;
//    	} else {
    	openingHoursValue = getArguments().getString("value");
//    	}
    	buildForm(openingHoursLayout,openingHoursValue);
 
		return openingHoursLayout;
    }

    /**
     * 
     * @param openingHoursLayout
     * @param openingHoursValue2
     */
    @SuppressLint("NewApi")
	private void buildForm(LinearLayout openingHoursLayout, String openingHoursValue) {
    	EditText text = (EditText) openingHoursLayout.findViewById(R.id.openinghours_string_edit);
    	LinearLayout openingHoursView = (LinearLayout) openingHoursLayout.findViewById(R.id.openinghours_view);
		if (text != null && openingHoursView != null) {
			text.setText(openingHoursValue);
			openingHoursView.removeAllViews();
			ScrollView sv = new ScrollView(getActivity());
			openingHoursView.addView(sv);
			LinearLayout ll = new LinearLayout(getActivity());
			ll.setOrientation(LinearLayout.VERTICAL);
//			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//					LinearLayout.LayoutParams.MATCH_PARENT,
//					LinearLayout.LayoutParams.WRAP_CONTENT);
//		    layoutParams.setMargins(20, 20, 20, 20);
//		    ll.setLayoutParams(layoutParams);
			sv.addView(ll);
			// some standard static elements
			TextView dash = new TextView(getActivity());
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT,0.0f);
			dash.setLayoutParams(params);
			dash.setText("-");
			dash.setGravity(Gravity.CENTER_VERTICAL);
			TextView comma = new TextView(getActivity());
			comma.setLayoutParams(params);
			comma.setText(",");
			comma.setGravity(Gravity.CENTER_VERTICAL);
			//
			OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHoursValue.getBytes()));
			try {
				ArrayList<Rule> rules = parser.rules();
				ArrayList<ArrayList<Rule>> mergeableRules = Util.getMergeableRules(rules);
				int n = 1;
				for (ArrayList<Rule>ruleList:mergeableRules)
				{
					boolean first = true;
					for (Rule r : ruleList)
					{
						if (first) {
							TextView header = new TextView(getActivity());
							header.setText("Rule " + n);
							header.setTextSize(24.0f);
							ll.addView(header);
							String comment = r.getComment();
							if (comment != null && comment.length() > 0) {
								TextView tv = new TextView(getActivity());
								tv.setText(comment);
								ll.addView(tv);
							}
							// year range list
							ArrayList<YearRange> years = r.getYears();
							LinearLayout yearLayout = new LinearLayout(getActivity());
							yearLayout.setOrientation(LinearLayout.HORIZONTAL);;
							ll.addView(yearLayout);
							if (years != null && years.size() > 0) {
								
								StringBuffer b = new StringBuffer();
								for (YearRange yr:years) {
									// NumberPicker np1 = getYearPicker(yr.getStartYear());
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
									if (years.get(years.size()-1)!=yr) {
										yearLayout.addView(comma);
									} 
								}
							}
							// week list
							ArrayList<WeekRange> weeks = r.getWeeks();
							if (weeks != null && weeks.size() > 0) {
								StringBuffer b = new StringBuffer();
								for (WeekRange wr:weeks) {
									b.append(wr.toString());
									if (weeks.get(weeks.size()-1)!=wr) {
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
								for (MonthDayRange md:monthdays) {
									b.append(md.toString());
									if (monthdays.get(monthdays.size()-1)!=md) {
										b.append(",");
									} 
								}
								TextView tv = new TextView(getActivity());
								tv.setText(b.toString());
								ll.addView(tv);
							}
							// holiday list
							ArrayList<Holiday> holidays = r.getHolidays();
							if (holidays != null && holidays.size() > 0) {
								StringBuffer b = new StringBuffer();
								for (Holiday hd:holidays) {
									b.append(hd.toString());
									if (holidays.get(holidays.size()-1)!=hd) {
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

						// day list
						ArrayList<WeekDayRange> days = r.getDays();
						if (days != null && days.size() > 0){
							StringBuffer b = new StringBuffer();
							for (WeekDayRange d:days) {
								b.append(d.toString());
								if (days.get(days.size()-1)!=d) {
									b.append(",");
								} 
							}
							TextView tv = new TextView(getActivity());
							tv.setText(b.toString());
							ll.addView(tv);
						}
						// times
						ArrayList<TimeSpan> times = r.getTimes();
						if (times != null && times.size() > 0){
							StringBuffer b = new StringBuffer();
							for (TimeSpan ts:times) {
								b.append(ts.toString());
								if (times.get(times.size()-1)!=ts) {
									b.append(",");
								} 
							}
							TextView tv = new TextView(getActivity());
							tv.setText(b.toString());
							ll.addView(tv);
						}
					}
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				return String.format("%04d",value);    
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
    	// savedParents  = getParentRelationMap();
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
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.openinghours_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.openinghours_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.openinghours_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.openinghours_layoutt");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
}
