package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ImageEditorActivity extends Activity implements View.OnTouchListener, Callback, View.OnClickListener {
    private static final String TAG = "ImageEditorActivity";

    private static final String EXTRA_FILE = "file";

    private static final int DEFAULT_COLOR = Color.RED;
    private static final int UNDO_LEVELS = 10;

    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 48;
    private static final int MIN_BRUSH_SIZE = 1;
    private static final int MAX_BRUSH_SIZE = 9;
    private static final int DEFAULT_FONT_SIZE = (MIN_FONT_SIZE + MAX_FONT_SIZE) / 2;
    private static final int DEFAULT_BRUSH_SIZE = MIN_BRUSH_SIZE + 1;

    public static Intent intentWithFile(Context context, File file) {
        Intent intent = new Intent(context, ImageEditorActivity.class);
        intent.putExtra(EXTRA_FILE, file);
        return intent;
    }

    @InjectView(R.id.activity_image_editor_accept)
    ImageButton acceptButton;

    @InjectView(R.id.activity_image_editor_discard)
    ImageButton discardButton;

    @InjectView(R.id.activity_image_editor_undo)
    ImageButton undoButton;

    @InjectView(R.id.activity_image_editor_text)
    ImageButton textButton;

    @InjectView(R.id.activity_image_editor_options)
    ImageButton optionsButton;

    @InjectView(R.id.activity_image_editor_cancel)
    ImageButton cancelButton;

    @InjectView(R.id.activity_image_editor_image)
    ImageView imageView;

    private Deque<Bitmap> undoStack;
    private Canvas canvas;
    private Bitmap originalBitmap;
    private Paint paint;
    private boolean imageLoaded = false;

    private float startX = 0, startY = 0;

    private boolean drawingText = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_editor);
        ButterKnife.inject(this);

        acceptButton.setOnClickListener(this);
        discardButton.setOnClickListener(this);
        undoButton.setOnClickListener(this);
        textButton.setOnClickListener(this);
        optionsButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        undoStack = new ArrayDeque<>();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setBrushSize(DEFAULT_BRUSH_SIZE);
        setFontSize(DEFAULT_FONT_SIZE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setColor(DEFAULT_COLOR);

        imageView.setOnTouchListener(this);

        int width = getResources().getDisplayMetrics().widthPixels; // FIXME: This is duplicated a few times.
        int height = getResources().getDisplayMetrics().heightPixels;
        Picasso.with(this)
                .load(getExtraFile())
                .skipMemoryCache()
                .resize(width, height)
                .centerInside()
                .into(imageView, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Picasso.with(this).cancelRequest(imageView);

        if (undoStack != null) {
            for (Bitmap bitmap : undoStack) {
                bitmap.recycle();
            }
            undoStack.clear();
        }
    }

    @Override
    public void onError() {
        finish();
    }

    @Override
    public void onSuccess() {
        originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        clearCanvas();
        imageLoaded = true;
    }

    private void clearCanvas() {
        canvas = null;
        imageView.setImageBitmap(originalBitmap.copy(Bitmap.Config.ARGB_8888, true));
    }

    private File getExtraFile() {
        return (File) getIntent().getSerializableExtra(EXTRA_FILE);
    }

    private void setColor(int color) {
        paint.setColor(color);
    }

    private int getColor() {
        return paint.getColor();
    }

    private void setBrushSize(int size) {
        if (size >= MIN_BRUSH_SIZE && size <= MAX_BRUSH_SIZE) {
            paint.setStrokeWidth(size * getResources().getDisplayMetrics().density);
        }
    }

    private int getBrushSize() {
        return (int) (paint.getStrokeWidth() / getResources().getDisplayMetrics().density);
    }

    private void setFontSize(int size) {
        if (size >= MIN_FONT_SIZE && size <= MAX_FONT_SIZE) {
            paint.setTextSize(size * getResources().getDisplayMetrics().density);
        }
    }

    private int getFontSize() {
        return (int) (paint.getTextSize() / getResources().getDisplayMetrics().density);
    }

    @Override
    public void onClick(View v) {
        if (v == acceptButton) {
            setResult(RESULT_OK);
            saveResult();
            finish();
        }
        else if (v == discardButton) {
            discardDrawing();
        }
        else if (v == undoButton) {
            undoDrawing();
        }
        else if (v == textButton) {
            setDrawingText(!drawingText);
        }
        else if (v == optionsButton) {
            OptionsFragment.newInstance(getBrushSize(), getFontSize(), getColor()).show(getFragmentManager(), "OptionsFragment");
        }
        else if (v == cancelButton) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void undoDrawing() {
        if (undoStack.isEmpty()) {
            return;
        }

        canvas = null;

        imageView.setImageBitmap(undoStack.pollLast());
    }

    private void saveResult() {
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            return;
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(getExtraFile());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        }
        catch (Exception e) {
            Log.e(TAG, "Could not save edited image", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void discardDrawing() {
        saveBitmapToUndoStack();
        clearCanvas();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        float[] m = new float[9];
        imageView.getImageMatrix().getValues(m);

        final float x = motionEvent.getX() / m[Matrix.MSCALE_X] - m[Matrix.MTRANS_X];
        final float y = motionEvent.getY() / m[Matrix.MSCALE_Y] - m[Matrix.MTRANS_Y];

        if (drawingText) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                TextPromptFragment.newInstance(x, y).show(getFragmentManager(), "TextPromptFragment");
            }
            return true;
        }

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startDrawing(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                draw(x, y);
                return true;
        }

        return false;
    }

    private void startDrawing(float x, float y) {
        startX = x;
        startY = y;

        saveBitmapToUndoStack();
    }

    private void draw(float x, float y) {
        Canvas canvas = getCanvas();

        if (canvas != null) {
            canvas.drawLine(startX, startY, x, y, paint);
            refreshCanvasView();
        }

        startX = x;
        startY = y;
    }

    private void drawText(String text, float x, float y) {
        setDrawingText(false);

        saveBitmapToUndoStack();

        Canvas canvas = getCanvas();

        if (canvas != null) {
            canvas.drawText(text, x, y, paint);
            refreshCanvasView();
        }
    }

    private void refreshCanvasView() {
        imageView.invalidate();
    }

    private void saveBitmapToUndoStack() {
        if (getBitmap() != null) {
            Log.d(TAG, "Undo stack size is " + undoStack.size());
            while (undoStack.size() >= UNDO_LEVELS) {
                Bitmap bitmap = undoStack.pollFirst();
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
            undoStack.addLast(getBitmap().copy(Bitmap.Config.ARGB_8888, true));
        }
    }

    @Nullable
    private Bitmap getBitmap() {
        if (!imageLoaded) {
            return null;
        }

        try {
            return ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Canvas getCanvas() {
        Bitmap bitmap = getBitmap();

        if (canvas == null && bitmap != null) {
            canvas = new Canvas(bitmap);
        }

        return canvas;
    }

    public void setDrawingText(boolean drawingText) {
        this.drawingText = drawingText;
        textButton.setBackgroundColor(drawingText ? Color.LTGRAY : Color.TRANSPARENT);
    }

    public static class TextPromptFragment extends DialogFragment implements DialogInterface.OnClickListener, View.OnFocusChangeListener {
        private static final String ARG_X = "x";
        private static final String ARG_Y = "y";

        public static DialogFragment newInstance(float x, float y) {
            DialogFragment fragment = new TextPromptFragment();
            Bundle arguments = new Bundle();
            arguments.putFloat(TextPromptFragment.ARG_X, x);
            arguments.putFloat(TextPromptFragment.ARG_Y, y);
            fragment.setArguments(arguments);
            return fragment;
        }

        private EditText textField;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            textField = new EditText(getActivity());
            textField.setHint(R.string.activity_image_editor_text_prompt_hint);
            textField.setOnFocusChangeListener(this);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.activity_image_editor_text_prompt)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setPositiveButton(android.R.string.ok, this)
                    .setView(textField)
                    .create();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            textField = null;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == DialogInterface.BUTTON_POSITIVE) {
                float x = getArguments().getFloat(ARG_X);
                float y = getArguments().getFloat(ARG_Y);
                ((ImageEditorActivity) getActivity()).drawText(textField.getText().toString(), x, y);
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
    }

    public static class OptionsFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, ColorPickerView.OnColorChangeListener {
        private static final String ARG_BRUSH_SIZE = "brush_size";
        private static final String ARG_FONT_SIZE = "font_size";
        private static final String ARG_COLOR = "color";

        public static DialogFragment newInstance(int brushSize, int fontSize, int color) {
            Bundle arguments = new Bundle();
            arguments.putInt(OptionsFragment.ARG_BRUSH_SIZE, brushSize);
            arguments.putInt(OptionsFragment.ARG_FONT_SIZE, fontSize);
            arguments.putInt(OptionsFragment.ARG_COLOR, color);
            DialogFragment fragment = new OptionsFragment();
            fragment.setArguments(arguments);
            return fragment;
        }

        @InjectView(R.id.dialog_image_editor_options_brush_size)
        SeekBar brushSize;

        @InjectView(R.id.dialog_image_editor_options_font_size)
        SeekBar fontSize;

        @InjectView(R.id.dialog_image_editor_options_dismiss)
        Button dismissButton;

        @InjectView(R.id.dialog_image_editor_options_color)
        ColorPickerView colorView;

        @InjectView(R.id.dialog_image_editor_options_color_value)
        SeekBar colorValue;

        @InjectView(R.id.dialog_image_editor_options_color_preview)
        View colorPreview;

        public OptionsFragment() {
            setStyle(STYLE_NO_TITLE, 0);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_image_editor_options, container, true);
            ButterKnife.inject(this, view);

            brushSize.setMax(MAX_BRUSH_SIZE - MIN_BRUSH_SIZE);
            brushSize.setProgress(getArguments().getInt(ARG_BRUSH_SIZE, DEFAULT_BRUSH_SIZE) - MIN_BRUSH_SIZE);
            brushSize.setOnSeekBarChangeListener(this);

            fontSize.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
            fontSize.setProgress(getArguments().getInt(ARG_FONT_SIZE, DEFAULT_FONT_SIZE) - MIN_FONT_SIZE);
            fontSize.setOnSeekBarChangeListener(this);

            final int color = getArguments().getInt(ARG_COLOR, DEFAULT_COLOR);

            colorView.setColor(color);
            colorView.setOnColorChangeListener(this);

            colorValue.setProgress((int) (colorValue.getMax() * colorView.getValue()));
            colorValue.setOnSeekBarChangeListener(this);

            colorPreview.setBackgroundColor(color);

            dismissButton.setOnClickListener(this);

            return view;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            ButterKnife.reset(this);
        }

        @Override
        public void onClick(View v) {
            if (v == dismissButton) {
                dismiss();
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            ImageEditorActivity activity = (ImageEditorActivity) getActivity();

            if (seekBar == brushSize) {
                activity.setBrushSize(MIN_BRUSH_SIZE + progress);
            }
            else if (seekBar == fontSize) {
                activity.setFontSize(MIN_FONT_SIZE + progress);
            }
            else if (seekBar == colorValue) {
                colorView.setValue(1.0f * progress / seekBar.getMax());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onColorChange(int color) {
            ((ImageEditorActivity) getActivity()).setColor(color);
            colorPreview.setBackgroundColor(color);
        }
    }
}
