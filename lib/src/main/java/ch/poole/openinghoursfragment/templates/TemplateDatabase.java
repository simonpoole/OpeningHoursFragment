package ch.poole.openinghoursfragment.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

/**
 * Access methods for the template database
 * 
 * @author simon
 *
 */
public final class TemplateDatabase {
    private static final String DEBUG_TAG = "TemplateDatabase";

    private static final String TEMPLATES_TABLE = "templates";
    static final String         TEMPLATE_FIELD  = "template";
    static final String         DEFAULT_FIELD   = "is_default";
    static final String         NAME_FIELD      = "name";
    static final String         KEY_FIELD       = "key";
    static final String         REGION_FIELD    = "region";
    static final String         OBJECT_FIELD    = "object";

    static final String QUERY_ALL      = "SELECT rowid as _id, key, name, is_default, template, region, object FROM templates";
    static final String QUERY_BY_ROWID = "SELECT key, name, is_default, template, region, object FROM templates WHERE rowid=?";
    static final String QUERY_BY       = "SELECT rowid as _id, key, name, is_default, template, region, object FROM templates WHERE ";

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
     * @param region the region or null
     * @param object the object or null
     * @return the default template or null if non
     */
    @Nullable
    public static String getDefault(@NonNull SQLiteDatabase database, @Nullable String key, @Nullable String region, @Nullable String object) {
        String result = null;
        List<String> params = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        if (key != null) {
            query.append("(key is NULL OR key=?)");
            params.add(key);
        }
        if (region != null) {
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("(region is NULL OR region=?)");
            params.add(region);
        }
        if (object != null) {
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("(object IS NULL OR object LIKE ?)");
            params.add(object);
        }
        if (query.length() > 0) {
            query.append(" AND ");
        }
        query.append("is_default=1");
        Cursor dbresult = database.rawQuery(TemplateDatabase.QUERY_BY + query.toString(), params.toArray(new String[0]));
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
     * @param key the key or null if we want entries for all keys
     * @param region the region or null if we want all entries for all regions
     * @param object the object or null if we want all entries for all objects
     * @return a Cursor pointing to the first entry
     */
    public static Cursor queryBy(@NonNull SQLiteDatabase database, @Nullable String key, @Nullable String region, @Nullable String object) {
        if (key == null && region == null && object == null) {
            return database.rawQuery(TemplateDatabase.QUERY_ALL, null);
        } else {
            List<String> params = new ArrayList<>();
            StringBuilder query = new StringBuilder();
            List<String> orderCols = new ArrayList<String>();
            if (key != null) {
                query.append("(key is NULL OR key=?)");
                params.add(key);
                orderCols.add("key");
            }
            if (region != null) {
                if (query.length() > 0) {
                    query.append(" AND ");                    
                }
                String[] regionParts = region.split("-");
                if (regionParts.length > 1) { // this will add a check for the country
                    query.append("(region is NULL OR region=? OR region=?)");
                    params.add(region);
                    params.add(regionParts[0]);
                } else {
                    query.append("(region is NULL OR region=?)");
                    params.add(region);   
                }
                orderCols.add("region");
            }
            if (object != null) {
                if (query.length() > 0) {
                    query.append(" AND ");                
                }
                query.append("(object IS NULL OR object LIKE ?)");
                params.add(object);
                orderCols.add("object");
            }
            query.append(" ORDER BY ");
            for (String col:orderCols) {
                query.append("LENGTH(");
                query.append(col);
                query.append(") DESC, ");
            }
            query.append("is_default DESC");
            return database.rawQuery(TemplateDatabase.QUERY_BY + query.toString(), params.toArray(new String[0]));
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
     * @param region region this entry applies to or null
     * @param object object this entry applies to (typically a list of OSM tags) or null
     */
    public static void add(@NonNull SQLiteDatabase db, @Nullable String key, @NonNull String name, boolean defaultFlag, @NonNull String template,
            @Nullable String region, @Nullable String object) {
        if (defaultFlag) { // set all existing is_default values to false
            resetDefaultFlags(db, key);
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(NAME_FIELD, name);
        values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
        values.put(TEMPLATE_FIELD, template);
        values.put(REGION_FIELD, region);
        values.put(OBJECT_FIELD, object);
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
     * @param region region this entry applies to or null
     * @param object object this entry applies to (typically a list of OSM tags) or null
     */
    static void update(@NonNull SQLiteDatabase db, int id, @Nullable String key, @NonNull String name, boolean defaultFlag, @NonNull String template,
            @Nullable String region, @Nullable String object) {
        if (defaultFlag) { // set all existing is_default values to false
            resetDefaultFlags(db, key);
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, key);
        values.put(NAME_FIELD, name);
        values.put(DEFAULT_FIELD, defaultFlag ? 1 : 0);
        values.put(TEMPLATE_FIELD, template);
        values.put(REGION_FIELD, region);
        values.put(OBJECT_FIELD, object);
        db.update(TEMPLATES_TABLE, values, "rowid=" + id, null);
    }

    /**
     * Delete an entry in the template database
     * 
     * @param db writable template database
     * @param id rowid of the template
     */
    static void delete(@NonNull final SQLiteDatabase db, final int id) {
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

    /**
     * Write the contents of the database to an OutputStream
     * 
     * @param database a readable instance of the database
     * @param out the OutputStream
     * @return true if writing was successful
     */
    static boolean writeJSON(@NonNull SQLiteDatabase database, @NonNull OutputStream out) {
        Cursor cursor = queryBy(database, null, null, null);
        int rows = cursor.getCount();
        try (JsonWriter writer = new JsonWriter(new PrintWriter(out))) {
            writer.beginArray();
            if (rows > 0) {
                cursor.moveToFirst();
                rowToJson(writer, cursor);
                while (cursor.moveToNext()) {
                    rowToJson(writer, cursor);
                }
            } else {
                Log.e(DEBUG_TAG, "Template database empty");
            }
            writer.endArray();
            writer.flush();
            Log.i(DEBUG_TAG, "Written " + rows + " templates");
            return true;
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error writing JSON " + e.getMessage());
            return false;
        }
    }

    /**
     * Write a database row to JSON
     * 
     * @param writer a JsonWriter
     * @param cursor the database cursor
     * @throws IOException if writing goes wrong
     */
    private static void rowToJson(@NonNull JsonWriter writer, @NonNull Cursor cursor) throws IOException {
        writer.beginObject();
        writer.name(NAME_FIELD);
        writer.value(cursor.getString(cursor.getColumnIndex(NAME_FIELD)));
        writer.name(DEFAULT_FIELD);
        writer.value(cursor.getInt(cursor.getColumnIndex(DEFAULT_FIELD)));
        writer.name(TEMPLATE_FIELD);
        writer.value(cursor.getString(cursor.getColumnIndex(TEMPLATE_FIELD)));
        writer.name(KEY_FIELD);
        writer.value(cursor.getString(cursor.getColumnIndex(KEY_FIELD)));
        writer.name(REGION_FIELD);
        writer.value(cursor.getString(cursor.getColumnIndex(REGION_FIELD)));
        writer.name(OBJECT_FIELD);
        writer.value(cursor.getString(cursor.getColumnIndex(OBJECT_FIELD)));
        writer.endObject();
    }

    /**
     * Load the contents of a Json InputStream in to the template database
     * 
     * @param database the database instance
     * @param in the InputStream
     * @param replace if true existing content will be replaced
     * @return true if reading and adding was successful
     */
    static boolean loadJson(@NonNull SQLiteDatabase database, @NonNull InputStream in, boolean replace) {
        if (replace) {
            database.execSQL("delete from templates");
        }
        database.beginTransaction();
        int row = 1;
        try (JsonReader reader = new JsonReader(new InputStreamReader(in))) {
            reader.beginArray();
            while (reader.hasNext()) {
                addRowFromJson(database, reader);
                row++;
            }
            reader.endArray();
            database.setTransactionSuccessful();
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(DEBUG_TAG, "Error reading JSON " + e.getMessage() + " row " + row);
            return false;
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Read a row from JSON and add it to the database
     * 
     * @param database the database
     * @param reader a JsonReader
     * @throws IOException if something goes wrong while reading
     */
    private static void addRowFromJson(SQLiteDatabase database, JsonReader reader) throws IOException {
        ContentValues values = new ContentValues();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                continue;
            }
            Log.e(DEBUG_TAG, "reading value");
            switch (name) {
            case KEY_FIELD:
                values.put(KEY_FIELD, reader.nextString());
                break;
            case NAME_FIELD:
                values.put(NAME_FIELD, reader.nextString());
                break;
            case DEFAULT_FIELD:
                values.put(DEFAULT_FIELD, reader.nextInt());
                break;
            case TEMPLATE_FIELD:
                values.put(TEMPLATE_FIELD, reader.nextString());
                break;
            case REGION_FIELD:
                values.put(REGION_FIELD, reader.nextString());
                break;
            case OBJECT_FIELD:
                values.put(OBJECT_FIELD, reader.nextString());
                break;
            default:
                Log.e(DEBUG_TAG, "Unknown field " + name);
            }
        }
        reader.endObject();
        if (database.insert(TEMPLATES_TABLE, null, values) < 0) {
            Log.e(DEBUG_TAG, "Insert failed");
            throw new IOException("Insert of row failed");
        }
        ;
    }
}
