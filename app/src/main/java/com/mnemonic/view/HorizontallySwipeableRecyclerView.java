package com.mnemonic.view;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;


public class HorizontallySwipeableRecyclerView extends RecyclerView {

    public interface SwipeListener {

        boolean isSwipeable(View view, int position);

        void onSwipeFeedback(View view, int position, float deltaX);

        void onSwipeCancel(View viw, int position);

        void onSwipe(View view, int position, boolean right);
    }

    // copied from ViewPager
    private static final int MIN_FLING_VELOCITY = 400;

    private static final int MIN_DISTANCE_FOR_FLING = 25;

    private final int touchSlop;

    private final int minFlingVelocity;

    private final int maxFlingVelocity;

    private final int minFlingDistance;

    private VelocityTracker tracker;

    private float initialX;

    private float initialY;

    private View swipedView;

    private int swipedViewPosition;

    private boolean swiping;

    private int xDeltaBeforeDetection;

    private boolean swipingEnabled;

    private SwipeListener swipeListener;

    public HorizontallySwipeableRecyclerView(Context context) {
        this(context, null);
    }

    public HorizontallySwipeableRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontallySwipeableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        float density = context.getResources().getDisplayMetrics().density;
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = (int) (MIN_FLING_VELOCITY * density);
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        minFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        swipingEnabled = true;

        setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setSwipingEnabled(newState == RecyclerView.SCROLL_STATE_IDLE);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // nothing
            }
        });

        reset();
    }

    public void setSwipingEnabled(boolean swipingEnabled) {
        this.swipingEnabled = swipingEnabled;
    }

    public void setSwipeListener(SwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (supportsSwiping()) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    onActionDown(e);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (swipedView != null) {
                        onActionMove(e);
                        if (swiping) {
                            return true;
                        }
                    }
                    break;

                // normally, UP and CANCEL would be handled here as well
                // in our case, there is nothing for them to do as they would
                // only reset the swiping state, but there is nothing to reset
                // if there were, MOVE would have returned true and UP/CANCEL
                // would never be seen here
            }
        }

        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (swiping) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    onActionMove(e);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    onActionCancel();
                    break;

                case MotionEvent.ACTION_UP:
                    onActionUp(e);
                    break;
            }

            return true;
        }

        return super.onTouchEvent(e);
    }

    private void onActionDown(MotionEvent e) {
        float touchX = e.getX();
        float touchY = e.getY();

        View touchedView = findChildViewUnder(touchX, touchY);
        if (touchedView != null) {
            int touchedViewPosition = getChildPosition(touchedView);
            if (swipeListener.isSwipeable(touchedView, touchedViewPosition)) {
                tracker = VelocityTracker.obtain();
                tracker.addMovement(e);

                initialX = touchX;
                initialY = touchY;

                swipedView = touchedView;
                swipedViewPosition = touchedViewPosition;
            }
        }
    }

    private void onActionMove(MotionEvent e) {
        tracker.addMovement(e);

        float deltaX = e.getX() - initialX;
        float deltaY = e.getY() - initialY;

        if (!swiping && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > 2 * Math.abs(deltaY)) {
            swiping = true;
            xDeltaBeforeDetection = deltaX > 0 ? touchSlop : -touchSlop;
        }

        if (swiping) {
            swipeListener.onSwipeFeedback(swipedView, swipedViewPosition, deltaX - xDeltaBeforeDetection);
        }
    }

    private void onActionCancel() {
        swipeListener.onSwipeCancel(swipedView, swipedViewPosition);

        reset();
    }

    private void onActionUp(MotionEvent e) {
        tracker.addMovement(e);
        tracker.computeCurrentVelocity(1000, maxFlingVelocity);

        float deltaX = e.getX() - initialX;
        float absDeltaX = Math.abs(deltaX);
        float velocityX = tracker.getXVelocity();
        float absVelocityX = Math.abs(velocityX);

        boolean swipeDetected = false;
        boolean swipeRight = false;

        if (absDeltaX > swipedView.getWidth() / 2) {
            swipeDetected = true;
            swipeRight = deltaX > 0;
        } else if (absDeltaX > minFlingDistance && absVelocityX >= minFlingVelocity) {
            swipeDetected = (velocityX < 0) == (deltaX < 0);
            swipeRight = velocityX > 0;
        }

        if (swipeDetected) {
            swipeListener.onSwipe(swipedView, swipedViewPosition, swipeRight);
        } else {
            swipeListener.onSwipeCancel(swipedView, swipedViewPosition);
        }

        reset();
    }

    private void reset() {
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
        initialX = NO_POSITION;
        initialY = NO_POSITION;
        swipedView = null;
        swipedViewPosition = NO_POSITION;
        swiping = false;
        xDeltaBeforeDetection = 0;
    }

    private boolean supportsSwiping() {
        return swipingEnabled && swipeListener != null;
    }
}
