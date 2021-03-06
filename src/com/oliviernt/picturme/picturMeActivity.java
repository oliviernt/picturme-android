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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.provider.MediaStore;
import lib.Config;
import lib.HttpHelper;
import lib.BitmapHelper;

public class picturMeActivity extends Activity implements OnClickListener {

	private Button select;
	private Button take;
	private Button process;
	private Button select_new;

	private Intent intent;
	private ImageView preview_image;
	private Bitmap bmp = null;
	private JSONObject finalResult;
	private ProgressDialog dialog;
	private OnClickListener context;
	private String message;
	private String url;
	private String thumbnail;

	protected final Handler handler = new Handler();
	private ExifInterface exif;

	private static final String TAG = "PICTURLOG";
	private static final int CODE_SELECT = 1;
	private static final int CODE_TAKE = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		context = this;

		if (!HttpHelper.hasNetworkConnection(this)) {
			message = "No upload possible without a network connection!";
			Config.toast(this, message);
		}

		/* Get view elements */
		take = (Button) this.findViewById(R.id.take_picture_btn);
		process = (Button) this.findViewById(R.id.process_data_btn);
		select = (Button) this.findViewById(R.id.select_picture_btn);
		select_new = (Button) this.findViewById(R.id.select_other_btn);
		preview_image = (ImageView) this.findViewById(R.id.preview_image);

		/* Set processing button to hidden */
		process.setVisibility(View.GONE);
		select_new.setVisibility(View.GONE);

		/* Bind listener to buttons */
		take.setOnClickListener(this);
		select.setOnClickListener(this);
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
			select_new.setVisibility(View.GONE);
			if (!HttpHelper.hasNetworkConnection(this)) {
				message = "No upload possible without a network connection!";
				Config.toast(this, message);
				take.setVisibility(View.VISIBLE);
				select.setVisibility(View.VISIBLE);
				return;
			}
			take.setVisibility(View.GONE);
			select.setVisibility(View.GONE);

			if (bmp != null) {
				dialog = ProgressDialog.show(this, "", "Uploading image...",
						true);
				// upload the image to the server
				Log.d(TAG, "Uploading the image");
				Thread t = new Thread() {
					@Override
					public void run() {
						bmp = BitmapHelper.scaleBmp(bmp, 500);
						try {
							HttpResponse response = HttpHelper
									.uploadBitmap(bmp);

							BufferedReader reader = new BufferedReader(
									new InputStreamReader(response.getEntity()
											.getContent(), "UTF-8"));
							StringBuilder builder = new StringBuilder();
							for (String line = null; (line = reader.readLine()) != null;) {
								builder.append(line).append("\n");
							}
							finalResult = new JSONObject(builder.toString());

						} catch (Exception e) {
							e.printStackTrace();
							message = "An error occured! Please try again.";
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								handleData();
							}
						});
					}
				};
				t.start();
			}
		}

		if (v == preview_image) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(url));
			startActivity(browserIntent);
		}
	}

	private String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = this.managedQuery(contentUri, proj, null,
				null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		// if an image was selected get data from uri
		if (requestCode == CODE_SELECT) {
			Uri uri = data.getData();
			try {
				exif = new ExifInterface(getRealPathFromURI(uri));
				bmp = MediaStore.Images.Media.getBitmap(
						this.getContentResolver(), uri);

				int rotate = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL) * 90 - 90;

				Matrix mtx = new Matrix();
				mtx.postRotate(rotate);
				Log.d("ROTATE", "" + rotate);
				bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
						bmp.getHeight(), mtx, true);

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
			bmp = BitmapHelper.scaleBmp(bmp);

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
		take.setVisibility(View.VISIBLE);
		select.setVisibility(View.VISIBLE);

		process.setVisibility(View.GONE);
		select_new.setVisibility(View.GONE);

		message = "An error occured! Please try again.";
		boolean success = false;
		try {
			success = finalResult.getBoolean("success");
			url = "http://pictur.me";
			url += finalResult.getString("path");
			thumbnail = finalResult.getString("thumbnail");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
		if (success) {
			message = "Picture was uploaded successfuly!";
		}
		dialog.dismiss();
		Config.toast(this, message);

		Thread t = new Thread() {
			@Override
			public void run() {
				final Bitmap b = HttpHelper.getBitmapFromURL(thumbnail);
				preview_image.post(new Runnable() {
					@Override
					public void run() {
						preview_image.setImageBitmap(b);
						preview_image.setVisibility(View.VISIBLE);
						preview_image.setOnClickListener(context);
					}
				});
			}
		};
		t.start();
	}
}