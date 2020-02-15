/*
 * File:    TagHotfixer.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.Jokes;
import pojo.Joke;
import resource.ConsoleProgressBar;
import worker.TextTagger;

/**
 * Helps with hotfixing tagged jokes.
 */
public class TagHotfixer {
    
    //Static Fields
    
    /**
     * A list of tags to hotfix.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> redo = Arrays.asList();
    
    /**
     * A list of tags indicating the joke should have its tags to hotfix.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> redoIfHas = Arrays.asList("Lightbulb");
    
    /**
     * A list of initial tags to hotfix.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> redoInitial = Arrays.asList();
    
    /**
     * The reference to the Text Tagger.
     */
    private static TextTagger textTagger = null;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        textTagger = TextTagger.getInstance();
        textTagger.load();
        
        for (Jokes.JokeSet jokeSet : Jokes.JokeSet.values()) {
            File fixedFile = new File(jokeSet.directory, "/source/3 - fixed/fixed.json");
            File taggedFile = new File(jokeSet.directory, "/source/4 - tagged/tagged.json");
            if (!taggedFile.exists()) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            List<Joke> preJokes = Jokes.readJokes(fixedFile);
            Map<Integer, List<String>> preTags = new HashMap<>();
            for (Joke preJoke : preJokes) {
                preTags.put(preJoke.hash, preJoke.tags);
            }
            List<Joke> jokes = Jokes.readJokes(taggedFile);
            
            progressBar.setTotal(jokes.size());
            
            for (Joke joke : jokes) {
                boolean retag = false;
                
                if (!redoIfHas.isEmpty()) {
                    for (String tag : joke.tags) {
                        if (redoIfHas.contains(tag)) {
                            retag = true;
                            break;
                        }
                    }
                }
                
                if (!retag && !redoInitial.isEmpty()) {
                    List<String> initialTags = textTagger.getInitialTags(joke.text);
                    for (String redoInitialEntry : redoInitial) {
                        if (initialTags.contains(redoInitialEntry)) {
                            retag = true;
                            break;
                        }
                    }
                }
                
                if (!retag && !redo.isEmpty()) {
                    for (String redoEntry : redo) {
                        if (textTagger.hasTag(joke.text, textTagger.tagList.get(redoEntry))) {
                            retag = true;
                            break;
                        }
                    }
                }
                
                if (retag) {
                    joke.tags = preTags.get(joke.hash);
                    joke.tags.addAll(textTagger.getTagsFromText(joke.text, joke.tags));
                }
                progressBar.addOne();
            }
            
            Jokes.outputJokes(taggedFile, jokes);
            progressBar.complete(true);
        }
    }
    
}
