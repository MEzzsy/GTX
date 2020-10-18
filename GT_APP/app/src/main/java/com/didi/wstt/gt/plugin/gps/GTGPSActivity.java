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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.didi.wstt.gt.R;
import com.didi.wstt.gt.activity.GTBaseActivity;
import com.didi.wstt.gt.api.utils.Env;
import com.didi.wstt.gt.plugin.PluginManager;
import com.didi.wstt.gt.utils.ToastUtil;

import java.io.File;
import java.util.ArrayList;

public class GTGPSActivity extends GTBaseActivity implements GPSRecordListener, GPSReplayListener {
    public static final String TAG = "GTGPSActivity";
    private static final String MOCK_GPS_PROVIDER_INDEX = "GpsMockProviderIndex";
    public static final int RES_GPSREPALY_ACTIVITY = 200;

    private TextView back_gt;
    private TextView tv_record;
    private TextView tv_replay;
    private TextView tv_refresh;
    private TextView tv_clear;
    private ListView lv_gpsFile;
    private ArrayAdapter<String> mAdapter;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 通知UI刷新
            mAdapter.clear();
            mAdapter.addAll(GTGPSUtils.getGPSFileList());
            mAdapter.notifyDataSetChanged();
        }
    };

    private static int mMockGpsProviderIndex = 0;

    private View.OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.record:
                    onClickRecord();
                    break;
                case R.id.replay:
                    onClickReplay();
                    break;
                case R.id.refresh:
                    onClickRefresh();
                    break;
                case R.id.clear:
                    onClickClear();
                    break;
                case R.id.frame_back_gt:
                    finish();
                    break;
            }
        }
    };

    /**
     * 点击清空按钮
     */
    private void onClickClear() {
        boolean res = GTGPSUtils.clearGPSFiles();
        handler.sendEmptyMessage(0);
        if (res) {
            ToastUtil.ShowShortToast(this, "清空成功");
        } else {
            ToastUtil.ShowShortToast(this, "清空失败");
        }
    }

    /**
     * 点击刷新按钮
     */
    private void onClickRefresh() {
        handler.sendEmptyMessage(0);
        ToastUtil.ShowShortToast(this, "刷新成功");
    }

    /**
     * 点击回放按钮
     */
    private void onClickReplay() {
        if (!GTGPSReplayEngine.getInstance().isReplay()) {
            Intent intent = new Intent();

            intent.putExtra("seq",
                    GTGPSReplayEngine.getInstance().selectedItemPos);
            PluginManager.getInstance().getPluginControler()
                    .startService(GTGPSReplayEngine.getInstance(), intent);
        } else {
            PluginManager.getInstance().getPluginControler()
                    .stopService(GTGPSReplayEngine.getInstance());
        }
    }

    /**
     * 点击录制按钮
     */
    private void onClickRecord() {
        if (!GTGPSRecordEngine.getInstance().isRecord()) {
            PluginManager.getInstance().getPluginControler()
                    .startService(GTGPSRecordEngine.getInstance());
        } else {
            PluginManager.getInstance().getPluginControler()
                    .stopService(GTGPSRecordEngine.getInstance());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pi_gps);

        // 恢复因GT crash而中断的模拟位置点
        if (savedInstanceState != null) {
            mMockGpsProviderIndex = savedInstanceState.getInt(
                    MOCK_GPS_PROVIDER_INDEX, 0);
        }

        back_gt = findViewById(R.id.frame_back_gt);
        back_gt.setOnClickListener(mOnClickListener);

        tv_record = findViewById(R.id.record);
        tv_replay = findViewById(R.id.replay);
        tv_refresh = findViewById(R.id.refresh);
        tv_clear = findViewById(R.id.clear);

        tv_record.setOnClickListener(mOnClickListener);
        tv_replay.setOnClickListener(mOnClickListener);
        tv_refresh.setOnClickListener(mOnClickListener);
        tv_clear.setOnClickListener(mOnClickListener);

        lv_gpsFile = findViewById(R.id.gpslist);
        initListView();

        if (GTGPSRecordEngine.getInstance().isRecord()) {
            tv_record.setBackgroundResource(R.drawable.switch_off_border);
            tv_record.setText(getString(R.string.pi_gps_record_stop));
        } else {
            tv_record.setBackgroundResource(R.drawable.switch_on_border);
            tv_record.setText(getString(R.string.pi_gps_record));

        }

        if (GTGPSReplayEngine.getInstance().isReplay()) {
            tv_replay.setBackgroundResource(R.drawable.switch_off_border);
            tv_replay.setText(getString(R.string.pi_gps_replay_stop));
        } else {
            tv_replay.setBackgroundResource(R.drawable.switch_on_border);
            tv_replay.setText(getString(R.string.pi_gps_replay));

        }
        if (GTGPSReplayEngine.getInstance().selectedItemPos >= 0) {
            lv_gpsFile
                    .setSelection(GTGPSReplayEngine.getInstance().selectedItemPos);
            lv_gpsFile.setItemChecked(GTGPSReplayEngine.getInstance().selectedItemPos, true);
            Object o = lv_gpsFile.getItemAtPosition(GTGPSReplayEngine
                    .getInstance().selectedItemPos);
            GTGPSReplayEngine.getInstance().selectedItem = o.toString();
        }

        GTGPSRecordEngine.getInstance().addListener(this);
        GTGPSReplayEngine.getInstance().addListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(MOCK_GPS_PROVIDER_INDEX,
                mMockGpsProviderIndex);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.sendEmptyMessage(0);
    }

    @Override
    protected void onDestroy() {
        GTGPSRecordEngine.getInstance().removeListener(this);
        GTGPSReplayEngine.getInstance().removeListener(this);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onRecordStart() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_record.setBackgroundResource(R.drawable.switch_off_border);
                tv_record.setText(getString(R.string.pi_gps_record_stop));
            }
        });
    }

    @Override
    public void onRecordStop() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_record.setBackgroundResource(R.drawable.switch_on_border);
                tv_record.setText(getString(R.string.pi_gps_record));
                // 更新UI
                handler.sendEmptyMessage(0);
            }
        });
    }

    @Override
    public void onRecordFail(final String errorstr) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.ShowLongToast(GTGPSActivity.this, errorstr);
            }
        });
    }

    @Override
    public void onReplayStart() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_replay.setBackgroundResource(R.drawable.switch_off_border);
                tv_replay.setText(getString(R.string.pi_gps_replay_stop));
            }
        });
    }

    @Override
    public void onReplayStop() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_replay.setBackgroundResource(R.drawable.switch_on_border);
                tv_replay.setText(getString(R.string.pi_gps_replay));
            }
        });
    }

    @Override
    public void onReplayEnd() {

    }

    @Override
    public void onReplayFail(final String errorstr) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.ShowLongToast(GTGPSActivity.this, errorstr);
            }
        });
    }

    private void initListView() {
        lv_gpsFile.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice,
                new ArrayList<String>());
        mAdapter.addAll(GTGPSUtils.getGPSFileList());
        lv_gpsFile.setAdapter(mAdapter);
        lv_gpsFile.setOnItemClickListener(listViewOnItemClickListener);
        lv_gpsFile.setOnItemLongClickListener(mOnItemLongClickListener);
    }

    private OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            // 如果是回放过程，禁止做文件操作
            if (GTGPSReplayEngine.getInstance().isReplay()) {
                ToastUtil.ShowShortToast(GTGPSActivity.this,
                        getString(R.string.pi_gps_warn_tip3));
                return true;
            }

            GTGPSReplayEngine.getInstance().selectedItemPos = position;
            String fName = parent.getItemAtPosition(position).toString();
            final File selectedFile = new File(Env.ROOT_GPS_FOLDER, fName);
            // 弹出对话框，询问是否删除
            if (selectedFile.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(GTGPSActivity.this);
                builder.setTitle(getString(R.string.operate_tip));
                builder.setItems(new String[]{getString(R.string.delete), getString(R.string.rename)},
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: {
                                        boolean deleteRes = selectedFile.delete();
                                        if (!deleteRes) {
                                            return;
                                        }
                                        GTGPSReplayEngine.getInstance().selectedItemPos =
                                                GTGPSReplayEngine.SELECTED_NULL_ITEM;
                                        handler.sendEmptyMessage(0);
                                    }
                                    break;
                                    case 1: {
                                        final EditText rename = new EditText(getApplicationContext());
                                        new AlertDialog.Builder(GTGPSActivity.this)
                                                .setTitle(getString(R.string.please_enter))
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setView(rename)
                                                .setPositiveButton(getString(R.string.ok),
                                                        new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                //noinspection ResultOfMethodCallIgnored
                                                                selectedFile.renameTo(new File(Env.ROOT_GPS_FOLDER, rename.getText().toString() + ".gps"));
                                                                handler.sendEmptyMessage(0);
                                                            }
                                                        })
                                                .show();
                                    }
                                    break;

                                    default:
                                        break;
                                }

                            }
                        });
                builder.show();
            }

            return true;
        }
    };

    private OnItemClickListener listViewOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapter, View v, int position,
                                long id) {

            if (!GTGPSReplayEngine.getInstance().isReplay()) {
                Object o = adapter.getItemAtPosition(position);
                GTGPSReplayEngine.getInstance().selectedItem = o.toString();
                GTGPSReplayEngine.getInstance().selectedItemPos = position;
            } else if (GTGPSReplayEngine.getInstance().selectedItemPos == position) {

                Intent intent = new Intent();
                intent.putExtra("gpsname",
                        GTGPSReplayEngine.getInstance().selectedItem);
                intent.putExtra("gpspercent", GTGPSReplayEngine.getInstance()
                        .getPercentage());
                intent.putExtra("relpayspeed", GTGPSReplayEngine.getInstance()
                        .getReplaySpeed());

                intent.setClass(GTGPSActivity.this, GTGPSReplayActivity.class);
                startActivityForResult(intent, RES_GPSREPALY_ACTIVITY);
                // startActivity(intent);
            }

        }

    };

    /**
     * 接收到上一页传递过来的保存信息
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RES_GPSREPALY_ACTIVITY: {
                /* 结束之前回放的service */
                PluginManager.getInstance().getPluginControler().stopService(GTGPSReplayEngine.getInstance());

                /* 启动新的回放service */
                Intent intent = new Intent();
                intent.putExtra("seq", GTGPSReplayEngine.getInstance().selectedItemPos);
                intent.putExtra("progress", data.getIntExtra("progress", 0));
                intent.putExtra("replayspeed", data.getIntExtra("replayspeed", 0));
                PluginManager.getInstance().getPluginControler().startService(GTGPSReplayEngine.getInstance(), intent);

                Toast.makeText(getApplicationContext(), "has GPS replayed on： " + data.getIntExtra("progress", 0) + "%", Toast.LENGTH_SHORT).show();
            }
            break;
            case RESULT_OK: {
                super.onActivityResult(requestCode, resultCode, data);
            }
            break;
            default:
                break;
        }
    }
}
