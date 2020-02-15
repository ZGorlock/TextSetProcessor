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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public enum JokeSet {
        Quirkology("quirkology"),
        Jokeriot("jokeriot"),
        StupidStuff("stupidstuff"),
        Wocka("wocka"),
        Reddit("reddit");
        
        public File directory;
        
        JokeSet(String directoryName) {
            directory = new File("jokes/" + directoryName);
        }
    }
    
    /**
     * An enumeration of steps that can be performed during the processing.
     */
    public enum ProcessStep {
        PARSE("/source/1 - cleaned", "/source/2 - parsed/parsed.json"),
        FIX("/source/2 - parsed/parsed.json", "/source/3 - fixed/fixed.json"),
        TAG("/source/3 - fixed/fixed.json", "/source/4 - tagged/tagged.json"),
        COMPILE("/source/4 - tagged/tagged.json", "/<jokeSet>.json"),
        MERGE("/../~", "/../jokes.json");
        
        public String in;
        
        public String out;
        
        ProcessStep(String in, String out) {
            this.in = in;
            this.out = out;
        }
    }
    
    
    //Static Fields
    
    /**
     * A list of flags indicating whether or not to process the corresponding joke set in the joke set list.
     */
    private static final List<Boolean> doJokeSet = Arrays.asList(true, true, true, true, true);
    
    /**
     * A list of flags indicating whether or not to perform the corresponding joke processing step.
     */
    private static final List<Boolean> doProcessStep = Arrays.asList(true, true, true, true, true);
    
    /**
     * A flag indicating whether or not to perform a clean start.
     */
    private static final boolean doCleanStart = false;
    
    /**
     * A flag indicating whether or not to perform a fast start.
     */
    private static final boolean doFastStart = false;
    
    /**
     * The file to save the time of processing steps in.
     */
    private static final File timeFile = new File("etc/state/jokes.txt");
    
    /**
     * The file to save the time of processing steps in.
     */
    private static final Map<String, Long> timeData = new LinkedHashMap<>();
    
    /**
     * The total processing time.
     */
    private static long totalTime = 0L;
    
    /**
     * The number of available processor cores.
     */
    private static final int numCores = Runtime.getRuntime().availableProcessors();
    
    /**
     * The number of jokes to fix before saving the state.
     */
    private static final int fixChunkSize = 100 * numCores;
    
    /**
     * The number of jokes to tag before saving the state.
     */
    private static final int tagChunkSize = 20 * numCores;
    
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
        long startupStartTime = System.currentTimeMillis();
        
        if (doFastStart) {
            spellChecker.loadAdditionalDicts = false;
            textTagger.loadTagLists = false;
        }
        
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
        
        if (doCleanStart) {
            Filesystem.writeStringToFile(timeFile, "");
        }
        readTimeFile();
        
        long startupEndTime = System.currentTimeMillis();
        long startupTime = ((startupEndTime - startupStartTime) / 1000);
        
        totalTime += startupTime;
        System.out.println("Started Up in " + produceDurationString(startupTime));
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
        long parseTime = 0L;
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            long subParseTime = getSaveProcessTime(jokeSet, ProcessStep.PARSE) * 1000;
            File parsedFile = new File(jokeSet.directory + ProcessStep.PARSE.out);
            
            if (subParseTime < 0) {
                Filesystem.deleteDirectory(parsedFile.getParentFile());
                Filesystem.createDirectory(parsedFile.getParentFile());
                long subParseStartTime = System.currentTimeMillis();
                List<Joke> jokes = jokeParser.parseJokeSet(jokeSet);
                Filesystem.createDirectory(parsedFile.getParentFile());
                outputJokes(parsedFile, jokes);
                long subParseEndTime = System.currentTimeMillis();
                subParseTime = (subParseEndTime - subParseStartTime);
                setSaveProcessTime(jokeSet, ProcessStep.PARSE, (subParseTime / 1000));
            } else {
                progressBar.setInitialDuration(subParseTime / 1000);
            }
            parseTime += (subParseTime / 1000);
            int size = readJokes(parsedFile).size();
            
            progressBar.setTotal(size);
            progressBar.complete();
        }
        
        totalTime += parseTime;
        System.out.println("Parsed Jokes in " + produceDurationString(parseTime));
        System.out.println();
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
        long fixTime = 0L;
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            long subFixTime = getSaveProcessTime(jokeSet, ProcessStep.FIX) * 1000;
            File fixedFile = new File(jokeSet.directory + ProcessStep.FIX.out);
            File fixedWorkFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-work.json"));
            File fixedFixFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-fix.json"));
            File fixedFixListFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-fixList.json"));
            File fixedBackupFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-bak.json"));
            File fixedWorkBackupFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-work-bak.json"));
            File fixedFixBackupFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-fix-bak.json"));
            File fixedFixListBackupFile = new File(jokeSet.directory + ProcessStep.FIX.out.replace(".json", "-fixList-bak.json"));
            
            if (subFixTime < 0 || fixedWorkFile.exists()) {
                if (subFixTime < 0) {
                    Filesystem.deleteDirectory(fixedFile.getParentFile());
                    subFixTime = 0;
                }
                Filesystem.createDirectory(fixedFile.getParentFile());
                
                List<Joke> work;
                List<Joke> fixed = new ArrayList<>();
                if (fixedWorkFile.exists()) {
                    if (fixedFile.exists()) {
                        fixed = readJokes(fixedFile);
                    }
                    if (fixedFixFile.exists()) {
                        fixed.addAll(readJokes(fixedFixFile));
                    }
                    work = readJokes(fixedWorkFile);
                } else {
                    work = readJokes(new File(jokeSet.directory + ProcessStep.FIX.in));
                    outputJokes(fixedWorkFile, work);
                    outputJokes(fixedFile, fixed);
                }
                
                progressBar.setTotal(work.size() + fixed.size());
                progressBar.setInitialProgress(fixed.size());
                progressBar.setInitialDuration(subFixTime / 1000);
                progressBar.update(fixed.size());
                
                while (!work.isEmpty()) {
                    long chunkStartTime = System.currentTimeMillis();
                    List<Joke> currentWork = work.subList(0, Math.min(fixChunkSize, work.size()));
                    currentWork.parallelStream().forEach(joke -> Jokes.fixJoke(joke, progressBar));
                    fixed.addAll(currentWork);
                    currentWork.clear();
                    long chunkEndTime = System.currentTimeMillis();
                    long chunkTime = chunkEndTime - chunkStartTime;
                    subFixTime += chunkTime;
                    
                    outputJokes(fixedFile, fixed);
                    outputJokes(fixedWorkFile, work);
                    setSaveProcessTime(jokeSet, ProcessStep.FIX, (subFixTime / 1000));
                }
                Filesystem.deleteFile(fixedWorkFile);
                Filesystem.deleteFile(fixedBackupFile);
                Filesystem.deleteFile(fixedFixBackupFile);
                Filesystem.deleteFile(fixedFixListBackupFile);
                Filesystem.deleteFile(fixedWorkBackupFile);
                setSaveProcessTime(jokeSet, ProcessStep.FIX, (subFixTime / 1000));
            } else {
                progressBar.setInitialDuration(subFixTime / 1000);
            }
            fixTime += (subFixTime / 1000);
            int fixSize = fixedFixFile.exists() ? readJokes(fixedFixFile).size() : 0;
            int size = readJokes(fixedFile).size() + fixSize;
            
            progressBar.setTotal(size);
            progressBar.complete(true, (fixSize > 0 ? (": " + fixSize + " to fix ") : ""));
        }
        
        totalTime += fixTime;
        System.out.println("Fixed Jokes in " + produceDurationString(fixTime));
        System.out.println();
    }
    
    /**
     * Fixes a joke.
     *
     * @param joke        The joke to fix.
     * @param progressBar The progress bar.
     */
    public static void fixJoke(Joke joke, ConsoleProgressBar progressBar) {
        joke.text = textFixer.cleanText(joke.text);
        joke.fix.addAll(spellChecker.checkForSpelling(joke.text));
        joke.fix.sort(Comparator.naturalOrder());
        if (progressBar != null) {
            progressBar.addOne();
        }
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
        long tagTime = 0L;
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            long subTagTime = getSaveProcessTime(jokeSet, ProcessStep.TAG) * 1000;
            File taggedFile = new File(jokeSet.directory + ProcessStep.TAG.out);
            File taggedWorkFile = new File(jokeSet.directory + ProcessStep.TAG.out.replace(".json", "-work.json"));
            File taggedBackupFile = new File(jokeSet.directory + ProcessStep.TAG.out.replace(".json", "-bak.json"));
            File taggedWorkBackupFile = new File(jokeSet.directory + ProcessStep.TAG.out.replace(".json", "-work-bak.json"));
            
            if (subTagTime < 0 || taggedWorkFile.exists()) {
                if (subTagTime < 0) {
                    Filesystem.deleteDirectory(taggedFile.getParentFile());
                    subTagTime = 0;
                }
                Filesystem.createDirectory(taggedFile.getParentFile());
                
                List<Joke> work;
                List<Joke> tagged = new ArrayList<>();
                if (taggedWorkFile.exists()) {
                    if (taggedFile.exists()) {
                        tagged = readJokes(taggedFile);
                    }
                    work = readJokes(taggedWorkFile);
                } else {
                    work = readJokes(new File(jokeSet.directory + ProcessStep.TAG.in));
                    outputJokes(taggedWorkFile, work);
                    outputJokes(taggedFile, tagged);
                }
                
                progressBar.setTotal(work.size() + tagged.size());
                progressBar.setInitialProgress(tagged.size());
                progressBar.setInitialDuration(subTagTime / 1000);
                progressBar.update(tagged.size());
                
                while (!work.isEmpty()) {
                    long chunkStartTime = System.currentTimeMillis();
                    List<Joke> currentWork = work.subList(0, Math.min(tagChunkSize, work.size()));
                    currentWork.parallelStream().forEach(joke -> Jokes.tagJoke(joke, progressBar));
                    tagged.addAll(currentWork);
                    currentWork.clear();
                    long chunkEndTime = System.currentTimeMillis();
                    long chunkTime = chunkEndTime - chunkStartTime;
                    subTagTime += chunkTime;
                    
                    outputJokes(taggedFile, tagged);
                    outputJokes(taggedWorkFile, work);
                    setSaveProcessTime(jokeSet, ProcessStep.TAG, (subTagTime / 1000));
                }
                Filesystem.deleteFile(taggedWorkFile);
                Filesystem.deleteFile(taggedBackupFile);
                Filesystem.deleteFile(taggedWorkBackupFile);
                setSaveProcessTime(jokeSet, ProcessStep.TAG, (subTagTime / 1000));
            } else {
                progressBar.setInitialDuration(subTagTime / 1000);
            }
            tagTime += (subTagTime / 1000);
            int size = readJokes(taggedFile).size();
            
            progressBar.setTotal(size);
            progressBar.complete();
        }
        
        totalTime += tagTime;
        System.out.println("Tagged Jokes in " + produceDurationString(tagTime));
        System.out.println();
    }
    
    /**
     * Tags a joke.
     *
     * @param joke        The joke to tag.
     * @param progressBar The progress bar.
     */
    public static void tagJoke(Joke joke, ConsoleProgressBar progressBar) {
        joke.tags = textTagger.getTagsFromText(joke.text, joke.tags);
        joke.nsfw = joke.nsfw || nsfwChecker.checkNsfw(joke.text, joke.tags);
        if (progressBar != null) {
            progressBar.addOne();
        }
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
        long compileTime = 0L;
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            long subCompileTime = getSaveProcessTime(jokeSet, ProcessStep.COMPILE) * 1000;
            File compiledFileIn = new File(jokeSet.directory + ProcessStep.COMPILE.in);
            File compiledFileOut = new File(jokeSet.directory + ProcessStep.COMPILE.out.replace("<jokeSet>", jokeSet.name().toLowerCase()));
            
            if (subCompileTime < 0) {
                long subCompileStartTime = System.currentTimeMillis();
                Filesystem.copyFile(compiledFileIn, compiledFileOut);
                long subCompileEndTime = System.currentTimeMillis();
                subCompileTime = (subCompileEndTime - subCompileStartTime);
                setSaveProcessTime(jokeSet, ProcessStep.COMPILE, (subCompileTime / 1000));
            } else {
                progressBar.setInitialDuration(subCompileTime / 1000);
            }
            compileTime += (subCompileTime / 1000);
            int size = readJokes(compiledFileOut).size();
            
            progressBar.setTotal(size);
            progressBar.complete();
        }
        
        totalTime += compileTime;
        System.out.println("Compiled Jokes in " + produceDurationString(compileTime));
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
        long mergeTime = 0L;
        
        File mergedFile = new File(JokeSet.Quirkology.directory + ProcessStep.MERGE.out);
        List<Joke> jokes = mergedFile.exists() ? readJokes(mergedFile) : new ArrayList<>();
        
        for (JokeSet jokeSet : JokeSet.values()) {
            if (!doJokeSet.get(jokeSet.ordinal())) {
                continue;
            }
            
            ConsoleProgressBar progressBar = new ConsoleProgressBar(jokeSet.name(), 1, "jokes");
            progressBar.update(0);
            
            long subMergeTime = getSaveProcessTime(jokeSet, ProcessStep.MERGE) * 1000;
            File mergedFileIn = new File(jokeSet.directory + ProcessStep.COMPILE.out.replace("<jokeSet>", jokeSet.name().toLowerCase()));
            
            if (subMergeTime < 0) {
                long subMergeStartTime = System.currentTimeMillis();
                List<Joke> jokeSetJokes = readJokes(new File("jokes/" + jokeSet.name().toLowerCase() + "/" + jokeSet.name().toLowerCase() + ".json"));
                jokes.addAll(jokeSetJokes);
                long subMergeEndTime = System.currentTimeMillis();
                subMergeTime = (subMergeEndTime - subMergeStartTime);
                setSaveProcessTime(jokeSet, ProcessStep.MERGE, (subMergeTime / 1000));
            } else {
                progressBar.setInitialDuration(subMergeTime / 1000);
            }
            mergeTime += (subMergeTime / 1000);
            int size = readJokes(mergedFileIn).size();
            
            progressBar.setTotal(size);
            progressBar.complete();
        }
        
        Collections.shuffle(jokes);
        outputJokes(mergedFile, jokes);
        
        totalTime += mergeTime;
        System.out.println("Merged Jokes in " + produceDurationString(mergeTime));
        
        System.out.println();
        System.out.println("Complete... " + jokes.size() + " jokes (" + produceDurationString(totalTime) + ")");
    }
    
    
    //Functions
    
    /**
     * Outputs a list of jokes to a file.
     *
     * @param out      The file to output the jokes to.
     * @param jokes    The list of jokes to output.
     * @param splitFix Whether or not to split the jokes that need to be fixed into another file.
     */
    public static void outputJokes(File out, List<Joke> jokes, boolean splitFix) {
        int count = splitFix ? (int) jokes.stream().filter(e -> e.fix.isEmpty()).count() : jokes.size();
        List<String> text = new ArrayList<>();
        text.add("{");
        text.add("    \"count\": " + count + ",");
        text.add("    \"jokes\": [");
        boolean firstJoke = true;
        for (Joke joke : jokes) {
            if (splitFix && !joke.fix.isEmpty()) {
                continue;
            }
            if (!firstJoke) {
                text.set(text.size() - 1, text.get(text.size() - 1) + ",");
            }
            text.addAll(writeJoke(joke, !splitFix));
            firstJoke = false;
        }
        text.add("    ]");
        text.add("}");
        safeRewrite(out, text);
        
        if (!splitFix) {
            return;
        }
        
        File outFix = new File(out.getAbsolutePath().replace(".json", "-fix.json"));
        
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
            safeRewrite(outFix, fixText);
        } else {
            Filesystem.deleteFile(outFix);
        }
        
        File outFixList = new File(out.getAbsolutePath().replace(".json", "-fixList.txt"));
        if (fixCount > 0) {
            fix = ListUtility.sortListByNumberOfOccurrences(fix);
            safeRewrite(outFixList, fix);
        } else {
            Filesystem.deleteFile(outFixList);
        }
    }
    
    /**
     * Outputs a list of jokes to a file.
     *
     * @param out   The file to output the jokes to.
     * @param jokes The list of jokes to output.
     */
    public static void outputJokes(File out, List<Joke> jokes) {
        outputJokes(out, jokes, true);
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
        String jokeSource = joke.source.replaceAll("\\\\*\"", "\\\\\"").replaceAll("\\s+", " ");
        
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
        out.add("            \"source\": \"" + jokeSource + "\",");
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
    @SuppressWarnings("unchecked")
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
    
    /**
     * Performs a save rewrite of a file, saving backups to prevent data loss.
     *
     * @param out   The output file.
     * @param lines The lines to write to the output file.
     * @return Whether the rewrite was successful or not.
     */
    public static boolean safeRewrite(File out, List<String> lines) {
        String fileType = out.getName().substring(out.getName().lastIndexOf('.'));
        File outBackup = new File(out.getAbsolutePath().replace(fileType, "-bak" + fileType));
        File outBackupBackup = new File(out.getAbsolutePath().replace(fileType, "-bak-bak" + fileType));
        
        return (!out.exists() || !outBackup.exists() || Filesystem.copyFile(outBackup, outBackupBackup, true)) &&
                (!out.exists() || Filesystem.moveFile(out, outBackup, true)) &&
                (Filesystem.writeLines(out, lines)) &&
                (!outBackupBackup.exists() || Filesystem.deleteFile(outBackupBackup));
    }
    
    /**
     * Reads the time data from the time file.
     */
    public static void readTimeFile() {
        List<String> data = Filesystem.readLines(timeFile);
        for (String dataLine : data) {
            if (dataLine.isEmpty()) {
                continue;
            }
            String[] dataLineParts = dataLine.split(":");
            timeData.put(dataLineParts[0], Long.parseLong(dataLineParts[1]));
        }
    }
    
    /**
     * Gets the saved process time for a particular process step of a particular joke set.
     *
     * @param jokeSet     The joke set.
     * @param processStep The process step.
     * @return The saved process time for the particular process step of the particular joke set, or -1 if it should be reprocessed.
     */
    public static long getSaveProcessTime(JokeSet jokeSet, ProcessStep processStep) {
        String key = getSaveProcessKey(jokeSet, processStep);
        if (key.isEmpty() || !timeData.containsKey(key)) {
            return -1L;
        }
        return timeData.get(key);
    }
    
    /**
     * Gets the saved process key for a particular process step of a particular joke set.
     *
     * @param jokeSet     The joke set.
     * @param processStep The process step.
     * @return The saved process key for the particular process step of the particular joke set.
     */
    public static String getSaveProcessKey(JokeSet jokeSet, ProcessStep processStep) {
        String key = jokeSet.name() + "-" + processStep.name() + "-";
        File checkFile = new File(jokeSet.directory + processStep.in.replace("<jokeSet>", jokeSet.name().toLowerCase()));
        File outFile = new File(jokeSet.directory + processStep.out.replace("<jokeSet>", jokeSet.name().toLowerCase()));
        if (!outFile.exists() && processStep != ProcessStep.MERGE) {
            return "";
        }
        if (checkFile.getAbsolutePath().endsWith("~")) {
            List<File> checkFileList = Filesystem.getDirs(checkFile.getParentFile());
            long hashKey = 0L;
            for (File checkFileListEntry : checkFileList) {
                hashKey += Filesystem.checksum(checkFileListEntry);
                hashKey %= Long.MAX_VALUE;
            }
            key += hashKey;
        } else {
            key += Filesystem.checksum(checkFile);
        }
        return key;
    }
    
    /**
     * Sets the saved process time for a particular process step of a particular joke set.
     *
     * @param jokeSet     The joke set.
     * @param processStep The process step.
     */
    public static void setSaveProcessTime(JokeSet jokeSet, ProcessStep processStep, long time) {
        String key = getSaveProcessKey(jokeSet, processStep);
        timeData.put(key, time);
        writeTimeFile();
    }
    
    /**
     * Writes the time data to the time file.
     */
    public static void writeTimeFile() {
        File timeFileBackup = new File(timeFile.getAbsolutePath().replace(".txt", "-bak.txt"));
        List<String> data = new ArrayList<>();
        for (Map.Entry<String, Long> timeDataEntry : timeData.entrySet()) {
            data.add(timeDataEntry.getKey() + ":" + timeDataEntry.getValue());
        }
        safeRewrite(timeFile, data);
    }
    
    /**
     * Produces a duration string.
     *
     * @param duration The duration in seconds.
     * @return The duration string.
     */
    public static String produceDurationString(long duration) {
        long totalSeconds = duration;
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalMinutes / 60;
        long totalDays = totalHours / 24;
        totalHours %= 24;
        totalMinutes %= 60;
        totalSeconds %= 60;
        String totalDuration =
                ((totalDays > 0) ? totalDays + "d " : "") +
                        ((totalDays > 0 || totalHours > 0) ? totalHours + "h " : "") +
                        ((totalDays > 0 || totalHours > 0 || totalMinutes > 0) ? totalMinutes + "m " : "") +
                        totalSeconds + "s";
        totalDuration = StringUtility.trim(totalDuration);
        return totalDuration;
    }
    
}
