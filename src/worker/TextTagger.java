/*
 * File:    TextTagger.java
 * Package: worker
 * Author:  Zachary Gill
 */

package worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pojo.Tag;
import utility.Filesystem;
import utility.ListUtility;
import utility.StringUtility;

/**
 * Tags Text.
 */
public final class TextTagger {
    
    //Constants
    
    /**
     * The base regex pattern for matching tags.
     */
    private static final String tagRegexBase = "^(Y*XY.*)|(.*YXY*)|(.*YXY.*)|(Y*XY*)$";
    
    /**
     * The regex pattern for matching tags.
     */
    private static final String tagRegex = tagRegexBase.replace("Y", "[^A-Z0-9]");
    
    /**
     * The regex pattern for matching tags with special characters.
     */
    private static final String tagRegexCombined = tagRegexBase.replace("Y", "[^A-Z0-9&\\-]");
    
    
    //Static Fields
    
    /**
     * The singleton instance of the Text Tagger.
     */
    private static TextTagger instance = null;
    
    /**
     * A flag indicating whether or not the Text Tagger has been loaded yet or not.
     */
    private static AtomicBoolean loaded = new AtomicBoolean(false);
    
    
    //Fields
    
    /**
     * The tag map.
     */
    public final Map<String, Tag> tagList = new LinkedHashMap<>();
    
    /**
     * A map between tag endings to a list of tags that should not use them.
     */
    public final Map<String, List<String>> tagEndingToDontDoList = new HashMap<>();
    
    /**
     * A map between tag endings and possible replacements for them.
     */
    public final Map<String, List<String>> tagEndingToReplacements = new HashMap<>();
    
    /**
     * Stores special tags for processing.
     */
    public final Map<String, Tag> specialTagList = new HashMap<>();
    
    /**
     * A flag indicating whether or not to load extra tag alias lists.
     */
    public boolean loadTagLists = true;
    
    /**
     * A flag indicating whether or not to print the tags after they are loaded.
     */
    public boolean printTags = false;
    
    /**
     * A flag indicating whether or not to print the word that triggered the tagging.
     */
    public boolean printTagTrigger = false;
    
    /**
     * A flag indicating whether or not to remove potentially obsolete aliases.
     */
    public boolean removeObsoleteAliases = false;
    
    
    //Constructors
    
    /**
     * The private constructor for the Text Tagger.
     */
    private TextTagger() {
    }
    
    /**
     * Returns the singleton instance of the Text Tagger.
     *
     * @return The singleton instance of the Text Tagger.
     */
    public static TextTagger getInstance() {
        if (instance == null) {
            instance = new TextTagger();
        }
        return instance;
    }
    
    /**
     * Loads the Text Tagger.
     */
    public void load() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        
        System.out.print("Loading Tags... ");
        
        tagEndingToReplacements.put("", Arrays.asList("", "S", "ES", "Y", "ER", "ERS", "EST", "ED", "ING", "I", "IC", "MAN"));
        tagEndingToReplacements.put("S", Arrays.asList("", "ING", "ED"));
        tagEndingToReplacements.put("ED", Collections.singletonList(""));
        tagEndingToReplacements.put("ES", Arrays.asList("", "ING", "ED"));
        tagEndingToReplacements.put("IES", Arrays.asList("Y", "C"));
        tagEndingToReplacements.put("Y", Arrays.asList("", "ER", "ERS", "IES", "IST", "IC", "ICAL", "ICALLY"));
        tagEndingToReplacements.put("ING", Arrays.asList("", "E", "ER", "ERS", "ED", "ES", "S"));
        tagEndingToReplacements.put("TION", Arrays.asList("T", "TE", "TEE", "TED", "TOR", "TING"));
        tagEndingToReplacements.put("TATION", Arrays.asList("T", "TE", "TEE", "TED", "TOR", "TING"));
        tagEndingToReplacements.put("ER", Arrays.asList("", "E", "OR", "ORS", "ATION", "ING"));
        tagEndingToReplacements.put("OR", Arrays.asList("", "E", "ER", "ERS", "ATION", "ING"));
        
        tagList.putAll(parseTags());
        
