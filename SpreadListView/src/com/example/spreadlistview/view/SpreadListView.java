package com.example.spreadlistview.view;

import java.util.LinkedList;
import java.util.Queue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Scroller;

public class SpreadListView extends AdapterView<BaseAdapter> {
	private Context mContext;
    public static int ANIMATION_DURATION = 300;
	protected BaseAdapter mAdapter;
	private int mTopViewIndex = -1;
	private int mDownViewIndex = 0;
	protected int mCurrentY;
	protected int mNextY;
	private int mMaxY = Integer.MAX_VALUE;
	private int mDisplayOffset = 0;
	protected Scroller mScroller;
	private GestureDetector mGesture;
	private Queue<View> mRemovedViewQueue = new LinkedList<View>();
	private OnItemSelectedListener mOnItemSelected;
	private OnItemClickListener mOnItemClicked;
    private OnItemClickListener lastViewClicked;
	private OnItemLongClickListener mOnItemLongClicked;
	private boolean mDataChanged = false;
    private int openPosition = 0;
    private int touchPosition = 0;
    private View openView = null;
    private View touchView = null;
    private boolean notMove = false;
    private boolean openViewExist = false;
    private View lastView;
    private boolean animationIsDoing = false;

	public SpreadListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initView();
	}
	
	private synchronized void initView() {
		mTopViewIndex = -1;
		mDownViewIndex = 0;
		mDisplayOffset = 0;
		mCurrentY = 0;
		mNextY = 0;
		mMaxY = Integer.MAX_VALUE;
		mScroller = new Scroller(getContext());
		mGesture = new GestureDetector(getContext(), mOnGesture);
	    openPosition = 0;
	    touchPosition = 0;
	    openView = null;
	    animationIsDoing = false;
	}
	
	@Override
	public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
		mOnItemSelected = listener;
	}
	
	@Override
	public void setOnItemClickListener(AdapterView.OnItemClickListener listener){
		mOnItemClicked = listener;
	}
	
	public void setLastItemClickListener(AdapterView.OnItemClickListener listener){
		lastViewClicked = listener;
	}
	
	@Override
	public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
		mOnItemLongClicked = listener;
	}

	private DataSetObserver mDataObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			synchronized(SpreadListView.this){
				mDataChanged = true;
			}
			invalidate();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			reset();
			invalidate();
			requestLayout();
		}
		
	};

	@Override
    protected int computeVerticalScrollOffset() {
        return mCurrentY;
    }
	
	@Override
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        //if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }
	
	@Override
	public BaseAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setAdapter(BaseAdapter adapter) {
		if(mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataObserver);
		}
		mAdapter = adapter;
		mAdapter.registerDataSetObserver(mDataObserver);
		int oldNextY = mNextY;
		int oldOpenPosition = openPosition;
		reset();
		mNextY = oldNextY;
		openPosition = oldOpenPosition;
	}
	
	private void reset(){
		initView();
		removeAllViewsInLayout();
        requestLayout();
	}

	@Override
	public void setSelection(int position) {
	}
	
	public void setLastView(View view){
		lastView = view;
	}
	
	private void addAndMeasureChild(final View child, int viewPos, int position) {
		LayoutParams params = child.getLayoutParams();
		if(params == null) {
			params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}
		addViewInLayout(child, viewPos, params, true);
		
        if(position == openPosition){
			openView = child;
			child.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(((int)getHeight()/6)*3, MeasureSpec.EXACTLY));
		}
		else{
			child.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec((int)getHeight()/6, MeasureSpec.EXACTLY));
		}
	}

	@Override
	protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if(mAdapter == null){
			return;
		}
		
		if(mDataChanged){
			int oldCurrentY = mCurrentY;
			int oldOpenPosition = openPosition;
			initView();
			removeAllViewsInLayout();
			mNextY = oldCurrentY;
			openPosition = oldOpenPosition;
			mDataChanged = false;
		}

		if(mScroller.computeScrollOffset()){
			mNextY = mScroller.getCurrY();
		}
		
		if(mNextY <= 0){
			mNextY = 0;
			mScroller.forceFinished(true);
		}
		if(mNextY >= mMaxY) {
			mNextY = mMaxY;
			mScroller.forceFinished(true);
		}
		
		int dy = mCurrentY - mNextY;
		if(notMove){
			fillList(0);
			positionItems(0);
			removeNonVisibleItems(0);
			notMove = false;
		}
		else{
			removeNonVisibleItems(dy);
			fillList(dy);
			positionItems(dy);
		}
		
		mCurrentY = mNextY;
		
		if(!mScroller.isFinished()){
			post(new Runnable(){
				@Override
				public void run() {
					requestLayout();
				}
			});
		}
	}
	
	private void fillList(final int dy) {
		int edge = 0;
		View child = getChildAt(getChildCount()-1);
		if(child != null) {
			edge = child.getBottom();
		}
		fillListDown(edge, dy);
		
		edge = 0;
		child = getChildAt(0);
		if(child != null) {
			edge = child.getTop();
		}
		fillListTop(edge, dy);
	}
	
	private void fillListDown(int bottomEdge, final int dy) {
		View child;
		while(bottomEdge + dy < getHeight() && mDownViewIndex <= mAdapter.getCount()) {
			if(mDownViewIndex == mAdapter.getCount()) {
				child = lastView;
			}
			else{
				child = mAdapter.getView(mDownViewIndex, mRemovedViewQueue.poll(), null);
			}
			addAndMeasureChild(child, -1, mDownViewIndex);
			bottomEdge += child.getMeasuredHeight();
			if(mDownViewIndex == mAdapter.getCount()) {
				mMaxY = mCurrentY + bottomEdge - getHeight();
			}
			if (mMaxY < 0) {
				mMaxY = 0;
			}
			mDownViewIndex++;
		}
	}
	
	private void fillListTop(int topEdge, final int dy) {
		while(topEdge + dy > 0 && mTopViewIndex >= 0) {
			View child = mAdapter.getView(mTopViewIndex, mRemovedViewQueue.poll(), null);
			addAndMeasureChild(child, 0, mTopViewIndex);			
			topEdge -= child.getMeasuredHeight();
			mDisplayOffset -= child.getMeasuredHeight();
			mTopViewIndex--;
		}
	}
	
	private void removeNonVisibleItems(final int dy) {
		View child = getChildAt(0);
		while(child != null && child.getBottom() + dy <= 0) {
			if(openView == child && openViewExist){
				break;
			}
			mDisplayOffset += child.getMeasuredHeight();
			//mRemovedViewQueue.offer(child);
			if(openPosition == getFirstVisiableItemPosition()){
				openView = null;
			}
			removeViewInLayout(child);
			mTopViewIndex++;
			child = getChildAt(0);	
		}
		child = getChildAt(getChildCount()-1);
		while(child != null && child.getTop() + dy >= getHeight()) {
			//mRemovedViewQueue.offer(child);
			if(openPosition == getLastVisiableItemPosition()){
				openView = null;
			}
			removeViewInLayout(child);
			mDownViewIndex--;
			child = getChildAt(getChildCount()-1);
		}
	}
	
	private void positionItems(final int dy) {
		if(getChildCount() > 0){
			mDisplayOffset += dy;
			int top = mDisplayOffset;
			for(int i=0,j=getFirstVisiableItemPosition();i<getChildCount();i++,j++){
				View child = getChildAt(i);
				int childHeight = child.getMeasuredHeight();

				child.layout(0, top, child.getMeasuredWidth(), top + childHeight);
				top += childHeight + child.getPaddingBottom();
			}
		}
	}
	
	private int getFirstVisiableItemPosition(){
		return mTopViewIndex + 1;
	}
	
	private int getLastVisiableItemPosition(){
		return mDownViewIndex-1;
	}
	
	public synchronized void scrollTo(int y) {
		mScroller.startScroll(0, mNextY, 0, y - mNextY);
		requestLayout();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		return handled;
	}
	
	protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
		synchronized(SpreadListView.this){
			mScroller.fling(0, mNextY, 0, (int)-velocityY, 0, 0, 0, mMaxY);
		}
		requestLayout();
		return true;
	}
	
	protected boolean onDown(MotionEvent e) {
		mScroller.forceFinished(true);
		return true;
	}
	
	private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			if(animationIsDoing) return false;
			touchPosition = pointToPosition((int)e.getX(), (int)e.getY());
			touchView = getChildAt(touchPosition - mTopViewIndex - 1);
			return SpreadListView.this.onDown(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return SpreadListView.this.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			Log.i("onScroll : ", "onScroll : e1 : " + e1);
			Log.i("onScroll : ", "onScroll : e2 : " + e2);
			if(e1 != null && e2 != null){
	            float angle = Math.abs((e2.getRawY() - e1.getRawY()) / (e2.getRawX() - e1.getRawX()));
	            angle = (float)Math.toDegrees(Math.atan(angle));
				synchronized(SpreadListView.this){
					if(angle > 45){
						mNextY += (int)distanceY;
					}
				}
			}
			requestLayout();
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
            if(touchView == null){
            	return false;
            }
            else{
				if(lastView == touchView){
					lastViewClicked.onItemClick(SpreadListView.this, touchView, mAdapter.getCount(), -1);
				}
				else{
					if(touchPosition != openPosition){
						expendViewHeight(touchView, touchPosition);
					}
					else{
						if(mOnItemClicked != null){
							mOnItemClicked.onItemClick(SpreadListView.this, touchView, touchPosition, mAdapter.getItemId(touchPosition));
						}
						if(mOnItemSelected != null){
							mOnItemSelected.onItemSelected(SpreadListView.this, touchView, touchPosition, mAdapter.getItemId(touchPosition));
						}
					}
				}
				return true;
            }
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			if(touchPosition == openPosition){
				int childCount = getChildCount();
				for (int i = 0; i < childCount; i++) {
					View child = getChildAt(i);
					if (isEventWithinView(e, child)) {
						if (mOnItemLongClicked != null) {
							int pos = getFirstVisiableItemPosition() + i;
							mOnItemLongClicked.onItemLongClick(SpreadListView.this, child, pos, mAdapter.getItemId(pos));
						}
						break;
					}
				}
			}
		}

		private boolean isEventWithinView(MotionEvent e, View child) {
            Rect viewRect = new Rect();
            int[] childPosition = new int[2];
            child.getLocationOnScreen(childPosition);
            int left = childPosition[0];
            int right = left + child.getWidth();
            int top = childPosition[1];
            int bottom = top + child.getHeight();
            viewRect.set(left, top, right, bottom);
            return viewRect.contains((int) e.getRawX(), (int) e.getRawY());
        }
	};
	
    public int pointToPosition(int x, int y) {
        Rect frame = new Rect();

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return getFirstVisiableItemPosition() + i;
                }
            }
        }
        return INVALID_POSITION;
    }
    
    private void expendViewHeight(final View view, final int position){
        final int originalHeight = view.getHeight(); 
        final int openOriginalHeight;
        if(openView != null){
        	openViewExist = true;
        	openOriginalHeight = openView.getHeight();
        }
        else{
        	openViewExist = false;
        	openOriginalHeight = 0;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, originalHeight*2); 
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {  
            @Override  
            public void onAnimationUpdate(ValueAnimator valueAnimator) { 
                int nowY = (Integer) valueAnimator.getAnimatedValue();
                if(openView != null){
	            	openView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
						     MeasureSpec.makeMeasureSpec(openOriginalHeight-nowY, MeasureSpec.EXACTLY));
                }
            	view.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
    					     MeasureSpec.makeMeasureSpec(originalHeight+nowY, MeasureSpec.EXACTLY));
            	requestLayout();
            }  
        });          
        animator.addListener(new AnimatorListenerAdapter() {  
        	@Override  
            public void onAnimationStart(Animator animation) { 
        		animationIsDoing = true;	
            } 
        	
            @Override  
            public void onAnimationEnd(Animator animation) {  
        		animationIsDoing = false;
            	if(!openViewExist){
            		//但整个页面没有打开的页面时，点击的页面打开后，使mMaxY没有增长originalHeight*2, 所以减小mNextY的值
            		mNextY += -originalHeight*2;
            		notMove = true;
            	}
            	openViewExist = false;
            	openView = view;
            	openPosition = position;
            }  
        });
        animator.setDuration(ANIMATION_DURATION);
        animator.start(); 
    }
    
}
