package io.openim.android.demo.vm;

import static io.openim.android.ouicore.utils.Common.md5;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;


import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.vm.State;
import io.openim.android.ouicore.entity.LoginCertificate;
import io.openim.android.demo.repository.OpenIMService;
import io.openim.android.ouicore.base.BaseViewModel;
import io.openim.android.ouicore.base.IView;
import io.openim.android.ouicore.net.RXRetrofit.N;
import io.openim.android.ouicore.net.RXRetrofit.NetObserver;
import io.openim.android.ouicore.net.RXRetrofit.Parameter;

import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.RegexValid;
import io.openim.android.ouicore.utils.SharedPreferencesUtil;
import io.openim.android.ouicore.widget.WaitDialog;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.enums.Platform;
import io.openim.android.sdk.listener.OnBase;

public class LoginVM extends BaseViewModel<LoginVM.ViewAction> {
    public static final int MAX_COUNTDOWN = 60;
    private static final String TAG = "LoginVM";
    public State<Boolean> isPhone = new State<>(true);
    public State<String> account = new State<>("");
    //密码或验证码
    public State<String> pwd = new State<>("");
    public State<Integer> countdown = new State<>(MAX_COUNTDOWN);
    public State<String> nickName = new State<>("");
    public State<String> areaCode = new State<>("+86");

    public String verificationCode;
    //是否是找回密码
    public boolean isFindPassword = false;

