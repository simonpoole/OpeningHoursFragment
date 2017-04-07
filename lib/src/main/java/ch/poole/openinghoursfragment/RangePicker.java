package ch.poole.openinghoursfragment;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;
import ch.poole.openinghoursfragment.R;

// import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class RangePicker extends DialogFragment
{
	
	private static final String LISTENER = "listener";

	private static final String TITLE = "title";

	private static final String MIN = "min";

	private static final String MAX = "max";

	private static final String START_CURRENT = "startCurrent";

	private static final String END_CURRENT = "endCurrent";

	private static final String DEBUG_TAG = RangePicker.class.getSimpleName();
	
	private static final String TAG = "fragment_rangepicker";
		
	/**
	 
	 */
	static public void showDialog(FragmentActivity activity, 
			int title,
			int min, int max, int startCurrent, int endCurrent,
			SetRangeListener listener) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    RangePicker rangePickerFragment = newInstance(title, min, max, startCurrent, endCurrent, listener);
	    if (rangePickerFragment != null) {
	    	rangePickerFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create range picker dialog ");
	    }
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
     */
    static private RangePicker newInstance(int title, int min, int max, int startCurrent, int endCurrent, SetRangeListener listener) {
    	RangePicker f = new RangePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(MIN, min);
        args.putInt(MAX, max);
        args.putInt(START_CURRENT, startCurrent);
        args.putInt(END_CURRENT, endCurrent);
        args.putSerializable(LISTENER, listener);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
//        if (!(activity instanceof Main)) {
//            throw new ClassCastException(activity.toString() + " can ownly be called from Main");
//        }
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
    	int min = getArguments().getInt(MIN);
    	int max = getArguments().getInt(MAX);
    	int startCurrent = getArguments().getInt(START_CURRENT);
    	int endCurrent = getArguments().getInt(END_CURRENT);
    	final SetRangeListener listener = (SetRangeListener) getArguments().getSerializable(LISTENER);
    	
		// Preferences prefs= new Preferences(getActivity());
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(title);
    	
    	final LayoutInflater inflater = getActivity().getLayoutInflater(); //ThemeUtils.getLayoutInflater(getActivity()); 
    	
		// DoNothingListener doNothingListener = new DoNothingListener();
		View layout = inflater.inflate(R.layout.rangepicker, null);
		builder.setView(layout);
		
		String[] startValues = new String[max-min+1];
		for (int i=0;i<startValues.length;i++) {
			startValues[i] = Integer.toString(min + i);
		}
		final NumberPickerView npvStart = (NumberPickerView)layout.findViewById(R.id.start);
		npvStart.setDisplayedValues(startValues);
	   	npvStart.setMinValue(0);
	   	npvStart.setMaxValue(max-min);
	   	npvStart.setValue(startCurrent);
	   	
		String[] endValues = new String[max-min+2];
		endValues[0]="-";
		for (int i=1;i<endValues.length;i++) {
			endValues[i] = Integer.toString(min + i);
		}
		final NumberPickerView npvEnd = (NumberPickerView)layout.findViewById(R.id.end);
		npvEnd.setDisplayedValues(endValues);
	   	npvEnd.setMinValue(-1);
	   	npvEnd.setMaxValue(max-min);
	   	npvEnd.setValue(endCurrent);

		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.setRange(npvStart.getValue(),npvEnd.getValue());
			}
		});
		builder.setNeutralButton(android.R.string.cancel, null);
	   	
    	return builder.create();
    }		
}
