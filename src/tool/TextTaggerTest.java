/*
 * File:    TextTaggerTest.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import worker.TextTagger;

/**
 * Tests the Text Tagger
 */
public final class TextTaggerTest {
    
    //Static Fields
    
    /**
     * The reference to the Text Tagger.
     */
    private static TextTagger textTagger = null;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        textTagger = TextTagger.getInstance();
        textTagger.printTagTrigger = true;
        textTagger.load();
        System.out.println("Ready");
        
        Scanner in = new Scanner(System.in);
        String test;
        while (!(test = in.nextLine()).isEmpty()) {
            List<String> tags = textTagger.getTagsFromText(test);
            tags.sort(Comparator.naturalOrder());
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(tags.get(i));
            }
            System.out.println();
        }
    }
    
}
