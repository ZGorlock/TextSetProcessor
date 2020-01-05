/*
 * File:    ListCleaner.java
 * Package: worker
 * Author:  Zachary Gill
 */

package tool;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import utility.Filesystem;

/**
 * Helps fix spelling mistakes from a fix list.
 */
public final class FixerHelper {
    
    //Static Fields
    
    /**
     * The file to read the fix list from.
     */
    private static File fixListFile = new File("jokes/reddit/source/3 - fixed/fixed-fixList.txt");
    
    /**
     * The fix list read from the file.
     */
    private static List<String> fixList = new ArrayList<>();
    
    /**
     * The current index in the fix list.
     */
    private static int index = 0;
    
    /**
     * The global key listener for the keyboard.
     */
    private static NativeKeyListener keyboardHook = null;
    
    /**
     * The reference to the clipboard.
     */
    private static Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    /**
     * Whether the control key is down or not.
     */
    private static boolean controlDown = false;
    
    /**
     * Whether the shift key is down or not.
     */
    private static boolean shiftDown = false;
    
    /**
     * A flag indicating whether or not to wait for a key reset.
     */
    private static boolean waitForReset = false;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args The arguments to the main method.
     */
    public static void main(String[] args) throws Exception {
        setupKeyListener();
        fixList = Filesystem.readLines(fixListFile);
        while (true) {
        }
    }
    
    
    //Functions
    
    /**
     * Sets up the key listener for the helper.
     */
    private static void setupKeyListener() throws Exception {
        GlobalScreen.registerNativeHook();
        NativeKeyListener keyboardHook = new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
                int code = nativeKeyEvent.getRawCode();
                switch (NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode())) {
                    case "Ctrl":
                        controlDown = true;
                        break;
                    case "Shift":
                    case "Unknown keyCode: 0xe36":
                        shiftDown = true;
                        break;
                }
                if (controlDown && shiftDown && !waitForReset) {
                    clipboard.setContents(new StringSelection(fixList.get(index++)), null);
                    waitForReset = true;
                }
            }
            
            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
                int code = nativeKeyEvent.getRawCode();
                switch (NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode())) {
                    case "Ctrl":
                        controlDown = false;
                        waitForReset = false;
                        break;
                    case "Shift":
                    case "Unknown keyCode: 0xe36":
                        shiftDown = false;
                        waitForReset = false;
                        break;
                }
            }
            
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
            }
        };
        GlobalScreen.addNativeKeyListener(keyboardHook);
    }
    
}
