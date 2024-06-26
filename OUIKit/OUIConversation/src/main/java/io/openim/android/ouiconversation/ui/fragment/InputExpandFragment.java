package io.openim.android.ouiconversation.ui.fragment;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.alibaba.android.arouter.launcher.ARouter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hjq.permissions.Permission;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import io.openim.android.ouiconversation.R;
import io.openim.android.ouiconversation.databinding.FragmentInputExpandBinding;
import io.openim.android.ouiconversation.databinding.ItemExpandMenuBinding;
import io.openim.android.ouiconversation.ui.ChatActivity;
import io.openim.android.ouiconversation.ui.ShootActivity;
import io.openim.android.ouiconversation.vm.ChatVM;
import io.openim.android.ouicore.adapter.RecyclerViewAdapter;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.BaseFragment;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.databinding.LayoutCommonDialogBinding;
import io.openim.android.ouicore.utils.ActivityManager;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.GetFilePathFromUri;
import io.openim.android.ouicore.utils.HasPermissions;
import io.openim.android.ouicore.utils.MThreadTool;
import io.openim.android.ouicore.utils.MediaFileUtil;
import io.openim.android.ouicore.utils.Routes;
import io.openim.android.ouicore.vm.SelectTargetVM;
import io.openim.android.ouicore.widget.CommonDialog;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.models.Message;


public class InputExpandFragment extends BaseFragment<ChatVM> {
    public static List<Integer> menuIcons = Arrays.asList(io.openim.android.ouicore.R.mipmap.ic_chat_photo,
        R.mipmap.ic_chat_shoot);
    public static List<String> menuTitles = Arrays.asList(BaseApp.inst().getString(io.openim.android.ouicore.R.string.album), BaseApp.inst().getString(io.openim.android.ouicore.R.string.shoot), BaseApp.inst().getString(io.openim.android.ouicore.R.string.video_calls));

    FragmentInputExpandBinding v;
    //权限
    private HasPermissions hasStorage, hasShoot;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MThreadTool.executorService.execute(() -> {
            hasStorage = new HasPermissions(getActivity(), Permission.MANAGE_EXTERNAL_STORAGE);
            hasShoot = new HasPermissions(getActivity(), Permission.CAMERA, Permission.RECORD_AUDIO);
        });

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = FragmentInputExpandBinding.inflate(inflater);
        init();
        return v.getRoot();
    }

    private void init() {
        v.getRoot().setLayoutManager(new GridLayoutManager(getContext(), 4));
        RecyclerViewAdapter adapter = new RecyclerViewAdapter<Object, ExpandHolder>(ExpandHolder.class) {

            @Override
            public void onBindView(@NonNull ExpandHolder holder, Object data, int position) {
                holder.v.menu.setCompoundDrawablesRelativeWithIntrinsicBounds(null, getContext().getDrawable(menuIcons.get(position)), null, null);
                holder.v.menu.setText(menuTitles.get(position));
                holder.v.menu.setOnClickListener(v -> {
                    switch (position) {
                        case 0:
                            showMediaPicker();
                            break;
                        case 1:
                            goToShoot();
                            break;
                    }
                });
            }
        };
        v.getRoot().setAdapter(adapter);
        adapter.setItems(menuIcons);
    }




    //去拍摄
    private void goToShoot() {
        hasShoot.safeGo(()-> shootLauncher.launch(new Intent(getActivity(), ShootActivity.class)));
    }

    private final ActivityResultLauncher<Intent> captureLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                List<String> files = Matisse.obtainPathResult(data);

                for (String file : files) {
                    Message msg = null;
                    if (MediaFileUtil.isImageType(file)) {
                        msg =
                            OpenIMClient.getInstance().messageManager.createImageMessageFromFullPath(file);
                    }
                    if (MediaFileUtil.isVideoType(file)) {
                        Glide.with(this).asBitmap().load(file).into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                String firstFame = MediaFileUtil.saveBitmap(resource,
                                    Constants.PICTURE_DIR, false);
                                long duration = MediaFileUtil.getDuration(file);
                                Message msg =
                                    OpenIMClient.getInstance().messageManager.createVideoMessageFromFullPath(file, MediaFileUtil.getFileType(file).mimeType, duration, firstFame);
                                vm.sendMsg(msg);
                            }
                        });
                        continue;
                    }
                    if (null == msg)
                        msg =
                            OpenIMClient.getInstance().messageManager.createTextMessage("[" + getString(io.openim.android.ouicore.R.string.unsupported_type) + "]");
                    vm.sendMsg(msg);
                }
            }
        });

    private final ActivityResultLauncher<Intent> shootLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            String fileUrl = result.getData().getStringExtra("fileUrl");
            if (MediaFileUtil.isImageType(fileUrl)) {
                Message msg = OpenIMClient.getInstance().messageManager.createImageMessageFromFullPath(fileUrl);
                vm.sendMsg(msg);
            }
            if (MediaFileUtil.isVideoType(fileUrl)) {
                String firstFrameUrl = result.getData().getStringExtra("firstFrameUrl");
                MediaFileUtil.MediaFileType mediaFileType = MediaFileUtil.getFileType(fileUrl);
                long duration = MediaFileUtil.getDuration(fileUrl);
                Message msg = OpenIMClient.getInstance().messageManager
                    .createVideoMessageFromFullPath(fileUrl, mediaFileType.mimeType, duration, firstFrameUrl);
                vm.sendMsg(msg);
            }
        }
    });

    @SuppressLint("WrongConstant")
    private void showMediaPicker() {
        hasStorage.safeGo(() -> Matisse.from(getActivity()).choose(MimeType.ofAll())
            .countable(true)
            .maxSelectable(9)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            .thumbnailScale(0.85f)
            .imageEngine(new GlideEngine()).forResult(captureLauncher));
    }



    public void setChatVM(ChatVM vm) {
        this.vm = vm;
    }

    public static class ExpandHolder extends RecyclerView.ViewHolder {
        public ItemExpandMenuBinding v;

        public ExpandHolder(@NonNull View itemView) {
            super(ItemExpandMenuBinding.inflate(LayoutInflater.from(itemView.getContext())).getRoot());
            v = ItemExpandMenuBinding.bind(this.itemView);
        }
    }
}
