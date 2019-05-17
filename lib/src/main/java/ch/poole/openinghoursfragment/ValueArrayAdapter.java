package ch.poole.openinghoursfragment;

import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ValueArrayAdapter extends ArrayAdapter<ValueWithDescription> {

    final List<ValueWithDescription> values;

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     * @param resource the resource id
     * @param values a List of ValuesWithDescription
     */
    public ValueArrayAdapter(@NonNull Context context, int resource, @NonNull List<ValueWithDescription> values) {
        super(context, resource, values);
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        View v = super.getView(position, convertView, container);
        TextView textView = (TextView) v.findViewById(android.R.id.text1);
        if (textView != null) {
            ValueWithDescription value = values.get(position);
            textView.setText(value.getDescription() != null ? value.getDescription() : value.getValue());
        } else {
            Log.e("ValidatorAdapterView", "position " + position + " view is null");
        }
        return v;
    }
}
