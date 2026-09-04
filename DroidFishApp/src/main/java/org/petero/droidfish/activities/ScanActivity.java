package org.petero.droidfish.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.petero.droidfish.R;

import java.io.File;
import java.io.InputStream;

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure no layout is set since we want it transparent
        // setContentView(R.layout.scan_activity);
        
        showChoiceDialog();
    }

    private void showChoiceDialog() {
        String[] options = {"Pick Image", "Open Camera"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan Chess Position")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImage();
                    } else if (which == 1) {
                        openCamera();
                    }
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = new File(getCacheDir(), "scan_camera_img.jpg");
                cameraImageUri = FileProvider.getUriForFile(this, "org.petero.droidfish.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Could not create file for camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                processImageUri(data.getData());
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && cameraImageUri != null) {
                processImageUri(cameraImageUri);
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void processImageUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                bitmap = rotateBitmapIfRequired(uri, bitmap);
                new ScanTask(bitmap).execute();
            } else {
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Bitmap rotateBitmapIfRequired(Uri uri, Bitmap bitmap) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            if (is != null) {
                androidx.exifinterface.media.ExifInterface exif = new androidx.exifinterface.media.ExifInterface(is);
                int orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
                int rotation = 0;
                switch (orientation) {
                    case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                        break;
                }
                if (rotation != 0) {
                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.postRotate(rotation);
                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    return rotated;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
        return bitmap;
    }

    private class ScanTask extends AsyncTask<Void, Void, String> {
        private Bitmap bitmap;
        private ProgressDialog dialog;

        public ScanTask(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(ScanActivity.this, "Scanning", "Running local YOLOv8 inference...", true);
            dialog.setOnCancelListener(d -> finish());
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                ScannerInference inference = new ScannerInference(ScanActivity.this);
                return inference.scanBoard(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (result == null || result.startsWith("ERROR")) {
                Toast.makeText(ScanActivity.this, "Scan failed: " + result, Toast.LENGTH_LONG).show();
                finish();
            } else if (result.equals("8/8/8/8/8/8/8/8 w - - 0 1")) {
                Toast.makeText(ScanActivity.this, "No chess pieces detected. Please ensure the board is clearly visible.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(ScanActivity.this, "Scan successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ScanActivity.this, EditBoard.class);
                intent.setAction(result);
                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(intent);
                finish();
            }
        }
    }
}
