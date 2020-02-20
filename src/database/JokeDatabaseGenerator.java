/*
 * File:    JokeDatabaseGenerator.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import main.Jokes;
import pojo.Joke;
import resource.ConsoleProgressBar;
import utility.Filesystem;
import worker.TextTagger;

/**
 * Generates the Joke Database.
 */
public class JokeDatabaseGenerator {
    
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
     * The list of Jokes.
     */
    private static final List<Joke> jokes = new ArrayList<>();
    
    /**
     * The maximum number of Jokes to process.
     */
    private static int maxJokes;
    
    /**
     * The reference to the Text Tagger.
     */
    private static TextTagger textTagger = null;
    
    /**
     * The Joke Database sql code.
     */
    private static final List<String> jokesSql = new ArrayList<>();
    
    /**
     * A flag indicating whether or not to produce only the Joke Database sql.
     */
    private static boolean sqlOnly = false;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        jokes.addAll(Jokes.readJokes(new File("jokes/jokes.json")));
        jokes.sort(Comparator.comparingLong(o -> o.hash));
        maxJokes = jokes.size();
        
        textTagger = TextTagger.getInstance();
        textTagger.loadTagLists = false;
        textTagger.load();
        
        if (!sqlOnly) {
            if (!DatabaseManager.setupDatabase()) {
                return;
            }
            
            conn = DatabaseManager.connectToDatabase("jokes/db/jokes", false);
            if (conn == null) {
                return;
            }
            
            s = DatabaseManager.createStatement(conn);
            if (s == null) {
                return;
            }
        }
        
        
        jokesSql.add("-- Create Tables");
        createSourceTable();
        createJokeTable();
        jokesSql.add("");
        
        jokesSql.add("-- Create Tag Tables");
        createTagTables();
        jokesSql.add("");
        
        jokesSql.add("-- Create Indices");
        createIndices();
        jokesSql.add("");
        
        jokesSql.add("-- Insert Sources");
        populateSourceTable();
        jokesSql.add("");
        
        jokesSql.add("-- Insert Jokes");
        populateJokeTable();
        jokesSql.add("");
        
        jokesSql.add("-- Insert Tags");
        populateTagTables();
        
        
        Filesystem.writeLines(new File("jokes/db/jokes.sql"), jokesSql);
        
