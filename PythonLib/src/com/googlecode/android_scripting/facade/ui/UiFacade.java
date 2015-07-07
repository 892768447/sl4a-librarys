/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting.facade.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;

import python.button.FancyButton;
import python.gif.GifDrawable;
import python.gif.GifImageView;
import python.menu.RayMenu;
import python.titanic.Titanic;
import python.titanic.TitanicTextView;
import python.titanic.Typefaces;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.Service;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.util.AndroidRuntimeException;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.FutureActivityTaskExecutor;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.interpreter.html.HtmlActivityTask;
import com.googlecode.android_scripting.interpreter.html.HtmlInterpreter;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

/**
 * User Interface Facade. <br>
 * <br>
 * <b>Usage Notes</b><br>
 * <br>
 * The UI facade provides access to a selection of dialog boxes for general user
 * interaction, and also hosts the {@link #webViewShow} call which allows
 * interactive use of html pages.<br>
 * The general use of the dialog functions is as follows:<br>
 * <ol>
 * <li>Create a dialog using one of the following calls:
 * <ul>
 * <li>{@link #dialogCreateInput}
 * <li>{@link #dialogCreateAlert}
 * <li>{@link #dialogCreateDatePicker}
 * <li>{@link #dialogCreateHorizontalProgress}
 * <li>{@link #dialogCreatePassword}
 * <li>{@link #dialogCreateSeekBar}
 * <li>{@link #dialogCreateSpinnerProgress}
 * </ul>
 * <li>Set additional features to your dialog
 * <ul>
 * <li>{@link #dialogSetItems} Set a list of items. Used like a menu.
 * <li>{@link #dialogSetMultiChoiceItems} Set a multichoice list of items.
 * <li>{@link #dialogSetSingleChoiceItems} Set a single choice list of items.
 * <li>{@link #dialogSetPositiveButtonText}
 * <li>{@link #dialogSetNeutralButtonText}
 * <li>{@link #dialogSetNegativeButtonText}
 * <li>{@link #dialogSetMaxProgress} Set max progress for your progress bar.
 * </ul>
 * <li>Display the dialog using {@link #dialogShow}
 * <li>Update dialog information if needed
 * <ul>
 * <li>{@link #dialogSetCurrentProgress}
 * </ul>
 * <li>Get the results
 * <ul>
 * <li>Using {@link #dialogGetResponse}, which will wait until the user performs
 * an action to close the dialog box, or
 * <li>Use eventPoll to wait for a "dialog" event.
 * <li>You can find out which list items were selected using
 * {@link #dialogGetSelectedItems}, which returns an array of numeric indices to
 * your list. For a single choice list, there will only ever be one of these.
 * </ul>
 * <li>Once done, use {@link #dialogDismiss} to remove the dialog.
 * </ol>
 * <br>
 * You can also manipulate menu options. The menu options are available for both
 * {@link #dialogShow} and {@link #fullShow}.
 * <ul>
 * <li>{@link #clearOptionsMenu}
 * <li>{@link #addOptionsMenuItem}
 * </ul>
 * <br>
 * <b>Some notes:</b><br>
 * Not every dialogSet function is relevant to every dialog type, ie,
 * dialogSetMaxProgress obviously only applies to dialogs created with a
 * progress bar. Also, an Alert Dialog may have a message or items, not both. If
 * you set both, items will take priority.<br>
 * In addition to the above functions, {@link #dialogGetInput} and
 * {@link #dialogGetPassword} are convenience functions that create, display and
 * return the relevant dialogs in one call.<br>
 * There is only ever one instance of a dialog. Any dialogCreate call will cause
 * the existing dialog to be destroyed.
 * 
 * @author MeanEYE.rcf (meaneye.rcf@gmail.com) Change By ざ凍結の→愛 892768447@qq.com
 */

@SuppressLint("NewApi")
public class UiFacade extends RpcReceiver {
	// This value should not be used for menu groups outside this class.
	private static final int MENU_GROUP_ID = Integer.MAX_VALUE;

	private final Service mService;
	private final FutureActivityTaskExecutor mTaskQueue;
	private DialogTask mDialogTask;

	private FullScreenTask mFullScreenTask;

	private final List<UiMenuItem> mContextMenuItems;
	private final List<UiMenuItem> mOptionsMenuItems;

	// RayMenu 菜单
	private RayMenu rayMenu;
	private final List<UiArcMenuItem> mRayMenuItems;
	private final AtomicBoolean mRMenuUpdated;
	private final AtomicBoolean mMenuUpdated;

	// 数字进度条
	// public Map<String, NumberProgressBar> npb;

	private final Handler mHandler;

	private final EventFacade mEventFacade;
	private List<Integer> mOverrideKeys = Collections
			.synchronizedList(new ArrayList<Integer>());

