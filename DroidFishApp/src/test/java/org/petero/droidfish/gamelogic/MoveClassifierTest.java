package org.petero.droidfish.gamelogic;

import org.junit.Test;
import static org.junit.Assert.*;

public class MoveClassifierTest {

    @Test
    public void testBookMove() {
        MoveClassification mc = MoveClassifier.classify(
            true, true, 20, false, 0, 20, false, 0, false
        );
        assertEquals(MoveClassification.BOOK, mc);
    }

    @Test
    public void testBestMove() {
        // Equal position before (0 cp) and after (0 cp)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, 0, false, 0, false
        );
        assertEquals(MoveClassification.BEST, mc);
    }

    @Test
    public void testBrilliantMove() {
        // White was winning or equal, piece sacrifice
        MoveClassification mc = MoveClassifier.classify(
            false, true, 100, false, 0, 120, false, 0, true
        );
        assertEquals(MoveClassification.BRILLIANT, mc);
    }

    @Test
    public void testGreatMove() {
        // White was losing/equal (e.g. -150 cp -> wBefore ~ 0.29) and found move giving equal/advantage (wAfter ~ 0.50)
        MoveClassification mc = MoveClassifier.classify(
            false, true, -150, false, 0, 0, false, 0, false
        );
        assertEquals(MoveClassification.GREAT, mc);
    }

    @Test
    public void testExcellentMove() {
        // delta <= 0.02
        // 0 cp (w=0.500) to -10 cp (w=0.486, delta=0.014)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, -10, false, 0, false
        );
        assertEquals(MoveClassification.EXCELLENT, mc);
    }

    @Test
    public void testGoodMove() {
        // delta <= 0.05
        // 0 cp (w=0.500) to -30 cp (w=0.457, delta=0.043)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, -30, false, 0, false
        );
        assertEquals(MoveClassification.GOOD, mc);
    }

    @Test
    public void testInaccuracyMove() {
        // delta <= 0.10
        // 0 cp (w=0.500) to -60 cp (w=0.415, delta=0.085)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, -60, false, 0, false
        );
        assertEquals(MoveClassification.INACCURACY, mc);
    }

    @Test
    public void testMistakeMove() {
        // delta <= 0.20
        // 0 cp (w=0.500) to -120 cp (w=0.334, delta=0.166)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, -120, false, 0, false
        );
        assertEquals(MoveClassification.MISTAKE, mc);
    }

    @Test
    public void testBlunderMove() {
        // delta > 0.20 and wBefore not winning (wBefore = 0.50)
        // 0 cp (w=0.500) to -300 cp (w=0.151, delta=0.349)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 0, false, 0, -300, false, 0, false
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testMissMove() {
        // White had winning position (+200 cp -> wBefore = 0.76 >= 0.65), dropped to equal (0 cp -> wAfter = 0.50)
        MoveClassification mc = MoveClassifier.classify(
            false, true, 200, false, 0, 0, false, 0, false
        );
        assertEquals(MoveClassification.MISS, mc);
    }

    @Test
    public void testBlackPerspective() {
        // Black plays best move: before was 0 cp, after is -100 cp (better for black)
        // Black's win prob increased from 0.50 to 0.64
        MoveClassification mc = MoveClassifier.classify(
            false, false, 0, false, 0, -100, false, 0, false
        );
        assertEquals(MoveClassification.BEST, mc);
    }

    @Test
    public void testLosingPositionMaintainsEvalGivesBest() {
        // Position was losing (-400 cp), move maintains eval (-400 cp) -> BEST, NOT BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, -400, false, 0, -400, false, 0, false
        );
        assertEquals(MoveClassification.BEST, mc);
    }

    @Test
    public void testLosingPositionBestMoveFlagGivesBest() {
        // Position was losing (-400 cp), played engine's top choice (isBestMove = true)
        // Even if eval drifts from -400 to -450, it must be BEST
        MoveClassification mc = MoveClassifier.classify(
            false, true, true, -400, false, 0, -450, false, 0, false
        );
        assertEquals(MoveClassification.BEST, mc);
    }

    @Test
    public void testLosingPositionExcellentMove() {
        // Position was -400 cp (w=0.0909), dropped slightly to -425 cp (w=0.0792, delta=0.0117 <= 0.02) -> EXCELLENT
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, -400, false, 0, -425, false, 0, false
        );
        assertEquals(MoveClassification.EXCELLENT, mc);
    }

    @Test
    public void testLosingPositionAllowedMateGivesBlunder() {
        // Position was losing (-400 cp), move walked into forced checkmate -> BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, -400, false, 0, 0, true, -2, false
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testMissedCheckmateGivesMiss() {
        // White had mate in 2, but played a move that dropped out of forced mate to +200 cp -> MISS!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, 0, true, 2, 200, false, 0, false
        );
        assertEquals(MoveClassification.MISS, mc);
    }

    @Test
    public void testBlunderWinningToLosing() {
        // Was winning (+300 cp, w=0.85), now losing (-250 cp, w=0.19) -> BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, 300, false, 0, -250, false, 0, false, 0
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testBlunderEqualToLosing() {
        // Was around equal (0 cp, w=0.50), now losing (-250 cp, w=0.19) -> BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, 0, false, 0, -250, false, 0, false, 0
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testBlunderLosingToMoreLosingEvalDrop() {
        // Was losing (-300 cp, w=0.15), now collapsed to -600 cp (w=0.03, evalDrop=300) -> BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, -300, false, 0, -600, false, 0, false, 0
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testBlunderLosingToMoreLosingHungPiece() {
        // Was losing (-300 cp), hung a minor piece (moverMatChange = -300 cp) -> BLUNDER!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, -300, false, 0, -450, false, 0, false, -300
        );
        assertEquals(MoveClassification.BLUNDER, mc);
    }

    @Test
    public void testMissWinningToAroundEqual() {
        // Was winning (+250 cp, w=0.81), dropped to around equal (0 cp, w=0.50) -> MISS!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, 250, false, 0, 0, false, 0, false, 0
        );
        assertEquals(MoveClassification.MISS, mc);
    }

    @Test
    public void testMissWinningToLessWinning() {
        // Was winning (+350 cp, w=0.88), dropped to less winning (+120 cp, w=0.67, delta=0.21) -> MISS!
        MoveClassification mc = MoveClassifier.classify(
            false, false, true, 350, false, 0, 120, false, 0, false, 0
        );
        assertEquals(MoveClassification.MISS, mc);
    }

    @Test
    public void testGetMaterialScore() throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        int mat = MoveClassifier.getMaterialScore(pos);
        assertEquals(0, mat); // Standard start position has equal material
    }
}
