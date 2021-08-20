package ch.poole.openinghoursfragment.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class TemplatesDatabaseTest {
    TemplateDatabaseHelper db;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        ApplicationProvider.getApplicationContext().deleteDatabase(TemplateDatabaseHelper.DATABASE_NAME);
        db = new TemplateDatabaseHelper(ApplicationProvider.getApplicationContext());
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        db.close();
        ApplicationProvider.getApplicationContext().deleteDatabase(TemplateDatabaseHelper.DATABASE_NAME);
    }

    /**
     * Query by key test
     */
    @Test
    public void queryByKeyTest() {
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), "opening_hours", null, null)) {
            assertEquals(5, cursor.getCount());
        }
        TemplateDatabase.add(db.getWritableDatabase(), "opening_hours", "test entry", true, "Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed", null,
                null);
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), "opening_hours", null, null)) {
            assertEquals(6, cursor.getCount());
        }
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), "opening1hours", null, null)) {
            assertEquals(6, cursor.getCount());
        }
        TemplateDatabase.add(db.getWritableDatabase(), "opening\\_hours", "test entry", true, "Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed", null,
                null);
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), "opening_hours", null, null)) {
            assertEquals(7, cursor.getCount());
        }
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), "opening1hours", null, null)) {
            assertEquals(6, cursor.getCount());
        }
    }

    /**
     * Query by region test
     */
    @Test
    public void queryByRegionTest() {
        TemplateDatabase.add(db.getWritableDatabase(), null, "test entry", true, "Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed", "CH", null);
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), null, null, null)) {
            assertEquals(7, cursor.getCount());
        }
        TemplateDatabase.add(db.getWritableDatabase(), null, "test entry", true, "Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed", "DE", null);
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), null, null, null)) {
            assertEquals(8, cursor.getCount());
        }
        try (Cursor cursor = TemplateDatabase.queryBy(db.getReadableDatabase(), null, "CH", null)) {
            assertEquals(7, cursor.getCount());
        }
    }

    /**
     * Default test
     */
    @Test
    public void defaultTest() {
        assertEquals("Mo-Fr 09:00-12:00,13:30-18:30;Sa 09:00-17:00;PH closed",
                TemplateDatabase.getDefault(db.getReadableDatabase(), "opening_hours", null, null));
        TemplateDatabase.add(db.getWritableDatabase(), "opening_hours", "test entry", true, "test", null, null);
        assertEquals("test", TemplateDatabase.getDefault(db.getReadableDatabase(), "opening_hours", null, null));
    }
}
