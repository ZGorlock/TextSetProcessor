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
import worker.TextTagger;

/**
 * Helps with hotfixing tagged jokes.
 */
public class TagHotfixer {
    
    //Static Fields
    
    /**
     * A list of tags to hotfix.
     */
    private static final List<String> redo = Arrays.asList("Airplane", "Anal Sex", "Anatomy", "Archaeology", "Arizona", "Army", "Asian", "Astrology", "Aviation", "Bad Language", "British", "Butt", "Cancer", "Car", "Childbirth", "Children", "Clubhouse", "Court", "Dead Baby", "Disability", "Disappointment", "Driving", "Drug", "Dwarf", "Earth", "Eating", "Egypt", "Electricity", "England", "English", "Family", "Fitness", "Genetics", "Geology", "Geometry", "Gun", "Health", "Heart", "Helicopter", "Herptile", "Hitler", "Horse", "Hospital", "Housework", "Invisibility", "Jail", "Jewish", "King", "Kung Fu", "Law", "Lifeguard", "Lying", "Mail", "Make a Wish", "Mammal", "Mechanic", "Medical", "Medicine", "Military", "Mirror", "Native American", "Nazi", "News", "Oral Sex", "PTSD", "Pepsi", "Period", "Pessimist", "Politics", "Pregnancy", "Profanity", "Prostitution", "Rape", "Rejection", "Religion", "Repairman", "Reporter", "Reptile", "Rich", "River", "Royalty", "Sarcastic", "School", "Scottish", "Slut", "Snail", "Snow", "Soda", "Speech Impediment", "Sport", "Student Loans", "Swimming", "Train", "Turtle", "Ugly", "Urination", "Vaccination", "Water", "Weather", "Work", "World War II");
    
    /**
     * A list of initial tags to hotfix.
     */
    private static final List<String> redoInitial = Arrays.asList("Snow", "Water", "Weather");
    
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
            
            List<Joke> preJokes = Jokes.readJokes(fixedFile);
            Map<Integer, List<String>> preTags = new HashMap<>();
            for (Joke preJoke : preJokes) {
                preTags.put(preJoke.hash, preJoke.tags);
            }
            
            List<Joke> jokes = Jokes.readJokes(taggedFile);
            for (Joke joke : jokes) {
                boolean retag = false;
                
                if (!redoInitial.isEmpty()) {
                    List<String> initialTags = textTagger.getInitialTags(joke.text);
                    for (String redoInitialEntry : redoInitial) {
                        if (initialTags.contains(redoInitialEntry)) {
                            retag = true;
                            break;
                        }
                    }
                }
                
                if (!retag) {
                    for (String redoEntry : redo) {
                        if (textTagger.hasTag(joke.text, textTagger.tagList.get(redoEntry))) {
                            retag = true;
                            break;
                        }
                    }
                }
                
                if (retag) {
                    joke.tags = preTags.get(joke.hash);
                    joke.tags.addAll(textTagger.getTagsFromText(joke.text));
                }
            }
            
            Jokes.outputJokes(taggedFile, jokes);
        }
    }
    
}
