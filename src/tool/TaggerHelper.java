/*
 * File:    TaggerHelper.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.Jokes;
import pojo.Joke;
import utility.ListUtility;
import utility.StringUtility;

/**
 * Helps with fixing tagging mistakes.
 */
public class TaggerHelper {
    
    //Main Methods
    
    /**
     * The Main method.
     *
     * @param args Arguments to the Main Method.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        List<Joke> tagged = new ArrayList<>();
        for (Jokes.JokeSet jokeSet : Jokes.JokeSet.values()) {
            File taggedFile = new File(jokeSet.directory, "source/4 - tagged/tagged.json");
            if (taggedFile.exists()) {
                tagged.addAll(Jokes.readJokes(taggedFile));
            }
        }
        
        Scanner input = new Scanner(System.in);
        while (true) {
            Joke joke = ListUtility.selectRandom(tagged);
            if (joke != null) {
                System.out.println("\n\n\n");
                for (String s : StringUtility.wrapText(joke.text, 120)) {
                    System.out.println(s);
                }
                StringBuilder tags = new StringBuilder();
                for (String tag : joke.tags) {
                    tags.append((tags.length() > 0) ? ", " : "").append(tag);
                }
                System.out.println(tags);
                input.nextLine();
            }
        }
    }
    
}
