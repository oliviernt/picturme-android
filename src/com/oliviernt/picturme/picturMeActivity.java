package com.oliviernt.picturme;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.provider.MediaStore;
import lib.Config;

public class picturMeActivity extends Activity implements OnClickListener {

	private Button select;
	private Button take;
	private Button process;
	private Intent intent;
	private ImageView preview_image;
	private Bitmap bmp = null;
	private String message;
	private JSONObject finalResult;
	private ProgressDialog dialog;
	protected Runnable runner = new Runnable() {
		@Override
		public void run() {
			handleData();
		}
	};

	private final String TAG = "PICTURLOG";

	private static final int CODE_SELECT = 1;
	private static final int CODE_TAKE = 2;

	protected final Handler handler = new Handler();
	private Button select_new;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		if (!Config.hasNetworkConnection(this)) {
			message = "No upload possible without a network connection!";
			Config.toast(this, message);
		}

		/* Get view elements */
		select = (Button) this.findViewById(R.id.select_picture_btn);
		take = (Button) this.findViewById(R.id.take_picture_btn);
		process = (Button) this.findViewById(R.id.process_data_btn);
		select_new = (Button) this.findViewById(R.id.select_other_btn);
		preview_image = (ImageView) this.findViewById(R.id.preview_image);

		/* Set processing button to hidden */
		process.setVisibility(View.GONE);
		select_new.setVisibility(View.GONE);

		/* Bind listener to buttons */
		select.setOnClickListener(this);
		take.setOnClickListener(this);
		process.setOnClickListener(this);
		select_new.setOnClickListener(this);
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
		
		if (v == select_new) {
			select_new.setVisibility(View.GONE);
			process.setVisibility(View.GONE);
			take.setVisibility(View.VISIBLE);
			select.setVisibility(View.VISIBLE);
		}

		if (v == process) {
			preview_image.setVisibility(View.GONE);
			process.setVisibility(View.GONE);

			if (!Config.hasNetworkConnection(this)) {
				message = "No upload possible without a network connection!";
				Config.toast(this, message);
				return;
			}

			take.setVisibility(View.GONE);
			select.setVisibility(View.GONE);

			if (bmp != null) {
				dialog = ProgressDialog.show(this, "", "Loading...", true);
				// upload the image to the server
				Log.d(TAG, "Uploading the image");
				Thread t = new Thread() {
					public void run() {
						try {
							bmp = Config.scaleBmp(bmp, 500);

							HttpResponse response = Config.uploadBitmap(bmp);

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

				take.setVisibility(View.GONE);
				select.setVisibility(View.GONE);
				select_new.setVisibility(View.VISIBLE);
				process.setVisibility(View.VISIBLE);
				preview_image.setVisibility(View.VISIBLE);
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
			bmp = Config.scaleBmp(bmp);
			
			preview_image.setImageBitmap(bmp);
			
			take.setVisibility(View.GONE);
			select.setVisibility(View.GONE);
			select_new.setVisibility(View.VISIBLE);
			process.setVisibility(View.VISIBLE);
			preview_image.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * handle the data once the upload is finished
	 */
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
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
		if (success) {
			message = "Picture was uploaded successfuly!";
		}
		dialog.dismiss();
		Config.toast(this, message);
		preview_image.setImageBitmap(Config.getBitmapFromURL(url));
	}
}