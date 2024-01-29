package com.droidev.imagetopdf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Image to PDF");

        handleIncomingIntent();

        Button pickImageButton = findViewById(R.id.abrirImagem);
        pickImageButton.setOnClickListener(v -> pickImage());
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("image/*".equals(type)) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Bitmap imageBitmap = getBitmapFromUri(imageUri);
                    if (imageBitmap != null) {
                        nameFile(imageBitmap);
                    }
                }
            }
        }
    }

    private void pickImage() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                Bitmap imageBitmap = getBitmapFromUri(imageUri);
                if (imageBitmap != null) {
                    nameFile(imageBitmap);
                }
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void convertImageToPdf(Bitmap imageBitmap, String fileName) {
        String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File file = new File(pdfPath, fileName + ".pdf");

        try {
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdfDocument = new PdfDocument(writer);

            pdfDocument.setDefaultPageSize(PageSize.A4);

            Document document = new Document(pdfDocument);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

            byte[] bitmapData = stream.toByteArray();
            ImageData imageData = ImageDataFactory.create(bitmapData);

            Image image = new Image(imageData);
            document.add(image);
            document.close();

            sharePdf(Uri.fromFile(file));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting image to PDF", e);
        }
    }


    private void sharePdf(Uri pdfUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");

        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(Objects.requireNonNull(pdfUri.getPath())));

        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF using"));
    }


    public void nameFile(Bitmap imageBitmap) {

        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setMaxLines(1);

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(editText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("File Name")
                .setPositiveButton("Share", null)
                .setNegativeButton("Cancel", null)
                .setView(lay)
                .show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        positiveButton.setOnClickListener(v -> {

            String fileName = removeSpecialCharacters(editText.getText().toString());

            if (fileName.equals("")) {

                convertImageToPdf(imageBitmap, "ImageToPDF");

                dialog.dismiss();

            } else {

                convertImageToPdf(imageBitmap, fileName);

                dialog.dismiss();
            }
        });
    }

    public static String removeSpecialCharacters(String input) {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");

        return pattern.matcher(input).replaceAll("");
    }

}