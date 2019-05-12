package ch.poole.openinghoursfragment;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Access methods for the template database
 * 
 * @author simon
 *
 */
class TemplateDatabase {
    private static final String TEMPLATES_TABLE = "templates";
    static final String TEMPLATE_FIELD = "template";
    static final String DEFAULT_FIELD = "is_default";
    static final String NAME_FIELD = "name";
    static final String KEY_FIELD = "key";

    static final String QUERY_ALL = "SELECT rowid as _id, key, name, is_default, template FROM templates";
    static final String QUERY_BY_ROWID = "SELECT key, name, is_default, template FROM templates WHERE rowid=?";
    static final String QUERY_BY_KEY = "SELECT rowid as _id, key, name, is_default, template FROM templates WHERE key is NULL OR key=? ORDER BY key DESC";

    /**
     * Private default constructor
     */
    private TemplateDatabase() {
        // Empty
    }
    
    /**
     * Return the default template entry if any
     * 
     * @param database readable template database
     * @param key key we are looking for the default for
     * @return the default template or null if non
     */
    @Nullable
    public static String getDefault(@NonNull SQLiteDatabase database, @Nullable String key) {
        String result = null;
        Cursor dbresult = database.query(TEMPLATES_TABLE, new String[] { TEMPLATE_FIELD },
                DEFAULT_FIELD + " = 1 AND " + KEY_FIELD + (key==null ? " is NULL" : " = '" + key + "'") , null, null, null, null);
        dbresult.moveToFirst();
        if (dbresult.getCount() >= 1) {
            result = dbresult.getString(0);
        }
        dbresult.close();
        return result;
    }

    /**
     * Get all entries for a specific key plus all which apply to any key
     * 
     * @param database the database to query
     * @param key the key or null if we want all entries
     * @return a Cursor pointing to the first entry
     */
    public static Cursor queryByKey(@NonNull SQLiteDatabase database, @Nullable String key) {
        if (key == null) {
            return database.rawQuery(TemplateDatabase.QUERY_ALL, null);
        } else {
            return database.rawQuery(TemplateDatabase.QUERY_BY_KEY, new String[] { key });
        }
    }
    
    /**
     * Add a new template with the given values to the database
     * 
     * @param db writable template database
     * @param key key the template should be used for, or null if applicable for all
     * @param name name of the template
     * @param defaultFlag true if default
     * @param template template itself
     */
    public static void add(@NonNull SQLiteDatabase db, @Nullable String key, @NonNull String name, boolean defaultFlag, @NonNull String template) {
        if (defaultFlag) { // set all existing is_default values to false
            resetDefaultFlags(db, key);
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(NAME_FIELD, name);
        values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
        values.put(TEMPLATE_FIELD, template);
        db.insert(TEMPLATES_TABLE, null, values);
    }

    /**
     * Update an existing template entry
     * 
     * @param db writable template database
     * @param id rowid of the template
     * @param key key the template should be used for, or null if applicable for all
     * @param name name of the template
     * @param defaultFlag true if default
     * @param template template itself
     */
    static void update(@NonNull SQLiteDatabase db, int id, @Nullable String key, @NonNull String name, boolean defaultFlag, @NonNull String template) {
        if (defaultFlag) { // set all existing is_default values to false
            resetDefaultFlags(db, key);
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(NAME_FIELD, name);
        values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
        values.put(TEMPLATE_FIELD, template);
        db.update(TEMPLATES_TABLE, values, "rowid=" + id, null);
    }

    /**
     * Delete an entry in the template database
     * 
     * @param db writable template database
     * @param id rowid of the template
     */
    static void delete(final SQLiteDatabase db, final int id) {
        db.delete(TEMPLATES_TABLE, "rowid=?", new String[] { Integer.toString(id) });
    }

    /**
     * Set the default flag to false for all entries for a specific key
     * 
     * @param key key the template should be used for, or null if applicable for all
     * @param db writable template database
     * 
     */
    static void resetDefaultFlags(@NonNull SQLiteDatabase db, @Nullable String key) {
        ContentValues values = new ContentValues();
        values.put(DEFAULT_FIELD, 0);
        db.update(TEMPLATES_TABLE, values, "key = '" + key + "'", null);
    }
}
