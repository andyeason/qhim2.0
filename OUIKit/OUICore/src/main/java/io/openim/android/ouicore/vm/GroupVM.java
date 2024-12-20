package io.openim.android.ouicore.vm;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.android.arouter.launcher.ARouter;
import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openim.android.ouicore.R;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.vm.State;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.entity.ExGroupMemberInfo;
import io.openim.android.ouicore.entity.LoginCertificate;

import io.openim.android.ouicore.im.IMBack;
import io.openim.android.ouicore.im.IMUtil;
import io.openim.android.ouicore.services.IConversationBridge;
import io.openim.android.ouicore.utils.Common;


import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.Obs;
import io.openim.android.ouicore.utils.Routes;
import io.openim.android.ouicore.widget.CommonDialog;
import io.openim.android.ouicore.widget.WaitDialog;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.enums.ConversationType;
import io.openim.android.sdk.enums.GroupRole;
import io.openim.android.sdk.enums.GroupType;
import io.openim.android.sdk.listener.OnBase;
import io.openim.android.sdk.models.ConversationInfo;
import io.openim.android.sdk.models.FriendInfo;
import io.openim.android.sdk.models.GroupInfo;
import io.openim.android.sdk.models.GroupMembersInfo;
import io.openim.android.sdk.models.SetGroupMemberInfo;

public class GroupVM extends SocialityVM {
    //禁言状态
    private final String TAG = "GroupVM";
    public State<Integer> muteStatus = new State<>(-1);
    public State<String> groupName = new State<>("");
    public State<GroupInfo> groupsInfo = new State<>();
    public State<ConversationInfo> conversationInfo = new State<>();
    //当前用户是否是群主或管理员
    public State<Boolean> isOwnerOrAdmin = new State<>(false);
    public State<Boolean> isOwner = new State<>(false);
    //是否加入群
    public State<Boolean> isJoinGroup = new State<>(true);
    //群所有成员
    public State<List<GroupMembersInfo>> groupMembers =
        new State<>(new ArrayList<>());
    //超级群成员分页加载
    public State<List<ExGroupMemberInfo>> superGroupMembers =
        new State<>(new ArrayList<>());
    //封装过的群成员 用于字母导航
    public State<List<ExGroupMemberInfo>> exGroupMembers =
        new State<>(new ArrayList<>());
    //群管理
    public State<List<ExGroupMemberInfo>> exGroupManagement =
        new State<>(new ArrayList<>());
    //群字母导航
    public State<List<String>> groupLetters = new State<>(new ArrayList<>());
    //封装过的好友信息 用于字母导航
    public String groupId;
    public State<List<FriendInfo>> selectedFriendInfo =
        new State<>(new ArrayList<>());

    public List<FriendInfo> selectedFriendInfoV3 = new ArrayList<>();

    public int page = 0;
    public int pageSize = 20;
    //已读Ids
    public List<String> hasReadIDList = new ArrayList<>();
    // 是否通过加群验证
    public State<Boolean> isAgreeVerify = new State<>(false);


    public void getConversationInfo() {
        OpenIMClient.getInstance().conversationManager.getOneConversation(new IMUtil.IMCallBack<ConversationInfo>() {
            @Override
            public void onSuccess(ConversationInfo data) {
                if (null != data)
                    conversationInfo.setValue(data);
            }
        }, groupId, ConversationType.SUPER_GROUP_CHAT);
    }

