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
package com.didi.wstt.gt.manager;

import com.didi.wstt.gt.OutPara;

import java.util.List;

public class EmptyOutParaManager extends IOutParaManager {

    private static EmptyOutParaManager INSTANCE = new EmptyOutParaManager();

    private EmptyOutParaManager() {
        super(EmptyClient.getInstance());
        this.outParaMap = null;
    }

    public static EmptyOutParaManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(OutPara para) {

    }

    public void register(String paraName, String alias, int typeProperty) {

    }

    public void removeOutPara(String paraName) {

    }

    public boolean isOutParaExsit(OutPara para) {
        return false;
    }

    public OutPara getOutPara(String paraName) {
        return null;
    }

    public void setOutparaMonitor(String str, boolean flag) {

    }

    public boolean contains(String paraName) {
        return false;
    }

    public void clear() {
    }

    public boolean isEmpty() {
        return true;
    }

    public List<OutPara> getAll() {
        return null;
    }
}