        if (!sqlOnly) {
            DatabaseManager.closeStatement(s);
            DatabaseManager.disconnectFromDatabase(conn);
            DatabaseManager.shutdownDatabase();
        }
    }
    
    /**
     * Creates the Source table in the Joke Database.
     *
     * @return Whether the table was created successfully or not.
     */
    private static boolean createSourceTable() {
        String sql = "CREATE TABLE source (" +
                "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                "name VARCHAR(16) UNIQUE NOT NULL, " +
                "CONSTRAINT source_pk PRIMARY KEY (id)" +
                ")";
        jokesSql.add(sql + ";");
        
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Creating Source Table", 1, "");
        progressBar.update(0);
        
        if (!sqlOnly) {
            if (!DatabaseManager.executeSql(s, sql)) {
                return false;
            }
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Creates the Joke table in the Joke Database.
     *
     * @return Whether the table was created successfully or not.
     */
    private static boolean createJokeTable() {
        String sql = "CREATE TABLE joke (" +
                "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                "text LONG VARCHAR NOT NULL, " +
                "length INT NOT NULL, " +
                "source INT NOT NULL, " +
                "nsfw BOOLEAN NOT NULL, " +
                "hash BIGINT UNIQUE NOT NULL, " +
                "CONSTRAINT joke_pk PRIMARY KEY (id), " +
                "CONSTRAINT joke_source_fk FOREIGN KEY (source) REFERENCES source(id)" +
                ")";
        jokesSql.add(sql + ";");
        
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Creating Joke Table", 1, "");
        progressBar.update(0);
        
        if (!sqlOnly && !DatabaseManager.executeSql(s, sql)) {
            return false;
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Creates the Tag tables in the Joke Database.
     *
     * @return Whether the tables were created successfully or not.
     */
    private static boolean createTagTables() {
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Creating Tag Tables", textTagger.tagList.size(), "");
        progressBar.update(0);
        
        for (String tag : textTagger.tagList.keySet()) {
            String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
            String sqlStatement = "CREATE TABLE " + tagTable + " (" +
                    "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                    "joke INT NOT NULL, " +
                    "CONSTRAINT " + tagTable + "_pk PRIMARY KEY (id), " +
                    "CONSTRAINT " + tagTable + "_joke_fk FOREIGN KEY (joke) REFERENCES joke(id)" +
                    ")";
            jokesSql.add(sqlStatement + ";");
            
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
            progressBar.addOne();
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Creates the indices in the Joke Database.
     *
     * @return Whether the indices were created successfully or not.
     */
    private static boolean createIndices() {
        List<String> baseIndices = new ArrayList<>();
        baseIndices.add("CREATE INDEX source_name_idx ON source (name)");
        baseIndices.add("CREATE INDEX joke_length_idx ON joke (length)");
        baseIndices.add("CREATE INDEX joke_source_idx ON joke (source)");
        baseIndices.add("CREATE INDEX joke_nsfw_idx ON joke (nsfw)");
        
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Creating Indices", textTagger.tagList.size() + baseIndices.size(), "");
        progressBar.update(0);
        
        for (String baseIndex : baseIndices) {
            jokesSql.add(baseIndex + ";");
            
            if (!sqlOnly && !DatabaseManager.executeSql(s, baseIndex)) {
                return false;
            }
            progressBar.addOne();
        }
        
        for (String tag : textTagger.tagList.keySet()) {
            String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
            String sql = "CREATE INDEX " + tagTable + "_joke_idx ON " + tagTable + " (joke)";
            jokesSql.add(sql + ";");
            
            if (!sqlOnly && !DatabaseManager.executeSql(s, sql)) {
                return false;
            }
            progressBar.addOne();
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Populates the Source table in the Joke Database.
     *
     * @return Whether the table was populated successfully or not.
     */
    private static boolean populateSourceTable() {
        List<String> sourceData = new ArrayList<>();
        sourceData.add("INSERT INTO source (name) VALUES('Quirkology')");
        sourceData.add("INSERT INTO source (name) VALUES('Jokeriot')");
        sourceData.add("INSERT INTO source (name) VALUES('StupidStuff')");
        sourceData.add("INSERT INTO source (name) VALUES('Wocka')");
        sourceData.add("INSERT INTO source (name) VALUES('Reddit')");
        
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Populating Source Table", sourceData.size(), "");
        progressBar.update(0);
        
        for (String sqlStatement : sourceData) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
            progressBar.addOne();
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Populates the Joke table in the Joke Database.
     *
     * @return Whether the table was populated successfully or not.
     */
    private static boolean populateJokeTable() {
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Populating Joke Table", jokes.size(), "");
        progressBar.update(0);
        
        for (int i = 0; i < maxJokes; i++) {
            Joke joke = jokes.get(i);
            String jokeSql = "INSERT INTO joke (text, length, source, nsfw, hash) VALUES(" +
                    "'" + joke.text.replace("'", "''") + "', " +
                    joke.length + ", " +
                    "(SELECT id FROM source WHERE name = '" + joke.source + "'), " +
                    (joke.nsfw ? "true" : "false") + ", " +
                    joke.hash + ")";
            jokesSql.add(jokeSql + ";");
            
            if (!sqlOnly && !DatabaseManager.executeSql(s, jokeSql)) {
                return false;
            }
            progressBar.addOne();
        }
        
        DatabaseManager.commitChanges(conn);
        progressBar.complete();
        return true;
    }
    
    /**
     * Populates the Tag tables in the Joke Database.
     *
     * @return Whether the tables were populated successfully or not.
     */
    private static boolean populateTagTables() {
        final AtomicInteger tagCount = new AtomicInteger(0);
        jokes.forEach(j -> tagCount.getAndAdd(j.tags.size()));
        
        ConsoleProgressBar progressBar = new ConsoleProgressBar("Populating Tag Tables", tagCount.get(), "");
        progressBar.update(0);
        
        Map<String, List<String>> sql = new HashMap<>();
        for (int i = 0; i < maxJokes; i++) {
            Joke joke = jokes.get(i);
            
            for (String tag : joke.tags) {
                String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
                String tagSql = "INSERT INTO " + tagTable + " (joke) VALUES(" + (i + 1) + ")";
                jokesSql.add(tagSql + ";");
                
                sql.putIfAbsent(tagTable, new ArrayList<>());
                sql.get(tagTable).add(tagSql);
            }
        }
        
        for (Map.Entry<String, List<String>> sqlEntry : sql.entrySet()) {
            for (String tagSql : sqlEntry.getValue()) {
                if (!sqlOnly && !DatabaseManager.executeSql(s, tagSql)) {
                    return false;
                }
                progressBar.addOne();
            }
            DatabaseManager.commitChanges(conn);
        }
        
        progressBar.complete();
        return true;
    }
    
}
