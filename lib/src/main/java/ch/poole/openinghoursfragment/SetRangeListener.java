package ch.poole.openinghoursfragment;

/**
 * Interface for listeners that return date values from RangePicker
 * 
 * @author simon
 *
 */
public interface SetRangeListener {
    /**
     * Listener for values from RangePicker
     * 
     * @param start selected start range value
     * @param end selected end range value
     */
    void setRange(int start, int end);
}
