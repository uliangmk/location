package com.test.location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 200;
    private TextView tvLocation, tv_location_times;
    private EditText etLat, etLon;
    private Button btSetLocation, btStart, btEnd;

    private LocationManager locationManager;
    private List<String> mockProviders = new ArrayList<>();

    private StartMockThread thread;
    int count = 0;
    double realLat = 39.8985499;
    double realLon = 116.465185;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestPermission();
    }

    private void initView() {
        tvLocation = findViewById(R.id.tv_location);
        tv_location_times = findViewById(R.id.tv_location_times);
        etLat = findViewById(R.id.et_latitude);
        etLon = findViewById(R.id.et_longitude);
        btSetLocation = findViewById(R.id.bt_set_location);
        btStart = findViewById(R.id.bt_start);
        btEnd = findViewById(R.id.bt_end);
        initListener();
    }

    private void initListener() {
        btSetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initLocation();
            }
        });
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMockLocation();
            }
        });
        btEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMockLocation();
            }
        });
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            Toast.makeText(MainActivity.this, "已开启定位权限", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "已开启定位权限", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "未开启定位权限,请手动到设置去开启权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void initLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 需要检查权限,否则编译报错,想抽取成方法都不行,还是会报错。只能这样重复 code 了。
        if (Build.VERSION.SDK_INT >= 23 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "没有定位权限", Toast.LENGTH_LONG).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "没有定位权限2", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            initPosition();
            initMock();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "去开发者中开权限", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 产生随机数地理位置防止一直一个点
     */
    private void initPosition() {
        int lat = (int) (Math.random() * 900) + 100;
        int lon = (int) (Math.random() * 900) + 100;
        String sLat = "39.898" + lat;
        String sLon = "116.465" + lon;
        double cacheLat = Double.valueOf(sLat);
        double cacheLon = Double.valueOf(sLon);
        realLat = cacheLat;
        realLon = cacheLon;
        updateLocation(realLat, realLon);
    }

    /**
     * 主要遍历两个provider network 和gps
     */
    private void initMock() throws Exception {
        boolean canMockPosition = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0) || Build.VERSION.SDK_INT > 22;
        if (!canMockPosition) {
            Toast.makeText(MainActivity.this, "不可以设置mock", Toast.LENGTH_LONG).show();
            return;
        }
        stopMockLocation();
        mockProviders = locationManager.getProviders(true);
        if (mockProviders.isEmpty()) {
            Log.d("ulog", "mockProviders空");
            return;
        }

        for (String providerStr : mockProviders) {
            if (providerStr.equals(LocationManager.GPS_PROVIDER)) {
                locationManager.addTestProvider(
                        providerStr
                        , true, true, false, false, true, true, true
                        , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);

                locationManager.setTestProviderEnabled(providerStr, true);
                locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());

            } else if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
                locationManager.addTestProvider(
                        providerStr
                        , true, false, true, false, false, false, false
                        , Criteria.POWER_LOW, Criteria.ACCURACY_FINE);

                locationManager.setTestProviderEnabled(providerStr, true);
                locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            }
        }
    }

    private void startMockLocation() {
        if (thread != null) {
            return;
        }
        thread = new StartMockThread();
        thread.start();
    }

    public void stopMockLocation() {
        if (thread != null) {
            thread.canWork = false;
            thread = null;
        }
        mockProviders = locationManager.getProviders(true);
        for (String provider : mockProviders) {
            try {
                if (!provider.equals(LocationManager.GPS_PROVIDER) && !provider.equals(LocationManager.NETWORK_PROVIDER)) {
                    continue;
                }
                locationManager.removeTestProvider(provider);
            } catch (Exception e) {
                Log.e("ulog", "2 -- " + e);
            }
        }
    }

    /**
     * 需要不断的刷新位置信息，否则有可能被覆盖
     */
    public class StartMockThread extends Thread {
        public boolean canWork = true;

        public void run() {
            while (true) {
                try {
                    Thread.sleep(500);
                    if (!canWork) {
                        return;
                    }
                    if (locationManager == null || locationManager.getProviders(true) == null || locationManager.getProviders(true).isEmpty()) {
                        continue;
                    }
                    mockProviders = locationManager.getProviders(true);
                    for (String providerStr : mockProviders) {
                        if (!providerStr.equals(LocationManager.GPS_PROVIDER) && !providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
                            continue;
                        }
                        Location mockLocation = new Location(providerStr);
                        mockLocation.setLatitude(realLat);   // 维度（度）
                        mockLocation.setLongitude(realLon);  // 经度（度）
                        mockLocation.setAltitude(30);    // 高程（米）
                        mockLocation.setBearing(180);   // 方向（度）
                        mockLocation.setSpeed(0.1f);    //速度（米/秒）
                        mockLocation.setAccuracy(0.1f);   // 精度（米）
                        mockLocation.setTime(new Date().getTime());   // 本地时间
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        }
                        locationManager.setTestProviderLocation(providerStr, mockLocation);
                    }
                    updateLocationTimes();
                } catch (Exception e) {
                    Log.e("ulog", "1 -- " + e);
                }
            }
        }
    }

    private void updateLocation(double latitude, double longitude) {
        tvLocation.post(new Runnable() {
            @Override
            public void run() {
                tvLocation.setText("经度：" + latitude + "  纬度  " + longitude + "  " + "\n" + getLocationAddress(latitude, longitude));
            }
        });
    }

    private void updateLocationTimes() {
        tv_location_times.post(new Runnable() {
            @Override
            public void run() {
                tv_location_times.setText(++count + "");
            }
        });
    }

    private String getLocationAddress(double latitude, double longitude) {
        String add = "";
        Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.CHINESE);
        try {
            List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);
            Address address = addresses.get(0);
            int maxLine = address.getMaxAddressLineIndex();
            if (maxLine >= 2) {
                add = address.getAddressLine(0) + address.getAddressLine(1);
            } else {
                add = address.getAddressLine(0);
            }
        } catch (IOException e) {
            add = "";
            e.printStackTrace();
        }
        return add;
    }
}