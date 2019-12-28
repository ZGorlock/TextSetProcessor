/*
 * File:    Jokes.java
 * Package: main
 * Author:  Zachary Gill
 */

package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import parser.JokeParser;
import pojo.Joke;
import resource.ConsoleProgressBar;
import utility.Filesystem;
import utility.ListUtility;
import utility.StringUtility;
import worker.NsfwChecker;
import worker.SpellChecker;
import worker.TextFixer;
import worker.TextTagger;

/**
 * Processes Jokes.
 */
public class Jokes {
    
    //Enums
    
    
    /**
     * An enumerations of joke sets that can be processed.
     */
    private enum JokeSet {
        Quirkology,
        Jokeriot,
        StupidStuff,
        Wocka,
        Reddit
    }
    
    /**
     * An enumeration of steps that can be performed during the processing.
     */
    private enum ProcessStep {
        PARSE,
        FIX,
        TAG,
        COMPILE,
        MERGE
    }
    
    
    //Static Fields
    
    /**
     * A list of flags indicating whether or not to process the corresponding joke set in the joke set list.
     */
    private static final List<Boolean> doJokeSet = Arrays.asList(false, false, false, false, true);
    
    /**
     * A list of flags indicating whether or not to perform the corresponding joke processing step.
     */
    private static final List<Boolean> doProcessStep = Arrays.asList(true, true, false, false, false);
    
    /**
     * The number of jokes to tag before saving the state.
     */
    private static final int tagChunkSize = 100;
    
