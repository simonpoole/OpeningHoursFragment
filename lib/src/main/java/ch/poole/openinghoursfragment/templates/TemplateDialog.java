package ch.poole.openinghoursfragment.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import ch.poole.openinghoursfragment.CancelableDialogFragment;
import ch.poole.openinghoursfragment.R;
import ch.poole.openinghoursfragment.Util;
import ch.poole.openinghoursfragment.ValueArrayAdapter;
import ch.poole.openinghoursfragment.ValueWithDescription;

public class TemplateDialog extends CancelableDialogFragment {

    private static final String DEBUG_TAG = "TemplateDialog";

    private static final String EXISTING_KEY = "existing";
    private static final String ID_KEY       = "id";
    private static final String CURRENT_KEY  = "current";
    private static final String KEY_KEY      = "key";

    private static final String TAG = "templatedialog_fragment";

    /**
     * Show a dialog for editing and saving a template
     * 
     * @param parentFragment the calling fragment
     * @param current the current opening hours string
     * @param key the key current is for
     * @param existing true if this is not a new template
     * @param id the rowid of the template in the database or -1 if not saved yet
     */
    public static void showDialog(@NonNull Fragment parentFragment, @NonNull final String current, @NonNull final ValueWithDescription key,
            final boolean existing, final int id) {
        dismissDialog(parentFragment, TAG);

        FragmentManager fm = parentFragment.getChildFragmentManager();
        TemplateDialog templateDialog = newInstance(current, key, existing, id);
        templateDialog.show(fm, TAG);
    }

