package ch.poole.openinghoursfragment.templates;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import ch.poole.openinghoursfragment.CancelableDialogFragment;
import ch.poole.openinghoursfragment.R;
import ch.poole.openinghoursfragment.Util;
import ch.poole.openinghoursfragment.ValueWithDescription;

public class TemplateMangementDialog extends CancelableDialogFragment implements UpdateCursorListener {
    public static final String DEBUG_TAG = "TemplateMangementDialog";

    private static final String MANAGE_KEY  = "manage";
    private static final String KEY_KEY     = "key";
    private static final String REGION_KEY  = "region";
    private static final String OBJECT_KEY  = "object";
    private static final String CURRENT_KEY = "current";

    private static final String TAG = "management_fragment";

    private static final int READ_CODE         = 23456;
    private static final int READ_REPLACE_CODE = 34567;
    private static final int WRITE_CODE        = 24679;

    /**
     * Template database related methods and fields
     */
    private Cursor          templateCursor;
    private TemplateAdapter templateAdapter;

    private ValueWithDescription key;
    private String               current;

    private UpdateTextListener updateListener;

    private SQLiteDatabase readableDb;

    /**
     * Show a list of the templates in the database, selection will either load a template or start the edit dialog on
     * it
     * 
     * @param parentFragment the calling fragment
     * @param manage if true the template editor will be started otherwise the template will replace the current OH
     *            value
     * @param key the key to search for templates for in the DB, if null all
     * @param region the region to search for for values
     * @param object an object id, typically an OSM tag, to search for
     * @param currentText the current contents of the OH string
     */
    public static void showDialog(@NonNull Fragment parentFragment, boolean manage, @Nullable ValueWithDescription key, @Nullable String region,
            @Nullable String object, @NonNull String currentText) {
        dismissDialog(parentFragment, TAG);
        FragmentManager fm = parentFragment.getChildFragmentManager();
        TemplateMangementDialog templateDialog = newInstance(manage, key, region, object, currentText);
        templateDialog.show(fm, TAG);
    }

