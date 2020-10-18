package com.didi.wstt.gt.analysis4.obj;

import java.util.HashMap;

/**
 * Created by Elvis on 2017/1/4.
 * Email:elvis@21kunpeng.com
 * 用于保存后台定时采集的数据（流量、内存、CPU、电量）
 */

public class NormalInfo {


    //(cpuApp - lastCpuApp) * 100L / (cpuTotal-lastCpuTotal);//计算cpu占用率

    public long time;//时间
    public long memory;
    public long flowUpload;
    public long flowDownload;
    public long cpuApp;
    public long cpuTotal;
    public HashMap<String, ThreadCpu> threadCpus = new HashMap<>();//所有线程的CPU占用


    public static class ThreadCpu {
        String threadId;
        String ThreadName;
        long threadCpu;
    }


}
