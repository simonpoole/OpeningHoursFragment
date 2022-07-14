/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.Fragment;
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

        fragment = OpeningHoursFragment.newInstance(key, null, null, null, R.style.Theme_AppCompat_Dialog_Alert, 5, true, null, null);
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
    public void addDaterange1() {
        clickAddDateRange();
        Assert.assertTrue(TestUtils.clickText(device, false, "Date - date", true));
        UiSelector uiSelector = new UiSelector().resourceIdMatches(".*startMonth.*").instance(0);
        UiObject date = device.findObject(uiSelector);
        try {
            date.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        uiSelector = new UiSelector().resourceIdMatches(".*startMonth.*");
        UiObject months = device.findObject(uiSelector);
        try {
            months.clickBottomRight(); // should select Mar
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "OK", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mar Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addDaterange2() {
        clickAddDateRange();
        Assert.assertTrue(TestUtils.clickText(device, false, "Variable date - date", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("easter Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addDaterange3() {
        clickAddDateRange();
        Assert.assertTrue(TestUtils.clickText(device, false, "Occurrence in month - occurrence", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Jan Mo[1]-Feb Mo[1] Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addDaterange4() {
        clickAddDateRange();
        TestUtils.scrollTo("With offsets");
        Assert.assertTrue(TestUtils.clickText(device, false, "With offsets", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Variable date - occurrence", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("easter+Mo-Jan Mo[1] Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addDaterange5() {
        clickAddDateRange();
        TestUtils.scrollTo("With offsets");
        Assert.assertTrue(TestUtils.clickText(device, false, "With offsets", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Date - variable date", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Jan 1+Mo-easter Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addDaterange6() {
        clickAddDateRange();
        TestUtils.scrollTo("With offsets");
        Assert.assertTrue(TestUtils.clickText(device, false, "With offsets", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Variable date - variable date", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("easter+Mo-easter Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    /**
     * 
     */
    private void clickAddDateRange() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Add date range", true));
    }

    public void occurranceInMonth() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        TestUtils.clickOverflowButton();
        UiSelector uiSelector = new UiSelector().description("More options").instance(1);
        UiObject overflowButton = device.findObject(uiSelector);
        try {
            overflowButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "Add occurrence", true));
        clickCheckBox(10);
        Assert.assertEquals("Sa[3] 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addWeekRange() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Add week range", true));
        UiSelector uiSelector = new UiSelector().description("More options").instance(1);
        UiObject overflowButton = device.findObject(uiSelector);
        try {
            overflowButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "Show interval", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("week 01 Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addYearRange() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete", true));
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, fragment.getString(R.string.spd_ohf_year_range_menu), true));
        Assert.assertTrue(TestUtils.clickTextExact(device, false, fragment.getString(R.string.add_year_range), true));
        UiSelector uiSelector = new UiSelector().description("More options").instance(1);
        UiObject overflowButton = device.findObject(uiSelector);
        try {
            overflowButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "Show interval", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals(Integer.toString(1900 + new Date().getYear()) + " Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void timerange() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        UiSelector uiSelector = new UiSelector().resourceIdMatches(".*timebar.*").instance(0);
        UiObject bar = device.findObject(uiSelector);
        try {
            bar.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        uiSelector = new UiSelector().resourceIdMatches(".*startHour.*");
        UiObject hours = device.findObject(uiSelector);
        try {
            hours.clickTopLeft(); // should select 7
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, false, "OK", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr 07:00-12:00,13:30-18:30; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    /**
     * Click via instance count as using the res id doesn't seem to work
     *
     * @param instance the 0 based occurrence of the checkbox
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

    @Test
    public void addTimeRange1() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickText(device, false, "Time - extended time", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr 00:00-48:00; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addTimeRange2() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickText(device, false, "Var. time - time", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr dawn-24:00; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    /**
     * 
     */
    private void clickAddTimeSpan() {
        TestUtils.clickOverflowButton();
        Assert.assertTrue(TestUtils.clickText(device, false, "Add time span", true));
    }

    @Test
    public void addTimeRange3() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickText(device, false, "Var. time - var. time", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr dawn-dusk; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addTimeRange4() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickText(device, false, "Time-open end", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr 06:00+; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addTimeRange5() {
        deleteTimeBars();
        clickAddTimeSpan();
        String text = fragment.getString(R.string.variable_time_open_end);
        TestUtils.scrollTo(text);
        Assert.assertTrue(TestUtils.clickText(device, false, text, true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr dawn+; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addTimeRange6() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickText(device, false, "Time - var. time", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr 06:00-dusk; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    @Test
    public void addTimeRange7() {
        deleteTimeBars();
        clickAddTimeSpan();
        Assert.assertTrue(TestUtils.clickTextExact(device, false, "Time", true));
        Assert.assertTrue(TestUtils.clickText(device, false, "Save", true));
        Assert.assertEquals("Mo-Fr 06:00; Sa 09:00-17:00; PH closed", mActivityRule.getActivity().getResult());
    }

    /**
     * 
     */
    private void deleteTimeBars() {
        Assert.assertTrue(TestUtils.clickText(device, false, "Weekdays", true));
        UiSelector uiSelector = new UiSelector().resourceIdMatches(".*timebar.*").instance(0);
        UiObject overflowButton = device.findObject(uiSelector.fromParent(new UiSelector().description("More options")));
        try {
            overflowButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        TestUtils.clickText(device, false, "Delete", true);
        overflowButton = device.findObject(uiSelector.fromParent(new UiSelector().description("More options")));
        try {
            overflowButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException ex) {
            Assert.fail(ex.getMessage());
        }
        TestUtils.clickText(device, false, "Delete", true);
    }
}