/*
 * File:    SpellChecker.java
 * Package: worker
 * Author:  Zachary Gill
 */

package worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pojo.Tag;
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
    public final List<String> dict = new ArrayList<>();
    
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
        
        List<String> buildDict = Filesystem.readLines(new File("etc/dicts/dict.txt"));
        
        if (loadAdditionalDicts) {
            List<String> additionalDict = new ArrayList<>();
            additionalDict.addAll(Filesystem.readLines(new File("etc/dicts/cities.txt")));
            additionalDict.addAll(Filesystem.readLines(new File("etc/dicts/countries.txt")));
            additionalDict.addAll(Filesystem.readLines(new File("etc/dicts/famousPeople.txt")));
            additionalDict.addAll(Filesystem.readLines(new File("etc/dicts/names.txt")));
            additionalDict.addAll(Filesystem.readLines(new File("etc/dicts/subCountries.txt")));
            for (File list : Filesystem.listFiles(new File("etc/lists/"), var -> true)) {
                additionalDict.addAll(Filesystem.readLines(list));
            }
            additionalDict = additionalDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            additionalDict = ListUtility.removeDuplicates(additionalDict);
            additionalDict.sort(Comparator.naturalOrder());
            
            for (String additionalWord : additionalDict) {
                if (additionalWord.contains(" ")) {
                    String[] additionalWordParts = additionalWord.split("\\s+");
                    for (String additionalWordPart : additionalWordParts) {
                        buildDict.add(StringUtility.trim(additionalWordPart));
                    }
                } else {
                    buildDict.add(additionalWord);
                }
            }
            
            List<String> tagDict = new ArrayList<>();
            for (Tag tag : textTagger.tagList.values()) {
                for (String ending : textTagger.tagEndingToDontDoList.keySet()) {
                    if (tag.name.toUpperCase().endsWith(ending) && !textTagger.tagEndingToDontDoList.get(ending).contains(tag.name)) {
                        for (String append : textTagger.tagEndingToReplacements.get(ending)) {
                            if (textTagger.tagEndingToDontDoList.containsKey(append) && textTagger.tagEndingToDontDoList.get(append).contains(tag.name)) {
                                continue;
                            }
                            tagDict.add(StringUtility.rShear(tag.name, ending.length()) + append);
                        }
                    }
                }
                final List<String> aliasAppends = Arrays.asList("", "S", "ES", "IES");
                for (String alias : tag.aliases) {
                    for (String append : aliasAppends) {
                        if ((append.equals("ES") && alias.length() <= 4) ||
                                (append.equals("IES") && !alias.toUpperCase().endsWith("Y"))) {
                            continue;
                        }
                        tagDict.add(StringUtility.rShear(alias, (append.equals("IES") ? 1 : 0)) + append);
                    }
                }
            }
            tagDict = tagDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            tagDict = ListUtility.removeDuplicates(tagDict);
            tagDict.sort(Comparator.naturalOrder());
            
            for (String tagDictEntry : tagDict) {
                if (tagDictEntry.contains(" ")) {
                    String[] tagDictEntryParts = tagDictEntry.split("\\s+");
                    for (String tagDictEntryPart : tagDictEntryParts) {
                        buildDict.add(StringUtility.trim(tagDictEntryPart));
                    }
                } else {
                    buildDict.add(tagDictEntry);
                }
            }
            
            List<String> nsfwDict = Filesystem.readLines(new File("etc/dicts/nsfw.txt"));
            nsfwDict = nsfwDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            nsfwDict = ListUtility.removeDuplicates(nsfwDict);
            nsfwDict.sort(Comparator.naturalOrder());
            Filesystem.writeLines(new File("etc/dicts/nsfw.txt"), nsfwDict);
            buildDict.addAll(nsfwDict);
            
            List<String> contractionDict = Filesystem.readLines(new File("etc/dicts/dict-contractions.txt"));
            contractionDict = contractionDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            contractionDict = ListUtility.removeDuplicates(contractionDict);
            contractionDict.sort(Comparator.naturalOrder());
            Filesystem.writeLines(new File("etc/dicts/dict-contractions.txt"), contractionDict);
            buildDict.addAll(contractionDict);
            
            List<String> localDict = Filesystem.readLines(new File("etc/dicts/dict-local.txt"));
            localDict = localDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            List<String> uniqueLocalDict = new ArrayList<>();
            for (String localDictEntry : localDict) {
                if (!buildDict.contains(localDictEntry)) {
                    uniqueLocalDict.add(localDictEntry);
                }
            }
            uniqueLocalDict = uniqueLocalDict.stream().map(String::toLowerCase).collect(Collectors.toList());
            uniqueLocalDict = ListUtility.removeDuplicates(uniqueLocalDict);
            uniqueLocalDict.sort(Comparator.naturalOrder());
            Filesystem.writeLines(new File("etc/dicts/dict-local.txt"), uniqueLocalDict);
            buildDict.addAll(uniqueLocalDict);
        }
        
        buildDict = buildDict.stream().map(String::toLowerCase).collect(Collectors.toList());
        buildDict = ListUtility.removeDuplicates(buildDict);
        buildDict.sort(Comparator.naturalOrder());
        
        dict.clear();
        dict.addAll(buildDict);
        
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
