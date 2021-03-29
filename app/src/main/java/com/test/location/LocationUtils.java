package com.test.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.Date;
import java.util.List;

/**
 * @Author： ZhangYuLiang
 * @description：
 */
public class LocationUtils {
    private volatile static LocationUtils uniqueInstance;
    private LocationManager locationManager;
    List<String> mockProviders;
    private String locationProvider;
    private Location location;
    private Context mContext;
    private String TAG = "ulog";

    private LocationUtils(Context context) {
        mContext = context;
    }

    public static LocationUtils getInstance(Context context) {
        if (uniqueInstance == null) {
            synchronized (LocationUtils.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new LocationUtils(context);
                }
            }
        }
        return uniqueInstance;
    }

    public void initLocation() {
        //1.获取位置管理器
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        //2.获取位置提供器，GPS或是NetWork
        mockProviders = locationManager.getProviders(true);

//        if (mockProviders.contains(LocationManager.GPS_PROVIDER)) {
//            locationProvider = LocationManager.GPS_PROVIDER;
//        } else {
//            Log.e(TAG, "没有gps");
//            return;
//        }

////
        if (mockProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是网络定位
            Log.d(TAG, "如果是网络定位");
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else if (mockProviders.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS定位
            Log.d(TAG, "如果是GPS定位");
            locationProvider = LocationManager.GPS_PROVIDER;
        } else {
            Log.d(TAG, "没有可用的位置提供器");
            return;
        }
        // 需要检查权限,否则编译报错,想抽取成方法都不行,还是会报错。只能这样重复 code 了。
        if (Build.VERSION.SDK_INT >= 23 &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "没有定位权限");
            return;
        }
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "没有定位权限2");
            return;
        }
        //3.获取上次的位置，一般第一次运行，此值为null
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {
            updateLocation(location);
        }
        // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistace
        locationManager.requestLocationUpdates(locationProvider, 5000, 1, locationListener);
        Log.i("ulog","ACCESS_COARSE_LOCATIONll -- "+locationManager.getAllProviders().size());

//        boolean success = getUseMockPosition(mContext);
//        Log.i("ulog", " 初始化-- " + success);
    }

    private void updateLocation(Location location) {
        this.location = location;
        String address = "纬度：" + location.getLatitude() + "经度：" + location.getLongitude();
        Log.d(TAG, address);
    }

//    //获取经纬度
//    public Location showLocation() {
//        return location;
//    }
//
//    // 移除定位监听
//    public void removeLocationUpdatesListener() {
//        // 需要检查权限,否则编译不过
//        if (Build.VERSION.SDK_INT >= 23 &&
//                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        if (locationManager != null) {
//            uniqueInstance = null;
//            locationManager.removeUpdates(locationListener);
//        }
//    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            location.getAccuracy();//精确度
            updateLocation(location);
        }
    };


    public boolean getUseMockPosition(Context context) {
        // Android 6.0以下，通过Setting.Secure.ALLOW_MOCK_LOCATION判断
        // Android 6.0及以上，需要【选择模拟位置信息应用】，未找到方法，因此通过addTestProvider是否可用判断
        boolean canMockPosition = (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0)
                || Build.VERSION.SDK_INT > 22;
        if (!canMockPosition) {
            Log.e(TAG, "没有模拟定位成功");
            return false;
        }
        try {
            LocationProvider provider = locationManager.getProvider(locationProvider);
            if (provider != null) {
                Log.i(TAG, "不是空");
                locationManager.addTestProvider(
                        provider.getName()
                        , provider.requiresNetwork()
                        , provider.requiresSatellite()
                        , provider.requiresCell()
                        , provider.hasMonetaryCost()
                        , provider.supportsAltitude()
                        , provider.supportsSpeed()
                        , provider.supportsBearing()
                        , provider.getPowerRequirement()
                        , provider.getAccuracy());
            } else {
                Log.i(TAG, "新建locationProvider");
                locationManager.addTestProvider(
                        locationProvider
                        , true, true, false, false, true, true, true
                        , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }

            locationManager.setTestProviderEnabled(locationProvider, true);
            locationManager.setTestProviderStatus(locationProvider, LocationProvider.AVAILABLE, null, System.currentTimeMillis());


//            for (String providerStr : mockProviders) {
//                LocationProvider provider = locationManager.getProvider(providerStr);
//                if (provider != null) {
//                    locationManager.addTestProvider(
//                            provider.getName()
//                            , provider.requiresNetwork()
//                            , provider.requiresSatellite()
//                            , provider.requiresCell()
//                            , provider.hasMonetaryCost()
//                            , provider.supportsAltitude()
//                            , provider.supportsSpeed()
//                            , provider.supportsBearing()
//                            , provider.getPowerRequirement()
//                            , provider.getAccuracy());
//                } else {
//                    if (providerStr.equals(LocationManager.GPS_PROVIDER)) {
//                        locationManager.addTestProvider(
//                                providerStr
//                                , true, true, false, false, true, true, true
//                                , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
//                    } else if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
//                        locationManager.addTestProvider(
//                                providerStr
//                                , true, false, true, false, false, false, false
//                                , Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
//                    } else {
//                        locationManager.addTestProvider(
//                                providerStr
//                                , false, false, false, false, true, true, true
//                                , Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
//                    }
//                }
//                locationManager.setTestProviderEnabled(providerStr, true);
//                locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
//
//
//                Location mockLocation = new Location(providerStr);
//                mockLocation.setLatitude(latitude);   // 维度（度）
//                mockLocation.setLongitude(longitude);  // 经度（度）
//                mockLocation.setAccuracy(0.1f);   // 精度（米）
//                mockLocation.setTime(new Date().getTime());   // 本地时间
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                    mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
//                }
//                Log.w("ulog", " setTestProviderLocation--   " + latitude + "  " + longitude);
//                locationManager.setTestProviderLocation(providerStr, mockLocation);
//            }
            canMockPosition = true;
        } catch (SecurityException e) {
            canMockPosition = false;
        }
        return canMockPosition;
    }

    /**
     * 模拟位置线程
     */
    private class RunnableMockLocation implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);

                    Location mockLocation = new Location(locationProvider);
                    mockLocation.setLatitude(latitude);   // 维度（度）
                    mockLocation.setLongitude(longitude);  // 经度（度）
                    mockLocation.setAltitude(30);    // 高程（米）
                    mockLocation.setBearing(180);   // 方向（度）
                    mockLocation.setSpeed(10);    //速度（米/秒）
                    mockLocation.setAccuracy(0.1f);   // 精度（米）
                    mockLocation.setTime(new Date().getTime());   // 本地时间
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    }
                    Log.w("ulog", " setTestProviderLocation--   " + latitude + "  " + longitude);
                    locationManager.setTestProviderLocation(locationProvider, mockLocation);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void start() {
        new Thread(new RunnableMockLocation()).start();
    }

    public double latitude;

    public double longitude;

    public void setLocationData(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

}
