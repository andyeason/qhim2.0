package io.openim.android.ouicore.vm;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.openim.android.ouicore.R;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.BaseViewModel;
import io.openim.android.ouicore.base.IView;
import io.openim.android.ouicore.base.vm.State;
import io.openim.android.ouicore.entity.MsgConversation;
import io.openim.android.ouicore.im.IMEvent;
import io.openim.android.ouicore.im.IMUtil;
import io.openim.android.ouicore.net.bage.GsonHel;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.listener.OnAdvanceMsgListener;
import io.openim.android.sdk.listener.OnBase;
import io.openim.android.sdk.listener.OnConversationListener;
import io.openim.android.sdk.models.C2CReadReceiptInfo;
import io.openim.android.sdk.models.ConversationInfo;
import io.openim.android.sdk.models.GroupMessageReceipt;
import io.openim.android.sdk.models.KeyValue;
import io.openim.android.sdk.models.Message;
import io.openim.android.sdk.models.RevokedInfo;

public class ContactListVM extends BaseViewModel<ContactListVM.ViewAction> implements OnConversationListener, OnAdvanceMsgListener {
    public State<List<MsgConversation>> conversations = new State<>(new ArrayList<>());


    @Override
    protected void viewCreate() {
        IMEvent.getInstance().addConversationListener(this);
        IMEvent.getInstance().addAdvanceMsgListener(this);
        updateConversation();
    }

    public void deleteConversationAndDeleteAllMsg(String conversationId) {
        OpenIMClient.getInstance().conversationManager
            .deleteConversationAndDeleteAllMsg(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {

            }

            @Override
            public void onSuccess(String data) {
                updateConversation();
            }
        }, conversationId);
    }

    public void updateConversation() {
        OpenIMClient.getInstance().conversationManager.getAllConversationList(new OnBase<List<ConversationInfo>>() {
            @Override
            public void onError(int code, String error) {
                getIView().onErr(error);
            }

            @Override
            public void onSuccess(List<ConversationInfo> data) {
                conversations.val().clear();
                for (ConversationInfo datum : data) {
                    Message msg = null;
                    if (null!=datum.getLatestMsg()){
                        msg = GsonHel.fromJson(datum.getLatestMsg(), Message.class);
                    }
                    MsgConversation msgConversation=new MsgConversation(msg, datum);
                    CharSequence draft=buildDraftMsg(datum);
                    if (null!=draft)
                        msgConversation.lastMsg=draft;
                    conversations.val().add(msgConversation);
                }
                conversations.setValue(conversations.getValue());
            }
        });
    }

    private CharSequence buildDraftMsg(ConversationInfo datum) {
        CharSequence draft= (CharSequence) IMUtil.getDraft(datum.getConversationID())[0];
        if (!TextUtils.isEmpty(draft)){
            String target =
                "[" + BaseApp.inst().getString(R.string.draft) + "]";

            String txt= target+draft;
          return  IMUtil.buildClickAndColorSpannable(new SpannableStringBuilder(txt),
                target, R.color.theme, null);
        }
        return null;
    }


    public void setOneConversationPrivateChat(IMUtil.OnSuccessListener<String> OnSuccessListener,
                                              String cid, boolean isChecked) {
        OpenIMClient.getInstance().conversationManager.setOneConversationPrivateChat(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                getIView().onErr(error);
            }

            @Override
            public void onSuccess(String data) {
                OnSuccessListener.onSuccess(data);
            }
        }, cid, isChecked);
    }

