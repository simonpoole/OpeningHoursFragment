package ch.poole.openinghoursfragment.templates;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

public interface UpdateCursorListener {

    /**
     * Replace the current cursor for the template database
     * 
     * @param db the template database
     */
    public void newCursor(@NonNull final SQLiteDatabase db);
}
