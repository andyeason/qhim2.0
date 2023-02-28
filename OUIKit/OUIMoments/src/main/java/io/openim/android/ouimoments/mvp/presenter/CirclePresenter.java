package io.openim.android.ouimoments.mvp.presenter;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.openim.android.ouicore.base.BaseApp;
import io.openim.android.ouicore.entity.UserLabel;
import io.openim.android.ouicore.net.RXRetrofit.N;
import io.openim.android.ouicore.net.RXRetrofit.NetObserver;
import io.openim.android.ouicore.net.RXRetrofit.Parameter;
import io.openim.android.ouicore.net.bage.GsonHel;
import io.openim.android.ouicore.services.NiService;
import io.openim.android.ouicore.services.OneselfService;
import io.openim.android.ouicore.utils.Common;
import io.openim.android.ouicore.utils.Constant;
import io.openim.android.ouicore.utils.L;
import io.openim.android.ouicore.utils.TimeUtil;
import io.openim.android.ouimoments.bean.CircleItem;
import io.openim.android.ouimoments.bean.Comment;
import io.openim.android.ouimoments.bean.CommentConfig;
import io.openim.android.ouimoments.bean.CommentItem;
import io.openim.android.ouimoments.bean.FavortItem;
import io.openim.android.ouimoments.bean.MomentsBean;
import io.openim.android.ouimoments.bean.MomentsContent;
import io.openim.android.ouimoments.bean.MomentsData;
import io.openim.android.ouimoments.bean.MomentsMeta;
import io.openim.android.ouimoments.bean.MomentsUser;
import io.openim.android.ouimoments.bean.PhotoInfo;
import io.openim.android.ouimoments.bean.User;
import io.openim.android.ouimoments.bean.WorkMoments;
import io.openim.android.ouimoments.listener.IDataRequestListener;
import io.openim.android.ouimoments.mvp.contract.CircleContract;
import io.openim.android.ouimoments.mvp.modle.CircleModel;
import io.openim.android.ouimoments.utils.DatasUtil;
import io.openim.android.sdk.OpenIMClient;
import io.openim.android.sdk.listener.OnBase;
import io.openim.android.sdk.listener.OnWorkMomentsListener;
import okhttp3.ResponseBody;

/**
 * @author yiw
 * @ClassName: CirclePresenter
 * @Description: 通知model请求服务器和通知view更新
 * @date 2015-12-28 下午4:06:03
 */
public class CirclePresenter implements CircleContract.Presenter {

    private static final String TAG = "---CirclePresenter---";
    public final static int TYPE_PULLREFRESH = 1;
    public final static int TYPE_UPLOADREFRESH = 2;
    public static int pageIndex = 1, pageSize = 20;
    private CircleModel circleModel;
    private CircleContract.View view;
    public String unReadCount;

    /**
     * momentsUserID 不为null，表示获取指定用户的朋友圈数据
     */
    public User user;

    /**
     * 是否是获取指定用户的朋友圈数据
     *
     * @return
     */
    public boolean isSpecifiedUser() {
        return null != user && !TextUtils.isEmpty(user.getId());
    }

    public CirclePresenter(CircleContract.View view) {
        circleModel = new CircleModel();
        this.view = view;

        OpenIMClient.getInstance().workMomentsManager.setWorkMomentsListener(this::getWorkMomentsUnReadCount);
    }

