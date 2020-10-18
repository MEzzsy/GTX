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
package com.didi.wstt.gt.plugin;

import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class PluginManager {

    private LinkedHashSet<PluginItem> pluginList;
    private Map<String, PluginItem> piMap;
    private static PluginItem[] EMPTY = {};
    private PluginControler mPluginControler;

    private enum SingletonHodler {
        SINGLETON;

        private PluginManager mPluginManager;

        SingletonHodler() {
            mPluginManager = new PluginManager();
        }

        public PluginManager getPluginManager() {
            return mPluginManager;
        }
    }

    public static PluginManager getInstance() {
        return SingletonHodler.SINGLETON.getPluginManager();
    }

    private PluginManager() {
        pluginList = new LinkedHashSet<>();
        piMap = new HashMap<>();
    }

    public PluginItem[] getPlugins() {
        return pluginList.toArray(EMPTY);
    }

    public void addPlugin(PluginItem item) {
        synchronized (getInstance()) {
            pluginList.add(item);
            piMap.put(item.name, item);
        }
    }

    public void removePlugin(String name) {
        synchronized (getInstance()) {
            PluginItem item = piMap.remove(name);
            if (null != item) {
                pluginList.remove(item);
            }
        }
    }

    public PluginItem getPlugin(String name) {
        return piMap.get(name);
    }

    @SuppressWarnings("unchecked")
    public void onInitConnectGT() {
        LinkedHashSet<PluginItem> copyPluginList = null;
        synchronized (getInstance()) {
            copyPluginList = (LinkedHashSet<PluginItem>) pluginList.clone();
        }
        for (PluginItem item : copyPluginList) {
            item.onInitConnectGT();
        }

    }

    public void register(PluginItem item) {
        if (null != item.name && item.name.length() > 0) {
            pluginList.add(item);
            piMap.put(item.name, item);
        }
    }

    public PluginControler getPluginControler() {
        if (null == mPluginControler) {
            mPluginControler = new PluginControler();
        }
        return mPluginControler;
    }


    public void dispatchCommand(String sReceiver, Bundle bundle) {
        PluginItem item = piMap.get(sReceiver);
        if (item != null) {
            // 加入任务队列，由插件自己决定如何执行
            item.addTask(bundle);
        }
    }

    public void dispatchCommandSync(String sReceiver, Bundle bundle) {
        PluginItem item = piMap.get(sReceiver);
        if (item != null) {
            // 加入任务队列，由插件自己决定如何执行
            item.doTask(bundle);
        }
    }
}
