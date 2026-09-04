package org.petero.droidfish.gamelogic;

/**
 * Move classifications matching Chess.com classification system.
 */
public enum MoveClassification {
    NONE(0x00000000, 0x00000000, ""),
    LOADING(0xFF26A69A, 0x3526A69A, ""),
    BOOK(0xFFA88865, 0x55A88865, "\uD83D\uDCD6"), // 📖
    BRILLIANT(0xFF1BACA6, 0x551BACA6, "!!"),
    GREAT(0xFF5C8BB0, 0x555C8BB0, "!"),
    BEST(0xFF81B64C, 0x5581B64C, "★"),
    EXCELLENT(0xFF81B64C, 0x5581B64C, "✓"),
    GOOD(0xFF96BC4B, 0x5596BC4B, "✓"),
    INACCURACY(0xFFF0C15C, 0x55F0C15C, "?!"),
    MISTAKE(0xFFE58F2A, 0x55E58F2A, "?"),
    MISS(0xFFEE5555, 0x55EE5555, "✕"),
    BLUNDER(0xFFCA3431, 0x55CA3431, "??");

    private final int badgeColor;
    private final int highlightColor;
    private final String glyph;

    MoveClassification(int badgeColor, int highlightColor, String glyph) {
        this.badgeColor = badgeColor;
        this.highlightColor = highlightColor;
        this.glyph = glyph;
    }

    public int getBadgeColor() {
        return badgeColor;
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public String getGlyph() {
        return glyph;
    }
}
