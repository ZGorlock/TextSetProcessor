/*
 * File:    TagEndingFinder.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.util.Map;

import pojo.Tag;
import worker.TextTagger;

/**
 * Finds tags ending with a certain string.
 */
public final class TagEndingFinder {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        TextTagger textTagger = TextTagger.getInstance();
        textTagger.loadTagLists = false;
        textTagger.load();
        
        String ending = "Y";
        
        int longer = 0;
        System.out.println();
        for (Map.Entry<String, Tag> t : textTagger.tagList.entrySet()) {
            if (t.getValue().name.length() > longer) {
                longer = t.getValue().name.length();
            }
//            if (t.getValue().name.toUpperCase().endsWith(ending.toUpperCase())) {
//                System.out.println(t.getValue().name);
//            }
        }
    
        System.out.println(longer);
    }
    
}
