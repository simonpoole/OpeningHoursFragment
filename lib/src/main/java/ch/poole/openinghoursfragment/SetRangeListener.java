package ch.poole.openinghoursfragment;

import java.io.Serializable;

public interface SetRangeListener extends Serializable {
	void setRange(int start, int end);
}
