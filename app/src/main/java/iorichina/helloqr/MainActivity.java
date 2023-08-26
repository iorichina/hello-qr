package iorichina.helloqr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.FloatRange;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    protected EditText edtResutlt;
    protected Button btnScan;
    protected Button btnJump;
    protected Button btnCreate;
    protected EditText edtData;
    protected ImageView ivQr;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Log.d("MainActivity", "Cancelled scan");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                        Toast.makeText(MainActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("MainActivity", "Scanned");
                    Toast.makeText(MainActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                    edtResutlt.setText(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtResutlt = findViewById(R.id.edt_resutlt);
        btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(MainActivity.this);
        btnJump = findViewById(R.id.btn_jump);
        btnJump.setOnClickListener(MainActivity.this);
        btnCreate = findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(MainActivity.this);
        edtData = findViewById(R.id.edt_data);
        ivQr = findViewById(R.id.iv_qr);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_scan) {
            ScanOptions options = new ScanOptions();
            options.setCaptureActivity(VerticalOrientationCaptureActivity.class);
            options.setPrompt("扫描框");
            barcodeLauncher.launch(options);
        } else if (view.getId() == R.id.btn_jump) {
            Uri uri = Uri.parse(edtResutlt.getText().toString());
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(uri);
            startActivity(intent);
        } else if (view.getId() == R.id.btn_create) {
            String data = edtData.getText().toString().trim();
            if (TextUtils.isEmpty(data)) {
                Toast.makeText(this, "请输入文字", Toast.LENGTH_SHORT).show();
            } else {
                //生成二维码
                Bitmap qrCode = createQRCode(data, 800, null);
                ivQr.setImageBitmap(qrCode);
                if (null == qrCode) {
                    Toast.makeText(this, "生成失败", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    protected Bitmap createQRCode(String content, int qrSize, Bitmap logo) {
        //配置参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //容错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hints.put(EncodeHintType.MARGIN, 1); //default is 4
        // 图像数据转换，使用了矩阵转换
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
        } catch (WriterException e) {
            Log.w("createQRCode", e.getMessage());
            return null;
        }
        int[] pixels = new int[qrSize * qrSize];
        // 下面这里按照二维码的算法，逐个生成二维码的图片，
        // 两个for循环是图片横列扫描的结果
        for (int y = 0; y < qrSize; y++) {
            for (int x = 0; x < qrSize; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[y * qrSize + x] = Color.BLACK;
                } else {
                    pixels[y * qrSize + x] = Color.WHITE;
                }
            }
        }

        // 生成二维码图片的格式
        Bitmap bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, qrSize, 0, 0, qrSize, qrSize);

        if (logo != null) {
            bitmap = addLogo(bitmap, logo, 0);
        }

        return bitmap;
    }

    /**
     * 在二维码中间添加Logo图案
     *
     * @param src
     * @param logo
     * @param ratio logo所占比例 因为二维码的最大容错率为30%，所以建议ratio的范围小于0.3
     * @return
     */
    private static Bitmap addLogo(Bitmap src, Bitmap logo, @FloatRange(from = 0.0f, to = 1.0f) float ratio) {
        if (src == null) {
            return null;
        }

        if (logo == null) {
            return src;
        }

        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }

        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }

        //logo大小为二维码整体大小
        float scaleFactor = srcWidth * ratio / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            Log.w("addLogo", e.getMessage());
        }

        return bitmap;
    }

}