    /**
     * 获取群组信息
     */
    public void getGroupsInfo() {
        List<String> groupIds = new ArrayList<>(); // 群ID集合
        groupIds.add(groupId);
        OpenIMClient.getInstance().groupManager.getGroupsInfo(new IMUtil.IMCallBack<List<GroupInfo>>() {
            @Override
            public void onSuccess(List<GroupInfo> data) {
                if (data.isEmpty()) return;
                groupsInfo.setValue(data.get(0));
            }
        }, groupIds);

        OpenIMClient.getInstance().groupManager.isJoinGroup(groupId, new OnBase<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                isJoinGroup.setValue(data);
            }
        });
    }

    public void getMyMemberInfo() {
        try {
            OpenIMClient.getInstance().groupManager.getGroupMembersInfo(new IMUtil.IMCallBack<List<GroupMembersInfo>>() {
                @Override
                public void onSuccess(List<GroupMembersInfo> data) {
                    if (data.isEmpty()) return;
                    updateRole(data.get(0));
                }
            }, groupId, new ArrayList<>(Collections.singleton(BaseApp
                .inst().loginCertificate.userID)));
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    public void updateRole(GroupMembersInfo data) {
        isOwnerOrAdmin.setValue(data.getRoleLevel()
            != GroupRole.MEMBER);
        isOwner.setValue(data.getRoleLevel()
            == GroupRole.OWNER);
    }

    /**
     * 创建群组
     */
    public void createGroup(String faceURL) {
        WaitDialog waitDialog = new WaitDialog(getContext());
        waitDialog.setNotDismiss();
        waitDialog.show();

        LoginCertificate loginCertificate = LoginCertificate.getCache(getContext());
        List<String> memberUserIDs = new ArrayList<>();
        for (FriendInfo friendInfo : selectedFriendInfo.getValue()) {
            if (!friendInfo.getUserID().equals(loginCertificate.userID)) {
                memberUserIDs.add(friendInfo.getUserID());
            }
        }
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setFaceURL(faceURL);
        groupInfo.setGroupName(groupName.getValue());
        groupInfo.setGroupType(GroupType.WORK);
        OpenIMClient.getInstance().groupManager.createGroup(memberUserIDs, null, groupInfo,
            loginCertificate.userID, new OnBase<GroupInfo>() {
                @Override
                public void onError(int code, String error) {
                    waitDialog.dismiss();
                    getIView().onError(error);
                }

                @Override
                public void onSuccess(GroupInfo data) {
                    Easy.delete(SelectTargetVM.class);
                    getIView().onSuccess(data);
                    Common.UIHandler.postDelayed(waitDialog::dismiss, 200);
                }
            });
    }

    public void selectMute(int status) {
        muteStatus.setValue(status);
    }

    /**
     * 更新群信息
     *
     * @param groupID      群ID
     * @param groupName    群名称
     * @param faceURL      群icon
     * @param notification 群公告
     * @param introduction 群简介
     * @param ex           其他信息
     */
    public void UPDATEGroup(String groupID, String groupName, String faceURL, String notification
        , String introduction, String ex) {

        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupID(groupID);
        groupInfo.setGroupName(groupName);
        groupInfo.setFaceURL(faceURL);
        groupInfo.setNotification(notification);
        groupInfo.setIntroduction(introduction);
        groupInfo.setEx(ex);
        OpenIMClient.getInstance().groupManager.setGroupInfo(groupInfo, new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().onError(error);
            }

            @Override
            public void onSuccess(String data) {
                if (!TextUtils.isEmpty(groupName)) {
                    Obs.newMessage(Constants.Event.UPDATE_GROUP_INFO, groupName);
                }
                if (!TextUtils.isEmpty(notification)) {
                    Obs.newMessage(Constants.Event.SET_GROUP_NOTIFICATION);
                }
                getIView().onSuccess(data);
                getGroupsInfo();
            }
        });
    }

    /**
     * 修改所在群的昵称
     *
     * @param gid           群ID
     * @param uid           群成员userID
     * @param groupNickname 群内显示名称
     */
    public void setGroupMemberNickname(String gid, String uid, String groupNickname) {
        SetGroupMemberInfo info = new SetGroupMemberInfo();
        info.setGroupID(gid);
        info.setUserID(uid);
        info.setNickname(groupNickname);
        OpenIMClient.getInstance().groupManager.setGroupMemberInfo(info, new IMUtil.IMCallBack<String>() {
            @Override
            public void onSuccess(String data) {
                getIView().onSuccess(data);
                getGroupsInfo();
            }
        });
    }

    public void getSuperGroupMemberList() {
        int start = page * pageSize;
        OpenIMClient.getInstance().groupManager.getGroupMemberList(new OnBase<List<GroupMembersInfo>>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + "-" + code);
            }

            @Override
            public void onSuccess(List<GroupMembersInfo> data) {
                if (data.isEmpty()) return;
                superGroupMembers.val().addAll(getExGroupMemberInfos(data));
                superGroupMembers.update();
            }


        }, groupId, 0, start, pageSize);
    }

    public void buildOwnSelect() {
        ExGroupMemberInfo own = new ExGroupMemberInfo();
        own.groupMembersInfo = new GroupMembersInfo();
        own.groupMembersInfo.setUserID(BaseApp.inst().loginCertificate.userID);
        int index = superGroupMembers.getValue().indexOf(own);
        if (index > -1) {
            ExGroupMemberInfo exGroupMemberInfo = superGroupMembers.getValue().get(index);
            exGroupMemberInfo.isSelect = true;
            exGroupMemberInfo.isEnabled = false;
        }
    }

    @NonNull
    private List<ExGroupMemberInfo> getExGroupMemberInfos(List<GroupMembersInfo> data) {
        List<ExGroupMemberInfo> exGroupMemberInfos = new ArrayList<>();
        for (GroupMembersInfo datum : data) {
            ExGroupMemberInfo exGroupMemberInfo = new ExGroupMemberInfo();
            exGroupMemberInfo.groupMembersInfo = datum;
            exGroupMemberInfos.add(exGroupMemberInfo);
        }
        return exGroupMemberInfos;
    }

    List<String> getNextHasReadIds() {
        int start = page * pageSize;
        if (start > hasReadIDList.size()) return null;
        int end = start + pageSize;
        if (end > hasReadIDList.size()) end = hasReadIDList.size();
        return hasReadIDList.subList(start, end);
    }

    public void loadHasReadGroupMembersInfo() {
        List<String> ids = getNextHasReadIds();
        if (null == ids) return;
        getGroupMembersInfo(ids, true);
    }

    public void getGroupMembersInfo(List<String> ids, boolean isAddUp) {
        OpenIMClient.getInstance().groupManager.getGroupMembersInfo(new OnBase<List<GroupMembersInfo>>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(List<GroupMembersInfo> data) {
                if (data.isEmpty()) return;
                if (isAddUp) {
                    superGroupMembers.getValue().addAll(getExGroupMemberInfos(data));
                    superGroupMembers.setValue(superGroupMembers.getValue());
                } else
                    superGroupMembers.setValue(getExGroupMemberInfos(data));
            }
        }, groupId, ids);
    }

    public void getGroupMemberList() {
        getGroupMemberList(pageSize);
    }

    /**
     * 获取群成员信息
     */
    public void getGroupMemberList(int count) {

        exGroupMembers.getValue().clear();
        exGroupManagement.getValue().clear();
        groupLetters.getValue().clear();
        OpenIMClient.getInstance().groupManager.getGroupMemberList(new OnBase<List<GroupMembersInfo>>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + "-" + code);
            }

            @Override
            public void onSuccess(List<GroupMembersInfo> data) {
                if (data.isEmpty()) return;
                groupMembers.setValue(data);

                List<ExGroupMemberInfo> exGroupMemberInfos = new ArrayList<>();
                List<ExGroupMemberInfo> otGroupMemberInfos = new ArrayList<>();
                for (GroupMembersInfo datum : data) {
                    ExGroupMemberInfo exGroupMemberInfo = new ExGroupMemberInfo();
                    exGroupMemberInfo.groupMembersInfo = datum;
                    if (datum.getRoleLevel() != GroupRole.MEMBER) {
                        //群管理单独存放
                        exGroupMemberInfo.sortLetter = "";
                        exGroupManagement.getValue().add(exGroupMemberInfo);
                        continue;
                    }
                    String nickName = "0";
                    if (!TextUtils.isEmpty(datum.getNickname())) nickName = datum.getNickname();
                    String letter = Pinyin.toPinyin(nickName.charAt(0));
                    letter = (letter.charAt(0) + "").trim().toUpperCase();
                    if (!Common.isAlpha(letter)) {
                        exGroupMemberInfo.sortLetter = "#";
                        otGroupMemberInfos.add(exGroupMemberInfo);
                    } else {
                        exGroupMemberInfo.sortLetter = letter;
                        exGroupMemberInfos.add(exGroupMemberInfo);
                    }
                    if (!groupLetters.getValue().contains(exGroupMemberInfo.sortLetter))
                        groupLetters.getValue().add(exGroupMemberInfo.sortLetter);
                }

                Collections.sort(groupLetters.getValue(), new LettersPinyinComparator());
                groupLetters.getValue().add(0, "↑");
                groupLetters.setValue(groupLetters.getValue());

                exGroupMembers.getValue().addAll(exGroupMemberInfos);
                exGroupMembers.getValue().addAll(otGroupMemberInfos);

                Collections.sort(exGroupMembers.getValue(), new PinyinComparator());
                Collections.sort(exGroupManagement.getValue(), (o1, o2) -> {
                    if (o2.groupMembersInfo.getRoleLevel() < o1.groupMembersInfo.getRoleLevel())
                        return 0;
                    return -1;
                });
                exGroupMembers.setValue(exGroupMembers.getValue());
            }
        }, groupId, 0, 0, count);
    }

    private void joinToGroup(List<String> userIds) {
        OpenIMClient.getInstance().groupManager
            .inviteUserToGroup(new OnBase<String>() {
                @Override
                public void onError(int code, String error) {
                    getIView().toast(error);
                }

                @Override
                public void onSuccess(String data) {
                    toast(BaseApp.inst().getString(io.openim.android.ouicore.R.string.Invitation_succeeded));
                    getGroupMemberList();
                    getIView().onSuccess(null);

                    Obs.newMessage(Constants.Event.UPDATE_GROUP_INFO, groupName);
                    postSubject(Constants.Event.UPDATE_GROUP_INFO + "");
                }
            }, groupId, userIds, "welcome");
    }

    private void filterUserIds(List<String> userIds) {
        OpenIMClient.getInstance().groupManager.getUsersInGroup(new OnBase<List<String>>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error);
            }

            @Override
            public void onSuccess(List<String> data) {
                userIds.removeAll(data);
                if (userIds.size() > 0) {
                    joinToGroup(userIds);
                }
            }
        }, groupId, userIds);
    }

    /**
     * 邀请入群
     */
    public void inviteUserToGroup(List<String> userIds) {
        filterUserIds(userIds);
    }

    /**
     * 踢出群
     */
    public void kickGroupMember(List<String> uids) {
        OpenIMClient.getInstance().groupManager.kickGroupMember(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error);
            }

            @Override
            public void onSuccess(String data) {
                getIView().toast(getContext().getString(io.openim.android.ouicore.R.string.kicked_out));
                getGroupMemberList();
                getIView().onSuccess(null);

                Obs.newMessage(Constants.Event.UPDATE_GROUP_INFO, groupName);
                postSubject(Constants.Event.UPDATE_GROUP_INFO+"");
            }
        }, groupId, uids, "");
    }

    /**
     * 获取userID在群里的ExGroupMemberInfo 对象
     *
     * @return
     */
    public ExGroupMemberInfo getOwnInGroup(String userID) {
        ExGroupMemberInfo memberInfo = new ExGroupMemberInfo();
        GroupMembersInfo groupMembersInfo = new GroupMembersInfo();
        groupMembersInfo.setUserID(userID);
        memberInfo.groupMembersInfo = groupMembersInfo;
        int index = exGroupManagement.getValue().indexOf(memberInfo);
        if (index != -1) return exGroupManagement.getValue().get(index);

        index = exGroupMembers.getValue().indexOf(memberInfo);
        if (index != -1) return exGroupMembers.getValue().get(index);
        return null;
    }


    public void getGroupMembersInfo(OnBase<List<GroupMembersInfo>> onBase, List<String> uIds) {
        OpenIMClient.getInstance().groupManager.getGroupMembersInfo(onBase, groupId, uIds);
    }

    public void dissolveGroup() {
        CommonDialog commonDialog = new CommonDialog(getContext());
        commonDialog.show();
        commonDialog.getMainView().tips.setText(io.openim.android.ouicore.R.string.dissolve_tips);
        commonDialog.getMainView().confirm.setTextColor(getContext().getResources().getColor(R.color.theme));
        commonDialog.getMainView().cancel.setOnClickListener(v -> commonDialog.dismiss());
        commonDialog.getMainView().confirm.setOnClickListener(v -> OpenIMClient.getInstance().groupManager.dismissGroup(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                getIView().toast(getContext().getString(io.openim.android.ouicore.R.string.dissolve_tips2));
                Obs.newMessage(Constants.Event.DISSOLVE_GROUP);
                close(commonDialog);
            }
        }, groupId));

    }

    public void quitGroup() {
        CommonDialog commonDialog = new CommonDialog(getContext());
        commonDialog.show();
        commonDialog.getMainView().tips.setText(io.openim.android.ouicore.R.string.quit_group_tips);
        commonDialog.getMainView().cancel.setOnClickListener(v -> commonDialog.dismiss());
        commonDialog.getMainView().confirm.setTextColor(getContext().getResources().getColor(R.color.theme));
        commonDialog.getMainView().confirm.setOnClickListener(v -> OpenIMClient.getInstance().groupManager.quitGroup(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                getIView().toast(getContext().getString(io.openim.android.ouicore.R.string.quit_group_tips2));
                close(commonDialog);

            }
        }, groupId));
    }

    public void transferGroupOwner(String uid, IMUtil.OnSuccessListener OnSuccessListener) {
        OpenIMClient.getInstance().groupManager.transferGroupOwner(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                getGroupsInfo();
                OnSuccessListener.onSuccess(data);
            }
        }, groupId, uid);
    }


    private void close(CommonDialog commonDialog) {
        IConversationBridge iConversationBridge =
            (IConversationBridge) ARouter.getInstance().build(Routes.Service.CONVERSATION).navigation();
        iConversationBridge.deleteConversationFromLocalAndSvr(groupId);
        iConversationBridge.closeChatPage();
        commonDialog.dismiss();
        getIView().close();
    }

    public void changeGroupMute(boolean isChecked, IMUtil.OnSuccessListener OnSuccessListener) {
        OpenIMClient.getInstance().groupManager.changeGroupMute(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                if (null != OnSuccessListener) OnSuccessListener.onSuccess(data);
            }
        }, groupId, isChecked);
    }

    public void setGroupVerification(int needVerification,
                                     IMUtil.OnSuccessListener OnSuccessListener) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupID(groupId);
        groupInfo.setNeedVerification(needVerification);
        OpenIMClient.getInstance().groupManager.setGroupInfo(groupInfo, new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                if (null != OnSuccessListener) OnSuccessListener.onSuccess(data);
            }
        });
    }

    public void setGroupLookMemberInfo(int status, IMUtil.OnSuccessListener OnSuccessListener) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupID(groupId);
        groupInfo.setLookMemberInfo(status);
        OpenIMClient.getInstance().groupManager.setGroupInfo(groupInfo, new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                if (null != OnSuccessListener) OnSuccessListener.onSuccess(data);
            }
        });
    }

    public void setGroupApplyMemberFriend(int status, IMUtil.OnSuccessListener OnSuccessListener) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupID(groupId);
        groupInfo.setApplyMemberFriend(status);
        OpenIMClient.getInstance().groupManager.setGroupInfo(groupInfo, new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().toast(error + code);
            }

            @Override
            public void onSuccess(String data) {
                if (null != OnSuccessListener) OnSuccessListener.onSuccess(data);
            }
        });
    }

    public void changeGroupMemberMute(IMBack<String> imBack, String uid, long seconds) {
        OpenIMClient.getInstance().groupManager.changeGroupMemberMute(imBack, groupId, uid,
            seconds);
    }

    public long setMemberMute(IMBack<String> imBack, String uid, long seconds) {
        int status = muteStatus.getValue();
        if (status == -1 || (status == 0 && seconds == 0)) {
            getIView().toast(BaseApp.inst().getString(R.string.mute_tips));
            return 0;
        }
        if (status == 1) {
            seconds = 60 * 10;
        }
        if (status == 2) {
            seconds = 60 * 60;
        }
        if (status == 3) {
            seconds = 60 * 60 * 12;
        }
        if (status == 4) {
            seconds = 60 * 60 * 24;
        }
        if (status == 5) {
            seconds = 0;
        }
        changeGroupMemberMute(imBack, uid, seconds);
        return seconds;
    }

}
