/*
 * File:    ListCleaner.java
 * Package: worker
 * Author:  Zachary Gill
 */

package tool;

import java.io.File;
import java.util.ArrayList;
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
        List<String> good = new ArrayList<>();
        List<String> list = Filesystem.readLines(new File("jokes/reddit/source/3 - fixed/fixed-fixList.json"));

        for (String i : list) {
            if (i.substring(0, 1).toUpperCase().equals(i.substring(0, 1)) &&
                i.substring(1).toLowerCase().equals(i.substring(1))) {
                good.add(i);
            } else {
                if (i.toUpperCase().contains("MOO") || i.toUpperCase().contains("PURR") ||
                    i.toUpperCase().contains("MEOW") || i.contains("'")) {
                    good.add(i);
                }
            }
        }

        Filesystem.writeLines(new File("jokes/reddit/source/3 - fixed/fixed-fixList1.txt"), good);
        
//        TextFixer textFixer = TextFixer.getInstance();
//        textFixer.load();
//
//        List<File> files = Filesystem.listFiles(new File("etc/lists/"), var -> true);
//        files.add(new File("etc/dicts/cities.txt"));
//        files.add(new File("etc/dicts/countries.txt"));
//        files.add(new File("etc/dicts/famousPeople.txt"));
//        files.add(new File("etc/dicts/names.txt"));
//        files.add(new File("etc/dicts/subCountries.txt"));
//        files.add(new File("etc/other/fileExtensions.txt"));
//
//        for (File f : files) {
//            List<String> a = Filesystem.readLines(f);
//            List<String> b = new ArrayList<>();
//            for (String as : a) {
//                String work = as.replaceAll("\\s+", " ").replaceAll("^\\s+", "").replaceAll("\\s+$", "");
//                work = textFixer.replaceDiacritics(work);
//                b.add(StringUtility.toTitleCase(work));
//            }
//            b = ListUtility.removeDuplicates(b);
//            b.sort(Comparator.naturalOrder());
//            Filesystem.writeLines(f, b);
//        }
    }
    
}
