package edu.feicui.mobileguard.commons;

import com.google.gson.Gson;

public class GsonUtils {

	private static Gson gson;
	private static GsonUtils gsonUtils;
	
	private GsonUtils(){
		gson = new Gson();
	}
	
	public static Gson getInstance(){
		if(gsonUtils == null){
			gsonUtils = new GsonUtils();
		}
		return gson;
	}
	
}
