/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment.templates;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_openinghours");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        // create the template database
        TemplateDatabaseHelper db = new TemplateDatabaseHelper(mActivityRule.getActivity());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("templates.json");
        TemplateDatabase.loadJson(db.getWritableDatabase(), is, true);

        ValueWithDescription key = new ValueWithDescription("opening_hours", "Opening hours");

        fragment = OpeningHoursFragment.newInstance(key, "AT", null, null, R.style.Theme_AppCompat_Dialog_Alert, 5, true, null);
        fragment.show(fm, "fragment_openinghours");
    }

    @Test
    public void manageTemplates() {
        Assert.assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        Assert.assertNull(TestUtils.findTextContains(device, false, "Switzerland"));
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        Assert.assertTrue(TestUtils.clickMenuButton("more button", false, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Manage templates", true));
        Assert.assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        Assert.assertNull(TestUtils.findTextContains(device, false, "Switzerland"));
        Assert.assertTrue(TestUtils.clickMenuButton("more button", false, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Show all", true));
        Assert.assertNotNull(TestUtils.findTextContains(device, false, "Austria"));
        Assert.assertNotNull(TestUtils.findTextContains(device, false, "Switzerland"));
        UiObject template = device.findObject(new UiSelector().textContains("Austria"));
        try {
            Assert.assertTrue(template.clickAndWaitForNewWindow());
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        Assert.assertNull(TestUtils.findTextContains(device, false, "Austria"));
    }
}