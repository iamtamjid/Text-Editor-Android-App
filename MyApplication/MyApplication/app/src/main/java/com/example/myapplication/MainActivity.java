


package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import java.util.Stack;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int CREATE_FILE_REQUEST_CODE = 1 ;
    private static final int OPEN_FILE_REQUEST_CODE = 2 ;
    private EditText editText;
    private Stack<String> undoStack;
    String originalText = "";
    private TextView fileNameTextView;
    private Uri currentFileUri;
    boolean undoState = false;

    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<Intent> openFileLauncher;
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private boolean isLoadingFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editTextTextMultiLine);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PackageManager.PERMISSION_GRANTED
        );


        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            currentFileUri = uri; // Set the URI to the newly created file

                            // Extract the actual filename from the URI and update the UI
                            String fileName = getFileName(uri);
                            fileNameTextView.setText("File Name: " + fileName);

                            // Save the current text to the new file
                            saveTextToUri(uri);
                        }
                    }
                }
        );
        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            currentFileUri = uri; // Set the URI to the opened file
                            loadTextFromUri(uri);
                        }
                    }
                }
        );

        fileNameTextView = findViewById(R.id.fileNameTextView);


        editText.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                // You can identify which key was pressed by checking keyCode value with
                // KeyEvent.KEYCODE
                if (i == KeyEvent.KEYCODE_DEL){
                    // This is for backspace
                    // If undoState is false, get the EditText content and store in originalText.
                    if(undoState == false){
                        originalText = editText.getText().toString().trim();
                        // Also, change the flag undoState to true so that we can store
                        // the original text one time.
                        undoState = true;
                    }
                }
                return false;
            }
        });

        Spinner fontSizeOptions = findViewById(R.id.font_size_options);
        initializeFontSizeSpinner(fontSizeOptions);

        editText.addTextChangedListener(new TextWatcher() {
            private int start;
            private int count;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                this.start = start;
                this.count = count;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isLoadingFile) return; // Skip styling during file loading

                if (count > 0) {
                    applyStylesToNewText(s, start, start + count);
                }
            }

            private void applyStylesToNewText(Editable s, int start, int end) {
                if (isBold) {
                    s.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (isItalic) {
                    s.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (isUnderline) {
                    s.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });


    }



    private void initializeFontSizeSpinner(Spinner fontSizeOptions) {
        fontSizeOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    int fontSize = Integer.parseInt(parent.getItemAtPosition(position).toString());
                    TextEditorUtils.changeFontSize(MainActivity.this, editText, fontSize);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid font size", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    public void undo(View view) {
        // When undo button is pressed set the EditText with originalText.
        editText.setText(originalText);
    }
    public void openColorPalette(View view) {
        // Define a list of colors you want to display in the palette
        final int[] colors = new int[] {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };
        // Create an AlertDialog with buttons representing each color
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a Color");

        builder.setItems(new CharSequence[] {
                "Red", "Green", "Blue", "Yellow", "Magenta", "Cyan"
        }, (dialog, which) -> {
            // Apply the selected color to the selected text in the EditText
            Spannable spannableString = new SpannableStringBuilder(editText.getText());
            spannableString.setSpan(new ForegroundColorSpan(colors[which]),
                    editText.getSelectionStart(),
                    editText.getSelectionEnd(),
                    0);
            editText.setText(spannableString);
        });

        // Show the color picker dialog
        builder.show();
    }

    public void openBGColorPalette(View view) {
        // Define a list of colors you want to display in the palette
        final int[] colors = new int[] {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };
        // Create an AlertDialog with buttons representing each color
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a Color");

        builder.setItems(new CharSequence[] {
                "Red", "Green", "Blue", "Yellow", "Magenta", "Cyan"
        }, (dialog, which) -> {
            // Apply the selected color to the selected text in the EditText
            Spannable spannableString = new SpannableStringBuilder(editText.getText());
            spannableString.setSpan(new BackgroundColorSpan(colors[which]),
                    editText.getSelectionStart(),
                    editText.getSelectionEnd(),
                    0);
            editText.setText(spannableString);
        });

        // Show the color picker dialog
        builder.show();
    }


    private String getFileName(Uri uri) {
        String fileName = "Unknown";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.getScheme().equals("file")) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public void buttonSaveFile(View view) {
        if (currentFileUri != null) {
            // Save to the currently opened file
            saveTextToUri(currentFileUri);
        } else {
            // If no file is opened, create a new file
            String uniqueFileName = "file_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("text/plain"); // Set file type as plain text
            intent.putExtra(Intent.EXTRA_TITLE, uniqueFileName); // Default file name
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Check if the file already exists in the same location
            Uri defaultUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aexample.txt");
            if (doesFileExist(defaultUri)) {
                // File exists, ask the user whether to overwrite
                new AlertDialog.Builder(this)
                        .setTitle("File Exists")
                        .setMessage("A file with the same name already exists. Do you want to overwrite it?")
                        .setPositiveButton("Overwrite", (dialog, which) -> {
                            createFileLauncher.launch(intent); // Proceed to create the file
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss(); // Cancel the operation
                        })
                        .show();
            } else {
                // No file with the same name, proceed to create a new one
                createFileLauncher.launch(intent);
            }
        }
    }

    private boolean doesFileExist(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    private void saveTextToUri(Uri uri) {
//        try {
//            // Convert the styled text in EditText to HTML
//            String htmlText = Html.toHtml(editText.getText(), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
//
//            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
//                if (outputStream != null) {
//                    outputStream.write(htmlText.getBytes());
//                    Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(this, "Failed to access file", Toast.LENGTH_SHORT).show();
//                }
//            }
//        } catch (Exception e) {
//            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }

    private void saveTextToUri(Uri uri) {
        try {
            // Convert the styled text in EditText to HTML
            String htmlText = Html.toHtml(editText.getText(), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);

            // Include font size as a `<span style="font-size:Xpx;">` wrapper
            float fontSize = editText.getTextSize(); // Get the current font size in pixels
            String styledHtml = "<div style=\"font-size:" + fontSize + "px;\">" + htmlText + "</div>";

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(styledHtml.getBytes());
                    Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to access file", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void resetEditTextStyles() {
        // Reset alignment to default (left)
        editText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        // Reset text to plain (removes formatting)
        editText.setText("");
        // Reset any additional settings as needed
    }


    public void buttonCreateFile(View view) {
        resetEditTextStyles(); // Reset styles for a fresh start

        // Generate a unique filename using a timestamp
        String uniqueFileName = "file_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";

        // Update the displayed filename
        fileNameTextView.setText("File Name: " + uniqueFileName);

        // Reset the current file URI since we're starting a new file
        currentFileUri = null;
        editText.setHint("Start typing..."); // Optional: Add a hint
    }

        public void buttonOpenFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain"); // File type
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        openFileLauncher.launch(intent);
    }


    private void loadTextFromUri(Uri uri) {
        try {
            isLoadingFile = true; // Start loading

            // Reset styles before loading new content
            resetEditTextStyles();

            // Display the file name
            String fileName = getFileName(uri);
            fileNameTextView.setText("Opened: " + fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();

                String htmlContent = stringBuilder.toString();

                // Extract font size from the HTML
                String fontSizePattern = "font-size:([0-9.]+)px;";
                Pattern pattern = Pattern.compile(fontSizePattern);
                Matcher matcher = pattern.matcher(htmlContent);
                if (matcher.find()) {
                    float fontSize = Float.parseFloat(matcher.group(1));
                    editText.setTextSize(fontSize / getResources().getDisplayMetrics().scaledDensity); // Convert pixels to sp
                }

                // Convert HTML content back to Spanned text for the EditText
                Spanned spannedText = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT);
                editText.setText(spannedText);

                Toast.makeText(this, "File loaded successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            isLoadingFile = false; // End loading
        }
    }

//    private void loadTextFromUri(Uri uri) {
//        try {
//            // Reset styles before loading new content
//            resetEditTextStyles();
//
//            // Display the file name
//            String fileName = getFileName(uri);
//            fileNameTextView.setText("Opened: " + fileName);
//
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//            if (inputStream != null) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                StringBuilder stringBuilder = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    stringBuilder.append(line).append("\n");
//                }
//                reader.close();
//
//                // Convert HTML content back to Spanned text for the EditText
//                Spanned spannedText = Html.fromHtml(stringBuilder.toString(), Html.FROM_HTML_MODE_COMPACT);
//                editText.setText(spannedText);
//
//                Toast.makeText(this, "File loaded successfully", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }

//    public void buttonBold(View view) {
//        Spannable spannableString = new SpannableStringBuilder(editText.getText());
//        spannableString.setSpan(new StyleSpan(Typeface.BOLD),
//                editText.getSelectionStart(),
//                editText.getSelectionEnd(),
//                0);
//
//        editText.setText(spannableString);
//    }
//
//    public void buttonItalics(View view) {
//        Spannable spannableString = new SpannableStringBuilder(editText.getText());
//        spannableString.setSpan(new StyleSpan(Typeface.ITALIC),
//                editText.getSelectionStart(),
//                editText.getSelectionEnd(),
//                0);
//
//        editText.setText(spannableString);
//
//    }
//
//    public void buttonUnderline(View view) {
//        Spannable spannableString = new SpannableStringBuilder(editText.getText());
//        spannableString.setSpan(new UnderlineSpan(),
//                editText.getSelectionStart(),
//                editText.getSelectionEnd(),
//                0);
//
//        editText.setText(spannableString);
//    }
//
//    public void buttonNoFormat(View view) {
//        String stringText = editText.getText().toString();
//        editText.setText(stringText);
//    }

    public void buttonBold(View view) {
        isBold = !isBold; // Toggle bold mode
        Toast.makeText(this, isBold ? "Bold ON" : "Bold OFF", Toast.LENGTH_SHORT).show();
    }

    public void buttonItalics(View view) {
        isItalic = !isItalic; // Toggle italic mode
        Toast.makeText(this, isItalic ? "Italic ON" : "Italic OFF", Toast.LENGTH_SHORT).show();
    }

    public void buttonUnderline(View view) {
        isUnderline = !isUnderline; // Toggle underline mode
        Toast.makeText(this, isUnderline ? "Underline ON" : "Underline OFF", Toast.LENGTH_SHORT).show();
    }

    public void buttonNoFormat(View view) {
        // Reset all formatting flags
        isBold = false;
        isItalic = false;
        isUnderline = false;
        Toast.makeText(this, "Formatting OFF", Toast.LENGTH_SHORT).show();
    }

    public void buttonAlignmentLeft(View view) {
        editText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        Spannable spannableString = new SpannableStringBuilder(editText.getText());
        editText.setText(spannableString);
    }

    public void buttonAlignmentCenter(View view) {
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        Spannable spannableString = new SpannableStringBuilder(editText.getText());
        editText.setText(spannableString);
    }

    public void buttonAlignmentRight(View view) {
        editText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        Spannable spannableString = new SpannableStringBuilder(editText.getText());
        editText.setText(spannableString);
    }
}

