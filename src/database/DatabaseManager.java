/*
 * File:    DatabaseManager.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages the Database.
 */
public final class DatabaseManager {
    
    /**
     * Sets up the Derby Database.
     *
     * @return Whether the Derby Database was set up properly or not.
     */
    public static boolean setupDatabase() {
        try {
            System.setProperty("derby.stream.error.file", "log" + File.separator + "derby.log");
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").getConstructor().newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            System.out.println("There was an error setting up the Derby Database");
            return false;
        }
        return true;
    }
    
    /**
     * Creates the Connection for a Database.
     *
     * @param db The Database to connect to.
     * @return The Connection that was made, or null if there was an error.
     */
    public static Connection connectToDatabase(String db) {
        Properties props = new Properties();
        props.put("user", "admin");
        props.put("password", "admin");
        
        try {
            Connection conn = DriverManager.getConnection("jdbc:derby:" + db + ";create=true", props);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            System.out.println("There was an error connecting to the Database: " + db);
            return null;
        }
    }
    
    /**
     * Determines if a connection is connected to a database or not.
     *
     * @param conn The connection to test.
     * @return Whether the connection is connected to a database or not.
     */
    public static boolean isConnected(Connection conn) {
        try {
            return !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Creates the Statement for a Database Connection.
     *
     * @param conn The Database Connection to create the Statement for.
     * @return The Statement that was created, or null if there was an error.
     */
    public static Statement createStatement(Connection conn) {
        try {
            Statement s = conn.createStatement();
            s.setMaxRows(Integer.MAX_VALUE);
            return s;
        } catch (SQLException e) {
            System.out.println("There was an error creating the Statement for the Database Connection");
            return null;
        }
    }
    
    /**
     * Executes an SQL statement.
     *
     * @param s   The statement to execute the SQL statement with.
     * @param sql The SQL statement.
     * @return Whether the SQL statement was successfully executed or not.
     */
    public static boolean executeSql(Statement s, String sql) {
        try {
            s.execute(sql);
        } catch (SQLException e) {
            System.out.println("There was an error executing the sql: " + sql);
            return false;
        }
        return true;
    }
    
    /**
     * Executes an SQL query statement.<br>
     * The returned ResultSet must be closed manually.
     *
     * @param s   The statement to execute the SQL query statement with.
     * @param sql The SQL query statement.
     * @return The formatted result set of the SQL query statement, or null if there was an error.
     */
    public static FormattedResultSet querySql(Statement s, String sql) {
        ResultSet rs;
        try {
            rs = s.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("There was an error querying the sql: " + sql);
            return null;
        }
        return new FormattedResultSet(rs);
    }
    
    /**
     * Closes a Statement for a Database Connection.
     *
     * @param s The Statement to close.
     * @return Whether the Statement was successfully closed or not.
     */
    public static boolean closeStatement(Statement s) {
        try {
            s.close();
        } catch (SQLException e) {
            System.out.println("There was an error closing the Statement for the Database Connection");
            return false;
        }
        return true;
    }
    
    /**
     * Closes the Connection for a Database.
     *
     * @param conn The Connection to close.
     * @return Whether the Connection to the Database was successfully closed or not.
     */
    public static boolean disconnectFromDatabase(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ignored) {
            System.out.println("There was an error disconnecting from the Database");
            return false;
        }
        return true;
    }
    
    /**
     * Shuts down the Derby Database.
     *
     * @return Whether the Derby Database was shut down properly or not.
     */
    public static boolean shutdownDatabase() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:derby:;shutdown=true");
            connection.close();
        } catch (SQLException ignored) {
            return true;
        }
        System.out.println("There was an error shutting down the Derby Database");
        return false;
    }
    
}
