package io.openim.android.ouimoments.bean;

import java.util.List;
import java.util.Objects;

public class WorkMoments {
    public WorkMoments workMoment;
    public String workMomentID;
    public String userID;
    public MomentsContent content;
    public List<MomentsUser> likeUsers;
    public List<Comment> comments;
    public String faceURL;
    public String nickname;
    public List<MomentsUser> atUsers;
    public List<MomentsUser> permissionUsers;
    public long createTime;
    public int permission;
    public int type;
}