    /**
     * Get a new TemplateMangementDialog instance
     * 
     * @param manage if true the template editor will be started otherwise the template will replace the current OH
     *            value
     * @param key the key to search for templates for in the DB, if null all
     * @param region the region to search for for values
     * @param object an object id, typically an OSM tag, to search for
     * @param currentText the current contents of the OH string
     * @return a TemplateMangementDialog instance
     */
    private static TemplateMangementDialog newInstance(boolean manage, @Nullable ValueWithDescription key, String region, String object,
            @NonNull String currentText) {
        TemplateMangementDialog f = new TemplateMangementDialog();
        Bundle args = new Bundle();
        args.putBoolean(MANAGE_KEY, manage);
        args.putSerializable(KEY_KEY, key);
        args.putString(REGION_KEY, region);
        args.putString(OBJECT_KEY, object);
        args.putString(CURRENT_KEY, currentText);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        final boolean manage = getArguments().getBoolean(MANAGE_KEY);
        key = (ValueWithDescription) getArguments().getSerializable(KEY_KEY);
        String region = getArguments().getString(REGION_KEY);
        String object = getArguments().getString(OBJECT_KEY);
        current = getArguments().getString(CURRENT_KEY);

        updateListener = getParentFragment() instanceof UpdateTextListener ? (UpdateTextListener) getParentFragment() : null;
        if (!manage && updateListener == null) {
            throw new IllegalStateException("parent must implement UpdateTextListener");
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        View templateView = inflater.inflate(R.layout.template_list, null);
        final FloatingActionButton fab = (FloatingActionButton) templateView.findViewById(R.id.more);
        alertDialog.setTitle(manage ? R.string.manage_templates_title : R.string.load_templates_title);
        alertDialog.setView(templateView);
        ListView lv = (ListView) templateView.findViewById(R.id.listView1);
        readableDb = new TemplateDatabaseHelper(getContext()).getReadableDatabase();
        templateCursor = TemplateDatabase.queryBy(readableDb, key == null ? null : key.getValue(), region, object);
        templateAdapter = new TemplateAdapter(getContext(), templateCursor, manage);
        lv.setAdapter(templateAdapter);
        alertDialog.setNegativeButton(R.string.Done, null);

        if (manage || key != null || region != null || object != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(getContext(), fab);
                // menu items
                MenuItem showAll = popup.getMenu().add(R.string.spd_ohf_show_all);
                showAll.setOnMenuItemClickListener(item -> {
                    newCursor(readableDb);
                    return true;
                });

                if (manage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    MenuItem loadTemplate = popup.getMenu().add(R.string.spd_ohf_save_to_file);
                    loadTemplate.setOnMenuItemClickListener(item -> {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.setType("*/*");
                        startActivityForResult(i, WRITE_CODE);
                        return true;
                    });
                    MenuItem saveTemplate = popup.getMenu().add(R.string.spd_ohf_load_from_file_replace);
                    saveTemplate.setOnMenuItemClickListener(item -> {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.setType("*/*");
                        startActivityForResult(i, READ_REPLACE_CODE);
                        return true;
                    });
                    MenuItem manageTemplate = popup.getMenu().add(R.string.spd_ohf_load_from_file);
                    manageTemplate.setOnMenuItemClickListener(item -> {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.setType("*/*");
                        startActivityForResult(i, READ_CODE);
                        return true;
                    });
                }
                popup.show();// showing popup menu
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        return alertDialog.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onDismiss");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(DEBUG_TAG, "onDismiss");
        super.onDismiss(dialog);
        if (templateCursor != null) {
            templateCursor.close();
        }
        if (readableDb != null) {
            readableDb.close();
        }
    }

    private class TemplateAdapter extends CursorAdapter {
        final boolean             manage;
        final Map<String, String> regionsMap = new HashMap<>();

        /**
         * Adapter to the template database
         * 
         * @param context Android Context
         * @param cursor a Cursor with the rows we want to use
         * @param manage if true start the TemplateDialog otherwise select the tempplate
         */
        public TemplateAdapter(@NonNull Context context, @NonNull Cursor cursor, boolean manage) {
            super(context, cursor, 0);
            this.manage = manage;

            // setting up the region spinner is a bit involved as we want to be able to sort it
            final TypedArray values = getResources().obtainTypedArray(R.array.region_values);
            final TypedArray entries = getResources().obtainTypedArray(R.array.region_entries);
            for (int i = 0; i < values.length(); i++) {
                regionsMap.put(values.getString(i), entries.getString(i));
            }
            values.recycle();
            entries.recycle();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            return LayoutInflater.from(getContext()).inflate(R.layout.template_list_item, parent, false);
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            if (!isAdded()) {
                // this seems to be enough to protect against crashes, but doesn't solve the actual issue
                return;
            }
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            boolean isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(TemplateDatabase.DEFAULT_FIELD)) == 1;
            String nameValue = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.NAME_FIELD));
            String keyValue = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.KEY_FIELD));
            String regionValue = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.REGION_FIELD));
            String objectValue = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.OBJECT_FIELD));
            TextView nameView = (TextView) view.findViewById(R.id.name);
            nameView.setText(nameValue);
            TextView metaView = (TextView) view.findViewById(R.id.meta);
            FragmentActivity activity = getActivity();
            String keyEntry = activity.getString(R.string.spd_ohf_meta, valueToEntry(R.array.key_values, R.array.key_entries, keyValue));
            keyEntry = isDefault ? activity.getString(R.string.is_default, keyEntry) : keyEntry;
            keyEntry = objectValue != null ? activity.getString(R.string.spd_ohf_with_object, keyEntry, objectValue) : keyEntry;
            metaView.setText(keyEntry);
            TextView regionView = (TextView) view.findViewById(R.id.region);
            if (regionValue != null) {
                regionView.setVisibility(View.VISIBLE);
                regionView.setText(activity.getString(R.string.spd_ohf_region, regionsMap.get(regionValue)));
            } else {
                regionView.setVisibility(View.GONE);
            }

            final String template = cursor.getString(cursor.getColumnIndexOrThrow(TemplateDatabase.TEMPLATE_FIELD));
            if (manage) {
                view.setOnClickListener(v -> {
                    Integer localId = (Integer) view.getTag();
                    TemplateDialog.showDialog(TemplateMangementDialog.this, current, key, true, localId != null ? localId.intValue() : -1);
                });
            } else {
                view.setOnClickListener(v -> {
                    updateListener.updateText(template);
                    TemplateMangementDialog.this.dismissAllowingStateLoss();
                });
            }
        }

        private String valueToEntry(int valuesId, int entriesId, @Nullable String value) {
            Resources res = getResources();
            final TypedArray values = res.obtainTypedArray(valuesId);
            final TypedArray entries = res.obtainTypedArray(entriesId);
            try {
                for (int i = 0; i < values.length(); i++) {
                    if ((value == null && "".equals(values.getString(i))) || (value != null && value.equals(values.getString(i)))) {
                        return entries.getString(i);
                    }
                }
            } finally {
                values.recycle();
                entries.recycle();
            }
            return "Invalid value";
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            Log.e(DEBUG_TAG, "null data in onActivityResult");
            return;
        }
        Uri uri = data.getData();
        if (uri != null) {
            if (requestCode == WRITE_CODE) {
                try (SQLiteDatabase db = new TemplateDatabaseHelper(getContext()).getReadableDatabase()) {
                    TemplateDatabase.writeJSON(db, getActivity().getContentResolver().openOutputStream(uri));
                } catch (FileNotFoundException e) {
                    Log.e(DEBUG_TAG, "Uri " + uri + " not found for writing");
                }
            } else if (requestCode == READ_CODE || requestCode == READ_REPLACE_CODE) {
                boolean worked = false;
                try (SQLiteDatabase writeableDb = new TemplateDatabaseHelper(getContext()).getWritableDatabase()) {
                    worked = TemplateDatabase.loadJson(writeableDb, getActivity().getContentResolver().openInputStream(uri), requestCode == READ_REPLACE_CODE);
                } catch (FileNotFoundException e) {
                    Log.e(DEBUG_TAG, "Uri " + uri + " not found for reading");
                } finally {
                    newCursor(readableDb);
                }
                if (!worked) {
                    Util.toastTop(getContext(), R.string.spd_ohf_toast_file_read_failure);
                }
            }
        }
    }

    @Override
    public void newCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = TemplateDatabase.queryBy(db, null, null, null);
        Cursor oldCursor = templateAdapter.swapCursor(newCursor);
        oldCursor.close();
        templateAdapter.notifyDataSetChanged();
    }
}
