package ch.poole.openinghoursfragment;

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
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

/**
 * Display a dialog allowing the user to select values for a start date and optionally an end date
 *
 */
public class TimeRangePicker extends DialogFragment {
    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String LISTENER = "listener";

    private static final String TITLE = "title";

    private static final String START_HOUR = "startHour";

    private static final String START_MINUTE = "startMinute";

    private static final String START_ONLY = "startOnly";

    private static final String END_HOUR = "endHour";

    private static final String END_MINUTE = "endMinute";

    private static final String DEBUG_TAG = TimeRangePicker.class.getSimpleName();

    private static final String TAG = "fragment_timepicker";

    /**
     * Show the TimeRangePicker dialog
     * 
     * @param activity activity calling this
     * @param title resource id for the title to display
     * @param startHour initial start hour
     * @param startMinute initial start minute
     * @param endHour initial end hour
     * @param endMinute initial end minute
     * @param listener listener used to return the chosen values
     */
    static void showDialog(FragmentActivity activity, int title, int startHour, int startMinute, int endHour,
            int endMinute, @NonNull SetTimeRangeListener listener) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        TimeRangePicker timePickerFragment = newInstance(title, startHour, startMinute, false, endHour, endMinute,
                listener);
        timePickerFragment.show(fm, TAG);
    }

    /**
     * Show the DateRangePicker dialog
     * 
     * @param activity activity calling this
     * @param title resource id for the title to display
     * @param startHour initial start hour
     * @param startMinute initial start minute
     * @param listener listener used to return the chosen values
     */
    static public void showDialog(FragmentActivity activity, int title, int startHour, int startMinute,
            @NonNull SetTimeRangeListener listener) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        TimeRangePicker timePickerFragment = newInstance(title, startHour, startMinute, true, NOTHING_SELECTED,
                NOTHING_SELECTED, listener);
        timePickerFragment.show(fm, TAG);
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
     * @param title resource id for the title to display
     * @param startHour initial start hour
     * @param startMinute initial start minute
     * @param startOnly only show a picker for one time
     * @param endHour initial end hour
     * @param endMinute initial end minute
     * @param listener listener used to return the chosen values
     * @return an instance of TimeRangePicker
     */
    static private TimeRangePicker newInstance(int title, int startHour, int startMinute, boolean startOnly,
            int endHour, int endMinute, @NonNull SetTimeRangeListener listener) {
        TimeRangePicker f = new TimeRangePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(START_HOUR, startHour);
        args.putInt(START_MINUTE, startMinute);
        args.putBoolean(START_ONLY, startOnly);
        args.putInt(END_HOUR, endHour);
        args.putInt(END_MINUTE, endMinute);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE);

        int startHour = getArguments().getInt(START_HOUR);
        int startMinute = getArguments().getInt(START_MINUTE);
        int endHour = getArguments().getInt(END_HOUR);
        int endMinute = getArguments().getInt(END_MINUTE);

        boolean startOnly = getArguments().getBoolean(START_ONLY);

        final SetTimeRangeListener listener = (SetTimeRangeListener) getArguments().getSerializable(LISTENER);

        // Preferences prefs= new Preferences(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater(); // ThemeUtils.getLayoutInflater(getActivity());

        // DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.timerangepicker, null);
        builder.setView(layout);

        String[] hourValues = new String[48];
        for (int i = 0; i < 48; i++) {
            hourValues[i] = String.format("%02d", i);
        }
        final NumberPickerView npvStartHour = (NumberPickerView) layout.findViewById(R.id.startHour);
        npvStartHour.setDisplayedValues(hourValues);
        npvStartHour.setMinValue(0);
        npvStartHour.setMaxValue(23);
        npvStartHour.setValue(startHour);

        String[] minValues = new String[60];
        for (int i = 0; i < 60; i++) {
            minValues[i] = String.format("%02d", i);
        }

        final NumberPickerView npvStartMinute = (NumberPickerView) layout.findViewById(R.id.startMinute);

        npvStartMinute.setDisplayedValues(minValues);
        npvStartMinute.setMinValue(0);
        npvStartMinute.setMaxValue(59);
        npvStartMinute.setValue(startMinute);

        final NumberPickerView npvEndHour = (NumberPickerView) layout.findViewById(R.id.endHour);
        final NumberPickerView npvEndMinute = (NumberPickerView) layout.findViewById(R.id.endMinute);
        if (startOnly) {
            npvEndHour.setVisibility(View.GONE);
            npvEndMinute.setVisibility(View.GONE);
        } else {
            npvEndHour.setVisibility(View.VISIBLE);
            npvEndMinute.setVisibility(View.VISIBLE);
            npvEndHour.setDisplayedValues(hourValues);
            npvEndHour.setMinValue(0);
            npvEndHour.setMaxValue(47);
            npvEndHour.setValue(endHour);

            npvEndMinute.setDisplayedValues(minValues);
            npvEndMinute.setMinValue(0);
            npvEndMinute.setMaxValue(59);
            npvEndMinute.setValue(endMinute);
        }

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int startHourValue = npvStartHour.getValue();
                int startMinuteValue = npvStartMinute.getValue();
                int endHourValue = npvEndHour.getValue();
                int endMinuteValue = npvEndMinute.getValue();

                listener.setTimeRange(startHourValue, startMinuteValue, endHourValue, endMinuteValue);
            }
        });
        builder.setNeutralButton(android.R.string.cancel, null);

        return builder.create();
    }
}
