/*
 * File:    JokeParser.java
 * Package: parser
 * Author:  Zachary Gill
 */

package parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Jokes;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import pojo.Joke;
import utility.Filesystem;
import utility.StringUtility;
import worker.TextTagger;

/**
 * Parses Jokes.
 */
public final class JokeParser {
    
    //Static Fields
    
    /**
     * The singleton instance of the Joke Parser.
     */
    private static JokeParser instance = null;
    
    /**
     * A flag indicating whether or not the Joke Parser has been loaded yet or not.
     */
    private static AtomicBoolean loaded = new AtomicBoolean(false);
    
    
    //Fields
    
    /**
     * The reference to the Text Tagger.
     */
    private TextTagger textTagger;
    
    /**
     * A flag indicating whether or not to preserve the text source.
     */
    public boolean preserveSource = false;
    
    
    //Constructors
    
    /**
     * The private constructor for the Joke Parser.
     */
    private JokeParser() {
    }
    
    /**
     * Returns the singleton instance of the Joke Parser.
     *
     * @return The singleton instance of the Joke Parser.
     */
    public static JokeParser getInstance() {
        if (instance == null) {
            instance = new JokeParser();
        }
        return instance;
    }
    
    /**
     * Loads the Joke Parser.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        
        System.out.print("Loading Joke Parser... ");
        
        textTagger = TextTagger.getInstance();
        
        System.out.println();
    }
    
    
    //Methods
    
    /**
     * Parses a joke set.
     *
     * @param jokeSet The joke set to process.
     * @return The list of jokes parsed from the joke set.
     */
    public List<Joke> parseJokeSet(Jokes.JokeSet jokeSet) {
        switch (jokeSet) {
            case Quirkology:
                return parseQuirkology();
            case Jokeriot:
                return parseJokeriot();
            case StupidStuff:
                return parseStupidStuff();
            case Wocka:
                return parseWocka();
            case Reddit:
                return parseReddit();
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Rewrites a joke set.
     *
     * @param jokeSet The joke set to process.
     * @param jokes   The list of jokes to write.
     */
    public void writeJokeSet(Jokes.JokeSet jokeSet, List<Joke> jokes) {
        switch (jokeSet) {
            case Quirkology:
                writeQuirkology(jokes);
                break;
            case Jokeriot:
                writeJokeriot(jokes);
                break;
            case StupidStuff:
                writeStupidStuff(jokes);
                break;
            case Wocka:
                writeWocka(jokes);
                break;
            case Reddit:
                writeReddit(jokes);
                break;
        }
    }
    
    /**
     * Parses jokes from Quirkology.
     *
     * @return The list of jokes parsed from Quirkology.
     */
    public List<Joke> parseQuirkology() {
        File in = new File("jokes/quirkology/source/1 - cleaned/cleaned.txt");
        String source = "Quirkology";
        boolean rewrite = false;
        
        List<Joke> jokes = new ArrayList<>();
        
        List<String> lines = Filesystem.readLines(in);
        StringBuilder jokeText = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                if (jokeText.length() > 0) {
                    Joke thisJoke = new Joke();
                    thisJoke.text = jokeText.toString();
                    thisJoke.source = preserveSource ? jokeText.toString() : source;
                    jokes.add(thisJoke);
                    
                    jokeText = new StringBuilder();
                }
                
            } else {
                if (jokeText.length() > 0) {
                    jokeText.append(" ");
                }
                jokeText.append(line);
            }
        }
        
        if (rewrite) {
            writeQuirkology(jokes, false);
        }
        
        return jokes;
    }
    
    /**
     * Writes jokes back to Quirkology.
     *
     * @param jokes The list of jokes parsed from Quirkology.
     * @param work  Whether or not the jokes should be written back to the work file.
     */
    private void writeQuirkology(List<Joke> jokes, boolean work) {
        File out = new File("jokes/quirkology/source/1 - cleaned/cleaned" + (work ? "-work" : "") + ".txt");
        List<String> output = new ArrayList<>();
        for (Joke joke : jokes) {
            output.add(joke.source);
            output.add("");
        }
        Filesystem.writeLines(out, output);
    }
    