    /**
     * The timestamp of the startup time of the Joke Processor.
     */
    private static final long startupTime = System.currentTimeMillis();
    
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
        setup();
        processJokes();
    }
    
    /**
     * Sets up the Joke Processor.
     */
    private static void setup() {
        System.out.println("Starting Up...");
        if (doProcessStep.get(ProcessStep.PARSE.ordinal())) {
            jokeParser.load();
        }
        
        
        if (doProcessStep.get(ProcessStep.FIX.ordinal())) {
            textTagger.load();
            textFixer.load();
            spellChecker.load();
        }
        if (doProcessStep.get(ProcessStep.TAG.ordinal())) {
            textTagger.load();
            nsfwChecker.load();
        }
        System.out.println("Started Up in " + ((System.currentTimeMillis() - startupTime) / 1000) + "s");
        System.out.println();
        
    }
    
    /**
     * Processes the jokes.
     */
    private static void processJokes() {
        parseJokes();
        fixJokes();
        tagJokes();
        compileJokes();
        mergeJokes();
    }
    
    
    //Parse Jokes
    
    /**
     * Parses the jokes.
     */
    private static void parseJokes() {
        if (!doProcessStep.get(ProcessStep.PARSE.ordinal())) {
            return;
        }
        
        System.out.println("Parsing Jokes...");
        long startTime = System.currentTimeMillis();
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            long subStartTime = System.currentTimeMillis();
            List<Joke> jokes = parseJokeSet(jokeSet.name());
            File parsedDirectory = new File("jokes/" + jokeSet.name().toLowerCase() + "/source/2 - parsed");
            Filesystem.createDirectory(parsedDirectory);
            outputJokes(new File(parsedDirectory, "parsed.json"), jokes);
            progressBar.total = jokes.size();
            progressBar.update(jokes.size());
            progressBar.print();
            System.out.println(" (" + ((System.currentTimeMillis() - subStartTime) / 1000) + "s)");
        }
        
        System.out.println("Parsed Jokes in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        System.out.println();
    }
    
    /**
     * Parses a joke set.
     *
     * @param jokeSet The name of the joke set to process.
     * @return The list of jokes parsed from the joke set.
     */
    private static List<Joke> parseJokeSet(String jokeSet) {
        switch (jokeSet) {
            case "Quirkology":
                return jokeParser.parseQuirkology();
            case "Jokeriot":
                return jokeParser.parseJokeriot();
            case "StupidStuff":
                return jokeParser.parseStupidStuff();
            case "Wocka":
                return jokeParser.parseWocka();
            case "Reddit":
                return jokeParser.parseReddit();
            default:
                return new ArrayList<>();
        }
    }
    
    
    //Fix Jokes
    
    /**
     * Fixes the jokes.
     */
    private static void fixJokes() {
        if (!doProcessStep.get(ProcessStep.FIX.ordinal())) {
            return;
        }
        
        System.out.println("Fixing Jokes...");
        long startTime = System.currentTimeMillis();
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            long subStartTime = System.currentTimeMillis();
            List<Joke> jokes = readJokes(new File("jokes/" + jokeSet.name().toLowerCase() + "/source/2 - parsed/parsed.json"));
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), jokes.size(), "jokes");
            progressBar.update(0);
            jokes.parallelStream().forEach(e -> fixJoke(e, progressBar));
            File fixedDirectory = new File("jokes/" + jokeSet.name().toLowerCase() + "/source/3 - fixed");
            Filesystem.createDirectory(fixedDirectory);
            int fixCount = outputJokes(new File(fixedDirectory, "fixed.json"), jokes);
            progressBar.update(jokes.size());
            progressBar.print();
            System.out.println(" " + (fixCount > 0 ? (": " + fixCount + " to fix ") : "") + "(" + ((System.currentTimeMillis() - subStartTime) / 1000) + "s)");
        }
        
        System.out.println("Fixed Jokes in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        System.out.println();
    }
    
    /**
     * Fixes a joke.
     *
     * @param joke The joke to fix.
     */
    private static void fixJoke(Joke joke, ConsoleProgressBar progressBar) {
        joke.text = textFixer.cleanText(joke.text);
        joke.fix.addAll(spellChecker.checkForSpelling(joke.text));
        joke.fix.sort(Comparator.naturalOrder());
        progressBar.addOne();
    }
    
    
    //Tag Jokes
    
    /**
     * Tags the jokes.
     */
    private static void tagJokes() {
        if (!doProcessStep.get(ProcessStep.TAG.ordinal())) {
            return;
        }
        
        System.out.println("Tagging Jokes...");
        long startTime = System.currentTimeMillis();
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            long duration = 0;
            
            File taggedDirectory = new File("jokes/" + jokeSet.name().toLowerCase() + "/source/4 - tagged");
            Filesystem.createDirectory(taggedDirectory);
            File taggedFile = new File(taggedDirectory, "tagged.json");
            File taggedWorkFile = new File(taggedDirectory, "tagged-work.json");
            File taggedTimeFile = new File(taggedDirectory, "tagged-time.json");
            
            List<Joke> work = new ArrayList<>();
            List<Joke> tagged = new ArrayList<>();
            if (taggedFile.exists()) {
                tagged = readJokes(taggedFile);
                if (taggedWorkFile.exists()) {
                    work = readJokes(taggedWorkFile);
                }
                if (taggedTimeFile.exists()) {
                    duration = Long.valueOf(Filesystem.readFileToString(taggedTimeFile));
                } else {
                    Filesystem.writeStringToFile(taggedTimeFile, "0");
                }
            } else {
                work = readJokes(new File("jokes/" + jokeSet.name().toLowerCase() + "/source/3 - fixed/fixed.json"));
                outputJokes(taggedFile, tagged);
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), work.size() + tagged.size(), "jokes");
            progressBar.update(tagged.size());
            
            List<Integer> hashes = new ArrayList<>();
            for (Joke joke : tagged) {
                hashes.add(joke.hash);
            }
            
            while (!work.isEmpty()) {
                long chunkStartTime = System.currentTimeMillis();
                int count = 0;
                for (int i = 0; i < tagChunkSize; i++) {
                    if (work.size() <= i) {
                        break;
                    }
                    Joke joke = work.get(i);
                    if (hashes.contains(joke.hash)) {
                        continue;
                    }
                    tagJoke(joke);
                    progressBar.addOne();
                    count++;
                    tagged.add(joke);
                    hashes.add(joke.hash);
                }
                work = work.subList(count, work.size());
                
                outputJokes(taggedFile, tagged);
                outputJokes(taggedWorkFile, work);
                
                long chunkDuration = System.currentTimeMillis() - chunkStartTime;
                duration += chunkDuration;
                Filesystem.writeStringToFile(taggedTimeFile, String.valueOf(duration));
                System.out.println(jokeSet + "... Tagged " + tagged.size() + "/" + (tagged.size() + work.size()) + " jokes (" + (chunkDuration / 1000) + "s)");
            }
            Filesystem.deleteFile(taggedWorkFile);
            Filesystem.deleteFile(taggedTimeFile);
            
            progressBar.update(tagged.size());
            progressBar.print();
            System.out.println(jokeSet + "... " + tagged.size() + " jokes (" + (duration / 1000) + "s)");
        }
        
        System.out.println("Tagged Jokes in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        System.out.println();
    }
    
    /**
     * Tags a joke.
     *
     * @param joke The joke to tag.
     */
    private static void tagJoke(Joke joke) {
        joke.tags.addAll(textTagger.getTagsFromText(joke.text));
        joke.nsfw = joke.nsfw || nsfwChecker.checkNsfw(joke.text, joke.tags);
    }
    
    
    //Compile Jokes
    
    /**
     * Compiles the jokes.
     */
    private static void compileJokes() {
        if (!doProcessStep.get(ProcessStep.COMPILE.ordinal())) {
            return;
        }
        
        System.out.println("Compiling Jokes...");
        long startTime = System.currentTimeMillis();
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            long subStartTime = System.currentTimeMillis();
            int size = readJokes(new File("jokes/" + jokeSet.name().toLowerCase() + "/4 - tagged/tagged.json")).size();
            Filesystem.copyFile(new File("jokes/" + jokeSet.name().toLowerCase() + "/4 - tagged/tagged.json"), 
                    new File("jokes/" + jokeSet.name().toLowerCase() + "/" + jokeSet.name().toLowerCase() + ".json"));
            progressBar.total = size;
            progressBar.update(size);
            progressBar.print();
            System.out.println(" (" + ((System.currentTimeMillis() - subStartTime) / 1000) + "s)");
        }
        
        System.out.println("Compiled Jokes in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        System.out.println();
    }
    
    
    //Merge Jokes
    
    /**
     * Merges the jokes.
     */
    private static void mergeJokes() {
        if (!doProcessStep.get(ProcessStep.MERGE.ordinal())) {
            return;
        }
        
        System.out.println("Merging Jokes...");
        long startTime = System.currentTimeMillis();
        
        List<Joke> jokes = new ArrayList<>();
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            long subStartTime = System.currentTimeMillis();
            List<Joke> jokeSetJokes = readJokes(new File("jokes/" + jokeSet.name().toLowerCase() + "/" + jokeSet.name().toLowerCase() + ".json"));
            jokes.addAll(jokeSetJokes);
            progressBar.total = jokeSetJokes.size();
            progressBar.update(jokeSetJokes.size());
            progressBar.print();
            System.out.println(" (" + ((System.currentTimeMillis() - subStartTime) / 1000) + "s)");
        }
        
        Collections.shuffle(jokes);
        outputJokes(new File("jokes/jokes.json"), jokes);
        
        System.out.println("Compiled Jokes in " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        System.out.println();
        System.out.println("Complete... " + jokes.size() + " jokes");
    }
    
    
    //Functions
    
    /**
     * Outputs a list of jokes to a file.
     *
     * @param out   The file to output the jokes to.
     * @param jokes The list of jokes to output.
     * @return The number of jokes in the list that still need to be fixed.
     */
    public static int outputJokes(File out, List<Joke> jokes) {
        List<String> text = new ArrayList<>();
        text.add("{");
        text.add("    \"count\": " + jokes.size() + ",");
        text.add("    \"jokes\": [");
        boolean firstJoke = true;
        for (Joke joke : jokes) {
            if (!joke.fix.isEmpty()) {
                continue;
            }
            if (!firstJoke) {
                text.set(text.size() - 1, text.get(text.size() - 1) + ",");
            }
            text.addAll(writeJoke(joke, false));
            firstJoke = false;
        }
        text.add("    ]");
        text.add("}");
        Filesystem.writeLines(out, text);
        
        File outFix = new File(out.getAbsolutePath().replace(".json", "-fix.json"));
        Filesystem.deleteFile(outFix);
    
        List<Joke> jokeFix = jokes.stream().filter(j -> !j.fix.isEmpty()).sorted(Comparator.comparingInt(j -> -j.fix.size())).collect(Collectors.toList());
        List<String> fix = new ArrayList<>();
        List<String> fixText = new ArrayList<>();
        fixText.add("{");
        fixText.add("    \"count\": " + jokeFix.size() + ",");
        fixText.add("    \"jokes\": [");
        int fixCount = 0;
        for (Joke joke : jokeFix) {
            fix.addAll(joke.fix);
            if (fixCount > 0) {
                fixText.set(fixText.size() - 1, fixText.get(fixText.size() - 1) + ",");
            }
            fixText.addAll(writeJoke(joke, true));
            fixCount++;
        }
        fixText.add("    ]");
        fixText.add("}");
        if (fixCount > 0) {
            Filesystem.writeLines(outFix, fixText);
        }
        
        File outFixList = new File(out.getAbsolutePath().replace(".json", "-fixList.txt"));
        Filesystem.deleteFile(outFixList);
        if (fixCount > 0) {
            fix = ListUtility.sortListByNumberOfOccurrences(fix);
            Filesystem.writeLines(outFixList, fix);
        }
        
        return fixCount;
    }
    
    /**
     * Writes a joke to a list of strings.
     *
     * @param joke The joke to write.
     * @param fix  Whether or not to include the 'fix' element in the output.
     * @return The joke written as a list of strings.
     */
    public static List<String> writeJoke(Joke joke, boolean fix) {
        List<String> out = new ArrayList<>();
        String jokeText = joke.text.replaceAll("\\\\*\"", "\\\\\"").replaceAll("\\s+", " ");
        
        out.add("        {");
        out.add("            \"joke\": \"" + jokeText + "\",");
        if (fix && !joke.fix.isEmpty()) {
            StringBuilder fixString = new StringBuilder();
            for (int i = 0; i < joke.fix.size(); i++) {
                fixString.append(joke.fix.get(i)).append((i < joke.fix.size() - 1) ? ", " : "");
            }
            out.add("            \"fix\": \"" + fixString.toString() + "\",");
        }
        out.add("            \"length\": " + jokeText.length() + ",");
        out.add("            \"source\": \"" + joke.source + "\",");
        out.add("            \"nsfw\": " + (joke.nsfw ? "true" : "false") + ",");
        StringBuilder tagString = new StringBuilder();
        joke.tags = ListUtility.removeDuplicates(joke.tags);
        joke.tags.sort(Comparator.naturalOrder());
        for (int i = 0; i < joke.tags.size(); i++) {
            tagString.append("\"").append(joke.tags.get(i)).append("\"").append((i < joke.tags.size() - 1) ? ", " : "");
        }
        out.add("            \"tags\": [" + tagString.toString() + "]");
        out.add("        }");
        
        return out;
    }
    
    /**
     * Reads a list of jokes from a file.
     *
     * @param in The file to input the jokes from.
     * @return The list of jokes read from the file.
     */
    public static List<Joke> readJokes(File in) {
        List<Joke> jokes = new ArrayList<>();
        
        JSONParser parser = new JSONParser();
        try {
            JSONObject jokesJson = (JSONObject) parser.parse(new FileReader(in));
            for (Object jokeObject : (JSONArray) jokesJson.get("jokes")) {
                JSONObject jokeJson = (JSONObject) jokeObject;
                Joke joke = new Joke();
                joke.text = (String) jokeJson.get("joke");
                joke.length = (Long) jokeJson.get("length");
                joke.source = (String) jokeJson.get("source");
                joke.nsfw = (Boolean) jokeJson.get("nsfw");
                joke.tags = (List<String>) jokeJson.get("tags");
                
                joke.fix = new ArrayList<>();
                if (jokeJson.containsKey("fix")) {
                    for (String toFix : ((String) jokeJson.get("fix")).replace("\"", "\\\"").split(",")) {
                        joke.fix.add(StringUtility.trim(toFix));
                    }
                }
                
                joke.hash = joke.text.hashCode();
                
                jokes.add(joke);
            }
            
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        
        return jokes;
    }
    
}
