package com.example.try_buttom_navigation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Network;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Calendar;
import android.text.method.MovementMethod;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public  WifiManager wifiManager;
    public LocationManager locationManager;
    public  ConnectivityManager connectManager;
    public  NetworkInfo netInfo;
    public  WifiInfo wifiInfo;
    public  DhcpInfo dhcpInfo;
    public  String SSID;
    public  List<ScanResult>scanResultList;
    public  double measured_distance = 1;
    public  int navigation_place = 0;

    private double rssiAtOneMeter = -45;
    private double environmental_factor = 4;
    private String wifiList = "";
    private static PermissionListener mListener;
    private Activity activity;
    private String FILE_NAME = "WIFI_LL.txt";
    private String dataToStorage = "";
    private File   myStorageFolder;
    private File   myStorageFile;
    private FileOutputStream fileOutputStream;

    public TextView tv1;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    navigation_place = 0;
                    return true;
                case R.id.navigation_dashboard:
                    navigation_place = 1;
                    return true;
                case R.id.navigation_notifications:
                    navigation_place = 2;
                    return true;
            }
            return false;
        }
    };

    private static String[] PERMISSIONS_STORAGE = {"android.permission.ACCESS_FINE_LOCATION",
                                                   "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        /** buttons *****************************************************************************/
        final Button calibrate_A = findViewById(R.id.recalibrate_A);
        activity = this;
        calibrate_A.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rssiAtOneMeter = wifiInfo.getRssi();
            }
        });

        final Button calibrate_N = findViewById(R.id.recalibrate_N);
        calibrate_N.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                environmental_factor = (Math.abs(wifiInfo.getRssi()) - Math.abs(rssiAtOneMeter)) /
                        ((Math.log10(2)) * 10);
            }
        });

        final Button write2File = findViewById(R.id.record_btn);
        write2File.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    dataToStorage = Calendar.getInstance().getTime().toString() + '\n' +
                                    "===================================================" +
                                    ":  " +'\n' + wifiList +
                                    "===================================================" +
                                    '\n' + '\n' + '\n' + '\n';
                    if(fileOutputStream != null){
                        fileOutputStream.write(dataToStorage.getBytes());
                    }
                    else {
                        Log.e("EXCEPTION", "fileOutputStream is null");
                    }
                }
                catch (IOException e){
                    Log.e("EXCEPTION","file write failed: " + e.toString());
                }
            }
        });


        // dynamically require the permission to use the fine localization
        if (Build.VERSION.SDK_INT >= 23) {
            requestRuntimePermissions(PERMISSIONS_STORAGE, new PermissionListener() {
                @Override
                public void granted() {}

                @Override
                public void denied(List<String> deniedList) {
                    for (String denied : deniedList) {}
                }
            });
        }

        // create the file
        if(isExternalStorageReadable() && isExternalStorageWritable()){
            myStorageFolder = getPublicDocumentStorageDir("storage for wifi test");
            FILE_NAME = Calendar.getInstance().getTime().toString().substring(4,19) + "_.txt";
            myStorageFile = new File(myStorageFolder, FILE_NAME);
            try{
                fileOutputStream = new FileOutputStream(myStorageFile, true);
            }
            catch (FileNotFoundException e){
                Log.e("EXCEPTION","File to get the file: " + e.toString());
            }
        }
        else{
            Log.e("EXCEPTION","file will be null");
        }


        new Thread(){
            @Override
            public void run(){
                tv1 = (TextView)findViewById(R.id.tv1);
                tv1.setMovementMethod(ScrollingMovementMethod.getInstance());

                while(true){
                    SystemClock.sleep(500);
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    wifiManager.startScan();
                    connectManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    netInfo = connectManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    wifiInfo = wifiManager.getConnectionInfo();
                    dhcpInfo = wifiManager.getDhcpInfo();
                    scanResultList =(List<ScanResult>) wifiManager.getScanResults();
                    SSID = wifiInfo.getSSID();

                    measured_distance = ComputeDistance(wifiInfo.getRssi());
                    final String wifiProperty = "WIFI_INFO" + '\n' +
                            "SSID: " + SSID + '\n' +
                            "ipAddress: " + FormatString(dhcpInfo.ipAddress) + '\n' +
                            "mask: " + FormatString(dhcpInfo.netmask) + '\n' +
                            "net gate: " + FormatString(dhcpInfo.gateway) + '\n' +
                            "dns: " + FormatString(dhcpInfo.dns1) + '\n' +
                            "RSSI: " + wifiInfo.getRssi() + '\n' +
                            "BSSID: " + wifiInfo.getBSSID();

                    final String measureDistance ="param A: " + rssiAtOneMeter + '\n' +
                            "param N: " + environmental_factor + '\n' + '\n' +
                            "FORMULA: " + '\n' + "lg(distance) = " + '\n' +
                            "   10 ^ (|RSSI| - |A|)/(10 * N)" + '\n' +
                            "Measured Distance: " + measured_distance + '\n';

                    if(scanResultList.size()==0)
                        wifiList = "empty";
                    else
                        wifiList = FormatList(scanResultList);

                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          switch (navigation_place)
                                          {
                                              case 0:
                                                  tv1.setTextSize(22);
                                                  tv1.setText(wifiProperty);
                                                  calibrate_A.setVisibility(View.GONE);
                                                  calibrate_N.setVisibility(View.GONE);
                                                  write2File.setVisibility(View.GONE);
                                                  break;
                                              case 1:
                                                  tv1.setTextSize(18);
                                                  tv1.setText(measureDistance);
                                                  calibrate_A.setVisibility(View.VISIBLE);
                                                  calibrate_N.setVisibility(View.VISIBLE);
                                                  write2File.setVisibility(View.GONE);
                                                  break;
                                              case 2:
                                                  tv1.setTextSize(10);
                                                  tv1.setText(wifiList);
                                                  calibrate_A.setVisibility(View.GONE);
                                                  calibrate_N.setVisibility(View.GONE);
                                                  write2File.setVisibility(View.VISIBLE);
                                                  break;
                                          }
                                      }
                                  });
                }
            }
        }.start();
