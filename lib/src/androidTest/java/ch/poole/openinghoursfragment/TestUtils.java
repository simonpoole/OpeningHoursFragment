package ch.poole.openinghoursfragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.Assert;

import android.util.Log;

/**
 * Various methods to support testing
 * 
 * @author Simon Poole
 *
 */
public class TestUtils {
    private static final String DEBUG_TAG = "TestUtils";

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
     * Click a text on screen (exact)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickTextExact(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).text(text);
            uiSelector = new UiSelector().clickable(true).text(text);
        } else {
            bySelector = By.text(text);
            uiSelector = new UiSelector().text(text);
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
     * @return an UiObject2 or null
     */
    @Nullable
    public static UiObject2 findText(@NonNull UiDevice device, boolean clickable, @NonNull String text) {
        return findText(device, clickable, text, 500);
    }

    /**
     * Find text on screen (case insensitive)
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @param waot ms to wait for text to appear
     * @return an UiObject2 or null
     */
    @Nullable
    public static UiObject2 findText(@NonNull UiDevice device, boolean clickable, @NonNull String text, int wait) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
        } else {
            bySelector = By.textStartsWith(text);
        }
        return device.wait(Until.findObject(bySelector), wait);
    }

    /**
     * Find text on screen (case sensitive)
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @return an UiObject2 or null
     */
    @Nullable
    public static UiObject2 findTextContains(@NonNull UiDevice device, boolean clickable, @NonNull String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        BySelector bySelector = null;
        if (clickable) {
            bySelector = By.clickable(true).textContains(text);
        } else {
            bySelector = By.textContains(text);
        }
        return device.wait(Until.findObject(bySelector), 500);
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
     * Double click at an object
     * 
     * @param device the UiDevice
     * @param object the object
     */
    public static void doubleClick(@NonNull UiDevice device, @NonNull UiObject2 object) {
        Configurator cc = Configurator.getInstance();
        long defaultAckTimeout = cc.getActionAcknowledgmentTimeout();
        cc.setActionAcknowledgmentTimeout(0);
        object.click();
        try {
            Thread.sleep(50); // NOSONAR
        } catch (InterruptedException e) {
        }
        object.click();
        cc.setActionAcknowledgmentTimeout(defaultAckTimeout);
    }
    
    /**
     * Scroll to a specific text
     * 
     * @param text the text
     * @throws UiObjectNotFoundException if the UiScrollable couldn't be found
     */
    public static void scrollTo(@NonNull String text) {
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().textStartsWith(text));
        } catch (UiObjectNotFoundException e) {
            Assert.fail(text + " not found");
        }
    }
}
