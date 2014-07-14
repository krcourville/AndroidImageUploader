package com.examples.imageupload;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;


import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static com.drew.metadata.exif.ExifIFD0Directory.*;


public class SendImage extends Activity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    private static final int REQ_CODE_PICK_IMAGE = 2;
    private static Uri photoUri = null;
    private static final String TAG = "SendImage.java";
    private ImageView imgSelectedImage = null;
    private TextView txtUrl = null;
    private TextView txtPostedImageUrl = null;
    private TextView txtHttpResponse = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_image);

        imgSelectedImage = (ImageView) findViewById(R.id.imgSelectedImage);
        txtUrl = (TextView) findViewById(R.id.txtUrl);
        txtPostedImageUrl = (TextView) findViewById(R.id.txtPostedImageUrl);
        txtHttpResponse = (TextView) findViewById(R.id.txtHttpResponse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.send_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {

            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // photoUri will be set already
                } else if (resultCode == RESULT_CANCELED) {
                    return;
                } else {
                    Log.e(TAG, "Failed to take photo");
                    return;
                }
                break;

            case REQ_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    photoUri = data.getData();
                    Log.d(TAG, "Selected image sourceUri : " + photoUri.toString());
                } else if (resultCode == RESULT_CANCELED) {
                    return;
                } else {
                    Log.e(TAG, "Failed to pick photo");
                    return;
                }
                break;
        }
        new DoImageOrientationTask().execute();
        new DoImagePreviewTask().execute();
    }

    public void postedImageUrlClick(View view) {
        if (txtPostedImageUrl.getText() == null) return;

        Intent i = new Intent(Intent.ACTION_VIEW);
        String url = txtPostedImageUrl.getText().toString();
        i.setDataAndType(Uri.parse(url),"image/*");

        startActivity(i);
    }


    public void takephotoClick(View view) {
        Log.d(TAG, "takephotoClick()");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoUri = ImageUtils.getOutputMediaFileUri(ImageUtils.MEDIA_TYPE_IMAGE);
        Log.d(TAG, "Photo will be saved to: " + photoUri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    public void pickphotoClick(View view) {
        Log.d(TAG, "pickphotoClick()");

        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(i, "Pick a photo"), REQ_CODE_PICK_IMAGE);
    }

    public void submitClick(View view) {
        Log.d(TAG, "submitClick()");


        new PostImageAsyncTask().execute();
    }


    public class DoImageOrientationTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog progressDialog;

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                return getImageRotation();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SendImage.this);
            progressDialog.setMessage("Orientating image....");
            progressDialog.show();
        }

        protected void onPostExecute(final Integer result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgSelectedImage.setRotation(result);
                }
            });
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        private int getImageRotation() throws IOException, URISyntaxException, ImageProcessingException, MetadataException {
            int rotate = 0;
            int orientation = getOrientation();
            Log.d(TAG, "Image orientation : " + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    Log.d(TAG, "Image orientation was undefined");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            Log.d(TAG, "Image rotate : " + rotate);
            return rotate;
        }

        private int getOrientation() throws IOException, ImageProcessingException, MetadataException {
            int result = 0;
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            Metadata metadata = ImageMetadataReader.readMetadata(bufferedInputStream, false);

            Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
            if (directory != null) {
                if (directory.containsTag(TAG_ORIENTATION)) {
                    result = directory.getInt(TAG_ORIENTATION);
                }
            }
            Log.d(TAG, "Image orientation : " + result);
            return result;
        }
    }

    public class DoImagePreviewTask extends AsyncTask<Void, Void, Bitmap> {
        private ProgressDialog progressDialog;

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                return ImageUtils.getThumbnailFromUri(getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPreExecute() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgSelectedImage.setImageBitmap(null);
                }
            });
            progressDialog = new ProgressDialog(SendImage.this);
            progressDialog.setMessage("Loading image preview....");
            progressDialog.show();
        }

        protected void onPostExecute(final Bitmap result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgSelectedImage.setMaxHeight(result.getHeight());
                    imgSelectedImage.setImageBitmap(result);
                }
            });
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (result == null) {
                Toast.makeText(getApplicationContext(), "Failed to load image preview", Toast.LENGTH_LONG).show();
            }
        }


    }


    public class PostImageAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        private String responseText = null;
        private String responseUrl = null;

        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SendImage.this);
            progressDialog.setMessage("Uploading image...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return postImage();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        protected void onPostExecute(String lengthOfFile) {
            super.onPostExecute(lengthOfFile);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtPostedImageUrl.setText(responseUrl);
                    txtHttpResponse.setText(responseText);
                }
            });

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        private String postImage() throws IOException {
            Log.i(TAG, "Source photo uri: " + photoUri.toString());

            String url = txtUrl.getText().toString();
            Log.i(TAG, "Target url: " + url);

            String filename = ImageUtils.getMediaFilename(ImageUtils.MEDIA_TYPE_IMAGE);
            Log.i(TAG, "Posting to filename: " + filename);

            Bitmap bitmap = ImageUtils.decodeUri(getApplicationContext(), photoUri, 436);
            Log.d(TAG, "Image size is :" + bitmap.getByteCount());

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());

            FileUploader uploader = new FileUploader(inputStream, url, filename);
            uploader.upload();

            Map<String, List<String>> headers = uploader.getResponseHeaders();
            this.responseUrl = null;
            if (headers.containsKey("Location")) {
                this.responseUrl = headers.get("Location").get(0);
            }
            this.responseText = uploader.getServerResponseMessage();

            return null;
        }
    }


}
