package com.stimednp.croppimage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.stimednp.croppimage.databinding.ActivityDecodeImageBinding;

public class DecodeImageActivity extends AppCompatActivity {
    private ActivityDecodeImageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_decode_image);
        doInitialization();
    }

    private void doInitialization() {
        Bundle bundle = getIntent().getBundleExtra("base64");
        String encodedImage = bundle.getString("bundle");

        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        binding.ivUser.setImageBitmap(decodedByte);

    }
}