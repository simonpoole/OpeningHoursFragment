package ch.poole.openinghoursfragment;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

public class Util {
	protected static final String DEBUG_TAG = "Util";

	/**
	 * Scroll to the supplied view 
	 * 
	 * This code is from vespucci and licensed under the Apache 2.0 licence
	 * 
	 * @author simon
	 * 
	 * @param sv 	the ScrollView or NestedScrollView to scroll
	 * @param row 	the row to display, if null scroll to top or bottom of sv
	 * @param up	if true scroll to top if row is null, otherwise scroll to bottom
	 * @param force	if true always try to scroll even if row is already on screen
	 */
	public static void scrollToRow(final View sv, final View row, final boolean up, boolean force) {	
		Rect scrollBounds = new Rect();
		sv.getHitRect(scrollBounds);
		if (row != null && row.getLocalVisibleRect(scrollBounds)&& !force) {
			return; // already on screen
		} 
		if (row==null) {
			sv.post(new Runnable() {
				@Override
				public void run() {
					if (sv != null && sv instanceof ScrollView) { 
						((ScrollView)sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
					} else if (sv != null && sv instanceof NestedScrollView) { 
						((NestedScrollView)sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
					} else {
						Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
					}
				}
			});
		} else {
			sv.post(new Runnable() {
				@SuppressLint("NewApi")
				@Override
				public void run() {
					final int target = up ? row.getTop(): row.getBottom();
					if (sv != null && sv instanceof ScrollView) { 
						((ScrollView)sv).smoothScrollTo(0, target);
					} else if (sv != null && sv instanceof NestedScrollView) {
						((NestedScrollView)sv).smoothScrollTo(0, target);
					} else {
						Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
					}
				}
			});
		}
	}
}
