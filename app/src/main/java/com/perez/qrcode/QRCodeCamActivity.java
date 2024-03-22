package com.perez.qrcode;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Size;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.perez.revkiller.R;

import java.io.IOException;

public class QRCodeCamActivity extends AppCompatActivity {

    private PreviewView previewView;
    private MediaPlayer mediaPlayer;
    private static final float BEEP_VOLUME = 0.50f;
    private static final long VIBRATE_DURATION = 200L;
    private boolean isDialogShowing = false;

    private void initBeepSound() {
        if (shouldPlayBeep() && mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(mp -> mp.seekTo(0));
            loadBeepSound();
        }
    }

    private void loadBeepSound() {
        try (AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep)) {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
        } catch (IOException ignored) {
            mediaPlayer = null;
        }
    }

    private boolean shouldPlayBeep() {
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        return audioService.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    private void playBeepSoundAndVibrate() {
        if (shouldPlayBeep() && mediaPlayer != null) {
            mediaPlayer.start();
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        previewView = findViewById(R.id.preview_qrcode);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please enable camera permissions before using the module.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBeepSound();
    }

    private void showResult(String result) {
        if (!isDialogShowing) {
            isDialogShowing = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(QRCodeCamActivity.this);
            builder.setTitle(R.string.scan_result_title);
            builder.setMessage(result);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> isDialogShowing = false); // Reset flag when dialog box is closed
            builder.setOnCancelListener(dialog -> isDialogShowing = false); // Ensure that the flag is also reset when the user cancels
            builder.setNeutralButton(android.R.string.copy, (dlg, which) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QrcodeResult", result);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(QRCodeCamActivity.this, R.string.copied_tips, Toast.LENGTH_SHORT).show();
            });
            builder.show();
        }
    }

    private void startCamera() {
        previewView.post(() -> {
            try {
                previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(previewView.getWidth(), previewView.getHeight()))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(QRCodeCamActivity.this),
                        new QrCodeAnalyzer(result -> {
                            if (!isDialogShowing) { // Also check in the parser if the dialog is being displayed
                                playBeepSoundAndVibrate();
                                showResult(result);
                            }
                        }));
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                cameraProvider.bindToLifecycle(QRCodeCamActivity.this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to capture QRCode because: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}