//        if(fileOutputStream != null){
//            try{
//                fileOutputStream.close();
//            }
//            catch (IOException e){
//                Log.e("EXCEPTION", "outputStream exception: " + e.toString());
//            }
//        }
//        else {
//            Log.e("EXCEPTION", "fileOutputStream is null");
//        }
    }

    /** transform the string part *******************************************************/
    // integer to byte
    private byte[] toBytes(int i){
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i >> 0);

        return result;
    }
    // extract useful information from the scanResult list.. SSID, MAC, RSSI

    public String FormatList(List<ScanResult> scanResults){
        String string = "";
        ListIterator<ScanResult> listIterator = scanResults.listIterator();
        while(listIterator.hasNext())
        {
            ScanResult scanResult_ = listIterator.next();
            string += "SSID: " + scanResult_.SSID + '\n' +
                      "MAC: " + scanResult_.BSSID + '\n' +
                      "RSSI: " + scanResult_.level + '\n' +
                      "Approximate Distance: " + ComputeDistance(scanResult_.level) + '\n' +
                      '\n';
        }
        return string;
    }
    // apply the formula to compute the distance of the specific wifi

    public double ComputeDistance(int RSSI){
        double distance = Math.pow(10,(double)(Math.abs(RSSI) - Math.abs(rssiAtOneMeter)) /
                            (10 * environmental_factor));
        return distance;
    }
    // format and make the wifi info readable

    public String FormatString(int value){
        String strValue ="";

        byte[] ary = toBytes(value);
        for(int i = ary.length-1;i>0;i--){
            strValue += (ary[i]& 0xFF);
            if(i>0){
                strValue +=".";
            }
        }
//        strValue += (ary[ary.length]& 0xFF);
        strValue += '0';
        return strValue;
    }

    /**  permission part ***************************************************************/
    public interface PermissionListener {

        void granted();
        void denied(List<String> deniedList);
    }

    public static void requestRuntimePermissions(
            String[] permissions, PermissionListener listener) {
        mListener = listener;
        List<String> permissionList = new ArrayList<>();
        // 遍历每一个申请的权限，把没有通过的权限放在集合中
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            } else {
                mListener.granted();
            }
        }
        // 申请权限
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            List<String> deniedList = new ArrayList<>();
            // 遍历所有申请的权限，把被拒绝的权限放入集合
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    mListener.granted();
                } else {
                    deniedList.add(permissions[i]);
                }
            }
            if (!deniedList.isEmpty()) {
                mListener.denied(deniedList);
            }
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getPublicDocumentStorageDir(String documentName) {
        // Get the directory for the user's public pictures directory.
        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), documentName);
        if (!folder.mkdirs()) {
            Log.e(null, "Directory not created");
        }
        return folder;
    }
}
