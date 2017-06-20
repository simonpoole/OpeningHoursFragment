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
	
	static final String QUERY_ALL = "SELECT rowid as _id, name, is_default, template FROM templates";
	static final String QUERY_BY_ROWID = "SELECT name, is_default FROM templates WHERE rowid=?";

	/**
	 * Return the default template entry if any
	 * 
	 * @param database	readable template database
	 * @return			teh default template or null if non
	 */
	@Nullable
	public static String getDefault(@NonNull SQLiteDatabase database) {
		String result = null;
		Cursor dbresult = database.query(
				TEMPLATES_TABLE,
				new String[] {NAME_FIELD, DEFAULT_FIELD, TEMPLATE_FIELD},
				DEFAULT_FIELD + " = 1", null , null, null, null);
		dbresult.moveToFirst();
		if (dbresult.getCount() >= 1) {
			result = dbresult.getString(2);
		}
		dbresult.close();
		return result;
	}
	
	/**
	 * Add a new template with the given values to the database
	 * 
	 * @param db			writable template database
	 * @param name			name of the template
	 * @param defaultFlag	true if default
	 * @param template		template itself
	 */
	public static void add(@NonNull SQLiteDatabase db, String name, boolean defaultFlag, String template) {
		if (defaultFlag) { // set all existing is_default values to false
			resetDefaultFlags(db);
		}
		ContentValues values = new ContentValues();
		values.put(NAME_FIELD, name);
		values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
		values.put(TEMPLATE_FIELD, template);
		db.insert(TEMPLATES_TABLE, null, values);	
	}

	/**
	 * Update an existing template entry
	 * 
	 * @param db			writable template database
	 * @param id			rowid of the template
	 * @param name			name of the template
	 * @param defaultFlag	true if default
	 * @param template		template itself
	 */
	static void update(@NonNull SQLiteDatabase db, int id, String name, boolean defaultFlag, String template) {
		if (defaultFlag) { // set all existing is_default values to false
			resetDefaultFlags(db);
		}
		ContentValues values = new ContentValues();
		values.put(NAME_FIELD, name);
		values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
		values.put(TEMPLATE_FIELD, template);
		db.update(TEMPLATES_TABLE, values, "rowid="+id, null);		
	}

	/**
	 * Delete an entry in the template database
	 * 
	 * @param db	writable template database
	 * @param id	rowid of the template
	 */
	static void delete(final SQLiteDatabase db, final int id) {
		db.delete(TEMPLATES_TABLE, "rowid=?", new String[]{Integer.toString(id)});
	}
	
	/**
	 * Set the default flag to false for all entries
	 * 
	 * @param db	writable template database
	 */
	static void resetDefaultFlags(@NonNull SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(DEFAULT_FIELD, 0);
		db.update(TEMPLATES_TABLE, values, null, null);
	}
}