    /**
     * Writes jokes back to Quirkology.
     *
     * @param jokes The list of jokes parsed from Quirkology.
     */
    public void writeQuirkology(List<Joke> jokes) {
        if (!preserveSource) {
            return;
        }
        writeQuirkology(jokes, true);
    }
    
    /**
     * Parses jokes from Jokeriot.
     *
     * @return The list of jokes parsed from Jokeriot.
     */
    public List<Joke> parseJokeriot() {
        String source = "Jokeriot";
        boolean rewrite = false;
        
        List<Joke> jokes = new ArrayList<>();
        Map<Integer, Joke> hashes = new HashMap<>();
        
        List<Character> punctuation = Arrays.asList('.', '!', '?', ';', ':', ',');
        Pattern textGetter = Pattern.compile("\\s*<p>(?<text>.*)</p>\\s*");
        Pattern textGetterAlt1 = Pattern.compile("\\s*<p>(?<text>.*)");
        Pattern textGetterAlt2 = Pattern.compile("\\s*<br>(?<text>.*)");
        Pattern tagGetter = Pattern.compile("\\s*<li><a[^>]*>(?<tag>.+)</a></li>\\s*");
        
        boolean nsfw = true;
        do {
            nsfw = !nsfw;
            File dir = new File("jokes/jokeriot/source/1 - cleaned/" + (nsfw ? "nsfw/" : ""));
            
            for (File in : Filesystem.listFiles(dir, File::isFile)) {
                String type = in.getName().toUpperCase().replace(".HTML", "");
                
                List<String> lines = Filesystem.readLines(in);
                
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    
                    if (line.contains("itemprop=\"text\"")) {
                        StringBuilder jokeText = new StringBuilder();
                        
                        for (i = i + 1; i < lines.size(); i++) {
                            line = lines.get(i);
                            Matcher textMatcher = textGetter.matcher(line);
                            if (textMatcher.matches()) {
                                String text = textMatcher.group("text");
                                if (!text.isEmpty()) {
                                    if (jokeText.length() > 0) {
                                        jokeText.append(" ");
                                    }
                                    text = StringUtility.trim(text.replaceAll("</?em>", "").replaceAll("</?strong>", ""));
                                    for (int j = text.length() - 1; j >= 0; j--) {
                                        if (punctuation.contains(text.charAt(j))) {
                                            break;
                                        } else if (text.charAt(j) != '"' && text.charAt(j) != '\'') {
                                            text = text.substring(0, j + 1) + '.' + text.substring(j + 1);
                                            break;
                                        }
                                    }
                                    jokeText.append(text);
                                }
                            } else {
                                Matcher textAltMatcher = textGetterAlt1.matcher(line);
                                if (textAltMatcher.matches()) {
                                    String text = textAltMatcher.group("text");
                                    if (!text.isEmpty()) {
                                        if (jokeText.length() > 0) {
                                            jokeText.append(" ");
                                        }
                                        text = StringUtility.trim(text.replaceAll("</?em>", "").replaceAll("</?strong>", ""));
                                        for (int j = text.length() - 1; j >= 0; j--) {
                                            if (punctuation.contains(text.charAt(j))) {
                                                break;
                                            } else if (text.charAt(j) != '"' && text.charAt(j) != '\'') {
                                                text = text.substring(0, j + 1) + '.' + text.substring(j + 1);
                                                break;
                                            }
                                        }
                                        jokeText.append(text);
                                    }
                                    for (i = i + 1; i < lines.size(); i++) {
                                        line = lines.get(i);
                                        Matcher textAlt2Matcher = textGetterAlt2.matcher(line);
                                        if (textAlt2Matcher.matches()) {
                                            text = textAlt2Matcher.group("text");
                                            if (!text.isEmpty()) {
                                                if (jokeText.length() > 0) {
                                                    jokeText.append(" ");
                                                }
                                                String jokeLine = StringUtility.trim(text);
                                                boolean endJoke = jokeLine.endsWith("</p>");
                                                if (endJoke) {
                                                    jokeLine = StringUtility.rShear(jokeLine, 4);
                                                }
                                                jokeLine = StringUtility.trim(jokeLine.replaceAll("</?em>", "").replaceAll("</?strong>", ""));
                                                for (int j = jokeLine.length() - 1; j >= 0; j--) {
                                                    if (punctuation.contains(jokeLine.charAt(j))) {
                                                        break;
                                                    } else if (jokeLine.charAt(j) != '"' && jokeLine.charAt(j) != '\'') {
                                                        jokeLine = jokeLine.substring(0, j + 1) + '.' + jokeLine.substring(j + 1);
                                                        break;
                                                    }
                                                }
                                                jokeText.append(jokeLine);
                                                if (endJoke) {
                                                    break;
                                                }
                                            }
                                        }
                                        if (line.contains("</p>")) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (line.contains("</div>")) {
                                break;
                            }
                        }
                        
                        if (jokeText.length() == 0) {
                            continue;
                        }
                        Joke thisJoke = new Joke();
                        thisJoke.text = jokeText.toString();
                        thisJoke.source = preserveSource ? jokeText.toString() : source;
                        thisJoke.nsfw = nsfw;
                        
                        for (i = i + 1; i < lines.size(); i++) {
                            line = lines.get(i);
                            if (line.contains("</div>")) {
                                break;
                            }
                            if (line.contains("<div class=\"tag-list-container-r0jbeo\"><span>Tags:</span>")) {
                                for (i = i + 1; i < lines.size(); i++) {
                                    line = lines.get(i);
                                    if (line.contains("class=\"tag-list-sojdneob\"")) {
                                        for (i = i + 1; i < lines.size(); i++) {
                                            line = lines.get(i);
                                            Matcher tagMatcher = tagGetter.matcher(line);
                                            if (tagMatcher.matches()) {
                                                String tag = tagMatcher.group("tag");
                                                if (!tag.isEmpty()) {
                                                    String extractedTag = StringUtility.toTitleCase(StringUtility.trim(StringUtility.removePunctuation(tag)));
                                                    if (textTagger.tagList.containsKey(extractedTag)) {
                                                        thisJoke.tags.add(extractedTag);
                                                    }
                                                }
                                            }
                                            if (line.contains("</ul>")) {
                                                break;
                                            }
                                        }
                                        if (line.contains("</ul>")) {
                                            break;
                                        }
                                    }
                                }
                                if (line.contains("</ul>")) {
                                    break;
                                }
                            }
                        }
                        
                        List<String> typeTags = new ArrayList<>();
                        switch (type) {
                            case "ANTI":
                                typeTags.add("Anti Joke");
                                break;
                            case "ONE LINE":
                                typeTags.add("One Liner");
                                break;
                            case "TWO LINE":
                                typeTags.add("Two Liner");
                                break;
                            case "PICKUP LINE":
                                typeTags.add("Pickup Line");
                                break;
                            case "PUN":
                                typeTags.add("Pun");
                                break;
                            case "QUESTION AND ANSWER":
                                typeTags.add("Question and Answer");
                                break;
                            case "KNOCK KNOCK":
                                typeTags.add("Knock Knock");
                                break;
                            case "SERIES":
//                                typeTags.add("Series");
                                break;
                            case "STORY":
//                                typeTags.add("Story");
                                break;
                            case "YO MAMA":
                                typeTags.add("Yo Mama");
                                break;
                        }
                        thisJoke.tags.addAll(typeTags);
                        thisJoke.hash = StringUtility.removePunctuation(StringUtility.removeWhiteSpace(thisJoke.text)).toUpperCase().hashCode();
                        
                        if (hashes.containsKey(thisJoke.hash)) {
                            hashes.get(thisJoke.hash).tags.addAll(thisJoke.tags);
                        } else {
                            jokes.add(thisJoke);
                            hashes.put(thisJoke.hash, thisJoke);
                        }
                    }
                    
                }
            }
        } while (!nsfw);
        
        if (rewrite) {
            writeJokeriot(jokes, false);
        }
        
        return jokes;
    }
    
    /**
     * Writes jokes back to Jokeriot.
     *
     * @param jokes The list of jokes parsed from Jokeriot.
     * @param work  Whether or not the jokes should be written back to the work file.
     */
    private void writeJokeriot(List<Joke> jokes, boolean work) {
    }
    
    /**
     * Writes jokes back to Jokeriot.
     *
     * @param jokes The list of jokes parsed from Jokeriot.
     */
    public void writeJokeriot(List<Joke> jokes) {
        if (!preserveSource) {
            return;
        }
        writeJokeriot(jokes, true);
    }
    
    /**
     * Parses jokes from StupidStuff.
     *
     * @return The list of jokes parsed from StupidStuff.
     */
    public List<Joke> parseStupidStuff() {
        File in = new File("jokes/stupidstuff/source/1 - cleaned/cleaned.json");
        String source = "StupidStuff";
        boolean rewrite = false;
        
        List<Joke> jokes = new ArrayList<>();
        Map<Integer, Joke> hashes = new HashMap<>();
        
        JSONParser p = new JSONParser();
        try {
            JSONArray o = (JSONArray) p.parse(new FileReader(in));
            
            for (Object i : o) {
                JSONObject io = (JSONObject) i;
                String body = StringUtility.trim((String) io.get("body"));
                String category = ((String) io.get("category"));
                String save = body + "|||||" + category;
                
                if (body.isEmpty() || body.endsWith(":") || body.contains("1.") || body.contains("B.)") ||
                    body.contains("2)") || body.toUpperCase().contains("STUPIDSTUFF") ||
                    body.toUpperCase().contains("JOKE") || body.toUpperCase().contains("THIS SITE")) {
                    continue;
                }
                
                if (body.endsWith(",")) {
                    body = StringUtility.rShear(body, 1);
                }
                
                Joke thisJoke = new Joke();
                thisJoke.text = body;
                thisJoke.source = preserveSource ? save : source;
                for (String cat : category.split(",")) {
                    String tag = StringUtility.toTitleCase(StringUtility.trim(StringUtility.removePunctuation(cat)));
                    if (!tag.isEmpty() && textTagger.tagList.containsKey(tag)) {
                        thisJoke.tags.add(tag);
                    }
                }
                thisJoke.hash = StringUtility.removePunctuation(StringUtility.removeWhiteSpace(thisJoke.text)).toUpperCase().hashCode();
                
                if (hashes.containsKey(thisJoke.hash)) {
                    hashes.get(thisJoke.hash).tags.addAll(thisJoke.tags);
                } else {
                    jokes.add(thisJoke);
                    hashes.put(thisJoke.hash, thisJoke);
                }
            }
            
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        
        if (rewrite) {
            writeStupidStuff(jokes, false);
        }
        
        return jokes;
    }
    
    /**
     * Writes jokes back to StupidStuff.
     *
     * @param jokes The list of jokes parsed from StupidStuff.
     * @param work  Whether or not the jokes should be written back to the work file.
     */
    private void writeStupidStuff(List<Joke> jokes, boolean work) {
        File out = new File("jokes/stupidstuff/source/1 - cleaned/cleaned" + (work ? "-work" : "") + ".json");
        List<String> output = new ArrayList<>();
        output.add("[");
        for (int i = 0; i < jokes.size(); i++) {
            String[] parts = jokes.get(i).source
                    .replaceAll("\\\\*\"", "\\\\\"")
                    .replaceAll("\\s+", " ")
                    .split("\\|\\|\\|\\|\\|", -1);
            output.add("    {");
            output.add("        \"body\": \"" + StringUtility.trim(parts[1]) + "\",");
            output.add("        \"category\": \"" + StringUtility.trim(parts[2]) + "\"");
            output.add("    }" + ((i < jokes.size() - 1) ? "," : ""));
        }
        output.add("]");
        Filesystem.writeLines(out, output);
    }
    
    /**
     * Writes jokes back to StupidStuff.
     *
     * @param jokes The list of jokes parsed from StupidStuff.
     */
    public void writeStupidStuff(List<Joke> jokes) {
        if (!preserveSource) {
            return;
        }
        writeStupidStuff(jokes, true);
    }
    
    /**
     * Parses jokes from Wocka.
     *
     * @return The list of jokes parsed from Wocka.
     */
    public List<Joke> parseWocka() {
        File in = new File("jokes/wocka/source/1 - cleaned/cleaned.json");
        String source = "Wocka";
        boolean rewrite = false;
        
        List<Joke> jokes = new ArrayList<>();
        Map<Integer, Joke> hashes = new HashMap<>();
        
        JSONParser p = new JSONParser();
        try {
            JSONArray o = (JSONArray) p.parse(new FileReader(in));
            
            for (Object i : o) {
                JSONObject io = (JSONObject) i;
                String body = StringUtility.trim((String) io.get("body"));
                String category = ((String) io.get("category"));
                String title = (String) io.get("title");
                String save = title + "|||||" + body + "|||||" + category;
                
                if (body.isEmpty() || body.endsWith(":") || body.contains("1.") || body.contains("B.)") ||
                    body.contains("2)") || body.toUpperCase().contains("WOCKA") ||
                    body.toUpperCase().contains("JOKE") || body.toUpperCase().contains("THIS SITE")) {
                    continue;
                }
                
                if (title.toUpperCase().contains("FACT") || title.toUpperCase().contains("BOOK") ||
                    title.toUpperCase().contains("QUOTE") || title.toUpperCase().contains("REAL") ||
                    title.toUpperCase().contains("NAME") || title.toUpperCase().contains("LIST") ||
                    title.toUpperCase().contains("COLLECTION") || title.toUpperCase().contains("TEST") ||
                    title.toUpperCase().contains("APPLICATION") || title.toUpperCase().contains("FORM") ||
                    title.toUpperCase().contains("RESPONSE") || title.toUpperCase().contains("THINGS") ||
                    title.toUpperCase().contains("LAST WORD") || title.toUpperCase().contains("THESE ARE")) {
                    continue;
                }
                
                body = body.trim();
                
                if (body.startsWith("(") && body.endsWith(")")) {
                    body = StringUtility.rShear(StringUtility.lShear(body, 1), 1);
                }
                
                if (body.endsWith(",")) {
                    body = StringUtility.rShear(body, 1);
                }
                
                Joke thisJoke = new Joke();
                thisJoke.text = body;
                thisJoke.source = preserveSource ? save : source;
                for (String cat : category.split(",")) {
                    if (!cat.isEmpty()) {
                        thisJoke.tags.add(StringUtility.trim(cat));
                    }
                }
                thisJoke.hash = StringUtility.removePunctuation(StringUtility.removeWhiteSpace(thisJoke.text)).toUpperCase().hashCode();
                
                if (hashes.containsKey(thisJoke.hash)) {
                    hashes.get(thisJoke.hash).tags.addAll(thisJoke.tags);
                } else {
                    jokes.add(thisJoke);
                    hashes.put(thisJoke.hash, thisJoke);
                }
            }
            
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        
        if (rewrite) {
            writeWocka(jokes, false);
        }
        
        return jokes;
    }
    
    /**
     * Writes jokes back to Wocka.
     *
     * @param jokes The list of jokes parsed from Wocka.
     * @param work  Whether or not the jokes should be written back to the work file.
     */
    private void writeWocka(List<Joke> jokes, boolean work) {
        File out = new File("jokes/wocka/source/1 - cleaned/cleaned" + (work ? "-work" : "") + ".json");
        List<String> output = new ArrayList<>();
        output.add("[");
        for (int i = 0; i < jokes.size(); i++) {
            String[] parts = jokes.get(i).source
                    .replaceAll("\\\\*\"", "\\\\\"")
                    .replaceAll("\\s+", " ")
                    .split("\\|\\|\\|\\|\\|", -1);
            output.add("    {");
            output.add("        \"body\": \"" + StringUtility.trim(parts[1]) + "\",");
            output.add("        \"category\": \"" + StringUtility.trim(parts[2]) + "\",");
            output.add("        \"title\": \"" + StringUtility.trim(parts[0]) + "\"");
            output.add("    }" + ((i < jokes.size() - 1) ? "," : ""));
        }
        output.add("]");
        Filesystem.writeLines(out, output);
    }
    
    /**
     * Writes jokes back to Wocka.
     *
     * @param jokes The list of jokes parsed from Wocka.
     */
    public void writeWocka(List<Joke> jokes) {
        if (!preserveSource) {
            return;
        }
        writeWocka(jokes, true);
    }
    
    /**
     * Parses jokes from Reddit.
     *
     * @return The list of jokes parsed from Reddit.
     */
    public List<Joke> parseReddit() {
        File in = new File("jokes/reddit/source/1 - cleaned/cleaned.json");
        String source = "Reddit";
        boolean rewrite = false;
        
        List<Joke> jokes = new ArrayList<>();
        Map<Integer, Joke> hashes = new HashMap<>();
        
        JSONParser p = new JSONParser();
        try {
            JSONArray o = (JSONArray) p.parse(new FileReader(in));
            List<Character> punctuation = Arrays.asList('.', ',', '!', '?', ';', ':');
            List<String> questionWords = Arrays.asList("HOW", "WHAT", "WHY", "WHO", "WHERE", "WILL", "WHEN", "WHICH");
            
            boolean first = true;
            for (Object i : o) {
                JSONObject io = (JSONObject) i;
                String body = ((String) io.get("body"));
                String title = ((String) io.get("title"));
                String save = title + "|||||" + body;
                
                if (!title.isEmpty() &&
                    StringUtility.removePunctuation(StringUtility.removeWhiteSpace(body.toUpperCase())).startsWith(
                            StringUtility.removePunctuation(StringUtility.removeWhiteSpace(title.toUpperCase())))) {
                    title = "";
                }
                
                if (title.toUpperCase().startsWith("HEHE") || body.toUpperCase().startsWith("HEHE")) {
                    continue;
                }
                String test = (title + body);
                if (test.contains("OC") || test.contains("AMA") || test.contains("DAMA")) {
                    continue;
                }
                if (test.toUpperCase().equals(test)) {
                    continue;
                }
                test = test.toUpperCase();
                if (test.contains("REDDIT") || test.contains("POST") || test.contains("[REMOVED]") || test.contains("R/") || test.contains("WARNING:") ||
                    test.contains("BLEW UP") || test.contains("EDIT") || test.contains("HANDWRITING") || test.contains("TOLD ME") || test.contains("CAKEDAY") || test.contains("CAKE DAY") ||
                    test.contains("HEARD THIS") || test.contains("HEARD IT") || test.contains("FIRST TIME") || test.contains("HAHA") || test.contains("MY FIRST") || test.contains("AMIRIGHT") ||
                    test.contains("JOKE") || test.contains("THIS SITE") || test.contains("1.") || test.contains("B.)") || test.contains("2)") || test.contains("TRUE STORY") || test.contains("APRIL FOOL") ||
                    test.endsWith(":") || test.contains(" TIL ") || test.startsWith("TIL ") || test.contains(" AMA ") || test.startsWith("AMA ") || test.contains("TOLD THIS") ||
                    test.contains("DOWN-VOTE") || test.contains("DOWNVOTE") || test.contains("UP-VOTE") || test.contains("UPVOTE") || test.contains("WRONG SUB") || test.contains("VOTE UP") || test.contains("VOTE DOWN") ||
                    test.contains("MODS") || test.contains(">") || test.contains("<") || test.contains("U/") || test.contains("_") || test.contains("  - ") ||
                    test.contains("==") || test.contains("^") || test.contains("*") || test.contains("HTTP") || test.contains("WWW") || test.contains(".COM") || test.contains("LAWL") ||
                    test.contains(".ORG") || test.contains(".NET") || test.contains(".GOV") || test.contains("AUTHOR") || test.contains(":)") || test.contains(":/") || test.contains(":P") ||
                    test.contains(":|") || test.contains("O.O") || test.contains(":D") || test.contains(":(") || test.contains("D:") || test.contains("[REQUEST]") || test.contains("PLS") || test.contains("PLZ") ||
                    test.contains(":-)") || test.contains(":-(") || test.contains("XD") || test.contains("TRANSLATED") || test.contains("LOL") || test.contains("KEK") || test.contains("#") ||
                    test.contains("[") || test.contains("]") || test.contains("PS.") || test.contains("PUNCH LINE") || test.contains("LMAO") || test.contains("PEPE") || test.contains("(") || test.contains(")") ||
                    test.contains("PUNCHLINE") || test.contains("MYSELF OUT") || test.contains("//") || test.contains("AYY") || test.contains("JK") || test.contains("WTF") || test.contains("DEEZ") ||
                    test.contains("BTW") || test.contains("SMH") || test.contains("I MADE THIS ONE UP") || test.contains("I MADE THIS UP") || test.contains("I HEARD THIS") || test.contains("LEL") || test.contains("AMIRITE") ||
                    test.contains("BADUMTSS") || test.contains("BA DUM TSS") || test.contains("TIFU") || test.contains("INB4") || test.contains("TLDR") || test.contains("OUT LOUD") || test.contains("HATE ME") ||
                    test.contains("SLAP ME") || test.contains("SMACK ME") || test.contains("4CHAN") || test.contains("SEE TITLE") || test.contains("CLICKBAIT") || test.contains("CLICK BAIT") || test.contains("HUEHUE")) {
                    continue;
                }
                if (test.matches(".*(I'?M\\s)?SORRY[^a-zA-Z0-9]*$") ||
                    test.matches(".*I\\sTRIED[^a-zA-Z0-9]*$") ||
                    test.matches(".*FOLKS[^a-zA-Z0-9]*$") ||
                    test.matches(".*YOU'?\\s?A?RE?\\sWELCOME[^a-zA-Z0-9]*$") ||
                    test.matches(".*\\sTS+[^a-zA-Z0-9]*$") ||
                    test.matches(".*\\sTHANK(S|\\sYOU)[^,?\"':;]*$") ||
                    test.matches(".*\\sPS[^a-zA-Z0-9].*$") ||
                    test.matches(".*(I'?LL\\sBE\\sHERE\\s)?ALL\\s(WEEK|NIGHT|MONTH|YEAR).*$") ||
                    test.matches(".*I'?LL\\s(LEAVE|STOP|GO)(\\sNOW)?[^a-zA-Z0-9]*$") ||
                    test.matches(".*I'?M\\s*(SO\\s)?(LONELY|TIRED)[^a-zA-Z0-9]*$") ||
                    test.matches(".*(GET|NAILED)\\sIT[^a-zA-Z0-9]*$") ||
                    test.matches(".*(SEE|SAW)\\s(THAT|THIS)\\s(ONE\\s)?COMING[^a-zA-Z0-9]*$") ||
                    test.matches(".*PROUD\\sOF\\sTHIS\\sONE[^a-zA-Z0-9]*$") ||
                    test.matches(".*MADE\\sTHIS\\s([^\\s]+\\s)?MYSELF.*$") ||
                    test.matches(".*I'?LL\\sSHOW\\sMY\\s?SELF.+$") ||
                    test.matches(".*THANK.+FOR\\sTHAT\\sONE[^a-zA-Z0-9]*$") ||
                    test.matches(".*(MADE|MAKE)\\s((THIS(\\s(ONE|JOKE))?)|IT)\\s(UP|(MY\\s?SELF)).*$") ||
                    test.matches(".*COURTESY\\sOF\\sMY\\s.*$") ||
                    test.matches(".*OUT[^a-zA-Z]*$") ||
                    test.matches(".*I'?LL\\sSEE\\sMY\\s?WAY.+$") ||
                    test.matches(".*HATE\\sMYSELF[^a-zA-Z0-9]*$") ||
                    test.matches(".*REALLY\\sHAPPEN.*") ||
                    test.matches(".*TRUE\\sSTORY.*") ||
                    test.matches("^FROM\\sMY.*") ||
                    test.matches("^(I\\s)?HEARD\\s(THIS|FROM).*")) {
                    continue;
                }
                if (test.replaceAll("[^\\x00-\\x7F]", "")
                        .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").length() != test.length()) {
                    continue;
                }
                
                boolean nsfw = false;
                if (body.toUpperCase().contains("(NSFW)")) {
                    nsfw = true;
                    body = body.replace("(NSFW)", " ");
                }
                if (title.toUpperCase().contains("(NSFW)")) {
                    nsfw = true;
                    title = title.replace("(NSFW)", " ");
                }
                
                title = StringUtility.trim(title);
                if (!title.isEmpty() && !punctuation.contains(title.charAt(title.length() - 1))) {
                    boolean questionTitle = false;
                    String questionTest = title.toUpperCase();
                    for (String questionWord : questionWords) {
                        if (questionTest.startsWith(questionWord)) {
                            questionTitle = true;
                            break;
                        }
                    }
                    title = title + (questionTitle ? "?" : ":");
                }
                body = StringUtility.trim(body);
                
                Joke thisJoke = new Joke();
                thisJoke.text = title + " " + body;
                thisJoke.source = preserveSource ? (title + "|||||" + body) : source;
                thisJoke.nsfw = nsfw;
                thisJoke.hash = StringUtility.removePunctuation(StringUtility.removeWhiteSpace(thisJoke.text)).toUpperCase().hashCode();
                
                if (hashes.containsKey(thisJoke.hash)) {
                    hashes.get(thisJoke.hash).tags.addAll(thisJoke.tags);
                } else {
                    jokes.add(thisJoke);
                    hashes.put(thisJoke.hash, thisJoke);
                }
            }
            
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        
        if (rewrite) {
            writeReddit(jokes, false);
        }
        
        return jokes;
    }
    
    /**
     * Writes jokes back to Reddit.
     *
     * @param jokes The list of jokes parsed from Reddit.
     * @param work  Whether or not the jokes should be written back to the work file.
     */
    private void writeReddit(List<Joke> jokes, boolean work) {
        File out = new File("jokes/reddit/source/1 - cleaned/cleaned" + (work ? "-work" : "") + ".json");
        List<String> output = new ArrayList<>();
        output.add("[");
        for (int i = 0; i < jokes.size(); i++) {
            String[] parts = jokes.get(i).source
                    .replaceAll("\\\\*\"", "\\\\\"")
                    .replaceAll("\\s+", " ")
                    .split("\\|\\|\\|\\|\\|", -1);
            output.add("    {");
            output.add("        \"body\": \"" + StringUtility.trim(parts[1]) + "\",");
            output.add("        \"title\": \"" + StringUtility.trim(parts[0]) + "\"");
            output.add("    }" + ((i < jokes.size() - 1) ? "," : ""));
        }
        output.add("]");
        Filesystem.writeLines(out, output);
    }
    
    /**
     * Writes jokes back to Reddit.
     *
     * @param jokes The list of jokes parsed from Reddit.
     */
    public void writeReddit(List<Joke> jokes) {
        if (!preserveSource) {
            return;
        }
        writeReddit(jokes, true);
    }
    
}
