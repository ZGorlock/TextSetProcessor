/*
 * File:    TagHotfixer.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.Jokes;
import pojo.Joke;
import resource.ConsoleProgressBar;
import worker.NsfwChecker;

/**
 * Hotfixes NSFW for tagged jokes.
 */
public class NsfwHotfixer {
    
    //Static Fields
    
    /**
     * The reference to the NSFW Checker
     */
    private static NsfwChecker nsfwChecker = null;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        nsfwChecker = NsfwChecker.getInstance();
        nsfwChecker.load();
        
        for (Jokes.JokeSet jokeSet : Jokes.JokeSet.values()) {
            File fixedFile = new File(jokeSet.directory, "/source/3 - fixed/fixed.json");
            File taggedFile = new File(jokeSet.directory, "/source/4 - tagged/tagged.json");
            if (!taggedFile.exists()) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            Map<Long, Joke> preJokes = new HashMap<>();
            for (Joke preJoke : Jokes.readJokes(fixedFile)) {
                preJokes.put(preJoke.hash, preJoke);
            }
            
            List<Joke> jokes = Jokes.readJokes(taggedFile);
            progressBar.setTotal(jokes.size());
            
            jokes.parallelStream().forEach(joke -> {
                joke.nsfw = preJokes.get(joke.hash).nsfw || nsfwChecker.checkNsfw(joke.text, joke.tags);
                progressBar.addOne();
            });
            
            Jokes.outputJokes(new File(taggedFile.getAbsolutePath().replace("tagged.json", "tagged-hotfix.json")), jokes);
            progressBar.complete();
        }
    }
    
}
