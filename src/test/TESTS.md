# Test Documentation

This document explains the JUnit tests in `src/test/java/gameproject`.

| Test class | Test method | What the test verifies |
|---|---|---|
| `GreetingTest` | `simpleGreetingTest` | Verifies that `App.getGreeting()` returns `"Hello, JavaFX!"`. |
| `GreetingTest` | `exampleTest` | Verifies the basic arithmetic assertion used in the example test. |
| `LocationTest` | `toStringTest` | Verifies that a new point is represented as `[0,0]`. |
| `LocationTest` | `addRemoveCheckerTest` | Verifies checker counts after adding and removing a white checker, and verifies that removing from an empty location or adding beyond the maximum capacity throws `IllegalStateException`. |
| `LocationTest` | `blockTest` | Verifies that adding an opposing checker to a point already containing two checkers throws `IllegalStateException`. |
| `LocationTest` | `colorIndependentLocationMethodsWorkForBothColorsTest` | Verifies generic add, remove, count, empty, and full checks for both checker colors. |
| `LocationTest` | `colorIndependentPointAddCheckerPreservesBlockingRulesTest` | Verifies that the generic `addChecker(color)` method still enforces point blocking. |
| `LocationTest` | `colorIndependentPointAddCheckerAllowsAddingToOwnStackTest` | Verifies that the generic point method allows a player to add to its own stack instead of treating its own checkers as an opposing block. |
| `BoardTest` | `toStringTest` | Verifies that a board in the standard starting position is converted to the expected string representation. |
| `BoardTest` | `newBoardHasTwentyFourEmptyPointsAndEmptyBarAndOffBoardTest` | Verifies that a new board has 24 empty points, an empty bar, and an empty off-board area. |
| `BoardTest` | `setupBoardPlacesStandardStartingPositionTest` | Verifies the standard starting positions and confirms that each player has 15 checkers on the board. |
| `BoardTest` | `makeNormalPlayMovesCheckerBetweenPointsTest` | Verifies that a normal play moves a white checker from its initial point to the destination point. |
| `BoardTest` | `makeNormalPlayMovesBlackCheckerBetweenPointsTest` | Verifies the reverse-direction normal move for a black checker. |
| `BoardTest` | `makeNormalPlayHitsLoneOpponentCheckerTest` | Verifies that moving onto a point with one opposing checker hits that checker and places it on the bar. |
| `BoardTest` | `makeNormalPlayHitsLoneWhiteCheckerWithBlackPlayTest` | Verifies that a black move hits a lone white checker and sends it to the bar. |
| `BoardTest` | `makeNormalPlayWithEmptySourceThrowsWithoutMutatingBoardTest` | Verifies that a normal play with no moving checker at the source throws and does not partially modify the board. |
| `BoardTest` | `makeNormalPlayOntoPointBlockedByTwoOpponentCheckersThrowsTest` | Verifies that a normal move onto a point occupied by two opposing checkers is rejected with `IllegalStateException`. |
| `BoardTest` | `makePlayDispatchesNormalPlayCorrectlyTest` | Verifies that `makePlay()` dispatches a normal play and updates the board correctly. |
| `BoardTest` | `makePlayDispatchesBearOffPlayCorrectlyTest` | Verifies that `makePlay()` dispatches a bearing-off play correctly. |
| `BoardTest` | `makePlayDispatchesOffBarPlayCorrectlyTest` | Verifies that `makePlay()` dispatches a bar-entry play correctly. |
| `BoardTest` | `makeBearOffPlayMovesCheckerFromPointToOffBoardTest` | Verifies that bearing off moves a checker from a point to the off-board area. |
| `BoardTest` | `makeBearOffPlayMovesBlackCheckerToOffBoardTest` | Verifies bearing off for a black checker. |
| `BoardTest` | `makeBearOffPlayFromEmptyPointThrowsTest` | Verifies that bearing off from an empty point throws `IllegalStateException`. |
| `BoardTest` | `makeOffBarPlayEntersWhiteCheckerOntoForwardPointTest` | Verifies that a white checker is removed from the bar and entered onto the specified point. |
| `BoardTest` | `makeOffBarPlayEntersBlackCheckerOntoForwardPointTest` | Verifies that a black checker is removed from the bar and entered onto the specified point. |
| `BoardTest` | `makeOffBarPlayWhiteHitsLoneBlackCheckerTest` | Verifies that a white checker entering from the bar hits a lone black checker. |
| `BoardTest` | `makeOffBarPlayBlackHitsLoneWhiteCheckerTest` | Verifies that a black checker entering from the bar hits a lone white checker. |
| `BoardTest` | `makeOffBarPlayOntoPointBlockedByTwoOpponentCheckersThrowsTest` | Verifies that re-entering from the bar onto a point blocked by two opposing checkers throws `IllegalStateException`. |
| `BoardTest` | `boardCopyConstructorAppliesPlayWithoutMutatingOriginalBoardTest` | Verifies that the board copy constructor applies a play to the new board while leaving the original board unchanged. |
| `RoundTest` | `determineFirstPlayerAlwaysPicksAnActualPlayerTest` | Verifies that determining the first player assigns either the white or black player, never an unrelated or null player. |
| `RoundTest` | `switchPlayerTogglesBetweenWhiteAndBlackTest` | Verifies that switching players alternates between white and black. |
| `RoundTest` | `allMovesReturnsBothDiceForARegularRollTest` | Verifies that a non-double roll produces two available moves, one for each die. |
| `RoundTest` | `allMovesReturnsFourCopiesForADoubleRollTest` | Verifies that a double roll produces four available moves with the doubled value. |
| `RoundTest` | `startTurnUsesTheInjectedInitialDiceValuesTest` | Verifies that the first `startTurn()` uses the values supplied by the injected `Dice` object without rolling again. |
| `RoundTest` | `startTurnThrowsInjectedDiceOnTheNextTurnTest` | Verifies that a later `startTurn()` rolls the injected dice and stores the next deterministic seeded values. |
| `RoundTest` | `determineFirstPlayerUsesTheInjectedRandomSequenceTest` | Verifies that `determineFirstPlayer()` uses the injected deterministic dice sequence and selects the correct player from the resulting roll. |
| `RoundTest` | `checkBearOffIsTrueOnlyWhenAllOfThatColorsCheckersAreHomeTest` | Verifies that bearing off is available when all of the current player's checkers are in the home board. |
| `RoundTest` | `checkBearOffIsTrueForBlackWhenAllBlackCheckersAreHomeTest` | Verifies bearing-off eligibility for black when its checkers are in the black home board. |
| `RoundTest` | `checkBearOffIsFalseWhenACheckerIsOutsideTheHomeBoardTest` | Verifies that bearing off is unavailable while a current player's checker remains outside the home board. |
| `RoundTest` | `checkBearOffIsFalseForBlackWhenACheckerIsOutsideTheHomeBoardTest` | Verifies that black cannot bear off while a checker remains outside its home board. |
| `RoundTest` | `checkBearOffIsFalseWhenCurrentPlayerHasACheckerOnTheBarTest` | Verifies that bearing off is unavailable when the current player has a checker on the bar, even if the other checker is in the home board. |
| `RoundTest` | `checkBarReflectsWhetherCurrentPlayerHasACheckerOnTheBarTest` | Verifies that `checkBar()` reflects whether the current player has a checker on the bar. |
| `RoundTest` | `checkBarReflectsBlackCheckerOnTheBarTest` | Verifies `checkBar()` for a black checker on the bar. |
| `RoundTest` | `moveFromBarIsBlockedWhenTheEntryPointIsMadeByTheOpponentTest` | Verifies that a checker cannot re-enter from the bar onto a point occupied by two opposing checkers. |
| `RoundTest` | `moveFromBarEntersOnTheOpenPointForThatDieTest` | Verifies that a white checker can re-enter onto the correct point for the die value. |
| `RoundTest` | `moveFromBarEntersBlackOnDieMinusOneTest` | Verifies that a black checker re-enters onto the correct point using black's movement direction. |
| `RoundTest` | `executingWhiteBarEntryKeepsOtherWhiteBarCheckersAndHitsBlackBlotTest` | Verifies that entering one of two white bar checkers removes only one white checker from the bar while hitting a black blot. |
| `RoundTest` | `whiteBarEntryWithSixAndTwoKeepsSecondCheckerWhenSixIsBlockedTest` | Reproduces the logged 6-and-2 scenario and verifies that the second white checker remains on the bar when the remaining six cannot enter because point 19 is blocked. |
| `RoundTest` | `possiblePlaysOnlyOffersBarEntriesWhileACheckerIsOnTheBarTest` | Verifies that, while a checker is on the bar, possible plays contain only bar-entry plays and do not move other checkers. |
| `RoundTest` | `possiblePlaysForBlackOnlyOffersBarEntriesWhileACheckerIsOnTheBarTest` | Verifies the same bar-priority rule for black. |
| `RoundTest` | `noLegalPlaysWhenOnTheBarAndOpponentsHomeBoardIsFullyClosedTest` | Verifies that no legal plays exist when a checker is on the bar and every possible entry point is blocked. |
| `RoundTest` | `mustPlayTheHigherDieWhenOnlyOneOfTwoDiceCanBePlayedTest` | Verifies that the higher die is required when only one die can be used during the turn. |
| `RoundTest` | `mustPlayInAWayThatUsesBothDiceWhenThatIsPossibleTest` | Verifies that the required first play is the one that allows both dice to be used when that sequence is possible. |
| `RoundTest` | `bearingOffWithAnOversizedDieIsBlockedWhileACheckerIsFurtherFromHomeTest` | Verifies that an oversized bearing-off move cannot skip a checker that is further from home. |
| `RoundTest` | `bearingOffWithAnOversizedDieIsAllowedOnceNoCheckerIsFurtherBackTest` | Verifies that an oversized die may bear off a checker once no checker remains further from home. |
| `RoundTest` | `bearingOffBlackWithAnExactDieIsAllowedInTheBlackHomeBoardTest` | Verifies an exact black bearing-off move. |
| `RoundTest` | `bearingOffBlackWithAnOversizedDieIsBlockedWhileACheckerIsFurtherFromHomeTest` | Verifies black’s oversized-die ordering rule. |
| `RoundTest` | `bearingOffBlackWithAnOversizedDieIsAllowedOnceNoCheckerIsFurtherBackTest` | Verifies black may use an oversized die when no checker is further from home. |
| `RoundTest` | `moveBoardMovesWhiteTowardLowerIndicesTest` | Verifies that white checkers move toward lower point indices. |
| `RoundTest` | `moveBoardMovesBlackTowardHigherIndicesTest` | Verifies that black checkers move toward higher point indices. |
| `RoundTest` | `moveBoardDoesNotOfferBlackMoveOntoBlockedPointTest` | Verifies that black cannot move onto a point blocked by two white checkers. |
| `BoardViewTest` | `selectingWhiteCheckerHighlightsItsLegalDestinations` | TestFX regression test verifying that clicking a rendered white checker highlights a legal destination. |
| `AIAgentBenchmarkTest` | `benchmarkAdvancedDepthTwoAndAppendToCsv` | Runs AdvancedAgent at depth 2 (0 warmup, 1 measured run by default; override with `-Dbenchmark.depth2.warmupRuns` / `-Dbenchmark.depth2.measuredRuns`) and appends all 21 dice results to `src/test/java/gameproject/ai/output.csv` under the label set in the method. |
| `AIAgentBenchmarkTest` | `benchmarkAndAppend(...)` | Generic append-only benchmark helper. It accepts the algorithm, depth, improvement label, warmup/measured-run system properties, skips existing algorithm/depth/label/dice keys, appends one row after each completed dice outcome, and records measured AdvancedAgent metrics (including `duplicateTurns`) averaged across measured runs. Refuses to append if the CSV header differs from `CSV_HEADER`. |
| `visualize_benchmark.py` | `main` | Generates `benchmark_visualization.html` from `output.csv`. A depth menu shows one search depth at a time with only the first run, the previous improvement and the current one ("Show all improvements" reveals the rest). Includes headline speedups, time per decision, time and search work per dice roll (log scale), and collapsible sections for turn deduplication, doubles vs non-doubles and all rows. Blank metric fields are ignored. |
| `AIAgentStrengthTest` | `depthOneVersusDepthZero` | Plays full games (no doubling cube) between two AI agents and appends one row per game to `src/test/java/gameproject/ai/strength.csv`. Games come in pairs with the same dice seed and swapped colours; already recorded games are skipped. Number of games via `-Dstrength.games` (default 100). The visualizer shows the win rate with a 95% confidence interval. |
| `AdvancedAgentApplyUndoTest` | `normalMoveRestoresState`, `normalMoveThatHitsABlotRestoresState`, `bearOffRestoresState`, `barEntryRestoresState`, `barEntryThatHitsABlotRestoresState`, `doubleRollRestoresAllFourRemainingMoves`, `completeTurnHelperRestoresOnePlayTurn`, `completeTurnHelperRestoresMultiPlayTurn`, `completeTurnHelperRestoresTurnContainingHit`, `completeTurnHelperRestoresBearOffTurn`, `completeTurnHelperRestoresBarEntryTurn`, `completeTurnHelperRestoresFourPlayDoubleTurn`, `chooseMoveRestoresInputRoundAfterRecursiveTurnGeneration`, `chooseMoveRestoresInputRoundAfterDoubleRoll` | Verifies that the AdvancedAgent apply/undo helpers restore every point, bar, off-board, and remaining-dice count for individual plays and complete turns, including normal moves, hits, bear-offs, bar entries, and doubles. |
| `AIAgentConsistencyTest` | `easyAndAdvancedDepthZeroChooseTheSameDecisionOnTheStandardBoard` | Verifies that EasyAgent and AdvancedAgent with depth 0 choose the same complete move sequence for every dice combination on the standard starting board using deterministic evaluator coefficients. |
