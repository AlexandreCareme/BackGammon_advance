package gameproject.game.logic;

/**
 * Represents the different game mode available in the application.
 * Each game mode determines which player are created when a game starts
 */
public enum GameMode {

    /**
     * A game between two human players
     */
    FRIEND,

    /**
     * A game between a human and AI player
     */
    VS_AI,

    /**
     * A Fog of War game between a human and AI player
     */
    VS_AI_FOG,

    /**
     * A demonstration between two AI players
     */
    DEMO
}
