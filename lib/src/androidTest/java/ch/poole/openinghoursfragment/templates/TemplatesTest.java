/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment.templates;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursfragment.R;
import ch.poole.openinghoursfragment.TestActivity;
import ch.poole.openinghoursfragment.TestUtils;
import ch.poole.openinghoursfragment.ValueWithDescription;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TemplatesTest {

    private OpeningHoursFragment fragment;
    private UiDevice             device;
    FragmentManager              fm;
    TemplateDatabaseHelper       db;
    FragmentActivity             activity;

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        activity = mActivityRule.getActivity();
        fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_openinghours");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        // create the template database
        db = new TemplateDatabaseHelper(activity);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("templates.json");
        TemplateDatabase.loadJson(db.getWritableDatabase(), is, true);
    }

    @Test
    public void manageTemplates() {
        ValueWithDescription key = new ValueWithDescription("opening_hours", "Opening hours");
        fragment = OpeningHoursFragment.newInstance(key, "AT", null, null, R.style.Theme_DialogLight, 5, true, null, null);
        fragment.show(fm, "fragment_openinghours");
        assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        assertNull(TestUtils.findTextContains(device, false, "Switzerland"));
        assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        assertTrue(TestUtils.clickMenuButton("more button", false, true));
        assertTrue(TestUtils.clickText(device, false, "Manage templates", true));
        assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        assertNull(TestUtils.findTextContains(device, false, "Switzerland"));
        assertTrue(TestUtils.clickMenuButton("more button", false, true));
        assertTrue(TestUtils.clickText(device, false, "Show all", true));
        assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        assertNotNull(TestUtils.findTextContains(device, false, "Switzerland"));
        UiObject template = device.findObject(new UiSelector().textContains("Austria"));
        try {
            assertTrue(template.clickAndWaitForNewWindow());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, "Delete", true));
        assertNull(TestUtils.findTextContains(device, false, "Austria"));
    }

    @Test
    public void useDefault() {
        ValueWithDescription key = new ValueWithDescription("opening_hours", "Opening hours");
        fragment = OpeningHoursFragment.newInstance(key, "CH", null, null, R.style.Theme_DialogLight, 5, false, null, null);
        fragment.show(fm, "fragment_openinghours");
        assertNotNull(TestUtils.findTextContains(device, false, "Mo-Fr 09:00-12:00,13:30-18:30; Sa 09:00-17:00; PH closed"));
    }

    @Test
    public void exportImportTemplates() {
        File dir = activity.getExternalCacheDir();
        final File file = new File(dir, "template_out.json");
        try {
            assertTrue(TemplateDatabase.writeJSON(db.getWritableDatabase(), new FileOutputStream(file)));
            TemplateDatabase.loadJson(db.getWritableDatabase(), new FileInputStream(file), true);
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        } finally {
            file.delete();
        }
        manageTemplates();
    }
}