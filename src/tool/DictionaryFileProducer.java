/*
 * File:    DictionaryFileProducer.java
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
 * Produces the dictionary file.
 */
public class DictionaryFileProducer {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        Filesystem.writeLines(new File("etc/dicts/dict.txt"),
                Filesystem.getFiles(new File("etc/dicts/original")).stream()
                        .map(Filesystem::readLines)
                        .flatMap(Collection::stream)
                        .filter(StringUtility::isAlphabetic)
                        .map(String::toLowerCase)
                        .distinct()
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList())
        );
    }
    
}