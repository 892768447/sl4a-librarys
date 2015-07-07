package zce.app.crash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import zce.app.sdpath.GetPath;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;

public class CrashEmail {

	private File file;

	private static String readTextFile(File file) {
		InputStreamReader reader;
		BufferedReader in;
		StringBuffer text = new StringBuffer();
		int c;
		try {
			reader = new InputStreamReader(new FileInputStream(file));
			in = new BufferedReader(reader);
			do {
				c = in.read();
				if (c != -1) {
					text.append((char) c);
				}
			} while (c != -1);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			return null;
		}
		return text.toString();
	}

	public CrashEmail(final Context context, final String address) {
		file = new File(new GetPath().path(context), context.getPackageName()
				+ "_last_err.log");
		if (file.exists()) {
			new AlertDialog.Builder(context)
					.setTitle("错误处理")
					.setMessage("是否通过邮件提交错误报告\n有利于帮助开发者改进!")
					.setPositiveButton("发送",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent(
											Intent.ACTION_SEND);
									intent.setType("plain/text");
									intent.putExtra(Intent.EXTRA_EMAIL,
											new String[] { address });// 收件人
									intent.putExtra(Intent.EXTRA_SUBJECT,
											context.getPackageName() + " 错误报告");// 主题
									// intent.putExtra(Intent.EXTRA_TEXT,
									// "错误报告附件");// 内容
									intent.putExtra(Intent.EXTRA_TEXT,
											readTextFile(file));
									// intent.putExtra(
									// Intent.EXTRA_STREAM,
									// Uri.parse("file://"
									// + file.toString()));
									Intent.createChooser(intent,
											context.getPackageName() + " 错误报告");
									context.startActivity(intent);
									file.delete();
								}
							})
					.setNegativeButton("算了",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									file.delete();
								}
							}).setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							file.delete();
						}
					}).create().show();
		}
	}
}
