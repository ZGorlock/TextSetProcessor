/*
 * File:    TagsFileFormatter.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pojo.Tag;
import utility.Filesystem;
import utility.StringUtility;
import worker.TextTagger;

/**
 * Formats the tags file.
 */
public final class TagsFileFormatter {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        TextTagger textTagger = TextTagger.getInstance();
        textTagger.loadTagLists = false;
        textTagger.printTags = true;
        textTagger.load();
        
        int longest = 0;
        for (Map.Entry<String, Tag> t : textTagger.tagList.entrySet()) {
            if (t.getValue().name.length() > longest) {
                longest = t.getValue().name.length();
            }
        }
        
        int indent = longest + 1;
        while (indent % 4 != 0) {
            indent++;
        }
        
        List<String> output = new ArrayList<>();
        for (Tag tag : textTagger.tagList.values()) {
            StringBuilder line = new StringBuilder(StringUtility.padRight(tag.name, indent));
            for (String alias : tag.aliases) {
                line.append(",").append(alias);
            }
            boolean first = tag.aliases.isEmpty();
            if (tag.nsfw) {
                line.append(first ? "" : " ").append("-nsfw");
                first = false;
            }
            if (tag.minor) {
                line.append(first ? "" : " ").append("-minor");
                first = false;
            }
            if (tag.dontDoS) {
                line.append(first ? "" : " ").append("-dontDoS");
                first = false;
            }
            if (tag.dontDoED) {
                line.append(first ? "" : " ").append("-dontDoED");
                first = false;
            }
            if (tag.dontDoES) {
                line.append(first ? "" : " ").append("-dontDoES");
                first = false;
            }
            if (tag.dontDoIES) {
                line.append(first ? "" : " ").append("-dontDoIES");
                first = false;
            }
            if (tag.dontDoY) {
                line.append(first ? "" : " ").append("-dontDoY");
                first = false;
            }
            if (tag.dontDoING) {
                line.append(first ? "" : " ").append("-dontDoING");
                first = false;
            }
            if (tag.dontDoTION) {
                line.append(first ? "" : " ").append("-dontDoTION");
                first = false;
            }
            if (tag.dontDoER) {
                line.append(first ? "" : " ").append("-dontDoER");
                first = false;
            }
            if (tag.dontDoOR) {
                line.append(first ? "" : " ").append("-dontDoOR");
            }
            output.add(line.toString());
        }
        
        Filesystem.writeLines(new File("etc/tags.txt"), output);
    }
    
}
