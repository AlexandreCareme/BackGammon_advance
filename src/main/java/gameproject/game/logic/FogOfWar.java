package gameproject.game.logic;

import gameproject.game.elements.Point;
import gameproject.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the visibility of opponent checkers during
 * a fog of war game
 */
public class FogOfWar {
    private List<Integer> revealPoints = new ArrayList<>();

    /**
     * Reveals an opponent checker at a board point
     * @param point the point to reveal
     */
    public void revealPoint(int point){
        if(!revealPoints.contains(point)){
            revealPoints.add(point);
        }
    }

    /**
     * Checks whether a point is currently revealed
     * @param point point to check
     * @return true if the point is revealed
     */
    public boolean isRevealed(int point){
        return revealPoints.contains(point);
    }

    /**
     * Checks whether an opponent checker should be revealed at the given point
     * @param point point being checked
     * @param player player who is looking
     * @return true if the opponent has excactly one checker there
     */
    public boolean shouldReveal(Point point, Player.Color player){
        Player.Color opponent = player.opponent();
        return point.getCheckers(opponent) == 1;
    }

    /**
     * Reveaks an opponent checker if the point should be revealed
     * @param point point being checked
     * @param player player who is looking
     * @param pointNumber board point number
     */
    public void revealIfNeeded(Point point, Player.Color player, int pointNumber){
        if(shouldReveal(point, player)){
            revealPoint(pointNumber);
        }
    }

    /**
     * Retruns the number of opponent checkers that should be visible
     * @param pointNumber board point number
     * @return number of opponent checkers that should be visible
     */
    public int getVisibleOpponentCheckers( int pointNumber){
        if(isRevealed(pointNumber)){
            return 1;
        }
        return 0;
    }

    /**
     * Removes all temporary revelations
     */
    public void clearRevealPoints(){
        revealPoints.clear();
    }
}
