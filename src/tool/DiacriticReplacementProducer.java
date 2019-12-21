/*
 * File:    DiacriticReplacementProducer.java
 * Package: tool
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utility.Filesystem;
import worker.TextFixer;

/**
 * Produces the diacritic replacement file.
 */
public final class DiacriticReplacementProducer {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args Arguments to the main method.
     */
    public static void main(String[] args) {
        TextFixer textFixer = TextFixer.getInstance();
        textFixer.load();
        
        Map<String, String> replacements = new HashMap<>();
        for (Map.Entry<String, String> diacritic : textFixer.diacritics.entrySet()) {
            if (!replacements.containsKey(diacritic.getValue())) {
                replacements.put(diacritic.getValue(), diacritic.getKey());
            } else {
                replacements.put(diacritic.getValue(), replacements.get(diacritic.getValue()) + "|" + diacritic.getKey());
            }
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            out.add(replacement.getKey() + " : " + replacement.getValue());
        }
        Filesystem.writeLines(new File("etc/other/diacritics_replacements.txt"), out);
    }
    
}
