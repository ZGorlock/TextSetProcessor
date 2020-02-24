/*
 * File:    JokeDatabaseHandler.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import pojo.Joke;

/**
 * Handles access to the Joke Database.
 */
public class JokeDatabaseHandler {
    
    //Enums
    
    /**
     * Enumeration of the queryable lengths of Jokes.
     */
    public enum JokeLength {
        SHORT(1, 200),
        NORMAL(201, 750),
        LONG(751, 32767);
        
        private final int min;
        
        private final int max;
        
        JokeLength(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        public int getMin() {
            return min;
        }
        
        public int getMax() {
            return max;
        }
    }
    
    
    //Static Fields
    
    /**
     * The Connection for the Joke Database.
     */
    private static Connection conn;
    
    /**
     * The Statement for the Joke Database Connection.
     */
    private static Statement s;
    
    /**
     * A flag indicating whether or not NSFW jokes are allowed.
     */
    private static boolean nsfwAllowed = true;
    
    /**
     * A list of sources that are enabled for Jokes.
     */
    private static Set<String> sourcesAllowed = new HashSet<>(Arrays.asList("Quirkology", "Jokeriot", "StupidStuff", "Wocka", "Reddit"));
    
    /**
     * A map of source names to their indices in the Joke Database.
     */
    private static Map<String, Integer> sourcesMap = new HashMap<>();
    
    /**
     * A map of source indices in the Joke Database to their name.
     */
    private static Map<Integer, String> reverseSourcesMap = new HashMap<>();
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        if (!DatabaseManager.setupDatabase()) {
            return;
        }
        
        conn = DatabaseManager.connectToDatabase("jar:(jokes/db/jokes-test.zip)jokes");
        if (conn == null) {
            return;
        }
        
        s = DatabaseManager.createStatement(conn);
        if (s == null) {
            return;
        }
        
        if (!populateSourceMaps()) {
            return;
        }
        
        long a, b;
        Joke joke;
        
        a = System.currentTimeMillis();
        getAllJokes();
        b = System.currentTimeMillis();
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        countJokes();
        b = System.currentTimeMillis();
        System.out.println(b - a);
        
        for (int i = 0; i < 50; i++) {
            getJoke();
        }
        
        a = System.currentTimeMillis();
        joke = getJoke();
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTag("Animal");
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterNsfw(true);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterSource("Reddit");
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagNsfw("Whale", true);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagSource("Beer", "Jokeriot");
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagLength("Beer", JokeLength.SHORT);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterNsfwSource(false, "Wocka");
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterNsfwLength(false, JokeLength.LONG);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterSourceLength("Quirkology", JokeLength.NORMAL);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagNsfwSource("Animal", true, "Quirkology");
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagNsfwLength("Animal", true, JokeLength.LONG);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagSourceLength("Animal", "Reddit", JokeLength.NORMAL);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterNsfwSourceLength(false, "Wocka", JokeLength.SHORT);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
        
        a = System.currentTimeMillis();
        joke = getJokeFilterTagNsfwSourceLength("Alcohol", false, "Jokeriot", JokeLength.LONG);
        b = System.currentTimeMillis();
        System.out.println(joke.text);
        System.out.println(b - a);
    }
    
    
    //Methods
    
    /**
     * Populates the source maps.
     *
     * @return Whether the source maps were successfully populated or not.
     */
    private static boolean populateSourceMaps() {
        String sql = "SELECT * FROM source";
        FormattedResultSet rs = DatabaseManager.querySql(s, sql);
        if (rs == null) {
            return false;
        }
        for (int i = 0; i < rs.getRowCount(); i++) {
            sourcesMap.put(rs.getStringResult("name", i), rs.getIntegerResult("ID", i));
        }
        for (Map.Entry<String, Integer> sourceMapEntry : sourcesMap.entrySet()) {
            reverseSourcesMap.put(sourceMapEntry.getValue(), sourceMapEntry.getKey());
        }
        return true;
    }
    
    /**
     * Gets all Jokes.
     *
     * @return A list of Jokes, or null if there was an error.
     */
    private static List<Joke> getAllJokes() {
        String sql = "SELECT * FROM joke" + addFilters();
        System.out.println(sql);
        
        FormattedResultSet rs = DatabaseManager.querySql(s, sql);
        if (rs == null || rs.getRowCount() == 0) {
            return null;
        }
        
        List<Joke> jokes = new ArrayList<>();
        for (int i = 0; i < rs.getRowCount(); i++) {
            jokes.add(createJokeObject(rs, i));
        }
        rs.close();
        return jokes;
    }
    
