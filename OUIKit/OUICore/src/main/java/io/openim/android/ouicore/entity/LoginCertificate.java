package io.openim.android.ouicore.entity;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.net.bage.GsonHel;
import io.openim.android.ouicore.utils.L;
import io.openim.android.ouicore.utils.SharedPreferencesUtil;

public class LoginCertificate {
    private static final String TAG = "LoginCertificate";
    public String nickname;
    public String faceURL;
    public String userID;
    public String imToken;
    public String chatToken;

    //允许非好友发送消息
    //public boolean allowSendMsgNotFriend;
    //允许添加好友
    public boolean allowAddFriend;
    //允许响铃
    public boolean allowBeep;
    //允许振动
    public boolean allowVibration;
    // 全局免打扰 0：正常；1：不接受消息；2：接受在线消息不接受离线消息；
    public int globalRecvMsgOpt;

    public void cache(Context context) {
        Log.d(TAG, "LoginCertificate cache context:" + context);
        SharedPreferencesUtil.get(context).setCache("user.LoginCertificate",
            GsonHel.toJson(this));
    }

    public static LoginCertificate getCache(Context context) {
        String u = SharedPreferencesUtil.get(context).getString("user.LoginCertificate");
        if (u.isEmpty()) {
            Log.d(TAG, "LoginCertificate getCache null, context:" + context);
            return null;
        }
        Log.d(TAG, "LoginCertificate getCache ok. context:" + context);
        return GsonHel.fromJson(u, LoginCertificate.class);
    }

    public static void clear() {
        SharedPreferencesUtil.remove(BaseApp.inst(),
            "user.LoginCertificate");
        L.e(TAG,"LoginCertificate 已移除");
    }

}
