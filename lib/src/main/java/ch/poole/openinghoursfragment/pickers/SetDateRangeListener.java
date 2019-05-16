package ch.poole.openinghoursfragment.pickers;

import android.support.annotation.Nullable;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.VarDate;
import ch.poole.openinghoursparser.WeekDay;

/**
 * Interface for listeners that return date values from DateRangePicker
 * 
 * @author simon
 *
 */
public interface SetDateRangeListener {
    /**
     * Listener for values from DateRangePicker
     * 
     * @param startYear selected start year
     * @param startMonth selected start month or null
     * @param startWeekday selected start weekday or null
     * @param startDay selected start day of the month or occurrence if startWeekday isn't null
     * @param startVarDate selected start variable date ie easter or null
     * @param endYear selected end year
     * @param endMonth selected end month or null
     * @param endWeekday selected end weekday or null
     * @param endDay selected end day of the month or occurrence if endWeekday isn't null
     * @param endVarDate selected end variable date ie easter or null
     */
    void setDateRange(int startYear, @Nullable Month startMonth, @Nullable WeekDay startWeekday, int startDay, @Nullable VarDate startVarDate, int endYear,
            @Nullable Month endMonth, @Nullable WeekDay endWeekday, int endDay, @Nullable VarDate endVarDate);
}
