package gameproject.ai;

import gameproject.game.elements.Play;
import gameproject.game.elements.Point;
import gameproject.game.elements.Round;
import gameproject.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the information available to an AI player
 * during a Fog of War game
 */
public class FogAIInformation {

    private List<Integer> revealPoints = new ArrayList<>();

    /**
     * Returns the opponent points that the AI is allowed to see
     * The AI can see an opponent checker when that point is a legal destination for one of its current moves
     * @param round current game round
     * @param legalPlays legal plays for the AI
     * @param aiColor colour of the AI
     * @return list of visible opponent points
     *
     * @author Alexandre
     */
    public List<Integer> getVisibleOpponentPoints(Round round, List<Play> legalPlays, Player.Color aiColor) {
        List<Integer> visiblePoints = new ArrayList<Integer>();
        Player.Color opponentColor = aiColor.opponent();

        for(Play play : legalPlays) {
            int destination = play.getForwardPoint();
            if(destination < 0 || destination >=24){
                continue;
            }

            int opponentCheckers = round.getCurrentBoard().getPoints().get(destination).getCheckers(opponentColor);

            if(opponentCheckers ==1){
                if(!visiblePoints.contains(destination)){
                    visiblePoints.add(destination);
                }
            }
        }
        return visiblePoints;
    }

    /**
     * Checks whether a play lands on a visible opponent blot
     * @param round current game round
     * @param play play being checked
     * @param aiColor colour of the AI
     * @return true if the destination contains exactly one opponent checker
     */
    public boolean isVisibleBlot(Round round, Play play, Player.Color aiColor) {
        int destination = play.getForwardPoint();

        //Bear off does not have a board destination
        if(destination < 0 || destination >=24){
            return false;
        }
        Player.Color opponentColor = aiColor.opponent();
        int opponentCheckers = round.getCurrentBoard().getPoints().get(destination).getCheckers(opponentColor);
        return opponentCheckers ==1;
    }

    /**
     * Counts the opponent checkers that are currently visible to the AI
     * @param round current game round
     * @param legalPlays List of legal Plays
     * @param aiColor colour of the AI
     * @return number of visible opponent checkers
     *
     */
    public int getVisibleOpponentCheckers(Round round, List<Play> legalPlays, Player.Color aiColor) {
        Player.Color opponent = aiColor.opponent();
        int visibleCheckers = 0;

        /*
         * Checkers on the bar are visible
         */
        visibleCheckers += round.getCurrentBoard().getBar().getCheckers(opponent);
        /*
         * Borne-off checkers are visible
         */
        visibleCheckers += round.getCurrentBoard().getOffBoard().getCheckers(opponent);

        /*
         * Keep track of points we have already seen
         */
        List<Integer> visiblePoints = new ArrayList<>();
        /*
         * Check legal destinations
         */
        for(Play play: legalPlays){
            int destination = play.getForwardPoint();
            if(destination < 0 || destination >=24){
                continue;
            }
            Point point = round.getCurrentBoard().getPoints().get(destination);
            int opponentCheckers = point.getCheckers(opponent);

            /*
             * Only a blot is visible
             */
            if(opponentCheckers ==1 && !visiblePoints.contains(destination)){
                visiblePoints.add(destination);
                visibleCheckers ++;
            }
        }
        return visibleCheckers;
    }

    /**
     * Counts the opponent checkers whose positions are hidden from the AI
     * @param round current round game
     * @param legalPlays legal plays available to the AI
     * @param aiColor colour of the AI
     * @return number of hidden opponent checkers
     */
    public int getHiddenOpponentCheckers(Round round, List<Play> legalPlays, Player.Color aiColor) {
        int totalOpponentCheckers = 15;
        int visibleCheckers = getVisibleOpponentCheckers(round, legalPlays, aiColor);
        return totalOpponentCheckers - visibleCheckers;
    }


    /**
     * Calculates the distance to bear off the opponent checkers that are currently
     * visible to the AI. Hidden opponent checkers are not included
     * @param round current game round
     * @param legalPlays legal moves available to the AI
     * @param aiColor colour of the AI
     * @return distance of the visible opponent checkers
     */
    public int getVisibleOpponentDistance(Round round, List<Play> legalPlays, Player.Color aiColor) {
        Player.Color opponent = aiColor.opponent();
        int totalDistance = 0;

        /*
         * Opponent checkers on the bar are visible
         */
        totalDistance+= round.getCurrentBoard().getBar().getCheckers(opponent) * 25;

        /*
         * Keep track of points already revealed
         */
        List<Integer> visiblePoints = new ArrayList<>();

        /*
         * Check legal destination
         */
        for (Play play: legalPlays){
            int destination = play.getForwardPoint();
            if(destination < 0 || destination >=24){
                continue;
            }
            Point point = round.getCurrentBoard().getPoints().get(destination);
            int opponentCheckers = point.getCheckers(opponent);

            /*
             * Only a single opponent checker is visible
             */
            if (opponentCheckers == 1 && !visiblePoints.contains(destination)){
                visiblePoints.add(destination);
                int distance;

                if(opponent== Player.Color.WHITE){
                    distance = point.getPosition() +1;
                }else{
                    distance = 24 - point.getPosition();
                }
                totalDistance += distance;
            }
        }
        return totalDistance;
    }

    /**
     * Calculates the minimum possible distance of the opponent's hidden checkers.
     * The hidden checkers could theoretically be as close as one point from being borne off
     * @param round current game round
     * @param legalPlays legal moves available to the AI
     * @param aiColor colour of the AI
     * @return minimum possible total distance
     */
    public int getMinimumOpponentDistance(Round round,  List<Play> legalPlays, Player.Color aiColor) {
        int visibleDistance = getVisibleOpponentDistance(round, legalPlays, aiColor);
        int hiddenCheckers = getHiddenOpponentCheckers(round, legalPlays, aiColor);
        return visibleDistance + hiddenCheckers;
    }
}
