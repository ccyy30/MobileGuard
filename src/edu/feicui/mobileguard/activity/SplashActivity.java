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
 * ����ҳ��
 * @author chenyun
 * @email ccyy30@163.com
 */
public class SplashActivity extends AppCompatActivity {

	//���Ŷ����Ŀؼ�
	@ViewInject(R.id.layout_splash_root)
	private RelativeLayout root;
	
	//��ʾ�ٷֱ�
	@ViewInject(R.id.tv_splash_point)
	private TextView tvPoint;
	
	//��������ʱ��,��ʼ�Ƕ�,�����Ƕ�
	private static final int ANIMATION_DURATION = 2000;
	private static final int ANIMATION_STARTANGEL = 0;
	private static final int ANIMATION_ENDANGEL = 360;
	
	private ActivityUtils activityUtils;
	//����չʾ�궯�����ܹ�������ת,��������Ҫ��¼�������ŵ�ʱ��Ϊ2��
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

	/** ɾ��apk*/
	public void deleteApk() {
		if(file != null && file.exists()){
			file.delete();
		}
	}

	/** ���������ȡ����������汾*/
	private void getVersionCode() {
		http = new HttpUtils();
		http.send(HttpMethod.GET,
		    "http://192.168.1.223:8080/mobileguardversion.json",
		    versionCodeCallBack);
	}
	
	//��ȡ���°汾��GET����
	private RequestCallBack<String> versionCodeCallBack = new RequestCallBack<String>(){

		@Override
		public void onFailure(HttpException e, String s) {
			LogUtils.i(s);
			activityUtils.showToast(R.string.error);
			navigationToHome();
		}

		@Override
		public void onSuccess(ResponseInfo<String> info) {
			//��������
			LogUtils.i(info.result);
	    	guardVersion = parserGuardVersion(info);
	    	//��¼�������ʱ��
	    	endTime = System.currentTimeMillis();
	    	//�ж�����������ʱ������û�н���,��Ҫ�����궯��
	    	if(endTime - startTime < ANIMATION_DURATION){
	    		SystemClock.sleep(ANIMATION_DURATION - (endTime - startTime));
	    	}
	    	
	    	//�жϰ汾�Ƿ���Ҫ����
	    	int version = guardVersion.getVersion();
	    	if(version <= CommonUtils.getInstance(SplashActivity.this).getVersionCode()){
	    		//����Ҫ����
	    		activityUtils.showToast(R.string.splash_version_not_update);
	    		navigationToHome();
	    	}else{
	    		//��Ҫ����,�����Ի���
	    		showUpdateDialog();
	    	}
	    	
		}

	};
	
	/** ��ת��������,���ٵ�ǰ����*/
	public void navigationToHome() {
		activityUtils.startActivity(HomeActivity.class);
		SplashActivity.this.finish();
	}
	
	/** �������*/
	public GuardVersion parserGuardVersion(ResponseInfo<String> info) {
		return GsonUtils.getInstance().
    			fromJson(
    					info.result, 
    					new TypeToken<GuardVersion>(){}.getType());
	}
	
	/** ��ʾ�Ƿ�Ҫ���µĶԻ���*/
	private void showUpdateDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.splash_dialog_title)
		.setMessage(getString(R.string.splash_dialog_msg)+guardVersion.getDesc())
		.setOnCancelListener(new OnCancelListener() {//ȡ���Ի���ļ���,��ֹ�û����back��ת����������
			
			@Override
			public void onCancel(DialogInterface dialog) {
				navigationToHome();
			}
		})
		.setPositiveButton(R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				if(file != null){
					//�����ļ���
					if(!file.getParentFile().exists()){
						file.getParentFile().mkdirs();
						
					}
					
					//���ظ���apk,��������
					http.download(
							"http://192.168.1.223:8080/MobileGuard.apk", 
							file.getPath(), 
							true, 
							true,
							downloadApkCallBack);
					return;
				}
				
				activityUtils.showToast("û�м�⵽SD��,���Ϻ���������ֻ�");
				SplashActivity.this.finish();
			}
		}).setNegativeButton(R.string.cancel, new OnClickListener() {//ȡ��
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				navigationToHome();
			}
		})
		.show();
	}

	//����apk������
	private RequestCallBack<File> downloadApkCallBack = new RequestCallBack<File>() {

        @Override
        public void onStart() {
        	LogUtils.i("��ʼ����apk");
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
        	LogUtils.i("����apk�ɹ�");
        	
        }

        @Override
        public void onFailure(HttpException error, String msg) {
        	LogUtils.i("����apkʧ��");
        	activityUtils.showToast(R.string.error);
			navigationToHome();
        }
	};
	
	/** ��ʾ����ҳ�涯��,͸��,��ת,����*/
	private void initAnimation() {
		AnimationSet animSet = new AnimationSet(true);
		
		AlphaAnimation alphaAnim = new AlphaAnimation(0f, 1f);
		alphaAnim.setDuration(ANIMATION_DURATION);
		alphaAnim.setFillAfter(true);
		
		//��ת����,���캯������Animation.RELATIVE_TO_SELF��ʾê��������������Ǹ��ؼ�
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
		//��ʼ����
		root.startAnimation(animSet);
	}

	/** ��װapk*/
	public void installApk(){
		Intent intent = new Intent(Intent.ACTION_VIEW);  
        intent.setDataAndType(Uri.fromFile(file),  
                "application/vnd.android.package-archive");  
        startActivityForResult(intent,0);
	}
	
	@Override
	protected void onActivityResult(int arg0, int arg1, Intent data) {
		super.onActivityResult(arg0, arg1, data);
		//�����Ƿ�װapk����Ҫ��ת��������
		navigationToHome();
	}
	
	/** ��ʼ������*/
	private void initView() {
		setContentView(R.layout.activity_splash);
		ViewUtils.inject(this); //ע��view���¼�
		activityUtils = new ActivityUtils(this);
	}
	
}
