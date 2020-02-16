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
     * A list of tags that have less aliases than before.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> tagHasLess = Arrays.asList("Gun", "Astrology", "Drug", "Beauty");
    
    /**
     * A list of tags that have more aliases than before
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static final List<String> tagHasMore = Arrays.asList("Drunk", "Star Wars", "Stealing", "Thief");
    
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
            Map<Long, List<String>> preTags = new HashMap<>();
            for (Joke preJoke : preJokes) {
                preTags.put(preJoke.hash, preJoke.tags);
            }
            
            List<Joke> jokes = Jokes.readJokes(taggedFile);
            progressBar.setTotal(jokes.size());
            
            jokes.parallelStream().forEach(joke -> {
                if (needsRetag(joke)) {
                    joke.tags = textTagger.getTagsFromText(joke.text, preTags.get(joke.hash));
                }
                progressBar.addOne();
            });
            
            Jokes.outputJokes(new File(taggedFile.getAbsolutePath().replace("tagged.json", "tagged-hotfix.json")), jokes);
            progressBar.complete();
        }
    }
    
    /**
     * Determines if a Joke needs to be retagged or not.
     *
     * @param joke The joke.
     * @return Whether or not the Joke needs to be retagged.
     */
    private static boolean needsRetag(Joke joke) {
        boolean retag = false;
        
        if (!tagHasLess.isEmpty()) {
            for (String tag : joke.tags) {
                if (tagHasLess.contains(tag)) {
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
        
        if (!retag && !tagHasMore.isEmpty()) {
            for (String tag : tagHasMore) {
                if (!joke.tags.contains(tag)) {
                    if (textTagger.hasTag(joke.text, textTagger.tagList.get(tag))) {
                        retag = true;
                        break;
                    }
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
        
        return retag;
    }
    
}
