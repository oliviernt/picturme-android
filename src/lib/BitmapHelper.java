package lib;

import android.graphics.Bitmap;

public class BitmapHelper {

	/**
	 * scale the bitmap to a long edge of 500
	 * 
	 * @param Bitmap
	 * @return Bitmap
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

}
