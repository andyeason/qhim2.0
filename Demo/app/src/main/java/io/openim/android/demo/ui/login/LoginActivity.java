package io.openim.android.demo.ui.login;

import android.content.Intent;

import io.openim.android.demo.ui.ServerConfigActivity;
import io.openim.android.demo.ui.main.MainActivity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.hbb20.CountryCodePicker;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.openim.android.demo.databinding.ActivityLoginBinding;
import io.openim.android.demo.vm.LoginVM;
import io.openim.android.ouicore.base.BaseActivity;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.LanguageUtil;
import io.openim.android.ouicore.utils.OnDedrepClickListener;
import io.openim.android.ouicore.utils.SharedPreferencesUtil;
import io.openim.android.ouicore.utils.SinkHelper;
import io.openim.android.ouicore.widget.BottomPopDialog;
import io.openim.android.ouicore.widget.WaitDialog;

public class LoginActivity extends BaseActivity<LoginVM, ActivityLoginBinding> implements LoginVM.ViewAction {

    public static final String FORM_LOGIN = "form_login";
    private WaitDialog waitDialog;
    //是否是验证码登录
    private boolean isVCLogin = false;
    private Timer timer;
    //验证码倒计时
    private int countdown = 60;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseApp.inst().removeCacheVM(LoginVM.class);
        bindVM(LoginVM.class, true);
        bindViewDataBinding(ActivityLoginBinding.inflate(getLayoutInflater()));
        setLightStatus();
        SinkHelper.get(this).setTranslucentStatus(null);

        initView();