        tagEndingToDontDoList.put("", Collections.emptyList());
        tagEndingToDontDoList.put("S", tagList.values().stream().filter(tag -> tag.dontDoS).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ED", tagList.values().stream().filter(tag -> tag.dontDoED).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ES", tagList.values().stream().filter(tag -> tag.dontDoES).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("IES", tagList.values().stream().filter(tag -> tag.dontDoIES).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("Y", tagList.values().stream().filter(tag -> tag.dontDoY).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ING", tagList.values().stream().filter(tag -> tag.dontDoING).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("TION", tagList.values().stream().filter(tag -> tag.dontDoTION).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("TATION", tagList.values().stream().filter(tag -> tag.dontDoTION).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ER", tagList.values().stream().filter(tag -> tag.dontDoER).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("OR", tagList.values().stream().filter(tag -> tag.dontDoOR).map(tag -> tag.name).collect(Collectors.toList()));
        
        int totalKeywords = 0;
        for (Tag tag : tagList.values()) {
            List<String> appendings = new ArrayList<>();
            for (String ending : tagEndingToDontDoList.keySet()) {
                if (tag.name.toUpperCase().endsWith(ending) && !tagEndingToDontDoList.get(ending).contains(tag.name)) {
                    appendings.addAll(tagEndingToReplacements.get(ending));
                }
            }
            appendings = ListUtility.removeDuplicates(appendings);
            int tagKeywords = appendings.size();
            for (String alias : tag.aliases) {
                tagKeywords += (2 + ((alias.length() > 4) ? 1 : 0) + (alias.toUpperCase().endsWith("Y") ? 1 : 0));
            }
            totalKeywords += tagKeywords;
        }
        
        System.out.println("(" + tagList.size() + " Tags : " + totalKeywords + " Keywords)");
    }
    
    /**
     * Parses the tags file and produces a tag map.
     *
     * @return The tag map produced from the tags file.
     */
    private Map<String, Tag> parseTags() {
        Map<String, Tag> tags = new LinkedHashMap<>();
        
        Pattern dontDoXPattern = Pattern.compile("^.+\\s-x\\[(?<list>[^]]*)].*$");
        for (String line : Filesystem.readLines(new File("etc/tags.txt"))) {
            Tag thisTag = new Tag();
            if (line.contains(" -nsfw")) {
                thisTag.nsfw = true;
                line = line.replace(" -nsfw", "");
            }
            if (line.contains(" -minor")) {
                thisTag.minor = true;
                line = line.replace(" -minor", "");
            }
            if (line.contains(" -dontDoS")) {
                thisTag.dontDoS = true;
                line = line.replace(" -dontDoS", "");
            }
            if (line.contains(" -dontDoED")) {
                thisTag.dontDoED = true;
                line = line.replace(" -dontDoED", "");
            }
            if (line.contains(" -dontDoES")) {
                thisTag.dontDoES = true;
                line = line.replace(" -dontDoES", "");
            }
            if (line.contains(" -dontDoIES")) {
                thisTag.dontDoIES = true;
                line = line.replace(" -dontDoIES", "");
            }
            if (line.contains(" -dontDoY")) {
                thisTag.dontDoY = true;
                line = line.replace(" -dontDoY", "");
            }
            if (line.contains(" -dontDoING")) {
                thisTag.dontDoING = true;
                line = line.replace(" -dontDoING", "");
            }
            if (line.contains(" -dontDoTION")) {
                thisTag.dontDoTION = true;
                line = line.replace(" -dontDoTION", "");
            }
            if (line.contains(" -dontDoER")) {
                thisTag.dontDoER = true;
                line = line.replace(" -dontDoER", "");
            }
            if (line.contains(" -dontDoOR")) {
                thisTag.dontDoOR = true;
                line = line.replace(" -dontDoOR", "");
            }
            
            String[] tokens = line.split(",");
            thisTag.name = StringUtility.trim(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                String alias = StringUtility.trim(tokens[i]);
                if (!alias.isEmpty()) {
                    thisTag.aliases.add(alias);
                }
            }
            
            tags.put(thisTag.name, thisTag);
        }
        
        if (printTags) {
            System.out.println();
            for (String tag : tags.keySet()) {
                System.out.println(tag);
            }
        }
        
        addListToTag(tags.get("Actor"), "actors");
        addListToTag(tags.get("Africa"), "countriesInAfrica");
        addListToTag(tags.get("Airplane"), "airlines", "airplanes", "airplaneParts");
        addListToTag(tags.get("Alcohol"), "alcoholicDrinks", "alcohols", "beers", "vodkas", "whiskeys", "wines");
        addListToTag(tags.get("Alligator"), "alligators");
        addListToTag(tags.get("Amphibian"), "amphibians", "frogs", "toads");
        addListToTag(tags.get("Anatomy"), "bones");
        addListToTag(tags.get("Animal"), "alligators", "amphibians", "animals", "animalTypes", "bears", "bees", "birds", "bugs", "camels", "cats", "chickens", "cows", "crabs", "crocodiles", "crustaceans", "caimans", "dinosaurs", "dogs", "dolphins", "ducks", "fishes", "flies", "frogs", "toads", "goats", "gorillas", "horses", "kangaroos", "koalas", "lions", "lobsters", "mammals", "mice", "monkeys", "parrots", "penguins", "pigs", "rabbits", "rats", "reptiles", "sharks", "shrimps", "snails", "snakes", "turkeys", "turtles", "whales");
        addListToTag(tags.get("Art"), "artists");
        addListToTag(tags.get("Asian"), "countriesInAsia");
        addListToTag(tags.get("Astrology"), "astrologicalSigns");
        addListToTag(tags.get("Athlete"), "athletes");
        addListToTag(tags.get("Aviation"), "airplanes", "airplaneParts");
        addListToTag(tags.get("Baptist"), "baptists");
        addListToTag(tags.get("Baseball"), "baseballPlayers", "baseballPositions", "baseballTeams");
        addListToTag(tags.get("Basketball"), "basketballPlayers", "basketballPositions", "basketballTeams");
        addListToTag(tags.get("Battery"), "batteries", "batteryTypes");
        addListToTag(tags.get("Bear"), "bears");
        addListToTag(tags.get("Bee"), "bees");
        addListToTag(tags.get("Beer"), "beers");
        addListToTag(tags.get("Biology"), "biologists", "biologicalScienceBranches");
        addListToTag(tags.get("Bird"), "birds", "chickens", "ducks", "parrots", "penguins", "turkeys");
        addListToTag(tags.get("Board Game"), "boardGames");
        addListToTag(tags.get("Boat"), "boats", "boatParts");
        addListToTag(tags.get("Bone"), "bones");
        addListToTag(tags.get("Book"), "books", "bookstores");
        addListToTag(tags.get("Bookstore"), "bookstores");
        addListToTag(tags.get("Boxing"), "boxers");
        addListToTag(tags.get("Brain"), "brainParts");
        addListToTag(tags.get("Bug"), "bees", "bugs", "flies", "spiders", "snails");
        addListToTag(tags.get("Camel"), "camels");
        addListToTag(tags.get("Candy"), "candies", "chocolates");
        addListToTag(tags.get("Car"), "cars", "carTypes");
        addListToTag(tags.get("Cat"), "cats");
        addListToTag(tags.get("Celebrity"), "actors", "baseballPlayers", "basketballPlayers", "boxers", "chefs", "comedians", "countryMusicSingers", "footballPlayers", "golfPlayers", "hockeyPlayers", "musicians", "rappers", "soccerPlayers", "tennisPlayers", "volleyballPlayers", "wrestlers");
        addListToTag(tags.get("Chemistry"), "chemists", "chemistryElements", "chemistryTools");
        addListToTag(tags.get("Chicken"), "chickens");
        addListToTag(tags.get("Christian"), "christians", "baptists", "protestants");
        addListToTag(tags.get("Cinematography"), "actors", "disneyMovies", "harryPotterBooks", "harryPotterCharacters", "harryPotterPlaces", "harryPotterSpells", "lordOfTheRingsCharacters", "lordOfTheRingsPlaces", "movies", "starTrekCharacters", "starTrekPlaces", "starTrekRaces", "starTrekTitles", "starWarsCharacters", "starWarsPlaces", "starWarsRaces", "starWarsTitles", "toyStoryCharacters", "transformersCharacters");
        addListToTag(tags.get("Cooking"), "chefs");
        addListToTag(tags.get("Cow"), "beefs", "cows");
        addListToTag(tags.get("Cheese"), "cheeses");
        addListToTag(tags.get("Clothing"), "clothingBrands", "clothingTypes");
        addListToTag(tags.get("Cloud"), "clouds");
        addListToTag(tags.get("Coca Cola"), "cokeProducts");
        addListToTag(tags.get("Coffee"), "coffees");
        addListToTag(tags.get("Color"), "colors");
        addListToTag(tags.get("Comedian"), "comedians");
        addListToTag(tags.get("Computer"), "computerHardware", "computerScientists");
        addListToTag(tags.get("Construction Worker"), "constructionVehicles");
        addListToTag(tags.get("Country Music"), "countryMusicSingers");
        addListToTag(tags.get("Crab"), "crabs");
        addListToTag(tags.get("Crocodile"), "caimans", "crocodiles");
        addListToTag(tags.get("Crustacean"), "crabs", "crustaceans", "lobsters", "shrimps");
        addListToTag(tags.get("Dancing"), "dances");
        addListToTag(tags.get("Diet"), "diets");
        addListToTag(tags.get("Dessert"), "desserts");
        addListToTag(tags.get("Dinosaur"), "dinosaurs");
        addListToTag(tags.get("Disney"), "disneyCharacters", "disneyMovies", "disneyParks");
        addListToTag(tags.get("Doctor"), "doctors");
        addListToTag(tags.get("Dog"), "dogs");
        addListToTag(tags.get("Dolphin"), "dolphins");
        addListToTag(tags.get("Drinking"), "alcoholicDrinks", "alcohols", "beers", "coffees", "cokeProducts", "drinks", "pepsiProducts", "vodkas", "whiskeys", "wines");
        addListToTag(tags.get("Drug"), "drugs");
        addListToTag(tags.get("Drum"), "drums");
        addListToTag(tags.get("Duck"), "ducks");
        addListToTag(tags.get("Earth"), "earthScienceBranches");
        addListToTag(tags.get("Egypt"), "pharaohs", "egyptianGods");
        addListToTag(tags.get("Elephant"), "elephants");
        addListToTag(tags.get("Fish"), "fishes", "sharks");
        addListToTag(tags.get("Flower"), "flowers", "flowerParts");
        addListToTag(tags.get("Fly"), "flies");
        addListToTag(tags.get("Food"), "candies", "cheeses", "chocolates", "chocolates", "desserts", "food", "fruits", "pastas", "restaurants", "vegetables");
        addListToTag(tags.get("Football"), "footballPlayers", "footballPositions", "footballTeams");
        addListToTag(tags.get("Frog"), "frogs");
        addListToTag(tags.get("Fruit"), "fruits");
        addListToTag(tags.get("Furniture"), "furniture");
        addListToTag(tags.get("Gambling"), "casinoGames");
        addListToTag(tags.get("Game"), "boardGames", "cardGames", "games", "videoGames", "videoGameConsoles", "videoGameTypes");
        addListToTag(tags.get("Game of Thrones"), "gameOfThronesCharacters", "gameOfThronesPlaces");
        addListToTag(tags.get("Geometry"), "polyhedron");
        addListToTag(tags.get("Glue"), "glues");
        addListToTag(tags.get("Goat"), "goats");
        addListToTag(tags.get("Golf"), "golfClubs", "golfPlayers", "golfTournaments");
        addListToTag(tags.get("Gorilla"), "gorillas");
        addListToTag(tags.get("Government"), "presidents");
        addListToTag(tags.get("Grass"), "grasses");
        addListToTag(tags.get("Groceries"), "groceryStores");
        addListToTag(tags.get("Guitar"), "guitars");
        addListToTag(tags.get("Gun"), "guns");
        addListToTag(tags.get("Gym"), "gyms", "gymEquipment");
        addListToTag(tags.get("Hardware"), "hardwares", "hardwareTools");
        addListToTag(tags.get("Harry Potter"), "harryPotterBooks", "harryPotterCharacters", "harryPotterPlaces", "harryPotterSpells");
        addListToTag(tags.get("Hawaii"), "hawaiianPlaces");
        addListToTag(tags.get("Heart"), "heartParts");
        addListToTag(tags.get("Heaven"), "heavens");
        addListToTag(tags.get("Hell"), "hells");
        addListToTag(tags.get("Hockey"), "hockeyPlayers", "hockeyPositions", "hockeyTeams");
        addListToTag(tags.get("Holiday"), "holidays");
        addListToTag(tags.get("Homeless"), "homelessPeople");
        addListToTag(tags.get("Horse"), "horses", "horseRaces");
        addListToTag(tags.get("Horse Racing"), "horseRaces");
        addListToTag(tags.get("Kangaroo"), "kangaroos");
        addListToTag(tags.get("King"), "kings");
        addListToTag(tags.get("Koala"), "koalas");
        addListToTag(tags.get("Internet"), "pornsites", "websites");
        addListToTag(tags.get("Lion"), "lions");
        addListToTag(tags.get("Lobster"), "lobsters");
        addListToTag(tags.get("Lord of the Rings"), "lordOfTheRingsCharacters", "lordOfTheRingsPlaces");
        addListToTag(tags.get("Magic"), "magicUsers");
        addListToTag(tags.get("Mail"), "mailCarriers");
        addListToTag(tags.get("Mammal"), "bears", "camels", "cats", "cows", "dogs", "dolphins", "goats", "gorillas", "horses", "kangaroos", "koalas", "lions", "mammals", "mice", "monkeys", "pigs", "rabbits", "rats", "whales");
        addListToTag(tags.get("Makeup"), "makeups");
        addListToTag(tags.get("Martial Arts"), "martialArts");
        addListToTag(tags.get("Math"), "mathematicians", "mathBranches", "mathConcepts");
        addListToTag(tags.get("McDonalds"), "mcDonaldsFood");
        addListToTag(tags.get("Medicine"), "medicines");
        addListToTag(tags.get("Mental Health"), "mentalIllnesses");
        addListToTag(tags.get("Mice"), "mice");
        addListToTag(tags.get("Microsoft"), "microsoftProducts");
        addListToTag(tags.get("Middle East"), "countriesInTheMiddleEast");
        addListToTag(tags.get("Money"), "creditCards", "currencies");
        addListToTag(tags.get("Monkey"), "monkeys");
        addListToTag(tags.get("Monster"), "monsters");
        addListToTag(tags.get("Mushroom"), "mushrooms");
        addListToTag(tags.get("Music"), "countryMusicSingers", "musicalGenres", "musicalInstruments", "musicalNotes", "musicalTempos", "musicians", "rappers");
        addListToTag(tags.get("Ninja"), "ninjaWeapons");
        addListToTag(tags.get("Ocean"), "crustaceans", "fishes", "oceans", "sharks");
        addListToTag(tags.get("Painkiller"), "painkillers");
        addListToTag(tags.get("Panties"), "panties");
        addListToTag(tags.get("Parrot"), "parrots");
        addListToTag(tags.get("Pasta"), "pastas");
        addListToTag(tags.get("Penguin"), "penguins");
        addListToTag(tags.get("Pepsi"), "pepsiProducts");
        addListToTag(tags.get("Pharmacy"), "drugstores");
        addListToTag(tags.get("Philosophy"), "philosophers");
        addListToTag(tags.get("Photography"), "cameras");
        addListToTag(tags.get("Physics"), "physicists", "physicsBranches", "physicsConcepts");
        addListToTag(tags.get("Piano"), "pianos");
        addListToTag(tags.get("Pig"), "pigs", "porks");
        addListToTag(tags.get("Pizza"), "pizzas", "pizzaRestaurants");
        addListToTag(tags.get("Planet"), "planets");
        addListToTag(tags.get("Plant"), "fruits", "plants", "plantParts", "trees", "treeParts", "vegetables");
        addListToTag(tags.get("Playing Cards"), "cardGames", "pokerHands");
        addListToTag(tags.get("Poetry"), "poems", "poets");
        addListToTag(tags.get("Poison"), "poisons");
        addListToTag(tags.get("Pokemon"), "pokemon", "pokemonCities");
        addListToTag(tags.get("Poker"), "pokerHands");
        addListToTag(tags.get("Politics"), "politicians", "presidents");
        addListToTag(tags.get("Porn"), "pornsites");
        addListToTag(tags.get("Pork"), "porks");
        addListToTag(tags.get("President"), "presidents");
        addListToTag(tags.get("Programming"), "programmingLanguages");
        addListToTag(tags.get("Protestant"), "baptists", "protestants");
        addListToTag(tags.get("Psychic"), "psychics", "psychicAbilities");
        addListToTag(tags.get("Psychology"), "psychologists");
        addListToTag(tags.get("Puzzle"), "puzzles");
        addListToTag(tags.get("Queen"), "queens");
        addListToTag(tags.get("Rabbit"), "rabbits");
        addListToTag(tags.get("Racing"), "carRaces", "horseRaces");
        addListToTag(tags.get("Racist"), "racialSlurs");
        addListToTag(tags.get("Rap"), "rappers");
        addListToTag(tags.get("Rat"), "rats");
        addListToTag(tags.get("Religion"), "religions");
        addListToTag(tags.get("Renewable Energy"), "renewableEnergies");
        addListToTag(tags.get("Reptile"), "alligators", "caimans", "crocodiles", "dinosaurs", "reptiles", "snakes", "turtles");
        addListToTag(tags.get("Restaurant"), "pizzaRestaurants", "restaurants", "restaurantTypes");
        addListToTag(tags.get("River"), "rivers");
        addListToTag(tags.get("Science"), "astronomers", "astrophysicists", "biologists", "chemists", "chemistryTools", "computerScientists", "mathematicians", "physicists", "scientists", "scienceBranches", "scienceConcepts", "scienceInstruments");
        addListToTag(tags.get("Sex"), "sexCategories", "stds");
        addListToTag(tags.get("Sex Toy"), "sexToys");
        addListToTag(tags.get("Shark"), "sharks");
        addListToTag(tags.get("Sheep"), "sheeps");
        addListToTag(tags.get("Shoe"), "shoeBrands", "shoeTypes");
        addListToTag(tags.get("Shrimp"), "shrimps");
        addListToTag(tags.get("Sick"), "sicknesses");
        addListToTag(tags.get("Skateboarding"), "skateboardTricks");
        addListToTag(tags.get("Skirt"), "skirts");
        addListToTag(tags.get("Skunk"), "skunks");
        addListToTag(tags.get("Snail"), "snails");
        addListToTag(tags.get("Snake"), "snakes");
        addListToTag(tags.get("Soccer"), "soccerPlayers", "soccerPositions", "soccerTeams");
        addListToTag(tags.get("Sock"), "socks");
        addListToTag(tags.get("Soda"), "cokeProducts", "pepsiProducts");
        addListToTag(tags.get("Software"), "software", "softwareTypes");
        addListToTag(tags.get("Space"), "astronauts", "astronomers", "astrophysicists", "starTypes");
        addListToTag(tags.get("Spider"), "spiders");
        addListToTag(tags.get("Sport"), "baseballPlayers", "baseballPositions", "baseballTeams", "basketballPlayers", "basketballPositions", "basketballTeams", "boxers", "footballPlayers", "footballPositions", "footballTeams", "golfClubs", "golfPlayers", "golfTournaments", "hockeyPlayers", "hockeyPositions", "hockeyTeams", "soccerPlayers", "soccerPositions", "soccerTeams", "sports", "swimmers", "swimmingStrokes", "tennisPlayers", "volleyballPlayers", "volleyballPositions", "wrestlers", "wrestlingMoves");
        addListToTag(tags.get("Star Trek"), "starTrekCharacters", "starTrekPlaces", "starTrekRaces", "starTrekTitles");
        addListToTag(tags.get("Star Wars"), "starWarsCharacters", "starWarsPlaces", "starWarsRaces", "starWarsTitles");
        addListToTag(tags.get("Superhero"), "superheroes", "superpowers");
        addListToTag(tags.get("Supermarket"), "supermarkets");
        addListToTag(tags.get("Surgery"), "surgeries");
        addListToTag(tags.get("Swimming"), "swimmers", "swimmingStrokes");
        addListToTag(tags.get("Television"), "actors", "gameOfThronesCharacters", "gameOfThronesPlaces", "harryPotterBooks", "harryPotterCharacters", "harryPotterPlaces", "harryPotterSpells", "lordOfTheRingsCharacters", "lordOfTheRingsPlaces", "starTrekCharacters", "starTrekPlaces", "starTrekRaces", "starTrekTitles", "starWarsCharacters", "starWarsPlaces", "starWarsRaces", "starWarsTitles", "televisionCharacters", "televisionShows", "televisionShowTypes", "theSimpsonsCharacters", "toyStoryCharacters", "transformersCharacters", "movies", "disneyMovies");
        addListToTag(tags.get("Tennis"), "tennisPlayers");
        addListToTag(tags.get("The Beatles"), "theBeatles");
        addListToTag(tags.get("The Simpsons"), "theSimpsonsCharacters");
        addListToTag(tags.get("Toad"), "toads");
        addListToTag(tags.get("Toy Story"), "toyStoryCharacters");
        addListToTag(tags.get("Transformers"), "transformersCharacters");
        addListToTag(tags.get("Tree"), "trees", "treeParts");
        addListToTag(tags.get("Turkey"), "turkeys");
        addListToTag(tags.get("Turtle"), "turtles");
        addListToTag(tags.get("United Kingdom"), "countriesInTheUnitedKingdom");
        addListToTag(tags.get("United States"), "unitedStates");
        addListToTag(tags.get("Vacuum"), "vacuums");
        addListToTag(tags.get("Vegetable"), "vegetables");
        addListToTag(tags.get("Video Game"), "videoGames", "videoGameConsoles", "videoGameTypes");
        addListToTag(tags.get("Viking"), "norseGods", "norseRealms");
        addListToTag(tags.get("Virus"), "viruses");
        addListToTag(tags.get("Vodka"), "vodkas");
        addListToTag(tags.get("Volleyball"), "volleyballPlayers", "volleyballPositions");
        addListToTag(tags.get("Water"), "oceans", "waterBodies");
        addListToTag(tags.get("Weather"), "weatherTypes");
        addListToTag(tags.get("Whale"), "whales");
        addListToTag(tags.get("Whiskey"), "whiskeys");
        addListToTag(tags.get("Wine"), "wines");
        addListToTag(tags.get("Wrestling"), "wrestlers", "wrestlingMoves");
        addListToTag(tags.get("Writing"), "authors", "books", "poems", "poets");
        
        boolean updated = true;
        while (updated) {
            updated = false;
            for (Tag tag : tags.values()) {
                List<String> toAdd = new ArrayList<>();
                for (String alias : tag.aliases) {
                    if (tags.containsKey(alias)) {
                        for (String aliasAlias : tags.get(alias).aliases) {
                            if (!tag.aliases.contains(aliasAlias)) {
                                toAdd.add(aliasAlias);
                                updated = true;
                            }
                        }
                    }
                }
                tag.aliases.addAll(toAdd);
            }
        }
        
        for (Tag tag : tags.values()) {
            tag.aliases = ListUtility.removeDuplicates(tag.aliases);
            tag.aliases.sort(Comparator.naturalOrder());
        }
        
        if (removeObsoleteAliases) {
            for (Tag tag : tags.values()) {
                List<String> toRemove = new ArrayList<>();
                List<String> toCheck = new ArrayList<>(tag.aliases);
                toCheck.add(tag.name);
                for (String aliasCheck : toCheck) {
                    for (String alias : tag.aliases) {
                        if (alias.toUpperCase().equals(aliasCheck.toUpperCase() + "S") ||
                                ((aliasCheck.length() > 4) && alias.toUpperCase().equals(aliasCheck.toUpperCase() + "ES")) ||
                                aliasCheck.toUpperCase().endsWith("Y") && alias.toUpperCase().equals(StringUtility.rShear(aliasCheck.toUpperCase(), 1) + "IES")) {
//                            System.out.println(tag.name + " = " + aliasCheck + " : " + alias);
                            toRemove.add(alias);
                            break;
                        }
                    }
                }
                tag.aliases.removeAll(toRemove);
            }
        }
        
        return tags;
    }
    
    /**
     * Adds a list of tag alias lists to a specified tag.
     *
     * @param tag   The tag to add the alias lists to.
     * @param lists The list of tag alias lists to add to the tag.
     */
    private void addListToTag(Tag tag, String... lists) {
        if (!loadTagLists) {
            return;
        }
        
        for (String list : lists) {
            tag.aliases.addAll(Filesystem.readLines(new File("etc/lists/" + list + ".txt")));
        }
    }
    
    
    //Methods
    
    /**
     * Determines a list of associated tags for a string.
     *
     * @param text        The string.
     * @param currentTags The current tag list.
     * @return The list of tags associated with the given string.
     */
    public List<String> getTagsFromText(String text, List<String> currentTags) {
        List<String> tags = new ArrayList<>(currentTags);
        HashSet<String> matches = new HashSet<>();
        HashSet<String> nonMatches = new HashSet<>();
        
        getInitialTags(text).forEach(e -> {
            tags.add(e);
            matches.add(e.toUpperCase());
        });
        
        for (Tag tag : tagList.values()) {
            if (tags.contains(tag.name)) {
                matches.add(tag.name.toUpperCase());
                continue;
            }
            if (hasTag(text, tag, matches, nonMatches)) {
                tags.add(tag.name);
            }
        }
        
        return fixTagList(text, tags, matches, nonMatches);
    }
    
    /**
     * Determines a list of associated tags for a string.
     *
     * @param text The string.
     * @return The list of tags associated with the given string.
     */
    public List<String> getTagsFromText(String text) {
        return getTagsFromText(text, new ArrayList<>());
    }
    
    /**
     * Determines the initial list of associated tags for a string.
     *
     * @param text The string.
     * @return The initial list of tags associated with the given string.
     */
    public List<String> getInitialTags(String text) {
        List<String> tags = new ArrayList<>();
        
        List<String> words = new ArrayList<>(Arrays.asList(StringUtility.removePunctuationSoft(text, Arrays.asList('&', '-')).split("\\s+", -1)));
        List<String> combinedWords = new ArrayList<>();
        for (String combinedWord : words) {
            combinedWords.add(combinedWord.replaceAll("[&\\-]", ""));
        }
        words.addAll(combinedWords);
        words.addAll(Arrays.asList(StringUtility.removePunctuationSoft(text, Arrays.asList('&', '-')).split("[\\s&\\-]+", -1)));
        words = ListUtility.removeDuplicates(words);
        for (String word : words) {
            word = word.replaceAll("^[&\\-]+", "").replaceAll("[&\\-]+$", "");
            
            if ((word.length() > 1) && (word.charAt(0) == 'i') && Character.isUpperCase(word.charAt(1))) {
                if (!tags.contains("Apple")) {
                    tags.add("Apple");
                    if (printTagTrigger) {
                        System.out.println("Apple" + " -> " + "i*");
                    }
                }
            }
            if (word.equalsIgnoreCase("gorilla") && text.toLowerCase().matches("^.+\\shar.+$")) {
                if (!tags.contains("Harambe")) {
                    tags.add("Harambe");
                    if (printTagTrigger) {
                        System.out.println("Harambe" + " -> " + "Gorilla & Har*");
                    }
                }
            }
            word = word.toUpperCase();
            if (word.endsWith("SAURUS") && !word.equals("THESAURUS")) {
                if (!tags.contains("Dinosaur")) {
                    tags.add("Dinosaur");
                    if (printTagTrigger) {
                        System.out.println("Dinosaur" + " -> " + "*saurus");
                    }
                }
            }
            if (word.startsWith("MOOO")) {
                if (!tags.contains("Cow")) {
                    tags.add("Cow");
                    if (printTagTrigger) {
                        System.out.println("Cow" + " -> " + "Mooo*");
                    }
                }
            }
            if (word.startsWith("MOON")) {
                if (!tags.contains("Moon")) {
                    tags.add("Moon");
                    if (printTagTrigger) {
                        System.out.println("Moon" + " -> " + "Moon*");
                    }
                }
            }
            if (word.startsWith("SNOW")) {
                if (!tags.contains("Snow")) {
                    tags.add("Snow");
                    if (printTagTrigger) {
                        System.out.println("Snow" + " -> " + "Snow*");
                    }
                }
            }
            if (word.endsWith("WATER") || word.startsWith("RAIN")) {
                if (!tags.contains("Water")) {
                    tags.add("Water");
                    if (printTagTrigger) {
                        System.out.println("Water" + " -> " + "Water*");
                    }
                }
            }
            if (word.startsWith("RAIN")) {
                if (!tags.contains("Weather")) {
                    tags.add("Weather");
                    if (printTagTrigger) {
                        System.out.println("Weather" + " -> " + "Rain*");
                    }
                }
            }
            if (word.startsWith("PURR") || word.startsWith("MEOW") || word.startsWith("NYAN")) {
                if (!tags.contains("Cat")) {
                    tags.add("Cat");
                    if (printTagTrigger) {
                        System.out.println("Cow" + " -> " + "Purr* | Meow* | Nyan*");
                    }
                }
            }
            if (word.startsWith("MEIN") || word.startsWith("KAMPF") || word.startsWith("LUFT")) {
                if (!tags.contains("Germany")) {
                    tags.add("Germany");
                    if (printTagTrigger) {
                        System.out.println("Germany" + " -> " + "Mein* | Kampf* | Luft*");
                    }
                }
            }
            if ((word.endsWith("SPORT") || word.endsWith("SPORTS"))) {
                if (!tags.contains("Sport")) {
                    List<String> notSport = Arrays.asList("Disport", "Gosport", "Passport", "Transport", "Spoilsport", "Cotransport");
                    boolean isNotSport = false;
                    for (String notSportEntry : notSport) {
                        if (word.equalsIgnoreCase(notSportEntry) || word.equalsIgnoreCase(notSportEntry + "s")) {
                            isNotSport = true;
                            break;
                        }
                    }
                    if (!isNotSport) {
                        tags.add("Sport");
                        if (printTagTrigger) {
                            System.out.println("Sport" + " -> " + "*sport | *sports");
                        }
                    }
                }
            }
            if (word.endsWith("SEXUAL") && !word.equals("SEXUAL")) {
                if (!tags.contains("Gender")) {
                    tags.add("Gender");
                    if (printTagTrigger) {
                        System.out.println("Gender" + " -> " + "*sexual");
                    }
                }
            }
            if (word.contains("SHREK")) {
                if (!tags.contains("Shrek")) {
                    tags.add("Shrek");
                    if (printTagTrigger) {
                        System.out.println("Shrek" + " -> " + "Shrek*");
                    }
                }
            }
            if (word.contains("NAZI")) {
                if (!tags.contains("Nazi")) {
                    tags.add("Nazi");
                    if (printTagTrigger) {
                        System.out.println("Nazi" + " -> " + "*nazi*");
                    }
                }
            }
        }
        
        return tags;
    }
    
    /**
     * Determines if a tag matches a string.
     *
     * @param text       The string.
     * @param tag        The tag.
     * @param matches    The list of known matches.
     * @param nonMatches The list of known non-matches.
     * @return Whether or not the tag matches the given string.
     */
    private boolean hasTag(String text, Tag tag, HashSet<String> matches, HashSet<String> nonMatches) {
        String textTest = text.toUpperCase().replace("-", "");
        String textTestCombined = text.toUpperCase();
        
        if (!tag.minor) {
            if (matches.contains(tag.name.toUpperCase())) {
                if (printTagTrigger) {
                    System.out.println(tag.name + " -> ~" + tag.name);
                }
                return true;
            }
            
            for (String ending : tagEndingToDontDoList.keySet()) {
                if (tag.name.toUpperCase().endsWith(ending) && !tagEndingToDontDoList.get(ending).contains(tag.name)) {
                    for (String append : tagEndingToReplacements.get(ending)) {
                        if (tagEndingToDontDoList.containsKey(append) && tagEndingToDontDoList.get(append).contains(tag.name)) {
                            continue;
                        }
                        
                        String tagTest = StringUtility.rShear(tag.name.toUpperCase(), ending.length()) + append;
                        if (textTest.matches(tagRegex.replace("X", tagTest)) ||
                                textTestCombined.matches(tagRegexCombined.replace("X", tagTest))) {
                            if (printTagTrigger) {
                                System.out.println(tag.name + " -> " + tag.name + (append.isEmpty() ? "" : (" (" + tagTest + ")")));
                            }
                            matches.add(tag.name.toUpperCase());
                            return true;
                        }
                    }
                }
            }
        }
        
        final List<String> aliasAppends = Arrays.asList("", "S", "ES", "IES");
        for (String alias : tag.aliases) {
            String aliasEntry = StringUtility.removePunctuationSoft(alias, Arrays.asList('&', '-')).toUpperCase();
            String aliasEntryTest = aliasEntry.replace("-", "\\\\-").toUpperCase();
            if (aliasEntry.isEmpty() || nonMatches.contains(aliasEntry)) {
                continue;
            }
            
            if (matches.contains(aliasEntry)) {
                if (printTagTrigger) {
                    System.out.println(tag.name + " -> ~+" + alias);
                }
                return true;
            }
            
            for (String append : aliasAppends) {
                if ((append.equals("ES") && aliasEntry.length() <= 4) ||
                        (append.equals("IES") && !aliasEntry.endsWith("Y"))) {
                    continue;
                }
                
                String aliasTest = StringUtility.rShear(aliasEntryTest, (append.equals("IES") ? 1 : 0)) + append;
                if (textTest.matches(tagRegex.replace("X", aliasTest)) ||
                        textTestCombined.matches(tagRegex.replace("X", aliasTest)) ||
                        textTestCombined.matches(tagRegexCombined.replace("X", aliasTest))) {
                    if (printTagTrigger) {
                        System.out.println(tag.name + " -> +" + alias + (append.isEmpty() ? "" : (" (" + aliasTest + ")")));
                    }
                    matches.add(aliasEntry);
                    return true;
                }
            }
            
            nonMatches.add(aliasEntry);
        }
        
        nonMatches.add(tag.name.toUpperCase());
        return false;
    }
    
    /**
     * Determines if a tag matches a string.
     *
     * @param text The string.
     * @param tag  The tag.
     * @return Whether or not the tag matches the given string.
     */
    public boolean hasTag(String text, Tag tag) {
        return hasTag(text, tag, new HashSet<>(), new HashSet<>());
    }
    
    /**
     * Performs additional work on a tag list.
     *
     * @param text       The string.
     * @param tags       The tag list.
     * @param matches    The list of known matches.
     * @param nonMatches The list of known non-matches.
     * @return The fixed tag list.
     */
    private List<String> fixTagList(String text, List<String> tags, HashSet<String> matches, HashSet<String> nonMatches) {
        tags = ListUtility.removeDuplicates(tags);
        
        if (tags.contains("Aviation") && tags.contains("Fly")) {
            tags.remove("Fly");
            if (printTagTrigger) {
                System.out.println("-Fly -> Aviation");
            }
            
            if (tags.contains("Bug")) {
                Tag bugNoFly = specialTagList.get("Bug~Fly");
                if (bugNoFly == null) {
                    bugNoFly = new Tag(tagList.get("Bug"));
                    bugNoFly.name += "~";
                    bugNoFly.aliases.remove("Fly");
                    bugNoFly.aliases.removeAll(tagList.get("Fly").aliases);
                    specialTagList.put("Bug~Fly", bugNoFly);
                }
                if (!hasTag(text, bugNoFly, matches, nonMatches)) {
                    tags.remove("Bug");
                    if (printTagTrigger) {
                        System.out.println("-Bug -> -Fly -> Aviation");
                    }
                }
            }
            
            if (tags.contains("Animal")) {
                Tag animalNoFly = specialTagList.get("Animal~Fly");
                if (animalNoFly == null) {
                    animalNoFly = new Tag(tagList.get("Animal"));
                    animalNoFly.name += "~";
                    animalNoFly.aliases.remove("Fly");
                    animalNoFly.aliases.removeAll(tagList.get("Fly").aliases);
                    specialTagList.put("Animal~Fly", animalNoFly);
                }
                if (!hasTag(text, animalNoFly, matches, nonMatches)) {
                    tags.remove("Animal");
                    if (printTagTrigger) {
                        System.out.println("-Animal -> -Fly -> Aviation");
                    }
                }
            }
        }
        
        if (tags.contains("Lightbulb") && tags.contains("Sex")) {
            Tag sexNoScrew = specialTagList.get("Sex~Screw");
            if (sexNoScrew == null) {
                sexNoScrew = new Tag(tagList.get("Sex"));
                sexNoScrew.name += "~";
                sexNoScrew.aliases.remove("Screw");
                sexNoScrew.aliases.remove("Screwed");
                sexNoScrew.aliases.remove("Screwing");
                specialTagList.put("Sex~Screw", sexNoScrew);
            }
            if (!hasTag(text, sexNoScrew, matches, nonMatches)) {
                tags.remove("Sex");
                if (printTagTrigger) {
                    System.out.println("-Sex -> Lightbulb");
                }
            }
        }
        
        return tags;
    }
    
}
