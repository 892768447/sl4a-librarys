package com.googlecode.android_scripting.extras;

import java.util.List;

import zce.app.sdpath.DevInfo;
import zce.app.sdpath.MountInfo;
import android.app.Service;
import android.os.Build;
import android.os.Environment;

import com.google.common.collect.Lists;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;

/**
 * 
 * @author ざ凍結の→愛 892768447@qq.com
 */
public class CustomAndroidFacade extends RpcReceiver {

	private final Service mService;

	public CustomAndroidFacade(FacadeManager manager) {
		super(manager);
		mService = manager.getService();
	}

	public void shutdown() {
	}

	@Rpc(description = "Get SdPath\n获取sd卡路径", returns = "Return List\n返回数组")
	public List<String> getSdPath() {
		List<String> result = Lists.newArrayList();
		MountInfo dev = MountInfo.getInstance();
		DevInfo info;
		info = dev.getInternalInfo();// Internal SD Card Informations
		if (info == null) {
			result.add("");
		} else {
			// 获取内置sd卡路径
			// info.getLabel(); // SD 卡的名称
			// info.getMount_point();// SD 卡挂载点
			result.add(info.getPath());// SD 卡路径
			// info.getSysfs_path(); // ....没弄清楚什么意思
		}
		info = dev.getExternalInfo();// External SD Card Informations
		if (info == null) {
			result.add("");
		} else {
			// 获取外置sd卡路径
			// info.getLabel(); // SD 卡的名称
			// info.getMount_point();// SD 卡挂载点
			result.add(info.getPath()); // SD 卡路径
			// info.getSysfs_path(); // ....没弄清楚什么意思
		}
		if (result.get(0).length() < 1 && result.get(1).length() < 1) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				try {
					result.add(Environment.getExternalStorageDirectory()
							.toString());
				} catch (Exception e) {
					result.add("");
				}
			}
			result.add("");
		}
		return result;
	}

	@Rpc(description = "Get App FilesDir\n获取内部储存路径", returns = " Return String")
	public String getFilesDir() {
		return mService.getFilesDir().getAbsolutePath();
	}

	@Rpc(description = "Get The OS Sdk\n获取系统版本", returns = " Return Integer")
	public int getSdk() {
		return Build.VERSION.SDK_INT;
	}

	@Rpc(description = "Get The OS Release\n获取系统版本", returns = " Return String")
	public String getRelease() {
		return Build.VERSION.RELEASE;
	}

}