    public void login(String verificationCode, int usedFor) {
        SharedPreferencesUtil.get(BaseApp.inst()).setCache(Constants.K_LOGIN_TYPE, isPhone.val() ?
            0 : 1);
        Parameter parameter = getParameter(verificationCode, usedFor);
        N.API(OpenIMService.class)
            .login(parameter.buildJsonBody())
            .compose(N.IOMain())
            .map(OpenIMService.turn(LoginCertificate.class))
            .subscribe(new NetObserver<LoginCertificate>(getContext()) {

                @Override
                public void onSuccess(LoginCertificate loginCertificate) {
                    try {
                        OpenIMClient.getInstance().login(new OnBase<String>() {
                            @Override
                            public void onError(int code, String error) {
                                getIView().err(error);
                            }

                            @Override
                            public void onSuccess(String data) {
                                //缓存登录信息
                                Log.d(TAG, "LoginCertificate OpenIMClient.getInstance().login onSuccess");
                                loginCertificate.cache(getContext());
                                BaseApp.inst().loginCertificate = loginCertificate;
                                getIView().jump();
                            }
                        }, loginCertificate.userID, loginCertificate.imToken);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void onFailure(Throwable e) {
                    getIView().err(e.getMessage());
                }
            });
    }


    /**
     * @param verificationCode
     * @param usedFor          1注册 2重置 3登录
     * @return
     */
    @NonNull
    private Parameter getParameter(String verificationCode, int usedFor) {
        Parameter parameter = new Parameter().add("password",
                TextUtils.isEmpty(verificationCode) ? md5(pwd.val()) : null)
            .add("platform", 2).add("usedFor", usedFor)
            .add("operationID", System.currentTimeMillis() + "")
            .add("verifyCode", verificationCode);
        if (isPhone.val()) {
            parameter.add("phoneNumber", account.getValue());
            parameter.add("areaCode", areaCode.val());
        } else
            parameter.add("email", account.getValue());
        return parameter;
    }

    /**
     * @param usedFor 1注册 2重置 3登录
     */
    public void getVerificationCode(int usedFor) {
        Parameter parameter = getParameter(null, usedFor);
        WaitDialog waitDialog = showWait();
        N.API(OpenIMService.class).getVerificationCode(parameter.buildJsonBody()).map(OpenIMService.turn(Object.class)).compose(N.IOMain()).subscribe(new NetObserver<Object>(getContext()) {
            @Override
            public void onSuccess(Object o) {
                getIView().succ(o);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                waitDialog.dismiss();
            }

            @Override
            protected void onFailure(Throwable e) {
                getIView().err(e.getMessage());
            }
        });

    }

    @NonNull
    public WaitDialog showWait() {
        WaitDialog waitDialog = new WaitDialog(getContext());
        waitDialog.setNotDismiss();
        waitDialog.show();
        return waitDialog;
    }

    private Timer timer;

    public void countdown() {
        if (null == timer) timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (countdown.getValue() == 0) {
                    timer.cancel();
                    timer = null;
                    return;
                }
                countdown.postValue(countdown.getValue() - 1);
            }
        }, 1000, 1000);

    }

    @Override
    protected void releaseRes() {
        super.releaseRes();
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 检查验证码并注册
     */
    public void checkVerificationCode(String verificationCode, int usedFor) {
        Parameter parameter = getParameter(verificationCode, usedFor);
        WaitDialog waitDialog = showWait();
        N.API(OpenIMService.class).checkVerificationCode(parameter.buildJsonBody()).map(OpenIMService.turn(HashMap.class)).compose(N.IOMain()).subscribe(new NetObserver<HashMap>(getContext()) {
            @Override
            public void onComplete() {
                super.onComplete();
                waitDialog.dismiss();
            }

            @Override
            public void onSuccess(HashMap o) {
                LoginVM.this.verificationCode = verificationCode;
                getIView().succ("checkVerificationCode");
            }

            @Override
            protected void onFailure(Throwable e) {
                getIView().err(e.getMessage());
            }
        });
    }

    public void resetPassword(String password) {
        Parameter parameter = getParameter(verificationCode, 2);
        //这里要把密码传入
        parameter.add("password", md5(password));
        WaitDialog waitDialog = showWait();
        N.API(OpenIMService.class).resetPassword(parameter.buildJsonBody()).map(OpenIMService.turn(HashMap.class)).compose(N.IOMain()).subscribe(new NetObserver<HashMap>(getContext()) {
            @Override
            public void onComplete() {
                super.onComplete();
                waitDialog.dismiss();
            }

            @Override
            public void onSuccess(HashMap o) {
                getIView().succ(null);
            }

            @Override
            protected void onFailure(Throwable e) {
                getIView().err(e.getMessage());
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword) {
        Parameter parameter = new Parameter();
        parameter.add("userID", BaseApp.inst().loginCertificate.userID)
            .add("currentPassword", Common.md5(currentPassword))
            .add("newPassword", Common.md5(newPassword));

        WaitDialog waitDialog = showWait();
        N.API(OpenIMService.class)
            .changePassword(parameter.buildJsonBody())
            .map(OpenIMService.turn(String.class))
            .compose(N.IOMain())
            .subscribe(new NetObserver<String>(context.get()) {

                @Override
                public void onComplete() {
                    waitDialog.dismiss();
                }

                @Override
                public void onSuccess(String o) {
                    getIView().succ(o);
                }

                @Override
                protected void onFailure(Throwable e) {
                    getIView().err(e.getMessage());
                }
            });
    }

    public void register() {
        String pwdValue = pwd.getValue();
        if (!RegexValid.isValidPassword(pwdValue)) {
            toast(BaseApp.inst().getString(
                io.openim.android.ouicore.R.string.password_valid_tips));
            return;
        }
        Parameter parameter = new Parameter();
        parameter.add("verifyCode", verificationCode);
        parameter.add("platform", Platform.ANDROID);
        parameter.add("autoLogin", true);

        Map<String, String> user = new HashMap<>();
        user.put("password", md5(pwdValue));
        user.put("nickname", nickName.getValue());
        user.put("areaCode", areaCode.val());
        if (isPhone.val()) {
            user.put("phoneNumber", account.getValue());
        } else {
            user.put("email", account.getValue());
        }
        parameter.add("user", user);

        WaitDialog waitDialog = showWait();
        N.API(OpenIMService.class).register(parameter.buildJsonBody()).map(OpenIMService.turn(LoginCertificate.class)).compose(N.IOMain()).subscribe(new NetObserver<LoginCertificate>(context.get()) {
            @Override
            public void onComplete() {
                super.onComplete();
                waitDialog.dismiss();
            }

            @Override
            public void onSuccess(LoginCertificate o) {
//                setSelfInfo();
                Log.d(TAG, "LoginCertificate register onSuccess ");
                o.cache(getContext());
                getIView().jump();
            }

            @Override
            protected void onFailure(Throwable e) {
                getIView().toast(e.getMessage());
            }
        });
    }


    public interface ViewAction extends IView {
        ///跳转
        void jump();

        void err(String msg);

        void succ(Object o);

        void initDate();

    }

}
