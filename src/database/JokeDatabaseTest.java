/*
 * File:    JokeDatabaseTest.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.Jokes;
import pojo.Joke;
import utility.StringUtility;
import worker.TextTagger;

/**
 * Tests the Joke Database.
 */
public class JokeDatabaseTest {
    
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
        
        
        System.out.println((testTablesExist() ? "Passed" : "Failed") + " Tables Exist Test");
        System.out.println((testTagTablesExist() ? "Passed" : "Failed") + " Tag Tables Exist Test");
        System.out.println((testSourceTable() ? "Passed" : "Failed") + " Source table Test");
        System.out.println((testJokeTable() ? "Passed" : "Failed") + " Joke table Test");
        System.out.println((testTagTables() ? "Passed" : "Failed") + " Tag tables Test");
        System.out.println();
        System.out.println("Speed (all)   : " + testSpeed("ALL;") + "ms");
        System.out.println("Speed (one)   : " + testSpeed("ONE;") + "ms");
        System.out.println("Speed (random): " + testSpeed("RANDOM;") + "ms");
        System.out.println();
        System.out.println("NSFW Speed (all)   : " + testSpeed("ALL;nsfw=true") + "ms");
        System.out.println("NSFW Speed (random): " + testSpeed("RANDOM;nsfw=true") + "ms");
        System.out.println();
        System.out.println("Source Speed (all)   : " + testSpeed("ALL;source=Jokeriot") + "ms");
        System.out.println("Source Speed (random): " + testSpeed("RANDOM;source=Jokeriot") + "ms");
        System.out.println();
        System.out.println("Length Speed (all)   : " + testSpeed("ALL;lengthGt=250") + "ms");
        System.out.println("Length Speed (random): " + testSpeed("RANDOM;lengthGt=250") + "ms");
        System.out.println();
        System.out.println("Tag Speed (all)   : " + testSpeed("ALL;tag=Lightbulb") + "ms");
        System.out.println("Tag Speed (random): " + testSpeed("RANDOM;tag=Lightbulb") + "ms");
        System.out.println();
        System.out.println("Tag NSFW Speed (all)   : " + testSpeed("ALL;tag=Lightbulb;nsfw=true") + "ms");
        System.out.println("Tag NSFW Speed (random): " + testSpeed("RANDOM;tag=Lightbulb;nsfw=true") + "ms");
        System.out.println();
        System.out.println("Tag NSFW Source Speed (all)   : " + testSpeed("ALL;tag=Lightbulb;nsfw=true;source=Reddit") + "ms");
        System.out.println("Tag NSFW Source Speed (random): " + testSpeed("RANDOM;tag=Lightbulb;nsfw=true;source=Reddit") + "ms");
        System.out.println();
        System.out.println("Tag NSFW Source Length Speed (all)   : " + testSpeed("ALL;tag=Lightbulb;nsfw=true;source=Reddit;lengthGt=100") + "ms");
        System.out.println("Tag NSFW Source Length Speed (random): " + testSpeed("RANDOM;tag=Lightbulb;nsfw=true;source=Reddit;lengthGt=100") + "ms");
        
        
        DatabaseManager.closeStatement(s);
        DatabaseManager.disconnectFromDatabase(conn);
        DatabaseManager.shutdownDatabase();
    }
    
    /**
     * Tests if the tables exist in the Joke Database.
     *
     * @return Whether the tables exist or not.
     */
    private static boolean testTablesExist() {
        FormattedResultSet rs;
        try {
            rs = new FormattedResultSet(conn.getMetaData().getTables(null, null, null, null));
        } catch (Exception ignored) {
            return false;
        }
        
        List<Object> tableNames = rs.getColumn("TABLE_NAME");
        if (tableNames == null) {
            return false;
        }
        
        for (String table : Arrays.asList("source", "joke")) {
            if (!tableNames.contains(table.toUpperCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Tests if the tag tables exist in the Joke Database.
     *
     * @return Whether the tag tables exist or not.
     */
    private static boolean testTagTablesExist() {
        FormattedResultSet rs;
        try {
            rs = new FormattedResultSet(conn.getMetaData().getTables(null, null, null, null));
        } catch (Exception ignored) {
            return false;
        }
        
        List<Object> tableNames = rs.getColumn("TABLE_NAME");
        if (tableNames == null) {
            return false;
        }
        
        for (String table : textTagger.tagList.keySet()) {
            if (!tableNames.contains("TAG_" + table.toUpperCase().replace(" ", "_"))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Tests the Source table in the Joke Database.
     *
     * @return Whether the table passed the tests or not.
     */
    private static boolean testSourceTable() {
        FormattedResultSet rs = DatabaseManager.querySql(s, "SELECT * FROM source");
        
        return (rs != null) &&
                (rs.getIntegerResult("id", 0) == 1) &&
                (rs.getIntegerResult("id", 1) == 2) &&
                (rs.getIntegerResult("id", 2) == 3) &&
                (rs.getIntegerResult("id", 3) == 4) &&
                (rs.getIntegerResult("id", 4) == 5) &&
                rs.getStringResult("name", 0).equals("Quirkology") &&
                rs.getStringResult("name", 1).equals("Jokeriot") &&
                rs.getStringResult("name", 2).equals("StupidStuff") &&
                rs.getStringResult("name", 3).equals("Wocka") &&
                rs.getStringResult("name", 4).equals("Reddit");
    }
    
    /**
     * Tests the Joke table in the Joke Database.
     *
     * @return Whether the table passed the tests or not.
     */
    private static boolean testJokeTable() {
        FormattedResultSet rs = DatabaseManager.querySql(s, "SELECT * FROM joke WHERE id = 1");
        if (rs == null) {
            return false;
        }
        
        Joke joke = jokes.get(0);
        
        FormattedResultSet sourceRs = DatabaseManager.querySql(s, "SELECT id FROM source WHERE name = '" + joke.source + "'");
        if (sourceRs == null) {
            return false;
        }
        int source = sourceRs.getIntegerResult("ID", 0);
        
        if (!StringUtility.removePunctuation(rs.getStringResult("TEXT", 0)).equals(StringUtility.removePunctuation(joke.text)) ||
                (rs.getIntegerResult("LENGTH", 0) != joke.length) ||
                (rs.getIntegerResult("SOURCE", 0) != source) ||
                (rs.getBooleanResult("NSFW", 0) != joke.nsfw) ||
                (rs.getLongResult("HASH", 0) != (joke.hash))) {
            return false;
        }
        
        rs = DatabaseManager.querySql(s, "SELECT COUNT(*) AS count FROM joke");
        if (rs == null) {
            return false;
        }
        
        return (rs.getIntegerResult("COUNT", 0) == jokes.size());
    }
    
    /**
     * Tests the Tag tables in the Joke Database.
     *
     * @return Whether the tables passed the tests or not.
     */
    private static boolean testTagTables() {
        Joke joke = jokes.get(0);
        for (String tag : joke.tags) {
            
            FormattedResultSet rs = DatabaseManager.querySql(s, "SELECT COUNT(*) AS count FROM tag_" + tag.toLowerCase().replace(" ", "_") + " WHERE joke = 1");
            if (rs == null) {
                return false;
            }
            
            if (rs.getIntegerResult("COUNT", 0) != 1) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Tests the Speed of the Joke Database.
     *
     * @param arguments The arguments for the test.
     * @return The length in milliseconds of the test.
     */
    private static long testSpeed(String arguments) {
        String[] args = arguments.split(";", -1);
        String type = "";
        String tag = "";
        Boolean nsfw = null;
        String source = "";
        Integer lengthGt = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("ALL") || arg.equalsIgnoreCase("ONE") || arg.equalsIgnoreCase("RANDOM")) {
                type = arg;
            } else if (arg.startsWith("tag=")) {
                tag = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("nsfw=")) {
                nsfw = arg.substring(arg.indexOf('=') + 1).equalsIgnoreCase("true");
            } else if (arg.startsWith("source=")) {
                source = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("lengthGt=")) {
                lengthGt = Integer.valueOf(arg.substring(arg.indexOf('=') + 1));
            }
        }
        String typeQuery = (type.equalsIgnoreCase("ONE") ? " OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY" : (type.equalsIgnoreCase("RANDOM") ? " ORDER BY RANDOM() OFFSET 0 ROWS FETCH NEXT 1 ROW ONLY" : ""));
        
        String sql = "SELECT * FROM joke WHERE id > 0" +
                (tag.isEmpty() ? "" : (" AND id IN (SELECT joke FROM tag_" + tag.toLowerCase().replace(" ", "_") + typeQuery + ")")) +
                (nsfw == null ? "" : (" AND nsfw = " + (nsfw ? "true" : "false"))) +
                (source.isEmpty() ? "" : (" AND source = (SELECT id FROM source WHERE name = '" + source + "')")) +
                (lengthGt == null ? "" : (" AND length > " + lengthGt)) +
                typeQuery;
        
        long startTime = System.currentTimeMillis();
        FormattedResultSet rs = DatabaseManager.querySql(s, sql);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
    
}
