/*
 * Tencent is pleased to support the open source community by making
 * Tencent GT (Version 2.4 and subsequent versions) available.
 *
 * Notwithstanding anything to the contrary herein, any previous version
 * of Tencent GT shall not be subject to the license hereunder.
 * All right, title, and interest, including all intellectual property rights,
 * in and to the previous version of Tencent GT (including any and all copies thereof)
 * shall be owned and retained by Tencent and subject to the license under the
 * Tencent GT End User License Agreement (http://gt.qq.com/wp-content/EULA_EN.html).
 *
 * Copyright (C) 2015 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.didi.wstt.gt.plugin.gps;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.didi.wstt.gt.GTApp;
import com.didi.wstt.gt.R;
import com.didi.wstt.gt.api.utils.Env;
import com.didi.wstt.gt.plugin.BaseService;
import com.didi.wstt.gt.utils.FileUtil;
import com.didi.wstt.gt.utils.ToastUtil;
import com.mezzsy.commonlib.tool.mocklocation.MockLocationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * GPS回放引擎
 */
public class GTGPSReplayEngine extends BaseService {
    private static final String TAG = "GTGPSReplayEngine";
    /**
     * 没有选择item
     */
    public static final int SELECTED_NULL_ITEM = -1;
    private MockGpsProvider mMockGpsProviderTask = null;
    private MockLocationManager mMockLocationManager;

    private List<GPSReplayListener> listeners;
    private volatile boolean isReplay = false;
    public String selectedItem;
    public int selectedItemPos = SELECTED_NULL_ITEM;

    /*回放gps文件的总长度*/
    public int mGPSFileLength = 0;
    /*回放gps文件的当前进度*/
    public int index = 0;
    /*回放速率*/
    public int mReplaySpeed;

    public static GTGPSReplayEngine getInstance() {
        return SingletonHolder.INSTANCE.mEngine;
    }

    private enum SingletonHolder {
        INSTANCE;

        SingletonHolder() {
        }

        private GTGPSReplayEngine mEngine;
    }

    private GTGPSReplayEngine() {
        listeners = new ArrayList<>();
    }

