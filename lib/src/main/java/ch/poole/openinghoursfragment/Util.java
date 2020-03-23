package ch.poole.openinghoursfragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class Util {
    protected static final String DEBUG_TAG = "Util";

    /**
     * Private default constructor
     */
    private Util() {
        // Empty
    }

    /**
     * Scroll to the supplied view
     * 
     * This code is from vespucci and licensed under the Apache 2.0 licence
     * 
     * @author simon
     * 
     * @param sv the ScrollView or NestedScrollView to scroll
     * @param row the row to display, if null scroll to top or bottom of sv
     * @param up if true scroll to top if row is null, otherwise scroll to bottom
     * @param force if true always try to scroll even if row is already on screen
     */
    public static void scrollToRow(@NonNull final View sv, @Nullable final View row, final boolean up, boolean force) {
        Rect scrollBounds = new Rect();
        sv.getHitRect(scrollBounds);
        if (row != null && row.getLocalVisibleRect(scrollBounds) && !force) {
            return; // already on screen
        }
        if (row == null) {
            sv.post(new Runnable() {
                @Override
                public void run() {
                    if (sv instanceof ScrollView) {
                        ((ScrollView) sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
                    } else if (sv instanceof NestedScrollView) {
                        ((NestedScrollView) sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
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
                    final int target = up ? row.getTop() : row.getBottom();
                    if (sv instanceof ScrollView) {
                        ((ScrollView) sv).smoothScrollTo(0, target);
                    } else if (sv instanceof NestedScrollView) {
                        ((NestedScrollView) sv).smoothScrollTo(0, target);
                    } else {
                        Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
                    }
                }
            });
        }
    }

    /**
     * Display a toast at the top of the screen
     * 
     * @param context android context
     * @param msg the message to display
     */
    public static void toastTop(@NonNull Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Display a toast at the top of the screen
     * 
     * @param context android context
     * @param msg the message resource id to display
     */
    public static void toastTop(@NonNull Context context, int msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Set initially selected value on a spinner
     * 
     * @param res the Resources
     * @param valuesId the value array resource id
     * @param spinner the Spinner
     * @param value the value to set
     */
    public static void setSpinnerInitialEntryValue(@NonNull Resources res, int valuesId, @NonNull Spinner spinner, @Nullable String value) {
        final TypedArray values = res.obtainTypedArray(valuesId);
        for (int i = 0; i < values.length(); i++) {
            if ((value == null && "".equals(values.getString(i))) || (value != null && value.equals(values.getString(i)))) {
                spinner.setSelection(i);
                break;
            }
        }
        values.recycle();
    }
}
