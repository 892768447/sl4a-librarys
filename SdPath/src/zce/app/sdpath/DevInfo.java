package zce.app.sdpath;

public class DevInfo {
	public String label, mount_point, path, sysfs_path;

	/**
	 * return the label name of the SD card
	 * 
	 * @return
	 */
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * the mount point of the SD card
	 * 
	 * @return
	 */
	public String getMount_point() {
		return mount_point;
	}

	public void setMount_point(String mount_point) {
		this.mount_point = mount_point;
	}

	/**
	 * SD mount path
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * "unknow"
	 * 
	 * @return
	 */
	public String getSysfs_path() {
		return sysfs_path;
	}

	public void setSysfs_path(String sysfs_path) {
		this.sysfs_path = sysfs_path;
	}

}
