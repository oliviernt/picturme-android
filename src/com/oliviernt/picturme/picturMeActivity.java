package com.oliviernt.picturme;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.provider.MediaStore;
import lib.Base64;

public class picturMeActivity extends Activity implements OnClickListener {

	private Button select;
	private Button take;
	private Button process;
	private Intent intent;
	private ImageView preview_image;
	private Bitmap bmp = null;
	private String message;

	private JSONObject finalResult;

	ArrayList<NameValuePair> nameValuePairs;

	private static final int CODE_SELECT = 1;
	private static final int CODE_TAKE = 2;
	protected final Handler handler = new Handler();
	protected Runnable runner = new Runnable() {
		@Override
		public void run() {
			handleData();
		}
	};

	String TAG = "PICTURLOG";
	private ProgressDialog dialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		select = (Button) this.findViewById(R.id.select_picture_btn);
		select.setOnClickListener(this);

		take = (Button) this.findViewById(R.id.take_picture_btn);
		take.setOnClickListener(this);
		// take.setClickable(false);// disable take button as it doesn't work

		process = (Button) this.findViewById(R.id.process_data_btn);
		process.setOnClickListener(this);
		process.setVisibility(View.GONE);

		preview_image = (ImageView) this.findViewById(R.id.preview_image);
	}

	@Override
	public void onClick(View v) {

		if (v == select) {
			// open the image gallery intent
			intent = new Intent(Intent.ACTION_PICK,
					MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			startActivityForResult(intent, CODE_SELECT);
		}

		if (v == take) {
			// open the camera intent
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, CODE_TAKE);
		}

		if (v == process) {
			preview_image.setVisibility(View.GONE);
			process.setVisibility(View.GONE);
			take.setVisibility(View.GONE);
			select.setVisibility(View.GONE);

			if (bmp != null) {
				dialog = ProgressDialog.show(this, "", "Loading...", true);
				// upload the image to the server
				Log.d(TAG, "Uploading the image");
				Thread t = new Thread() {

					public void run() {
						try {
							scaleBmp();
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							bmp.compress(CompressFormat.PNG, 0, bos);
							byte[] bitmapdata = bos.toByteArray();

							String encoded = Base64.encodeBytes(bitmapdata);

							nameValuePairs = new ArrayList<NameValuePair>();

							nameValuePairs.add(new BasicNameValuePair("file",
									encoded));
							Log.d(TAG, "Uploading...");
							String url = "http://www.pictur.me/upload.ajax";
							HttpClient httpclient = new DefaultHttpClient();
							HttpPost httppost = new HttpPost(url);
							httppost.setEntity(new UrlEncodedFormEntity(
									nameValuePairs));
							HttpResponse response = httpclient
									.execute(httppost);
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(response.getEntity()
											.getContent(), "UTF-8"));
							StringBuilder builder = new StringBuilder();
							for (String line = null; (line = reader.readLine()) != null;) {
								builder.append(line).append("\n");
								Log.d("LINE", line);
							}
							Log.d("JSON", builder.toString());
							finalResult = new JSONObject(builder.toString());

						} catch (Exception e) {
							e.printStackTrace();
							message = "An error occured! Please try again.";
						}
						handler.post(runner);
					}
				};
				t.run();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		// if an image was selected get data from uri
		if (requestCode == CODE_SELECT) {
			Uri uri = (Uri) data.getData();
			try {
				bmp = MediaStore.Images.Media.getBitmap(
						this.getContentResolver(), uri);
				preview_image.setImageBitmap(bmp);
				preview_image.setVisibility(View.VISIBLE);
				process.setVisibility(View.VISIBLE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			}
		}
		// if an image was made with the camera, get the bitmap data
		if (requestCode == CODE_TAKE && resultCode == Activity.RESULT_OK) {
			bmp = (Bitmap) data.getExtras().get("data");
			scaleBmp();
			preview_image.setImageBitmap(bmp);
			preview_image.setVisibility(View.VISIBLE);
			process.setVisibility(View.VISIBLE);
		}
	}

	public boolean scaleBmp() {

		float bmpWidth = bmp.getWidth();
		float bmpHeight = bmp.getHeight();

		int width = 500;

		bmpHeight = (bmpHeight / bmpWidth) * 500;
		int height = (int) bmpHeight;

		if (bmp.getHeight() > bmp.getWidth()) {
			height = 500;
			bmpWidth = (bmp.getWidth() / bmp.getHeight()) * 500;
			width = (int) bmpWidth;
		}

		if (height > 0 && width > 0) {
			bmp = Bitmap.createScaledBitmap(bmp, (width), (height), true);
			preview_image.setImageBitmap(bmp);
			return true;
		} else {
			return false;
		}
	}

	public void handleData() {
		Log.d(TAG, "Uploaded!");
		take.setVisibility(View.VISIBLE);
		select.setVisibility(View.VISIBLE);
		dialog.dismiss();

		message = "An error occured! Please try again.";
		boolean success = false;
		String url = "http://pictur.me";

		try {
			success = finalResult.getBoolean("success");
			url += finalResult.getString("url_path");
		} catch (Exception e) {
		}
		if (success) {
			message = "Picture was uploaded successfuly!";
		}
		dialog.dismiss();
		Toast.makeText(this, message, Toast.LENGTH_LONG);
		preview_image.setImageBitmap(getBitmapFromURL(url));
	}

	public static Bitmap getBitmapFromURL(String src) {
		try {
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}