/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment;

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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FragmentTest {

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

        ValueWithDescription key = new ValueWithDescription("collection_times", "Collection times");

        fragment = OpeningHoursFragment.newInstance(key, null, null, null, R.style.Theme_AppCompat_Dialog_Alert, 5, true, null);
        fragment.show(fm, "fragment_openinghours");
    }

    @Test
    public void weekdays() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));

        clickCheckBox(3);
        clickCheckBox(6);

        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-We,Fr,Su 09:00-12:00,13:30-18:30; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void weekdays2() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Add date range", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Date - date", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Jan Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    /**
     * Click via instance count as using the res id doesn't seem to work
     *
     * @param instance the o based occurance of the checkbox
     */
    private void clickCheckBox(int instance) {
        UiSelector uiSelector = new UiSelector().className("android.widget.CheckBox").instance(instance);
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}