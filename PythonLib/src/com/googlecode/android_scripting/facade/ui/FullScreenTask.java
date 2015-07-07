package com.googlecode.android_scripting.facade.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.xmlpull.v1.XmlPullParser;

import python.gif.GifDrawable;
import python.listview.JazzyHelper;
import python.listview.JazzyListView;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.future.FutureActivityTask;

public class FullScreenTask extends FutureActivityTask<Object> implements
		OnClickListener, OnItemClickListener {
	private EventFacade mEventFacade;
	private UiFacade mUiFacade;
	public View mView = null;
	protected ViewInflater mInflater = new ViewInflater();
	protected String mLayout;
	protected final CountDownLatch mShowLatch = new CountDownLatch(1);
	protected Handler mHandler = null;
	private List<Integer> mOverrideKeys;
	private Map<String, GifDrawable> gifTask = new HashMap<String, GifDrawable>();
	protected String mTitle;
	private final int[] effects = { JazzyHelper.STANDARD, JazzyHelper.GROW,
			JazzyHelper.CARDS, JazzyHelper.CURL, JazzyHelper.WAVE,
			JazzyHelper.FLIP, JazzyHelper.FLY, JazzyHelper.REVERSE_FLY,
			JazzyHelper.HELIX, JazzyHelper.FAN, JazzyHelper.TILT,
			JazzyHelper.ZIPPER, JazzyHelper.FADE, JazzyHelper.TWIRL,
			JazzyHelper.SLIDE_IN };

	public FullScreenTask(String layout, String title) {
		super();
		mLayout = layout;
		if (title != null) {
			mTitle = title;
		} else {
			mTitle = "";
		}
	}

	@Override
	public void onCreate() {
		// super.onCreate();
		if (mHandler == null) {
			mHandler = new Handler();
		}
		mInflater.getErrors().clear();
		try {
			if (mView == null) {
				InputStream inputStream = null;
				try {
					Uri uri = Uri.parse(mLayout);
					String path;
					if ("file".equals(uri.getScheme())) {
						path = uri.getPath();
					} else {
						path = mLayout;
					}
					File file = new File(path);
					if (file.isFile()) {// 如果是文件路径则读取文件
						inputStream = new FileInputStream(file);
					}
				} catch (Exception e) {
					inputStream = null;
				}
				XmlPullParser xml;
				if (inputStream != null) {
					xml = ViewInflater.getXml(inputStream);
				} else {
					StringReader sr = new StringReader(mLayout);
					xml = ViewInflater.getXml(sr);
				}
				mView = mInflater.inflate(getActivity(), xml);
			}
		} catch (Exception e) {
			mInflater.getErrors().add(e.toString());
			mView = defaultView();
			mInflater.setIdList(android.R.id.class);
		}
		getActivity().setContentView(mView);
		getActivity().setTitle(mTitle);
		mInflater.setClickListener(mView, this, this);
		mShowLatch.countDown();
	}

	@Override
	public void onResume() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < gifTask.size(); i++) {
					final GifDrawable gifDrawable = gifTask.get(i);
					if (gifDrawable == null) {
						continue;
					}
					if (!gifDrawable.isPlaying()) {
						if (gifDrawable.isRunning()) {
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									gifDrawable.reset();
								}
							});
						} else {
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									gifDrawable.start();
								}
							});
						}
					}
				}
			}
		}).start();
		super.onResume();
	}

	@Override
	public void onPause() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < gifTask.size(); i++) {
					final GifDrawable gifDrawable = gifTask.get(i);
					if (gifDrawable == null) {
						continue;
					}
					if (gifDrawable.isPlaying()) {
						if (gifDrawable.canPause()) {
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									gifDrawable.pause();
								}
							});
						}
					}
				}
			}
		}).start();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < gifTask.size(); i++) {
					final GifDrawable gifDrawable = gifTask.get(i);
					if (gifDrawable == null) {
						continue;
					}
					if (gifDrawable.isRunning()) {
						if (gifDrawable.canPause()) {
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									gifDrawable.pause();
								}
							});
						}
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								gifDrawable.stop();
								gifDrawable.recycle();
							}
						});
					}
				}
			}
		}).start();
		mEventFacade.postEvent("screen", "destroy");
		super.onDestroy();
	}

	/**
	 * 添加gif视图
	 * 
	 * @param gifDrawable
	 */
	public void addGifTask(String id, GifDrawable gifDrawable) {
		if (!gifTask.containsKey(id)) {
			gifTask.put(id, gifDrawable);
		}
	}

	/** default view in case of errors */
	@SuppressWarnings("deprecation")
	protected View defaultView() {
		LinearLayout result = new LinearLayout(getActivity());
		result.setOrientation(LinearLayout.VERTICAL);
		result.setGravity(Gravity.CENTER);
		TextView text = new TextView(getActivity());
		text.setGravity(Gravity.CENTER);
		text.setText("Sample Layout");
		result.addView(text, LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		Button b = new Button(getActivity());
		b.setText("OK");
		result.addView(b, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		return result;
	}

	public EventFacade getEventFacade() {
		return mEventFacade;
	}

	public void setEventFacade(EventFacade eventFacade) {
		mEventFacade = eventFacade;
	}

	public void setUiFacade(UiFacade uiFacade) {
		mUiFacade = uiFacade;
	}

	public CountDownLatch getShowLatch() {
		return mShowLatch;
	}

	public Map<String, Map<String, String>> getViewAsMap() {
		return mInflater.getViewAsMap(mView);
	}

	public View getViewByName(String idName) {
		View result = null;
		int id = mInflater.getId(idName);
		if (id != 0) {
			result = mView.findViewById(id);
		}
		return result;
	}

	public Map<String, String> getViewDetail(String idName) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("error", "id not found (" + idName + ")");
		View v = getViewByName(idName);
		if (v != null) {
			result = mInflater.getViewInfo(v);
		}
		return result;
	}

	public String setViewProperty(String idName, String property, String value) {
		View v = getViewByName(idName);
		mInflater.getErrors().clear();
		if (v != null) {
			SetProperty p = new SetProperty(v, property, value);
			mHandler.post(p);
			try {
				p.mLatch.await();
			} catch (InterruptedException e) {
				mInflater.getErrors().add(e.toString());
			}
		} else {
			return "View " + idName + " not found.";
		}
		if (mInflater.getErrors().size() == 0) {
			return "OK";
		}
		return mInflater.getErrors().get(0);
	}

	public String setList(String id, JSONArray items) {
		View v = getViewByName(id);
		mInflater.getErrors().clear();
		if (v != null) {
			SetList p = new SetList(v, items);
			mHandler.post(p);
			try {
				p.mLatch.await();
			} catch (InterruptedException e) {
				mInflater.getErrors().add(e.toString());
			}
		} else {
			return "View " + id + " not found.";
		}
		if (mInflater.getErrors().size() == 0) {
			return "OK";
		}
		return mInflater.getErrors().get(0);
	}

	public String setJList(String id, JSONArray items) {
		View v = getViewByName(id);
		mInflater.getErrors().clear();
		if (v != null) {
			JazzyListView view = (JazzyListView) v;
			SetJList p = new SetJList(view, items);
			mHandler.post(p);
			try {
				p.mLatch.await();
			} catch (InterruptedException e) {
				mInflater.getErrors().add(e.toString());
			}
		} else {
			return "View " + id + " not found.";
		}
		if (mInflater.getErrors().size() == 0) {
			return "OK";
		}
		return mInflater.getErrors().get(0);
	}

	@Override
	public void onClick(View view) {
		mEventFacade.postEvent("click", mInflater.getViewInfo(view));
	}

	public void loadLayout(String layout) {
		ViewInflater inflater = new ViewInflater();
		View view;
		InputStream inputStream = null;
		try {
			Uri uri = Uri.parse(layout);
			String path;
			if ("file".equals(uri.getScheme())) {
				path = uri.getPath();
			} else {
				path = layout;
			}
			File file = new File(path);
			if (file.isFile()) {// 如果是文件路径则读取文件
				inputStream = new FileInputStream(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			XmlPullParser xml;
			if (inputStream != null) {
				xml = ViewInflater.getXml(inputStream);
			} else {
				StringReader sr = new StringReader(layout);
				xml = ViewInflater.getXml(sr);
			}
			view = inflater.inflate(getActivity(), xml);
			mView = view;
			mInflater = inflater;
			getActivity().setContentView(mView);
			mInflater.setClickListener(mView, this, this);
			mLayout = layout;
			mView.invalidate();
		} catch (Exception e) {
			mInflater.getErrors().add(e.toString());
		}
	}

	private class SetProperty implements Runnable {
		View mView;
		String mProperty;
		String mValue;
		CountDownLatch mLatch = new CountDownLatch(1);

		SetProperty(View view, String property, String value) {
			mView = view;
			mProperty = property;
			mValue = value;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mInflater.setProperty(mView, mProperty, mValue);
			mView.invalidate();
			mLatch.countDown();
		}
	}

	private class SetList implements Runnable {
		View mView;
		JSONArray mItems;
		CountDownLatch mLatch = new CountDownLatch(1);

		SetList(View view, JSONArray items) {
			mView = view;
			mItems = items;
			mLatch.countDown();
		}

		@Override
		public void run() {
			mInflater.setListAdapter(mView, mItems);
			mView.invalidate();
		}
	}

	private class SetJList implements Runnable {
		JazzyListView mView;
		JSONArray mItems;
		CountDownLatch mLatch = new CountDownLatch(1);

		SetJList(JazzyListView view, JSONArray items) {
			mView = view;
			mItems = items;
			mLatch.countDown();
		}

		@Override
		public void run() {
			mInflater.setListAdapter(mView, mItems);
			mView.setTransitionEffect(getEffects());
			mView.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
					switch (scrollState) {
					case OnScrollListener.SCROLL_STATE_IDLE:
						// 当不滚动时
						// 判断是否滚动到底部
						if (mView.getLastVisiblePosition() == (mView.getCount() - 1)) {
							mView.setTransitionEffect(getEffects());
						}
						// 判断是否滚动到顶部
						if (mView.getFirstVisiblePosition() == 1) {
							mView.setTransitionEffect(getEffects());
						}
					}
				}

				@Override
				public void onScroll(AbsListView absListView,
						int firstVisibleItem, int visibleItemCount,
						int totalItemCount) {
				}
			});
			mView.invalidate();
		}
	}

	private class SetLayout implements Runnable {
		String mLayout;
		CountDownLatch mLatch = new CountDownLatch(1);

		SetLayout(String layout) {
			mLayout = layout;
		}

		@Override
		public void run() {
			loadLayout(mLayout);
			mLatch.countDown();
		}
	}

	private class SetTitle implements Runnable {
		String mSetTitle;
		CountDownLatch mLatch = new CountDownLatch(1);

		SetTitle(String title) {
			mSetTitle = title;
		}

		@Override
		public void run() {
			mTitle = mSetTitle;
			getActivity().setTitle(mSetTitle);
			mLatch.countDown();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("key", String.valueOf(keyCode));
		data.put("action", String.valueOf(event.getAction()));
		mEventFacade.postEvent("key", data);
		boolean overrideKey = (keyCode == KeyEvent.KEYCODE_BACK)
				|| (mOverrideKeys == null ? false : mOverrideKeys
						.contains(keyCode));
		return overrideKey;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return mUiFacade.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> aview, View aitem, int position,
			long id) {
		Map<String, String> data = mInflater.getViewInfo(aview);
		data.put("position", String.valueOf(position));
		mEventFacade.postEvent("itemclick", data);
	}

	public void setOverrideKeys(List<Integer> overrideKeys) {
		mOverrideKeys = overrideKeys;
	}

	// Used to hot-switch screens.
	public void setLayout(String layout) {
		SetLayout p = new SetLayout(layout);
		mHandler.post(p);
		try {
			p.mLatch.await();
		} catch (InterruptedException e) {
			mInflater.getErrors().add(e.toString());
		}
	}

	public void setTitle(String title) {
		SetTitle p = new SetTitle(title);
		mHandler.post(p);
		try {
			p.mLatch.await();
		} catch (InterruptedException e) {
			mInflater.getErrors().add(e.toString());
		}
	}

	public int getEffects() {
		return effects[(int) (Math.random() * effects.length)];
	}

	public ViewInflater getInflater() {
		return mInflater;
	}
}
