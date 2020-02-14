/*
 * File:    FormattedResultSet.java
 * Package: database
 * Author:  Zachary Gill
 */

package database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class that defines a formatted ResultSet.
 */
public final class FormattedResultSet {
    
    //Fields
    
    /**
     * The formatted result set.
     */
    private Map<String, List<Object>> formattedResultSet = new HashMap<>();
    
    
    //Constructors
    
    /**
     * Constructor for a FormattedResultSet from a ResultSet.
     *
     * @param rs The ResultSet.
     */
    public FormattedResultSet(ResultSet rs) {
        if (rs == null) {
            formattedResultSet = null;
            return;
        }
        
        try {
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                formattedResultSet.put(resultSetMetaData.getColumnLabel(i).toUpperCase(), new ArrayList<>());
            }
            
            while (rs.next()) {
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    String column = resultSetMetaData.getColumnLabel(i).toUpperCase();
                    formattedResultSet.get(column).add(rs.getObject(column));
                }
            }
            
            rs.close();
            
        } catch (SQLException e) {
            formattedResultSet = null;
        }
    }
    
    
    //Methods
    
    /**
     * Returns the number of rows in the formatted result set.
     *
     * @return The number of rows in the formatted result set or -1 if it is null.
     */
    public int getRowCount() {
        if (formattedResultSet == null) {
            return -1;
        }
        Entry<String, List<Object>> entry = formattedResultSet.entrySet().iterator().next();
        return entry.getValue().size();
    }
    
    /**
     * Returns a column from the formatted result set.
     *
     * @param column The name of the column to retrieve.
     * @return The column from the formatted result set or null if it does not exist.
     */
    public List<Object> getColumn(String column) {
        if (formattedResultSet == null) {
            return null;
        }
        return formattedResultSet.get(column.toUpperCase());
    }
    
    /**
     * Returns the size of the column from the formatted result set.
     *
     * @param column The name of the column to retrieve.
     * @return The size of the column from the formatted result set or -1 if it does not exist.
     */
    public int getColumnSize(String column) {
        List<Object> formattedColumn = getColumn(column);
        if (formattedColumn == null) {
            return -1;
        }
        return formattedColumn.size();
    }
    
    /**
     * Returns a result from the formatted result set.
     *
     * @param column The name of the column to retrieve the result from.
     * @param row    The row of the column to retrieve, starting with 0.
     * @return The result from the formatted result set or null if it does not exist.
     */
    public Object getResult(String column, int row) {
        List<Object> resultColumn = getColumn(column);
        if ((resultColumn == null) || (row < 0) || (row >= resultColumn.size())) {
            return null;
        }
        return resultColumn.get(row);
    }
    
    /**
     * Returns a string result from the formatted result set.
     *
     * @param column The name of the column to retrieve the result from.
     * @param row    The row of the column to retrieve, starting with 0.
     * @return The string result from the formatted result set or null if it does not exist.
     */
    public String getStringResult(String column, int row) {
        Object result = getResult(column, row);
        if (result == null) {
            return null;
        }
        return result.toString();
    }
    
    /**
     * Returns an int result from the formatted result set.
     *
     * @param column The name of the column to retrieve the result from.
     * @param row    The row of the column to retrieve, starting with 0.
     * @return The int result from the formatted result set or null if it does not exist.
     */
    @SuppressWarnings("ConstantConditions")
    public Integer getIntResult(String column, int row) {
        try {
            return Integer.valueOf(getStringResult(column, row));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    
    /**
     * Returns an boolean result from the formatted result set.
     *
     * @param column The name of the column to retrieve the result from.
     * @param row    The row of the column to retrieve, starting with 0.
     * @return The boolean result from the formatted result set or null if it does not exist.
     */
    @SuppressWarnings("ConstantConditions")
    public Boolean getBooleanResult(String column, int row) {
        try {
            return Boolean.valueOf(getStringResult(column, row));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    
    /**
     * Returns a long result from the formatted result set.
     *
     * @param column The name of the column to retrieve the result from.
     * @param row    The row of the column to retrieve, starting with 0.
     * @return The long result from the formatted result set or null if it does not exist.
     */
    @SuppressWarnings("ConstantConditions")
    public Long getLongResult(String column, int row) {
        try {
            return Long.valueOf(getStringResult(column, row));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    
}