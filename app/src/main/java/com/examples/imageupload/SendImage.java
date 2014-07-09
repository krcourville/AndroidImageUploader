package com.examples.imageupload;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import org.apache.http.entity.mime.content.FileBody;

import java.io.*;
import java.net.URISyntaxException;
import java.text.*;
import java.util.*;

import static com.drew.metadata.exif.ExifIFD0Directory.*;


public class SendImage extends Activity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final int REQ_CODE_PICK_IMAGE = 2;
    private static Uri photoUri = null;
    private static PostFileParams postFileParams = new PostFileParams();
    private static final String TAG = "SendImage.java";
    private ImageView imgSelectedImage = null;
    private TextView txtUrl = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_image);

        imgSelectedImage = (ImageView) findViewById(R.id.imgSelectedImage);
        txtUrl = (TextView) findViewById(R.id.txtUrl);
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
        postFileParams = new PostFileParams();


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
        imgSelectedImage.setImageBitmap(null);
        new doImageOrientationTask().execute();
        new doImagePreviewTask().execute();
    }


    public void takephotoClick(View view) {
        Log.d(TAG, "takephotoClick()");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        Log.d(TAG, "Photo will be saved to: " + photoUri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    public void pickphotoClick(View view) {
        Log.d(TAG, "pickphotoClick()");

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select a picture"), REQ_CODE_PICK_IMAGE);
    }

    public void submitClick(View view) {
        Log.d(TAG, "submitClick()");

        postFileParams.setTargetUrl(txtUrl.getText().toString());
        PostFileAsyncTask task = new PostFileAsyncTask();

        task.execute(postFileParams);
    }

    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SendImage");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        return new File(
                String.format("%s%s%s", mediaStorageDir.getPath(), File.separator, getMediaFilename(type))
        );
    }

    private static String getMediaFilename(int type) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (type == MEDIA_TYPE_IMAGE) {
            return "IMG_" + timeStamp + ".jpg";
        } else if (type == MEDIA_TYPE_VIDEO) {
            return "VID_" + timeStamp + ".mp4";
        } else {
            return null;
        }
    }


    public class doImageOrientationTask extends AsyncTask<Void, Void, Integer> {
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

    public class doImagePreviewTask extends AsyncTask<Void, Void, Bitmap> {
        private ProgressDialog progressDialog;

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                return doImagePreview();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SendImage.this);
            progressDialog.setMessage("Loading image preview....");
            progressDialog.show();
        }

        protected void onPostExecute(final Bitmap result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
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

        private Bitmap doImagePreview() throws IOException {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);

            int width = imgSelectedImage.getWidth() - 40;
            int height = width + 20;
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] bytes = outputStream.toByteArray();

            imageBitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, bytes.length);
            return imageBitmap;
        }

    }


    public class PostFileAsyncTask extends AsyncTask<PostFileParams, Void, String> {
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(PostFileParams... params) {
            PostFileParams postFileParams = (PostFileParams) params[0];
            postFile(postFileParams);
            return null;
        }

        protected void onPostExecute(String lengthOfFile) {
            super.onPostExecute(lengthOfFile);
        }
    }

    private void postFile(PostFileParams postFileParams) {

//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        postFileParams.getSourceBitmap().compress(
//                Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//        File f = new File(postFileParams.getSourceUri().getPath());
//
//
//        FileBody body = new FileBody(f, postFileParams.getTargetFilename());
//        MultipartEntityBuilder
//
//        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//        entity.addPart("file", body);
//
////        String postReceiverUrl = postFileParams.g //"http://mobile311test.cloudapp.net/Photos/Upload";
//        HttpPost httpPost = new HttpPost(postFileParams.getTargetUrl());
//        httpPost.setEntity(entity);
//        HttpClient httpClient = new DefaultHttpClient();
//        try {
//            HttpResponse response = httpClient.execute(httpPost);
//            String responseStr = EntityUtils.toString(response.getEntity()).trim();
//            Log.d(TAG, "Response text: " + responseStr);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}
