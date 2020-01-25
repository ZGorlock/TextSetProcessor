/*
 * File:    TextFixer.java
 * Package: worker
 * Author:  Zachary Gill
 */

package worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import utility.Filesystem;
import utility.StringUtility;

/**
 * Fixes Text.
 */
public final class TextFixer {
    
    //Static Fields
    
    /**
     * The singleton instance of the Text Fixer.
     */
    private static TextFixer instance = null;
    
    /**
     * A flag indicating whether or not the Text Fixer has been loaded yet or not.
     */
    private static AtomicBoolean loaded = new AtomicBoolean(false);
    
    
    //Fields
    
    /**
     * A map of diacritics to their replacements.
     */
    public final Map<String, String> diacritics = new HashMap<>();
    
    /**
     * A list of file extensions.
     */
    public final List<String> fileExtensionList = new ArrayList<>();
    
    
    //Constructors
    
    /**
     * The private constructor for the Text Fixer.
     */
    private TextFixer() {
    }
    
    /**
     * Returns the singleton instance of the Text Fixer.
     *
     * @return The singleton instance of the Text Fixer.
     */
    public static TextFixer getInstance() {
        if (instance == null) {
            instance = new TextFixer();
        }
        return instance;
    }
    
    /**
     * Loads the Text Fixer.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        
        fileExtensionList.addAll(Filesystem.readLines(new File("etc/other/fileExtensions.txt")));
        
        System.out.print("Loading Diacritics... ");
        
        List<String> lines = Filesystem.readLines(new File("etc/other/diacritics.txt"));
        String replacement = "";
        for (String line : lines) {
            if (((int) line.charAt(0)) < 128) {
                replacement = line;
                continue;
            }
            diacritics.put(line, replacement);
        }
        
        System.out.println("(" + diacritics.size() + " Diacritics)");
    }
    
    
    //Methods
    
    /**
     * Cleans and formats a string.
     *
     * @param text The string.
     * @return The cleaned and formatted string.
     */
    public String cleanText(String text) {
        text = replaceDiacritics(text);
        
        if (StringUtility.numberOfOccurrences("\\\"", text) % 2 != 0) {
            text = text.replace("\\\"", "");
        }
        
        text = StringUtility.trim(text);
        for (int i = 0; i < text.length(); i++) {
            if (!StringUtility.isSymbol(text.charAt(i))) {
                if (!Character.isUpperCase(text.charAt(i))) {
                    text = text.substring(0, i) + String.valueOf(text.charAt(i)).toUpperCase() + text.substring(i + 1);
                }
                break;
            }
        }
        List<Character> punctuation = Arrays.asList('.', '!', '?', ';', ':', '(', ')');
        for (int i = text.length() - 1; i >= 0; i--) {
            if (punctuation.contains(text.charAt(i))) {
                break;
            } else if (text.charAt(i) != '"' && text.charAt(i) != '\'') {
                text = text.substring(0, i + 1) + '.' + text.substring(i + 1);
                break;
            }
        }
        
        text = text.replaceAll("([,.!?;:)])([^,.!?;:(){}\\-'])", "$1 $2")
                   .replaceAll("\\.\\s*\\.(\\s*\\.)+", "...")
                   .replaceAll("\\s+([,.!?:;])\\s+", "$1 ")
                   .replaceAll("\\s*!(\\s*!)+", "!")
                   .replaceAll("\\s*\\?(\\s*\\?)+", "?")
                   .replaceAll("\\s+", " ")
                   .replaceAll("([\"!?,])\\s+\\.\\.\\.\\s+", "$1... ")
                   .replaceAll("\\s+\\.\\.\\.\\s+", "... ")
                   .replaceAll("''", "\"")
                   .replaceAll("\\\\*\"", "\\\\\"")
                   .replaceAll("\\\\\"\\\\\"", "\\\\\" \\\\\"")
                   .replaceAll("(\\d+)([.,:])\\s+(\\d+)", "$1$2$3")
                   .replaceAll("[^\\x00-\\x7F]", "")
                   .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        boolean doCapitalize = true;
        for (int i = 0; i < text.length(); i++) {
            if (punctuation.contains(text.charAt(i))) {
                doCapitalize = true;
            } else if (doCapitalize) {
                if (StringUtility.isAlphanumeric(text.charAt(i))) {
                    if (!((text.charAt(i) == 'i') && (text.length() > (i + 1)) && String.valueOf(text.charAt(i + 1)).toUpperCase().endsWith(String.valueOf(text.charAt(i + 1))))) {
                        text = text.substring(0, i) + String.valueOf(text.charAt(i)).toUpperCase() + text.substring(i + 1);
                    }
                    doCapitalize = false;
                }
            }
        }
        
        boolean inQuote = false;
        boolean justInQuote = false;
        for (int i = 1; i < text.length(); i++) {
            if (inQuote) {
                if (justInQuote) {
                    for (int j = i - 1; j >= 0; j--) {
                        if (punctuation.contains(text.charAt(j + 1))) {
                            break;
                        }
                        if (StringUtility.isAlphabetic(text.charAt(j))) {
                            if (!punctuation.contains(text.charAt(j + 1)) && text.charAt(j + 1) != ',') {
                                text = text.substring(0, j + 1) + "," + text.substring(j + 1);
                                i++;
                                if (j + 2 < text.length() && !StringUtility.isWhitespace(text.charAt(j + 2))) {
                                    text = text.substring(0, j + 2) + " " + text.substring(j + 2);
                                    i++;
                                }
                            }
                            break;
                        }
                    }
                    justInQuote = false;
                }
                if ((text.charAt(i) == '"' && text.charAt(i - 1) == '\\')) {
                    if (i < text.length() - 1) {
                        if (text.charAt(i + 1) != ' ') {
                            text = text.substring(0, i + 1) + " " + text.substring(i + 1);
                        }
                    }
                    if (i > 1) {
                        char replacePunct = '\0';
                        if (i < text.length() - 2 && StringUtility.isSymbol(text.charAt(i + 2)) && (i >= text.length() - 3 || !StringUtility.isSymbol(text.charAt(i + 3))) &&
                                (text.charAt(i + 2) != '(' && text.charAt(i + 2) != '*')
                        ) {
                            replacePunct = text.charAt(i + 2);
                            text = text.substring(0, i + 2) + text.substring(i + 3);
                        }
                        for (int j = i - 2; j >= 1; j--) {
                            if (StringUtility.isAlphanumeric(text.charAt(j))) {
                                break;
                            } else {
                                if (StringUtility.isWhitespace(text.charAt(j))) {
                                    text = text.substring(0, j) + text.substring(j + 1);
                                    i--;
                                    j++;
                                }
                            }
                        }
                        char rc = text.charAt(i - 2);
                        if (!StringUtility.isSymbol(rc)) {
                            boolean endSentence = true;
                            for (int j = i + 1; j < text.length(); j++) {
                                if (StringUtility.isSymbol(text.charAt(j)) && text.charAt(j) != '"' &&
                                        (text.charAt(j) != '\\' && (j + 1 < text.length() && text.charAt(j + 1) != '"')) &&
                                        (text.charAt(j) != '.' && (j + 1 < text.length() && text.charAt(j + 1) != '.') && (j + 2 < text.length() && text.charAt(j + 2) != '.'))
                                ) {
                                    text = text.substring(0, j - 1) + text.substring(j);
                                    j--;
                                    continue;
                                }
                                if (StringUtility.isAlphabetic(text.charAt(j))) {
                                    if (!Character.isUpperCase(text.charAt(j))) {
                                        endSentence = false;
                                    } else if (replacePunct == ',') {
                                        text = text.substring(0, j - 1) + Character.toLowerCase(text.charAt(j)) + text.substring(j + 1);
                                    }
                                    break;
                                }
                            }
                            text = text.substring(0, i - 1) + (replacePunct != '\0' ? replacePunct : (endSentence ? '.' : ',')) + text.substring(i - 1);
                            i++;
                        }
                    }
                    inQuote = false;
                }
            } else {
                if (text.charAt(i) == '"') {
                    inQuote = true;
                    justInQuote = true;
                    while (text.length() > i + 1 && text.charAt(i + 1) == ' ') {
                        text = text.substring(0, i + 1) + (text.length() > i + 2 ? text.substring(i + 2) : "");
                    }
                }
            }
        }
        
        for (String fileExtension : fileExtensionList) {
            text = text.replaceAll("\\.\\s+" + fileExtension + "([.\\s])", "." + fileExtension.toLowerCase() + "$1");
        }
        
        text = StringUtility.trim(text);
        text = text.replaceAll("\\s+", " ")
                   .replaceAll("\\s([!?.])$", "$1")
                   .replaceAll("(\\d+)([,])\\s+(\\d+)", "$1$2$3")
                   .replace(".\\.\\\"", ".\\\"");
        
        return StringUtility.trim(text);
    }
    
    /**
     * Replaces diacritics in a string.
     *
     * @param text The string.
     * @return The string with diacritics replaced.
     */
    public String replaceDiacritics(String text) {
        String work = text;
        for (Map.Entry<String, String> diacritic : diacritics.entrySet()) {
            work = work.replace(diacritic.getKey(), diacritic.getValue());
        }
        return work;
    }
    
}
