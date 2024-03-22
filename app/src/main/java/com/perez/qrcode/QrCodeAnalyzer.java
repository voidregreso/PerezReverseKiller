package com.perez.qrcode;

import android.graphics.ImageFormat;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.PlanarYUVLuminanceSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnQrCodeScannedCallback {
        void onScanned(String qrCode);
    }

    private OnQrCodeScannedCallback onQrCodeScanned;
    private List<Integer> supportedImageFormats = Arrays.asList(
            ImageFormat.YUV_420_888,
            ImageFormat.YUV_422_888,
            ImageFormat.YUV_444_888
    );

    public QrCodeAnalyzer(OnQrCodeScannedCallback onQrCodeScanned) {
        this.onQrCodeScanned = onQrCodeScanned;
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        buffer.rewind(); // Rewind the buffer to zero
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data); // Copy the buffer into a byte array
        return data;
    }

    @Override
    public void analyze(ImageProxy image) {
        // Check if the format of the scanning image proxy is among the supported formats
        if (supportedImageFormats.contains(image.getFormat())) {
            // Get raw data
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = toByteArray(buffer);

            // Create ZXing brightness source for decoding
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    bytes, image.getWidth(), image.getHeight(),
                    0, 0, image.getWidth(), image.getHeight(), false
            );

            // Create binary bitmap
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                MultiFormatReader reader = new MultiFormatReader();
                Map<DecodeHintType, Object> hints = new HashMap<>();
                hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(BarcodeFormat.QR_CODE));
                reader.setHints(hints);
                Result result = reader.decode(binaryBitmap);

                // Callback the result
                onQrCodeScanned.onScanned(result.getText());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                image.close();
            }
        }
    }
}
