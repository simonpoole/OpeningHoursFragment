package ch.poole.openinghoursfragment;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TemplateDatabaseHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG        = "TemplateDatabase";
    private static final String DATABASE_NAME    = "openinghours_templates";
    private static final int    DATABASE_VERSION = 3;

    private final Context context;

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     */
    public TemplateDatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE templates (key TEXT DEFAULT NULL, name TEXT, is_default INTEGER DEFAULT 0, template TEXT DEFAULT '')");
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays_with_lunch), true, "Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays_with_lunch_late_shopping), false,
                    "Mo,Tu,Th,Fr 09:00-12:00,13:30-18:30;We 09:00-12:00,13:30-20:00;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays), false, "Mo-Fr 09:00-18:30;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays_late_shopping), false,
                    "Mo,Tu,Th,Fr 09:00-18:30;We 09:00-20:00;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.twentyfourseven), false, "24/7");
            TemplateDatabase.add(db, "collection_times", context.getString(R.string.collection_times_weekdays), true, "Mo-Fr 09:00; Sa 07:00; PH closed");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion <= 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE templates ADD COLUMN key TEXT DEFAULT NULL");
            TemplateDatabase.add(db, "collection_times", context.getString(R.string.collection_times_weekdays), true, "Mo-Fr 09:00; Sa 07:00; PH closed");
        }
        if (oldVersion <= 2 && newVersion >= 3) {
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays_with_lunch_late_shopping), false,
                    "Mo,Tu,Th,Fr 09:00-12:00,13:30-18:30;We 09:00-12:00,13:30-20:00;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.weekdays_late_shopping), false,
                    "Mo,Tu,Th,Fr 09:00-18:30;We 09:00-20:00;Sa 09:00-17:00;PH closed");
            TemplateDatabase.add(db, null, context.getString(R.string.twentyfourseven), false, "24/7");
        }
    }
}
