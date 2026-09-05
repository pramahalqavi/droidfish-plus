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
                isPieceSacrifice, 0);
    }

    /**
     * Classify move based on position before and after move, with engine bestMove verification.
     */
    public static MoveClassification classify(
            boolean isBook,
            boolean isBestMove,
            boolean whiteMoved,
            int beforeScore, boolean beforeIsMate, int beforeMateMoves,
            int afterScore, boolean afterIsMate, int afterMateMoves,
            boolean isPieceSacrifice) {
        return classify(isBook, isBestMove, whiteMoved,
                beforeScore, beforeIsMate, beforeMateMoves,
                afterScore, afterIsMate, afterMateMoves,
                isPieceSacrifice, 0);
    }

    /**
     * Classify move based on position before and after move, with bestMove and material swing verification.
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
     * @param moverMatChange Centipawn material change from the mover's perspective
     * @return MoveClassification
     */
    public static MoveClassification classify(
            boolean isBook,
            boolean isBestMove,
            boolean whiteMoved,
            int beforeScore, boolean beforeIsMate, int beforeMateMoves,
            int afterScore, boolean afterIsMate, int afterMateMoves,
            boolean isPieceSacrifice,
            int moverMatChange) {
        return classify(isBook, isBestMove, whiteMoved,
                beforeScore, beforeIsMate, beforeMateMoves,
                afterScore, afterIsMate, afterMateMoves,
                isPieceSacrifice, moverMatChange,
                false, 0, false, 0);
    }

    /**
     * Classify move with second-best move evaluation for detecting Great Moves ("only move").
     */
    public static MoveClassification classify(
            boolean isBook,
            boolean isBestMove,
            boolean whiteMoved,
            int beforeScore, boolean beforeIsMate, int beforeMateMoves,
            int afterScore, boolean afterIsMate, int afterMateMoves,
            boolean isPieceSacrifice,
            int moverMatChange,
            boolean hasSecondScore,
            int secondScore,
            boolean secondIsMate,
            int secondMateMoves) {

        if (isBook) {
            return MoveClassification.BOOK;
        }

        double wBeforeWhite = expectedPoints(beforeScore, beforeIsMate, beforeMateMoves);
        double wAfterWhite = expectedPoints(afterScore, afterIsMate, afterMateMoves);

        double wBefore = whiteMoved ? wBeforeWhite : (1.0 - wBeforeWhite);
        double wAfter = whiteMoved ? wAfterWhite : (1.0 - wAfterWhite);

        double delta = Math.max(0.0, wBefore - wAfter);

        // Mover centipawn drop calculation
        int moverBeforeCp = whiteMoved ? beforeScore : -beforeScore;
        int moverAfterCp = whiteMoved ? afterScore : -afterScore;
        int evalDrop = moverBeforeCp - moverAfterCp;

        // Brilliant: Piece sacrifice that maintains winning or equal advantage
        if ((delta <= 0.02 || isBestMove) && isPieceSacrifice && wAfter >= 0.45) {
            return MoveClassification.BRILLIANT;
        }

        // Great Move: The "only good move" in a position, or critical saving/punishing move
        if (isBestMove || delta <= 0.005) {
            boolean isGreat = false;
            if (hasSecondScore) {
                double wSecondWhite = expectedPoints(secondScore, secondIsMate, secondMateMoves);
                double wSecond = whiteMoved ? wSecondWhite : (1.0 - wSecondWhite);
                double gapW = wBefore - wSecond;

                int moverSecondCp = whiteMoved ? secondScore : -secondScore;
                int cpGap = moverBeforeCp - moverSecondCp;

                // 1. Second best move allows forced checkmate against mover, but best move avoids it
                boolean secondAllowsMate = secondIsMate &&
                    ((whiteMoved && secondMateMoves < 0) || (!whiteMoved && secondMateMoves > 0));
                if (secondAllowsMate && wBefore >= 0.40) {
                    isGreat = true;
                }
                // 2. Best move maintains winning (wBefore >= 0.58), but 2nd best drops to equal or losing (wSecond <= 0.50)
                else if (wBefore >= 0.58 && wSecond <= 0.50 && (gapW >= 0.08 || cpGap >= 75)) {
                    isGreat = true;
                }
                // 3. Best move maintains equal (wBefore >= 0.45), but 2nd best drops to losing (wSecond <= 0.35)
                else if (wBefore >= 0.45 && wSecond <= 0.35 && (gapW >= 0.08 || cpGap >= 75)) {
                    isGreat = true;
                }
                // 4. Substantial gap where any other move is significantly worse (cpGap >= 120 or gapW >= 0.12)
                else if ((cpGap >= 120 || gapW >= 0.12) && wBefore >= 0.40) {
                    isGreat = true;
                }
            } else {
                // Heuristic fallback when second move evaluation is not available
                // e.g. turning a losing position into equal or winning
                if ((wBefore <= 0.35 && wAfter >= 0.48) || (wBefore <= 0.50 && wAfter >= 0.68)) {
                    isGreat = true;
                }
            }

            if (isGreat) {
                return MoveClassification.GREAT;
            }
            return MoveClassification.BEST;
        }

        // Best move by delta (delta <= 0.005, maintained winning chances)
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

        // Check forced mate transitions
        boolean allowedForcedMate = !beforeIsMate && afterIsMate &&
            ((whiteMoved && afterMateMoves < 0) || (!whiteMoved && afterMateMoves > 0));
        boolean missedForcedMate = beforeIsMate &&
            ((whiteMoved && beforeMateMoves > 0) || (!whiteMoved && beforeMateMoves < 0)) &&
            (!afterIsMate || (whiteMoved ? afterMateMoves <= 0 : afterMateMoves >= 0));

        // ==========================================
        // 1. BLUNDER
        // - Winning to losing
        // - Around equal to losing
        // - Losing to even more losing
        // ==========================================

        // Allowed forced checkmate against mover
        if (allowedForcedMate) {
            return MoveClassification.BLUNDER;
        }

        // Winning to losing: was winning (wBefore >= 0.58), now losing (wAfter <= 0.38)
        if (wBefore >= 0.58 && wAfter <= 0.38) {
            return MoveClassification.BLUNDER;
        }

        // Around equal to losing: was equal/near equal (wBefore >= 0.38), now clearly losing (wAfter <= 0.28) with notable drop
        if (wBefore >= 0.38 && wAfter <= 0.28 && delta >= 0.15) {
            return MoveClassification.BLUNDER;
        }

        // Losing to even more losing: already losing (wBefore <= 0.35), but made position much worse
        if (wBefore <= 0.35) {
            // Hung a piece (material drop <= -200 cp) confirmed by engine loss
            if (moverMatChange <= -200 && delta >= 0.02) {
                return MoveClassification.BLUNDER;
            }
            // Severe eval collapse (eval dropped by >= 250 cp)
            if (evalDrop >= 250 && delta >= 0.02) {
                return MoveClassification.BLUNDER;
            }
            // Substantial win-chance collapse in losing position
            if (wBefore >= 0.15 && wAfter <= 0.05 && delta >= 0.10) {
                return MoveClassification.BLUNDER;
            }
        }

        // Catastrophic delta drop that ends in a losing or dead-lost position
        if (delta >= 0.30 && wAfter <= 0.38) {
            return MoveClassification.BLUNDER;
        }

        // ==========================================
        // 2. MISS (Miss win)
        // - Winning to around equal
        // - Winning to less winning
        // ==========================================

        // Had a forced mate and threw it away, but not losing (wAfter >= 0.38)
        if (missedForcedMate && wAfter >= 0.38) {
            return MoveClassification.MISS;
        }

        // Winning to around equal: was winning (wBefore >= 0.60), dropped to around equal (0.38 <= wAfter <= 0.58), delta >= 0.10
        if (wBefore >= 0.60 && (wAfter >= 0.38 && wAfter <= 0.58) && delta >= 0.10) {
            return MoveClassification.MISS;
        }

        // Winning to less winning: was clearly winning (wBefore >= 0.65), still ahead (wAfter >= 0.50), but dropped substantial winning chances (delta >= 0.12)
        if (wBefore >= 0.65 && wAfter >= 0.50 && delta >= 0.12) {
            return MoveClassification.MISS;
        }

        // ==========================================
        // 3. INACCURACY & MISTAKE
        // ==========================================

        // Inaccuracy: Expected points drop <= 0.10 (10%)
        if (delta <= 0.10) {
            return MoveClassification.INACCURACY;
        }

        // Mistake: Noticeable error that worsened position (delta <= 0.22)
        if (delta <= 0.22) {
            return MoveClassification.MISTAKE;
        }

        // Fallback for large drops that didn't meet earlier specific conditions
        return (wAfter <= 0.35) ? MoveClassification.BLUNDER : MoveClassification.MISTAKE;
    }
}
