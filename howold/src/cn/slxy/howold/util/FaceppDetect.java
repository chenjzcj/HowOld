package cn.slxy.howold.util;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.graphics.Bitmap;
import android.util.Log;

public class FaceppDetect {
	public interface CallBack{
		void success(JSONObject jsonObject);
		void error(FaceppParseException exception);
	}
	public static void detect(final Bitmap bm,final CallBack callBack){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpRequests requests = new HttpRequests(Constant.Key, Constant.Secret, true, true);
					Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.JPEG,100, stream);
					byte[] array = stream.toByteArray();
					PostParameters parameters = new PostParameters();
					parameters.setImg(array);
					JSONObject jsonObject = requests.detectionDetect(parameters);
					Log.i("TAG", jsonObject.toString());
					if(callBack!=null){
						callBack.success(jsonObject);
					}
				} catch (FaceppParseException e) {
					e.printStackTrace();
					if(callBack!=null){
						callBack.error(e);
					}
				}
			}
		}).start();
	}
}
