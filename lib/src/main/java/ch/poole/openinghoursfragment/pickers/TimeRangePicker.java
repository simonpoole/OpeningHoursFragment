package ch.poole.openinghoursfragment.pickers;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import ch.poole.openinghoursfragment.CancelableDialogFragment;
import ch.poole.openinghoursfragment.R;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;
import cn.carbswang.android.numberpickerview.library.NumberPickerView.OnValueChangeListener;

/**
 * Display a dialog allowing the user to select values for a start date and optionally an end date
 *
 */
public class TimeRangePicker extends CancelableDialogFragment {
    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String TITLE        = "title";
    private static final String START_HOUR   = "startHour";
    private static final String START_MINUTE = "startMinute";
    private static final String START_ONLY   = "startOnly";
    private static final String END_HOUR     = "endHour";
    private static final String END_MINUTE   = "endMinute";
    private static final String INCREMENT    = "increment";

    private static final String TAG = "fragment_timepicker";

    private static final int MAX_MINUTES        = 59;
    private static final int MAX_EXTENDED_HOURS = 48;

    /**
     * Show the TimeRangePicker dialog
     * 
     * @param parentFragment Fragment calling this
     * @param title resource id for the title to display
     * @param startHour initial start hour
     * @param startMinute initial start minute
     * @param endHour initial end hour
     * @param endMinute initial end minute
     * @param increment minute tick size
     */
    public static void showDialog(@NonNull Fragment parentFragment, int title, int startHour, int startMinute, int endHour, int endMinute, int increment) {
        dismissDialog(parentFragment, TAG);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        TimeRangePicker timePickerFragment = newInstance(title, startHour, startMinute, false, endHour, endMinute, increment);
        timePickerFragment.show(fm, TAG);
    }

    /**
     * Show the TimeRangePicker dialog
     * 
     * @param parentFragment Fragment calling this
     * @param title resource id for the title to display
     * @param startHour initial start hour
     * @param startMinute initial start minute
     * @param increment minute tick size
     */
    public static void showDialog(@NonNull Fragment parentFragment, int title, int startHour, int startMinute, int increment) {
        dismissDialog(parentFragment, TAG);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        TimeRangePicker timePickerFragment = newInstance(title, startHour, startMinute, true, NOTHING_SELECTED, NOTHING_SELECTED, increment);
        timePickerFragment.show(fm, TAG);
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
     * @param increment minute tick size
     * @return an instance of TimeRangePicker
     */
    private static TimeRangePicker newInstance(int title, int startHour, int startMinute, boolean startOnly, int endHour, int endMinute, int increment) {
        TimeRangePicker f = new TimeRangePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(START_HOUR, startHour);
        args.putInt(START_MINUTE, startMinute);
        args.putBoolean(START_ONLY, startOnly);
        args.putInt(END_HOUR, endHour);
        args.putInt(END_MINUTE, endMinute);
        args.putInt(INCREMENT, increment);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
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
        int increment = getArguments().getInt(INCREMENT, 1);
        boolean startOnly = getArguments().getBoolean(START_ONLY);

        final SetTimeRangeListener listener = (SetTimeRangeListener) getParentFragment();

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.timerangepicker, null);
        builder.setView(layout);

        String[] hourValues = new String[MAX_EXTENDED_HOURS + 1];
        for (int i = 0; i <= MAX_EXTENDED_HOURS; i++) {
            hourValues[i] = String.format("%02d", i);
        }
        final NumberPickerView npvStartHour = (NumberPickerView) layout.findViewById(R.id.startHour);
        npvStartHour.setDisplayedValues(hourValues);
        npvStartHour.setMinValue(0);
        npvStartHour.setMaxValue(23);
        npvStartHour.setValue(startHour);

        int ticks = 60 / increment;
        String[] minValues = new String[ticks];
        for (int i = 0; i < ticks; i++) {
            minValues[i] = String.format("%02d", i * increment);
        }

        final NumberPickerView npvStartMinute = (NumberPickerView) layout.findViewById(R.id.startMinute);

        int maxMinutes = MAX_MINUTES / increment;
        npvStartMinute.setDisplayedValues(minValues);
        npvStartMinute.setMinValue(0);
        npvStartMinute.setMaxValue(maxMinutes);
        npvStartMinute.setValue(startMinute / increment);

        final NumberPickerView npvEndHour = (NumberPickerView) layout.findViewById(R.id.endHour);
        final NumberPickerView npvEndMinute = (NumberPickerView) layout.findViewById(R.id.endMinute);
        if (startOnly) {
            npvEndHour.setVisibility(View.GONE);
            npvEndMinute.setVisibility(View.GONE);
        } else {
            npvStartHour.setOnValueChangedListener(new OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
                    if (newVal >= npvEndHour.getValue()) {
                        npvEndHour.smoothScrollToValue(newVal);
                    }
                }
            });
            npvEndHour.setVisibility(View.VISIBLE);
            npvEndMinute.setVisibility(View.VISIBLE);
            npvEndHour.setDisplayedValues(hourValues);
            npvEndHour.setMinValue(0);
            npvEndHour.setMaxValue(MAX_EXTENDED_HOURS);
            npvEndHour.setValue(endHour);
            npvEndHour.setOnValueChangedListener(new OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
                    if (newVal == MAX_EXTENDED_HOURS) {
                        npvEndMinute.smoothScrollToValue(0);
                        npvEndMinute.setMaxValue(0);
                    } else {
                        npvEndMinute.setMaxValue(maxMinutes);
                    }
                }
            });

            npvEndMinute.setDisplayedValues(minValues);
            npvEndMinute.setMinValue(0);
            npvEndMinute.setMaxValue(maxMinutes);
            npvEndMinute.setValue(endMinute / increment);
        }

        builder.setPositiveButton(R.string.spd_ohf_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int startHourValue = npvStartHour.getValue();
                int startMinuteValue = npvStartMinute.getValue() * increment;
                int endHourValue = npvEndHour.getValue();
                int endMinuteValue = npvEndMinute.getValue() * increment;

                listener.setTimeRange(startHourValue, startMinuteValue, endHourValue, endMinuteValue);
            }
        });
        builder.setNeutralButton(R.string.spd_ohf_cancel, null);

        return builder.create();
    }
}
