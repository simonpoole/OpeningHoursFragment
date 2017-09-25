package ch.poole.openinghoursfragment;

import android.support.annotation.Nullable;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.VarDate;

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
     * @param startDay selected start day of the month
     * @param startVarDate selected start variable date ie easter or null
     * @param endYear selected end year
     * @param endMonth selected end month or null
     * @param endDay selected end day of the month
     * @param endVarDate selected end variable date ie easter or null
     */
    void setDateRange(int startYear, @Nullable Month startMonth, int startDay, @Nullable VarDate startVarDate,
            int endYear, @Nullable Month endMonth, int endDay, @Nullable VarDate endVarDate);
}
