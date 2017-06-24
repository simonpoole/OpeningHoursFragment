package ch.poole.openinghoursfragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.VarDate;
import ch.poole.openinghoursparser.YearRange;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;


/**
 * Display a dialog allowing the user to select values for a start date and optionally an end date
 *
 */
public class DateRangePicker extends DialogFragment
{
	private static final int MAX_YEAR = 2100;

	public static final int NOTHING_SELECTED = Integer.MIN_VALUE;
	
	private static final String LISTENER = "listener";

	private static final String TITLE = "title";

	private static final String START_YEAR = "startYear";

	private static final String START_MONTH = "startMonth";
	
	private static final String START_DAY = "startDay";
	
	private static final String START_VARDATE = "startVarDate";
	
	private static final String START_ONLY = "startOnly";
	
	private static final String END_YEAR = "endYear";

	private static final String END_MONTH = "endMonth";
	
	private static final String END_DAY = "endDay";
	
	private static final String END_VARDATE = "endVarDate";

	private static final String DEBUG_TAG = DateRangePicker.class.getSimpleName();
	
	private static final String TAG = "fragment_datepicker";
		
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startMonth	initial start month
	 * @param startDay		initial start day of the month
	 * @param endYear		initial end year
	 * @param endMonth		initial end month
	 * @param endDay		initial end day of the month
	 * @param listener		listener used to return the chosen values
	 */
	static void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull Month startMonth, int startDay,
			int endYear, @NonNull Month endMonth, int endDay,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, startMonth, startDay, null,
	    		false,
	    		endYear, endMonth, endDay, null, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startVarDate	initial start variable date ie easter
	 * @param endYear		initial end year
	 * @param endMonth		initial end month
	 * @param endDay		initial end day of the month
	 * @param listener		listener used to return the chosen values
	 */
	static void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull VarDate startVarDate,
			int endYear, @NonNull Month endMonth, int endDay,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, null, DateWithOffset.UNDEFINED_MONTH_DAY, startVarDate, 
	    		false,
	    		endYear, endMonth, endDay, null, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startMonth	initial start month
	 * @param startDay		initial start day of the month
	 * @param endYear		initial end year
	 * @param endVarDate	initial end variable date ie easter
	 * @param listener		listener used to return the chosen values
	 */
	static void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull Month startMonth, int startDay,
			int endYear, @NonNull VarDate endVarDate,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, startMonth, startDay, null, 
	    		false,
	    		endYear, null, DateWithOffset.UNDEFINED_MONTH_DAY, endVarDate, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startVarDate	initial start variable date ie easter
	 * @param endYear		initial end year
	 * @param endVarDate	initial end variable date ie easter
	 * @param listener		listener used to return the chosen values
	 */
	static void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull VarDate startVarDate,
			int endYear, @NonNull VarDate endVarDate,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, null, DateWithOffset.UNDEFINED_MONTH_DAY, startVarDate, 
	    		false,
	    		endYear, null, DateWithOffset.UNDEFINED_MONTH_DAY, endVarDate, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startMonth	initial start month
	 * @param startDay		initial start day of the month
	 * @param listener		listener used to return the chosen values
	 */
	static public void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull Month startMonth, int startDay,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, startMonth, startDay, null, 
	    		true,
	    		YearRange.UNDEFINED_YEAR, null, DateWithOffset.UNDEFINED_MONTH_DAY, null, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	/**
	 * Show the DateRangePicker dialog
	 * 
	 * @param activity		activity calling this
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startVarDate	initial start variable date ie easter
	 * @param listener		listener used to return the chosen values
	 */
	static public void showDialog(FragmentActivity activity, 
			int title,
			int startYear, @NonNull VarDate startVarDate,
			@NonNull SetDateRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DateRangePicker datePickerFragment = newInstance(title, startYear, null, DateWithOffset.UNDEFINED_MONTH_DAY, startVarDate, 
	    		true,
	    		YearRange.UNDEFINED_YEAR, null, DateWithOffset.UNDEFINED_MONTH_DAY, null, listener);
	    datePickerFragment.show(fm, TAG);
	}
	
