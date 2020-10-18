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
package com.didi.wstt.gt.activity;

import android.Manifest;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.didi.wstt.gt.GTApp;
import com.didi.wstt.gt.R;
import com.didi.wstt.gt.api.utils.Env;
import com.mezzsy.commonlib.util.PermissionUtils;

public class GTMainActivity extends GTBaseFragmentActivity implements OnClickListener {
    private static final String TAG = "GTX";
    // Android6.x之后，需要由用户明确授权的权限，放在MainActivity里提前做申请交互
    private static final int REQUEST_NEED_PERMISSION = 101;
    // 悬浮窗的权限是特殊权限，需要单独处理
    private static final int REQUEST_FLOAT_VIEW = 102;
    //    private static boolean isFloatViewAllowed = GTPref.getGTPref().getBoolean(GTPref.FLOAT_ALLOWED, false);
    private boolean isFloatViewAllowed = false;

    // 页面碎片对象
    private GTAUTFragment1 autFragment;//AUT
    private GTParamTopFragment paramFragment;//参数
    private GTPerfFragment perfFragment;//耗时
    private GTLogFragment logFragment;//日志
    private GTPluginFragment pluginFragment;//插件

    // 页面布局
    private View autLayout;
    private View paramLayout;
    private View perfLayout;
    private View logLayout;
    private View pluginLayout;

    // 在Tab布局上显示的图标的控件
    private ImageView autImage;
    private ImageView paramImage;
    private ImageView perfImage;
    private ImageView logImage;
    private ImageView pluginImage;

    private TextView autText;
    private TextView paramText;
    private TextView perfText;
    private TextView logText;
    private TextView pluginText;

    public static boolean isActived = false;
    public static Notification notification;
    public static boolean dlgIsShow = false;

    private int curTabId;

    private static GTMainActivity instance;

    public static GTMainActivity getInstance() {
        return instance;
    }

