package ch.poole.openinghoursfragment;

import java.io.Serializable;

/**
 * Interface for listeners that return date values from RangePicker
 * 
 * @author simon
 *
 */
public interface SetRangeListener extends Serializable {
	/**
	 * Listener for values from RangePicker
	 * 
	 * @param start	selected start range value
	 * @param end	selected end range value
	 */
	void setRange(int start, int end);
}
