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

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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

        fragment = OpeningHoursFragment.newInstance(key, "AT", null, null, R.style.Theme_AppCompat_Dialog_Alert, 5, true, null, null);
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