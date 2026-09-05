/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;

public class ColorTheme {
    private static ColorTheme inst = null;

    /** Get singleton instance. */
    public static ColorTheme instance() {
        if (inst == null)
            inst = new ColorTheme();
        return inst;
    }

    public final static int DARK_SQUARE = 0;
    public final static int BRIGHT_SQUARE = 1;
    public final static int SELECTED_SQUARE = 2;
    public final static int DARK_PIECE = 3;
    public final static int BRIGHT_PIECE = 4;
    public final static int CURRENT_MOVE = 5;
    public final static int ARROW_0 = 6;
    public final static int ARROW_1 = 7;
    public final static int ARROW_2 = 8;
    public final static int ARROW_3 = 9;
    public final static int ARROW_4 = 10;
    public final static int ARROW_5 = 11;
    public final static int ARROW_6 = 12;
    public final static int ARROW_7 = 13;
    public final static int MAX_ARROWS = 8;
    public final static int SQUARE_LABEL = 14;
    public final static int DECORATION = 15;
    public final static int PGN_COMMENT = 16;
    public final static int FONT_FOREGROUND = 17;
    public final static int GENERAL_BACKGROUND = 18;
    private final static int numColors = 19;

    private int[] colorTable = new int[numColors];

    private static final String[] prefNames = {
        "darkSquare", "brightSquare", "selectedSquare", "darkPiece", "brightPiece", "currentMove",
        "arrow0", "arrow1", "arrow2", "arrow3", "arrow4", "arrow5", "arrow6", "arrow7",
        "squareLabel", "decoration", "pgnComment", "fontForeground", "generalBackground"
    };
    private static final String prefPrefix = "color_";

    private final static int defaultTheme = 0;
    final static int[] themeNames = {
        R.string.colortheme_blue,
        R.string.colortheme_lavender,
        R.string.colortheme_forest_green,
        R.string.colortheme_marine,
        R.string.colortheme_terracotta,
        R.string.colortheme_grey,
        R.string.colortheme_scid_default,
        R.string.colortheme_scid_brown,
        R.string.colortheme_scid_green
    };
    private final static String[][] themeColors = {
    { // Blue
        "#FF83A5D2", "#FFFFFFFA", "#FF3232D1", "#FF282828", "#FFF0F0F0", "#FF3333FF",
        "#A01F1FFF", "#A01FFF1F", "#501F1FFF", "#501FFF1F", "#371F1FFF", "#3C1FFF1F", "#1E1F1FFF", "#281FFF1F",
        "#FFFF0000", "#FF808080", "#FFC0C000", "#FFFFFF00", "#FF2E2B53"
    },
    { // Lavender
        "#FF8677B9", "#FFF0F1F0", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF8677B9",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FF8677B9", "#FF808080", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Forest Green
        "#FF33674B", "#FFEEEEEB", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF33674B",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FF33674B", "#FF808080", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Marine
        "#FF4E7398", "#FFEAE9D2", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF4E7398",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FF4E7398", "#FF808080", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Terracotta
        "#FFB95948", "#FFF4DBC4", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FFB95948",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FFB95948", "#FF808080", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Grey
        "#FF666666", "#FFDDDDDD", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF888888",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FFFF0000", "#FF909090", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Scid Default
        "#FF80A0A0", "#FFD0E0D0", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FFFF0000", "#FF808080", "#FFC0C000", "#FFDEFBDE", "#FF213429"
    },
    { // Scid Brown
        "#B58863",   "#F0D9B5",   "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FFFF0000", "#FF808080", "#FFC0C000", "#FFF7FAE3", "#FF40260A"
    },
    { // Scid Green
        "#FF769656", "#FFEEEED2", "#FFFF0000", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#371F1FFF", "#3CFF1F1F", "#1E1F1FFF", "#28FF1F1F",
        "#FFFF0000", "#FF808080", "#FFC0C000", "#FFDEE3CE", "#FF183C21"
    }
    };

    final void readColors(SharedPreferences settings) {
        for (int i = 0; i < numColors; i++) {
            if (i == CURRENT_MOVE || i == PGN_COMMENT || i == FONT_FOREGROUND || i == GENERAL_BACKGROUND)
                continue;
            String prefName = prefPrefix + prefNames[i];
            String defaultColor = themeColors[defaultTheme][i];
            String colorString = settings.getString(prefName, defaultColor);
            colorTable[i] = 0;
            try {
                colorTable[i] = Color.parseColor(colorString);
            } catch (IllegalArgumentException|StringIndexOutOfBoundsException ignore) {
            }
        }
    }

    final void setTheme(SharedPreferences settings, int themeType) {
        Editor editor = settings.edit();
        for (int i = 0; i < numColors; i++) {
            if (i == CURRENT_MOVE || i == PGN_COMMENT || i == FONT_FOREGROUND || i == GENERAL_BACKGROUND)
                continue;
            editor.putString(prefPrefix + prefNames[i], themeColors[themeType][i]);
        }
        editor.apply();
        readColors(settings);
    }

    public final int getColor(int colorType) {
        boolean dark = DroidFishApp.isDarkMode(DroidFishApp.getContext());
        switch (colorType) {
        case CURRENT_MOVE:
            return dark ? 0xFF3D5A80 : 0xFFD3E3FD;
        case PGN_COMMENT:
            return dark ? 0xFFA5D6A7 : 0xFF2E7D32;
        case FONT_FOREGROUND:
            return dark ? 0xFFE6E1E5 : 0xFF1D1B20;
        case GENERAL_BACKGROUND:
            return dark ? 0xFF1D1B20 : 0xFFF7F2FA;
        default:
            return colorTable[colorType];
        }
    }
}
