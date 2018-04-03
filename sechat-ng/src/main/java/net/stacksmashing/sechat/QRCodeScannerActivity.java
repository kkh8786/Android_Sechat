package net.stacksmashing.sechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Collections;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class QRCodeScannerActivity extends Activity implements ZBarScannerView.ResultHandler {
    public final static String EXTRA_CONTENT = "EXTRA_CONTENT";

    public static Intent intent(Context context) {
        return new Intent(context, QRCodeScannerActivity.class);
    }

    private ZBarScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZBarScannerView(this);
        setContentView(scannerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.setFormats(Collections.singletonList(BarcodeFormat.QRCODE));
        scannerView.startCamera();
    }

    @Override
    protected void onPause() {
        scannerView.stopCamera();
        super.onPause();
    }

    @Override
    public void handleResult(Result result) {
        String content = result.getContents();
        Log.d("Barcode", "Got result: " + content);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTENT, content);
        setResult(RESULT_OK, intent);
        finish();
    }
}
