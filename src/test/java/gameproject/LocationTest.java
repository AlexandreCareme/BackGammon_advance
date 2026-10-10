package gameproject;

// AI-assisted (GitHub Copilot), reviewed and adapted by Kilian.

import gameproject.game.elements.Location;
import gameproject.game.elements.Point;
import gameproject.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocationTest {

    /**
     * Checks whether toString of Location objects works as intended
     */
    @Test
    void toStringTest(){
        Point point = new Point(1);
        assertEquals("[0,0]", point.toString());
    }
    @Test
    void addRemoveCheckerTest(){
        Location point = new Point(1);

        // Check correct configuration of Location that is with zero checkers
        assertEquals(0, point.getCheckers(Player.Color.WHITE));
        assertEquals(0, point.getCheckers(Player.Color.BLACK));

        //check correct count after adding checker
        point.addChecker(Player.Color.WHITE);
        assertEquals(1, point.getCheckers(Player.Color.WHITE));
        point.removeChecker(Player.Color.WHITE);
        // Check correct checker count of location object
        assertEquals(0, point.getCheckers(Player.Color.WHITE));

        // Check min checker limit of location
        assertThrows(
                IllegalStateException.class,
                () -> point.removeChecker(Player.Color.WHITE)
        );

        for( int i= 1; i<= 15; i++ ){
            point.addChecker(Player.Color.WHITE);
        }
        // Check max checker limit of location

        assertThrows(
                IllegalStateException.class,
                () -> point.addChecker(Player.Color.WHITE)
        );

    }

    /**
     * Tests if ignoring blocking throws illegal state
     */
    @Test
    void blockTest(){
        Location point = new Point(1);
        point.addChecker(Player.Color.WHITE);
        point.addChecker(Player.Color.WHITE);

        assertThrows(
                IllegalStateException.class,
                () -> point.addChecker(Player.Color.BLACK)
        );
    }

    @Test
    void colorIndependentLocationMethodsWorkForBothColorsTest() {
        Location point = new Point(1);

        point.addChecker(Player.Color.WHITE);
        point.addChecker(Player.Color.BLACK);

        assertEquals(1, point.getCheckers(Player.Color.WHITE));
        assertEquals(1, point.getCheckers(Player.Color.BLACK));
        assertFalse(point.isEmpty(Player.Color.WHITE));
        assertFalse(point.isEmpty(Player.Color.BLACK));
        assertFalse(point.isFull(Player.Color.WHITE));
        assertFalse(point.isFull(Player.Color.BLACK));

        point.removeChecker(Player.Color.WHITE);
        point.removeChecker(Player.Color.BLACK);

        assertTrue(point.isEmpty(Player.Color.WHITE));
        assertTrue(point.isEmpty(Player.Color.BLACK));
    }

    @Test
    void colorIndependentPointAddCheckerPreservesBlockingRulesTest() {
        Location point = new Point(1);
        point.addChecker(Player.Color.BLACK);
        point.addChecker(Player.Color.BLACK);

        assertThrows(
                IllegalStateException.class,
                () -> point.addChecker(Player.Color.WHITE)
        );
    }

    @Test
    void colorIndependentPointAddCheckerAllowsAddingToOwnStackTest() {
        Location point = new Point(1);
        point.addChecker(Player.Color.BLACK);
        point.addChecker(Player.Color.BLACK);

        point.addChecker(Player.Color.BLACK);

        assertEquals(3, point.getCheckers(Player.Color.BLACK));
    }
}
