/*
 * Copyright (C) 2013 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package python.filechooser;

import java.io.File;
import java.util.List;

import python.listview.JazzyHelper;
import python.listview.JazzyListView;
import zce.app.sdpath.GetPath;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment that displays a list of Files in a given path.
 * 
 * @version 2013-12-11
 * @author paulburke (ipaulpro)
 */
@SuppressLint({ "InlinedApi", "NewApi" })
public class FileListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<List<File>> {

	/**
	 * Interface to listen for events.
	 */
	public interface Callbacks {
		/**
		 * Called when a file is selected from the list.
		 * 
		 * @param file
		 *            The file selected
		 */
		public void onFileSelected(File file,boolean ok);
	}

	private static final int LOADER_ID = 0;

	private FileListAdapter mAdapter;
	private String mPath;
	private Callbacks mListener;
	private View view;
	private JazzyListView mList;// 列表

	private static final int[] effects = { JazzyHelper.STANDARD,
			JazzyHelper.GROW, JazzyHelper.CARDS, JazzyHelper.CURL,
			JazzyHelper.WAVE, JazzyHelper.FLIP, JazzyHelper.FLY,
			JazzyHelper.REVERSE_FLY, JazzyHelper.HELIX, JazzyHelper.FAN,
			JazzyHelper.TILT, JazzyHelper.ZIPPER, JazzyHelper.FADE,
			JazzyHelper.TWIRL, JazzyHelper.SLIDE_IN };

	/**
	 * Create a new instance with the given file path.
	 * 
	 * @param path
	 *            The absolute path of the file (directory) to display.
	 * @return A new Fragment with the given file path.
	 */
	public static FileListFragment newInstance(String path) {
		FileListFragment fragment = new FileListFragment();
		Bundle args = new Bundle();
		args.putString(FileChooserActivity.PATH, path);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mListener = (Callbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement FileListFragment.Callbacks");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new FileListAdapter(getActivity());
		mPath = getArguments() != null ? getArguments().getString(
				FileChooserActivity.PATH) : new GetPath().path(getActivity())
				.getAbsolutePath();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mList = (JazzyListView) getListView();
		mList.setTransitionEffect(getEffects());
		mList.setOnScrollListener(scrollListener);// 绑定滑动事件
		mList.setOnItemLongClickListener(itemLongClickListener);// 绑定长按事件
		((TextView) mList.getEmptyView())
				.setText(getString(R.string.empty_directory));
		setListAdapter(mAdapter);

		// setListShown(false);
		getLoaderManager().initLoader(LOADER_ID, null, this);

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		// return super.onCreateView(inflater, container, savedInstanceState);
		view = inflater.inflate(R.layout.file_list, container, false);
		return view;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FileListAdapter adapter = (FileListAdapter) l.getAdapter();
		if (adapter != null) {
			File file = (File) adapter.getItem(position);
			mPath = file.getAbsolutePath();
			mListener.onFileSelected(file,false);
		}
	}

	@Override
	public Loader<List<File>> onCreateLoader(int id, Bundle args) {
		return new FileLoader(getActivity(), mPath);
	}

	@Override
	public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
		mAdapter.setListItems(data);
		// if (isResumed())
		// setListShown(true);
		// else
		// setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<List<File>> loader) {
		mAdapter.clear();
	}

	protected OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> a, View v, int position,
				long id) {
			FileListAdapter adapter = (FileListAdapter) a.getAdapter();
			if (adapter != null) {
				File file = (File) adapter.getItem(position);
				mPath = file.getAbsolutePath();
				System.out.println(mPath);
				mListener.onFileSelected(file,true);
			}
			return true;
		}
	};

	protected OnScrollListener scrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
				// 当不滚动时
				// 判断是否滚动到底部
				if (mList.getLastVisiblePosition() > (mList.getCount() - 2)) {
					mList.setTransitionEffect(getEffects());
				}
				// 判断是否滚动到顶部
				if (mList.getFirstVisiblePosition() < 2) {
					mList.setTransitionEffect(getEffects());
				}
			}
		}

		@Override
		public void onScroll(AbsListView absListView, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
		}
	};

	public int getEffects() {
		return effects[(int) (Math.random() * effects.length)];
	}
}
