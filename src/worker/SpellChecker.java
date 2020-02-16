/*
 * File:    SpellChecker.java
 * Package: worker
 * Author:  Zachary Gill
 */

package worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import utility.Filesystem;
import utility.ListUtility;
import utility.StringUtility;

/**
 * Checks Spelling.
 */
public final class SpellChecker {
    
    //Static Fields
    
    /**
     * The singleton instance of the Spell Checker.
     */
    private static SpellChecker instance = null;
    
    /**
     * A flag indicating whether or not the Spell Checker has been loaded yet or not.
     */
    private static AtomicBoolean loaded = new AtomicBoolean(false);
    
    
    //Fields
    
    /**
     * A list of strings in the dictionary.
     */
    public final HashSet<String> dict = new HashSet<>();
    
    /**
     * A flag indicating whether or not to load additional dicts.
     */
    public boolean loadAdditionalDicts = true;
    
    
    //Constructors
    
    /**
     * The private constructor for the Spell Checker.
     */
    private SpellChecker() {
    }
    
    /**
     * Returns the singleton instance of the Spell Checker.
     *
     * @return The singleton instance of the Spell Checker.
     */
    public static SpellChecker getInstance() {
        if (instance == null) {
            instance = new SpellChecker();
        }
        return instance;
    }
    
    /**
     * Loads the Spell Checker.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        
        System.out.print("Loading Dictionary... ");
        
        TextTagger textTagger = TextTagger.getInstance();
        textTagger.load();
        
        final List<String> buildDict = Filesystem.readLines(new File("etc/dicts/dict.txt"));
        
        if (loadAdditionalDicts) {
            List<File> dictFiles = Filesystem.getFiles(new File("etc/lists/"));
            dictFiles.addAll(Arrays.asList(
                    new File("etc/dicts/dict-local.txt"),
                    new File("etc/dicts/dict-contractions.txt"),
                    new File("etc/dicts/dict-nsfw.txt"),
                    new File("etc/dicts/cities.txt"),
                    new File("etc/dicts/countries.txt"),
                    new File("etc/dicts/famousPeople.txt"),
                    new File("etc/dicts/names.txt"),
                    new File("etc/dicts/subCountries.txt"),
                    new File("etc/other/fileExtensions.txt")));
            dictFiles.forEach(e -> Filesystem.readLines(e).forEach(e2 -> buildDict.addAll(Arrays.asList(e2.split("\\s+")))));
            
            textTagger.tagDict.forEach(e -> buildDict.addAll(Arrays.asList(e.split("\\s+"))));
        }
        
        dict.clear();
        dict.addAll(ListUtility.removeDuplicates(buildDict.stream().map(String::toLowerCase).collect(Collectors.toList())));
        
        System.out.println("(" + dict.size() + " Words)");
    }
    
    
    //Methods
    
    /**
     * Checks a string for spelling mistakes.
     *
     * @param text The string.
     * @return The list of words in the string that were not found in the dictionary.
     */
    public List<String> checkForSpelling(String text) {
        List<String> fix = new ArrayList<>();
        Pattern wordGetter = Pattern.compile("(^|(?<=[^a-zA-Z\\-]))(?<word>[a-zA-Z\\-']*[a-zA-Z\\-]('?[a-zA-Z]*))((?=[^a-zA-Z\\-]|$))");
        Matcher wordMatcher = wordGetter.matcher(text);
        String current = "";
        String last;
        while (wordMatcher.find()) {
            String word = wordMatcher.group("word");
            last = current;
            current = word;
            String testWord = word;
            if (testWord.contains("-")) {
                continue;
            }
            if (testWord.startsWith("'") && testWord.endsWith("'")) {
                word = testWord.replaceAll("^'", "").replaceAll("'$", "");
            } else {
                word = testWord.replaceAll("^'", "");
            }
            if ((word.length() > 1) && (word.charAt(0) == 'i') && Character.isUpperCase(word.charAt(1))) {
                continue;
            }
            testWord = word;
            if (fix.contains(word)) {
                continue;
            }
            if (testWord.toLowerCase().startsWith("o'") || testWord.toLowerCase().startsWith("m'") || testWord.startsWith("Mc")) {
                continue;
            }
            testWord = word.replaceAll("'?[sS]?$", "");
            if (testWord.toUpperCase().equals(testWord)) {
                continue;
            }
            testWord = word.toLowerCase().replaceAll("'s?$", "");
            if (!dict.contains(testWord)) {
                if (dict.contains(StringUtility.removePunctuation(testWord))) {
                    continue;
                }
                if (last.toUpperCase().equals("MR") || last.toUpperCase().equals("MRS") || last.toUpperCase().equals("MS") || last.toUpperCase().equals("DR")) {
                    continue;
                }
                if (!testWord.endsWith("g") && dict.contains(testWord + "g")) {
                    continue;
                }
                if (testWord.endsWith("a") && dict.contains(StringUtility.rShear(testWord, 1))) {
                    continue;
                }
                if (testWord.endsWith("th") && dict.contains(StringUtility.rShear(testWord, 2))) {
                    continue;
                }
                if (testWord.endsWith("eth") && dict.contains(StringUtility.rShear(testWord, 3))) {
                    continue;
                }
                testWord = word.toLowerCase();
                if (!testWord.matches("((n+)?a+(h+|w+))|(h*m+h+m*)|(a+(w+|r+))|(o+h*)|(so+)|([uh]+(m+|h+))|([nyw]o+)|(wa+h+)|(p+(f+t+|s+h+))|(d+u+h+)|(b+z+)|(s+h+)|(b+a+)|(e+r+)|(w+h?[oa]+[wh]+)|(e+w+)|(u+n+h+)|((h+a+)+)|(m+)|(p+s+t+)|(p+f+t+)|(m+o+)|(v+r+o+m+)|(a+r+g+h*)|(e+h+)|(d+a+)|(z+)|(w+h*e+w*)|(b+o+)|(g+r+)|(s+h*)|((h+e+)+h*)")) {
                    if (!fix.contains(word)) {
                        fix.add(word);
                    }
                }
            }
        }
        return fix;
    }
    
}
