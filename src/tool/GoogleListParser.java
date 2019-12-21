/*
 * File:    GoogleListParser.java
 * Package: worker
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utility.Filesystem;
import utility.StringUtility;

/**
 * Parses a Google list.
 */
public final class GoogleListParser {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args The arguments to the main method.
     */
    public static void main(String[] args) {
        File googleListHtml = new File("etc/google.html");
        
        Pattern textPattern = Pattern.compile("\\s*<div\\sclass=\"wfg6Pb\">(?<text>[^<]*)</div>\\s*");
        List<String> results = new ArrayList<>();
        
        StringBuilder text = new StringBuilder();
        List<String> lines = Filesystem.readLines(googleListHtml);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher textMatcher = textPattern.matcher(line);
            if (textMatcher.matches()) {
                text.append(textMatcher.group("text"));
                for (i = i + 1; i < lines.size(); i++) {
                    line = lines.get(i);
                    if (line.contains("P2m1Af")) {
                        i += 2;
                        break;
                    }
                    textMatcher = textPattern.matcher(line);
                    if (textMatcher.matches()) {
                        text.append(textMatcher.group("text"));
                    } else {
                        break;
                    }
                }
                if (text.length() > 0) {
                    String finalText = text.toString().replace("'", "");
                    if (finalText.contains("-")) {
                        results.add(StringUtility.toTitleCase(finalText.replace("-", "")));
                        results.add(StringUtility.toTitleCase(finalText.replace("-", " ")));
                    } else {
                        results.add(StringUtility.toTitleCase(finalText));
                    }
                    
                    text = new StringBuilder();
                }
            }
        }
        
        for (String result : results) {
            System.out.println(result);
        }
    }
    
}
