package ch.poole.openinghoursfragment;

/**
 * Interface for listeners to return the constructed openinghous value
 * 
 * @author simon
 *
 */
public interface OnSaveListener {
	/**
	 * Save the value
	 * 
	 * @param key	the OSM key (for example opening_hours or service_times
	 * @param value	the constructed value
	 */
	void save(String key, String value); 
}
