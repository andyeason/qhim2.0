package io.openim.android.ouiconversation.adapter;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.airbnb.lottie.LottieAnimationView;
import com.alibaba.android.arouter.launcher.ARouter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openim.android.ouiconversation.R;


import io.openim.android.ouiconversation.databinding.LayoutLoadingSmallBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgAudioLeftBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgAudioRightBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgExMenuBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgImgLeftBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgImgRightBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgNoticeLeftBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgTxtLeftBinding;
import io.openim.android.ouiconversation.databinding.LayoutMsgTxtRightBinding;
import io.openim.android.ouiconversation.ui.ChatActivity;
import io.openim.android.ouiconversation.ui.PreviewMediaActivity;
import io.openim.android.ouiconversation.vm.CustomEmojiVM;
import io.openim.android.ouiconversation.widget.SendStateView;
import io.openim.android.ouiconversation.vm.ChatVM;
import io.openim.android.ouiconversation.ui.fragment.InputExpandFragment;
import io.openim.android.ouicore.adapter.RecyclerViewAdapter;
import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.entity.CallHistory;
import io.openim.android.ouicore.entity.MsgExpand;
import io.openim.android.ouicore.ex.AtUser;
import io.openim.android.ouicore.im.IMUtil;
import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.GetFilePathFromUri;
import io.openim.android.ouicore.utils.L;
import io.openim.android.ouicore.utils.MediaFileUtil;
import io.openim.android.ouicore.utils.OnDedrepClickListener;
import io.openim.android.ouicore.utils.Routes;
import io.openim.android.ouicore.utils.TimeUtil;
import io.openim.android.ouicore.vm.ForwardVM;
import io.openim.android.ouicore.vm.SelectTargetVM;
import io.openim.android.ouicore.vm.PreviewMediaVM;
import io.openim.android.ouicore.voice.SPlayer;
import io.openim.android.ouicore.voice.listener.PlayerListener;
import io.openim.android.ouicore.voice.player.SMediaPlayer;
import io.openim.android.ouicore.widget.AvatarImage;
import io.openim.android.ouicore.widget.PlaceHolderDrawable;
import io.openim.android.sdk.enums.ConversationType;
import io.openim.android.sdk.enums.MessageStatus;
import io.openim.android.sdk.enums.MessageType;
import io.openim.android.sdk.models.AttachedInfoElem;
import io.openim.android.sdk.models.Message;
import io.openim.android.sdk.models.QuoteElem;
import io.openim.android.sdk.models.VideoElem;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.plugins.RxJavaPlugins;

public class MessageViewHolder {
    public static RecyclerView.ViewHolder createViewHolder(@NonNull ViewGroup parent,
                                                           int viewType) {
        if (viewType == Constants.LOADING) return new LoadingView(parent);
        if (viewType == MessageType.TEXT) return new TXTView(parent);
        if (viewType == MessageType.PICTURE || viewType == MessageType.CUSTOM_FACE)
            return new IMGView(parent);
        if (viewType == MessageType.VOICE) return new AudioView(parent);
        if (viewType == MessageType.VIDEO) return new VideoView(parent);
        if (viewType == MessageType.OA_NTF) return new NotificationItemView(parent);
        if (viewType >= MessageType.NTF_BEGIN) return new NoticeView(parent);
        if (viewType == Constants.MsgType.LOCAL_CALL_HISTORY) return new CallHistoryView(parent);
        if (viewType == MessageType.QUOTE) return new QuoteTXTView(parent);

        return new TXTView(parent);
    }

    public abstract static class MsgViewHolder extends RecyclerView.ViewHolder {
        protected RecyclerView recyclerView;
        protected MessageAdapter messageAdapter;

        protected Message message;
        protected ChatVM chatVM = BaseApp.inst().getVMByCache(ChatVM.class);

        private boolean leftIsInflated = false, rightIsInflated = false;
        private final ViewStub right;
        private final ViewStub left;

        public MsgViewHolder(ViewGroup itemView) {
            super(buildRoot(itemView));
            left = this.itemView.findViewById(R.id.left);
            right = this.itemView.findViewById(R.id.right);

            left.setOnInflateListener((stub, inflated) -> leftIsInflated = true);
            right.setOnInflateListener((stub, inflated) -> rightIsInflated = true);
        }

        public static View buildRoot(ViewGroup parent) {
            return LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_msg, parent,
                false);
        }

        protected abstract int getLeftInflatedId();

        protected abstract int getRightInflatedId();

        protected abstract void bindLeft(View itemView, Message message);

        protected abstract void bindRight(View itemView, Message message);

        /**
         * 是否是自己发的消息
         */
        protected boolean isOwn = false;