    public GTMainActivity() {
        instance = this;
    }

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,//读写权限
            Manifest.permission.ACCESS_FINE_LOCATION,//允许一个程序访问精良位置(如GPS)
            Manifest.permission.READ_PHONE_STATE,//允许程序访问电话状态
//            Manifest.permission.SYSTEM_ALERT_WINDOW,//允许程序显示系统窗口，这个权限系统默认禁止，不在跟随批量申请
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gt_activity_main);
        // 初始化布局元素
        initViews();

        // 重新初始化各页
        removeFragments();

        // 第一次启动时选中第0个tab
        if (savedInstanceState != null) {
            setTabSelection(savedInstanceState.getInt("curTabId"));
        } else {
            setTabSelection(0);
        }
        isActived = true;

        boolean isNeedRequest = PermissionUtils.requestPermissions(this,
                PERMISSIONS, REQUEST_NEED_PERMISSION);
        isFloatViewAllowed = PermissionUtils.checkFloatPermission(this);
        Log.i(TAG, "onCreate: isFloatViewAllowed = " + isFloatViewAllowed);
        if (!isNeedRequest && !isFloatViewAllowed) {
            PermissionUtils.requestAlertWindowPermission(this, REQUEST_FLOAT_VIEW);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_NEED_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 授权了就可以保存了，do nothing即可
                    // 接收了危险权限后，再弹出悬浮窗处理特殊权限，
                    // 这样还可以避过ACTION_MANAGE_OVERLAY_PERMISSION不支持API23之前的问题
                    if (!isFloatViewAllowed) {
                        PermissionUtils.requestAlertWindowPermission(this, REQUEST_FLOAT_VIEW);
                    }

                    // 在授权后，需要将之前没权限创建的目录重新创建一次
                    if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Env.init();
                    }

                } else {
                    Log.i(TAG, "onRequestPermissionsResult: PERMISSION_DENIED : " + permissions[0]);
//                    ToastUtil.ShowLongToast(GTApp.getContext(),
//                            "Permission not enough. Please consider granting it this permission.");
                }
                break;
        }
    }

    /* 进行验证码验证，或者进行快速登录的回调，这两个是进行快速登录可能遇到的情况 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_FLOAT_VIEW:
                // 得到权限，置标志位，相应的提示用户重启GT等

                isFloatViewAllowed = PermissionUtils.checkFloatPermission(this);
                Log.i(TAG, "onActivityResult: isFloatViewAllowed = " + isFloatViewAllowed);

                // 因为授权之前的启动悬浮窗会报异常关闭，所以这里重新启动悬浮窗服务
//			if ( GTPref.getGTPref().getBoolean(GTPref.AC_SWITCH_FLAG, true))
//			{
//				Intent intent = new Intent(this, GTLogo.class);
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startService(intent);

//				Intent mintent = new Intent(this, GTFloatView.class);
//				mintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startService(mintent);
//			}

                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActived = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("curTabId", curTabId);
        super.onSaveInstanceState(savedInstanceState);
    }

    /*
     * 在Activity重新加载时，要先清除遗留的Fragment
     */
    private void removeFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment fragment = fragmentManager.findFragmentByTag("a");
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = fragmentManager.findFragmentByTag("b");
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = fragmentManager.findFragmentByTag("c");
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = fragmentManager.findFragmentByTag("d");
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = fragmentManager.findFragmentByTag("e");
        if (fragment != null) {
            transaction.remove(fragment);
        }
        transaction.commitAllowingStateLoss();
    }

    /**
     * 在这里获取到每个需要用到的控件的实例，并给它们设置好必要的点击事件。
     */
    private void initViews() {
        autLayout = findViewById(R.id.aut_layout);
        paramLayout = findViewById(R.id.param_layout);
        perfLayout = findViewById(R.id.perf_layout);
        logLayout = findViewById(R.id.log_layout);
        pluginLayout = findViewById(R.id.plugin_layout);

        autImage = findViewById(R.id.aut_image);
        paramImage = findViewById(R.id.param_image);
        perfImage = findViewById(R.id.perf_image);
        logImage = findViewById(R.id.log_image);
        pluginImage = findViewById(R.id.plugin_image);

        autText = findViewById(R.id.aut_text);
        paramText = findViewById(R.id.param_text);
        perfText = findViewById(R.id.perf_text);
        logText = findViewById(R.id.log_text);
        pluginText = findViewById(R.id.plugin_text);

        autLayout.setOnClickListener(this);
        paramLayout.setOnClickListener(this);
        perfLayout.setOnClickListener(this);
        logLayout.setOnClickListener(this);
        pluginLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.aut_layout:
                curTabId = 0;
                setTabSelection(0);
                break;
            case R.id.param_layout:
                curTabId = 1;
                setTabSelection(1);
                break;
            case R.id.perf_layout:
                curTabId = 2;
                setTabSelection(2);
                break;
            case R.id.log_layout:
                curTabId = 3;
                setTabSelection(3);
                break;
            case R.id.plugin_layout:
                curTabId = 4;
                setTabSelection(4);
                break;
            default:
                break;
        }
    }

    /**
     * 根据传入的index参数来设置选中的tab页。
     *
     * @param index 每个tab页对应的下标。
     */
    private synchronized void setTabSelection(int index) {
        // 每次选中之前先清楚掉上次的选中状态
        clearSelection();
        // 开启一个Fragment事务
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(transaction);

        switch (index) {
            case 0:
                // 当点击了AUT tab时，改变控件的图片和文字颜色
                autImage.setImageResource(R.drawable.tab_selected_border);
                autText.setTextColor(Color.WHITE);
                if (autFragment == null) {
                    autFragment = new GTAUTFragment1();
                    transaction.add(R.id.content, autFragment, "a");
                } else {
                    transaction.show(autFragment);
                }
                break;
            case 1:
                // 当点击了参数 tab时，改变控件的图片和文字颜色
                paramImage.setImageResource(R.drawable.tab_selected_border);
                paramText.setTextColor(Color.WHITE);
                if (paramFragment == null) {
                    paramFragment = new GTParamTopFragment();
                    transaction.add(R.id.content, paramFragment, "b");
                } else {
                    transaction.show(paramFragment);
                    paramFragment.onShow(true);
                }
                break;
            case 2:
                // 当点击了耗时 tab时，改变控件的图片和文字颜色
                perfImage.setImageResource(R.drawable.tab_selected_border);
                perfText.setTextColor(Color.WHITE);
                if (perfFragment == null) {
                    perfFragment = new GTPerfFragment();
                    transaction.add(R.id.content, perfFragment, "c");
                } else {
                    transaction.show(perfFragment);
                }
                break;
            case 3:
                // 当点击了日志 tab时，改变控件的图片和文字颜色
                logImage.setImageResource(R.drawable.tab_selected_border);
                logText.setTextColor(Color.WHITE);
                if (logFragment == null) {
                    logFragment = new GTLogFragment();
                    transaction.add(R.id.content, logFragment, "d");
                } else {
                    transaction.show(logFragment);
                }
                break;
            case 4:
            default:
                // 当点击了插件 tab时，改变控件的图片和文字颜色
                pluginImage.setImageResource(R.drawable.tab_selected_border);
                pluginText.setTextColor(Color.WHITE);
                if (pluginFragment == null) {
                    pluginFragment = new GTPluginFragment();
                    transaction.add(R.id.content, pluginFragment, "e");
                } else {
                    transaction.show(pluginFragment);
                }
                break;
        }
        /*
         * 直接使用commit()可能会出错：
         * IllegalStateException: Can not perform this action after onSaveInstanceState：\
         *
         * @see http://developer.android.com/reference/android/app/FragmentTransaction.html#commitAllowingStateLoss()
         *
         * 大致意思是说我使用的 commit方法是在Activity的onSaveInstanceState()之后调用的，这样会出错，因为onSaveInstanceState
         * 方法是在该Activity即将被销毁前调用，来保存Activity数据的，如果在保存玩状态后再给它添加Fragment就会出错。解决办法就
         * 是把commit（）方法替换成 commitAllowingStateLoss()就行了，其效果是一样的。
         */
        // transaction.commit();
        transaction.commitAllowingStateLoss();
    }

    /**
     * 清除掉所有的选中状态。
     */
    private void clearSelection() {
        int defaultColor = getResources().getColor(R.color.tab_default_textcolor, null);
        autImage.setImageResource(R.drawable.tab_default_border);
        autText.setTextColor(defaultColor);
        paramImage.setImageResource(R.drawable.tab_default_border);
        paramText.setTextColor(defaultColor);
        perfImage.setImageResource(R.drawable.tab_default_border);
        perfText.setTextColor(defaultColor);
        logImage.setImageResource(R.drawable.tab_default_border);
        logText.setTextColor(defaultColor);
        pluginImage.setImageResource(R.drawable.tab_default_border);
        pluginText.setTextColor(defaultColor);
    }

    /**
     * 将所有的Fragment都置为隐藏状态。
     *
     * @param transaction 用于对Fragment执行操作的事务
     */
    private void hideFragments(FragmentTransaction transaction) {
        if (autFragment != null) {
            transaction.hide(autFragment);
        }
        if (paramFragment != null) {
            paramFragment.onShow(false);
            transaction.hide(paramFragment);
        }
        if (perfFragment != null) {
            transaction.hide(perfFragment);
        }
        if (logFragment != null) {
            transaction.hide(logFragment);
        }
        if (pluginFragment != null) {
            transaction.hide(pluginFragment);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dlgIsShow = false;

            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        Intent intent = null;

        switch (item_id) {
            case R.id.exit:
                GTApp.exitGT();
                break;
            case R.id.log_switch:
                intent = new Intent(this, GTLogSwitchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.air_console:
                intent = new Intent(this, GTACSettingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.intervals:
                intent = new Intent(this, GTIntervalSettingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.about:
                intent = new Intent(this, GTAboutActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
        }

        return false;
    }
}
