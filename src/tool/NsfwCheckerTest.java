/*
 * File:    TextTaggerTest.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.util.Scanner;

import worker.NsfwChecker;
import worker.TextTagger;

/**
 * Tests the NSFW Checker.
 */
public final class NsfwCheckerTest {
    
    //Static Fields
    
    /**
     * The reference to the Text Tagger.
     */
    private static TextTagger textTagger = null;
    
    /**
     * The reference to the NSFW Checker
     */
    private static NsfwChecker nsfwChecker = null;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        textTagger = TextTagger.getInstance();
        textTagger.loadTagLists = false;
        nsfwChecker = NsfwChecker.getInstance();
        textTagger.load();
        nsfwChecker.load();
        System.out.println("Ready");
        
        Scanner in = new Scanner(System.in);
        String test;
        while (!(test = in.nextLine()).isEmpty()) {
            boolean nsfw = nsfwChecker.checkNsfw(test);
            System.out.println(nsfw);
            System.out.println();
        }
    }
    
}
