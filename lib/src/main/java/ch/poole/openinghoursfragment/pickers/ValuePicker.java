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
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

/**
 * Display a dialog allowing the user to set a single numeric value
 *
 */
public class ValuePicker extends CancelableDialogFragment {
    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String TITLE_KEY   = "title";
    private static final String MIN_KEY     = "min";
    private static final String MAX_KEY     = "max";
    private static final String CURRENT_KEY = "current";

    private static final String TAG = "fragment_valuepicker";

    /**
     * Show the range picker dialog
     * 
     * @param parentFragment Fragment that called this
     * @param title resource id for the title to display
     * @param min minimum range value
     * @param max maximum range value
     * @param current initial value
     */
    public static void showDialog(Fragment parentFragment, int title, int min, int max, int current) {
        dismissDialog(parentFragment, TAG);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        ValuePicker valuePickerFragment = newInstance(title, min, max, current);
        valuePickerFragment.show(fm, TAG);
    }

    /**
     * Create a new instance of RangePicker
     * 
     * @param title resource id for the title to display
     * @param min minimum range value
     * @param max maximum range value
     * @param current initial value
     * @return an instance of ValuePicker
     */
    private static ValuePicker newInstance(int title, int min, int max, int current) {
        ValuePicker f = new ValuePicker();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, title);
        args.putInt(MIN_KEY, min);
        args.putInt(MAX_KEY, max);
        args.putInt(CURRENT_KEY, current);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE_KEY);
        final int min = getArguments().getInt(MIN_KEY);
        final int max = getArguments().getInt(MAX_KEY);
        int current = getArguments().getInt(CURRENT_KEY);
        final SetRangeListener listener = (SetRangeListener) getParentFragment();

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.valuepicker, null);
        builder.setView(layout);

        String[] startValues = new String[max - min + 1];
        for (int i = min; i <= max; i++) {
            startValues[i - min] = Integer.toString(i);
        }
        final NumberPickerView npvStart = (NumberPickerView) layout.findViewById(R.id.start);
        npvStart.setDisplayedValues(startValues);
        npvStart.setMinValue(min);
        npvStart.setMaxValue(max);
        npvStart.setValue(current);

        builder.setPositiveButton(R.string.spd_ohf_ok, (DialogInterface dialog, int which) -> listener.setRange(npvStart.getValue(), -1));
        builder.setNeutralButton(R.string.spd_ohf_cancel, null);

        return builder.create();
    }
}
