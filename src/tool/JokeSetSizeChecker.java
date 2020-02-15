/*
 * File:    JokeSetSizeChecker.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;

import main.Jokes;
import parser.JokeParser;

/**
 * Checks that the size of a Joke Set remained constant throughout processing.
 */
public class JokeSetSizeChecker {
    
    //Static Fields
    
    /**
     * The reference to the Joke Parser.
     */
    private static final JokeParser jokeParser = JokeParser.getInstance();
    
    
    //Main Methods
    
    /**
     * The Main method.
     *
     * @param args Arguments to the Main Method.
     */
    public static void main(String[] args) {
        jokeParser.load();
        System.out.println();
        
        int totalCount = 0;
        for (Jokes.JokeSet jokeSet : Jokes.JokeSet.values()) {
            System.out.println(jokeSet.name() + "...");
            
            int cleanedCount = jokeParser.parseJokeSet(jokeSet).size();
            System.out.println("Cleaned:  " + cleanedCount);
            
            File parsed = new File(jokeSet.directory, "source/2 - parsed/parsed.json");
            if (parsed.exists()) {
                int parsedCount = Jokes.readJokes(parsed).size();
                if (parsedCount == cleanedCount) {
                    System.out.println("Parsed:   " + parsedCount);
                } else {
                    System.err.println("Parsed:   " + parsedCount);
                }
            }
            
            File fixed = new File(jokeSet.directory, "source/3 - fixed/fixed.json");
            if (fixed.exists()) {
                int fixedCount = Jokes.readJokes(fixed).size();
                if (fixedCount == cleanedCount) {
                    System.out.println("Fixed:    " + fixedCount);
                } else {
                    System.err.println("Fixed:    " + fixedCount);
                }
            }
            
            File tagged = new File(jokeSet.directory, "source/4 - tagged/tagged.json");
            if (tagged.exists()) {
                int taggedCount = Jokes.readJokes(tagged).size();
                if (taggedCount == cleanedCount) {
                    System.out.println("Tagged:   " + taggedCount);
                } else {
                    System.err.println("Tagged:   " + taggedCount);
                }
            }
            
            File compiled = new File(jokeSet.directory, jokeSet.name().toLowerCase() + ".json");
            if (compiled.exists()) {
                int compiledCount = Jokes.readJokes(compiled).size();
                if (compiledCount == cleanedCount) {
                    System.out.println("Compiled: " + compiledCount);
                } else {
                    System.err.println("Compiled: " + compiledCount);
                }
            }
            
            totalCount += cleanedCount;
            System.out.println();
        }
        
        File merged = new File("jokes/jokes.json");
        if (merged.exists()) {
            System.out.println("Total...");
            int mergedCount = Jokes.readJokes(merged).size();
            if (mergedCount == totalCount) {
                System.out.println("Merged: " + mergedCount);
            } else {
                System.err.println("Merged: " + mergedCount);
            }
        }
    }
    
}