    public void getWorkMomentsUnReadCount() {
        OpenIMClient.getInstance().workMomentsManager.getWorkMomentsUnReadCount(new OnBase<String>() {
            @Override
            public void onError(int code, String error) {
            }

            @Override
            public void onSuccess(String data) {
                try {
                    Map map = JSONObject.parseObject(data, Map.class);
                    int size = (int) map.get("unreadCount");
                    if (size > 0) {
                        unReadCount = String.valueOf(size);
                        view.updateAdapterIndex(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //    String? userID
    public void loadData(int loadType) {
        loadData(loadType, isSpecifiedUser() ? user.getId() : null);
    }

    public void loadData(int loadType, String userID) {
        if (loadType == TYPE_PULLREFRESH) pageIndex = 1;
        else pageIndex++;

        Parameter parameter = NiService.buildParameter().add("pageNumber", pageIndex).add(
            "showNumber", pageSize).add("userID", userID);
        NetObserver<MomentsBean> netObserver = new NetObserver<MomentsBean>(TAG) {
            @Override
            public void onSuccess(MomentsBean o) {
                try {
                    packInContent(o);
                    List<CircleItem> circleData = packInCircleData(o);

                    if (view != null) {
                        view.update2loadData(loadType, circleData);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                view.showError(e.getMessage());
                view.setRefreshing(false);
            }
        };
        if (TextUtils.isEmpty(userID)) {
            N.API(NiService.class).CommNI(Constant.getImApiUrl() + "office" +
                    "/get_user_friend_work_moments", BaseApp.inst().loginCertificate.imToken,
                parameter.buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(MomentsBean.class)).subscribe(netObserver);
        } else {
            N.API(NiService.class).CommNI(Constant.getImApiUrl() + "office/get_user_work_moments"
                , BaseApp.inst().loginCertificate.imToken, parameter.buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(MomentsBean.class)).subscribe(netObserver);
        }
    }

    /**
     * 封装CircleData
     *
     * @param o
     */
    private List<CircleItem> packInCircleData(MomentsBean o) {
        List<CircleItem> circleItems = new ArrayList<>();
        for (WorkMoments workMoment : o.workMoments) {
            CircleItem item = getPackInCircleData(workMoment);
            circleItems.add(item);
        }
        return circleItems;
    }

    @NonNull
    private CircleItem getPackInCircleData(WorkMoments workMoment) {
        CircleItem item = new CircleItem();
        item.setType(workMoment.momentsContents.type == 0 ? "2" : "3");
        item.setUser(new User(workMoment.userID, workMoment.userName, workMoment.faceURL));
        item.setId(workMoment.workMomentID);
        item.setPermission(workMoment.permission);
        item.setPermissionUsers(workMoment.permissionUsers);
        if (null != workMoment.atUsers && !workMoment.atUsers.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (MomentsUser atUser : workMoment.atUsers) {
                stringBuilder.append(atUser.userName);
                stringBuilder.append("、");
            }
            item.setAtUsers(stringBuilder.substring(0, stringBuilder.length() - 1));
        }
        item.setContent(workMoment.momentsContents.text);
        item.setCreateTime(TimeUtil.getTime(workMoment.createTime * 1000L,
            TimeUtil.monthTimeFormat));

        List<FavortItem> favortItems = new ArrayList<>();
        for (MomentsUser likeUser : workMoment.likeUsers) {
            FavortItem favortItem = new FavortItem();
            favortItem.setId(likeUser.userID);
            favortItem.setUser(new User(likeUser.userID, likeUser.userName, ""));
            favortItems.add(favortItem);
        }
        item.setFavorters(favortItems);

        List<CommentItem> commentItems = new ArrayList<>();
        for (Comment comment : workMoment.comments) {
            CommentItem commentItem = new CommentItem();
            replaceUser(comment, commentItem);
            commentItem.setId(comment.contentID);
            commentItem.setContent(comment.content);
            commentItems.add(commentItem);
        }
        item.setComments(commentItems);

        List<PhotoInfo> photos = new ArrayList<>();
        for (MomentsMeta meta : workMoment.momentsContents.metas) {
            if (item.getType().equals(CircleItem.TYPE_VIDEO)) {
                item.setVideoUrl(meta.original);
                item.setVideoImgUrl(meta.thumb);
            } else {
                PhotoInfo photoInfo = new PhotoInfo();
                photoInfo.url = meta.original;
                photos.add(photoInfo);
            }
        }
        if (!photos.isEmpty()) item.setPhotos(photos);
        return item;
    }

    /**
     * 如果userID是自己 则替换
     *
     * @param comment
     * @param commentItem
     */
    private void replaceUser(Comment comment, CommentItem commentItem) {
        if (comment.userID.equals(DatasUtil.curUser.getId()))
            commentItem.setUser(DatasUtil.curUser);
        else commentItem.setUser(new User(comment.userID, comment.userName, ""));
        if (comment.replyUserID.equals(DatasUtil.curUser.getId()))
            commentItem.setToReplyUser(DatasUtil.curUser);
        else commentItem.setToReplyUser(new User(comment.replyUserID, comment.replyUserName, ""));
    }

    /**
     * 解析Content 并赋值给momentsContents
     */
    public void packInContent(MomentsBean momentsBean) {
        try {
            for (WorkMoments workMoment : momentsBean.workMoments) {
                packInContent(workMoment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void packInContent(WorkMoments workMoment) {
        Map map = JSONObject.parseObject(workMoment.content, Map.class);
        JsonElement string = JsonParser.parseString((String) map.get("data"));
        MomentsData momentsContent = GsonHel.fromJson(string.toString(), MomentsData.class);
        workMoment.momentsContents = momentsContent.data;
    }


    /**
     * @param circleId
     * @return void    返回类型
     * @throws
     * @Title: deleteCircle
     * @Description: 删除动态
     */
    public void deleteCircle(final String circleId) {
        N.API(NiService.class).CommNI(Constant.getImApiUrl() + "office/delete_one_work_moment",
            BaseApp.inst().loginCertificate.imToken, NiService.buildParameter().add("workMomentID"
                , circleId).buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(Object.class)).subscribe(new NetObserver<Object>(TAG) {
            @Override
            public void onSuccess(Object o) {
                if (view != null) {
                    view.update2DeleteCircle(circleId);
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                if (view != null) {
                    view.showError(e.getMessage());
                }
            }
        });
    }

    /**
     * @param circlePosition
     * @return void    返回类型
     * @throws
     * @Title: addFavort
     * @Description: 点赞
     */
    public void addFavort(final int circlePosition, final String mFavorId) {
        favortorDeleteFavort(circlePosition, mFavorId, true);
    }

    void favortorDeleteFavort(final int circlePosition, final String mFavorId, Boolean isStar) {
        N.API(NiService.class).CommNI(Constant.getImApiUrl() + "/office/like_one_work_moment",
            BaseApp.inst().loginCertificate.imToken, NiService.buildParameter().add("workMomentID"
                , mFavorId).buildJsonBody()).map(OneselfService.turn(Object.class)).compose(N.IOMain()).subscribe(new NetObserver<Object>(TAG) {
            @Override
            public void onSuccess(Object o) {
                if (isStar) {
                    FavortItem favortItem = new FavortItem();
                    favortItem.setId(BaseApp.inst().loginCertificate.userID);
                    favortItem.setUser(new User(BaseApp.inst().loginCertificate.userID,
                        BaseApp.inst().loginCertificate.nickname,
                        BaseApp.inst().loginCertificate.faceURL));
                    view.update2AddFavorite(circlePosition, favortItem);
                } else {
                    view.update2DeleteFavort(circlePosition,
                        BaseApp.inst().loginCertificate.userID);
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                view.showError(e.getMessage());
            }
        });
    }

    /**
     * @param @param circlePosition
     * @param @param favortId
     * @return void    返回类型
     * @throws
     * @Title: deleteFavort
     * @Description: 取消点赞
     */
    public void deleteFavort(final int circlePosition, final String favortId) {
        favortorDeleteFavort(circlePosition, favortId, false);
    }

    /**
     * @param content
     * @param config  CommentConfig
     * @return void    返回类型
     * @throws
     * @Title: addComment
     * @Description: 增加评论
     */
    public void addComment(final String content, final CommentConfig config) {
        if (config == null) {
            return;
        }
        N.API(NiService.class).CommNI(Constant.getImApiUrl() + "/office/comment_one_work_moment",
            BaseApp.inst().loginCertificate.imToken, NiService.buildParameter().add("workMomentID"
                , config.momentID).add("replyUserID", null == config.replyUser ? "" :
                config.replyUser.getId()).add("content", content).buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(Object.class)).subscribe(new NetObserver<Object>(TAG) {
            @Override
            public void onSuccess(Object o) {
                CommentItem newItem = null;
                if (config.commentType == CommentConfig.Type.PUBLIC) {
                    newItem = DatasUtil.createPublicComment(content);
                } else if (config.commentType == CommentConfig.Type.REPLY) {
                    newItem = DatasUtil.createReplyComment(config.replyUser, content);
                }
                if (view != null) {
                    view.update2AddComment(config.circlePosition, newItem);
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                view.showError(e.getMessage());
            }
        });
    }

    /**
     * @param @param circlePosition
     * @param @param commentId
     * @return void    返回类型
     * @throws
     * @Title: deleteComment
     * @Description: 删除评论
     */
    public void deleteComment(String momentID, final int circlePosition, final String commentId) {
        N.API(NiService.class).CommNI(Constant.getImApiUrl() + "office/delete_comment",
            BaseApp.inst().loginCertificate.imToken, NiService.buildParameter().add("workMomentID"
                , momentID).add("contentID", commentId).buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(Object.class)).subscribe(new NetObserver<Object>(TAG) {
            @Override
            public void onSuccess(Object o) {
                if (view != null) {
                    view.update2DeleteComment(circlePosition, commentId);
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                view.showError(e.getMessage());
            }
        });
    }

    /**
     * @param commentConfig
     */
    public void showEditTextBody(CommentConfig commentConfig) {
        if (view != null) {
            view.updateEditTextBodyVisible(View.VISIBLE, commentConfig);
        }
    }

    public void getMomentsDetail(String momentID) {
        N.API(NiService.class).CommNI(Constant.getImApiUrl() + "/office/get_work_moment_by_id",
            BaseApp.inst().loginCertificate.imToken, NiService.buildParameter().add("workMomentID"
                , momentID).buildJsonBody()).compose(N.IOMain()).map(OneselfService.turn(WorkMoments.class)).subscribe(new NetObserver<WorkMoments>(TAG) {
            @Override
            public void onSuccess(WorkMoments o) {
                try {
                    WorkMoments workMoment = o.workMoment;
                    packInContent(workMoment);
                    List<CircleItem> circleItems = new ArrayList<>();
                    circleItems.add(getPackInCircleData(workMoment));

                    if (view != null) {
                        view.update2loadData(0, circleItems);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            protected void onFailure(Throwable e) {
                view.showError(e.getMessage());
            }
        });
    }

    /**
     * 清除对外部对象的引用，反正内存泄露。
     */
    public void recycle() {
        N.clearDispose(TAG);
        OpenIMClient.getInstance().workMomentsManager.setWorkMomentsListener(null);
        this.view = null;
    }


}