    /**
     * Gets a random Joke.
     *
     * @param filters A list of filters to apply when choosing the Joke.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJoke(List<String> filters) {
        String sql = "SELECT * FROM joke" + addFilters(filters) + addRandom();
        System.out.println(sql);
        
        FormattedResultSet rs = DatabaseManager.querySql(s, sql);
        if (rs == null || rs.getRowCount() == 0) {
            return null;
        }
        
        Joke joke = createJokeObject(rs);
        rs.close();
        return joke;
    }
    
    /**
     * Gets a random Joke.
     *
     * @param filter A filter to apply when choosing the Joke.
     * @return The Joke, or null if there was an error or no Jokes match the filter.
     */
    private static Joke getJoke(String filter) {
        return getJoke(Collections.singletonList(filter));
    }
    
    /**
     * Gets a random Joke.
     *
     * @return The Joke, or null if there was an error.
     */
    private static Joke getJoke() {
        return getJoke(Collections.emptyList());
    }
    
    /**
     * Gets a random Joke with filters applied.
     *
     * @param tag    The tag, or null if it should not be filtered.
     * @param nsfw   Whether or not the Joke should be NSFW, or null if it should not be filtered.
     * @param source The source, or null if it should not be filtered.
     * @param length The length specification, or null if it should not be filtered.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeWithFilters(String tag, Boolean nsfw, String source, JokeLength length) {
        //TODO check that tag exists
        if ((nsfw != null) && nsfw && !nsfwAllowed) {
            return null;
        }
        if ((source != null) && !sourcesAllowed.contains(source)) {
            return null;
        }
        
        return getJoke(Arrays.asList(
                createTagFilter(tag),
                createNsfwFilter(nsfw),
                createSourceFilter(source),
                createLengthFilter(length)
        ));
    }
    
    /**
     * Gets a random Joke with a tag filter.
     *
     * @param tag The tag.
     * @return The Joke, or null if there was an error or no Jokes match the filter.
     */
    private static Joke getJokeFilterTag(String tag) {
        return getJokeWithFilters(tag, null, null, null);
    }
    
    /**
     * Gets a random Joke with a NSFW filter.
     *
     * @param nsfw Whether or not the Joke should be NSFW.
     * @return The Joke, or null if there was an error or no Jokes match the filter.
     */
    private static Joke getJokeFilterNsfw(boolean nsfw) {
        return getJokeWithFilters(null, nsfw, null, null);
    }
    
    /**
     * Gets a random Joke with a source filter.
     *
     * @param source The source.
     * @return The Joke, or null if there was an error or no Jokes match the filter.
     */
    private static Joke getJokeFilterSource(String source) {
        return getJokeWithFilters(null, null, source, null);
    }
    
    /**
     * Gets a random Joke with a length filter.
     *
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filter.
     */
    private static Joke getJokeWithLength(JokeLength length) {
        return getJokeWithFilters(null, null, null, length);
    }
    
    /**
     * Gets a random Joke with tag and nsfw filters.
     *
     * @param tag  The tag.
     * @param nsfw Whether or not the Joke should be NSFW.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagNsfw(String tag, boolean nsfw) {
        return getJokeWithFilters(tag, nsfw, null, null);
    }
    
    /**
     * Gets a random Joke with tag and source filters.
     *
     * @param tag    The tag.
     * @param source The source.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagSource(String tag, String source) {
        return getJokeWithFilters(tag, null, source, null);
    }
    
    /**
     * Gets a random Joke with tag and length filters.
     *
     * @param tag    The tag.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagLength(String tag, JokeLength length) {
        return getJokeWithFilters(tag, null, null, length);
    }
    
    /**
     * Gets a random Joke with nsfw and source filters.
     *
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param source The source.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterNsfwSource(boolean nsfw, String source) {
        return getJokeWithFilters(null, nsfw, source, null);
    }
    
    /**
     * Gets a random Joke with nsfw and length filters.
     *
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterNsfwLength(boolean nsfw, JokeLength length) {
        return getJokeWithFilters(null, nsfw, null, length);
    }
    
    /**
     * Gets a random Joke with source and length filters.
     *
     * @param source The source.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterSourceLength(String source, JokeLength length) {
        return getJokeWithFilters(null, null, source, length);
    }
    
    /**
     * Gets a random Joke with tag, nsfw, and source filters.
     *
     * @param tag    The tag.
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param source The source.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagNsfwSource(String tag, boolean nsfw, String source) {
        return getJokeWithFilters(tag, nsfw, source, null);
    }
    
    /**
     * Gets a random Joke with tag, nsfw, and length filters.
     *
     * @param tag    The tag.
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagNsfwLength(String tag, boolean nsfw, JokeLength length) {
        return getJokeWithFilters(tag, nsfw, null, length);
    }
    
    /**
     * Gets a random Joke with tag, source, and length filters.
     *
     * @param tag    The tag.
     * @param source The source.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagSourceLength(String tag, String source, JokeLength length) {
        return getJokeWithFilters(tag, null, source, length);
    }
    
    /**
     * Gets a random Joke with nsfw, source, and length filters.
     *
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param source The source.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterNsfwSourceLength(boolean nsfw, String source, JokeLength length) {
        return getJokeWithFilters(null, nsfw, source, length);
    }
    
    /**
     * Gets a random Joke with tag, nsfw, source, and length filters.
     *
     * @param tag    The tag.
     * @param nsfw   Whether or not the Joke should be NSFW.
     * @param source The source.
     * @param length The length specification.
     * @return The Joke, or null if there was an error or no Jokes match the filters.
     */
    private static Joke getJokeFilterTagNsfwSourceLength(String tag, boolean nsfw, String source, JokeLength length) {
        return getJokeWithFilters(tag, nsfw, source, length);
    }
    