        //绑定数据
        public void bindData(Message message, int position) {
            this.message = message;
            try {
                isOwn = getSendWay(message);
                if (isOwn) {
                    if (leftIsInflated) left.setVisibility(View.GONE);
                    if (rightIsInflated) right.setVisibility(View.VISIBLE);
                    if (!rightIsInflated) {
                        right.setLayoutResource(getRightInflatedId());
                        right.inflate();
                    }
                    bindRight(itemView, message);
                } else {
                    if (leftIsInflated) left.setVisibility(View.VISIBLE);
                    if (rightIsInflated) right.setVisibility(View.GONE);
                    if (!leftIsInflated) {
                        left.setLayoutResource(getLeftInflatedId());
                        left.inflate();
                    }
                    bindLeft(itemView, message);
                }
                unifiedProcess(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected boolean getSendWay(Message message) {
            return message.getSendID().equals(BaseApp.inst().loginCertificate.userID);
        }

        /**
         * 统一处理
         */
        protected void unifiedProcess(int position) {
            MsgExpand msgExpand = (MsgExpand) message.getExt();
            hFirstItem(position);

            hAvatar();
            hName();
            showTime(msgExpand);
            hMultipleChoice(msgExpand);
            hSendState();
        }

        public void hFirstItem(int position) {
            View root = itemView.findViewById(R.id.root);
            root.setPadding(0, position == 0 ? Common.dp2px(15) : 0, 0, 0);
        }

        /**
         * 处理发送状态
         */
        private void hSendState() {
            if (isOwn) {
                SendStateView sendStateView = itemView.findViewById(R.id.sendState2);
                if (null == sendStateView) return;
                sendStateView.setOnClickListener(new OnDedrepClickListener() {
                    @Override
                    public void click(View v) {
                        chatVM.sendMsg(message, true);
                    }
                });
            }
        }

        /**
         * 处理多选
         */
        private void hMultipleChoice(MsgExpand msgExpand) {
            CheckBox checkBox = itemView.findViewById(R.id.choose);
            if (null == checkBox) return;
            if (null != chatVM.enableMultipleSelect.getValue() && chatVM.enableMultipleSelect.getValue() && message.getContentType() < MessageType.NTF_BEGIN) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(msgExpand.isChoice);
                checkBox.setOnClickListener((buttonView) -> msgExpand.isChoice =
                    checkBox.isChecked());
            } else {
                checkBox.setVisibility(View.GONE);
            }
            ((LinearLayout.LayoutParams) checkBox.getLayoutParams()).topMargin =
                msgExpand.isShowTime ? Common.dp2px(15) : 0;
        }

        /**
         * 处理名字
         */
        @SuppressLint("SetTextI18n")
        public void hName() {
            TextView nickName;
            if (isOwn) nickName = itemView.findViewById(R.id.nickName2);
            else nickName = itemView.findViewById(R.id.nickName);
            if (null != nickName) {
                nickName.setVisibility(View.VISIBLE);
                nickName.setMaxLines(1);
                nickName.setMaxEms(18);
                nickName.setEllipsize(TextUtils.TruncateAt.MIDDLE);

                boolean isSending = message.getStatus() == MessageStatus.SENDING;
                String time = TimeUtil.getTimeString(isSending ? System.currentTimeMillis() :
                    message.getSendTime());
                if (isSending || message.getSessionType() == ConversationType.SINGLE_CHAT) {
                    nickName.setText(time);
                } else nickName.setText(message.getSenderNickname() + "  " + time);
            }
        }

        /**
         * 处理头像
         */
        public void hAvatar() {
            AvatarImage avatarImage = itemView.findViewById(R.id.avatar);
            AvatarImage avatarImage2 = itemView.findViewById(R.id.avatar2);
            if (null != avatarImage) {
                avatarImage.load(message.getSenderFaceUrl(), message.getSenderNickname());
                AtomicBoolean isLongClick = new AtomicBoolean(false);
                avatarImage.setOnLongClickListener(v -> {
                    if (chatVM.isSingleChat) return false;
                    isLongClick.set(true);
                    List<AtUser> atUsers = chatVM.atUsers.val();
                    AtUser atUser = new AtUser(message.getSendID());
                    if (atUsers.contains(atUser)) {
                        return false;
                    }
                    atUser.name = message.getSenderNickname();
                    chatVM.atUsers.val().add(atUser);
                    chatVM.atUsers.update();
                    return false;
                });
                avatarImage.setOnClickListener(v -> {
                    if (isLongClick.get()) {
                        isLongClick.set(false);
                        return;
                    }
                    ARouter.getInstance().build(Routes.Main.PERSON_DETAIL).withString(Constants.K_ID, message.getSendID()).withString(Constants.K_GROUP_ID, message.getGroupID()).navigation();
                });
            }
            if (null != avatarImage2) {
                avatarImage2.load(message.getSenderFaceUrl(), message.getSenderNickname());
                avatarImage2.setOnClickListener(v -> ARouter.getInstance().build(Routes.Main.PERSON_DETAIL).withString(Constants.K_ID, message.getSendID()).withString(Constants.K_GROUP_ID, message.getGroupID()).navigation());
            }
        }

        private void showTime(MsgExpand msgExpand) {
            TextView notice = itemView.findViewById(R.id.notice);
            if (msgExpand.isShowTime) {
                //显示时间
                String time = TimeUtil.getTimeString(message.getSendTime());
                notice.setVisibility(View.VISIBLE);
                notice.setText(time);
            } else notice.setVisibility(View.GONE);
        }


        /**
         * 处理at
         *
         * @return true 处理了
         */
        protected boolean handleSequence(TextView showView, Message message) {
            MsgExpand msgExpand = (MsgExpand) message.getExt();
            if (null != msgExpand.sequence) {
                showView.setText(msgExpand.sequence);
                showView.setMovementMethod(CustomLinkMovementMethod.getInstance());
                return true;
            }
            return false;
        }


        public void bindRecyclerView(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        public void setMessageAdapter(MessageAdapter messageAdapter) {
            this.messageAdapter = messageAdapter;
        }

        public void toPreview(View view, String url, String firstFrameUrl) {
            toPreview(view, url, firstFrameUrl, false);
        }

        /**
         * 预览图片或视频
         *
         * @param view
         * @param url           地址
         * @param firstFrameUrl 缩略图
         */
        public void toPreview(View view, String url, String firstFrameUrl, boolean isSingle) {
            view.setOnClickListener(v -> {
                PreviewMediaVM previewMediaVM = Easy.installVM(PreviewMediaVM.class);
                if (isSingle || message.getContentType() == MessageType.CUSTOM_FACE) {
                    PreviewMediaVM.MediaData mediaData =
                        new PreviewMediaVM.MediaData(message.getClientMsgID());
                    mediaData.mediaUrl = url;
                    mediaData.isVideo = MediaFileUtil.isVideoType(url);
                    mediaData.thumbnail = firstFrameUrl;
                    previewMediaVM.previewSingle(mediaData);
                } else
                    previewMediaVM.previewMultiple(chatVM.mediaDataList, message.getClientMsgID());
                view.getContext().startActivity(new Intent(view.getContext(),
                    PreviewMediaActivity.class));
//                }
            });
        }
    }


    //加载中...
    public static class LoadingView extends RecyclerView.ViewHolder {
        public LoadingView(ViewGroup parent) {
            super(LayoutLoadingSmallBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false).getRoot());
        }
    }

    //通知消息
    public static class NoticeView extends MessageViewHolder.MsgViewHolder {

        public NoticeView(ViewGroup itemView) {
            super(itemView);
        }

        @SuppressLint({"SetTextI18n", "StringFormatInvalid"})
        @Override
        public void bindData(Message message, int position) {
            hFirstItem(position);
            TextView textView = itemView.findViewById(R.id.notice);
            textView.setVisibility(View.VISIBLE);

            MsgExpand msgExpand = (MsgExpand) message.getExt();
            textView.setText(msgExpand.tips);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }


        @Override
        protected int getLeftInflatedId() {
            return 0;
        }

        @Override
        protected int getRightInflatedId() {
            return 0;
        }

        @Override
        protected void bindLeft(View itemView, Message message) {

        }

        @Override
        protected void bindRight(View itemView, Message message) {

        }
    }

    //文本消息
    public static class TXTView extends MessageViewHolder.MsgViewHolder {

        public TXTView(ViewGroup parent) {
            super(parent);
        }

        @Override
        protected int getLeftInflatedId() {
            return R.layout.layout_msg_txt_left;
        }

        @Override
        protected int getRightInflatedId() {
            return R.layout.layout_msg_txt_right;
        }

        @Override
        protected void bindLeft(View itemView, Message message) {
            LayoutMsgTxtLeftBinding v = LayoutMsgTxtLeftBinding.bind(itemView);
            v.avatar.load(message.getSenderFaceUrl(), message.getSenderNickname());
            if (handleSequence(v.content, message)) return;
            // 防止错误信息所导致的空指针
            if (message.getContentType() == MessageType.AT_TEXT && message.getTextElem() == null) {
                String content = message.getAtTextElem().getText();
                v.content.setText(content);
                return;
            }
            String content = message.getTextElem().getContent();
            v.content.setText(content);
        }

        @Override
        protected void bindRight(View itemView, Message message) {
            LayoutMsgTxtRightBinding v = LayoutMsgTxtRightBinding.bind(itemView);
            v.avatar2.load(message.getSenderFaceUrl(), message.getSenderNickname());
            v.sendState2.setSendState(message.getStatus());
            if (handleSequence(v.content2, message)) return;
            // 防止错误信息所导致的空指针
            if (message.getContentType() == MessageType.AT_TEXT && message.getTextElem() == null) {
                String content = message.getAtTextElem().getText();
                v.content2.setText(content);
                return;
            }

            String content = message.getTextElem().getContent();
            v.content2.setText(content);
        }
    }

    public static class IMGView extends MessageViewHolder.MsgViewHolder {

        public IMGView(ViewGroup itemView) {
            super(itemView);
        }

        @Override
        protected int getLeftInflatedId() {
            return R.layout.layout_msg_img_left;
        }

        @Override
        protected int getRightInflatedId() {
            return R.layout.layout_msg_img_right;
        }

        @Override
        protected void bindLeft(View itemView, Message message) {
            LayoutMsgImgLeftBinding v = LayoutMsgImgLeftBinding.bind(itemView);

            v.sendState.setSendState(message.getStatus());
            String url = loadIMG(v.content, message);
            toPreview(v.content, url, null);
        }

        private String loadIMG(ImageView img, Message message) {
            String url;
            if (message.getContentType() == MessageType.CUSTOM_FACE) {
                MsgExpand msgExpand = (MsgExpand) message.getExt();
                url = msgExpand.customEmoji.url;
                scale(img, msgExpand.customEmoji.width, msgExpand.customEmoji.height);
                Glide.with(img.getContext()).load(url).fitCenter().transform(new RoundedCorners(15)).placeholder(new PlaceHolderDrawable(BaseApp.inst())).error(io.openim.android.ouicore.R.mipmap.ic_chat_photo).into(img);
            } else {
                url = message.getPictureElem().getSourcePicture().getUrl();
                if (TextUtils.isEmpty(url)) url = message.getPictureElem().getSourcePath();

                int w = message.getPictureElem().getSourcePicture().getWidth();
                int h = message.getPictureElem().getSourcePicture().getHeight();
                scale(img, w, h);
                IMUtil.loadPicture(message.getPictureElem()).fitCenter().transform(new RoundedCorners(15)).into(img);
            }
            return url;
        }

        public void scale(View img, int sourceW, int sourceH) {
            int pictureWidth = Common.dp2px(180);
            int _trulyWidth;
            int _trulyHeight;
            if (sourceW == 0) {
                sourceW = 1;
            }
            if (sourceH == 0) {
                sourceH = 1;
            }
            if (pictureWidth > sourceW) {
                _trulyWidth = sourceW;
                _trulyHeight = sourceH;
            } else {
                _trulyWidth = pictureWidth;
                _trulyHeight = _trulyWidth * sourceH / sourceW;
            }
            ViewGroup.LayoutParams params = img.getLayoutParams();
            params.width = _trulyWidth;
            params.height = _trulyHeight;
            img.setLayoutParams(params);
        }

        @Override
        protected void bindRight(View itemView, Message message) {
            LayoutMsgImgRightBinding v = LayoutMsgImgRightBinding.bind(itemView);
            v.avatar2.load(message.getSenderFaceUrl(), message.getSenderNickname());
            v.videoPlay2.setVisibility(View.GONE);
            v.mask2.setVisibility(View.GONE);

            v.sendState2.setSendState(message.getStatus());
            String url = loadIMG(v.content2, message);
            toPreview(v.content2, url, null);
        }

    }

    public static class AudioView extends MessageViewHolder.MsgViewHolder {
        private Message playingMessage;

        public AudioView(ViewGroup itemView) {
            super(itemView);
        }

        @Override
        public void bindRecyclerView(RecyclerView recyclerView) {
            super.bindRecyclerView(recyclerView);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (null != playingMessage) {
                        int index = messageAdapter.getMessages().indexOf(playingMessage);
                        LinearLayoutManager linearLayoutManager =
                            (LinearLayoutManager) recyclerView.getLayoutManager();
                        int firstVisiblePosition =
                            linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                        int lastVisiblePosition =
                            linearLayoutManager.findLastCompletelyVisibleItemPosition();

                        if (index < firstVisiblePosition || index > lastVisiblePosition) {
                            SPlayer.instance().stop();
                            playingMessage = null;
                        }
                    }

                }
            });
        }

        @Override
        protected int getLeftInflatedId() {
            return R.layout.layout_msg_audio_left;
        }

        @Override
        protected int getRightInflatedId() {
            return R.layout.layout_msg_audio_right;
        }

        @Override
        protected void bindLeft(View itemView, Message message) {
            final LayoutMsgAudioLeftBinding view = LayoutMsgAudioLeftBinding.bind(itemView);
            TextView badge = itemView.findViewById(io.openim.android.ouicore.R.id.badge);
            badge.setVisibility(message.isRead() ? View.GONE : View.VISIBLE);
            view.duration.setText(message.getSoundElem().getDuration() + "``");
            view.content.setOnClickListener(v -> clickPlay(message, view.lottieView));
        }

        @Override
        protected void bindRight(View itemView, Message message) {
            final LayoutMsgAudioRightBinding view = LayoutMsgAudioRightBinding.bind(itemView);
            view.avatar2.load(message.getSenderFaceUrl(), message.getSenderNickname());
            view.sendState2.setSendState(message.getStatus());
            view.duration2.setText(message.getSoundElem().getDuration() + "``");

            view.content2.setOnClickListener(v -> clickPlay(message, view.lottieView2));
        }


        public void clickPlay(Message message, LottieAnimationView lottieView) {
            String sourceUrl = message.getSoundElem().getSourceUrl();
            if (TextUtils.isEmpty(sourceUrl)) return;
            SPlayer.instance().getMediaPlayer();
            if (SPlayer.instance().isPlaying()) {
                SPlayer.instance().stop();
            } else {
                SPlayer.instance().playByUrl(sourceUrl, new PlayerListener() {
                    @Override
                    public void LoadSuccess(SMediaPlayer mediaPlayer) {
                        playingMessage = message;
                        lottieView.playAnimation();
                        mediaPlayer.start();
                    }

                    @Override
                    public void Loading(SMediaPlayer mediaPlayer, int i) {

                    }

                    @Override
                    public void onCompletion(SMediaPlayer mediaPlayer) {
                        mediaPlayer.stop();
                    }

                    @Override
                    public void onError(Exception e) {
                        lottieView.cancelAnimation();
                        lottieView.setProgress(1);
                    }
                });
            }

            SPlayer.instance().getMediaPlayer().setOnPlayStateListener(new SMediaPlayer.OnPlayStateListener() {
                @Override
                public void started() {
                    if (!isOwn) {
                        RxJavaPlugins.setErrorHandler(handler -> {
                            if (handler.getCause() instanceof UndeliverableException) {
                                L.e(handler.getMessage());
                            }
                        });
                        chatVM.markReadWithObservable(message)
                            .subscribe(new DisposableObserver<String>() {
                                @Override
                                public void onNext(String result) {

                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {
                                    try {
                                        if (!message.isRead()) {
                                            message.setRead(true);
                                            message.getAttachedInfoElem().setHasReadTime(System.currentTimeMillis());
                                            Common.UIHandler.post(() -> {
                                                messageAdapter.notifyItemChanged(chatVM.messages.val().indexOf(message));
                                            });
                                        }
                                    }catch (Exception e) {
                                        L.e(e.getMessage());
                                    }
                                }
                            });
                    }
                }

                @Override
                public void paused() {
                }

                @Override
                public void stopped() {
                    lottieView.cancelAnimation();
                    lottieView.setProgress(1);
                }

                @Override
                public void completed() {

                }
            });
        }
    }

    public static class VideoView extends IMGView {

        public VideoView(ViewGroup itemView) {
            super(itemView);
        }

        @Override
        protected void bindRight(View itemView, Message message) {
            LayoutMsgImgRightBinding view = LayoutMsgImgRightBinding.bind(itemView);
            view.sendState2.setSendState(message.getStatus());
            view.mask2.setVisibility(View.VISIBLE);
            view.videoPlay2.setVisibility(View.VISIBLE);

            MsgExpand msgExpand = (MsgExpand) message.getExt();

            int progress = (int) msgExpand.sendProgress;
            view.circleBar2.setTargetProgress(progress);
            boolean sendSuccess = message.getStatus() == MessageStatus.SUCCEEDED;
            if (sendSuccess) view.circleBar2.reset();
            view.mask2.setVisibility(sendSuccess ? View.GONE : View.VISIBLE);

            VideoElem videoElem = message.getVideoElem();
            String secondFormat = TimeUtil.getTime(videoElem.getDuration() * 1000,
                TimeUtil.minuteTimeFormat);
            view.duration2.setText(secondFormat);
            scale((View) view.content2.getParent(), videoElem.getSnapshotWidth(),
                videoElem.getSnapshotHeight());
            scale(view.content2, videoElem.getSnapshotWidth(), videoElem.getSnapshotHeight());
            IMUtil.loadVideoSnapshot(message.getVideoElem()).fitCenter().transform(new RoundedCorners(15)).into(view.content2);
            preview(message, view.videoPlay2);
        }


        private void preview(Message message, View view) {
            String snapshotUrl = message.getVideoElem().getSnapshotUrl();
            toPreview(view, IMUtil.getFastVideoPath(message.getVideoElem()), snapshotUrl);
        }


        @Override
        protected void bindLeft(View itemView, Message message) {
            LayoutMsgImgLeftBinding view = LayoutMsgImgLeftBinding.bind(itemView);

            view.sendState.setSendState(message.getStatus());
            view.playBtn.setVisibility(View.VISIBLE);
            view.circleBar.setVisibility(View.VISIBLE);
            view.durationLeft.setText(TimeUtil.getTime(message.getVideoElem().getDuration() * 1000,
                TimeUtil.minuteTimeFormat));

            int w = message.getVideoElem().getSnapshotWidth();
            int h = message.getVideoElem().getSnapshotHeight();
            scale(view.content, w, h);
            IMUtil.loadVideoSnapshot(message.getVideoElem()).fitCenter().transform(new RoundedCorners(15)).into(view.content);
            preview(message, view.contentGroup);
        }
    }

    public static class NotificationItemView extends MessageViewHolder.MsgViewHolder {


        public NotificationItemView(ViewGroup itemView) {
            super(itemView);
        }

        @Override
        protected int getLeftInflatedId() {
            return R.layout.layout_msg_notice_left;
        }

        @Override
        protected int getRightInflatedId() {
            return 0;
        }

        @Override
        protected void bindLeft(View itemView, Message message) {
            LayoutMsgNoticeLeftBinding v = LayoutMsgNoticeLeftBinding.bind(itemView);
            MsgExpand msgExpand = (MsgExpand) message.getExt();
            v.noticeAvatar.load(msgExpand.oaNotification.notificationFaceURL,
                msgExpand.oaNotification.notificationName);
            v.noticeNickName.setText(msgExpand.oaNotification.notificationName);
            v.title.setText(msgExpand.oaNotification.notificationName);
            v.content.setText(msgExpand.oaNotification.text);
            try {
                if (msgExpand.oaNotification.mixType == 1) {
                    v.picture.setVisibility(View.VISIBLE);
                    Glide.with(v.getRoot().getContext()).load(msgExpand.oaNotification.pictureElem.getBigPicture().getUrl()).into(v.picture);
                } else {
                    v.picture.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void bindRight(View itemView, Message message) {

        }
    }

    public static class CallHistoryView extends AudioView {

        private CallHistory callHistory;
        private MsgExpand msgExpand;
        private boolean isAudio = false;

        public CallHistoryView(ViewGroup parent) {
            super(parent);
        }

        @Override
        public void bindData(Message message, int position) {
            msgExpand = (MsgExpand) message.getExt();
            callHistory = msgExpand.callHistory;
            isAudio = callHistory.getType().equals("audio");
            super.bindData(message, position);

        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void bindLeft(View itemView, Message message) {
            final LayoutMsgAudioLeftBinding v = LayoutMsgAudioLeftBinding.bind(itemView);
            v.lottieView.setImageResource(isAudio ?
                io.openim.android.ouicore.R.mipmap.ic_voice_call :
                io.openim.android.ouicore.R.mipmap.ic_video_call);

            if (callHistory.isSuccess()) v.duration.setText(msgExpand.callDuration);
            else {
                if (callHistory.getFailedState() == 0)
                    v.duration.setText(io.openim.android.ouicore.R.string.conn_failed);
                if (callHistory.getFailedState() == 1)
                    v.duration.setText(io.openim.android.ouicore.R.string.cancelled);
                if (callHistory.getFailedState() == 3)
                    v.duration.setText(io.openim.android.ouicore.R.string.declined);
            }
            v.content.setOnClickListener(v1 -> chatVM.singleChatCall(!isAudio));
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void bindRight(View itemView, Message message) {
            final LayoutMsgAudioRightBinding v = LayoutMsgAudioRightBinding.bind(itemView);
            v.avatar2.load(message.getSenderFaceUrl(), message.getSenderNickname());
            v.sendState2.setSendState(message.getStatus());
            v.lottieView2.setVisibility(View.GONE);
            v.icon2.setVisibility(View.VISIBLE);
            v.icon2.setImageResource(isAudio ? io.openim.android.ouicore.R.mipmap.ic_voice_call :
                io.openim.android.ouicore.R.mipmap.ic_video_call);
            if (callHistory.isSuccess()) v.duration2.setText(msgExpand.callDuration);
            else {
                if (callHistory.getFailedState() == 0)
                    v.duration2.setText(io.openim.android.ouicore.R.string.conn_failed);
                if (callHistory.getFailedState() == 1)
                    v.duration2.setText(io.openim.android.ouicore.R.string.cancelled);
                if (callHistory.getFailedState() == 2)
                    v.duration2.setText(io.openim.android.ouicore.R.string.ot_refuses);
            }
            v.content2.setOnClickListener(v1 -> chatVM.singleChatCall(!isAudio));
        }


    }

    public static class QuoteTXTView extends TXTView {

        public QuoteTXTView(ViewGroup parent) {
            super(parent);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void bindLeft(View itemView, Message message) {
            LayoutMsgTxtLeftBinding v = LayoutMsgTxtLeftBinding.bind(itemView);
            v.quoteLy1.setVisibility(View.VISIBLE);
            QuoteElem quoteElem = message.getQuoteElem();
            if (!handleSequence(v.content, message)) v.content.setText(quoteElem.getText());

            message = quoteElem.getQuoteMessage();
            int contentType = message.getContentType();
            if (contentType == MessageType.REVOKE_MESSAGE_NTF) {
                v.quoteContent1.setText(message.getSenderNickname() + ":" + BaseApp.inst().getString(io.openim.android.ouicore.R.string.quote_delete_tips));
                v.picture1.setVisibility(View.GONE);
                return;
            }
            v.downloadView1.setVisibility(View.GONE);
            if (contentType == MessageType.FILE) {
                v.downloadView1.setVisibility(View.VISIBLE);
                String tips =
                    message.getSenderNickname() + ":" + message.getFileElem().getFileName();
                String path = message.getFileElem().getFilePath();
                boolean isLocal = GetFilePathFromUri.fileIsExists(path);
                if (!isLocal) path = message.getFileElem().getSourceUrl();
                v.downloadView1.setRes(path);
                v.quoteContent1.setText(tips);
                Message finalMessage1 = message;
                v.quoteLy1.setOnClickListener(v1 -> {
                    GetFilePathFromUri.openFile(itemView.getContext(), finalMessage1);
                });
            } else if (contentType == MessageType.CARD
                && null != message.getCardElem()) {
                String tips =
                    message.getSenderNickname() + ":" + IMUtil.getMsgParse(message) + message.getCardElem().getNickname();
                v.quoteContent1.setText(tips);
                String uid = message.getCardElem().getUserID();
                v.quoteLy1.setOnClickListener(v1 -> {
                    ARouter.getInstance().build(Routes.Main.PERSON_DETAIL)
                        .withString(Constants.K_ID, uid).navigation(v.quoteContent1.getContext());
                });
            } else if (contentType == MessageType.TEXT || contentType == MessageType.AT_TEXT) {
                v.quoteContent1.setText(message.getSenderNickname() + ":" + IMUtil.getMsgParse(message));
                v.picture1.setVisibility(View.GONE);
            } else {
                v.picture1.setVisibility(View.VISIBLE);
                v.playBtn1.setVisibility(View.GONE);
                if (contentType == MessageType.PICTURE) {
                    v.quoteContent1.setText(message.getSenderNickname() + ":");
                    IMUtil.loadPicture(message.getPictureElem()).centerCrop().into(v.picture1);
                    toPreview(v.quoteLy1, message.getPictureElem().getSourcePicture().getUrl(),
                        message.getPictureElem().getSnapshotPicture().getUrl(), true);
                } else if (contentType == MessageType.VIDEO) {
                    v.playBtn1.setVisibility(View.VISIBLE);
                    v.quoteContent1.setText(message.getSenderNickname() + ":");
                    IMUtil.loadVideoSnapshot(message.getVideoElem()).centerInside().into(v.picture1);
                    previewVideo(v.quoteLy1, message);
                } else {
                    String content = BaseApp.inst().getString(io.openim.android.ouicore.R.string.unsupported_type);
                    v.quoteContent1.setText(message.getSenderNickname() + ":" + "[" + content + "]");
                    v.picture1.setVisibility(View.GONE);
                }
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void bindRight(View itemView, Message message) {
            LayoutMsgTxtRightBinding v = LayoutMsgTxtRightBinding.bind(itemView);
            v.quoteLy2.setVisibility(View.VISIBLE);
            v.sendState2.setSendState(message.getStatus());
            QuoteElem quoteElem = message.getQuoteElem();
            if (!handleSequence(v.content2, message)) v.content2.setText(quoteElem.getText());

            message = quoteElem.getQuoteMessage();
            int contentType = message.getContentType();
            if (contentType == MessageType.REVOKE_MESSAGE_NTF) {
                v.quoteContent2.setText(message.getSenderNickname() + ":" + BaseApp.inst().getString(io.openim.android.ouicore.R.string.quote_delete_tips));
                v.picture2.setVisibility(View.GONE);
                v.playBtn2.setVisibility(View.GONE);
                return;
            }
            v.downloadView.setVisibility(View.GONE);
            if (contentType == MessageType.FILE) {
                v.downloadView.setVisibility(View.VISIBLE);
                String tips =
                    message.getSenderNickname() + ":" + message.getFileElem().getFileName();
                String path = message.getFileElem().getFilePath();
                boolean isLocal = GetFilePathFromUri.fileIsExists(path);
                if (!isLocal) path = message.getFileElem().getSourceUrl();
                v.downloadView.setRes(path);
                v.quoteContent2.setText(tips);
                Message finalMessage1 = message;
                v.quoteLy2.setOnClickListener(v1 -> {
                    GetFilePathFromUri.openFile(itemView.getContext(), finalMessage1);
                });
            } else if (contentType == MessageType.CARD
                && null != message.getCardElem()) {
                String tips =
                    message.getSenderNickname() + ":" + IMUtil.getMsgParse(message) + message.getCardElem().getNickname();
                v.quoteContent2.setText(tips);
                String uid = message.getCardElem().getUserID();
                v.quoteLy2.setOnClickListener(v1 -> {
                    ARouter.getInstance().build(Routes.Main.PERSON_DETAIL)
                        .withString(Constants.K_ID, uid).navigation(v.quoteContent2.getContext());
                });
            } else if (contentType == MessageType.TEXT
                || contentType == MessageType.AT_TEXT) {
                v.quoteContent2.setText(message.getSenderNickname() + ":" + IMUtil.getMsgParse(message));
                v.picture2.setVisibility(View.GONE);
            } else {
                v.picture2.setVisibility(View.VISIBLE);
                v.playBtn2.setVisibility(View.GONE);
                if (contentType == MessageType.PICTURE) {
                    v.quoteContent2.setText(message.getSenderNickname() + ":" + IMUtil.getMsgParse(message));
                    IMUtil.loadPicture(message.getPictureElem()).centerCrop().into(v.picture2);
                    toPreview(v.quoteLy2, message.getPictureElem().getSourcePicture().getUrl(),
                        message.getPictureElem().getSnapshotPicture().getUrl(), true);
                } else if (contentType == MessageType.VIDEO) {
                    v.playBtn2.setVisibility(View.VISIBLE);
                    v.quoteContent2.setText(message.getSenderNickname() + ":" + IMUtil.getMsgParse(message));
                    IMUtil.loadVideoSnapshot(message.getVideoElem()).centerInside().into(v.picture2);
                    previewVideo(v.quoteLy2, message);
                } else {
                    String content = BaseApp.inst().getString(io.openim.android.ouicore.R.string.unsupported_type);
                    v.quoteContent2.setText(message.getSenderNickname() + ":" + "[" + content + "]");
                    v.picture2.setVisibility(View.GONE);
                }
            }
        }

        private static void previewVideo(View itemView, Message message) {
            itemView.setOnClickListener(new OnDedrepClickListener() {
                @Override
                public void click(View v) {
                    PreviewMediaVM previewMediaVM = Easy.installVM(PreviewMediaVM.class);
                    PreviewMediaVM.MediaData mediaData =
                        new PreviewMediaVM.MediaData(IMUtil.getFastVideoPath(message.getVideoElem()));
                    mediaData.mediaUrl = IMUtil.getFastVideoPath(message.getVideoElem());
                    mediaData.thumbnail = message.getVideoElem().getSnapshotUrl();
                    mediaData.isVideo = true;
                    previewMediaVM.previewSingle(mediaData);
                    itemView.getContext().startActivity(new Intent(itemView.getContext(),
                        PreviewMediaActivity.class));
                }
            });
        }
    }

    private static class CustomLinkMovementMethod extends LinkMovementMethod {
        private static CustomLinkMovementMethod mCustomLinkMovementMethod;
        private long duration = 0L;

        public static CustomLinkMovementMethod getInstance() {
            if (mCustomLinkMovementMethod == null) {
                mCustomLinkMovementMethod = new CustomLinkMovementMethod();
            }
            return mCustomLinkMovementMethod;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                duration = System.currentTimeMillis();
                return super.onTouchEvent(widget, buffer, event);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (System.currentTimeMillis() - duration > 100L) {
                    duration = 0L;
                    widget.performLongClick();
                    return false;
                } else {
                    return super.onTouchEvent(widget, buffer, event);
                }
            } else {
                return super.onTouchEvent(widget, buffer, event);
            }
        }
    }
}
