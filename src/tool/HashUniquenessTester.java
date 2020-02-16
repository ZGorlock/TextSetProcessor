/*
 * File:    HashUniquenessTester.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import main.Jokes;
import pojo.Joke;

/**
 * Tests if the hashes for Jokes are unique or not.
 */
public class HashUniquenessTester {
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        HashSet<Long> hashes = new HashSet<>();
        List<Joke> jokes = Jokes.readJokes(new File("jokes/jokes.json"));
        jokes.forEach(e -> hashes.add(e.hash));
        System.out.println("Number of Jokes:  " + jokes.size());
        System.out.println("Number of Hashes: " + hashes.size());
    }
    
}
