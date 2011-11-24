package lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class Config {
	public static String IMAGE_BYTE_ARRAY = "image_byte_array";
	public static String UPLOAD_URL = "http://www.pictur.me/upload.ajax";

	public static void toast(Context c, String msg) {
		Toast t = Toast.makeText(c, msg, Toast.LENGTH_LONG);
		t.show();
	}

	/**
	 * check if the context has a network conntection
	 * 
	 * @param c
	 * @return
	 */
	public static boolean hasNetworkConnection(Context c) {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}

	/**
	 * scale the bitmap to a long edge of 500
	 * 
	 * @param Bitmap
	 * @return
	 */
	public static Bitmap scaleBmp(Bitmap bmp) {
		return scaleBmp(bmp, 500);
	}

	/**
	 * scale the bitmap to a long edge of max
	 * 
	 * @param Bitmap
	 * @param Integer
	 * @return Bitmap
	 */
	public static Bitmap scaleBmp(Bitmap bmp, int max) {
		float bmpWidth = bmp.getWidth();
		float bmpHeight = bmp.getHeight();

		if (bmpWidth < bmpHeight) {
			bmpWidth = (bmpWidth / bmpHeight) * max;
			bmpHeight = max;
		} else {
			bmpHeight = (bmpHeight / bmpWidth) * max;
			bmpWidth = max;
		}
		if (bmpWidth > 0 && bmpWidth > 0) {
			bmp = Bitmap.createScaledBitmap(bmp, (int) bmpWidth,
					(int) bmpHeight, true);
		}
		return bmp;
	}

	/**
	 * get bitmap data from a url
	 * 
	 * @param String
	 * @return Bitmap
	 */
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

	/**
	 * uploads a bitmap to UPLOAD_URL
	 * 
	 * @param Bitmap
	 * @return HttpResponse
	 */
	public static HttpResponse uploadBitmap(Bitmap bmp) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 0, bos);
		byte[] bitmapdata = bos.toByteArray();

		String encoded = Base64.encodeBytes(bitmapdata);
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		nameValuePairs.add(new BasicNameValuePair("file", encoded));
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(UPLOAD_URL);
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			return httpclient.execute(httppost);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
