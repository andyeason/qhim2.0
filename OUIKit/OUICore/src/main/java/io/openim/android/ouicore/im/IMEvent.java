package io.openim.android.ouicore.im;


import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import io.openim.android.ouicore.R;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.data.DataManager;
import io.openim.android.ouicore.utils.L;
import io.openim.android.ouicore.vm.SocialityVM;
import io.openim.android.ouicore.vm.UserLogic;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.enums.ConversationType;
import io.openim.android.sdk.enums.MessageType;
import io.openim.android.sdk.listener.OnAdvanceMsgListener;
import io.openim.android.sdk.listener.OnConnListener;
import io.openim.android.sdk.listener.OnConversationListener;
import io.openim.android.sdk.listener.OnFriendshipListener;
import io.openim.android.sdk.listener.OnGroupListener;
import io.openim.android.sdk.listener.OnUserListener;
import io.openim.android.sdk.models.BlacklistInfo;
import io.openim.android.sdk.models.C2CReadReceiptInfo;
import io.openim.android.sdk.models.ConversationInfo;
import io.openim.android.sdk.models.CustomSignalingInfo;
import io.openim.android.sdk.models.FriendApplicationInfo;
import io.openim.android.sdk.models.FriendInfo;
import io.openim.android.sdk.models.GroupApplicationInfo;
import io.openim.android.sdk.models.GroupInfo;
import io.openim.android.sdk.models.GroupMembersInfo;
import io.openim.android.sdk.models.GroupMessageReceipt;
import io.openim.android.sdk.models.KeyValue;
import io.openim.android.sdk.models.MeetingStreamEvent;
import io.openim.android.sdk.models.Message;
import io.openim.android.sdk.models.RevokedInfo;
import io.openim.android.sdk.models.RoomCallingInfo;
import io.openim.android.sdk.models.SignalingInfo;
import io.openim.android.sdk.models.UserInfo;
import io.openim.android.sdk.models.UsersOnlineStatus;

///im事件 统一处理
public class IMEvent {
    private static final String TAG = "IMEvent";
    private static IMEvent listener = null;
    private List<OnConnListener> connListeners;
    private List<OnUserListener> userListeners;
    private List<OnAdvanceMsgListener> advanceMsgListeners;
    private List<OnConversationListener> conversationListeners;
    private List<OnGroupListener> groupListeners;
    private List<OnFriendshipListener> friendshipListeners;

    public void init() {
        connListeners = new ArrayList<>();
        userListeners = new ArrayList<>();
        advanceMsgListeners = new ArrayList<>();
        conversationListeners = new ArrayList<>();
        groupListeners = new ArrayList<>();
        friendshipListeners = new ArrayList<>();

        userListener();
        advanceMsgListener();
        friendshipListener();
        conversationListener();
        groupListeners();
    }

    public static synchronized IMEvent getInstance() {
        if (null == listener) listener = new IMEvent();
        return listener;
    }

    //连接事件
    public void addConnListener(OnConnListener onConnListener) {
        if (!connListeners.contains(onConnListener)) {
            connListeners.add(onConnListener);
        }
    }

    public void removeConnListener(OnConnListener onConnListener) {
        connListeners.remove(onConnListener);
    }

    // 会话新增或改变监听
    public void addConversationListener(OnConversationListener onConversationListener) {
        if (!conversationListeners.contains(onConversationListener)) {
            conversationListeners.add(onConversationListener);
        }
    }

    public void removeConversationListener(OnConversationListener onConversationListener) {
        conversationListeners.remove(onConversationListener);
    }

    // 收到新消息，已读回执，消息撤回监听。
    public void addAdvanceMsgListener(OnAdvanceMsgListener onAdvanceMsgListener) {
        if (!advanceMsgListeners.contains(onAdvanceMsgListener)) {
            advanceMsgListeners.add(onAdvanceMsgListener);
        }
    }

    public void removeAdvanceMsgListener(OnAdvanceMsgListener onAdvanceMsgListener) {
        advanceMsgListeners.remove(onAdvanceMsgListener);
    }

    // 群组关系发生改变监听
    public void addGroupListener(OnGroupListener onGroupListener) {
        if (!groupListeners.contains(onGroupListener)) {
            groupListeners.add(onGroupListener);
        }
    }

