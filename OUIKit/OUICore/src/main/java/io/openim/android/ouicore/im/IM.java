package io.openim.android.ouicore.im;


import android.app.Application;
import android.util.Log;

import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.L;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.models.InitConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

public class IM {
    public static void initSdk(Application app) {
        L.e("App", "---IM--initSdk");
        InitConfig initConfig = new InitConfig(Constants.getImApiUrl(),
            Constants.getImWsUrl(), getStorageDir());
        initConfig.isLogStandardOutput = true;
//        initConfig.logLevel=Integer.parseInt(Constants.getLogLevel());
//        initConfig.systemType = "native";
        initConfig.logLevel = 6;
        initConfig.logFilePath = getStorageDir();

        ///IM 初始化
        OpenIMClient.getInstance().initSDK(app,
            initConfig, IMEvent.getInstance().connListener);

        IMEvent.getInstance().init();

        Log.d("initSdk", "BaseApp.inst().getFilesDir().getAbsolutePath():" + BaseApp.inst().getFilesDir().getAbsolutePath() + ",BaseApp.inst().getFilesDir()："+BaseApp.inst().getFilesDir());
        String filePath = getStorageDir() + "/example_file.txt";
        // 使用字符串路径创建 File 对象
        File file = new File(filePath);
        writeFile(file, "hello test only");
        Log.d("initSdk", "readFile(file):" + readFile(file));

    }

    private static void writeFile(File file, String data) {
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readFile(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream is = new FileInputStream(file)) {
            int character;
            while ((character = is.read()) != -1) {
                stringBuilder.append((char) character);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
    //存储路径
    public static String getStorageDir() {
          return BaseApp.inst().getFilesDir().getAbsolutePath();
    }
}
