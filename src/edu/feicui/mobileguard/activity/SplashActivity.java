package edu.feicui.mobileguard.activity;

import java.io.File;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;
import com.lidroid.xutils.view.annotation.ViewInject;

import edu.feicui.mobileguard.R;
import edu.feicui.mobileguard.commons.ActivityUtils;
import edu.feicui.mobileguard.commons.CommonUtils;
import edu.feicui.mobileguard.commons.GsonUtils;
import edu.feicui.mobileguard.commons.LogUtils;
import edu.feicui.mobileguard.entity.GuardVersion;

/**
 * 引导页面
 * @author chenyun
 * @email ccyy30@163.com
 */
public class SplashActivity extends AppCompatActivity {

	//播放动画的控件
	@ViewInject(R.id.layout_splash_root)
	private RelativeLayout root;
	
	//显示百分比
	@ViewInject(R.id.tv_splash_point)
	private TextView tvPoint;
	
	//动画持续时间,开始角度,结束角度
	private static final int ANIMATION_DURATION = 2000;
	private static final int ANIMATION_STARTANGEL = 0;
	private static final int ANIMATION_ENDANGEL = 360;
	
	private ActivityUtils activityUtils;
	//必须展示完动画才能够进行跳转,所以这里要记录动画播放的时间为2秒
	private long startTime,endTime;

	private HttpUtils http;
	private GuardVersion guardVersion;
	private static File file;
	
	static{
		
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			file = new File(
					Environment.getExternalStorageDirectory().getPath()
					+"/mobileguard/MobileGuard.apk");
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		deleteApk();
		
		initView();
		
		getVersionCode();
		
		initAnimation();
		
	}

	/** 删除apk*/
	public void deleteApk() {
		if(file != null && file.exists()){
			file.delete();
		}
	}

	/** 网络请求获取服务器软件版本*/
	private void getVersionCode() {
		http = new HttpUtils();
		http.send(HttpMethod.GET,
		    "http://192.168.1.223:8080/mobileguardversion.json",
		    versionCodeCallBack);
	}
	
	//获取最新版本的GET请求
	private RequestCallBack<String> versionCodeCallBack = new RequestCallBack<String>(){

		@Override
		public void onFailure(HttpException e, String s) {
			LogUtils.i(s);
			activityUtils.showToast(R.string.error);
			navigationToHome();
		}

		@Override
		public void onSuccess(ResponseInfo<String> info) {
			//解析数据
			LogUtils.i(info.result);
	    	guardVersion = parserGuardVersion(info);
	    	//记录请求结束时间
	    	endTime = System.currentTimeMillis();
	    	//判断如果请求结束时动画还没有结束,则要播放完动画
	    	if(endTime - startTime < ANIMATION_DURATION){
	    		SystemClock.sleep(ANIMATION_DURATION - (endTime - startTime));
	    	}
	    	
	    	//判断版本是否需要更新
	    	int version = guardVersion.getVersion();
	    	if(version <= CommonUtils.getInstance(SplashActivity.this).getVersionCode()){
	    		//不需要更新
	    		activityUtils.showToast(R.string.splash_version_not_update);
	    		navigationToHome();
	    	}else{
	    		//需要更新,弹出对话框
	    		showUpdateDialog();
	    	}
	    	
		}

	};
	
	/** 跳转到主界面,销毁当前界面*/
	public void navigationToHome() {
		activityUtils.startActivity(HomeActivity.class);
		SplashActivity.this.finish();
	}
	
	/** 解析结果*/
	public GuardVersion parserGuardVersion(ResponseInfo<String> info) {
		return GsonUtils.getInstance().
    			fromJson(
    					info.result, 
    					new TypeToken<GuardVersion>(){}.getType());
	}
	
	/** 显示是否要更新的对话框*/
	private void showUpdateDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.splash_dialog_title)
		.setMessage(getString(R.string.splash_dialog_msg)+guardVersion.getDesc())
		.setOnCancelListener(new OnCancelListener() {//取消对话框的监听,防止用户点击back跳转不到主界面
			
			@Override
			public void onCancel(DialogInterface dialog) {
				navigationToHome();
			}
		})
		.setPositiveButton(R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				if(file != null){
					//创建文件夹
					if(!file.getParentFile().exists()){
						file.getParentFile().mkdirs();
						
					}
					
					//下载更新apk,网络请求
					http.download(
							"http://192.168.1.223:8080/MobileGuard.apk", 
							file.getPath(), 
							true, 
							true,
							downloadApkCallBack);
					return;
				}
				
				activityUtils.showToast("没有检测到SD卡,请大虾将卡插入手机");
				SplashActivity.this.finish();
			}
		}).setNegativeButton(R.string.cancel, new OnClickListener() {//取消
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				navigationToHome();
			}
		})
		.show();
	}

	//下载apk的请求
	private RequestCallBack<File> downloadApkCallBack = new RequestCallBack<File>() {

        @Override
        public void onStart() {
        	LogUtils.i("开始下载apk");
        	tvPoint.setText("0%");
        }

        @Override
        public void onLoading(long total, long current, boolean isUploading) {
        	if((float)current / total * 100 >= 1){
        		tvPoint.setText(CommonUtils.keepDecimalPoint((float)current/total*100)+"%");
        	}
        }

        @Override
        public void onSuccess(ResponseInfo<File> responseInfo) {
        	LogUtils.i("下载apk成功");
        	
        }

        @Override
        public void onFailure(HttpException error, String msg) {
        	LogUtils.i("下载apk失败");
        	activityUtils.showToast(R.string.error);
			navigationToHome();
        }
	};
	
	/** 显示引导页面动画,透明,旋转,缩放*/
	private void initAnimation() {
		AnimationSet animSet = new AnimationSet(true);
		
		AlphaAnimation alphaAnim = new AlphaAnimation(0f, 1f);
		alphaAnim.setDuration(ANIMATION_DURATION);
		alphaAnim.setFillAfter(true);
		
		//旋转动画,构造函数参数Animation.RELATIVE_TO_SELF表示锚点是相对于自身还是父控件
		RotateAnimation rotateAnim = new RotateAnimation(ANIMATION_STARTANGEL, ANIMATION_ENDANGEL, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		rotateAnim.setDuration(ANIMATION_DURATION);
		rotateAnim.setFillAfter(true);
		
		ScaleAnimation scaleAnim = new ScaleAnimation(
				0f, 1f, 
				0f, 1f, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		scaleAnim.setDuration(ANIMATION_DURATION);
		scaleAnim.setFillAfter(true);
		
		animSet.addAnimation(alphaAnim);
		animSet.addAnimation(rotateAnim);
		animSet.addAnimation(scaleAnim);
		
		startTime = System.currentTimeMillis();
		//开始动画
		root.startAnimation(animSet);
	}

	/** 安装apk*/
	public void installApk(){
		Intent intent = new Intent(Intent.ACTION_VIEW);  
        intent.setDataAndType(Uri.fromFile(file),  
                "application/vnd.android.package-archive");  
        startActivityForResult(intent,0);
	}
	
	@Override
	protected void onActivityResult(int arg0, int arg1, Intent data) {
		super.onActivityResult(arg0, arg1, data);
		//无论是否安装apk都需要跳转到主界面
		navigationToHome();
	}
	
	/** 初始化布局*/
	private void initView() {
		setContentView(R.layout.activity_splash);
		ViewUtils.inject(this); //注入view和事件
		activityUtils = new ActivityUtils(this);
	}
	
}