	public UiFacade(FacadeManager manager) {
		super(manager);
		mService = manager.getService();
		mTaskQueue = ((BaseApplication) mService.getApplication())
				.getTaskExecutor();
		mHandler = new Handler(mService.getMainLooper());
		mContextMenuItems = new CopyOnWriteArrayList<UiMenuItem>();
		mRayMenuItems = new CopyOnWriteArrayList<UiArcMenuItem>();
		mOptionsMenuItems = new CopyOnWriteArrayList<UiMenuItem>();
		mEventFacade = manager.getReceiver(EventFacade.class);
		mMenuUpdated = new AtomicBoolean(false);
		mRMenuUpdated = new AtomicBoolean(false);
		// npb = new HashMap<String, NumberProgressBar>();
	}

	// @Rpc(description = "Create a number progress.")
	// public void createNumberProgress(
	// @RpcParameter(name = "id", description = "The NumberProgressBar Id")
	// final String id,
	// @RpcParameter(name = "maximum progress") @RpcDefault("100") final Integer
	// max,
	// @RpcParameter(name = "textSize") @RpcOptional final String textSize,
	// @RpcParameter(name = "textColor") @RpcOptional final String textColor,
	// @RpcParameter(name = "barColor") @RpcOptional final String barColor,
	// @RpcParameter(name = "progressColor") @RpcOptional final String
	// progressColor)
	// throws Exception {
	// if (mFullScreenTask == null) {
	// throw new RuntimeException("No screen displayed.");
	// }
	// final View v = mFullScreenTask.getViewByName(id);
	// if (v == null) {
	// throw new RuntimeException("No Found The Id: " + id);
	// }
	// mHandler.post(new Runnable() {
	// public void run() {
	// NumberProgressBar np = npb.get(id);
	// if (np == null) {// 未找到则创建一个
	// np = (NumberProgressBar) v;
	// npb.put(id, np);
	// }
	// np.setMax(max);
	// if (textSize != null) {
	// try {
	// np.setProgressTextSize(Float.parseFloat(textSize));
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// try {
	// if (textColor != null) {
	// np.setProgressTextColor(Color.parseColor(textColor));
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// try {
	// if (barColor != null) {
	// np.setUnreachedBarColor(Color.parseColor(barColor));
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// try {
	// if (progressColor != null) {
	// np.setReachedBarColor(Color.parseColor(progressColor));
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// });
	// }

	/**
	 * For inputType, see <a href=
	 * "http://developer.android.com/reference/android/R.styleable.html#TextView_inputType"
	 * >InputTypes</a>. Some useful ones are text, number, and textUri. Multiple
	 * flags can be supplied, seperated by "|", ie: "textUri|textAutoComplete"
	 */
	@Rpc(description = "Create a text input dialog.")
	public void dialogCreateInput(
			@RpcParameter(name = "title", description = "title of the input box") @RpcDefault("Value") final String title,
			@RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter value:") final String message,
			@RpcParameter(name = "defaultText", description = "text to insert into the input box") @RpcOptional final String text,
			@RpcParameter(name = "inputType", description = "type of input data, ie number or text") @RpcOptional final String inputType)
			throws InterruptedException {
		dialogDismiss();
		mDialogTask = new AlertDialogTask(title, message);
		((AlertDialogTask) mDialogTask).setTextInput(text);
		if (inputType != null) {
			((AlertDialogTask) mDialogTask).setEditInputType(inputType);
		}
	}

	@Rpc(description = "Create a password input dialog.")
	public void dialogCreatePassword(
			@RpcParameter(name = "title", description = "title of the input box") @RpcDefault("Password") final String title,
			@RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter password:") final String message) {
		dialogDismiss();
		mDialogTask = new AlertDialogTask(title, message);
		((AlertDialogTask) mDialogTask).setPasswordInput();
	}

