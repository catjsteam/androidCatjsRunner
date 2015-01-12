package com.catjs.catjsteam.catjsrunnerapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class CatjsActivity extends Activity {

    private WebView catWebView;
    private String webSiteAdd;
//    private String webSiteAdd = "http://192.168.0.102:8089";

    private DeviceInfo deviceInfo = new DeviceInfo();
    private Screenshot screenshot;


    public enum SupportAPIS {
        CATJSGETSCREENSHOT,
        CATJSDEVICEINFO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_catjs);

        // set strict mode for requests
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        screenshot = new Screenshot(findViewById(R.id.catWebView).getRootView());
//        deviceInfo.getDeviceInfo();
        // set web view property
        setWebViewSettings();


        // set web client
        webClientManager();

        loadCatjsUrl();
//        loadCatjsUrlSimple();

    }


    private Map<String, String> getParams(String paramsString) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        String[] paramsArray = paramsString.split("&");
        for (int i =0; i < paramsArray.length; i++) {
            String[] paramPair = paramsArray[i].split("=");
            paramsMap.put(paramPair[0], paramPair[1]);

        }

        return paramsMap;

    }

    private void webClientManager() {

        catWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                Log.i("CatjsActivity", "test url " + url);
                boolean checkcatjs = url.startsWith("catjs");

                Log.i("CatjsActivity", "checkcatjs : " + checkcatjs);


                if (checkcatjs) {

                    String[] urlArray =  url.split("://");

                    if (urlArray.length >= 2) {
                        String temp = urlArray[0].toUpperCase();
                        SupportAPIS schema = SupportAPIS.valueOf(temp);
                        Map<String, String> params = getParams(urlArray[1]);

                        switch (schema) {
                            case CATJSGETSCREENSHOT:
                                screenshot.getScreenshot(webSiteAdd, params);
                                break;
                            case CATJSDEVICEINFO :
                                deviceInfo.getDeviceInfo(webSiteAdd, params);
                                break;
                            default:
                                break;
                        }
                    }
                }
                return true;
            }

        });
    }





    private void loadCatjsUrl() {
        Bundle extras = this.getIntent().getExtras();
        if ( extras != null ) {
            if (extras.containsKey("catserveraddress")) {
                webSiteAdd =  extras.getString("catserveraddress");
                catWebView.loadUrl(webSiteAdd);

            } else {
//                Log.d("FOO", "no foo here");
            }
        }
    }

    private void loadCatjsUrlSimple() {
        catWebView.loadUrl(webSiteAdd);
    }


    private void setWebViewSettings() {
        catWebView = (WebView) findViewById(R.id.catWebView);
        catWebView.clearCache(true);
        catWebView.clearHistory();
        WebSettings webSettings = catWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        catWebView.setWebViewClient(new WebViewClient());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_catjs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    public class DeviceInfo {

        public static final int IDLE = 5;
        public static final int USER = 2;
        public static final int NICE = 3;
        public static final int SYSTEM = 4;
        public static final int IOWAIT = 6;
        public static final int IRQ = 7;
        public static final int SOFTIRQ = 8;
        public static final int APP_CPU = 13;
        public static final int APP_IDLE = 14;
        public String serverAddress;
        public Map<String, String> params;


        class MyTimerTask extends TimerTask {
            public void run() {
                Log.i("CatjsActivity", "Timer task executed.");
                float cpuUsage = readUsage();

                long totalCpu = getTotalCpu();

                double[] memoryParams = memoryManager();
                buildApi();
                getBatteryPercentage();

                String availableInternalMemorySize = getAvailableInternalMemorySize();
                String totalInternalMemorySize = getTotalInternalMemorySize();
                String availableExternalMemorySize = getAvailableExternalMemorySize();
                String totalExternalMemorySize = getTotalExternalMemorySize();

                String result = "";
                try {

                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpPost postRequest = new HttpPost(serverAddress + "/deviceinfo");
                    postRequest.addHeader("accept", "application/json");


                    MultipartEntityBuilder multiPartEntityBuilder = MultipartEntityBuilder
                            .create();


                    postRequest.setEntity(multiPartEntityBuilder.build());

                    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                    pairs.add(new BasicNameValuePair("scrapName", params.get("scrapName")));
                    pairs.add(new BasicNameValuePair("deviceId", params.get("deviceId")));
                    pairs.add(new BasicNameValuePair("deviceName", "deviceName"));
                    pairs.add(new BasicNameValuePair("deviceType", "android"));
                    pairs.add(new BasicNameValuePair("availableInternalMemorySize", availableInternalMemorySize));
                    pairs.add(new BasicNameValuePair("totalInternalMemorySize", totalInternalMemorySize));
                    pairs.add(new BasicNameValuePair("availableExternalMemorySize", availableExternalMemorySize));
                    pairs.add(new BasicNameValuePair("totalExternalMemorySize", totalExternalMemorySize));
                    pairs.add(new BasicNameValuePair("totalCpu", String.valueOf(totalCpu)));
                    pairs.add(new BasicNameValuePair("cpuUsage", String.valueOf(cpuUsage)));
                    pairs.add(new BasicNameValuePair("totalMem", String.valueOf(memoryParams[0])));
                    pairs.add(new BasicNameValuePair("availableMegs", String.valueOf(memoryParams[1])));
                    pairs.add(new BasicNameValuePair("percentAvail", String.valueOf(memoryParams[2])));


                    postRequest.setEntity(new UrlEncodedFormEntity(pairs));

                    HttpResponse response = httpClient.execute(postRequest);
//                result = getResult(response).toString();
                    httpClient.getConnectionManager().shutdown();


                } catch (UnsupportedEncodingException e) {
                    Log.i("CatjsActivity", "UnsupportedEncodingException");
                    e.printStackTrace();
//                } catch (ClientProtocolException e){
//                    Log.i("CatjsActivity", "ClientProtocolException");
//                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("CatjsActivity", "IOException");
                    e.printStackTrace();
                }


                Log.i("CatjsActivity", "End interval");

            }
        }

        public void getDeviceInfo(String serverAddress, Map<String, String> params) {
            this.serverAddress = serverAddress;
            this.params = params;
            TimerTask tasknew = new MyTimerTask();
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(tasknew,500,1000);


            return;
        }

        private double[] memoryManager() {

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            double availableMegs = mi.availMem / (1024*1024);
            double totalMem = mi.totalMem / (1024*1024);
//Percentage can be calculated for API 16+
            double percentAvail = (double)(availableMegs / totalMem);

            return new double[]{totalMem, availableMegs, percentAvail};
        }

        private void getBatteryPercentage() {
            BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    context.unregisterReceiver(this);
                    int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int level = -1;
                    if (currentLevel >= 0 && scale > 0) {
                        level = (currentLevel * 100) / scale;
                    }
                    Log.i("CatAct" , ("Battery Level Remaining: " + level + "%"));
                }
            };
            IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        }



        private long getTotalCpu() {
            try {
                RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
                String loadTotalCpu = reader.readLine();
                String[] toks = loadTotalCpu.split(" ");
                long totalIdleBefore = Long.parseLong(toks[IDLE]);
                return Long.parseLong(toks[USER]) + Long.parseLong(toks[NICE]) + Long.parseLong(toks[SYSTEM])
                        + Long.parseLong(toks[IOWAIT]) + Long.parseLong(toks[IRQ]) + Long.parseLong(toks[SOFTIRQ]);
            } catch (Exception e) {
                return 0;
            }
        }


        private void buildApi(){
            StringBuffer sb = new StringBuffer();
            sb.append("abi: ").append(Build.CPU_ABI).append("\n");
            if (new File("/proc/cpuinfo").exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                    String aLine;
                    while ((aLine = br.readLine()) != null) {
                        sb.append(aLine + "\n");
                    }
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String temp = sb.toString();
            Log.i("CatjsActivity", temp);


        }

        private float readUsage() {
            try {
                RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
                String load = reader.readLine();

                String[] toks = load.split(" ");

                long idle1 = Long.parseLong(toks[4]);
                long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

                try {
                    Thread.sleep(360);
                } catch (Exception e) {}

                reader.seek(0);
                load = reader.readLine();
                reader.close();

                toks = load.split(" ");

                long idle2 = Long.parseLong(toks[4]);
                long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

                return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

            } catch (IOException ex) {
                ex.printStackTrace();
            }




            return 0;
        }


        public boolean externalMemoryAvailable() {
            return android.os.Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED);
        }

        public String getAvailableInternalMemorySize() {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return formatSize(availableBlocks * blockSize);
        }

        public String getTotalInternalMemorySize() {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return formatSize(totalBlocks * blockSize);
        }

        public String getAvailableExternalMemorySize() {
            if (externalMemoryAvailable()) {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long availableBlocks = stat.getAvailableBlocks();
                return formatSize(availableBlocks * blockSize);
            } else {
                return "0";
            }
        }

        public String getTotalExternalMemorySize() {
            if (externalMemoryAvailable()) {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                return formatSize(totalBlocks * blockSize);
            } else {
                return "0";
            }
        }

        public String formatSize(long size) {
            String suffix = null;

            if (size >= 1024) {
                suffix = "KB";
                size /= 1024;
                if (size >= 1024) {
                    suffix = "MB";
                    size /= 1024;
                }
            }

            StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

            int commaOffset = resultBuffer.length() - 3;
            while (commaOffset > 0) {
                resultBuffer.insert(commaOffset, ',');
                commaOffset -= 3;
            }

            if (suffix != null) resultBuffer.append(suffix);
            return resultBuffer.toString();
        }

    }



}

