package net.stacksmashing.sechat.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.lang.ref.WeakReference;

public class QrCodeGenerator extends AsyncTask<String, Void, Bitmap> {
    private static final int QR_CODE_SCALE_FACTOR = 10;

    private final WeakReference<ImageView> imageViewRef;

    public QrCodeGenerator(ImageView imageView) {
        imageViewRef = new WeakReference<>(imageView);
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(params[0], BarcodeFormat.QR_CODE, 0, 0);
        }
        catch (Exception e) {
            Log.d("QrCodeGenerator", "Failed to encode data into QR code", e);
            return null;
        }
        int width = bitMatrix.getWidth() * QR_CODE_SCALE_FACTOR;
        int height = bitMatrix.getHeight() * QR_CODE_SCALE_FACTOR;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < height; x++) {
                pixels[offset + x] = bitMatrix.get(x / QR_CODE_SCALE_FACTOR, y / QR_CODE_SCALE_FACTOR) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        ImageView imageView = imageViewRef.get();
        if (imageView != null && bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
