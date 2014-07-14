package com.examples.imageupload;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by kcourville on 7/10/2014.
 */
public class FileUploader {

    private InputStream sourceStream;
    private String targetUrl;
    private String httpMethod = "POST";
    private String targetFileName;

    private int httpResponseCode;
    private String serverResponseMessage;

    private Map<String, List<String>> responseHeaders;

    public FileUploader(InputStream inputStream, String url, String filename) {
        this.sourceStream = inputStream;
        this.targetUrl = url;
        this.targetFileName = filename;
    }

    public void upload() throws IOException {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        URL url = new URL(targetUrl);
        connection = (HttpURLConnection) url.openConnection();

        // Allow Inputs &amp; Outputs.
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        // Set HTTP method to POST.
        connection.setRequestMethod(httpMethod);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + targetFileName + "\"" + lineEnd);
        outputStream.writeBytes(lineEnd);


        bytesAvailable = sourceStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        // Read file
        bytesRead = sourceStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = sourceStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = sourceStream.read(buffer, 0, bufferSize);
        }

        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        // Responses from the server (code and message)
        httpResponseCode = connection.getResponseCode();
        serverResponseMessage = connection.getResponseMessage();
        responseHeaders = connection.getHeaderFields();
        outputStream.flush();
        outputStream.close();
    }


    public InputStream getSourceStream() {
        return sourceStream;
    }

    public void setSourceStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public String getServerResponseMessage() {
        return serverResponseMessage;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }
}