//    /**
//     * 更新常联系
//     *
//     * @param data
//     */
//    private void updateFrequentContacts(List<ConversationInfo> data) {
//        List<UserInfo> uList = new ArrayList<>();
//        for (ConversationInfo datum : data) {
//            if (datum.getConversationType() == ConversationType.SINGLE_CHAT) {
//                UserInfo u = new UserInfo();
//                u.setUserID(datum.getUserID());
//                u.setNickname(datum.getShowName());
//                u.setFaceURL(datum.getFaceURL());
//                uList.add(u);
//            }
//        }
//        if (uList.isEmpty()) return;
//        frequentContacts.setValue(uList.size() > 15 ? uList.subList(0, 15) : uList);
//    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void insertDBContact(List<ConversationInfo> data) {
//        RealmList<UserInfoDB> uList = new RealmList<>();
//        for (ConversationInfo datum : data) {
//            if (datum.getConversationType() == Constants.SessionType.SINGLE_CHAT) {
//                UserInfoDB u = new UserInfoDB();
//                u.setUserID(datum.getUserID());
//                u.setNickname(datum.getShowName());
//                u.setFaceURL(datum.getFaceURL());
//                uList.add(u);
//            }
//        }
//        if (uList.isEmpty()) return;
//        BaseApp.inst().realm.executeTransactionAsync(realm -> {
//            RealmResults<UserInfoDB> realmResults = realm.where(UserInfoDB.class).findAll();
//            if (realmResults.isEmpty()) {
//                realm.insert(uList.size() > 15 ? uList.subList(0, 15) : uList);
//            } else {
//                realm.where(UserInfoDB.class)
//                    .in("userID", (String[]) uList.stream()
//                        .map(UserInfoDB::getUserID)
//                        .distinct().toArray()).findAll().deleteAllFromRealm();
//
//                for (UserInfoDB userInfoDB : uList) {
//                    UserInfoDB task = realm.where(UserInfoDB.class)
//                        .equalTo("userID", userInfoDB.getUserID()).findFirst();
//                    if (null != task)
//                        task.deleteFromRealm();
//                }
//            }
//        });
//    }

    @Override
    public void onConversationChanged(List<ConversationInfo> list) {
        updateConversation();
    }


    private void sortConversation(List<ConversationInfo> list) {
        List<MsgConversation> msgConversations = new ArrayList<>();
        Iterator<MsgConversation> iterator = conversations.val().iterator();
        for (ConversationInfo info : list) {
            msgConversations.add(new MsgConversation(GsonHel.fromJson(info.getLatestMsg(),
                Message.class), info));
            while (iterator.hasNext()) {
                if (iterator.next().conversationInfo.getConversationID()
                    .equals(info.getConversationID()))
                    iterator.remove();
            }
        }
        conversations.val().addAll(msgConversations);
        Collections.sort(conversations.val(), IMUtil.simpleComparator());
        conversations.setValue(conversations.val());
    }

    @Override
    public void onNewConversation(List<ConversationInfo> list) {
        sortConversation(list);
    }

    @Override
    public void onSyncServerFailed() {

    }

    @Override
    public void onSyncServerFinish() {

    }

    @Override
    public void onSyncServerStart() {

    }

    @Override
    public void onTotalUnreadMessageCountChanged(int i) {

    }

    @Override
    public void onRecvNewMessage(Message msg) {

    }

    @Override
    public void onRecvC2CReadReceipt(List<C2CReadReceiptInfo> list) {

    }

    @Override
    public void onRecvGroupMessageReadReceipt(GroupMessageReceipt receipt) {

    }

    @Override
    public void onRecvMessageRevokedV2(RevokedInfo info) {

    }

    @Override
    public void onRecvMessageExtensionsChanged(String msgID, List<KeyValue> list) {

    }

    @Override
    public void onRecvMessageExtensionsDeleted(String msgID, List<String> list) {

    }

    @Override
    public void onRecvMessageExtensionsAdded(String msgID, List<KeyValue> list) {

    }

    @Override
    public void onMsgDeleted(Message message) {

    }

    @Override
    public void onRecvOfflineNewMessage(List<Message> msg) {

    }

    //置顶/取消置顶 会话
    public void pinConversation(ConversationInfo conversationInfo, boolean isPinned) {
        OpenIMClient.getInstance().conversationManager.pinConversation(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
                toast(error+"("+code+")");
            }

            @Override
            public void onSuccess(String data) {
                conversationInfo.setPinned(isPinned);
            }
        }, conversationInfo.getConversationID(), isPinned);
    }

    public interface ViewAction extends IView {
        void onErr(String msg);
    }
}
