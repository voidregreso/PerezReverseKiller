package com.perez.imageviewer;

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.perez.revkiller.R;
import com.perez.revkiller.exifremover.Interfaz;

import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageButton;

import org.apache.sanselan.ImageReadException;

public class HugeImageViewerActivity extends AppCompatActivity {

    private TileDrawable mTileDrawable;
    private ImageButton ibtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_huge);
        final String str = this.getIntent().getStringExtra("IMAGEPATH");
        final PinchImageView pinchImageView = (PinchImageView) findViewById(R.id.pic);
        ibtn = (ImageButton)findViewById(R.id.verEXIF);
        ibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alg = null;
                try {
                    alg = new AlertDialog.Builder(HugeImageViewerActivity.this)
                            .setTitle("EXIF Information")
                            .setMessage(Interfaz.showExif(str))
                            .setPositiveButton("OK", null).create();
                    alg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    alg.show();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ImageReadException e) {
                    e.printStackTrace();
                }
            }
        });
        pinchImageView.post(new Runnable() {
            @Override
            public void run() {
                mTileDrawable = new TileDrawable();
                mTileDrawable.setInitCallback(new TileDrawable.InitCallback() {
                    @Override
                    public void onInit() {
                        pinchImageView.setImageDrawable(mTileDrawable);
                    }
                });
                mTileDrawable.init(new HugeImageRegionLoader(HugeImageViewerActivity.this, Uri.fromFile(new File(str))), new Point(pinchImageView.getWidth(), pinchImageView.getHeight()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(mTileDrawable != null)
            mTileDrawable.recycle();
        super.onDestroy();
    }
}