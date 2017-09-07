package ch.poole.openinghoursfragment;

import ch.poole.openinghoursparser.DateRange;

/**
 * This is used to reduce code duplication when adding menu items for the various date range variants
 * 
 * @author simon
 *
 */
interface DateMenuInterface {
    /**
     * Add the actual dates that you want included in the config for the menu item
     * 
     * @param dateRange the DateRange to add the dates to
     */
    void addDates(DateRange dateRange);
}
