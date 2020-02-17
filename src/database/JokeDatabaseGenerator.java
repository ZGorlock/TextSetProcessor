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
import java.util.List;

import main.Jokes;
import pojo.Joke;
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
    private static boolean sqlOnly = true;
    
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        jokes.addAll(Jokes.readJokes(new File("jokes/jokes.json")));
        
        textTagger = TextTagger.getInstance();
        textTagger.loadTagLists = false;
        textTagger.load();
        
        if (!sqlOnly) {
            if (!DatabaseManager.setupDatabase()) {
                return;
            }
            
            conn = DatabaseManager.connectToDatabase("jokes/db/jokes");
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
        
        return sqlOnly || DatabaseManager.executeSql(s, sql);
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
        
        return sqlOnly || DatabaseManager.executeSql(s, sql);
    }
    
    /**
     * Creates the Tag tables in the Joke Database.
     *
     * @return Whether the tables were created successfully or not.
     */
    private static boolean createTagTables() {
        List<String> sql = new ArrayList<>();
        for (String tag : textTagger.tagList.keySet()) {
            String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
            String sqlStatement = "CREATE TABLE " + tagTable + " (" +
                    "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                    "joke INT NOT NULL, " +
                    "CONSTRAINT " + tagTable + "_pk PRIMARY KEY (id), " +
                    "CONSTRAINT " + tagTable + "_joke_fk FOREIGN KEY (joke) REFERENCES joke(id)" +
                    ")";
            sql.add(sqlStatement);
        }
        
        for (String sqlStatement : sql) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creates the indices in the Joke Database.
     *
     * @return Whether the indices were created successfully or not.
     */
    private static boolean createIndices() {
        List<String> sql = new ArrayList<>();
        sql.add("CREATE INDEX source_name_idx ON source (name)");
        sql.add("CREATE INDEX joke_length_idx ON joke (length)");
        sql.add("CREATE INDEX joke_source_idx ON joke (source)");
        sql.add("CREATE INDEX joke_nsfw_idx ON joke (nsfw)");
        
        for (String tag : textTagger.tagList.keySet()) {
            String tagTable = "tag_" + tag.toLowerCase().replace(" ", "_");
            sql.add("CREATE INDEX " + tagTable + "_joke_idx ON " + tagTable + " (joke)");
        }
        
        for (String sqlStatement : sql) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Populates the Source table in the Joke Database.
     *
     * @return Whether the table was populated successfully or not.
     */
    private static boolean populateSourceTable() {
        List<String> sql = new ArrayList<>();
        sql.add("INSERT INTO source (name) VALUES('Quirkology')");
        sql.add("INSERT INTO source (name) VALUES('Jokeriot')");
        sql.add("INSERT INTO source (name) VALUES('StupidStuff')");
        sql.add("INSERT INTO source (name) VALUES('Wocka')");
        sql.add("INSERT INTO source (name) VALUES('Reddit')");
        
        for (String sqlStatement : sql) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Populates the Joke table in the Joke Database.
     *
     * @return Whether the table was populated successfully or not.
     */
    private static boolean populateJokeTable() {
        List<String> sql = new ArrayList<>();
        for (Joke joke : jokes) {
            
            StringBuilder jokeSql = new StringBuilder("INSERT INTO joke (text, length, source, nsfw, hash) VALUES(");
            jokeSql.append("'").append(joke.text.replace("'", "''")).append("', ");
            jokeSql.append(joke.length).append(", ");
            jokeSql.append("(SELECT id FROM source WHERE name = '").append(joke.source).append("'), ");
            jokeSql.append(joke.nsfw ? "true" : "false").append(", ");
            jokeSql.append(joke.hash).append(")");
            sql.add(jokeSql.toString());
        }
        
        for (String sqlStatement : sql) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Populates the Tag tables in the Joke Database.
     *
     * @return Whether the tables were populated successfully or not.
     */
    private static boolean populateTagTables() {
        List<String> sql = new ArrayList<>();
        for (int i = 0; i < jokes.size(); i++) {
            Joke joke = jokes.get(i);
            
            for (String tag : joke.tags) {
                sql.add("INSERT INTO tag_" + tag.toLowerCase().replace(" ", "_") + " (joke) VALUES(" + (i + 1) + ")");
            }
        }
        
        for (String sqlStatement : sql) {
            jokesSql.add(sqlStatement + ";");
            if (!sqlOnly && !DatabaseManager.executeSql(s, sqlStatement)) {
                return false;
            }
        }
        return true;
    }
    
}
