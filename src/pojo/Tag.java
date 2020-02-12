/*
 * File:    Tag.java
 * Package: pojo
 * Author:  Zachary Gill
 */

package pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a Tag.
 */
public class Tag {
    
    //Fields
    
    /**
     * The name of the tag.
     */
    public String name = "";
    
    /**
     * The list of aliases of the tag.
     */
    public List<String> aliases = new ArrayList<>();
    
    /**
     * A flag indicating whether or not the tag is NSFW.
     */
    public boolean nsfw = false;
    
    /**
     * A flag indicating whether or not the tag is minor.
     */
    public boolean minor = false;
    
    /**
     * A flag indicating whether or not the tag is should process -S ending replacements.
     */
    public boolean dontDoS = false;
    
    /**
     * A flag indicating whether or not the tag is should process -ED ending replacements.
     */
    public boolean dontDoED = false;
    
    /**
     * A flag indicating whether or not the tag is should process -ES ending replacements.
     */
    public boolean dontDoES = false;
    
    /**
     * A flag indicating whether or not the tag is should process -IES ending replacements.
     */
    public boolean dontDoIES = false;
    
    /**
     * A flag indicating whether or not the tag is should process -Y ending replacements.
     */
    public boolean dontDoY = false;
    
    /**
     * A flag indicating whether or not the tag is should process -ING ending replacements.
     */
    public boolean dontDoING = false;
    
    /**
     * A flag indicating whether or not the tag is should process -TION ending replacements.
     */
    public boolean dontDoTION = false;
    
    /**
     * A flag indicating whether or not the tag is should process -ER ending replacements.
     */
    public boolean dontDoER = false;
    
    /**
     * A flag indicating whether or not the tag is should process -OR ending replacements.
     */
    public boolean dontDoOR = false;
    
    
    //Constructors
    
    /**
     * The default no-argument constructor for a Tag.
     */
    public Tag() {
    }
    
    /**
     * Clones a Tag from an existing Tag.
     *
     * @param tag The Tag to clone.
     */
    public Tag(Tag tag) {
        this.name = tag.name;
        this.aliases = new ArrayList<>(tag.aliases);
        this.nsfw = tag.nsfw;
        this.minor = tag.minor;
        this.dontDoS = tag.dontDoS;
        this.dontDoED = tag.dontDoED;
        this.dontDoES = tag.dontDoES;
        this.dontDoIES = tag.dontDoIES;
        this.dontDoY = tag.dontDoY;
        this.dontDoING = tag.dontDoING;
        this.dontDoTION = tag.dontDoTION;
        this.dontDoER = tag.dontDoER;
        this.dontDoOR = tag.dontDoOR;
    }
    
}