    public void removeGroupListener(OnGroupListener onGroupListener) {
        groupListeners.remove(onGroupListener);
    }

    // 好友关系发生改变监听
    public void addFriendListener(OnFriendshipListener onFriendshipListener) {
        if (!friendshipListeners.contains(onFriendshipListener)) {
            friendshipListeners.add(onFriendshipListener);
        }
    }

    public void removeFriendListener(OnFriendshipListener onFriendshipListener) {
        friendshipListeners.remove(onFriendshipListener);
    }


    //连接事件
    public OnConnListener connListener = new OnConnListener() {

        @Override
        public void onConnectFailed(int code, String error) {
            // 连接服务器失败，可以提示用户当前网络连接不可用
            L.d(TAG, "连接服务器失败(" + error + ")");
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onConnectFailed(code, error);
            }
        }

        @Override
        public void onConnectSuccess() {
            // 已经成功连接到服务器
            L.d(TAG, "已经成功连接到服务器");
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onConnectSuccess();
            }
        }

        @Override
        public void onConnecting() {
            // 正在连接到服务器，适合在 UI 上展示“正在连接”状态。
            L.d(TAG, "正在连接到服务器...");
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onConnecting();
            }
        }

        @Override
        public void onKickedOffline() {
            // 当前用户被踢下线，此时可以 UI 提示用户“您已经在其他端登录了当前账号，是否重新登录？”
            L.d(TAG, "当前用户被踢下线");
            Toast.makeText(BaseApp.inst(),
                BaseApp.inst().getString(io.openim.android.ouicore.R.string.kicked_offline_tips),
                Toast.LENGTH_SHORT).show();
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onKickedOffline();
            }
        }

        @Override
        public void onUserTokenExpired() {
            // 登录票据已经过期，请使用新签发的 UserSig 进行登录。
            L.d(TAG, "登录票据已经过期");
            Toast.makeText(BaseApp.inst(),
                BaseApp.inst().getString(io.openim.android.ouicore.R.string.token_expired),
                Toast.LENGTH_SHORT).show();
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onUserTokenExpired();
            }
        }

        @Override
        public void onUserTokenInvalid(String reason) {
            Toast.makeText(BaseApp.inst(),
                BaseApp.inst().getString(R.string.token_invalid),
                Toast.LENGTH_SHORT).show();
            for (OnConnListener onConnListener : connListeners) {
                onConnListener.onUserTokenInvalid(reason);
            }
        }
    };


    // 群组关系发生改变监听
    private void groupListeners() {
        OpenIMClient.getInstance().groupManager.setOnGroupListener(new OnGroupListener() {
            @Override
            public void onGroupApplicationAccepted(GroupApplicationInfo info) {
                // 发出或收到的组申请被接受
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupApplicationAccepted(info);
                }
            }

            @Override
            public void onGroupApplicationAdded(GroupApplicationInfo info) {
                // 发出或收到的组申请有新增
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupApplicationAdded(info);
                }
            }

            @Override
            public void onGroupApplicationDeleted(GroupApplicationInfo info) {
                // 发出或收到的组申请被删除
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupApplicationDeleted(info);
                }
            }

            @Override
            public void onGroupApplicationRejected(GroupApplicationInfo info) {
                // 发出或收到的组申请被拒绝
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupApplicationRejected(info);
                }
            }

            @Override
            public void onGroupDismissed(GroupInfo info) {
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupDismissed(info);
                }
            }

            @Override
            public void onGroupInfoChanged(GroupInfo info) {
                // 组资料变更
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupInfoChanged(info);
                }
            }

            @Override
            public void onGroupMemberAdded(GroupMembersInfo info) {
                // 组成员进入
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupMemberAdded(info);
                }
            }

            @Override
            public void onGroupMemberDeleted(GroupMembersInfo info) {
                // 组成员退出
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupMemberDeleted(info);
                }
            }

            @Override
            public void onGroupMemberInfoChanged(GroupMembersInfo info) {
                // 组成员信息发生变化
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onGroupMemberInfoChanged(info);
                }
            }

            @Override
            public void onJoinedGroupAdded(GroupInfo info) {
                // 创建群： 初始成员收到；邀请进群：被邀请者收到
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onJoinedGroupAdded(info);
                }
            }

            @Override
            public void onJoinedGroupDeleted(GroupInfo info) {
                // 退出群：退出者收到；踢出群：被踢者收到
                for (OnGroupListener onGroupListener : groupListeners) {
                    onGroupListener.onJoinedGroupDeleted(info);
                }
            }
        });
    }

    // 会话新增或改变监听
    private void conversationListener() {
        OpenIMClient.getInstance().conversationManager.setOnConversationListener(new OnConversationListener() {

            @Override
            public void onConversationChanged(List<ConversationInfo> list) {
                // 已添加的会话发生改变
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onConversationChanged(list);
                }
            }

            @Override
            public void onNewConversation(List<ConversationInfo> list) {
                // 新增会话
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onNewConversation(list);
                }
            }

            @Override
            public void onSyncServerFailed(boolean reinstalled) {
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onSyncServerFailed(reinstalled);
                }
            }

            @Override
            public void onSyncServerFinish(boolean reinstalled) {
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onSyncServerFinish(reinstalled);
                }
                Log.d(TAG, "onSyncServerFinish reinstalled:" + reinstalled);
                DataManager.getInstance().getAllGroup();
                DataManager.getInstance().getAllFriend();
                DataManager.getInstance().getTotalUnreadMsgCount();
                DataManager.getInstance().getAllConversations();
            }

            @Override
            public void onSyncServerProgress(long progress) {
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onSyncServerProgress(progress);
                }
            }

            @Override
            public void onSyncServerStart(boolean reinstalled) {
                Log.d(TAG, "onSyncServerStart reinstalled:" + reinstalled);
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onSyncServerStart(reinstalled);
                }
            }

            @Override
            public void onTotalUnreadMessageCountChanged(int i) {
                // 未读消息数发送变化
                for (OnConversationListener onConversationListener : conversationListeners) {
                    onConversationListener.onTotalUnreadMessageCountChanged(i);
                }
            }
        });
    }

    public void promptSoundOrNotification(Message msg) {
        if (BaseApp.inst().loginCertificate.globalRecvMsgOpt == 2
            || msg.getContentType() == MessageType.TYPING ||
            Objects.equals(msg.getSendID(), BaseApp.inst().loginCertificate.userID)) return;
        try {
            if (Easy.find(UserLogic.class).connectStatus.val()
                == UserLogic.ConnectStatus.SYNCING) return;
        } catch (Exception ignore) {}

        String sourceID = msg.getSessionType() == ConversationType.SINGLE_CHAT
            ? msg.getSendID() : msg.getGroupID();

        if (!TextUtils.isEmpty(sourceID)) {
            OpenIMClient.getInstance().conversationManager
                .getOneConversation(new IMUtil.IMCallBack<ConversationInfo>() {
                @Override
                public void onSuccess(ConversationInfo data) {
                    if (null == data) return;
                    if (isDeviceLocked(BaseApp.inst())) return;
                    if (data.getRecvMsgOpt() == 0) {
                        if (BaseApp.inst().isAppBackground.val())
                            IMUtil.sendNotice(msg.getClientMsgID().hashCode());
                        else {
                            if (BaseApp.inst().loginCertificate.allowBeep)
                                IMUtil.playPrompt();
                            if (BaseApp.inst().loginCertificate.allowVibration)
                                IMUtil.vibrate(200);
                        }
                    }
                }
            }, sourceID, msg.getSessionType());
        }
    }

    // 好关系发生变化监听
    private void friendshipListener() {
        OpenIMClient.getInstance().friendshipManager.setOnFriendshipListener(new OnFriendshipListener() {
            @Override
            public void onBlacklistAdded(BlacklistInfo u) {
                // 拉入黑名单
            }

            @Override
            public void onBlacklistDeleted(BlacklistInfo u) {
                // 从黑名单删除
            }

            @Override
            public void onFriendApplicationAccepted(FriendApplicationInfo u) {
                // 发出或收到的好友申请已同意
                for (OnFriendshipListener friendshipListener : friendshipListeners) {
                    friendshipListener.onFriendApplicationAccepted(u);
                }
            }

            @Override
            public void onFriendApplicationAdded(FriendApplicationInfo u) {
                // 发出或收到的好友申请被添加
                for (OnFriendshipListener friendshipListener : friendshipListeners) {
                    friendshipListener.onFriendApplicationAdded(u);
                }
            }

            @Override
            public void onFriendApplicationDeleted(FriendApplicationInfo u) {
                // 发出或收到的好友申请被删除
                for (OnFriendshipListener friendshipListener : friendshipListeners) {
                    friendshipListener.onFriendApplicationDeleted(u);
                }
            }

            @Override
            public void onFriendApplicationRejected(FriendApplicationInfo u) {
                // 发出或收到的好友申请被拒绝
                for (OnFriendshipListener friendshipListener : friendshipListeners) {
                    friendshipListener.onFriendApplicationRejected(u);
                }
            }

            @Override
            public void onFriendInfoChanged(FriendInfo u) {
                // 朋友的资料发生变化
            }

            @Override
            public void onFriendAdded(FriendInfo u) {
                // 好友被添加
            }

            @Override
            public void onFriendDeleted(FriendInfo u) {
                // 好友被删除
            }
        });
    }

    // 收到新消息，已读回执，消息撤回监听。
    private void advanceMsgListener() {
        OpenIMClient.getInstance().messageManager.setAdvancedMsgListener(new OnAdvanceMsgListener() {
            @Override
            public void onRecvNewMessage(Message msg) {
                Log.d(TAG, "onRecvNewMessage, advanceMsgListeners:" + advanceMsgListeners);
                if (!IMUtil.isSignalingMsg(msg)){
                    promptSoundOrNotification(msg);
                }
                // 收到新消息，界面添加新消息
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvNewMessage(msg);
                }
            }

            @Override
            public void onRecvC2CReadReceipt(List<C2CReadReceiptInfo> list) {
                // 消息被阅读回执，将消息标记为已读
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvC2CReadReceipt(list);
                }
            }

            @Override
            public void onRecvMessageRevokedV2(RevokedInfo info) {
                // 消息成功撤回，从界面移除消息
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvMessageRevokedV2(info);
                }
            }

            @Override
            public void onRecvMessageExtensionsChanged(String msgID, List<KeyValue> list) {
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvMessageExtensionsChanged(msgID, list);
                }
            }

            @Override
            public void onRecvMessageExtensionsDeleted(String msgID, List<String> list) {
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvMessageExtensionsDeleted(msgID, list);
                }
            }

            @Override
            public void onRecvMessageExtensionsAdded(String msgID, List<KeyValue> list) {
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvMessageExtensionsAdded(msgID, list);
                }
            }

            @Override
            public void onMsgDeleted(Message message) {
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onMsgDeleted(message);
                }
            }

            @Override
            public void onRecvOfflineNewMessage(List<Message> msg) {
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvOfflineNewMessage(msg);
                }
            }

            @Override
            public void onRecvGroupMessageReadReceipt(GroupMessageReceipt groupMessageReceipt) {
                // 消息被阅读回执，将消息标记为已读
                Log.d(TAG, "onRecvGroupMessageReadReceipt, advanceMsgListeners:" + advanceMsgListeners);
                for (OnAdvanceMsgListener onAdvanceMsgListener : advanceMsgListeners) {
                    onAdvanceMsgListener.onRecvGroupMessageReadReceipt(groupMessageReceipt);
                }
            }
        });
    }

    /**
     * 用户状态变化
     *
     * @param onUserListener
     */
    public void removeUserListener(OnUserListener onUserListener) {
        userListeners.remove(onUserListener);
    }

    public void addUserListener(OnUserListener onUserListener) {
        if (!userListeners.contains(onUserListener)) userListeners.add(onUserListener);
    }

    // 用户资料变更监听
    private void userListener() {
        OpenIMClient.getInstance().userInfoManager.setOnUserListener(new OnUserListener() {
            private final  UserLogic userLogic = Easy.find(UserLogic.class);
            @Override
            public void onSelfInfoUpdated(UserInfo info) {
                userLogic.info.setValue(info);
                // 当前登录用户资料变更回调
                for (OnUserListener userListener : userListeners) {
                    userListener.onSelfInfoUpdated(info);
                }
            }

            @Override
            public void onUserStatusChanged(UsersOnlineStatus onlineStatus) {
                for (OnUserListener userListener : userListeners) {
                    userListener.onUserStatusChanged(onlineStatus);
                }
            }
        });
    }

    private boolean isDeviceLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return keyguardManager.isDeviceLocked();
        } else {
            return keyguardManager.isKeyguardLocked();
        }
    }
}


