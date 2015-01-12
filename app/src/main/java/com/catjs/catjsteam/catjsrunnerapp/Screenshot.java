package com.catjs.catjsteam.catjsrunnerapp;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ransnir on 12/3/14.
 */
public class Screenshot {

    private View rootView;

    public Screenshot(View rootView) {
        this.rootView = rootView;
    }

    private StringBuilder getResult(HttpResponse response) throws IllegalStateException, IOException {

        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())), 1024);
        String output;
        while ((output = br.readLine()) != null)
            result.append(output);
        return result;
    }


    public void getScreenshot(String serverAddress, Map<String, String> params) {
        String result = "";
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(serverAddress + "/screenshot");
            postRequest.addHeader("accept", "application/json");

            byte[] temp = commitScreenshot();
            String encoded = Base64.encodeToString(temp, Base64.DEFAULT);
            Log.i("CatjsActivity", encoded);

            MultipartEntityBuilder multiPartEntityBuilder = MultipartEntityBuilder
                    .create();


            postRequest.setEntity(multiPartEntityBuilder.build());

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("scrapName", params.get("scrapName")));
            pairs.add(new BasicNameValuePair("deviceId", params.get("deviceId")));
            pairs.add(new BasicNameValuePair("deviceName", "deviceName"));
            pairs.add(new BasicNameValuePair("deviceType", "android"));

            pairs.add(new BasicNameValuePair("pic", encoded));
            postRequest.setEntity(new UrlEncodedFormEntity(pairs));

            HttpResponse response = httpClient.execute(postRequest);
            result = getResult(response).toString();
            httpClient.getConnectionManager().shutdown();


        } catch (UnsupportedEncodingException e) {
            Log.i("CatjsActivity", "UnsupportedEncodingException");
            e.printStackTrace();
        } catch (ClientProtocolException e){
            Log.i("CatjsActivity", "ClientProtocolException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.i("CatjsActivity", "IOException");
            e.printStackTrace();
        }

    }

    private byte[] commitScreenshot() {
        Log.i("CatjsActivity", "take screenshot");

        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return baos.toByteArray();
    }

}
