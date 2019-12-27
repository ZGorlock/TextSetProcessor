/*
 * File:    TextFixerTest.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.util.Scanner;

import worker.TextFixer;

/**
 * Tests the Text Fixer.
 */
public final class TextFixerTest {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        TextFixer textFixer = TextFixer.getInstance();
        textFixer.load();
        System.out.println("Ready");
        
        Scanner in = new Scanner(System.in);
        String test;
        while (!(test = in.nextLine()).isEmpty()) {
            System.out.println(textFixer.cleanText(test));
        }
    }
    
}
