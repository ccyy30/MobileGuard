package edu.feicui.mobileguard.commons;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class CommonUtils {

	private static CommonUtils commonUtils;
	private PackageManager packageManager;
	private Context context;
	
	private CommonUtils(Context context){
		this.context = context;
		packageManager = context.getPackageManager();
	}
	
	public static CommonUtils getInstance(Context context){
		if(commonUtils == null){
			commonUtils = new CommonUtils(context);
		}
		return commonUtils;
	}
	
	/** ��ȡ��ǰӦ�ó���汾*/
	public int getVersionCode(){
		PackageInfo packageInfo = null;
		try {
			packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			LogUtils.i("����û���ҵ�");
		}
		return packageInfo.versionCode;
	}
	
	/** ���ֱ�����λС��*/
	public static String keepDecimalPoint(double number){
		return new DecimalFormat("#.00").format(number);
	}
}
