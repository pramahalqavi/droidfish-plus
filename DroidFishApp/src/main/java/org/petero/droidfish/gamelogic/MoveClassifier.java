package org.petero.droidfish.gamelogic;

/**
 * Move classifier implementing Chess.com Expected Points Model (V2).
 */
public class MoveClassifier {

    /**
     * Compute expected win probability in [0.0, 1.0] from centipawns (from White's perspective).
     */
    public static double expectedPoints(int centipawns, boolean isMate, int mateMoves) {
        if (isMate) {
            if (mateMoves > 0) {
                return Math.max(0.95, 1.0 - (0.001 * Math.min(mateMoves, 50)));
            } else if (mateMoves < 0) {
                return Math.min(0.05, 0.001 * Math.min(Math.abs(mateMoves), 50));
            } else {
                return 0.5;
            }
        }
        double winProb = 1.0 / (1.0 + Math.pow(10.0, -centipawns / 400.0));
        return Math.max(0.0, Math.min(1.0, winProb));
    }

    /**
     * Compute total material balance from White's perspective in centipawns.
     */
    public static int getMaterialScore(Position pos) {
        if (pos == null) return 0;
        int score = 0;
        for (int sq = 0; sq < 64; sq++) {
            int p = pos.getPiece(sq);
            switch (p) {
                case Piece.WPAWN: score += 100; break;
                case Piece.WKNIGHT: score += 300; break;
                case Piece.WBISHOP: score += 315; break;
                case Piece.WROOK: score += 500; break;
                case Piece.WQUEEN: score += 900; break;
                case Piece.BPAWN: score -= 100; break;
                case Piece.BKNIGHT: score -= 300; break;
                case Piece.BBISHOP: score -= 315; break;
                case Piece.BROOK: score -= 500; break;
                case Piece.BQUEEN: score -= 900; break;
            }
        }
        return score;
    }

    /**
     * Classify move based on position before and after move.
     */
    public static MoveClassification classify(
            boolean isBook,
            boolean whiteMoved,
            int beforeScore, boolean beforeIsMate, int beforeMateMoves,
            int afterScore, boolean afterIsMate, int afterMateMoves,
            boolean isPieceSacrifice) {
        return classify(isBook, false, whiteMoved,
                beforeScore, beforeIsMate, beforeMateMoves,
                afterScore, afterIsMate, afterMateMoves,
                isPieceSacrifice);
    }

    /**
     * Classify move based on position before and after move, with engine bestMove verification.
     *
     * @param isBook True if move is in opening book
     * @param isBestMove True if move matches the engine's #1 recommendation
     * @param whiteMoved True if White made the move being evaluated
     * @param beforeScore White centipawn score before the move
     * @param beforeIsMate True if before position is a mate score
     * @param beforeMateMoves Mate distance before the move
     * @param afterScore White centipawn score after the move
     * @param afterIsMate True if after position is a mate score
     * @param afterMateMoves Mate distance after the move
     * @param isPieceSacrifice True if player sacrificed material on this move
     * @return MoveClassification
     */
    public static MoveClassification classify(
            boolean isBook,
            boolean isBestMove,
            boolean whiteMoved,
            int beforeScore, boolean beforeIsMate, int beforeMateMoves,
            int afterScore, boolean afterIsMate, int afterMateMoves,
            boolean isPieceSacrifice) {

        if (isBook) {
            return MoveClassification.BOOK;
        }

        double wBeforeWhite = expectedPoints(beforeScore, beforeIsMate, beforeMateMoves);
        double wAfterWhite = expectedPoints(afterScore, afterIsMate, afterMateMoves);

        double wBefore = whiteMoved ? wBeforeWhite : (1.0 - wBeforeWhite);
        double wAfter = whiteMoved ? wAfterWhite : (1.0 - wAfterWhite);

        double delta = Math.max(0.0, wBefore - wAfter);

        // Brilliant: Piece sacrifice that maintains winning or equal advantage
        if ((delta <= 0.02 || isBestMove) && isPieceSacrifice && wAfter >= 0.45) {
            return MoveClassification.BRILLIANT;
        }

        // Best move: Top engine choice is always BEST (even in a losing position)
        if (isBestMove) {
            return MoveClassification.BEST;
        }

        // Check if move walked into forced checkmate that didn't exist before
        boolean allowedForcedMate = !beforeIsMate && afterIsMate &&
            ((whiteMoved && afterMateMoves < 0) || (!whiteMoved && afterMateMoves > 0));
        if (allowedForcedMate) {
            return MoveClassification.BLUNDER;
        }

        // Check if player threw away a forced win/checkmate
        boolean missedForcedMate = beforeIsMate &&
            ((whiteMoved && beforeMateMoves > 0) || (!whiteMoved && beforeMateMoves < 0)) &&
            (!afterIsMate || (whiteMoved ? afterMateMoves <= 0 : afterMateMoves >= 0));
        if (missedForcedMate) {
            return MoveClassification.MISS;
        }

        // Great: Turned a losing position into equal, or equal into winning (only move holding)
        if (delta <= 0.01) {
            if ((wBefore <= 0.35 && wAfter >= 0.48) || (wBefore <= 0.55 && wAfter >= 0.70)) {
                return MoveClassification.GREAT;
            }
        }

        // Best move (delta <= 0.005, maintained winning chances)
        if (delta <= 0.005) {
            return MoveClassification.BEST;
        }

        // Excellent: Expected points drop <= 0.02 (2%)
        if (delta <= 0.02) {
            return MoveClassification.EXCELLENT;
        }

        // Good: Expected points drop <= 0.05 (5%)
        if (delta <= 0.05) {
            return MoveClassification.GOOD;
        }

        // Inaccuracy: Expected points drop <= 0.10 (10%)
        if (delta <= 0.10) {
            return MoveClassification.INACCURACY;
        }

        // Mistake: Expected points drop <= 0.20 (20%)
        if (delta <= 0.20) {
            return MoveClassification.MISTAKE;
        }

        // Miss: Missed winning chance (was winning >= 65%, but dropped to <= 50%)
        if (wBefore >= 0.65 && wAfter <= 0.50 && delta >= 0.15) {
            return MoveClassification.MISS;
        }

        // Blunder: Expected points drop > 0.20 (20%)
        return MoveClassification.BLUNDER;
    }
}
