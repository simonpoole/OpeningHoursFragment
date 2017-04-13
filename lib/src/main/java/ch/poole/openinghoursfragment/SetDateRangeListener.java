package ch.poole.openinghoursfragment;

import java.io.Serializable;

import ch.poole.openinghoursparser.Month;

public interface SetDateRangeListener extends Serializable {
	void setDateRange(int startYear, Month startMonth, int startDay, int endYear, Month endMonth, int endDay);
}