	private static void dismissDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(TAG);
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    ft.commit();
	}
		
	/**
	 * Create a new instance of DateRangePicker
	 * 
	 * @param title			resource id for the title to display
	 * @param startYear		initial start year
	 * @param startMonth	initial start month or null
	 * @param startDay		initial start day of the month
	 * @param startVarDate	initial start variable date ie easter or null
	 * @param startOnly		only show a picker for one date
	 * @param endYear		initial end year
	 * @param endMonth		initial end month or null
	 * @param endDay		initial end day of the month
	 * @param endVarDate	initial end variable date ie easter or null
	 * @param listener		listener used to return the chosen values
	 * @return and instance of DateRangePicker
	 */
    static private DateRangePicker newInstance(int title, int startYear, @Nullable Month startMonth, int startDay, @Nullable VarDate startVarDate, 
    		boolean startOnly,
    		int endYear, @Nullable Month endMonth, int endDay, @Nullable VarDate endVarDate, @NonNull SetDateRangeListener listener) {
    	DateRangePicker f = new DateRangePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(START_YEAR, startYear);
        args.putSerializable(START_MONTH, startMonth);
        args.putInt(START_DAY, startDay);
        args.putSerializable(START_VARDATE, startVarDate);
        args.putBoolean(START_ONLY, startOnly);
        args.putInt(END_YEAR, endYear);
        args.putSerializable(END_MONTH, endMonth);
        args.putInt(END_DAY, endDay);
        args.putSerializable(END_VARDATE, endVarDate);
        args.putSerializable(LISTENER, listener);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
	@SuppressLint("InflateParams")
	@Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	int title = getArguments().getInt(TITLE);
    	
    	VarDate startVarDate = (VarDate) getArguments().getSerializable(START_VARDATE);
    	VarDate endVarDate = (VarDate) getArguments().getSerializable(END_VARDATE);
    	
    	int startYear = getArguments().getInt(START_YEAR);
    	Month startMonth = (Month) getArguments().getSerializable(START_MONTH);
    	int startDay = getArguments().getInt(START_DAY);
    	int endYear = getArguments().getInt(END_YEAR);
    	Month endMonth = (Month) getArguments().getSerializable(END_MONTH);
    	int endDay = getArguments().getInt(END_DAY);	
    	
    	boolean startOnly =  getArguments().getBoolean(START_ONLY);
    	
    	final SetDateRangeListener listener = (SetDateRangeListener) getArguments().getSerializable(LISTENER);
    	
		// Preferences prefs= new Preferences(getActivity());
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(title);
    	
    	final LayoutInflater inflater = getActivity().getLayoutInflater(); //ThemeUtils.getLayoutInflater(getActivity()); 
    	
		// DoNothingListener doNothingListener = new DoNothingListener();
		View layout = inflater.inflate(R.layout.daterangepicker, null);
		builder.setView(layout);
		
		String[] yearValues = new String[MAX_YEAR - YearRange.FIRST_VALID_YEAR+2];
		yearValues[0]="-";
		for (int i=YearRange.FIRST_VALID_YEAR;i<=MAX_YEAR;i++) {
			yearValues[i-YearRange.FIRST_VALID_YEAR+1] = Integer.toString(i);
		}
		final NumberPickerView npvStartYear = (NumberPickerView)layout.findViewById(R.id.startYear);
		npvStartYear.setDisplayedValues(yearValues);
	   	npvStartYear.setMinValue(YearRange.FIRST_VALID_YEAR-1);
	   	npvStartYear.setMaxValue(MAX_YEAR);
	   	npvStartYear.setValue(startYear != YearRange.UNDEFINED_YEAR ? startYear : YearRange.FIRST_VALID_YEAR-1);
	   	
		String[] monthEntries = getActivity().getResources().getStringArray(R.array.months_entries);
		String[] dayValues = new String[32];
		dayValues[0]="-";
		for (int i=1;i<=31;i++) {
			dayValues[i] = Integer.toString(i);
		}		
		
		String[] varDateValues = getActivity().getResources().getStringArray(R.array.vardate_values);
		
		final NumberPickerView npvStartVarDate = (NumberPickerView)layout.findViewById(R.id.startVarDate);
		final NumberPickerView npvStartMonth = (NumberPickerView)layout.findViewById(R.id.startMonth);
		final NumberPickerView npvStartDay = (NumberPickerView)layout.findViewById(R.id.startDay);
		if (startVarDate == null) {
			npvStartVarDate.setVisibility(View.GONE);
			npvStartMonth.setVisibility(View.VISIBLE);
			npvStartDay.setVisibility(View.VISIBLE);
			
			npvStartMonth.setDisplayedValues(monthEntries);
			npvStartMonth.setMinValue(1);
			npvStartMonth.setMaxValue(12);
			npvStartMonth.setValue(startMonth.ordinal()+1);

			npvStartDay.setDisplayedValues(dayValues);
			npvStartDay.setMinValue(0);
			npvStartDay.setMaxValue(31);
			npvStartDay.setValue(startDay != DateWithOffset.UNDEFINED_MONTH_DAY ? startDay : 0);
		} else {
			npvStartVarDate.setVisibility(View.VISIBLE);
			npvStartMonth.setVisibility(View.GONE);
			npvStartDay.setVisibility(View.GONE);
			
			npvStartVarDate.setDisplayedValues(varDateValues);
			npvStartVarDate.setMinValue(1);
			npvStartVarDate.setMaxValue(VarDate.values().length);
			npvStartVarDate.setValue(startVarDate.ordinal()+1);
		}
	   	
		
		final NumberPickerView npvEndYear = (NumberPickerView)layout.findViewById(R.id.endYear);
		npvEndYear.setDisplayedValues(yearValues);
	   	npvEndYear.setMinValue(YearRange.FIRST_VALID_YEAR-1);
	   	npvEndYear.setMaxValue(MAX_YEAR);
	   	npvEndYear.setValue(endYear != YearRange.UNDEFINED_YEAR ? endYear : YearRange.FIRST_VALID_YEAR-1);
	   	
	   	final NumberPickerView npvEndVarDate = (NumberPickerView)layout.findViewById(R.id.endVarDate);
		final NumberPickerView npvEndMonth = (NumberPickerView)layout.findViewById(R.id.endMonth);
		final NumberPickerView npvEndDay = (NumberPickerView)layout.findViewById(R.id.endDay);
		
		if (startOnly) {
			npvEndYear.setVisibility(View.GONE);
			npvEndVarDate.setVisibility(View.GONE);
			npvEndMonth.setVisibility(View.GONE);
			npvEndDay.setVisibility(View.GONE);
		} else {
			if (endVarDate == null) {
				npvEndVarDate.setVisibility(View.GONE);
				npvEndMonth.setVisibility(View.VISIBLE);
				npvEndDay.setVisibility(View.VISIBLE);

				String[] tempMonthValues = new String[monthEntries.length+1];
				tempMonthValues[0] = "-";
				for(int i=0;i < monthEntries.length;i++) {
					tempMonthValues[i+1] = monthEntries[i];
				}
				npvEndMonth.setDisplayedValues(tempMonthValues);
				npvEndMonth.setMinValue(0);
				npvEndMonth.setMaxValue(12);
				npvEndMonth.setValue(endMonth != null ? endMonth.ordinal()+1 : 0);

				npvEndDay.setDisplayedValues(dayValues);
				npvEndDay.setMinValue(0);
				npvEndDay.setMaxValue(31);
				npvEndDay.setValue(endDay != DateWithOffset.UNDEFINED_MONTH_DAY ? endDay : 0);
			} else {
				npvEndVarDate.setVisibility(View.VISIBLE);
				npvEndMonth.setVisibility(View.GONE);
				npvEndDay.setVisibility(View.GONE);

				npvEndVarDate.setDisplayedValues(varDateValues);
				npvEndVarDate.setMinValue(1);
				npvEndVarDate.setMaxValue(VarDate.values().length);
				npvEndVarDate.setValue(endVarDate.ordinal()+1);
			}
		}

		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int startYearValue = getYearValue(npvStartYear);
				Month startMonthValue = getMonthValue(npvStartMonth);
				int startDayValue = getDayValue(npvStartDay);
				int endYearValue = getYearValue(npvEndYear);
				Month endMonthValue = getMonthValue(npvEndMonth);
				int endDayValue = getDayValue(npvEndDay);
				
				VarDate startVarDateValue = getVarDateValue(npvStartVarDate);
				VarDate endVarDateValue = getVarDateValue(npvEndVarDate);

				listener.setDateRange(startYearValue, startMonthValue, startDayValue, startVarDateValue, endYearValue, endMonthValue, endDayValue, endVarDateValue);
			}

			private Month getMonthValue(final NumberPickerView npvMonth) {
				Month monthValue = null;
				if (npvMonth.getValue() != 0) {
					monthValue = Month.values()[npvMonth.getValue()-1];
				}
				return monthValue;
			}

			private int getYearValue(final NumberPickerView npvYear) {
				int yearValue = npvYear.getValue();
				if (yearValue == YearRange.FIRST_VALID_YEAR-1) {
					yearValue=NOTHING_SELECTED;
				}
				return yearValue;
			}

			private int getDayValue(final NumberPickerView npvEndDay) {
				int dayValue = npvEndDay.getValue();
				if (dayValue == 0) {
					dayValue=NOTHING_SELECTED;
				}
				return dayValue;
			}
			
			private VarDate getVarDateValue(final NumberPickerView npvVarDate) {
				VarDate varDateValue = null;
				if (npvVarDate.getValue() != 0) {
					varDateValue = VarDate.values()[npvVarDate.getValue()-1];
				}
				return varDateValue;
			}
		});
		builder.setNeutralButton(android.R.string.cancel, null);
	   	
    	return builder.create();
    }	
}
