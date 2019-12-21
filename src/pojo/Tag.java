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
    
}
