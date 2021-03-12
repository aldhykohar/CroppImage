package com.stimednp.croppimage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.Toast;

import com.stimednp.croppimage.databinding.ActivityMainBinding;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Uri imgUri;
    private String imgBase64;
    private String encodedImage;

    private static final int PERMISSION_FILE = 101;
    private static final int ACCESS_FILE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        doInitialization();
    }

    private void doInitialization() {
        binding.btSelectImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_FILE);
            } else {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), ACCESS_FILE);
            }
        });

        binding.btCompress.setOnClickListener(v -> {
            if (imgUri == null) {
                Toast.makeText(this, "Upload Image", Toast.LENGTH_SHORT).show();
            } else {
//                resultFromMedia(imgUri);
                convertToBase64();
//                Log.e("TAG", "doInitialization: " + imgBase64);
            }
        });

        binding.btDecode.setOnClickListener(v -> {
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            binding.ivDecode.setImageBitmap(decodedByte);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACCESS_FILE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri FILE_URI = data.getData();
            Log.e("TAG", "onActivityResult: " + data.getExtras());
            CropImage.activity(FILE_URI)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.RECTANGLE)
                    .setActivityTitle("Crop Image")
                    .setFixAspectRatio(false)
                    .setCropMenuCropButtonTitle("Done")
                    .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imgUri = result.getUri();
                binding.ivUser.setImageURI(imgUri);
                if (data != null) {
//                    Bitmap transferEvidence = (Bitmap) data.getExtras().get("data");
                    Log.e("TAG", "onActivityResult: " + data.getExtras().get("CROP_IMAGE_EXTRA_BUNDLE"));
                    Log.e("TAG", "onActivityResult: " + result);
//                    Log.e("TAG", "onActivityResult: " + transferEvidence);
//                    Log.e("TAG", "onActivityResult: " + convertToBase64(transferEvidence));

                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(imgUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    encodedImage = encodeImage(selectedImage);
                    if (encodedImage != null) {
                        binding.tvBase64.setText(encodedImage);
                    }
                    Log.e("TAG", "onActivityResult: " + encodedImage);


                }
//                imgBase64 = resultFromMedia(imgUri);
//                imgBase64 = resizeAndCompressImageBeforeSend(imgUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
    }

    private String convertToBase64() {
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(imgUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        final int MAX_IMAGE_SIZE = 700 * 1024; // max final file size in kilobytes
        Bitmap bmpPic = BitmapFactory.decodeStream(imageStream);

        int compressQuality = 100; // quality decreasing by 5 every loop.
        int streamLength;
        byte[] bmpPicByteArray;
        do {
            ByteArrayOutputStream bmpStream = new ByteArrayOutputStream();
            Log.d("compressBitmap", "Quality: " + compressQuality);
            Log.d("compressBitmap", "Quality: " + bmpStream);
            bmpPic.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream);
            bmpPicByteArray = bmpStream.toByteArray();
            streamLength = bmpPicByteArray.length;
            compressQuality -= 5;
            Log.d("compressBitmap", "Size: " + streamLength / 1024 + " kb");
        } while (streamLength >= MAX_IMAGE_SIZE);

        return Base64.encodeToString(bmpPicByteArray, Base64.DEFAULT);
    }

    private String resultFromMedia(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        int columnIndex = 0;
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        }
        String picturePath = null;
        if (cursor != null) {
            picturePath = cursor.getString(columnIndex);
        }
        if (cursor != null) {
            cursor.close();
        }

        Log.e("TAG", "resultFromMedia: " + picturePath);
        Bitmap transferEvidence = BitmapFactory.decodeFile(picturePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transferEvidence.compress(Bitmap.CompressFormat.PNG, 60, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public String resizeAndCompressImageBeforeSend(Uri selectedImage) {

        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        int columnIndex = 0;
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        }
        String picturePath = null;
        if (cursor != null) {
            picturePath = cursor.getString(columnIndex);
        }
        if (cursor != null) {
            cursor.close();
        }

        final int MAX_IMAGE_SIZE = 700 * 1024; // max final file size in kilobytes

        // First decode with inJustDecodeBounds=true to check dimensions of image
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, options);

        // Calculate inSampleSize(First we are going to resize the image to 800x800 image, in order to not have a big but very low quality image.
        //resizing the image will already reduce the file size, but after resizing we will check the file size and start to compress image
        options.inSampleSize = calculateInSampleSize(options, 300, 300);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmpPic = BitmapFactory.decodeFile(picturePath, options);

        int compressQuality = 100; // quality decreasing by 5 every loop.
        int streamLength;
        byte[] bmpPicByteArray;
        do {
            ByteArrayOutputStream bmpStream = new ByteArrayOutputStream();
            Log.d("compressBitmap", "Quality: " + compressQuality);
            Log.d("compressBitmap", "Quality: " + bmpStream);
            bmpPic.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream);
            bmpPicByteArray = bmpStream.toByteArray();
            streamLength = bmpPicByteArray.length;
            compressQuality -= 5;
            Log.d("compressBitmap", "Size: " + streamLength / 1024 + " kb");
        } while (streamLength >= MAX_IMAGE_SIZE);

        return Base64.encodeToString(bmpPicByteArray, Base64.DEFAULT);
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        String debugTag = "MemoryInformation";
        // Image nin islenmeden onceki genislik ve yuksekligi
        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.d(debugTag, "image height: " + height + "---image width: " + width);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        Log.d(debugTag, "inSampleSize: " + inSampleSize);
        return inSampleSize;
    }
}