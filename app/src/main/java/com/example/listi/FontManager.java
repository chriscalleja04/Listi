package com.example.listi;
import android.content.Context;
import android.content.SharedPreferences;
public class FontManager {
    private static final String NAME = "fontPreference";
    private static final String TYPE = "font_type";
    public static final String FONT_COMIC_SANS = "comic_sans";
    public static final String FONT_OPEN_DYSLEXIC = "open_dyslexic";

    public static final String FONT_ANDIKA = "andika";

    private final SharedPreferences sharedPreferences;

    public FontManager(Context context) {
        sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void setFontType(String fontType){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TYPE,fontType);
        editor.apply();
    }

    public String getFontType(){
        return sharedPreferences.getString(TYPE, FONT_COMIC_SANS);
    }
}
