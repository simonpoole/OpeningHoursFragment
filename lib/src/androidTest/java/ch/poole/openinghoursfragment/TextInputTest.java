/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.view.KeyEvent;
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
import androidx.test.uiautomator.Until;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TextInputTest {

    private OpeningHoursFragment fragment;
    private UiDevice             device;
    private Instrumentation instrumentation;

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);

        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_openinghours");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        ValueWithDescription key = new ValueWithDescription("opening_hours", "Opening hours");

        fragment = OpeningHoursFragment.newInstance(key, null, null, null, R.style.Theme_AppCompat_Dialog_Alert, 5, false, null, null);
        fragment.show(fm, "fragment_openinghours");
        UiSelector uiSelector = new UiSelector().resourceIdMatches(".*more.*");
        UiObject fab = device.findObject(uiSelector);
        try {
            fab.click();
            TestUtils.clickText(device, false, fragment.getString(R.string.clear), true);
        } catch (UiObjectNotFoundException e) {
           fail(e.getMessage());
        }
    }

    @Test
    public void twentyfourseven() {
        UiSelector uiSelector = new UiSelector().resourceIdMatches(".*openinghours_string_edit.*");
        UiObject text = device.findObject(uiSelector);
        try {
            text.click();
            device.pressKeyCode(KeyEvent.KEYCODE_2);
            device.pressKeyCode(KeyEvent.KEYCODE_4);
            device.pressKeyCode(KeyEvent.KEYCODE_SLASH);
            device.pressKeyCode(KeyEvent.KEYCODE_7);
            device.pressEnter();
            device.performActionAndWait(()->{}, Until.newWindow(), 1000);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertNotNull(TestUtils.findText(device, false, fragment.getString(R.string.twentyfourseven)));
    }
}