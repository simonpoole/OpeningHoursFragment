/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.poole.openinghoursfragment;

import java.util.ArrayList;

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
import androidx.test.uiautomator.UiObject2;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MixedModeTest {

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

        ValueWithDescription key = new ValueWithDescription("fee", "Fee");

        ArrayList<ValueWithDescription> values = new ArrayList<>();
        values.add(new ValueWithDescription("yes", "Yes"));
        values.add(new ValueWithDescription("no", "No"));

        fragment = OpeningHoursFragment.newInstance(key, null, null, "no", R.style.Theme_AppCompat_Dialog_Alert, 5, false, values, null);
        fragment.show(fm, "fragment_openinghours");
    }

    @Test
    public void switchMode() {
        UiObject2 buttonText = TestUtils.findText(device, false, "Text values");
        Assert.assertTrue(buttonText.isChecked());
        UiObject2 buttonOH = TestUtils.findText(device, false, "Opening hours");
        buttonOH.click();
        Assert.assertTrue(buttonOH.isChecked());
        buttonText.click();
        UiObject2 textField = TestUtils.findText(device, false, "no");
        textField.click();
        textField.click();
        Assert.assertNotNull(TestUtils.findText(device, false, "Yes"));
    }
}