        click();
        listener();
    }

    void initView() {
        waitDialog = new WaitDialog(this);
        view.loginContent.setLoginVM(vm);
        view.setLoginVM(vm);

        CountryCodePicker.Language language = buildDefaultLanguage();
        view.loginContent.countryCode.changeDefaultLanguage(language);

        view.version.setText(Common.getAppPackageInfo().versionName);
        vm.isPhone.setValue(SharedPreferencesUtil.get(this).getInteger(Constants.K_LOGIN_TYPE)==0);
    }

    public static CountryCodePicker.Language buildDefaultLanguage() {
        Locale locale = LanguageUtil.getCurrentLocale(BaseApp.inst());
        CountryCodePicker.Language language;
        if (locale.getLanguage().equals(Locale.CHINA.getLanguage()))
            language = CountryCodePicker.Language.CHINESE_SIMPLIFIED;
        else language = CountryCodePicker.Language.forCountryNameCode(locale.getLanguage());
        return language;
    }

    private void listener() {
        vm.isPhone.observe(this, v -> {
            if (v) {
                view.phoneTv.setTextColor(Color.parseColor("#1D6BED"));
                view.mailTv.setTextColor(Color.parseColor("#333333"));
                view.phoneVv.setVisibility(View.VISIBLE);
                view.mailVv.setVisibility(View.INVISIBLE);
            } else {
                view.mailTv.setTextColor(Color.parseColor("#1D6BED"));
                view.phoneTv.setTextColor(Color.parseColor("#333333"));
                view.mailVv.setVisibility(View.VISIBLE);
                view.phoneVv.setVisibility(View.INVISIBLE);
            }
            view.loginContent.edt1.setText("");
            view.loginContent.edt2.setText("");
            submitEnabled();
            view.loginContent.edt1.setHint(v ?
                getString(io.openim.android.ouicore.R.string.input_phone) :
                getString(io.openim.android.ouicore.R.string.input_mail));
        });
        vm.account.observe(this, v -> submitEnabled());
        vm.pwd.observe(this, v -> submitEnabled());

    }

    private void toRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private final GestureDetector gestureDetector = new GestureDetector(BaseApp.inst(),
        new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            startActivity(new Intent(LoginActivity.this, ServerConfigActivity.class));
            return super.onDoubleTap(e);
        }
    });

    private void click() {
        view.welcome.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        view.changeLoginType.setOnClickListener(v -> {
            vm.isPhone.setValue( !vm.isPhone.val());
        });

        view.loginContent.clear.setOnClickListener(v -> view.loginContent.edt1.setText(""));
        view.loginContent.clearPwd.setOnClickListener(v -> view.loginContent.edt2.setText(""));
        view.loginContent.eyes.setOnCheckedChangeListener((buttonView, isChecked) -> view.loginContent.edt2.setTransformationMethod(isChecked ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance()));
        view.protocol.setOnCheckedChangeListener((buttonView, isChecked) -> submitEnabled());
        view.registerTv.setOnClickListener(new OnDedrepClickListener() {
            @Override
            public void click(View v) {
                BottomPopDialog dialog =new BottomPopDialog(LoginActivity.this);
                dialog.getMainView().menu1.setText(io.openim.android.ouicore.R.string.phone_register);
                dialog.getMainView().menu2.setText(io.openim.android.ouicore.R.string.email_register);
                dialog.getMainView().menu3.setOnClickListener(v1 -> dialog.dismiss());
                dialog.getMainView().menu1.setOnClickListener(v1 -> {
                    vm.isPhone.setValue(true);
                    vm.isFindPassword=false;
                    toRegister();
                });
                dialog.getMainView().menu2.setOnClickListener(v1 -> {
                    vm.isPhone.setValue(false);
                    vm.isFindPassword=false;
                    toRegister();
                });
                dialog.show();
            }
        });
        view.submit.setOnClickListener(v -> {
            vm.areaCode.setValue("+"+view.loginContent.countryCode.getSelectedCountryCode());
            waitDialog.show();
            vm.login(isVCLogin ? vm.pwd.getValue() : null, 3);
        });
        view.loginContent.vcLogin.setOnClickListener(v -> {
            isVCLogin = !isVCLogin;
            updateEdit2();
        });
        view.loginContent.getVC.setOnClickListener(v -> {
            //正在倒计时中...不触发操作
            if (countdown != 60) return;
            vm.getVerificationCode(3);
        });
        view.loginContent.forgotPasswordTv.setOnClickListener(v -> {
            BottomPopDialog dialog =new BottomPopDialog(LoginActivity.this);
            dialog.getMainView().menu1.setText(io.openim.android.ouicore.R.string.forgot_pasword_by_phone);
            dialog.getMainView().menu2.setText(io.openim.android.ouicore.R.string.forgot_pasword_by_email);
            dialog.getMainView().menu3.setOnClickListener(v1 -> dialog.dismiss());
            dialog.getMainView().menu1.setOnClickListener(v1 -> {
                vm.isPhone.setValue(true);
                vm.isFindPassword=true;
                toRegister();
            });
            dialog.getMainView().menu2.setOnClickListener(v1 -> {
                vm.isPhone.setValue(false);
                vm.isFindPassword=true;
                toRegister();
            });
            dialog.show();
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void updateEdit2() {
        if (isVCLogin) {
            view.loginContent.vcTitle.setText(io.openim.android.ouicore.R.string.vc);
            view.loginContent.vcLogin.setText(io.openim.android.ouicore.R.string.password_login);
            view.loginContent.getVC.setVisibility(View.VISIBLE);
            view.loginContent.eyes.setVisibility(View.GONE);
            view.loginContent.edt2.setHint(io.openim.android.ouicore.R.string.input_verification_code);
        } else {
            view.loginContent.vcTitle.setText(io.openim.android.ouicore.R.string.password);
            view.loginContent.vcLogin.setText(io.openim.android.ouicore.R.string.vc_login);
            view.loginContent.getVC.setVisibility(View.GONE);
            view.loginContent.eyes.setVisibility(View.VISIBLE);
            view.loginContent.edt2.setHint(io.openim.android.ouicore.R.string.input_password);
        }
    }

    private void submitEnabled() {
        view.submit.setEnabled(!vm.account.getValue().isEmpty() && !vm.pwd.getValue().isEmpty());
    }

    @Override
    public void jump() {
        startActivity(new Intent(this, MainActivity.class).putExtra(FORM_LOGIN, true));
        waitDialog.dismiss();
        finish();
    }

    @Override
    public void err(String msg) {
        waitDialog.dismiss();
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void succ(Object o) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                countdown--;
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        view.loginContent.getVC.setText(countdown + "s");
                    }
                });

                if (countdown <= 0) {
                    view.loginContent.getVC.setText(io.openim.android.ouicore.R.string.get_vc);
                    countdown = 60;
                    timer.cancel();
                    timer = null;
                }
            }
        }, 0, 1000);
    }

    @Override
    public void initDate() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }
}
