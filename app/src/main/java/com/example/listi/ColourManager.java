package com.example.listi;

import android.content.Context;
import android.content.SharedPreferences;

public class ColourManager {
    private static final String NAME = "colourPreference";
    private static final String TYPE = "colour_type";
    public static final String COLOUR_1 = "1";
    public static final String COLOUR_2 = "2";

    private final SharedPreferences sharedPreferences;

    public ColourManager(Context context) {
        sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void setColourType(String colourType){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TYPE,colourType);
        editor.apply();
    }

    public String getColourType(){
        return sharedPreferences.getString(TYPE, COLOUR_1);
    }
}
