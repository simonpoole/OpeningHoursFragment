package ch.poole.openinghoursfragment;

/**
 * Interface for listeners that return time values from TimeRangePicker
 * 
 * @author simon
 *
 */
public interface SetTimeRangeListener {
    /**
     * Listener for values from DateRangePicker
     * 
     * @param startHour selected start year
     * @param startMinute selected start day of the month
     * @param endHour selected end year
     * @param endMinute selected end day of the month
     */
    void setTimeRange(int startHour, int startMinute, int endHour, int endMinute);
}
