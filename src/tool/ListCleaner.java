/*
 * File:    ListCleaner.java
 * Package: worker
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import utility.Filesystem;
import utility.ListUtility;
import utility.StringUtility;
import worker.TextFixer;

/**
 * Cleans the joke alias lists.
 */
public final class ListCleaner {
    
    //Main Method
    
    /**
     * The main method.
     *
     * @param args The arguments to the main method.
     */
    public static void main(String[] args) {
        TextFixer textFixer = TextFixer.getInstance();
        textFixer.load();
        
        List<File> files = Filesystem.getFiles(new File("etc/lists/"));
        files.addAll(Arrays.asList(
                new File("etc/dicts/dict-local.txt"),
                new File("etc/dicts/dict-contractions.txt"),
                new File("etc/dicts/dict-nsfw.txt"),
                new File("etc/dicts/cities.txt"),
                new File("etc/dicts/countries.txt"),
                new File("etc/dicts/famousPeople.txt"),
                new File("etc/dicts/names.txt"),
                new File("etc/dicts/subCountries.txt"),
                new File("etc/other/fileExtensions.txt")));
        
        for (File f : files) {
            boolean isDict = f.getName().contains("dict-");
            List<String> a = Filesystem.readLines(f);
            List<String> b = new ArrayList<>();
            for (String as : a) {
                String work = StringUtility.trim(as.replaceAll("\\s+", " "));
                work = textFixer.replaceDiacritics(work);
                b.add(isDict ? work.toLowerCase() : StringUtility.toTitleCase(work));
            }
            b = ListUtility.removeDuplicates(b);
            b.sort(Comparator.naturalOrder());
            Filesystem.writeLines(f, b);
        }
    }
    
}
