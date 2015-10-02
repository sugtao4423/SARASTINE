package com.tao.sarastine;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class CustomListView extends ListView{
	public CustomListView(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public interface OnSoftKeyShownListener{
		public void onSoftKeyShown(boolean isShown);
	}

	private OnSoftKeyShownListener listener;

	public void setListener(OnSoftKeyShownListener listener){
		this.listener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		super.onSizeChanged(w, h, oldw, oldh);
		// リストビューのサイズが変更されたらソフトキーボードに変化があったとみなす
		listener.onSoftKeyShown(true);
	}
}