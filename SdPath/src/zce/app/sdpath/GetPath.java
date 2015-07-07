package zce.app.sdpath;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class GetPath {

	public File path(Context context) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// 如果有sd卡挂载
			File path = Environment.getExternalStorageDirectory();
			if (path.length() < 5) {
				// 肯定不正确
				MountInfo dev = MountInfo.getInstance();
				DevInfo info;
				info = dev.getExternalInfo();// External SD Card Informations
				if (info == null) {// 先获取外置sd卡路径
					info = dev.getInternalInfo();
					// Internal SD Card Informations
					if (info == null) {// 获取内置sd卡路径
						return null;
					} else {
						return new File(info.getPath());
					}
				} else {
					return new File(info.getPath());
				}
			} else {
				return path;
			}
		} else {
			// 返回内部储存路径
			return context.getFilesDir();// 内部储存路径
		}
	}
}
