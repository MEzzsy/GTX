package com.didi.wstt.gt.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.didi.wstt.gt.R;
import com.didi.wstt.gt.analysis4.GTRDataToJsManager;
import com.didi.wstt.gt.api.utils.Env;
import com.didi.wstt.gt.utils.ToastUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by p_gumingcai on 2017/8/1.
 */

public class ShowFileListActivity extends GTBaseActivity {
    //存储文件名称
    private ArrayList<String> names = null;
    //存储文件路径
    private ArrayList<String> paths = null;

    private String originalPath = Env.GTR_PATH;
    private String desPath = Env.GTR_DATA_PATH;
    private ListView lv_Showfile;
    private ProgressDialog progressDialog;
    private static final int COPYSUCCESS = 1;
    private static final int COPYFAILED = 2;

    private String pullType = "";
    private int CONTENT_LENGTH_LIMIT = 10485760;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case COPYSUCCESS:
                    ToastUtil.ShowLongToast(ShowFileListActivity.this,
                            "导出成功", "center");
                    dismissProgress();
                    break;
                case COPYFAILED:
                    ToastUtil.ShowLongToast(ShowFileListActivity.this,
                            "请选择需要导出带有测试数据的文件夹", "center");
                    dismissProgress();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gtr_activity_showfile);
        lv_Showfile = findViewById(R.id.lv_showfile);

        // 获取导出方式
        pullType = getIntent().getStringExtra("pullType");

        showFileDir(originalPath);
    }

    private void showFileDir(String path) {
        names = new ArrayList<>();
        paths = new ArrayList<>();
        File file = new File(path);
        File[] files = file.listFiles();
        //添加所有文件
        for (File f : files) {
            names.add(f.getName());
            paths.add(f.getPath());
        }
        lv_Showfile.setAdapter(new MyAdapter(this, names, paths));
        lv_Showfile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                switch (pullType) {
                    case "local":
                        new AlertDialog.Builder(ShowFileListActivity.this)
                                .setMessage(R.string.pi_file_alert)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, int which) {
                                        displayProgress();
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                boolean success = false;
                                                try {
                                                    Log.d("adam", "开始解析数据 ");
                                                    long startTime = System.currentTimeMillis();
                                                    Boolean b = GTRDataToJsManager.toAnalysis(paths.get(position), false);
                                                    if (b) {
                                                        Log.d("adam", "解析数据完成，耗时 = " + (System.currentTimeMillis() - startTime) + "ms");
                                                        success = true;
                                                    } else {
                                                        ToastUtil.ShowShortToast(ShowFileListActivity.this, "Can not to analysis");
                                                    }
                                                } catch (Exception e) {
                                                    Message message = new Message();
                                                    message.what = COPYFAILED;
                                                    mHandler.handleMessage(message);
                                                    success = false;
                                                    e.printStackTrace();
                                                }
                                                if (success) {
                                                    Message message = new Message();
                                                    message.what = COPYSUCCESS;
                                                    mHandler.handleMessage(message);
                                                }
                                            }
                                        }).start();
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                        break;
                    default:
                        Toast.makeText(ShowFileListActivity.this, "The pullType is not found",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void displayProgress() {
        displayProgress(Env.GTR_DATA_DEFAULT_MESSAGE);
    }

    private void displayProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.getWindow().addFlags(Window.FEATURE_NO_TITLE);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(message);
        }

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private static class MyAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        //存储文件名称
        private ArrayList<String> names = null;
        //存储文件路径
        private ArrayList<String> paths = null;

        //参数初始化
        public MyAdapter(Context context, ArrayList<String> na, ArrayList<String> pa) {
            names = na;
            paths = pa;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return names.size();
        }

        @Override
        public Object getItem(int position) {
            return names.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (null == convertView) {
                convertView = inflater.inflate(R.layout.item_file, null);
                holder = new ViewHolder();
                holder.text = convertView.findViewById(R.id.textView);
                holder.image = convertView.findViewById(R.id.imageView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            File f = new File(paths.get(position).toString());
            holder.text.setText(f.getName());
            if (f.isDirectory()) {
                holder.image.setImageResource(R.drawable.folder);
            } else if (f.isFile()) {
                holder.image.setImageResource(R.drawable.file);
            } else {
                Log.e("file", f.getName());
            }
            return convertView;
        }

        private static class ViewHolder {
            private TextView text;
            private ImageView image;
        }
    }

}
