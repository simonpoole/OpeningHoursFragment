package ch.poole.openinghoursfragment;

import java.io.Serializable;

import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.VarDate;

public interface SetDateRangeListener extends Serializable {
	void setDateRange(int startYear, Month startMonth, int startDay, VarDate startVarDate, int endYear, Month endMonth, int endDay, VarDate endVarDate);
}
