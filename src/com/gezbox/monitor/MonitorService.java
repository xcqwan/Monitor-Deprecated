package com.gezbox.monitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by zombie on 14/12/4.
 */
public class MonitorService extends Service {
    private static final String TAG = "MonitorService";
    protected static MonitorService Instance = null;
    private static String AppFileName = "";
    private static String UserID = "";
    private static int CacheSize = 5;
    private static String IMEI = "";

    private IBinder mIBinder = new Binder();
    private LinkedList<String> writeCache = new LinkedList<String>();
    private int writeCacheSize = 0;
    private boolean isWrite = false;

    private String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();

    /**
     * 初始化, appName为存放日志目录名称
     * @param context
     * @param appName
     */
    public static void Init(Context context, String appName, String userID) {
        if (Instance != null) {
            return;
        }
        AppFileName = appName;
        UserID = userID;
        TelephonyManager mTm = (TelephonyManager)context.getSystemService(TELEPHONY_SERVICE);
        IMEI = mTm.getDeviceId();
        context.startService(new Intent(context, MonitorService.class));
    }

    /**
     * 设置当前用户ID
     * @param userID
     */
    public static void SetUserID(String userID) {
        if (Instance != null) {
            Instance.writeAllCache();
        }
        UserID = userID;
    }

    /**
     * 设置写入频率, 每size条日志写一次
     * @param size
     */
    public static void SetCacheSize(int size) {
        if (size > 0) {
            CacheSize = size;
        }
    }

    /**
     * 获取需上传文件地址列表
     * @return
     */
    public static ArrayList<String> GetUpLoadFileList() {
        ArrayList<String> logFileList = new ArrayList<String>();
        if (Instance != null) {
            logFileList = Instance.getUpLoadFileList();
        }
        return logFileList;
    }

    /**
     * 获取当前写入文件地址
     * @return
     */
    public static String GetCurrentLogFile() {
        if (Instance != null) {
            return Instance.getCurrentLogFile();
        }
        return "";
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate 开启服务");
        super.onCreate();

        Instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand 开启服务");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy 销毁服务");
        super.onDestroy();
        //服务被干掉时, 将缓存的全部写入文件
        writeAllCache();

        //若已登录, 销毁时重启服务
        if (!UserID.isEmpty()) {
            startService(new Intent(this, MonitorService.class));
        }
    }

    /**
     * 需上传文件列表, 除当前写入文件
     * @return
     */
    protected ArrayList<String> getUpLoadFileList() {
        ArrayList<String> uploadFileList = new ArrayList<String>();
        File dir = new File(basePath + "/" + AppFileName);
        if (dir.exists()) {
            File files[] = dir.listFiles();
            String logFilePath = getCurrentLogFile();
            for (File file : files) {
                String filePath = file.getAbsolutePath();
                if (!filePath.equals(logFilePath) && file.length() > 0) {
                    uploadFileList.add(filePath);
                }
            }
        }

        return uploadFileList;
    }

    /**
     * 当前写入log文件
     * @return
     */
    protected String getCurrentLogFile() {
        String date = MonitorUtils.getDateStr();
        File dir = new File(basePath + "/" + AppFileName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir.getPath() + "/" + UserID + "_" + date + "_" + IMEI);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        return "";
    }

    /**
     * 将缓存全写入文件
     */
    protected void writeAllCache() {
        if (writeCacheSize > 0) {
            writeToFile();
        }
    }

    /**
     * 写入缓存
     * @param content
     */
    protected void pushToCache(String content) {
        writeCache.addLast(content);
        writeCacheSize++;
        if (writeCacheSize >= CacheSize) {
            writeToFile();
        }
    }

    /**
     * 写入日志到文件
     */
    private void writeToFile() {
        if (isWrite) {
            return;
        }

        isWrite = true;
        StringBuilder writeSB = new StringBuilder();
        while (!writeCache.isEmpty()) {
            writeSB.append("\n" + writeCache.removeFirst());
        }
        writeCacheSize = 0;
        write(getWriteFile(), writeSB.toString(), true);
        isWrite = false;
    }

    /**
     * 获取每天写入的文件
     * @return
     */
    private File getWriteFile() {
        String date = MonitorUtils.getDateStr();
        File dir = new File(basePath + "/" + AppFileName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir.getPath() + "/" + UserID + "_" + date + "_" + IMEI);
        if (!file.exists()) {
            try {
                file.createNewFile();
                String headerContent = getDeviceInfo();
                write(file, headerContent, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 获取设备信息
     * @return
     */
    private String getDeviceInfo() {
        TelephonyManager mTm = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
        String imei = mTm.getDeviceId();
        String numer = mTm.getLine1Number();

        Map<String, String> deviceInfo = new HashMap<String, String>();
        deviceInfo.put("IMEI", imei);
        deviceInfo.put("MODEL", Build.MODEL);
        deviceInfo.put("BRAND", Build.BRAND);
        deviceInfo.put("numer", numer);
        deviceInfo.put("SDK_INT", Build.VERSION.SDK_INT + "");
        deviceInfo.put("DISPLAY", Build.DISPLAY);
        deviceInfo.put("APP_VERSION", getVersionCode(this) + "");
        return deviceInfo.toString();
    }

    /**
     * 向文件file写入content
     * @param file
     * @param content
     * @param append
     */
    private void write(File file, String content, boolean append) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(file, append);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 获取应用版本号
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
