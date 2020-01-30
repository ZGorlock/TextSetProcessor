/*
 * File:    TaggerHelper.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.Jokes;
import pojo.Joke;
import utility.ListUtility;
import utility.StringUtility;
import worker.NsfwChecker;
import worker.TextTagger;

/**
 * Helps with fixing tagging mistakes.
 */
public class TaggerHelper {
    
    //Static Fields
    
    /**
     * The reference to the Text Tagger.
     */
    private static TextTagger textTagger = null;
    
    /**
     * The reference to the NSFW Checker
     */
    private static NsfwChecker nsfwChecker = null;
    
    
    //Main Methods
    
    /**
     * The Main method.
     *
     * @param args Arguments to the Main Method.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        textTagger = TextTagger.getInstance();
        nsfwChecker = NsfwChecker.getInstance();
        textTagger.printTagTrigger = true;
        textTagger.load();
        nsfwChecker.load();
        
        List<Joke> tagged = new ArrayList<>();
        for (Jokes.JokeSet jokeSet : Jokes.JokeSet.values()) {
            File taggedFile = new File(jokeSet.directory, "source/3 - fixed/fixed.json");
            if (taggedFile.exists()) {
                tagged.addAll(Jokes.readJokes(taggedFile));
            }
        }
        System.out.println("Ready");
        
        Scanner input = new Scanner(System.in);
        while (true) {
            Joke joke = ListUtility.selectRandom(tagged);
            if (joke != null) {
                System.out.println("\n\n\n");
                for (String s : StringUtility.wrapText(joke.text, 120)) {
                    System.out.println(s);
                }
                joke.tags = textTagger.getTagsFromText(joke.text);
                joke.nsfw = joke.nsfw || nsfwChecker.checkNsfw(joke.text, joke.tags);
                System.out.println();
    
                System.out.println("Tags: ");
                StringBuilder tags = new StringBuilder();
                for (String tag : joke.tags) {
                    tags.append((tags.length() > 0) ? ", " : "").append(tag);
                }
                System.out.println(tags);
                System.out.println("NSFW: " + joke.nsfw);
                System.out.println("Source: " + joke.source);
                input.nextLine();
            }
        }
    }
    
}