    /**
     * Create a new instance of the dialog
     * 
     * @param current the current opening hours string
     * @param key the key current is for
     * @param existing true if this is not a new template
     * @param id the rowid of the template in the database or -1 if not saved yet
     * @return a TemplateDialog instance
     */
    private static TemplateDialog newInstance(@NonNull final String current, @Nullable final ValueWithDescription key, final boolean existing, final int id) {
        TemplateDialog f = new TemplateDialog();
        Bundle args = new Bundle();
        args.putString(CURRENT_KEY, current);
        args.putSerializable(KEY_KEY, key);
        args.putBoolean(EXISTING_KEY, existing);
        args.putInt(ID_KEY, id);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final SQLiteDatabase db = new TemplateDatabaseHelper(getContext()).getWritableDatabase();

        final UpdateCursorListener updateListener = getParentFragment() instanceof UpdateCursorListener ? (UpdateCursorListener) getParentFragment() : null;
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        View templateView = inflater.inflate(R.layout.template_item, null);
        alertDialog.setView(templateView);
        final CheckBox defaultCheck = (CheckBox) templateView.findViewById(R.id.is_default);
        final EditText nameEdit = (EditText) templateView.findViewById(R.id.template_name);
        final Spinner keySpinner = (Spinner) templateView.findViewById(R.id.template_key);
        final EditText custom = (EditText) templateView.findViewById(R.id.template_custom_key);
        final Spinner regionSpinner = (Spinner) templateView.findViewById(R.id.template_region);
        final EditText objectEdit = (EditText) templateView.findViewById(R.id.template_object);

        final boolean existing = getArguments().getBoolean(EXISTING_KEY);
        final int id = getArguments().getInt(ID_KEY);
        final String current = getArguments().getString(CURRENT_KEY);
        final ValueWithDescription key = (ValueWithDescription) getArguments().getSerializable(KEY_KEY);

        String template = null;
        String templateKey = key != null ? key.getValue() : null;
        String templateRegion = null;
        String templateObject = null;
        if (existing) {
            try (Cursor cursor = db.rawQuery(TemplateDatabase.QUERY_BY_ROWID, new String[] { Integer.toString(id) })) {
                if (cursor.moveToFirst()) {
                    boolean isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(TemplateDatabase.DEFAULT_FIELD)) == 1;
                    defaultCheck.setChecked(isDefault);
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.NAME_FIELD));
                    nameEdit.setText(name);
                    template = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.TEMPLATE_FIELD));
                    templateKey = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.KEY_FIELD));
                    templateRegion = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.REGION_FIELD));
                    templateObject = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.OBJECT_FIELD));
                } else {
                    Log.e(DEBUG_TAG, "template id " + Integer.toString(id) + " not found");
                }
            }

            alertDialog.setTitle(R.string.edit_template);
            alertDialog.setNeutralButton(R.string.Delete, (dialog, which) -> {
                Log.d(DEBUG_TAG, "deleting template " + Integer.toString(id));
                TemplateDatabase.delete(db, id);
                if (updateListener != null) {
                    updateListener.newCursor(db);
                }
            });
        } else {
            alertDialog.setTitle(R.string.save_template);
        }
        alertDialog.setNegativeButton(R.string.spd_ohf_cancel, null);

        keySpinner.setSelection(0);
        Util.setSpinnerInitialEntryValue(getResources(), R.array.key_values, keySpinner, templateKey);
        // hack for custom keys -- requires that the custom key resource is last
        final TypedArray keyValues = getContext().getResources().obtainTypedArray(R.array.key_values);
        final int customKeyPos = keyValues.length() - 1;
        if (templateKey != null && keySpinner.getSelectedItemPosition() == 0) {
            keySpinner.setSelection(customKeyPos);
            custom.setText(templateKey);
        }

        keySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                custom.setVisibility(position == customKeyPos ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        // setting up the region spinner is a bit involved as we want to be able to sort it
        final TypedArray values = getResources().obtainTypedArray(R.array.region_values);
        final TypedArray entries = getResources().obtainTypedArray(R.array.region_entries);
        Set<ValueWithDescription> regionsSet = new TreeSet<>((v1, v2) -> v1.getDescription().compareTo(v2.getDescription()));
        for (int i = 0; i < values.length(); i++) {
            regionsSet.add(new ValueWithDescription(values.getString(i), entries.getString(i)));
        }
        values.recycle();
        entries.recycle();
        List<ValueWithDescription> regions = new ArrayList<>(regionsSet);
        regions.add(0, new ValueWithDescription("", getString(R.string.spd_ohf_any)));
        ValueArrayAdapter adapter = new ValueArrayAdapter(getContext(), android.R.layout.simple_spinner_item, regions);
        regionSpinner.setAdapter(adapter);
        regionSpinner.setSelection(0);
        if (templateRegion != null) {
            for (int i = 0; i < regions.size(); i++) {
                if (templateRegion.equals(regions.get(i).getValue())) {
                    regionSpinner.setSelection(i);
                    break;
                }
            }
        }

        objectEdit.setText(templateObject == null ? "" : templateObject);

        final String finalTemplate = template;
        alertDialog.setPositiveButton(R.string.Save, (dialog, which) -> {
            int spinnerPos = keySpinner.getSelectedItemPosition();
            final String spinnerKey = spinnerPos == 0 ? null : spinnerPos == customKeyPos ? custom.getText().toString() : keyValues.getString(spinnerPos);
            keyValues.recycle();
            spinnerPos = regionSpinner.getSelectedItemPosition();
            final String spinnerRegion = spinnerPos == 0 ? null : regions.get(spinnerPos).getValue();
            final String object = objectEdit.length() == 0 ? null : objectEdit.getText().toString();
            if (!existing) {
                TemplateDatabase.add(db, spinnerKey, nameEdit.getText().toString(), defaultCheck.isChecked(), current, spinnerRegion, object);
                db.close();
            } else {
                if (!current.equals(finalTemplate)) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                    alert.setTitle(R.string.update_template);
                    alert.setPositiveButton(R.string.Yes, (d, w) -> {
                        TemplateDatabase.update(db, id, spinnerKey, nameEdit.getText().toString(), defaultCheck.isChecked(), current, spinnerRegion, object);
                        if (updateListener != null) {
                            updateListener.newCursor(db);
                        }
                    });
                    alert.setNegativeButton(R.string.No, (d, w) -> {
                        TemplateDatabase.update(db, id, spinnerKey, nameEdit.getText().toString(), defaultCheck.isChecked(), finalTemplate, spinnerRegion,
                                object);
                        if (updateListener != null) {
                            updateListener.newCursor(db);
                        }
                    });
                    alert.show();
                } else {
                    TemplateDatabase.update(db, id, spinnerKey, nameEdit.getText().toString(), defaultCheck.isChecked(), current, null, null);
                    if (updateListener != null) {
                        updateListener.newCursor(db);
                    }
                }
            }
        });

        return alertDialog.create();
    }
}
