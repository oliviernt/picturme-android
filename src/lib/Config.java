package lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

public class Config {
	public static String IMAGE_BYTE_ARRAY = "image_byte_array";
	public static String UPLOAD_URL = "http://www.pictur.me/upload.ajax";

	public static void toast(Context c, String msg) {
		Toast t = Toast.makeText(c, msg, Toast.LENGTH_LONG);
		t.show();
	}
}
