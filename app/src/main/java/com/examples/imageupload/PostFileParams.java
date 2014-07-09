package com.examples.imageupload;

import android.graphics.Bitmap;

/**
 * Created by kcourville on 7/8/2014.
 */
public class PostFileParams {
    private Bitmap sourceBitmap;
    private String targetUrl;
    private String targetFilename;


    public String getTargetFilename() {
        return targetFilename;
    }

    public void setTargetFilename(String targetFilename) {
        this.targetFilename = targetFilename;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Bitmap getSourceBitmap() {
        return sourceBitmap;
    }

    public void setSourceBitmap(Bitmap sourceBitmap) {
        this.sourceBitmap = sourceBitmap;
    }
}
