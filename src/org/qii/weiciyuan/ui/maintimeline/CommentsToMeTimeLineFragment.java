package org.qii.weiciyuan.ui.maintimeline;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.AccountBean;
import org.qii.weiciyuan.bean.CommentListBean;
import org.qii.weiciyuan.bean.UnreadBean;
import org.qii.weiciyuan.bean.UserBean;
import org.qii.weiciyuan.bean.android.CommentTimeLineData;
import org.qii.weiciyuan.bean.android.TimeLinePosition;
import org.qii.weiciyuan.dao.destroy.DestroyCommentDao;
import org.qii.weiciyuan.dao.maintimeline.ICommentsTimeLineDao;
import org.qii.weiciyuan.dao.maintimeline.MainCommentsTimeLineDao;
import org.qii.weiciyuan.support.database.CommentsTimeLineDBTask;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.GlobalContext;
import org.qii.weiciyuan.support.utils.Utility;
import org.qii.weiciyuan.ui.actionmenu.CommentFloatingMenu;
import org.qii.weiciyuan.ui.actionmenu.CommentSingleChoiceModeListener;
import org.qii.weiciyuan.ui.adapter.CommentListAdapter;
import org.qii.weiciyuan.ui.basefragment.AbstractTimeLineFragment;
import org.qii.weiciyuan.ui.interfaces.ICommander;
import org.qii.weiciyuan.ui.interfaces.IRemoveItem;
import org.qii.weiciyuan.ui.loader.CommentsToMeDBLoader;
import org.qii.weiciyuan.ui.main.MainTimeLineActivity;

/**
 * User: qii
 * Date: 12-7-29
 */
public class CommentsToMeTimeLineFragment extends AbstractTimeLineFragment<CommentListBean> implements IRemoveItem {


    private AccountBean accountBean;
    private UserBean userBean;
    private String token;

    private RemoveTask removeTask;

    private CommentListBean bean = new CommentListBean();
    private UnreadBean unreadBean;
    private TimeLinePosition timeLinePosition;


    @Override
    public CommentListBean getList() {
        return bean;
    }

    public CommentsToMeTimeLineFragment() {

    }

    public CommentsToMeTimeLineFragment(AccountBean accountBean, UserBean userBean, String token) {
        this.accountBean = accountBean;
        this.userBean = userBean;
        this.token = token;
    }

    public void setCurrentGroupId(int positoin) {

    }

    protected void clearAndReplaceValue(CommentListBean value) {
        getList().getItemList().clear();
        getList().getItemList().addAll(value.getItemList());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("account", accountBean);
        outState.putSerializable("bean", bean);
        outState.putSerializable("userBean", userBean);
        outState.putString("token", token);

        outState.putSerializable("unreadBean", unreadBean);
        outState.putSerializable("timeLinePosition", timeLinePosition);

    }


    public void refreshUnread(UnreadBean unreadBean) {

//        Activity activity = getActivity();
//        if (activity != null) {
//            if (unreadBean == null) {
//                activity.getActionBar().getTabAt(2).setText(getString(R.string.comments));
//                return;
//            }
//            this.unreadBean = unreadBean;
//            String number = Utility.buildTabText(unreadBean.getMention_cmt() + unreadBean.getCmt());
//            if (!TextUtils.isEmpty(number))
//                activity.getActionBar().getTabAt(2).setText(getString(R.string.comments) + number);
//        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onListViewScrollStop() {
        super.onListViewScrollStop();
        timeLinePosition = Utility.getCurrentPositionFromListView(getListView());
    }

