package ch.poole.openinghoursfragment;

import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.util.Log;

import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Various methods to support testing
 * 
 * @author Simon Poole
 *
 */
public class TestUtils {
    private static final String DEBUG_TAG = "TestUtils";

    /**
     * Grant permissions by clicking on the dialogs, currently only works for English and German
     */
    public static void grantPermissons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            boolean notdone = true;
            while (notdone) {
                notdone = clickText(mDevice, true, "allow", true) || clickText(mDevice, true, "zulassen", false);
            }
        }
    }

    /**
     * Click the overflow button in a menu bar
     * 
     * @return true if successful
     */
    public static boolean clickOverflowButton() {
        return clickMenuButton("More options", false, true);
    }

    /**
     * Click a menu bar button
     * 
     * @param description the description of the button
     * @param longClick if true perform a long click
     * @param waitForNewWindow if true wait for a new window
     * @return true if successful
     */
    public static boolean clickMenuButton(String description, boolean longClick, boolean waitForNewWindow) {
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.clickable(true).descStartsWith(description);
        UiSelector uiSelector = new UiSelector().clickable(true).descriptionStartsWith(description);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (longClick) {
                    button.longClick();
                } else if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                }
                return true; // the button clicks don't seem to reliably return a true
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Click on a button
     * 
     * @param resId resource id
     * @param waitForNewWindow if true wait for a new window after clicking
     * @return true if the button was found and clicked
     * @throws UiObjectNotFoundException
     */
    public static boolean clickButton(String resId, boolean waitForNewWindow) {
        try {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiSelector uiSelector = new UiSelector().clickable(true).resourceId(resId);
            UiObject button = device.findObject(uiSelector);
            if (waitForNewWindow) {
                return button.clickAndWaitForNewWindow();
            } else {
                return button.click();
            }
        } catch (UiObjectNotFoundException e) {
            System.out.println(e.getMessage() + " " + resId);
            return false;
        }
    }

    /**
     * Single click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void clickAt(float x, float y) {
        System.out.println("clicking at " + x + " " + y);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click((int) x, (int) y);
    }

    /**
     * An attempt at getting reliable long clicks with swiping
     * 
     * @param mDevice the current UiDevice
     * @param o the UiObject to long click on
     * @throws UiObjectNotFoundException if o is not found
     */
    public static void longClick(UiDevice mDevice, UiObject o) throws UiObjectNotFoundException {
        Rect rect = o.getBounds();
        mDevice.swipe(rect.centerX(), rect.centerY(), rect.centerX(), rect.centerY(), 200);
        try {
            Thread.sleep(2000); // NOSONAR
        } catch (InterruptedException e1) {
        }
    }

    /**
     * Long click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void longClickAt(float x, float y) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.swipe((int) x, (int) y, (int) x, (int) y, 200);
    }

    /**
     * Double click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void doubleClickAt(float x, float y) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click((int) x, (int) y);
        try {
            Thread.sleep(100); // NOSONAR
        } catch (InterruptedException e) {
        }
        device.click((int) x, (int) y);
    }

    /**
     * Execute a drag
     * 
     * @param startX start screen X coordinate
     * @param startY start screen Y coordinate
     * @param endX end screen X coordinate
     * @param endY end screen Y coordinate
     * @param steps number of 5ms steps
     */
    public static void drag(float startX, float startY, float endX, float endY, int steps) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.swipe((int) startX, (int) startY, (int) endX, (int) endY, steps);
    }

    /**
     * Click a text on screen (case insensitive, start of a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickText(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
            uiSelector = new UiSelector().clickable(true).textStartsWith(text);
        } else {
            bySelector = By.textStartsWith(text);
            uiSelector = new UiSelector().textStartsWith(text);
        }
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                    Log.e(DEBUG_TAG, ".... clicked");
                }
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Long click a text on screen (case insensitive, start of a string)
     * 
     * @param device UiDevice object
     * @param text text to search (case insensitive, uses textStartsWith)
     * @return true if successful
     */
    public static boolean longClickText(UiDevice device, String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.textStartsWith(text);
        UiSelector uiSelector = new UiSelector().textStartsWith(text);
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                longClick(device, button);
                Log.e(DEBUG_TAG, ".... clicked");
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Click a text on screen (case sensitive, any position in a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case sensitive, uses textContains)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickTextContains(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        //
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).textContains(text);
            uiSelector = new UiSelector().clickable(true).textContains(text);
        } else {
            bySelector = By.textContains(text);
            uiSelector = new UiSelector().textContains(text);
        }
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                    Log.e(DEBUG_TAG, ".... clicked");
                }
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Find text on screen (case insensitive)
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @return true if successful
     */
    public static boolean findText(UiDevice device, boolean clickable, String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
        } else {
            bySelector = By.textStartsWith(text);
        }
        UiObject2 ob = device.wait(Until.findObject(bySelector), 500);
        return ob != null;
    }

    /**
     * Click on an object
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param resourceId resource id of the object
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickResource(UiDevice device, boolean clickable, String resourceId, boolean waitForNewWindow) {
        Log.w(DEBUG_TAG, "Searching for object with " + resourceId);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).res(resourceId);
            uiSelector = new UiSelector().clickable(true).resourceId(resourceId);
        } else {
            bySelector = By.res(resourceId);
            uiSelector = new UiSelector().resourceId(resourceId);
        }
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                    Log.e(DEBUG_TAG, ".... clicked");
                }
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Click "Up" button in action modes
     * 
     * @param mDevice UiDevice object
     * @return true if the button was clicked
     */
    public static boolean clickUp(@NonNull UiDevice mDevice) {
        UiObject homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
        if (!homeButton.exists()) {
            homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
        }
        try {
            return homeButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
            return false; // can't actually be reached
        }
    }

    /**
     * Click "Home" button in Activity app bars
     * 
     * @param mDevice UiDevice object
     */
    public static void clickHome(UiDevice mDevice) {
        clickResource(mDevice, true, "de.blau.android:id/action_mode_close_button", true);
    }

    /**
     * Buffered read an InputStream into a byte array
     * 
     * @param is the InputStream to read
     * @return a byte array
     * @throws IOException
     */
    public static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readBytes = -1;
        try {
            while ((readBytes = is.read(buffer)) > -1) {
                baos.write(buffer, 0, readBytes);
            }
        } finally {
            is.close();
        }
        return baos.toByteArray();
    }
}
