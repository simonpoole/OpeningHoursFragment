package ch.poole.openinghoursfragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

/**
 * Display a dialog allowing the user to set a numeric range
 *
 */
public class RangePicker extends DialogFragment {
    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String LISTENER = "listener";

    private static final String TITLE = "title";

    private static final String MIN = "min";

    private static final String MAX = "max";

    private static final String START_CURRENT = "startCurrent";

    private static final String END_CURRENT = "endCurrent";

    private static final String DEBUG_TAG = RangePicker.class.getSimpleName();

    private static final String TAG = "fragment_rangepicker";

    /**
     * Show the range picker dialog
     * 
     * @param parentFragment Fragment that called this
     * @param title resource id for the title to display
     * @param min minimum range value
     * @param max maximum range value
     * @param startCurrent initial start value
     * @param endCurrent initial end value
     */
    static void showDialog(Fragment parentFragment, int title, int min, int max, int startCurrent, int endCurrent) {
        dismissDialog(parentFragment);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        RangePicker rangePickerFragment = newInstance(title, min, max, startCurrent, endCurrent);
        rangePickerFragment.show(fm, TAG);
    }

    private static void dismissDialog(Fragment parentFragment) {
        FragmentManager fm = parentFragment.getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }
        ft.commit();
    }

    /**
     * Create a new instance of RangePicker
     * 
     * @param title resource id for the title to display
     * @param min minimum range value
     * @param max maximum range value
     * @param startCurrent initial start value
     * @param endCurrent initial end value
     * @return an instance of RangePicker
     */
    static private RangePicker newInstance(int title, int min, int max, int startCurrent, int endCurrent) {
        RangePicker f = new RangePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(MIN, min);
        args.putInt(MAX, max);
        args.putInt(START_CURRENT, startCurrent);
        args.putInt(END_CURRENT, endCurrent);

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE);
        final int min = getArguments().getInt(MIN);
        final int max = getArguments().getInt(MAX);
        int startCurrent = getArguments().getInt(START_CURRENT);
        int endCurrent = getArguments().getInt(END_CURRENT);
        final SetRangeListener listener = (SetRangeListener) getParentFragment();

        // Preferences prefs= new Preferences(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater(); // ThemeUtils.getLayoutInflater(getActivity());

        // DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.rangepicker, null);
        builder.setView(layout);

        String[] startValues = new String[max - min + 1];
        for (int i = min; i <= max; i++) {
            startValues[i - min] = Integer.toString(i);
        }
        final NumberPickerView npvStart = (NumberPickerView) layout.findViewById(R.id.start);
        npvStart.setDisplayedValues(startValues);
        npvStart.setMinValue(min);
        npvStart.setMaxValue(max);
        npvStart.setValue(startCurrent);

        String[] endValues = new String[max - min + 2];
        endValues[0] = "-";
        for (int i = min; i <= max; i++) {
            endValues[i - min + 1] = Integer.toString(i);
        }
        final NumberPickerView npvEnd = (NumberPickerView) layout.findViewById(R.id.end);
        npvEnd.setDisplayedValues(endValues);
        npvEnd.setMinValue(min - 1);
        npvEnd.setMaxValue(max);
        if (endCurrent == NOTHING_SELECTED) {
            endCurrent = min - 1;
        }
        npvEnd.setValue(endCurrent);

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int endValue = npvEnd.getValue();
                if (endValue == min - 1) {
                    endValue = NOTHING_SELECTED;
                }
                listener.setRange(npvStart.getValue(), endValue);
            }
        });
        builder.setNeutralButton(android.R.string.cancel, null);

        return builder.create();
    }
}
