/*
 * File:    RedditFixer.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.Jokes;
import parser.JokeParser;
import pojo.Joke;
import resource.Console;
import utility.Filesystem;
import utility.StringUtility;
import worker.NsfwChecker;
import worker.SpellChecker;
import worker.TextFixer;
import worker.TextTagger;

/**
 * Helps with fixing spelling mistakes for Reddit.
 */
public class RedditFixer {
    
    //Static Fields
    
    /**
     * The reference to the Joke Parser.
     */
    private static final JokeParser jokeParser = JokeParser.getInstance();
    
    /**
     * The reference to the Text Tagger.
     */
    private static final TextTagger textTagger = TextTagger.getInstance();
    
    /**
     * The reference to the Text Fixer.
     */
    private static final TextFixer textFixer = TextFixer.getInstance();
    
    /**
     * The reference to the Spell Checker.
     */
    private static final SpellChecker spellChecker = SpellChecker.getInstance();
    
    /**
     * The reference to the NSFW Checker
     */
    private static final NsfwChecker nsfwChecker = NsfwChecker.getInstance();
    
    
    //Main Methods
    
    /**
     * The Main method.
     *
     * @param args Arguments to the Main Method.
     */
    public static void main(String[] args) {
        List<Joke> jokes;
        jokeParser.preserveSource = true;
        
        File workFixedFile = new File("jokes/reddit/source/1 - cleaned/cleaned-work-fixed.json");
        File workFile = new File("jokes/reddit/source/1 - cleaned/cleaned-work.json");
        File workBackupFile = new File("jokes/reddit/source/1 - cleaned/cleaned-work-bak.json");
        File workFixListFile = new File("jokes/reddit/source/1 - cleaned/cleaned-work-fixList.txt");
        File workIndexFile = new File("jokes/reddit/source/1 - cleaned/cleaned-work-index.txt");
        
        if (!workFixedFile.exists()) {
            System.out.println("Starting Up...");
            jokeParser.load();
            textTagger.load();
            textFixer.load();
            spellChecker.load();
            
            System.out.println("Parsing Reddit...");
            jokes = Jokes.parseJokeSet(Jokes.JokeSet.Reddit);
            
            System.out.println("Fixing Reddit...");
            jokes.parallelStream().forEach(joke -> Jokes.fixJoke(joke, null));
            
            Jokes.outputJokes(workFixedFile, jokes, false);
        } else {
            jokes = Jokes.readJokes(workFixedFile);
        }
        
        Scanner input = new Scanner(System.in);
        List<String> fixList = new ArrayList<>();
        int count = 0;
        int lastIndex = 0;
        int index = workIndexFile.exists() ? Integer.parseInt(Filesystem.readFileToString(workIndexFile)) : 0;
        for (int i = index; i < jokes.size(); i++) {
            Joke j = jokes.get(i);
            if (!j.fix.isEmpty()) {
                boolean goBack = false;
                boolean delete = false;
                for (String fixing : j.fix) {
                    System.out.println("\n\n\n");
                    for (String s : StringUtility.wrapText(j.text.replaceAll(fixing, Console.yellow("***" + fixing + "***")), 120)) {
                        System.out.println(s);
                    }
                    System.out.println("::" + fixing + "::");
                    System.out.print(":");
                    StringSelection selection = new StringSelection(fixing);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                    String typed = input.nextLine();
                    if (!typed.isEmpty()) {
                        if (typed.equalsIgnoreCase("~")) {
                            goBack = true;
                            break;
                        } else if (typed.equalsIgnoreCase("*")) {
                            delete = true;
                            break;
                        } else {
                            j.source = j.source.replaceAll(fixing, typed);
                        }
                    } else {
                        fixList.add(fixing);
                    }
                    count++;
                }
                if (goBack) {
                    i = lastIndex - 1;
                    continue;
                }
                if (delete) {
                    jokes.remove(i);
                    i--;
                } else {
                    lastIndex = i;
                }
            }
            if (count >= 10) {
                Filesystem.copyFile(workFile, workBackupFile, true);
                jokeParser.writeReddit(jokes);
                Filesystem.writeLines(workFixListFile, fixList, true);
                fixList.clear();
                Filesystem.writeStringToFile(workIndexFile, String.valueOf(i + 1));
                count = 0;
            }
        }
        jokeParser.writeReddit(jokes);
    }
    
}