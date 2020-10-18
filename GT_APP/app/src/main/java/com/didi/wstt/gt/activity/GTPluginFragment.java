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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.didi.wstt.gt.R;
import com.didi.wstt.gt.plugin.PluginItem;
import com.didi.wstt.gt.plugin.PluginManager;

public class GTPluginFragment extends Fragment {
    private ListView listView;
    private PluginAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new PluginAdapter(getActivity());
        adapter.notifyDataSetChanged();
        setHasOptionsMenu(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View pluginLayout = inflater.inflate(R.layout.gt_plugin,
                container, false);

        listView = pluginLayout.findViewById(R.id.plugin_list);
        listView.setAdapter(adapter);

        return pluginLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.setGroupVisible(0, true); // 屏蔽设置主菜单
    }

    static class PluginAdapter extends ArrayAdapter<PluginItem> {

        Context context;

        public PluginAdapter(Context context) {
            super(context, R.layout.gt_plugin_item, PluginManager.getInstance().getPlugins());
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            RelativeLayout rl;
            ImageView img_logo;
            TextView tv_title;
            TextView tv_description;

            if (convertView == null) {
                rl = (RelativeLayout) LayoutInflater.from(context).inflate(
                        R.layout.gt_plugin_item, parent, false);
            } else {
                rl = (RelativeLayout) convertView;
            }

            img_logo = rl.findViewById(R.id.item_ivLogo);
            tv_description = rl.findViewById(R.id.item_tvDesc);
            tv_title = rl.findViewById(R.id.item_tvSubTitle);

            final PluginItem item = this.getItem(position);
            if (item != null) {
                img_logo.setImageResource(item.logo_id);
                tv_title.setText(item.alias);
                tv_description.setText(item.description);

                rl.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(context, item.activityClass);
                        context.startActivity(intent);
                    }
                });
            }

            return rl;
        }
    }
}
