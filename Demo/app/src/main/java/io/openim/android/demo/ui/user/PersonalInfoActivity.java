package io.openim.android.demo.ui.user;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.view.TimePickerView;

import java.util.HashMap;
import java.util.Map;

import io.openim.android.demo.databinding.ActivityPersonalInfoBinding;
import io.openim.android.demo.ui.main.EditTextActivity;
import io.openim.android.demo.vm.PersonalVM;
import io.openim.android.ouicore.base.BaseActivity;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.net.bage.GsonHel;
import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.RegexValid;
import io.openim.android.ouicore.utils.TimeUtil;
import io.openim.android.ouicore.widget.BottomPopDialog;
import io.openim.android.ouicore.widget.PhotographAlbumDialog;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.listener.OnFileUploadProgressListener;
import io.openim.android.sdk.models.PutArgs;

public class PersonalInfoActivity extends BaseActivity<PersonalVM, ActivityPersonalInfoBinding> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        bindVM(PersonalVM.class);
        super.onCreate(savedInstanceState);
        bindViewDataBinding(ActivityPersonalInfoBinding.inflate(getLayoutInflater()));
        vm.getSelfUserInfo();

        initView();
        click();
    }

    private void click() {
        PhotographAlbumDialog albumDialog = new PhotographAlbumDialog(this);
        view.avatarLy.setOnClickListener(view -> {
            albumDialog.setOnSelectResultListener(path -> {
                vm.setFaceURL(path[0]);
                vm.waitDialog.show();

                PutArgs putArgs = new PutArgs(path[0]);
                OpenIMClient.getInstance().uploadFile(new OnFileUploadProgressListener() {
                    @Override
                    public void onError(int code, String error) {
                        vm.waitDialog.dismiss();
                        toast(error + code);
                    }

                    @Override
                    public void onProgress(long progress) {
                        vm.waitDialog.dismiss();
                    }

                    @Override
                    public void onSuccess(String s) {
                        Map<String, String> map = GsonHel.fromJson(s, HashMap.class);
                        s = map.get("url");
                        vm.setFaceURL(s);
                        Common.UIHandler.postDelayed(() -> vm.waitDialog.dismiss(), 1500);
                    }
                }, null, putArgs);
            });
            albumDialog.show();
        });
        view.nickNameLy.setOnClickListener(v ->
            nicknameLauncher.launch(new Intent(this, EditTextActivity.class)
                .putExtra(EditTextActivity.INIT_TXT, vm.userInfo.val().getNickname())
                .putExtra(EditTextActivity.TITLE,
                    getString(io.openim.android.ouicore.R.string.NickName))));
        view.genderLy.setOnClickListener(v -> {
            BottomPopDialog dialog = new BottomPopDialog(this);
            dialog.show();
            dialog.getMainView().menu3.setOnClickListener(v1 -> dialog.dismiss());
            dialog.getMainView().menu1.setText(io.openim.android.ouicore.R.string.male);
            dialog.getMainView().menu2.setText(io.openim.android.ouicore.R.string.girl);

            dialog.getMainView().menu1.setOnClickListener(v1 -> {
                vm.setGender(1);
                dialog.dismiss();
            });
            dialog.getMainView().menu2.setOnClickListener(v1 -> {
                vm.setGender(2);
                dialog.dismiss();
            });
        });
        view.birthdayLy.setOnClickListener(v -> {
            TimePickerView pvTime = new TimePickerBuilder(this,
                (date, v12) -> vm.setBirthday(date.getTime())).build();
            pvTime.show(v);

        });
        view.email.setOnClickListener(v -> {
            emailLauncher.launch(new Intent(this, EditTextActivity.class)
                .putExtra(EditTextActivity.INIT_TXT, vm.userInfo.val().getEmail())
                .putExtra(EditTextActivity.TITLE,
                    getString(io.openim.android.ouicore.R.string.mail)));
        });
    }

    private final ActivityResultLauncher<Intent> nicknameLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) return;
            String resultStr = result.getData().getStringExtra(Constants.K_RESULT);
            vm.setNickname(resultStr);
        });
    private final ActivityResultLauncher<Intent> emailLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) return;
            String resultStr = result.getData().getStringExtra(Constants.K_RESULT);
            if(!RegexValid.isValidEmail(resultStr)){
                toast(BaseApp.inst().getString(io.openim.android.ouicore.R.string.email_tips));
                return;
            }
            vm.setEmail(resultStr);
        });

    private void initView() {
        vm.userInfo.observe(this, v -> {
            if (null == v) return;
            view.avatar.load(v.getFaceURL(), v.getNickname());
            view.nickName.setText(v.getNickname());
            view.gender.setText(v.getGender() == 1 ? io.openim.android.ouicore.R.string.male :
                io.openim.android.ouicore.R.string.girl);
            if (v.getBirth() > 0) {
                view.birthday.setText(TimeUtil.getTime(v.getBirth(),
                    TimeUtil.yearMonthDayFormat));
            }
            view.phoneNumTv.setText(v.getPhoneNumber());
            view.emailTV.setText(v.getEmail());
        });
    }
}
