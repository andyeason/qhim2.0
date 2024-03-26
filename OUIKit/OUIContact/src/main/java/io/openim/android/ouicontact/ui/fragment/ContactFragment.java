package io.openim.android.ouicontact.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import io.openim.android.ouicontact.databinding.FragmentContactMainBinding;
import io.openim.android.ouicontact.databinding.ViewContactHeaderBinding;
import io.openim.android.ouicontact.ui.AddRelationActivity;
import io.openim.android.ouicontact.ui.AllFriendActivity;
import io.openim.android.ouicontact.ui.GroupNoticeListActivity;
import io.openim.android.ouicontact.ui.MyGroupActivity;
import io.openim.android.ouicontact.ui.NewFriendActivity;
import io.openim.android.ouicontact.vm.ContactVM;
import io.openim.android.ouicore.base.BaseFragment;
import io.openim.android.ouicore.base.vm.injection.Easy;
import io.openim.android.ouicore.ex.MultipleChoice;
import io.openim.android.ouicore.services.MomentsBridge;
import io.openim.android.ouicore.utils.ActivityManager;
import io.openim.android.ouicore.utils.Constants;
import io.openim.android.ouicore.utils.Routes;
import io.openim.android.ouicore.utils.SinkHelper;
import io.openim.android.ouicore.vm.NotificationVM;
import io.openim.android.ouicore.vm.SelectTargetVM;

@Route(path = Routes.Contact.HOME)
public class ContactFragment extends BaseFragment<ContactVM>  {
    private FragmentContactMainBinding view;
    private ViewContactHeaderBinding header;
    private NotificationVM notificationVM;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        bindVM(ContactVM.class);
         notificationVM=Easy.find(NotificationVM.class);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = FragmentContactMainBinding.inflate(getLayoutInflater());
        header = view.header;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.title.getLayoutParams();
        lp.setMargins(0, SinkHelper.getStatusBarHeight(), 0, 0);
        view.title.setLayoutParams(lp);

        initView();
        click();

        return view.getRoot();
    }


    public ContactVM getVM() {
        return vm;
    }

    private void click() {
        MomentsBridge momentsBridge =
            (MomentsBridge) ARouter.getInstance().build(Routes.Service.MOMENTS).navigation();
        view.header.moments.setVisibility(null==momentsBridge?View.GONE:View.VISIBLE);
        view.header.moments.setOnClickListener(v -> ARouter.getInstance().build(Routes.Moments.HOME).navigation());

        view.addFriend.setOnClickListener(view1 -> startActivity(new Intent(getActivity(), AddRelationActivity.class)));
        view.search.setOnClickListener(view1 -> {
            ARouter.getInstance().build(Routes.Conversation.SEARCH).navigation();
        });

        header.groupNotice.setOnClickListener(v -> {
            notificationVM.clearDot(notificationVM.groupDot);
            startActivity(new Intent(getActivity(), GroupNoticeListActivity.class));
        });

        header.newFriendNotice.setOnClickListener(v -> {
            notificationVM.clearDot(notificationVM.friendDot);
            startActivity(new Intent(getActivity(), NewFriendActivity.class));
        });

        header.myGoodFriend.setOnClickListener(v -> {
            SelectTargetVM vm=Easy.installVM(SelectTargetVM.class)
                .setIntention(SelectTargetVM.Intention.singleSelect);
            vm.setOnFinishListener(() -> {
                    Activity activity =ActivityManager.isExist(AllFriendActivity.class);
                    MultipleChoice target = vm.metaData.val().get(0);

                    ARouter.getInstance().build(Routes.Main.PERSON_DETAIL)
                        .withString(Constants.K_ID, target.key)
                        .navigation(activity, 1001);

                });
            startActivity(new Intent(getActivity(), AllFriendActivity.class));
        });

        header.myGroup.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MyGroupActivity.class));
        });
//        header.labelLy.setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), LabelActivity.class));
//        });
    }


    private void initView() {
        notificationVM.groupDot.observe(getActivity(), v -> {
            header.badge.badge.setVisibility(v.size()==0 ? View.GONE : View.VISIBLE);
            header.badge.badge.setText(v.size() + "");
        });
        notificationVM.friendDot.observe(getActivity(), v -> {
            header.newFriendNoticeBadge.badge.setVisibility(v.size()==0 ? View.GONE : View.VISIBLE);
            header.newFriendNoticeBadge.badge.setText(v.size() + "");
        });

      notificationVM.momentsUnread.observe(getActivity(),v->
            header.newMomentsMsg.badge.setVisibility(v == 0 ? View.GONE : View.VISIBLE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}
