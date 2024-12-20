package io.openim.android.ouigroup.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.bigkoo.pickerview.adapter.ArrayWheelAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.openim.android.ouicore.adapter.RecyclerViewAdapter;
import io.openim.android.ouicore.base.BaseActivity;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.databinding.LayoutBurnAfterReadingBinding;
import io.openim.android.ouicore.entity.ExGroupMemberInfo;
import io.openim.android.ouicore.entity.MsgConversation;
import io.openim.android.ouicore.ex.MultipleChoice;
import io.openim.android.ouicore.im.IMEvent;
import io.openim.android.ouicore.im.IMUtil;
import io.openim.android.ouicore.net.bage.GsonHel;
import io.openim.android.ouicore.services.IConversationBridge;
import io.openim.android.ouicore.utils.ActivityManager;
import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.OnDedrepClickListener;
import io.openim.android.ouicore.utils.Routes;
import io.openim.android.ouicore.vm.ContactListVM;
import io.openim.android.ouicore.vm.GroupMemberVM;
import io.openim.android.ouicore.vm.SelectTargetVM;
import io.openim.android.ouicore.widget.CommonDialog;
import io.openim.android.ouicore.widget.ImageTxtViewHolder;
import io.openim.android.ouicore.widget.PhotographAlbumDialog;
import io.openim.android.ouicore.widget.SingleInfoModifyActivity;
import io.openim.android.ouicore.widget.WaitDialog;
import io.openim.android.ouigroup.R;
import io.openim.android.ouigroup.databinding.ActivityGroupMaterialBinding;

import io.openim.android.ouicore.vm.GroupVM;
import io.openim.android.ouigroup.ui.v3.SelectTargetActivityV3;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.enums.ConversationType;
import io.openim.android.sdk.enums.Opt;
import io.openim.android.sdk.listener.OnFileUploadProgressListener;
import io.openim.android.sdk.listener.OnGroupListener;
import io.openim.android.sdk.models.ConversationReq;
import io.openim.android.sdk.models.GroupApplicationInfo;
import io.openim.android.sdk.models.GroupInfo;
import io.openim.android.sdk.models.GroupMembersInfo;
import io.openim.android.sdk.models.PutArgs;

@Route(path = Routes.Group.MATERIAL)
public class GroupMaterialActivity extends BaseActivity<GroupVM, ActivityGroupMaterialBinding> implements OnGroupListener {
    private final String TAG = "GroupMaterialActivity";
    int spanCount = 5;
    private PhotographAlbumDialog albumDialog;
    private ActivityResultLauncher infoModifyLauncher;
    private int infoModifyType;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        bindVM(GroupVM.class, true);
        vm.setContext(this);
        Easy.put(vm);
        vm.groupId = getIntent().getStringExtra(Constants.K_GROUP_ID);
        conversationId = getIntent().getStringExtra(Constants.K_ID);
        super.onCreate(savedInstanceState);
        bindViewDataBinding(ActivityGroupMaterialBinding.inflate(getLayoutInflater()));
        view.setGroupVM(vm);

        sink();

        init();
        initView();
        click();

        infoModifyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK) return;
            String var = result.getData().getStringExtra(SingleInfoModifyActivity.SINGLE_INFO_MODIFY_DATA);
            if (infoModifyType == 1) {
                vm.UPDATEGroup(vm.groupId, var, null, null, null, null);
            }
            if (infoModifyType == 2) {
                try {
                    vm.setGroupMemberNickname(vm.groupId, BaseApp.inst().loginCertificate.userID, var);
                } catch (Exception e) {
                    Log.e(TAG, "Exception:" + e);
                }
            }
        });
    }


    void init() {
        IMEvent.getInstance().addGroupListener(this);

        vm.getGroupsInfo();
        vm.getMyMemberInfo();
        vm.getConversationInfo();
    }

    private void click() {
        view.groupId.setOnClickListener(v -> {
            Common.copy(vm.groupId);
            toast(getString(io.openim.android.ouicore.R.string.copy_succ));
        });
        view.qrCode.setOnClickListener(v -> startActivity(new Intent(this, ShareQrcodeActivity.class)));

        view.groupMember.setOnClickListener(v -> {
            gotoMemberList();
        });
        view.groupName.setOnClickListener(v -> {
            try {
                infoModifyType = 1;
                SingleInfoModifyActivity.SingleInfoModifyData modifyData = new SingleInfoModifyActivity.SingleInfoModifyData();
                modifyData.title = getString(io.openim.android.ouicore.R.string.edit_group_name2);
                modifyData.description = getString(io.openim.android.ouicore.R.string.edit_group_name_tips);
                modifyData.avatarUrl = vm.groupsInfo.getValue().getFaceURL();
                modifyData.editT = vm.groupsInfo.getValue().getGroupName();
                infoModifyLauncher.launch(new Intent(this, SingleInfoModifyActivity.class).putExtra(SingleInfoModifyActivity.SINGLE_INFO_MODIFY_DATA, modifyData));
            } catch (Exception ignored) {
            }
        });
        view.avatarEdit.setOnClickListener(v -> {
            if (!vm.isOwnerOrAdmin.val()) return;
            albumDialog.show();
        });

        view.quitGroup.setOnClickListener(v -> {
            vm.quitGroup();
        });

        view.dissolveGroup.setOnClickListener(v -> {
            vm.dissolveGroup();
        });
    }

    @Override
    protected void fasterDestroy() {
        removeCacheVM();
        Easy.delete(GroupVM.class);
        IMEvent.getInstance().removeGroupListener(this);
    }

    @Override
    public void onSuccess(Object body) {
        super.onSuccess(body);

    }

    private void initView() {

        albumDialog = new PhotographAlbumDialog(this);
        albumDialog.setOnSelectResultListener(path -> {
            PutArgs putArgs = new PutArgs(path[0]);
            OpenIMClient.getInstance().uploadFile(new OnFileUploadProgressListener() {
                @Override
                public void onError(int code, String error) {

                }

                @Override
                public void onProgress(long progress) {

                }

                @Override
                public void onSuccess(String s) {
                    try {
                        Map<String, String> fromJson = GsonHel.fromJson(s, Map.class);
                        vm.UPDATEGroup(vm.groupId, null, fromJson.get("url"), null, null, null);
                    } catch (Exception ignored) {
                    }
                }
            }, null, putArgs);
        });

        view.recyclerview.setLayoutManager(new GridLayoutManager(this, spanCount));
        RecyclerViewAdapter adapter = new RecyclerViewAdapter<GroupMembersInfo, ImageTxtViewHolder>(ImageTxtViewHolder.class) {

            @Override
            public void onBindView(@NonNull ImageTxtViewHolder holder, GroupMembersInfo data, int position) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.view.img.getLayoutParams();
                layoutParams.topMargin = 0;
                layoutParams.width = Common.dp2px(48);
                layoutParams.height = Common.dp2px(48);

                holder.view.txt.setTextSize(12);
                holder.view.txt.setTextColor(getResources().getColor(io.openim.android.ouicore.R.color.txt_shallow));
                holder.view.img.setVisibility(TextUtils.isEmpty(data.getGroupID()) ? View.GONE : View.VISIBLE);
                holder.view.img2.setVisibility(TextUtils.isEmpty(data.getGroupID()) ? View.VISIBLE : View.GONE);
                if (TextUtils.isEmpty(data.getGroupID())) {
                    //加/减按钮
                    int reId;
                    holder.view.img2.setImageResource(reId = data.getRoleLevel());
                    holder.view.txt.setText(reId == R.mipmap.ic_group_add ? io.openim.android.ouicore.R.string.add : io.openim.android.ouicore.R.string.remove);
                    holder.view.getRoot().setOnClickListener(v -> {
                        boolean isAdd = reId == R.mipmap.ic_group_add;
                        if (isAdd) {
                            inviteIntoGroup(GroupMaterialActivity.this, vm);
                        } else {
                            gotoMemberListByRemove();
                        }
                    });
                } else {
                    holder.view.img.load(data.getFaceURL(), data.getNickname());
                    holder.view.txt.setText(data.getNickname());
                    holder.view.getRoot().setOnClickListener(v -> {
                        ARouter.getInstance().build(Routes.Main.PERSON_DETAIL).withString(Constants.K_ID, data.getUserID()).withString(Constants.K_GROUP_ID, vm.groupId).navigation();
                    });
                }
            }

        };
        view.recyclerview.setAdapter(adapter);
        vm.groupsInfo.observe(this, groupInfo -> {
            view.avatar.load(groupInfo.getFaceURL(), true);
            vm.getGroupMemberList(spanCount * 2);

            view.all.setText(String.format(getResources().getString(io.openim.android.ouicore.R.string.view_all_member), groupInfo.getMemberCount()));
            if (groupInfo.getMemberCount() == 0) {
                deleteChat();
            }
        });
        vm.isJoinGroup.observe(this, v -> {
            if (!v) {
                deleteChat();
            }
        });

        vm.groupMembers.observe(this, groupMembersInfos -> {
            if (groupMembersInfos.isEmpty()) return;
            view.all.setText(String.format(getResources().getString(io.openim.android.ouicore.R.string.view_all_member), vm.groupsInfo.getValue().getMemberCount()));
            spanCount = spanCount * 2;
            boolean owner = vm.isOwnerOrAdmin.val();
            int end = owner ? spanCount - 2 : spanCount - 1;
            if (groupMembersInfos.size() < end) {
                end = groupMembersInfos.size();
            }
            List<GroupMembersInfo> groupMembersInfos1 = new ArrayList<>(groupMembersInfos.subList(0, end));
            GroupMembersInfo add = new GroupMembersInfo();
            add.setRoleLevel(R.mipmap.ic_group_add);
            groupMembersInfos1.add(add);

            if (owner) {
                GroupMembersInfo reduce = new GroupMembersInfo();
                reduce.setRoleLevel(R.mipmap.ic_group_reduce);
                groupMembersInfos1.add(reduce);
            }

            adapter.setItems(groupMembersInfos1);
            spanCount = 5;
        });
    }

    private void deleteChat() {
        view.quitGroupText.setText(io.openim.android.ouicore.R.string.delete_conversiton);
        view.quitGroup.setOnClickListener(new OnDedrepClickListener() {
            @Override
            public void click(View v) {
                ContactListVM contactListVM = BaseApp.inst().getVMByCache(ContactListVM.class);
                if (null != contactListVM) {
                    contactListVM.deleteConversationAndDeleteAllMsg(conversationId);
                    Postcard postcard = ARouter.getInstance().build(Routes.Conversation.CHAT);
                    LogisticsCenter.completion(postcard);
                    ActivityManager.finishActivity(postcard.getDestination());
                    finish();
                }
            }
        });
    }

    private void gotoMemberListByRemove() {
        GroupMemberVM memberVM = Easy.installVM(GroupMemberVM.class);
        memberVM.groupId = vm.groupId;
        memberVM.setIntention(GroupMemberVM.Intention.AT);
        memberVM.isSearchSingle = true;
        memberVM.isRemoveOwnerAndAdmin = false;
        memberVM.setOnFinishListener(activity -> {
            List<String> ids = new ArrayList<>();
            for (MultipleChoice choice : memberVM.choiceList.val()) {
                ids.add(choice.key);
            }
            vm.kickGroupMember(ids);
            activity.finish();
        });
        startActivity(new Intent(this, SuperGroupMemberActivity.class));
    }

    public static void inviteIntoGroup(Context ctx, GroupVM vm) {
        SelectTargetVM sv = Easy.installVM(SelectTargetVM.class);
        sv.setIntention(SelectTargetVM.Intention.invite);
        ContactListVM ctv = BaseApp.inst().getVMByCache(ContactListVM.class);
        if (null == ctv) return;
        List<String> ids = new ArrayList<>();
        for (MsgConversation msgConversation : ctv.conversations.val()) {
            if (msgConversation.conversationInfo.getConversationType() == ConversationType.SINGLE_CHAT) {
                ids.add(msgConversation.conversationInfo.getUserID());
            }
        }
        sv.isInGroup(vm.groupId, ids, null);
        sv.setOnFinishListener(() -> {
            List<String> selectIds = new ArrayList<>();
            for (MultipleChoice choice : sv.inviteList.val()) {
                selectIds.add(choice.key);
            }
            vm.inviteUserToGroup(selectIds);
        });
        ctx.startActivity(new Intent(ctx, SelectTargetActivityV3.class).putExtra("groupId", vm.groupId));
    }

    public String convertToDaysWeeksMonths(long seconds) {
        long days = seconds / (60 * 60 * 24);
        long weeks = days / 7;
        long months = weeks / 4;

        if (days <= 6) {
            return days + getString(io.openim.android.ouicore.R.string.day);
        } else if (weeks <= 6 && (days % 7 == 0)) {
            return weeks + getString(io.openim.android.ouicore.R.string.week);
        } else {
            return months + getString(io.openim.android.ouicore.R.string.month);
        }
    }

    private void gotoMemberList() {
        GroupMemberVM memberVM = Easy.installVM(GroupMemberVM.class);
        memberVM.groupId = vm.groupId;
        memberVM.isOwnerOrAdmin = vm.isOwnerOrAdmin.val();
        memberVM.setIntention(GroupMemberVM.Intention.CHECK);
        memberVM.setOnFinishListener(activity -> {
            ARouter.getInstance().build(Routes.Main.PERSON_DETAIL).withString(Constants.K_ID, memberVM.choiceList.val().get(0).key).withString(Constants.K_GROUP_ID, vm.groupId).navigation();
            memberVM.removeChoice(memberVM.choiceList.val().get(0).key);
        });
        startActivity(new Intent(this, SuperGroupMemberActivity.class));
    }

    @Override
    public void onGroupApplicationAccepted(GroupApplicationInfo info) {

    }

    @Override
    public void onGroupApplicationAdded(GroupApplicationInfo info) {

    }

    @Override
    public void onGroupApplicationDeleted(GroupApplicationInfo info) {

    }

    @Override
    public void onGroupApplicationRejected(GroupApplicationInfo info) {

    }

    @Override
    public void onGroupDismissed(GroupInfo info) {

    }

    @Override
    public void onGroupInfoChanged(GroupInfo info) {
        if (info.getGroupID().equals(vm.groupId)) {
            vm.groupsInfo.setValue(info);
        }
    }

    @Override
    public void onGroupMemberAdded(GroupMembersInfo info) {

    }

    @Override
    public void onGroupMemberDeleted(GroupMembersInfo info) {

    }

    @Override
    public void onGroupMemberInfoChanged(GroupMembersInfo info) {
        if (info.getGroupID().equals(vm.groupId) && info.getUserID().equals(BaseApp.inst().loginCertificate.userID)) {
            vm.updateRole(info);
            vm.getGroupsInfo();
        }
    }

    @Override
    public void onJoinedGroupAdded(GroupInfo info) {

    }

    @Override
    public void onJoinedGroupDeleted(GroupInfo info) {

    }
}
