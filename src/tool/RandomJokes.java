/*
 * File:    RandomJokeTest.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.List;
import java.util.Scanner;

import main.Jokes;
import pojo.Joke;
import utility.ListUtility;
import utility.StringUtility;

/**
 * Displays random Jokes.
 */
public class RandomJokes {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        List<Joke> jokes = Jokes.readJokes(new File("jokes/jokes.json"));
        System.out.println("Ready");
        
        Scanner in = new Scanner(System.in);
        while (true) {
            in.nextLine();
            Joke joke = ListUtility.selectRandom(jokes);
            if (joke == null) {
                continue;
            }
            
            for (String s : StringUtility.wrapText(joke.text, 120)) {
                System.out.println(s);
            }
            System.out.println("Tags: ");
            StringBuilder tags = new StringBuilder();
            for (String tag : joke.tags) {
                tags.append((tags.length() > 0) ? ", " : "").append(tag);
            }
            System.out.println(tags);
            System.out.println("NSFW: " + joke.nsfw);
            System.out.println("Source: " + joke.source);
            System.out.println();
        }
    }
    
}
