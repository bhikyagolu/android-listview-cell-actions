package com.yv.techgee.android.listener;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.yv.techgee.android.R;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


public class ListViewSwipeGestureListener implements OnTouchListener {
    private static final String TAG = ListViewSwipeGestureListener.class.getSimpleName();

    private Activity mActivity;
    private int mSlop;
    // Fixed properties
    private ListView mListView;


    private TouchCallbacks mCallBacks;

    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private int actionItemWidth = 1;
    private int actionItemHeight = 1;

    // Transient properties
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int listRowPosition, opened_position, stagged_position;
    private ViewGroup mDownView, old_mDownView;
    private ViewGroup mDownView_parent, old_mDownView_parent;
    private TextView action1TextView, action2TextView;
    private boolean mOptionsDisplay = false;

    //Functional  Usages
    private String action1Text;
    private String action2Text;
    private Drawable deleteDrawable;
    private Drawable pingDrawable;

    private boolean canDelete = false;
    private boolean canPing = false;

    //Callback functions
    public interface TouchCallbacks {
        boolean canAction1(int position);

        boolean canAction2(int position);

        void onAction1Clicked(int position);

        void onAction2Clicked(int position);

        void OnClickListView(int position);

    }

    public ListViewSwipeGestureListener(ListView listView, TouchCallbacks Callbacks, Activity context) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        mSlop = vc.getScaledTouchSlop() * 2;
        mListView = listView;
        mActivity = context;
        mCallBacks = Callbacks;
        action1Text = mActivity.getResources().getString(R.string.basic_action_1);
        action2Text = mActivity.getResources().getString(R.string.basic_action_2);
        deleteDrawable = mActivity.getResources().getDrawable(android.R.drawable.star_on);
        pingDrawable = mActivity.getResources().getDrawable(android.R.drawable.star_on);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {            //Invokes OnClick Functionality
                if (!mOptionsDisplay) {
                    mCallBacks.OnClickListView(listRowPosition);
                }

            }
        });
    }

    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (mViewWidth < 2) {
            mViewWidth = mListView.getWidth();
            actionItemWidth = mViewWidth / 4;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                if (old_mDownView != null) {
                    resetListItemPosition(old_mDownView_parent, old_mDownView);
                    old_mDownView = null;
                    old_mDownView_parent = null;

                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(
                            MotionEvent.ACTION_CANCEL
                                    | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mListView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    if (action1TextView != null) {
                        action1TextView.setOnClickListener(null);
                    }
                    if (action2TextView != null) {
                        action2TextView.setOnClickListener(null);
                    }
                    action1TextView = null;
                    action2TextView = null;
                    return false;
                }

                listRowPosition = mListView.pointToPosition((int) event.getX(), (int) event.getY());

                if (listRowPosition < 0) {
                    return false;
                }

                canDelete = mCallBacks.canAction1(listRowPosition);
                canPing = mCallBacks.canAction2(listRowPosition);


                Rect rect = new Rect();
                int childCount = mListView.getChildCount();
                int[] listViewCoords = new int[2];
                mListView.getLocationOnScreen(listViewCoords);
                int x = (int) event.getRawX() - listViewCoords[0];
                int y = (int) event.getRawY() - listViewCoords[1];
                ViewGroup child;
                for (int i = 0; i < childCount; i++) {
                    child = (ViewGroup) mListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView_parent = child;
                        mDownView = (ViewGroup) child.findViewById(R.id.list_display_view_container);
                        break;
                    }
                }
                if (mDownView != null && (canDelete || canPing)) {
                    mDownX = event.getRawX();
                    mDownY = event.getRawY();
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                    actionItemHeight = mDownView.getHeight();
                }

                return false;
            }

            case MotionEvent.ACTION_MOVE: {

                float deltaX = event.getRawX() - mDownX;
                float deltaY = event.getRawY() - mDownY;
                if (mVelocityTracker == null || deltaY > mSlop) {
                    break;
                }
                mVelocityTracker.addMovement(event);
                if (Math.abs(deltaX) > mSlop && !mSwiping && (canDelete || canPing) && Math.abs(deltaY) < actionItemHeight) {
                    mSwiping = true;
                    boolean swipeRight = deltaX > 0;
                    mListView.requestDisallowInterceptTouchEvent(true);
                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(
                            MotionEvent.ACTION_CANCEL
                                    | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mListView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();

                    //AppLog.d(TAG, "canAction1" + deltaX);

                    if (canDelete && swipeRight || canPing && !swipeRight) {
                        setupActionView(swipeRight);
                        doListItemTranslation(swipeRight);
                    }
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }
                /*float deltaX = event.getRawX() - mDownX;
                if (mSwiping && Math.abs(deltaX) < mSlop) {
                    resetListItemPosition(mDownView);
                }*/
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mSwiping = false;
                break;
            }


        }
        return false;
    }


    private void resetListItemPosition(final ViewGroup tempView_parent, final ViewGroup tempView) {
        //AppLog.d(TAG, "call to resetListItemPosition");
        animate(tempView).translationX(0).alpha(1f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                int count = tempView_parent.getChildCount();
                //AppLog.d(TAG, "count : "+count);
                for (int i = 0; i < count; i++) {
                    View childView = tempView_parent.getChildAt(i);
                    if (childView == null) {
                        continue;
                    }
                    //AppLog.d(TAG, "id "+tempView_parent.getId());

                    if (childView.getId() == R.id.action1_handle || childView.getId() == R.id.action2_handle) {
                        //AppLog.d(TAG, "deleting delete view");
                        tempView_parent.removeViewAt(i);
                    }
                }
                //AppLog.d(TAG, "count : "+tempView.getChildCount());
                mOptionsDisplay = false;

            }
        });
        stagged_position = -1;
        opened_position = -1;

    }

    private void doListItemTranslation(boolean swipeRight) {
        old_mDownView = mDownView;
        old_mDownView_parent = mDownView_parent;
        animate(mDownView)
                .translationX(swipeRight ? actionItemWidth : -actionItemWidth).setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mOptionsDisplay = true;
                        stagged_position = listRowPosition;
                        if (canDelete) {
                            if (action1TextView != null) {
                                action1TextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        resetListItemPosition(old_mDownView_parent, old_mDownView);
                                        mCallBacks.onAction1Clicked(listRowPosition);
                                    }
                                });
                            }
                        }
                        if (canPing) {
                            if (action2TextView != null) {
                                action2TextView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        resetListItemPosition(old_mDownView_parent, old_mDownView);
                                        mCallBacks.onAction2Clicked(listRowPosition);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                    }
                });
    }

    private void setupActionView(boolean setupDelete) {
        if (setupDelete) {
            //AppLog.d(TAG, "Setting up delete view");
            action1TextView = new TextView(mActivity.getApplicationContext());
            RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(actionItemWidth, actionItemHeight);
            lp1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            action1TextView.setId(R.id.action1_handle);
            action1TextView.setLayoutParams(lp1);
            action1TextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            action1TextView.setText(action1Text);
            //action1TextView.setWidth(actionItemWidth);
            action1TextView.setPadding(0, actionItemHeight / 5, 0, 0);
            //action1TextView.setHeight(actionItemHeight);
            action1TextView.setBackgroundResource(R.color.action1);
            action1TextView.setTextColor(mActivity.getResources().getColor(android.R.color.white));
            action1TextView.setCompoundDrawablesWithIntrinsicBounds(null, deleteDrawable, null, null);
            mDownView_parent.addView(action1TextView, 0);
        } else {
            action2TextView = new TextView(mActivity.getApplicationContext());
            action2TextView.setId(R.id.action2_handle);
            RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(actionItemWidth, actionItemHeight);
            lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            action2TextView.setLayoutParams(lp2);
            action2TextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            action2TextView.setText(action2Text);
            //action2TextView.setWidth(actionItemWidth);
            action2TextView.setPadding(0, actionItemHeight / 5, 0, 0);
            //action2TextView.setHeight(actionItemHeight);
            action2TextView.setBackgroundResource(R.color.action2);
            action2TextView.setTextColor(mActivity.getResources().getColor(android.R.color.white));
            action2TextView.setCompoundDrawablesWithIntrinsicBounds(null, pingDrawable, null, null);
            mDownView_parent.addView(action2TextView, 0);

        }
    }

    class ActionItemTouchListener implements OnTouchListener {
        private ViewGroup listRowView_parent;
        private ViewGroup listRowView;

        ActionItemTouchListener(ViewGroup listRowView_parent, ViewGroup listRowView) {
            this.listRowView_parent = listRowView_parent;
            this.listRowView = listRowView;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            opened_position = mListView.getPositionForView((View) v.getParent());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    if (opened_position == stagged_position && mOptionsDisplay) {
                        mOptionsDisplay = false;
                        switch (v.getId()) {
                            case R.id.action1_handle:
                                resetListItemPosition(listRowView_parent, listRowView);
                                mCallBacks.onAction1Clicked(listRowPosition);
                                return true;
                            case R.id.action2_handle:
                                resetListItemPosition(listRowView_parent, listRowView);
                                mCallBacks.onAction2Clicked(listRowPosition);
                                return true;
                        }
                    }
                }
                return false;
            }

            return false;
        }

    }

}
