package com.example.listi;

import android.app.Activity;

public class ThemeManager {
    public static void applyTheme(Activity activity){
        FontManager fontManager = new FontManager(activity);
        ColourManager colourManager = new ColourManager(activity);

        String fontType = fontManager.getFontType();
        String colourType = colourManager.getColourType();

        if(fontType.equals(FontManager.FONT_OPEN_DYSLEXIC)){
            if(colourType.equals(ColourManager.COLOUR_2)){
                activity.setTheme(R.style.Theme_Listi_OpenDyslexic_Colour2_NoActionBar);
            }else{
                activity.setTheme(R.style.Theme_Listi_OpenDyslexic_NoActionBar);
            }
        }else {
            if (colourType.equals(ColourManager.COLOUR_2)) {
                activity.setTheme(R.style.Theme_Listi_Colour2_NoActionBar);
            }else{
                activity.setTheme(R.style.Theme_Listi_NoActionBar);
            }
        }
    }
}