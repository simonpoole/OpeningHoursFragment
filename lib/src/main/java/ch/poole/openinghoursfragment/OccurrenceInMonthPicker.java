package ch.poole.openinghoursfragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.YearRange;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

/**
 * Display a dialog allowing the user to select values for a start date and optionally an end date
 *
 */
public class OccurrenceInMonthPicker extends CancelableDialogFragment {
    private static final int MAX_YEAR = 2100;

    public static final int NOTHING_SELECTED = Integer.MIN_VALUE;

    private static final String TITLE      = "title";
    private static final String YEAR       = "startYear";
    private static final String MONTH      = "startMonth";
    private static final String WEEKDAY    = "weekday";
    private static final String OCCURRENCE = "occurrence";

    private static final String TAG = "fragment_occurrenceinmonthpicker";

    /**
     * Show the DateRangePicker dialog
     * 
     * @param parentFragment Fragment calling this
     * @param title resource id for the title to display
     * @param year initial year
     * @param month initial month
     * @param weekday initial weekday
     * @param occurrence initial occurrence
     */
    static void showDialog(Fragment parentFragment, int title, int year, @NonNull Month month, @NonNull WeekDay weekday, int occurrence) {
        dismissDialog(parentFragment);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        OccurrenceInMonthPicker occurrenceInMontPickerFragment = newInstance(title, year, month, weekday, occurrence);
        occurrenceInMontPickerFragment.show(fm, TAG);
    }

    /**
     * Dismiss any instance of this dialog
     * 
     * @param parentFragment the Fragement calling this
     */
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
     * Create a new instance of OccurrenceInMonthPicker
     * 
     * @param title resource id for the title to display
     * @param year initial year
     * @param month initial month
     * @param weekday initial weekday
     * @param occurrence initial occurrence
     * @return an instance of OccurrenceInMonthPicker
     */
    private static OccurrenceInMonthPicker newInstance(int title, int year, @Nullable Month month, @Nullable WeekDay weekday, int occurrence) {
        OccurrenceInMonthPicker f = new OccurrenceInMonthPicker();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(YEAR, year);
        args.putSerializable(MONTH, month);
        args.putSerializable(WEEKDAY, weekday);
        args.putInt(OCCURRENCE, occurrence);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE);

        int year = getArguments().getInt(YEAR);
        Month month = (Month) getArguments().getSerializable(MONTH);
        WeekDay weekday = (WeekDay) getArguments().getSerializable(WEEKDAY);
        int occurrence = getArguments().getInt(OCCURRENCE);

        final SetDateRangeListener listener = (SetDateRangeListener) getParentFragment();

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.occurrence_in_month_picker, null);
        builder.setView(layout);

        String[] yearValues = new String[MAX_YEAR - YearRange.FIRST_VALID_YEAR + 2];
        yearValues[0] = "-";
        for (int i = YearRange.FIRST_VALID_YEAR; i <= MAX_YEAR; i++) {
            yearValues[i - YearRange.FIRST_VALID_YEAR + 1] = Integer.toString(i);
        }
        final NumberPickerView npvYear = (NumberPickerView) layout.findViewById(R.id.year);
        npvYear.setDisplayedValues(yearValues);
        npvYear.setMinValue(YearRange.FIRST_VALID_YEAR - 1);
        npvYear.setMaxValue(MAX_YEAR);
        npvYear.setValue(year != YearRange.UNDEFINED_YEAR ? year : YearRange.FIRST_VALID_YEAR - 1);

        String[] monthEntries = getActivity().getResources().getStringArray(R.array.months_entries);
        String[] weekdayEntries = getActivity().getResources().getStringArray(R.array.weekdays_entries);
        String[] occurrenceValues = new String[10];
        for (int i = -5; i <= 5; i++) {
            occurrenceValues[i > 0 ? i + 4 : i + 5] = "[" + Integer.toString(i) + "]";
        }

        final NumberPickerView npvMonth = (NumberPickerView) layout.findViewById(R.id.month);
        npvMonth.setDisplayedValues(monthEntries);
        npvMonth.setMinValue(1);
        npvMonth.setMaxValue(12);
        npvMonth.setValue(month.ordinal() + 1);

        final NumberPickerView npvWeekday = (NumberPickerView) layout.findViewById(R.id.weekday);
        npvWeekday.setDisplayedValues(weekdayEntries);
        npvWeekday.setMinValue(1);
        npvWeekday.setMaxValue(7);
        npvWeekday.setValue(weekday.ordinal() + 1);

        final NumberPickerView npvOccurrence = (NumberPickerView) layout.findViewById(R.id.occurrence);
        npvOccurrence.setDisplayedValues(occurrenceValues);
        npvOccurrence.setMinValue(0);
        npvOccurrence.setMaxValue(9);
        npvOccurrence.setValue(occurrence < 0 ? occurrence + 5 : occurrence + 4);

        builder.setPositiveButton(R.string.spd_ohf_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int yearValue = getYearValue(npvYear);
                Month monthValue = getMonthValue(npvMonth);
                WeekDay weekday = getWeekdayValue(npvWeekday);
                int occurrenceValue = getOccurrenceValue(npvOccurrence);

                listener.setDateRange(yearValue, monthValue, weekday, occurrenceValue, null, NOTHING_SELECTED, null, null, NOTHING_SELECTED, null);
            }

            private Month getMonthValue(final NumberPickerView npvMonth) {
                Month monthValue = null;
                if (npvMonth.getValue() != 0) {
                    monthValue = Month.values()[npvMonth.getValue() - 1];
                }
                return monthValue;
            }

            private WeekDay getWeekdayValue(final NumberPickerView npvWeekday) {
                WeekDay weekdayValue = null;
                if (npvWeekday.getValue() != 0) {
                    weekdayValue = WeekDay.values()[npvWeekday.getValue() - 1];
                }
                return weekdayValue;
            }

            private int getYearValue(final NumberPickerView npvYear) {
                int yearValue = npvYear.getValue();
                if (yearValue == YearRange.FIRST_VALID_YEAR - 1) {
                    yearValue = NOTHING_SELECTED;
                }
                return yearValue;
            }

            private int getOccurrenceValue(final NumberPickerView npvDay) {
                int occurrence = npvDay.getValue();
                if (occurrence <= 4) {
                    return occurrence - 5;
                }
                return occurrence - 4;
            }
        });
        builder.setNeutralButton(R.string.spd_ohf_cancel, null);

        return builder.create();
    }
}
