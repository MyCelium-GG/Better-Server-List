package my.celium.org.betterserverlist.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;

/**
 * Keyboard calls that differ between Minecraft lines (26.3 moved input to
 * SDL); this is the 26.3 copy (one per line in common/src/mc).
 */
final class Keys {
    private Keys() {
    }

    static boolean isDown(int key) {
        return InputConstants.isKeyDown(key);
    }

    /** The character of a printable key, lower case (letters, digits, punctuation); anything else is not printable ASCII. */
    static int typedKey(KeyEvent event) {
        return event.shortcutKey();
    }
}
