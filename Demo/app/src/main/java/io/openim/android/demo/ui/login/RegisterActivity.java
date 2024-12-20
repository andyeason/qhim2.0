package io.openim.android.demo.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import io.openim.android.demo.databinding.ActivityRegisterBinding;
import io.openim.android.demo.vm.LoginVM;
import io.openim.android.ouicore.base.BaseActivity;
import io.openim.android.ouicore.utils.RegexValid;

public class RegisterActivity extends BaseActivity<LoginVM, ActivityRegisterBinding> implements LoginVM.ViewAction, TextWatcher {


    String id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindVMByCache(LoginVM.class);
        if (null == vm) {
            bindVM(LoginVM.class);
        }
        bindViewDataBinding(ActivityRegisterBinding.inflate(getLayoutInflater()));
        sink();
        view.setLoginVM(vm);

        initView();
        listener();
    }


    private void listener() {
        view.edt1.addTextChangedListener(this);
        view.protocol.setOnCheckedChangeListener((buttonView, isChecked) -> submitEnabled());
        view.clear.setOnClickListener(v -> view.edt1.setText(""));

        view.submit.setOnClickListener(v -> {
            if (vm.isPhone.val()){
               if ( !RegexValid.isValidPhoneNumber(vm.account.val())){
                   toast(getString(io.openim.android.ouicore.R.string.valid_phone_num));
                   return;
               }
            }
            vm.areaCode.setValue("+"+view.countryCode.getSelectedCountryCode());
            vm.getVerificationCode(vm.isFindPassword?2:1);
        });
    }

    private void initView() {
        view.countryCode.changeDefaultLanguage(LoginActivity.buildDefaultLanguage());

        view.tips.setText(vm.isPhone.getValue() ? getString(io.openim.android.ouicore.R.string.phone_register) : getString(io.openim.android.ouicore.R.string.mail_register));
        view.edt1.setHint(vm.isPhone.getValue() ? getString(io.openim.android.ouicore.R.string.input_phone) : getString(io.openim.android.ouicore.R.string.input_mail));
        if (vm.isFindPassword){
            view.tips.setText(io.openim.android.ouicore.R.string.phone_num);
            view.title.setText(io.openim.android.ouicore.R.string.forgot_password);
            view.protocolLy.setVisibility(View.GONE);
            view.submit.setText(io.openim.android.ouicore.R.string.get_vc);
            view.protocol.setChecked(true);
        }
    }

    private void submitEnabled() {
        id = view.edt1.getText().toString();
        view.submit.setEnabled(!id.isEmpty());
    }

    public void back(View view) {
        finish();
    }

    @Override
    public void jump() {

    }

    @Override
    public void err(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void succ(Object o) {
        Toast.makeText(this, io.openim.android.ouicore.R.string.send_succ, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, VerificationCodeActivity.class));
    }

    @Override
    public void initDate() {
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        view.edt1.removeTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        submitEnabled();
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