    public synchronized void addListener(GPSReplayListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(GPSReplayListener listener) {
        listeners.remove(listener);
    }

    public boolean isReplay() {
        return isReplay;
    }

    @Override
    public void onCreate(Context context) {
        super.onCreate(context);
        mMockLocationManager = new MockLocationManager(context,
                MockLocationManager.MOCK_PROVIDER_NAME);
        mMockLocationManager.addTestProvider(
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                0,
                5);
        mMockLocationManager.setTestProviderEnabled(true);
        Log.i(TAG, "onCreate: ");
    }

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        if (null == intent) {
            return;
        }

        selectedItemPos = intent.getIntExtra("seq", -1);
        int progress = Math.min(100, intent.getIntExtra("progress", 0));
        mReplaySpeed = intent.getIntExtra("replayspeed", 1);
        index = getGPSFileLength() * progress / 100;
        // 先找好对应的文件名，再按文件名回放
        ArrayList<String> items = GTGPSUtils.getGPSFileList();
        if (items.size() > 0 && items.size() > selectedItemPos && !items.get(0).equals("empty")) {
            replay(items.get(selectedItemPos));
        }
        Log.i(TAG, "onStart: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopReplay(); // 停止回放
        Log.i(TAG, "onDestroy: ");
    }

    @Override
    public IBinder onBind() {
        return null;
    }

    private boolean isAllowMock() {
//        if (!locationManager.isProviderEnabled(MockGpsProvider.GPS_MOCK_PROVIDER)) {
//            try {
//                locationManager.addTestProvider(MockGpsProvider.GPS_MOCK_PROVIDER,
//                        false, false, false, false, true, false, false, 0, 5);
//                locationManager.setTestProviderEnabled(
//                        MockGpsProvider.GPS_MOCK_PROVIDER, true);
//            } catch (SecurityException e) {
//                // 如果之前未在开发者选项中手动打开“允许模拟GPS”，这里可能会抛安全异常
//                for (GPSReplayListener listener : listeners) {
//                    listener.onReplayFail(
//                            GTApp.getContext().getString(R.string.pi_gps_warn_tip));
//                }
//                return false;
//            }
//        }
        return true;
    }

    /*
     * 模拟轨迹
     */
    private void replay(String fileName) {
        if (!isAllowMock()) {
            return;
        }

        BufferedReader reader = null;
        try {
            List<String> data = new ArrayList<String>();

            File f = new File(Env.ROOT_GPS_FOLDER, fileName);
            InputStream is = new FileInputStream(f);
            reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while ((line = reader.readLine()) != null) {
                data.add(line);
            }
            if (data.size() == 0) {
                // 通知观察者
                for (GPSReplayListener listener : listeners) {
                    listener.onReplayFail(
                            GTApp.getContext().getString(R.string.pi_gps_warn_tip2));
                }
                return;
            }
            mGPSFileLength = data.size();
            // 为了使用AsyncTask需要转普通数组
            String[] coordinates = new String[mGPSFileLength];
            data.toArray(coordinates);

            if (mMockGpsProviderTask != null) {
                mMockGpsProviderTask.cancel(true);
            }
            mMockGpsProviderTask = new MockGpsProvider(selectedItem);
            isReplay = true;
            mMockGpsProviderTask.execute(coordinates);
        } catch (Exception e) {
            e.printStackTrace();
            isReplay = false;
        } finally {
            FileUtil.closeReader(reader);
        }
    }

    private void stopReplay() {
        isReplay = false;
        stopMockLocation();
    }

    /**
     * add on 20140630
     * 退出应用前也需要调用停止模拟位置，否则手机的正常GPS定位不会恢复
     */
    public void stopMockLocation() {
        try {
            mMockGpsProviderTask.cancel(true);
            mMockGpsProviderTask = null;
        } catch (Exception e) {
        }

        mMockLocationManager.removeTestProvider();
    }

    /**
     * 得带当前GPS回放到了百分之多少
     */
    public double getPercentage() {
        if (mGPSFileLength != 0 && index != 0) {
            return ((double) index / (double) mGPSFileLength);
        }
        return 0.0;
    }

    /**
     * 得带当前GPS回放速率
     */
    public int getReplaySpeed() {
        return mReplaySpeed;
    }

    /**
     * 得带当前GPS回放文件的总数
     */
    public int getGPSFileLength() {
        return mGPSFileLength;
    }

    //TODO static
    private class MockGpsProvider extends AsyncTask<String, Integer, Void> {
        public static final String LOG_TAG = "GpsMockProvider";
        public static final String GPS_MOCK_PROVIDER = LocationManager.GPS_PROVIDER;
        public String orgiFileName;

        public MockGpsProvider(String fileName) {
            this.orgiFileName = fileName;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }

        @Override
        protected Void doInBackground(String... data) {

            boolean hasMockEnd = false;

            double nowtimeStamp;

            for (GPSReplayListener listener : listeners) {
                listener.onReplayStart();
            }

            /*
             * 修改为保持最后1点的位置
             */
            while (true) {
                if (!isReplay()) {
                    break;
                }
                String str = data[index];// 先获取当前点，下一句就index就切到下一个了
                if (index < data.length - 1) // 到最后一点就不加序号了
                {
                    if (index + mReplaySpeed > data.length - 1) {
                        index++;
                    } else {
                        index += mReplaySpeed;
                    }

                }
                // add on 20150108 到最后一点立即发出广播通知测试程序回放逻辑已结束
                else if (!hasMockEnd) {
                    hasMockEnd = true;

                    for (GPSReplayListener listener : listeners) {
                        listener.onReplayEnd();
                    }
                }

                Location location = new Location(GPS_MOCK_PROVIDER);

                try {
                    String[] parts = str.split(",");
                    nowtimeStamp = Double.parseDouble(parts[6]);
                    location.setTime(System.currentTimeMillis());
                    location.setLatitude(Double.parseDouble(parts[1]));
                    location.setLongitude(Double.parseDouble(parts[0]));
                    location.setAccuracy((Float.parseFloat(parts[2])));
                    location.setAltitude(Double.parseDouble(parts[7]));
                    location.setBearing(Float.parseFloat(parts[3]));
                    location.setSpeed(Float.parseFloat(parts[4]));
                    location.setElapsedRealtimeNanos(System.nanoTime());
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

                // 提供新的位置信息
                try {
                    mMockLocationManager.setTestProviderLocation(location);
                    Log.i(LOG_TAG, location.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果未开位置模拟，这里可能出异常
                    ToastUtil.ShowLongToast(GTApp.getContext(),
                            GTApp.getContext().getString(R.string.pi_gps_warn_tip));
                    break;
                }

                // 如果是最后一点，默认停2s
                if (index == data.length - 1) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                } else {
                    // 如果是最后一点，间隔默认按2s算
                    int interval = 2000;
                    try {
                        if (index < data.length) {
                            String next = data[index];
                            String[] parts = next.split(",");
                            interval = (int) ((Double.valueOf(parts[6]) - nowtimeStamp) * 1000);

                            if (interval <= 0) // 针对复制的数据，时间间隔没错开的情况的保护
                            {
                                interval = 2000;
                            }
                        }
                        Log.i("interval", String.valueOf(interval));
                        if (mReplaySpeed == 1) {
                            Thread.sleep(interval);
                        } else {
                            Thread.sleep(1000);
                        }
                        if (Thread.currentThread().isInterrupted())
                            throw new InterruptedException("");
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            index = 0;
            for (GPSReplayListener listener : listeners) {
                listener.onReplayStop();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(LOG_TAG, "onProgressUpdate():" + values[0]);
        }
    }
}
