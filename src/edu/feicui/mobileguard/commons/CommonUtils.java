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
	
	/** 获取当前应用程序版本*/
	public int getVersionCode(){
		PackageInfo packageInfo = null;
		try {
			packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			LogUtils.i("包名没有找到");
		}
		return packageInfo.versionCode;
	}
	
	/** 数字保留两位小数*/
	public static String keepDecimalPoint(double number){
		return new DecimalFormat("#.00").format(number);
	}
}