	/**
	 * The result is the user's input, or None (null) if cancel was hit. <br>
	 * Example (python)
	 * 
	 * <pre>
	 * import android
	 * droid=android.Android()
	 * 
	 * print droid.dialogGetInput("Title","Message","Default").result
	 * </pre>
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Rpc(description = "Queries the user for a text input.")
	public String dialogGetInput(
			@RpcParameter(name = "title", description = "title of the input box") @RpcDefault("Value") final String title,
			@RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter value:") final String message,
			@RpcParameter(name = "defaultText", description = "text to insert into the input box") @RpcOptional final String text)
			throws InterruptedException {
		dialogCreateInput(title, message, text, "text");
		dialogSetNegativeButtonText("Cancel");
		dialogSetPositiveButtonText("Ok");
		dialogShow();
		Map<String, Object> response = (Map<String, Object>) dialogGetResponse();
		if ("positive".equals(response.get("which"))) {
			return (String) response.get("value");
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Rpc(description = "Queries the user for a password.")
	public String dialogGetPassword(
			@RpcParameter(name = "title", description = "title of the password box") @RpcDefault("Password") final String title,
			@RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter password:") final String message)
			throws InterruptedException {
		dialogCreatePassword(title, message);
		dialogSetNegativeButtonText("Cancel");
		dialogSetPositiveButtonText("Ok");
		dialogShow();
		Map<String, Object> response = (Map<String, Object>) dialogGetResponse();
		if ("positive".equals(response.get("which"))) {
			return (String) response.get("value");
		} else {
			return null;
		}
	}

	@Rpc(description = "Create a spinner progress dialog.")
	public void dialogCreateSpinnerProgress(
			@RpcParameter(name = "title") @RpcOptional String title,
			@RpcParameter(name = "message") @RpcOptional String message,
			@RpcParameter(name = "maximum progress") @RpcDefault("100") Integer max) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new ProgressDialogTask(ProgressDialog.STYLE_SPINNER, max,
				title, message, true);
	}

	@Rpc(description = "Create a horizontal progress dialog.")
	public void dialogCreateHorizontalProgress(
			@RpcParameter(name = "title") @RpcOptional String title,
			@RpcParameter(name = "message") @RpcOptional String message,
			@RpcParameter(name = "maximum progress") @RpcDefault("100") Integer max) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new ProgressDialogTask(ProgressDialog.STYLE_HORIZONTAL,
				max, title, message, true);
	}

	/**
	 * <b>Example (python)</b>
	 * 
	 * <pre>
	 *   import android
	 *   droid=android.Android()
	 *   droid.dialogCreateAlert("I like swords.","Do you like swords?")
	 *   droid.dialogSetPositiveButtonText("Yes")
	 *   droid.dialogSetNegativeButtonText("No")
	 *   droid.dialogShow()
	 *   response=droid.dialogGetResponse().result
	 *   droid.dialogDismiss()
	 *   if response.has_key("which"):
	 *     result=response["which"]
	 *     if result=="positive":
	 *       print "Yay! I like swords too!"
	 *     elif result=="negative":
	 *       print "Oh. How sad."
	 *   elif response.has_key("canceled"): # Yes, I know it's mispelled.
	 *     print "You can't even make up your mind?"
	 *   else:
	 *     print "Unknown response=",response
	 * 
	 *   print "Done"
	 * </pre>
	 */
	@Rpc(description = "Create alert dialog.")
	public void dialogCreateAlert(
			@RpcParameter(name = "title") @RpcOptional String title,
			@RpcParameter(name = "message") @RpcOptional String message) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new AlertDialogTask(title, message);
	}

	/**
	 * Will produce "dialog" events on change, containing:
	 * <ul>
	 * <li>"progress" - Position chosen, between 0 and max
	 * <li>"which" = "seekbar"
	 * <li>"fromuser" = true/false change is from user input
	 * </ul>
	 * Response will contain a "progress" element.
	 */
	@Rpc(description = "Create seek bar dialog.")
	public void dialogCreateSeekBar(
			@RpcParameter(name = "starting value") @RpcDefault("50") Integer progress,
			@RpcParameter(name = "maximum value") @RpcDefault("100") Integer max,
			@RpcParameter(name = "title") String title,
			@RpcParameter(name = "message") String message) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new SeekBarDialogTask(progress, max, title, message);
	}

	@Rpc(description = "Create time picker dialog.")
	public void dialogCreateTimePicker(
			@RpcParameter(name = "hour") @RpcDefault("0") Integer hour,
			@RpcParameter(name = "minute") @RpcDefault("0") Integer minute,
			@RpcParameter(name = "is24hour", description = "Use 24 hour clock") @RpcDefault("false") Boolean is24hour) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new TimePickerDialogTask(hour, minute, is24hour);
	}

	@Rpc(description = "Create date picker dialog.")
	public void dialogCreateDatePicker(
			@RpcParameter(name = "year") @RpcDefault("1970") Integer year,
			@RpcParameter(name = "month") @RpcDefault("1") Integer month,
			@RpcParameter(name = "day") @RpcDefault("1") Integer day) {
		dialogDismiss(); // Dismiss any existing dialog.
		mDialogTask = new DatePickerDialogTask(year, month, day);
	}

	@Rpc(description = "Dismiss dialog.")
	public void dialogDismiss() {
		if (mDialogTask != null) {
			mDialogTask.dismissDialog();
			mDialogTask = null;
		}
	}

	@Rpc(description = "Show dialog.")
	public void dialogShow() throws InterruptedException {
		if (mDialogTask != null && mDialogTask.getDialog() == null) {
			mDialogTask.setEventFacade(mEventFacade);
			mTaskQueue.execute(mDialogTask);
			mDialogTask.getShowLatch().await();
		} else {
			throw new RuntimeException("No dialog to show.");
		}
	}

	@Rpc(description = "Set progress dialog current value.")
	public void dialogSetCurrentProgress(
			@RpcParameter(name = "current") Integer current) {
		if (mDialogTask != null && mDialogTask instanceof ProgressDialogTask) {
			((ProgressDialog) mDialogTask.getDialog()).setProgress(current);
		} else {
			throw new RuntimeException("No valid dialog to assign value to.");
		}
	}

	@Rpc(description = "Set progress dialog maximum value.")
	public void dialogSetMaxProgress(@RpcParameter(name = "max") Integer max) {
		if (mDialogTask != null && mDialogTask instanceof ProgressDialogTask) {
			((ProgressDialog) mDialogTask.getDialog()).setMax(max);
		} else {
			throw new RuntimeException(
					"No valid dialog to set maximum value of.");
		}
	}

	@Rpc(description = "Set alert dialog positive button text.")
	public void dialogSetPositiveButtonText(
			@RpcParameter(name = "text") String text) {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask).setPositiveButtonText(text);
		} else if (mDialogTask != null
				&& mDialogTask instanceof SeekBarDialogTask) {
			((SeekBarDialogTask) mDialogTask).setPositiveButtonText(text);
		} else {
			throw new AndroidRuntimeException("No dialog to add button to.");
		}
	}

	@Rpc(description = "Set alert dialog button text.")
	public void dialogSetNegativeButtonText(
			@RpcParameter(name = "text") String text) {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask).setNegativeButtonText(text);
		} else if (mDialogTask != null
				&& mDialogTask instanceof SeekBarDialogTask) {
			((SeekBarDialogTask) mDialogTask).setNegativeButtonText(text);
		} else {
			throw new AndroidRuntimeException("No dialog to add button to.");
		}
	}

	@Rpc(description = "Set alert dialog button text.")
	public void dialogSetNeutralButtonText(
			@RpcParameter(name = "text") String text) {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask).setNeutralButtonText(text);
		} else {
			throw new AndroidRuntimeException("No dialog to add button to.");
		}
	}

	// TODO(damonkohler): Make RPC layer translate between JSONArray and
	// List<Object>.
	/**
	 * This effectively creates list of options. Clicking on an item will
	 * immediately return an "item" element, which is the index of the selected
	 * item.
	 */
	@Rpc(description = "Set alert dialog list items.")
	public void dialogSetItems(@RpcParameter(name = "items") JSONArray items) {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask).setItems(items);
		} else {
			throw new AndroidRuntimeException("No dialog to add list to.");
		}
	}

	/**
	 * This creates a list of radio buttons. You can select one item out of the
	 * list. A response will not be returned until the dialog is closed, either
	 * with the Cancel key or a button (positive/negative/neutral). Use
	 * {@link #dialogGetSelectedItems()} to find out what was selected.
	 */
	@Rpc(description = "Set dialog single choice items and selected item.")
	public void dialogSetSingleChoiceItems(
			@RpcParameter(name = "items") JSONArray items,
			@RpcParameter(name = "selected", description = "selected item index") @RpcDefault("0") Integer selected) {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask).setSingleChoiceItems(items,
					selected);
		} else {
			throw new AndroidRuntimeException("No dialog to add list to.");
		}
	}

	/**
	 * This creates a list of check boxes. You can select multiple items out of
	 * the list. A response will not be returned until the dialog is closed,
	 * either with the Cancel key or a button (positive/negative/neutral). Use
	 * {@link #dialogGetSelectedItems()} to find out what was selected.
	 */

	@Rpc(description = "Set dialog multiple choice items and selection.")
	public void dialogSetMultiChoiceItems(
			@RpcParameter(name = "items") JSONArray items,
			@RpcParameter(name = "selected", description = "list of selected items") @RpcOptional JSONArray selected)
			throws JSONException {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			((AlertDialogTask) mDialogTask)
					.setMultiChoiceItems(items, selected);
		} else {
			throw new AndroidRuntimeException("No dialog to add list to.");
		}
	}

	@Rpc(description = "Returns dialog response.")
	public Object dialogGetResponse() {
		try {
			return mDialogTask.getResult();
		} catch (Exception e) {
			throw new AndroidRuntimeException(e);
		}
	}

	@Rpc(description = "This method provides list of items user selected.", returns = "Selected items")
	public Set<Integer> dialogGetSelectedItems() {
		if (mDialogTask != null && mDialogTask instanceof AlertDialogTask) {
			return ((AlertDialogTask) mDialogTask).getSelectedItems();
		} else {
			throw new AndroidRuntimeException("No dialog to add list to.");
		}
	}

	/**
	 * See <a
	 * href=http://code.google.com/p/android-scripting/wiki/UsingWebView>wiki
	 * page</a> for more detail.
	 */
	@Rpc(description = "Display a WebView with the given URL.")
	public void webViewShow(
			@RpcParameter(name = "url") String url,
			@RpcParameter(name = "wait", description = "block until the user exits the WebView") @RpcOptional Boolean wait)
			throws IOException {
		String jsonSrc = FileUtils.readFromAssetsFile(mService,
				HtmlInterpreter.JSON_FILE);
		String AndroidJsSrc = FileUtils.readFromAssetsFile(mService,
				HtmlInterpreter.ANDROID_JS_FILE);
		HtmlActivityTask task = new HtmlActivityTask(mManager, AndroidJsSrc,
				jsonSrc, url, false);
		mTaskQueue.execute(task);
		if (wait != null && wait) {
			try {
				task.getResult();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Context menus are used primarily with {@link #webViewShow}
	 */
	@Rpc(description = "Adds a new item to context menu.")
	public void addContextMenuItem(
			@RpcParameter(name = "label", description = "label for this menu item") String label,
			@RpcParameter(name = "event", description = "event that will be generated on menu item click") String event,
			@RpcParameter(name = "eventData") @RpcOptional Object data) {
		mContextMenuItems.add(new UiMenuItem(label, event, data, null));
	}

	/**
	 * <b>Example (python)</b>
	 * 
	 * <pre>
	 * import android
	 * droid=android.Android()
	 * 
	 * droid.addOptionsMenuItem("Silly","silly",None,"star_on")
	 * droid.addOptionsMenuItem("Sensible","sensible","I bet.","star_off")
	 * droid.addOptionsMenuItem("Off","off",None,"ic_menu_revert")
	 * 
	 * print "Hit menu to see extra options."
	 * print "Will timeout in 10 seconds if you hit nothing."
	 * 
	 * while True: # Wait for events from the menu.
	 *   response=droid.eventWait(10000).result
	 *   if response==None:
	 *     break
	 *   print response
	 *   if response["name"]=="off":
	 *     break
	 * print "And done."
	 * 
	 * </pre>
	 */
	@Rpc(description = "Adds a new item to options menu.")
	public void addOptionsMenuItem(
			@RpcParameter(name = "label", description = "label for this menu item") String label,
			@RpcParameter(name = "event", description = "event that will be generated on menu item click") String event,
			@RpcParameter(name = "eventData") @RpcOptional Object data,
			@RpcParameter(name = "iconName", description = "Android system menu icon, see http://developer.android.com/reference/android/R.drawable.html") @RpcOptional String iconName) {
		mOptionsMenuItems.add(new UiMenuItem(label, event, data, iconName));
		mMenuUpdated.set(true);
	}

	@Rpc(description = "Adds a ray item to RayMenu.\n添加滑动菜单子项")
	public void addRayMenuItem(
			@RpcParameter(name = "event", description = "event that will be generated on menu item click\n点击时产生的事件名称") String event,
			@RpcParameter(name = "eventData\n事件内容") @RpcOptional Object data,
			@RpcParameter(name = "iconPath", description = "the icon path\n图标路径") String iconPath) {
		try {
			mRayMenuItems.add(new UiArcMenuItem(event, data, iconPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Rpc(description = "App Msg Toast.\n消息提示")
	public void appMsg(
			@RpcParameter(name = "message", description = "The Toast Message\n消息提示") final String message,
			@RpcParameter(name = "level", description = "The Toast Level Are: 0-ALERT,1-CONFIRM,2-INFO\n提示级别: 0-错误提示,1-确认提示,2-一般提示") @RpcDefault("2") Integer level,
			@RpcParameter(name = "clear", description = "Remove all tips\n清除所有还未提示的内容") @RpcDefault("false") final Boolean clear)
			throws Exception {
		if (mFullScreenTask == null) {
			mHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(mService, message, Toast.LENGTH_SHORT)
							.show();
				}
			});
		} else {
			final Crouton appMsg;
			switch (level) {
			case 0:
				appMsg = Crouton.makeText(mFullScreenTask.getActivity(),
						message, Style.ALERT);
				break;
			case 1:
				appMsg = Crouton.makeText(mFullScreenTask.getActivity(),
						message, Style.CONFIRM);
				break;
			case 2:
				appMsg = Crouton.makeText(mFullScreenTask.getActivity(),
						message, Style.INFO);
				break;
			default:
				appMsg = Crouton.makeText(mFullScreenTask.getActivity(),
						message, Style.INFO);
				break;
			}
			mHandler.post(new Runnable() {
				public void run() {
					try {
						if (clear) {
							Crouton.cancelAllCroutons();
						}
						appMsg.show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Rpc(description = "Removes all items previously added to ray menu.\n清除所有ray菜单的子项")
	public void clearRayMenu() {
		mRayMenuItems.clear();
	}

	@Rpc(description = "Removes all items previously added to context menu.")
	public void clearContextMenu() {
		mContextMenuItems.clear();
	}

	@Rpc(description = "Removes all items previously added to options menu.")
	public void clearOptionsMenu() {
		mOptionsMenuItems.clear();
		mMenuUpdated.set(true);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		for (UiMenuItem item : mContextMenuItems) {
			MenuItem menuItem = menu.add(item.mmTitle);
			menuItem.setOnMenuItemClickListener(item.mmListener);
		}
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mMenuUpdated.getAndSet(false)) {
			menu.removeGroup(MENU_GROUP_ID);
			for (UiMenuItem item : mOptionsMenuItems) {
				MenuItem menuItem = menu.add(MENU_GROUP_ID, Menu.NONE,
						Menu.NONE, item.mmTitle);
				if (item.mmIcon != null) {
					menuItem.setIcon(mService.getResources().getIdentifier(
							item.mmIcon, "drawable", "android"));
				}
				menuItem.setOnMenuItemClickListener(item.mmListener);
			}
			return true;
		}
		return true;
	}

	/**
	 * 显示ray菜单
	 * 
	 * @param id
	 */
	@Rpc(description = "Init RayMenu")
	public void fullShowRayMenu(
			@RpcParameter(name = "id", description = "id of layout widget\n显示raymenu菜单") String id) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		try {
			if (rayMenu == null) {// 只允许存在一个
				View view = mFullScreenTask.getViewByName(id);
				if (view == null) {
					throw new RuntimeException("The RayMenu Id: +" + id
							+ " Not Found.");
				}
				rayMenu = (RayMenu) view;
			}
			mRMenuUpdated.set(true);
			if (mRMenuUpdated.getAndSet(false)) {
				for (final UiArcMenuItem item : mRayMenuItems) {
					File file = new File(item.mmIcon);
					final ImageView image = new ImageView(
							mFullScreenTask.getActivity());
					if (file.exists()) {
						Bitmap bm = BitmapFactory.decodeFile(file
								.getAbsolutePath());
						image.setImageBitmap(bm);
					} else {
						image.setImageResource(android.R.drawable.ic_dialog_info);
					}
					mHandler.post(new Runnable() {
						public void run() {
							rayMenu.addItem(image, item.mmListener);
						}
					});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * See <a
	 * href=http://code.google.com/p/android-scripting/wiki/FullScreenUI>wiki
	 * page</a> for more detail.
	 */
	@Rpc(description = "Show Full Screen.")
	public List<String> fullShow(
			@RpcParameter(name = "xml", description = "String containing View layout Or Xml File Path") String layout,
			@RpcParameter(name = "title", description = "Activity Title") @RpcOptional String title)
			throws InterruptedException {
		if (mFullScreenTask != null) {
			// fullDismiss();
			mFullScreenTask.setLayout(layout);
			if (title != null) {
				mFullScreenTask.setTitle(title);
			}
		} else {
			mFullScreenTask = new FullScreenTask(layout, title);
			mFullScreenTask.setEventFacade(mEventFacade);
			mFullScreenTask.setUiFacade(this);
			mFullScreenTask.setOverrideKeys(mOverrideKeys);
			mTaskQueue.execute(mFullScreenTask);
			mFullScreenTask.getShowLatch().await();
		}
		return mFullScreenTask.mInflater.getErrors();
	}

	@Rpc(description = "Dismiss Full Screen.")
	public void fullDismiss() {
		if (mFullScreenTask != null) {
			rayMenu = null;
			mFullScreenTask.finish();
			mFullScreenTask = null;
		}
	}

	@Rpc(description = "Get Fullscreen Properties")
	public Map<String, Map<String, String>> fullQuery() {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		return mFullScreenTask.getViewAsMap();
	}

	@Rpc(description = "Get fullscreen properties for a specific widget")
	public Map<String, String> fullQueryDetail(
			@RpcParameter(name = "id", description = "id of layout widget") String id) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		return mFullScreenTask.getViewDetail(id);
	}

	@Rpc(description = "Set fullscreen widget property")
	public String fullSetProperty(
			@RpcParameter(name = "id", description = "id of layout widget") String id,
			@RpcParameter(name = "property", description = "name of property to set") String property,
			@RpcParameter(name = "value", description = "value to set property to") String value) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		return mFullScreenTask.setViewProperty(id, property, value);
	}

	@Rpc(description = "Attach a list to a fullscreen widget")
	public String fullSetList(
			@RpcParameter(name = "id", description = "id of layout widget") String id,
			@RpcParameter(name = "list", description = "List to set") JSONArray items) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		return mFullScreenTask.setList(id, items);
	}

	@Rpc(description = "Attach a Jlist to a fullscreen widget\n特效listview")
	public String fullSetJList(
			@RpcParameter(name = "id", description = "id of layout widget\n特效listview的id") String id,
			@RpcParameter(name = "list", description = "List to set\n列表数据") JSONArray items) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		return mFullScreenTask.setJList(id, items);
	}

	@Rpc(description = "Set the Full Screen Activity Title")
	public void fullSetTitle(
			@RpcParameter(name = "title", description = "Activity Title") String title) {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		mFullScreenTask.setTitle(title);
	}

	/**
	 * This will override the default behaviour of keys while in the fullscreen
	 * mode. ie:
	 * 
	 * <pre>
	 *   droid.fullKeyOverride([24,25],True)
	 * </pre>
	 * 
	 * This will override the default behaviour of the volume keys (codes 24 and
	 * 25) so that they do not actually adjust the volume. <br>
	 * Returns a list of currently overridden keycodes.
	 */
	@Rpc(description = "Override default key actions")
	public JSONArray fullKeyOverride(
			@RpcParameter(name = "keycodes", description = "List of keycodes to override") JSONArray keycodes,
			@RpcParameter(name = "enable", description = "Turn overriding or off") @RpcDefault(value = "true") Boolean enable)
			throws JSONException {
		for (int i = 0; i < keycodes.length(); i++) {
			int value = (int) keycodes.getLong(i);
			if (value > 0) {
				if (enable) {
					if (!mOverrideKeys.contains(value)) {
						mOverrideKeys.add(value);
					}
				} else {
					int index = mOverrideKeys.indexOf(value);
					if (index >= 0) {
						mOverrideKeys.remove(index);
					}
				}
			}
		}
		if (mFullScreenTask != null) {
			mFullScreenTask.setOverrideKeys(mOverrideKeys);
		}
		return new JSONArray(mOverrideKeys);
	}

	@Rpc(description = "set titanic text\n上升下降文字")
	public void setTitanicText(
			@RpcParameter(name = "id", description = "The TextView Id") String id,
			@RpcParameter(name = "usefont", description = "Use Satisfy-Regular.ttf\n是否使用字体") @RpcOptional @RpcDefault("true") Boolean font)
			throws Exception {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		if (Build.VERSION.SDK_INT < 11) {
			throw new RuntimeException("This Api Need Sdk >= 11");
		}
		View view = mFullScreenTask.getViewByName(id);
		if (view == null) {
			throw new RuntimeException("No Found The Id: " + id);
		}
		try {
			final TitanicTextView textView = (TitanicTextView) view;
			if (font != null) {
				if (font) {
					mHandler.post(new Runnable() {
						public void run() {
							textView.setTypeface(Typefaces.get(
									mService.getApplicationContext(),
									"Satisfy-Regular.ttf"));
						}
					});
				}
			}
			mHandler.post(new Runnable() {
				public void run() {
					new Titanic().start(textView);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 自定义按钮
	 * 
	 * @param id
	 * @param defaultColor
	 * @param text
	 * @param textColor
	 * @param textSize
	 * @param textFont
	 * @param radius
	 * @param focusColor
	 * @param fontIconSize
	 * @param fontIconResource
	 * @param iconPosition
	 * @param borderColor
	 * @param borderWidth
	 * @throws Exception
	 */
	@Rpc(description = "set fancy button\n自定义按钮")
	public void setFancyButton(
			@RpcParameter(name = "id", description = "The FancyButton Id\n按钮id") String id,
			@RpcParameter(name = "defaultColor", description = "The Button DefaultColor\n按钮默认颜色") @RpcOptional final String defaultColor,
			@RpcParameter(name = "text", description = "Text\n按钮文本") @RpcOptional @RpcDefault("FancyButton") final String text,
			@RpcParameter(name = "textColor", description = "The Text Color\n文本颜色") @RpcOptional final String textColor,
			@RpcParameter(name = "textSize", description = "The Text Size\n文字大小") @RpcOptional final Integer textSize,
			@RpcParameter(name = "textFont", description = "The Text Font: robotothin.ttf,robotoregular.ttf,Satisfy-Regular.ttf\n文字字体") @RpcOptional final String textFont,
			@RpcParameter(name = "radius", description = "The Button Radius\n按钮圆角") @RpcOptional final Integer radius,
			@RpcParameter(name = "focusColor", description = "The Button FocusColor\n按钮焦点颜色") @RpcOptional final String focusColor,
			@RpcParameter(name = "fontIconSize", description = "The Button FontIconSize\n按钮字体大小") @RpcOptional final Integer fontIconSize,
			@RpcParameter(name = "fontIconResource", description = "The Button FontIconResource\n按钮字体图标") @RpcOptional final String fontIconResource,
			@RpcParameter(name = "iconPosition", description = "The Button IconPosition\n图标位置") @RpcOptional final String iconPosition,
			@RpcParameter(name = "borderColor", description = "The Button BorderColor\n边框颜色") @RpcOptional final String borderColor,
			@RpcParameter(name = "borderWidth", description = "The Button BorderWidth\n边框大小") @RpcOptional final Integer borderWidth)
			throws Exception {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		View view = mFullScreenTask.getViewByName(id);
		if (view == null) {
			throw new RuntimeException("No Found The Id: " + id);
		}
		final FancyButton button;
		try {
			button = (FancyButton) view;
		} catch (Exception e) {
			throw new RuntimeException("This Button Isn't FancyButton");
		}
		if (defaultColor != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setBackgroundColor(mFullScreenTask.getInflater()
							.getColor(defaultColor));
				}
			});
		}
		if (text != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setText(text);
				}
			});
		}
		if (textColor != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setTextColor(mFullScreenTask.getInflater().getColor(
							textColor));
				}
			});
		}
		if (textSize != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setTextSize(textSize);
				}
			});
		}
		if (textFont != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setCustomTextFont(textFont);
				}
			});
		}
		if (radius != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setRadius(radius);
				}
			});
		}
		if (focusColor != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setFocusBackgroundColor(mFullScreenTask
							.getInflater().getColor(focusColor));
				}
			});
		}
		if (fontIconSize != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setFontIconSize(fontIconSize);
				}
			});
		}
		if (fontIconResource != null) {
			if (fontIconResource.startsWith("@string")) {
				mHandler.post(new Runnable() {
					public void run() {
						button.setIconResource(mService.getResources()
								.getString(
										mService.getResources().getIdentifier(
												fontIconResource.replace(
														"@string/", ""),
												"string",
												mService.getPackageName())));
					}
				});
			} else if (fontIconResource.indexOf("&#") > -1) {
				mHandler.post(new Runnable() {
					public void run() {
						button.setIconResource(fontIconResource);
					}
				});
			}
		}
		if (iconPosition != null) {// left top right bottom
			mHandler.post(new Runnable() {
				public void run() {
					button.setIconPosition(mService.getResources()
							.getIdentifier(iconPosition, "id",
									mService.getPackageName()));
				}
			});
		}
		if (borderColor != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setBorderColor(mFullScreenTask.getInflater()
							.getColor(borderColor));
				}
			});
		}
		if (borderWidth != null) {
			mHandler.post(new Runnable() {
				public void run() {
					button.setBorderWidth(borderWidth);
				}
			});
		}
	}

	@SuppressWarnings("deprecation")
	@Rpc(description = "Set Gif View\nGif动画控件")
	public void setGifView(
			@RpcParameter(name = "id", description = "The View Id") String id,
			@RpcParameter(name = "image", description = "The gif path\ngif文件路径") String path)
			throws Exception {
		if (mFullScreenTask == null) {
			throw new RuntimeException("No screen displayed.");
		}
		if (path == null) {
			throw new RuntimeException("No Found The Path: " + path);
		}
		View view = mFullScreenTask.getViewByName(id);
		if (view == null) {
			throw new RuntimeException("No Found The Id: " + id);
		}
		final GifImageView gifImageView;
		try {
			gifImageView = (GifImageView) view;
		} catch (Exception e) {
			throw new RuntimeException("This View Can't Use Gif");
		}
		final GifDrawable gifDrawable;
		try {
			gifDrawable = new GifDrawable(new File(path));
			if (gifDrawable != null) {
				mFullScreenTask.addGifTask(id, gifDrawable);
				if (Build.VERSION.SDK_INT >= 16) {
					mHandler.post(new Runnable() {
						public void run() {
							gifImageView.setBackground(gifDrawable);
						}
					});
				} else {
					mHandler.post(new Runnable() {
						public void run() {
							gifImageView.setBackgroundDrawable(gifDrawable);
						}
					});
				}
				if (!gifDrawable.isPlaying()) {
					mHandler.post(new Runnable() {
						public void run() {
							gifDrawable.start();
						}
					});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置数字进度条进度
	 * 
	 * @param id
	 * @param current
	 * @param hideText
	 * @throws Exception
	 */
	// @Rpc(description = "Set the progress.")
	// public void setNumberProgress(
	// @RpcParameter(name = "id", description = "The NumberProgressBar Id")
	// String id,
	// @RpcParameter(name = "current", description = "current progress") final
	// Integer current,
	// @RpcParameter(name = "hideText", description = "nedd hide the text?")
	// @RpcDefault("false") final Boolean hideText)
	// throws Exception {
	// final NumberProgressBar np = npb.get(id);
	// if (np == null) {// 未找到
	// throw new RuntimeException(
	// "The Id NumberProgressBar Not Found,Are You Create?");
	// }
	// if (mFullScreenTask == null) {
	// throw new RuntimeException("No screen displayed.");
	// }
	// View v = mFullScreenTask.getViewByName(id);
	// if (v == null) {
	// throw new RuntimeException("No Found The Id: " + id);
	// }
	// if (hideText != null) {
	// mHandler.post(new Runnable() {
	// public void run() {
	// if (!hideText) {
	// np.setProgress(current);
	// } else {
	// np.setProgressTextVisibility(ProgressTextVisibility.Invisible);
	// }
	// }
	// });
	// }
	// }

	@Override
	public void shutdown() {
		fullDismiss();
		HtmlActivityTask.shutdown();
	}

	private class UiMenuItem {

		private final String mmTitle;
		private final String mmEvent;
		private final Object mmEventData;
		private final String mmIcon;
		private final MenuItem.OnMenuItemClickListener mmListener;

		public UiMenuItem(String title, String event, Object data, String icon) {
			mmTitle = title;
			mmEvent = event;
			mmEventData = data;
			mmIcon = icon;
			mmListener = new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					mEventFacade.postEvent(mmEvent, mmEventData);
					return true;
				}
			};
		}
	}

	private class UiArcMenuItem {

		private final String mmEvent;
		private final Object mmEventData;
		private final String mmIcon;
		private final OnClickListener mmListener;

		public UiArcMenuItem(String event, Object data, String icon) {
			mmEvent = event;
			mmEventData = data;
			mmIcon = icon;
			mmListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					mEventFacade.postEvent(mmEvent, mmEventData);
				}
			};
		}
	}

}