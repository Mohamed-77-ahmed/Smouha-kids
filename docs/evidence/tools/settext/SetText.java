import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * Types any text (Arabic too) into a field on the TEST emulator, for screenshots: `adb shell input text`
 * and UiObject.setText (key events) only handle Latin letters, so this uses the accessibility action
 * ACTION_SET_TEXT on the field. Built and run by settext.ps1:
 *   uiautomator runtest settext.jar -c SetText -e id newNameInput -e cps 646.648.631
 * cps = the text as hexadecimal Unicode code points, so no shell or code page can change it.
 */
public class SetText extends UiAutomatorTestCase {

    /** UiObject keeps the node lookup protected; a subclass may use it. */
    static class Field extends UiObject {
        Field(String id) {
            super(new UiSelector().resourceIdMatches(".*:id/" + id));
        }

        boolean set(String text) {
            AccessibilityNodeInfo node = findAccessibilityNodeInfo(5000);
            if (node == null) return false;
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
    }

    public void testSetText() throws Exception {
        String id = getParams().getString("id");
        StringBuilder text = new StringBuilder();
        for (String hex : getParams().getString("cps").split("\\.")) text.appendCodePoint(Integer.parseInt(hex, 16));
        assertTrue("could not set the text of " + id, new Field(id).set(text.toString()));
    }
}
