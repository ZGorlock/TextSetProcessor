/*
 * File:    NsfwChecker.java
 * Package: worker
 * Author:  Zachary Gill
 */

package worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utility.Filesystem;
import utility.StringUtility;

/**
 * Checks NSFW.
 */
public final class NsfwChecker {
    
    //Static Fields
    
    /**
     * The singleton instance of the NSFW Checker.
     */
    private static NsfwChecker instance = null;
    
    /**
     * A flag indicating whether or not the NSFW Checker has been loaded yet or not.
     */
    private static AtomicBoolean loaded = new AtomicBoolean(false);
    
    
    //Fields
    
    /**
     * A list of strings in the NSFW dictionary.
     */
    public final List<String> nsfw = new ArrayList<>();
    
    /**
     * A list of strings in the NSFW dictionary to not check for as prefixes.
     */
    public final List<String> dontDoStartNsfw = new ArrayList<>();
    
    /**
     * A list of strings in the NSFW dictionary to not check for as suffixes.
     */
    public final List<String> dontDoEndNsfw = new ArrayList<>();
    
    /**
     * The reference to the Text Tagger.
     */
    public TextTagger textTagger;
    
    
    //Constructors
    
    /**
     * The private constructor for the NSFW Checker.
     */
    private NsfwChecker() {
    }
    
    /**
     * Returns the singleton instance of the NSFW Checker.
     *
     * @return The singleton instance of the NSFW Checker.
     */
    public static NsfwChecker getInstance() {
        if (instance == null) {
            instance = new NsfwChecker();
        }
        return instance;
    }
    
    /**
     * Loads the NSFW Checker.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        
        System.out.print("Loading NSFW List... ");
        
        textTagger = TextTagger.getInstance();
        textTagger.load();
        
        nsfw.addAll(Filesystem.readLines(new File("etc/dicts/nsfw.txt")));
        
        dontDoStartNsfw.addAll(Arrays.asList("coon", "tit", "ass", "arse", "fuh"));
        dontDoEndNsfw.addAll(Arrays.asList("anal", "ass", "crap", "homo", "muff", "prick", "tit", "fuh"));
        
        System.out.println("(" + nsfw.size() + " Words)");
    }
    
    
    //Methods
    
    /**
     * Determines if a string or its associated tags are NSFW.
     *
     * @param text The string.
     * @param tags The tags associated with the string.
     * @return Whether or not the string or its associated tags are NSFW.
     */
    public boolean checkNsfw(String text, List<String> tags) {
        for (String tag : tags) {
            if (textTagger.tagList.get(tag).nsfw) {
                return true;
            }
        }
        
        Pattern wordGetter = Pattern.compile("(^|(?<=\\s))(?<word>.+)((?=\\s|$))");
        Matcher wordMatcher = wordGetter.matcher(text);
        while (wordMatcher.find()) {
            String word = StringUtility.removePunctuation(wordMatcher.group("word").toLowerCase().replaceAll("'?s?$", ""));
            for (String append : Arrays.asList("", "s", "es", "ing")) {
                if (nsfw.contains(word + append)) {
                    return true;
                }
            }
            for (String nsfwWord : nsfw) {
                for (String append : Arrays.asList("", "s", "es", "ing")) {
                    if ((word.startsWith(nsfwWord + append) && !dontDoStartNsfw.contains(nsfwWord)) || (word.endsWith(nsfwWord + append) && !dontDoEndNsfw.contains(nsfwWord))) {
                        return true;
                    }
                }
            }
        }
        return text.toUpperCase().contains("FUCK") || text.toUpperCase().contains("NIGGER") || text.contains("PORN");
    }
    
    /**
     * Determines if a string is NSFW.
     *
     * @param text The string.
     * @return Whether or not the string is NSFW.
     *
     * @see #checkNsfw(String, List)
     */
    public boolean checkNsfw(String text) {
        return checkNsfw(text, new ArrayList<>());
    }
    
}
