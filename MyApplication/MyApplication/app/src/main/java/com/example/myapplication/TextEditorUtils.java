package com.example.myapplication;

import android.widget.EditText;
import android.widget.Toast;
import android.content.Context;

public class TextEditorUtils {

    public static void changeFontSize(Context context, EditText editText, int size) {
        if (size > 0) {
            editText.setTextSize(size);
        } else {
            Toast.makeText(context, "Font size must be positive", Toast.LENGTH_SHORT).show();
        }
    }
}