    /**
     * Counts the number of Jokes.
     *
     * @return The number of Jokes.
     */
    private static int countJokes() {
        String sql = "SELECT COUNT(*) AS count FROM joke" + addFilters();
        System.out.println(sql);
        
        FormattedResultSet rs = DatabaseManager.querySql(s, sql);
        if (rs == null || rs.getRowCount() == 0) {
            return 0;
        }
        
        return rs.getIntegerResult("count");
    }
    
    /**
     * Creates a tag filter.
     *
     * @param tag The tag.
     * @return The tag filter.
     */
    private static String createTagFilter(String tag) {
        if (tag == null) {
            return null;
        }
        String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
        return "id IN (SELECT joke FROM " + tagTable + ")";
    }
    
    /**
     * Creates a NSFW filter.
     *
     * @param nsfw Whether or not the Joke should be NSFW.
     * @return The NSFW filter.
     */
    private static String createNsfwFilter(Boolean nsfw) {
        if (nsfw == null) {
            return null;
        }
        return "nsfw = " + (nsfw ? "true" : "false");
    }
    
    /**
     * Creates a source filter.
     *
     * @param source The source.
     * @return The source filter.
     */
    private static String createSourceFilter(String source) {
        if (source == null) {
            return null;
        }
        return "source = " + sourcesMap.get(source);
    }
    
    /**
     * Creates a length filter.
     *
     * @param length The length specification.
     * @return The length filter.
     */
    private static String createLengthFilter(JokeLength length) {
        if (length == null) {
            return null;
        }
        return "length >= " + length.getMin() + " AND length <= " + length.getMax();
    }
    
    /**
     * Creates a filter query to add to the base query.
     *
     * @param specificFilters A list of filter to apply to the query.
     * @return The filter query.
     */
    private static String addFilters(List<String> specificFilters) {
        List<String> filters = new ArrayList<>();
        specificFilters.stream().filter(Objects::nonNull).forEach(filters::add);
        
        Set<String> overrideFilters = new HashSet<>();
        for (String filter : filters) {
            overrideFilters.add(filter.substring(0, filter.indexOf(' ')));
        }
        
        if (!overrideFilters.contains("nsfw") && !nsfwAllowed) {
            filters.add("nsfw = false");
        }
        if (!overrideFilters.contains("source") && (sourcesAllowed.size() < sourcesMap.size())) {
            filters.add("source IN " +
                    sourcesAllowed.stream().map(s -> String.valueOf(sourcesMap.get(s))).collect(Collectors.joining(", ", "(", ")")));
        }
        
        if (filters.isEmpty()) {
            return "";
        }
        return filters.stream().collect(Collectors.joining(" AND ", " WHERE ", ""));
    }
    
    /**
     * Creates a filter query to add to the base query.
     *
     * @return The filter query.
     */
    private static String addFilters() {
        return addFilters(Collections.emptyList());
    }
    
    /**
     * Creates a random query to add to the base query.
     *
     * @return The random query.
     */
    private static String addRandom() {
        return " ORDER BY RANDOM() OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY";
    }
    
    /**
     * Creates a Joke from the results of a query.
     *
     * @param rs  The formatted result set of the query.
     * @param row The row in the formatted result set to create a Joke from.
     * @return A Joke.
     */
    private static Joke createJokeObject(FormattedResultSet rs, int row) {
        Joke joke = new Joke();
        joke.id = rs.getIntegerResult("id", row);
        joke.text = rs.getStringResult("text", row);
        joke.length = rs.getIntegerResult("length", row);
        joke.source = reverseSourcesMap.get(rs.getIntegerResult("source", row));
        joke.nsfw = rs.getBooleanResult("nsfw", row);
        joke.hash = rs.getLongResult("hash", row);
        return joke;
    }
    
    /**
     * Creates a Joke from the first result of a query.
     *
     * @param rs The formatted result set of the query.
     * @return A Joke.
     */
    private static Joke createJokeObject(FormattedResultSet rs) {
        return createJokeObject(rs, 0);
    }
    
}
