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
package com.didi.wstt.gt.internal;

public abstract class GTDaemonThread extends Thread {
    protected boolean flag = true;
    protected long curInterval = 3 * 60 * 1000; // 默认监测间隔是3分钟一次

    public GTDaemonThread() {
        this.setDaemon(true);
    }

    public void quickFnish() {
        flag = false;
    }

    public void setInterval(long interval) {
        curInterval = interval;
    }

    @Override
    public void run() {
        do {
            try {
                sleep(curInterval); // 守护线程，先启动监视即可，sleep放前面
                doTask();
            } catch (InterruptedException e) {
                return;
            }

        }
        while (flag);
    }

    abstract void doTask();
}
