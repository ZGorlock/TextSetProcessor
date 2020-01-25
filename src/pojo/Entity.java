/*
 * File:    Entity.java
 * Package: pojo
 * Author:  Zachary Gill
 */

package pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an Entity.
 */
class Entity {
    
    //Fields
    
    /**
     * The title of the entity.
     */
    public String title = "";
    
    /**
     * The body of the entity.
     */
    public String body = "";
    
    /**
     * The text of the entity.
     */
    public String text = "";
    
    /**
     * The length of the text of the entity.
     */
    public long length = 0;
    
    /**
     * The source of the entity.
     */
    public String source = "";
    
    /**
     * A flag indicating whether or not the entity is NSFW.
     */
    public boolean nsfw = false;
    
    /**
     * The list of tags associated with the entity.
     */
    public List<String> tags = new ArrayList<>();
    
    /**
     * The list of words in the text of the entity that did not pass the Spell Checker.
     */
    public List<String> fix = new ArrayList<>();
    
    /**
     * The hash of the entity.
     */
    public int hash;
    
}
