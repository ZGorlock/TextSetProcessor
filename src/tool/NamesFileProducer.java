/*
 * File:    NamesFileProducer.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import utility.Filesystem;
import utility.StringUtility;

/**
 * Produces the names file.
 */
public class NamesFileProducer {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        Filesystem.writeLines(new File("etc/dicts/names.txt"), 
                Filesystem.getFiles(new File("etc/dicts/original/names")).stream()
                          .map(Filesystem::readLines)
                          .flatMap(Collection::stream)
                          .map(e -> e.substring(0, e.indexOf(',')))
                          .filter(StringUtility::isAlphabetic)
                          .filter(e -> e.length() > 2)
                          .distinct()
                          .sorted(Comparator.naturalOrder())
                          .collect(Collectors.toList())
        );
    }
    
}