package cn.slxy.howold;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.slxy.howold.util.FaceppDetect;
import cn.slxy.howold.util.FaceppDetect.CallBack;

import com.facepp.error.FaceppParseException;

public class MainActivity extends Activity implements OnClickListener {

	private static final int PIC_CODE = 0;
	protected static final int SUCCESS = 0x111;
	protected static final int ERROR = 0x112;
	private TextView mCount;
	private Button mSelect;
	private Button mSend;
	private ImageView mImage;
	private String mImageStr;
	private Bitmap mPhoto;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initEvent();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == PIC_CODE) {
			if (intent != null) {
				Uri uri = intent.getData();
				Cursor cursor = getContentResolver().query(uri, null, null,
						null, null);
				cursor.moveToFirst();
				int idx = cursor
						.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				mImageStr = cursor.getString(idx);
				cursor.close();
				resizePhoto();
				mImage.setImageBitmap(mPhoto);
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	/**
	 * 获得照片并压缩
	 */
	private void resizePhoto() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mImageStr, options);
		double ratio = Math.max(options.outWidth * 1.0d / 1024f,
				options.outHeight * 1.0d / 1024f);
		options.inSampleSize = (int) Math.ceil(ratio);
		options.inJustDecodeBounds = false;
		mPhoto = BitmapFactory.decodeFile(mImageStr, options);

	}

	private void initEvent() {
		mSelect.setOnClickListener(this);
		mSend.setOnClickListener(this);
	}

	private void initView() {
		mCount = (TextView) findViewById(R.id.tv_count);
		mSelect = (Button) findViewById(R.id.bt_select);
		mSend = (Button) findViewById(R.id.bt_send);
		mImage = (ImageView) findViewById(R.id.image);
		mWaitting = (FrameLayout) findViewById(R.id.waitting);
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case SUCCESS:
				mWaitting.setVisibility(View.GONE);
				JSONObject rs = (JSONObject) msg.obj;
				jsonRsBitmap(rs);
				mImage.setImageBitmap(mPhoto);
				break;
			case ERROR:
				mWaitting.setVisibility(View.GONE);
				Toast.makeText(MainActivity.this, "请检查网络连接", 0).show();
				break;

			default:
				break;
			}
		}
	};
	private FrameLayout mWaitting;

	/**
	 * json数据转化
	 * 
	 * @param rs
	 */
	private void jsonRsBitmap(JSONObject rs) {
		Bitmap bitmap = Bitmap.createBitmap(mPhoto.getWidth(),
				mPhoto.getHeight(), mPhoto.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(mPhoto, 0, 0, null);
		try {
			JSONArray faces = rs.getJSONArray("face");
			int faceCount = faces.length();
			mCount.setText("find " + faceCount);
			for (int i = 0; i < faceCount; i++) {
				JSONObject face = faces.getJSONObject(i);
				JSONObject position = face.getJSONObject("position");
				float x = (float) position.getJSONObject("center").getDouble(
						"x");
				float y = (float) position.getJSONObject("center").getDouble(
						"y");
				float w = (float) position.getDouble("width");
				float h = (float) position.getDouble("height");

				x = x / 100 * bitmap.getWidth();
				y = y / 100 * bitmap.getHeight();

				w = w / 100 * bitmap.getWidth();
				h = h / 100 * bitmap.getHeight();
				Paint mPaint = new Paint();
				mPaint.setColor(0xffffffff);
				mPaint.setStrokeWidth(3);
				canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2,
						mPaint);
				canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2,
						mPaint);
				canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2,
						mPaint);
				canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2,
						mPaint);

				int age = face.getJSONObject("attribute").getJSONObject("age")
						.getInt("value");
				String gender = face.getJSONObject("attribute")
						.getJSONObject("gender").getString("value");
				Bitmap ageBitmap = buildBitmap(age, "Male".equals(gender));
				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();
				if (bitmap.getWidth() < mImage.getWidth()
						&& bitmap.getHeight() < mImage.getHeight()) {
					float ratio = Math.max(
							bitmap.getWidth() * 1.0f / mImage.getWidth(),
							bitmap.getHeight() * 1.0f / mImage.getHeight());
					ageBitmap = Bitmap.createScaledBitmap(ageBitmap,
							(int) (ageBitmap.getWidth() * ratio),
							(int) (ageBitmap.getHeight() * ratio), false);
				}
				canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth(), y
						- h / 2 - ageBitmap.getHeight(), null);
				mPhoto = bitmap;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	/**
	 * 构建显示年龄和性别的bitmap
	 * 
	 * @param age
	 * @param equals
	 * @return
	 */
	private Bitmap buildBitmap(int age, boolean isMale) {
		TextView age_gender = (TextView) mWaitting
				.findViewById(R.id.age_and_gender);
		age_gender.setText(age + "");
		if (isMale) {
			age_gender.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.male), null, null, null);
		} else {
			age_gender.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.female), null, null, null);
		}
		age_gender.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(age_gender.getDrawingCache());
		age_gender.destroyDrawingCache();
		return bitmap;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_select:
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, PIC_CODE);
			break;
		case R.id.bt_send:
			mWaitting.setVisibility(View.VISIBLE);
			if(mImageStr!=null && !mImageStr.equals("")){
				resizePhoto();
			}else {
				mPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
			}
			FaceppDetect.detect(mPhoto, new CallBack() {
				@Override
				public void success(JSONObject jsonObject) {
					Message msg = Message.obtain();
					msg.obj = jsonObject;
					msg.what = SUCCESS;
					handler.sendMessage(msg);
				}

				@Override
				public void error(FaceppParseException exception) {
					Message msg = Message.obtain();
					msg.obj = exception.getErrorMessage();
					msg.what = ERROR;
					handler.sendMessage(msg);
				}
			});
			break;

		default:
			break;
		}
	}
}
