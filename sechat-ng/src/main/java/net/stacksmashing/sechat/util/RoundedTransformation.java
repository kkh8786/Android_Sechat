package net.stacksmashing.sechat.util;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

/**
 * Created by kulikov on 15.12.2014.
 */

/**
 * Rounded Corner Image Transformation for square's Picasso
 */
public class RoundedTransformation implements Transformation {
    private final int radius;
    private final int margin;

    // radius is corner radii in px
    // margin is the board in px
    public RoundedTransformation(final int radius, final int margin) {
        this.radius = radius;
        this.margin = margin;
    }

    @Override
    public Bitmap transform(final Bitmap source) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(
                new RectF(margin, margin, source.getWidth() - margin, source.getHeight() - margin),
                radius, radius, paint);
        if (source != output) {
            source.recycle();
        }
        return output;
    }

    @Override
    public String key() {
        return "rounded(radius=" + radius + ", margin=" + margin + ")";
    }
}