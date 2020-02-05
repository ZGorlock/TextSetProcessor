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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
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
        
        tagEndingToReplacements.put("", Arrays.asList("", "S", "ES", "Y", "ER", "ERS", "ED", "ING", "I", "IC", "MAN"));
        tagEndingToReplacements.put("s", Arrays.asList("", "ING", "ED"));
        tagEndingToReplacements.put("ed", Collections.singletonList(""));
        tagEndingToReplacements.put("es", Arrays.asList("", "ING", "ED"));
        tagEndingToReplacements.put("ies", Arrays.asList("Y", "C"));
        tagEndingToReplacements.put("y", Arrays.asList("", "ER", "ERS", "IES", "IST", "IC", "ICAL", "ICALLY"));
        tagEndingToReplacements.put("ing", Arrays.asList("", "E", "ER", "ERS", "ED", "ES", "S"));
        tagEndingToReplacements.put("tion", Arrays.asList("T", "TE", "TEE", "TED", "TOR", "TING"));
        tagEndingToReplacements.put("tation", Arrays.asList("T", "TE", "TEE", "TED", "TOR", "TING"));
        tagEndingToReplacements.put("er", Arrays.asList("", "E", "OR", "ORS", "ATION", "ING"));
        tagEndingToReplacements.put("or", Arrays.asList("", "E", "ER", "ERS", "ATION", "ING"));
        
        tagList.putAll(parseTags());
        
        tagEndingToDontDoList.put("", Collections.emptyList());
        tagEndingToDontDoList.put("s", tagList.values().stream().filter(tag -> tag.dontDoS).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ed", tagList.values().stream().filter(tag -> tag.dontDoED).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("es", tagList.values().stream().filter(tag -> tag.dontDoES).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ies", tagList.values().stream().filter(tag -> tag.dontDoIES).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("y", tagList.values().stream().filter(tag -> tag.dontDoY).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("ing", tagList.values().stream().filter(tag -> tag.dontDoING).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("tion", tagList.values().stream().filter(tag -> tag.dontDoTION).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("tation", tagList.values().stream().filter(tag -> tag.dontDoTION).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("er", tagList.values().stream().filter(tag -> tag.dontDoER).map(tag -> tag.name).collect(Collectors.toList()));
        tagEndingToDontDoList.put("or", tagList.values().stream().filter(tag -> tag.dontDoOR).map(tag -> tag.name).collect(Collectors.toList()));
        
        int totalKeywords = 0;
        for (Tag tag : tagList.values()) {
            List<String> appendings = new ArrayList<>();
            for (String ending : tagEndingToDontDoList.keySet()) {
                if (tag.name.endsWith(ending) && !tagEndingToDontDoList.get(ending).contains(tag.name)) {
                    appendings.addAll(tagEndingToReplacements.get(ending));
                }
            }
            appendings = ListUtility.removeDuplicates(appendings);
            if (tagEndingToDontDoList.get("ing").contains(tag.name)) {
                appendings.remove("ING");
            }
            int tagKeywords = appendings.size();
            for (String alias : tag.aliases) {
                tagKeywords += (3 + (alias.toUpperCase().endsWith("Y") ? 1 : 0));
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
        
        for (Tag tag : tags.values()) {
            List<String> toRemove = new ArrayList<>();
            List<String> toCheck = new ArrayList<>(tag.aliases);
            toCheck.add(tag.name);
            for (String alias : tag.aliases) {
                for (String aliasCheck : toCheck) {
                    if (alias.matches("^" + aliasCheck + "(s|es)?\\s.*$") ||
                            alias.matches("^.*\\s" + aliasCheck + "(s|es)?\\s.*$") ||
                            alias.matches("^.*\\s" + aliasCheck + "(s|es)?$")) {
                        toRemove.add(alias);
                    }
                }
            }
            tag.aliases.removeAll(toRemove);
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
        if (loadTagLists) {
            for (String list : lists) {
                tag.aliases.addAll(Filesystem.readLines(new File("etc/lists/" + list + ".txt")));
            }
        }
    }
    
    
    //Methods
    
    /**
     * Determines a list of associated tags for a string.
     *
     * @param text The string.
     * @return The list of tags associated with the given string.
     */
    public List<String> getTagsFromText(String text) {
        List<String> tags = new ArrayList<>();
        List<String> matches = new ArrayList<>();
        List<String> nonMatches = new ArrayList<>();
        String textTest = text.toUpperCase();
        
        String wordDiv = "[^A-Z0-9]";
        String wordDivCombined = "[^A-Z0-9&\\-]";
        String tagRegex = "^" +
                "(" + wordDiv + "*X" + wordDiv + ".*)|" +
                "(.*" + wordDiv + "X" + wordDiv + "*)|" +
                "(.*" + wordDiv + "X" + wordDiv + ".*)|" +
                "(" + wordDiv + "*X" + wordDiv + "*)" +
                "$";
        String tagRegexCombined = "^" + 
                "(" + wordDivCombined + "*X" + wordDivCombined + ".*)|" + 
                "(.*" + wordDivCombined + "X" + wordDivCombined + "*)|" + 
                "(.*" + wordDivCombined + "X" + wordDivCombined + ".*)|" + 
                "(" + wordDivCombined + "*X" + wordDivCombined + "*)" + 
                "$";
        
        List<String> words = new ArrayList<>();
        words.addAll(Arrays.asList(StringUtility.removePunctuationSoft(text, Arrays.asList('&', '-')).split("\\s+", -1)));
        words.addAll(Arrays.asList(StringUtility.removePunctuationSoft(text, Arrays.asList('&', '-')).split("[\\s&\\-]+", -1)));
        for (String word : words) {
            word = word.replaceAll("^[&\\-]+", "");
            word = word.replaceAll("[&\\-]+$", "");
            
            if ((word.length() > 1) && (word.charAt(0) == 'i') && Character.isUpperCase(word.charAt(1))) {
                if (!tags.contains("Apple")) {
                    tags.add("Apple");
                    matches.add("Apple");
                    if (printTagTrigger) {
                        System.out.println("Apple" + " -> " + "i*");
                    }
                }
            }
            if (word.equalsIgnoreCase("gorilla") && text.toLowerCase().matches("^.+\\shar.+$")) {
                if (!tags.contains("Harambe")) {
                    tags.add("Harambe");
                    matches.add("Harambe");
                    if (printTagTrigger) {
                        System.out.println("Harambe" + " -> " + "Gorilla & Har*");
                    }
                }
            }
            word = word.toUpperCase();
            if (word.endsWith("SAURUS")) {
                if (!tags.contains("Dinosaur")) {
                    tags.add("Dinosaur");
                    matches.add("Dinosaur");
                    if (printTagTrigger) {
                        System.out.println("Dinosaur" + " -> " + "*saurus");
                    }
                }
            }
            if (word.startsWith("MOO")) {
                if (!tags.contains("Cow")) {
                    tags.add("Cow");
                    matches.add("Cow");
                    if (printTagTrigger) {
                        System.out.println("Cow" + " -> " + "Moo*");
                    }
                }
            }
            if (word.startsWith("PURR") || word.startsWith("MEOW") || word.startsWith("NYAN")) {
                if (!tags.contains("Cat")) {
                    tags.add("Cat");
                    matches.add("Cat");
                    if (printTagTrigger) {
                        System.out.println("Cow" + " -> " + "Purr* | Meow* | Nyan*");
                    }
                }
            }
            if (word.startsWith("MEIN") || word.startsWith("KAMPF") || word.startsWith("LUFT")) {
                if (!tags.contains("Germany")) {
                    tags.add("Germany");
                    matches.add("Germany");
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
                        matches.add("Sport");
                        if (printTagTrigger) {
                            System.out.println("Sport" + " -> " + "*sport | *sports");
                        }
                    }
                }
            }
            if (word.endsWith("SEXUAL") && !word.equals("SEXUAL")) {
                if (!tags.contains("Gender")) {
                    tags.add("Gender");
                    matches.add("Gender");
                    if (printTagTrigger) {
                        System.out.println("Gender" + " -> " + "*sexual");
                    }
                }
            }
            if (word.contains("SHREK")) {
                if (!tags.contains("Shrek")) {
                    tags.add("Shrek");
                    matches.add("Shrek");
                    if (printTagTrigger) {
                        System.out.println("Shrek" + " -> " + "Shrek*");
                    }
                }
            }
            if (word.contains("NAZI")) {
                if (!tags.contains("Nazi")) {
                    tags.add("Nazi");
                    matches.add("Nazi");
                    if (printTagTrigger) {
                        System.out.println("Nazi" + " -> " + "*nazi*");
                    }
                }
            }
        }
        
        for (Tag tag : tagList.values()) {
            String tagEntry = StringUtility.trim(StringUtility.removePunctuationSoft(tag.name, Arrays.asList('&', '-')));
            if (tags.contains(tagEntry) || tag.minor) {
                continue;
            }
            
            if (matches.contains(tagEntry)) {
                tags.add(tagEntry);
                if (printTagTrigger) {
                    System.out.println(tag.name + " -> " + tag.name);
                }
                continue;
            }
            
            for (String ending : tagEndingToDontDoList.keySet()) {
                
                if (tagEntry.endsWith(ending) && !tagEndingToDontDoList.get(ending).contains(tagEntry)) {
                    for (String tagAppend : tagEndingToReplacements.get(ending)) {
                        if (tagEndingToDontDoList.containsKey(tagAppend.toLowerCase()) && tagEndingToDontDoList.get(tagAppend.toLowerCase()).contains(tagEntry)) {
                            continue;
                        }
                        
                        String tagTest = StringUtility.rShear(tagEntry.toUpperCase(), ending.length()) + tagAppend;
                        if (textTest.matches(tagRegex.replace("X", tagTest)) || 
                                textTest.matches(tagRegexCombined.replace("X", tagTest))) {
                            tags.add(tagEntry);
                            if (printTagTrigger) {
                                System.out.println(tag.name + " -> " + tagTest);
                            }
                            matches.add(tagEntry);
                            break;
                        }
                    }
                }
                
                if (tags.contains(tagEntry)) {
                    break;
                }
            }
            if (tags.contains(tagEntry)) {
                continue;
            }
            
            for (String alias : tag.aliases) {
                String aliasEntry = StringUtility.trim(StringUtility.removePunctuationSoft(alias, Arrays.asList('&', '-')));
                if (aliasEntry.isEmpty()) {
                    continue;
                }
                if (nonMatches.contains(aliasEntry)) {
                    continue;
                }
                if (matches.contains(aliasEntry)) {
                    tags.add(tagEntry);
                    if (printTagTrigger) {
                        System.out.println(tag.name + " -> " + aliasEntry);
                    }
                    break;
                }
                
                for (String append : Arrays.asList("", "S", "ES")) {
                    if (textTest.matches(tagRegex.replace("X", aliasEntry.toUpperCase() + append)) ||
                            textTest.matches(tagRegexCombined.replace("X", aliasEntry.toUpperCase() + append))) {
                        tags.add(tagEntry);
                        if (printTagTrigger) {
                            System.out.println(tag.name + " -> " + aliasEntry.toUpperCase() + append.toLowerCase());
                        }
                        matches.add(aliasEntry);
                        break;
                    }
                }
                if (tags.contains(tagEntry)) {
                    break;
                }
                if (aliasEntry.toUpperCase().endsWith("Y")) {
                    if (textTest.matches(tagRegex.replace("X", StringUtility.rShear(aliasEntry.toUpperCase(), 1) + "IES")) ||
                            textTest.matches(tagRegexCombined.replace("X", StringUtility.rShear(aliasEntry.toUpperCase(), 1) + "IES"))) {
                        tags.add(tagEntry);
                        if (printTagTrigger) {
                            System.out.println(tag.name + " -> " + StringUtility.rShear(aliasEntry.toUpperCase(), 1) + "ies");
                        }
                        matches.add(aliasEntry);
                        break;
                    }
                }
                nonMatches.add(aliasEntry);
            }
            if (!tags.contains(tagEntry)) {
                nonMatches.add(tagEntry);
            }
        }
        
        return ListUtility.removeDuplicates(tags);
    }
    
}
