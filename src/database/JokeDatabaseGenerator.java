/*
 * File:    JokeDatabaseGenerator.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Generates the Joke Database.
 */
public class JokeDatabaseGenerator {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        try {
            System.setProperty("derby.stream.error.file", "log" + File.separator + "derby.log");
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").getConstructor().newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            System.out.println("There was an error setting up the DatabaseManager");
            return;
        }
        
        
        String db = "jokes/db/jokes";
        Properties props = new Properties();
        props.put("user", "admin");
        props.put("password", "admin");
        
        try {
            Connection conn = DriverManager.getConnection("jdbc:derby:" + db + ";create=true", props);
            conn.setAutoCommit(true);
            
            Statement s = conn.createStatement();
            s.setMaxRows(Integer.MAX_VALUE);
            
            List<String> sql = new ArrayList<>();
            sql.add("CREATE TABLE source (" +
                    "id INT NOT NULL, " + "" +
                    "name VARCHAR(16) UNIQUE NOT NULL, " +
                    "CONSTRAINT source_pk PRIMARY KEY (id)" +
                    ");");
            sql.add("INSERT INTO source VALUES(0, 'Quirkology');");
            sql.add("INSERT INTO source VALUES(1, 'Jokeriot');");
            sql.add("INSERT INTO source VALUES(2, 'StupidStuff');");
            sql.add("INSERT INTO source VALUES(3, 'Wocka');");
            sql.add("INSERT INTO source VALUES(4, 'Reddit');");
            
            sql.add("CREATE TABLE joke (" +
                    "id INT NOT_NULL, " +
                    "text VARCHAR(32768) NOT_NULL, " +
                    "length INT NOT_NULL, " +
                    "source INT NOT_NULL, " +
                    "nsfw BOOLEAN NOT_NULL, " +
                    "CONSTRAINT joke_pk PRIMARY KEY (id), " +
                    "CONSTRAINT joke_source_fk FOREIGN KEY (source) REFERENCES source(id)" +
                    ");");
            sql.add("INSERT INTO jokes VALUES(1, 'What kind of pig can you ignore at a party? A wild bore.', 56, 0, 0);");
            
            for (String sqlStatement : sql) {
                s.execute(sqlStatement);
            }
            
        } catch (Exception ignored) {
        }
        
        
        try {
            Connection connection = DriverManager.getConnection("jdbc:derby:;shutdown=true");
            connection.close();
        } catch (SQLException ignored) {
        }
    }
    
}
