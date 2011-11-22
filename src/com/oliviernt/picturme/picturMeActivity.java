package com.oliviernt.picturme;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Bitmap.CompressFormat;
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
import lib.Config;
import lib.Upload;

public class picturMeActivity extends Activity implements OnClickListener {

	private Button select;
	private Button take;
	private Button process;
	private Intent intent;
	private ImageView preview_image;
	private Bitmap bmp = null;
	private Bundle bundle;

	private Upload uploader;

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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		select = (Button) this.findViewById(R.id.select_picture_btn);
		select.setOnClickListener(this);

		take = (Button) this.findViewById(R.id.take_picture_btn);
		take.setOnClickListener(this);

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
			if (bmp != null) {
				scaleBmp();
				preview_image.setVisibility(View.GONE);
				process.setVisibility(View.GONE);
				take.setVisibility(View.GONE);
				select.setVisibility(View.GONE);
				// upload the image to the server
				Log.d(TAG, "Uploading the image");

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bmp.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
				byte[] bitmapdata = bos.toByteArray();

				String encoded = Base64.encodeBytes(bitmapdata);

				nameValuePairs = new ArrayList<NameValuePair>();

				nameValuePairs.add(new BasicNameValuePair("file", encoded));
				Thread t = new Thread() {
					public void run() {
						try {
							Log.d(TAG, "Uploading...");
							String url = "http://www.pictur.me/upload.ajax";
							HttpClient httpclient = new DefaultHttpClient();
							HttpPost httppost = new HttpPost(url);
							httppost.setEntity(new UrlEncodedFormEntity(
									nameValuePairs));
							HttpResponse response = httpclient
									.execute(httppost);
							Log.d(TAG, response.toString());

						} catch (Exception e) {
							e.printStackTrace();
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
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
			}
			process.setVisibility(View.VISIBLE);
		}
		// if an image was made with the camera, get the bitmap data
		if (requestCode == CODE_TAKE && resultCode == Activity.RESULT_OK) {
			bmp = (Bitmap) data.getExtras().get("data");
			preview_image.setImageBitmap(bmp);
			process.setVisibility(View.VISIBLE);
		}
	}

	public void scaleBmp() {

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

		bmp = Bitmap.createScaledBitmap(bmp, (width), (height), true);
	}

	public void handleData() {
		Log.d(TAG, "Uploaded!");
	}
}