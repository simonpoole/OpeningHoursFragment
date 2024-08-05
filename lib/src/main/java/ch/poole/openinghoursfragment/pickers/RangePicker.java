package ch.poole.openinghoursfragment.pickers;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import ch.poole.openinghoursfragment.CancelableDialogFragment;
import ch.poole.openinghoursfragment.R;
import ch.poole.android.numberpickerview.library.NumberPickerView;

/**
 * Display a dialog allowing the user to set a numeric range
 *
 */
public class RangePicker extends CancelableDialogFragment {
    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String TITLE         = "title";
    private static final String MIN           = "min";
    private static final String MAX           = "max";
    private static final String START_CURRENT = "startCurrent";
    private static final String END_CURRENT   = "endCurrent";

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
    public static void showDialog(Fragment parentFragment, int title, int min, int max, int startCurrent, int endCurrent) {
        dismissDialog(parentFragment, TAG);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        RangePicker rangePickerFragment = newInstance(title, min, max, startCurrent, endCurrent);
        rangePickerFragment.show(fm, TAG);
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
    private static RangePicker newInstance(int title, int min, int max, int startCurrent, int endCurrent) {
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

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();

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

        builder.setPositiveButton(R.string.spd_ohf_ok, (DialogInterface dialog, int which) -> {
            int endValue = npvEnd.getValue();
            if (endValue == min - 1) {
                endValue = NOTHING_SELECTED;
            }
            listener.setRange(npvStart.getValue(), endValue);
        });
        builder.setNeutralButton(R.string.spd_ohf_cancel, null);

        return builder.create();
    }
}
