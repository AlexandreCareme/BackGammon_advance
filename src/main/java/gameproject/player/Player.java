package gameproject.player;

/**
 * Class which models a player, It has a name a color as which it plays and a certain strategy for AI-agents how to play the game
 * This Class is the superclass of all human and AI players and determine basic characteristics of each.
 *
 * @author Sven
 * @author Kilian
 * @author Alexandre
 * @author Diederick
 */
public class Player {

    /**
     * Class which represents the two colors players can have (White and Black)
     * As well a method which gives the opponent's color.
     */
    public enum Color {
        WHITE,
        BLACK;

        /**
         * Method which gives the color of the opponent
         *
         * @return color of the opponent
         */
        public Color opponent(){

            return switch (this) {
                case WHITE -> BLACK;
                case BLACK -> WHITE;
            };
        }
    }

    /**
     * A player has a certain name
     */
    private String name;
    /**
     * A player has one of the two colors
     */
    private Color color;
    /**
     * A player might use a certain strategy to move (AI)
     */
    private Strategy strategy;


    /**
     * Creates a new player
     *
     * @param name the name of the player
     * @param color the color of the player (white or black)
     * @param strategy for choosing between different possible moves at each turn
     */
    public Player(String name, Color color, Strategy strategy){
        this.name = name;
        this.color = color;
        this.strategy = strategy;
    }

    /**
     * Gives the name of the player
     *
     * @return the name of the player
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gives the color of the player
     *
     * @return the color of the player
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gives the strategy of the player
     *
     * @return the strategy of the player
     */
    public Strategy getStrategy() {
        return strategy;
    }


    /**
     * Checks if this player is controlled by AI
     *
     * @return true if the player uses an AI strategy
     */
    public boolean isAI(){
        return strategy.isAI();
    }
}