    @Override
    public void onPause() {
        super.onPause();
        CommentsTimeLineDBTask.asyncUpdatePosition(timeLinePosition, accountBean.getUid());
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
//        if (isVisible() && isVisibleToUser) {
//            if (getActivity().getActionBar().getTabAt(0).getText().toString().contains(")")) {
//                getPullToRefreshListView().startRefreshNow();
//            }
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        commander = ((MainTimeLineActivity) getActivity()).getBitmapDownloader();

        switch (getCurrentState(savedInstanceState)) {
            case FIRST_TIME_START:
                getLoaderManager().initLoader(0, null, dbCallback);
                break;
            case ACTIVITY_DESTROY_AND_CREATE:
                userBean = (UserBean) savedInstanceState.getSerializable("userBean");
                accountBean = (AccountBean) savedInstanceState.getSerializable("account");
                token = savedInstanceState.getString("token");
                timeLinePosition = (TimeLinePosition) savedInstanceState.getSerializable("timeLinePosition");
                unreadBean = (UnreadBean) savedInstanceState.getSerializable("unreadBean");
                CommentListBean savedBean = (CommentListBean) savedInstanceState.getSerializable("bean");
                if (savedBean != null && savedBean.getSize() > 0) {
                    clearAndReplaceValue(savedBean);
                    timeLineAdapter.notifyDataSetChanged();
                    refreshLayout(getList());
                    setListViewPositionFromPositionsCache();
                    getLoaderManager().destroyLoader(0);
                } else {
                    getLoaderManager().initLoader(0, null, dbCallback);
                }
                break;
        }

        refreshUnread(unreadBean);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemLongClickListener(onItemLongClickListener);
    }

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (position - 1 < getList().getSize() && position - 1 >= 0) {
                if (mActionMode != null) {
                    mActionMode.finish();
                    mActionMode = null;
                    getListView().setItemChecked(position, true);
                    timeLineAdapter.notifyDataSetChanged();
                    mActionMode = getActivity().startActionMode(new CommentSingleChoiceModeListener(getListView(), timeLineAdapter, CommentsToMeTimeLineFragment.this, getList().getItemList().get(position - 1)));
                    return true;
                } else {
                    getListView().setItemChecked(position, true);
                    timeLineAdapter.notifyDataSetChanged();
                    mActionMode = getActivity().startActionMode(new CommentSingleChoiceModeListener(getListView(), timeLineAdapter, CommentsToMeTimeLineFragment.this, getList().getItemList().get(position - 1)));
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    public void removeItem(int position) {
        clearActionMode();
        if (removeTask == null || removeTask.getStatus() == MyAsyncTask.Status.FINISHED) {
            removeTask = new RemoveTask(GlobalContext.getInstance().getSpecialToken(), getList().getItemList().get(position).getId(), position);
            removeTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void removeCancel() {
        clearActionMode();
    }

    class RemoveTask extends MyAsyncTask<Void, Void, Boolean> {

        String token;
        String id;
        int positon;
        WeiboException e;

        public RemoveTask(String token, String id, int positon) {
            this.token = token;
            this.id = id;
            this.positon = positon;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DestroyCommentDao dao = new DestroyCommentDao(token, id);
            try {
                return dao.destroy();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return false;
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            if (Utility.isAllNotNull(getActivity(), this.e)) {
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                ((CommentListAdapter) timeLineAdapter).removeItem(positon);

            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(false);
    }

    private void setListViewPositionFromPositionsCache() {
        if (timeLinePosition != null)
            getListView().setSelectionFromTop(timeLinePosition.position + 1, timeLinePosition.top);
        else
            getListView().setSelectionFromTop(0, 0);
    }


    @Override
    protected void buildListAdapter() {
        timeLineAdapter = new CommentListAdapter(this, ((ICommander) getActivity()).getBitmapDownloader(), getList().getItemList(), getListView(), true, true);
        pullToRefreshListView.setAdapter(timeLineAdapter);
    }


    protected void listViewItemClick(AdapterView parent, View view, int position, long id) {
        if (!clearActionModeIfOpen()) {
            CommentFloatingMenu menu = new CommentFloatingMenu(getList().getItem(position));
            menu.show(getFragmentManager(), "");
        }
    }


    @Override
    protected CommentListBean getDoInBackgroundNewData() throws WeiboException {
        CommentListBean result;
        ICommentsTimeLineDao dao = new MainCommentsTimeLineDao(token);

        if (getList() != null && getList().getItemList().size() > 0) {
            dao.setSince_id(getList().getItemList().get(0).getId());
        }
        result = dao.getGSONMsgList();
        if (result != null) {
            CommentsTimeLineDBTask.addCommentLineMsg(result, accountBean.getUid());
        }
        return result;
    }

    @Override
    protected CommentListBean getDoInBackgroundOldData() throws WeiboException {
        CommentListBean result;
        ICommentsTimeLineDao dao = new MainCommentsTimeLineDao(token);

        if (getList() != null && getList().getItemList().size() > 0) {
            dao.setMax_id(getList().getItemList().get(getList().getItemList().size() - 1).getId());
        }
        result = dao.getGSONMsgList();
        return result;
    }

    @Override
    protected CommentListBean getDoInBackgroundMiddleData(String beginId, String endId) throws WeiboException {
        throw new UnsupportedOperationException("comment list dont support this operation");
    }

    @Override
    protected void newMsgOnPostExecute(CommentListBean newValue) {
        if (newValue != null) {
            if (newValue.getItemList().size() == 0) {
                Toast.makeText(getActivity(), getString(R.string.no_new_message), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.total) + newValue.getItemList().size() + getString(R.string.new_messages), Toast.LENGTH_SHORT).show();
                getList().addNewData(newValue);
                getAdapter().notifyDataSetChanged();
                getListView().setSelectionAfterHeaderView();
            }
        }
        unreadBean = null;
        refreshUnread(unreadBean);
        NotificationManager notificationManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Long.valueOf(GlobalContext.getInstance().getCurrentAccountId()).intValue());
    }

    @Override
    protected void oldMsgOnPostExecute(CommentListBean newValue) {
        if (newValue != null && newValue.getItemList().size() > 1) {
            getList().addOldData(newValue);
            getAdapter().notifyDataSetChanged();
        }
    }

    private LoaderManager.LoaderCallbacks<CommentTimeLineData> dbCallback = new LoaderManager.LoaderCallbacks<CommentTimeLineData>() {
        @Override
        public Loader<CommentTimeLineData> onCreateLoader(int id, Bundle args) {
            getPullToRefreshListView().setVisibility(View.INVISIBLE);
            return new CommentsToMeDBLoader(getActivity(), GlobalContext.getInstance().getCurrentAccountId());
        }

        @Override
        public void onLoadFinished(Loader<CommentTimeLineData> loader, CommentTimeLineData result) {
            if (result != null) {
                clearAndReplaceValue(result.cmtList);
                timeLinePosition = result.position;
            }

            getPullToRefreshListView().setVisibility(View.VISIBLE);
            getAdapter().notifyDataSetChanged();
            setListViewPositionFromPositionsCache();

            refreshLayout(getList());
            /**
             * when this account first open app,if he don't have any data in database,fetch data from server automally
             */
            if (getList().getSize() == 0) {
                getPullToRefreshListView().startRefreshNow();
            }

        }

        @Override
        public void onLoaderReset(Loader<CommentTimeLineData> loader) {

        }
    };
}
