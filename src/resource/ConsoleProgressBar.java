/*
 * File:    ConsoleProgressBar.java
 * Package: resource
 * Author:  Zachary Gill
 */

package resource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.BoundUtility;
import utility.StringUtility;

/**
 * A progress bar for the console.
 */
public class ConsoleProgressBar {
    
    //Logger
    
    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConsoleProgressBar.class);
    
    
    //Constants
    
    /**
     * The default width of the progress bar in characters.
     */
    public static final int DEFAULT_PROGRESS_BAR_WIDTH = 32;
    
    /**
     * The default value of the flag indicating whether or not to automatically print the progress bar after an update.
     */
    public static final boolean DEFAULT_PROGRESS_BAR_AUTO_PRINT = true;
    
    /**
     * The minimum number of nanoseconds that must pass before an update can occur.
     */
    public static final long PROGRESS_BAR_MINIMUM_UPDATE_DELAY = TimeUnit.MILLISECONDS.toNanos(200);
    
    
    //Fields
    
    /**
     * The title to display for the progress bar.
     */
    private String title;
    
    /**
     * The total progress of the progress bar.
     */
    private long total;
    
    /**
     * The current progress of the progress bar.
     */
    private long progress = 0;
    
    /**
     * The currently completed progress of the progress bar.
     */
    private long current = 0;
    
    /**
     * The completed size of the progress at the time of the last update.
     */
    private long previous = 0;
    
    /**
     * The initial progress of the progress bar.
     */
    private long initialProgress = 0;
    
    /**
     * The time of the current update of the progress bar.
     */
    private long currentUpdate = 0;
    
    /**
     * The time of the previous update of the progress bar.
     */
    private long previousUpdate = 0;
    
    /**
     * The time the progress bar was updated for the firstUpdate time.
     */
    private long firstUpdate = 0;
    
    /**
     * The width of the bar in the progress bar.
     */
    private int width;
    
    /**
     * The units of the progress bar.
     */
    private String units;
    
    /**
     * A flag indicating whether or not to automatically print the progress bar after an update.
     */
    private boolean autoPrint;
    
    /**
     * The current progress bar.
     */
    private String progressBar = "";
    
    /**
     * A flag indicating whether there was an update to the progress bar or not.
     */
    private boolean update = false;
    
    
    //Constructors
    
    /**
     * Creates a new ConsoleProgressBar object.
     *
     * @param title     The title to display for the progress bar.
     * @param total     The total size of the progress bar.
     * @param width     The with of the bar in the progress bar.
     * @param units     The units of the progress bar.
     * @param autoPrint Whether or not to automatically print the progress bar after an update.
     */
    public ConsoleProgressBar(String title, long total, int width, String units, boolean autoPrint) {
        this.title = title;
        this.total = total;
        this.width = width;
        this.units = units;
        this.autoPrint = autoPrint;
    }
    
    /**
     * Creates a new ConsoleProgressBar object.
     *
     * @param title The title to display for the progress bar.
     * @param total The total size of the progress bar.
     * @param width The with of the bar in the progress bar.
     * @param units The units of the progress bar.
     */
    public ConsoleProgressBar(String title, long total, int width, String units) {
        this(title, total, width, units, DEFAULT_PROGRESS_BAR_AUTO_PRINT);
    }
    
    /**
     * Creates a new ConsoleProgressBar object.
     *
     * @param title The title to display for the progress bar.
     * @param total The total size of the progress bar.
     * @param units The units of the progress bar.
     */
    public ConsoleProgressBar(String title, long total, String units) {
        this(title, total, DEFAULT_PROGRESS_BAR_WIDTH, units);
    }
    
    /**
     * Creates a new ConsoleProgressBar object.
     *
     * @param title The title to display for the progress bar.
     * @param total The total size of the progress bar.
     * @param width The with of the bar in the progress bar.
     */
    public ConsoleProgressBar(String title, long total, int width) {
        this(title, total, width, "");
    }
    
    /**
     * Creates a new ConsoleProgressBar object.
     *
     * @param title The title to display for the progress bar.
     * @param total The total size of the progress bar.
     */
    public ConsoleProgressBar(String title, long total) {
        this(title, total, DEFAULT_PROGRESS_BAR_WIDTH, "");
    }
    
    
    //Methods
    
    /**
     * Builds the progress bar.<br>
     * This must be displayed with print(), not println().
     *
     * @return The progress bar.
     */
    @SuppressWarnings("HardcodedLineSeparator")
    public String get() {
        if (update) {
            progressBar = '\r' + StringUtility.spaces(width + ((String.valueOf(total).length() + units.length()) * 2) + 30) + '\r' +
                    getPercentageString() + ' ' +
                    getBarString() + ' ' +
                    getRatioString() + " - " +
                    getTimeRemainingString();
        }
        
        return progressBar;
    }
    
    /**
     * Updates the progress bar.<br>
     * If the time between updates is less than PROGRESS_BAR_MINIMUM_UPDATE_DELAY then the update will not take place until called again after the delay.
     *
     * @param newProgress The new progress of the progress bar.
     * @return Whether the progress bar was updated or not.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public synchronized boolean update(long newProgress) {
        if (isComplete()) {
            return false;
        }
        
        if (firstUpdate == 0) {
            if (!title.isEmpty()) {
                System.out.println(Console.cyan(title + ": "));
                System.out.flush();
            }
            firstUpdate = System.nanoTime();
        }
        
        progress = BoundUtility.truncateNum(newProgress, 0, total).longValue();
        
        if (((System.nanoTime() - currentUpdate) >= PROGRESS_BAR_MINIMUM_UPDATE_DELAY) || (progress == total)) {
            previous = current;
            current = progress;
            
            previousUpdate = currentUpdate;
            currentUpdate = System.nanoTime();
            
            update = true;
        }
        
        if (update && autoPrint) {
            print();
        }
        return update;
    }
    
    /**
     * Adds one to the current progress.
     *
     * @return Whether the progress bar was updated or not.
     */
    public synchronized boolean addOne() {
        return update(progress + 1);
    }
    
    /**
     * Prints the progress bar to the console.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public synchronized void print() {
        String bar = get();
        System.out.print(bar);
        System.out.flush();
        System.err.flush();
    }
    
    /**
     * Calculates the ratio of the progress bar.
     *
     * @return The ratio of the progress bar.
     */
    public double getRatio() {
        return (double) current / total;
    }
    
    /**
     * Calculates the percentage of the progress bar.
     *
     * @return The percentage of the progress bar.
     */
    public int getPercentage() {
        return (int) (getRatio() * 100);
    }
    
    /**
     * Calculates the last recorded speed of the progress bar.
     *
     * @return The last recorded speed of the progress bar in units per second.
     */
    public double getLastSpeed() {
        double recentTime = (double) (currentUpdate - previousUpdate) / TimeUnit.SECONDS.toNanos(1);
        if ((recentTime == 0) || (previousUpdate == 0)) {
            return 0;
        }
        long recentProgress = current - previous;
        
        return recentProgress / recentTime;
    }
    
    /**
     * Calculates the average speed of the progress bar.
     *
     * @return The average speed of the progress bar in units per second.
     */
    public double getAverageSpeed() {
        double totalTime = (double) (currentUpdate - firstUpdate) / TimeUnit.SECONDS.toNanos(1);
        if ((totalTime == 0) || (firstUpdate == 0)) {
            return 0;
        }
        
        return current / totalTime;
    }
    
    /**
     * Estimates the time remaining in seconds.
     *
     * @return The estimated time remaining in seconds.
     */
    public long getTimeRemaining() {
        long remaining = total - current;
        if (remaining == 0) {
            return 0;
        }
        if (current == 0) {
            return Long.MAX_VALUE;
        }
        
        long timeRemaining = (long) (((double) remaining / (current - initialProgress)) * (currentUpdate - firstUpdate));
        return TimeUnit.NANOSECONDS.toSeconds(timeRemaining);
    }
    
    /**
     * Determines if the progress bar is complete or not.
     *
     * @return Whether the progress bar is complete or not.
     */
    public boolean isComplete() {
        return (current == total);
    }
    
    /**
     * Completes the progress bar.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void complete() {
        current = total;
        update = true;
        String completeProgressBar = get();
        System.out.println(completeProgressBar);
        System.out.flush();
        System.err.flush();
    }
    
    /**
     * Builds the percentage string for the progress bar.
     *
     * @return The percentage string.
     */
    public String getPercentageString() {
        int percentage = getPercentage();
        String percentageString = StringUtility.padLeft(String.valueOf(percentage), 3);
        
        return ((percentage == 100) ? Console.cyan(percentageString) : Console.green(percentageString)) + '%';
    }
    
    /**
     * Builds the progress bar string for the progress bar.
     *
     * @return The progress bar string.
     */
    public String getBarString() {
        double ratio = getRatio();
        int completed = (int) ((double) width * ratio);
        int remaining = width - completed;
        
        StringBuilder bar = new StringBuilder();
        bar.append('[');
        StringBuilder progress = new StringBuilder();
        for (int i = 0; i < completed; i++) {
            progress.append('=');
        }
        if (completed != width) {
            progress.append('>');
            remaining--;
        }
        bar.append((completed == width) ? Console.cyan(progress.toString()) : Console.green(progress.toString()));
        for (int i = 0; i < remaining; i++) {
            bar.append(' ');
        }
        bar.append(']');
        
        return bar.toString();
    }
    
    /**
     * Builds the ratio string for the progress bar.
     *
     * @return The ratio string.
     */
    public String getRatioString() {
        String formattedCurrent = StringUtility.padLeft(String.valueOf(current), String.valueOf(total).length());
        
        return ((current == total) ? Console.cyan(formattedCurrent) : Console.green(formattedCurrent)) +
                units + '/' +
                Console.cyan(String.valueOf(total)) +
                units;
    }
    
    /**
     * Builds the time remaining string for the progress bar.
     *
     * @return The time remaining string.
     */
    public String getTimeRemainingString() {
        long time = getTimeRemaining();
        
        if (current == total) {
            return Console.cyan("Complete");
        }
        if (time == Long.MAX_VALUE) {
            return "ETA: --:--:--";
        }
        
        int hours = (int) ((double) time / Duration.ofHours(1).getSeconds());
        time -= hours * TimeUnit.HOURS.toSeconds(1);
        
        int minutes = (int) ((double) time / Duration.ofMinutes(1).getSeconds());
        time -= minutes * TimeUnit.MINUTES.toSeconds(1);
        
        int seconds = (int) time;
        
        return "ETA: " + StringUtility.padZero(hours, 2) + ':' + StringUtility.padZero(minutes, 2) + ':' + StringUtility.padZero(seconds, 2);
    }
    
    
    //Setters
    
    /**
     * Sets the total progress of the progress bar.
     *
     * @param total The total progress of the progress bar.
     */
    public void setTotal(long total) {
        this.total = total;
    }
    
    /**
     * Sets the initial progress of the progress bar.
     *
     * @param initialProgress The initial progress of the progress bar.
     */
    public void setInitialProgress(long initialProgress) {
        this.initialProgress = initialProgress;
    }
    
    /**
     * Sets the flag indicating whether or not to automatically print the progress bar after an update.
     *
     * @param autoPrint The flag indicating whether or not to automatically print the progress bar after an update.
     */
    public void setAutoPrint(boolean autoPrint) {
        this.autoPrint = autoPrint;
    }
    
}
