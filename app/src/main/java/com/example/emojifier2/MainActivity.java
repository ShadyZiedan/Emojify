package com.example.emojifier2;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int RC_CAMERA = 100;
    private static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";
    private Uri selectedPhotoPathUri;
    private Bitmap resultBitmap;
    private String mFilePath;
    @BindView(R.id.iv_photo) ImageView mPhotoImageView;
    @BindView(R.id.btn_emojify) Button mEmojifyButton;
    @BindView(R.id.fab_save) FloatingActionButton mSaveFab;
    @BindView(R.id.fab_delete) FloatingActionButton mDeleteFab;
    @BindView(R.id.fab_share) FloatingActionButton mShareFab;
    @BindView(R.id.fab_clear) FloatingActionButton mClearFab;
    private String mNewImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Hiding Action Bar
        getSupportActionBar().hide();

        ButterKnife.bind(this);
        //Hide Buttons
//        mSaveFab.setVisibility(View.GONE);
//        mDeleteFab.setVisibility(View.GONE);
        clearActivity();
    }

    @OnClick(R.id.btn_emojify)
    void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File imagePath = new File(getFilesDir(),"images");

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        File newFile = new File(imagePath, "IMG_" + timestamp +".jpg");

        newFile.getParentFile().mkdirs();

        mFilePath = newFile.getAbsolutePath();
        selectedPhotoPathUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, newFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedPhotoPathUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        } else {
            ClipData clip = ClipData.newUri(getContentResolver(), "A photo", selectedPhotoPathUri);
            intent.setClipData(clip);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        startActivityForResult(intent, RC_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CAMERA && resultCode == RESULT_OK){
            processAndSetImage();
        }
    }


    private void processAndSetImage() {
        mEmojifyButton.setVisibility(View.GONE);
        //Show Buttons
        mClearFab.setVisibility(View.VISIBLE);
        mSaveFab.setVisibility(View.VISIBLE);
        mDeleteFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);



        resultBitmap = Emojifier.resamplePic(this, mFilePath);
        resultBitmap = Emojifier.detectFacesAndOverlayEmoji(this, resultBitmap);

//        resultBitmap = BitmapFactory.decodeFile(mFilePath);

        mPhotoImageView.setImageBitmap(resultBitmap);

    }

    @OnClick({R.id.fab_save, R.id.fab_delete, R.id.fab_clear, R.id.fab_share})
    void onClick(View view){
        switch (view.getId()){
            case R.id.fab_save:
                saveToGallery(resultBitmap);
                break;
            case R.id.fab_delete:
                deletePhoto(this,selectedPhotoPathUri);
                break;
            case R.id.fab_clear:
                clearActivity();
                break;
            case R.id.fab_share:

                mNewImagePath = saveToGallery(resultBitmap);
                shareImage(this, mNewImagePath);
                break;

        }
    }

    private String saveToGallery(Bitmap picture){
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), picture + ".jpg");
        //test
        String savedImageFile = imageFile.getAbsolutePath();
        Toast.makeText(this, mNewImagePath, Toast.LENGTH_SHORT).show();
        /**********/
        try{
            FileOutputStream fos = new FileOutputStream(imageFile);
            picture.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();

        } catch (IOException ex){
            Toast.makeText(this, "Couldn't save image.", Toast.LENGTH_SHORT).show();
        }

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(imageFile));
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "Image saved to device.", Toast.LENGTH_SHORT).show();

        return savedImageFile;

    }

    private void deletePhoto(Context context, Uri photoUri){

        int i = context.getContentResolver().delete(photoUri,null, null);
        if (i != 0){
            Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
            mPhotoImageView.setImageBitmap(null);
        }
    }

    private void clearActivity(){

        if (selectedPhotoPathUri != null){
            deletePhoto(this,selectedPhotoPathUri);

        }

        mEmojifyButton.setVisibility(View.VISIBLE);
        mPhotoImageView.setImageBitmap(null);

        mSaveFab.setVisibility(View.GONE);
        mDeleteFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);
        mShareFab.setVisibility(View.GONE);

    }

    private void shareImage(Context context, String imagePath){
        File imageFile = new File(imagePath);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType("image/*");
        Uri photoURI = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, imageFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        context.startActivity(shareIntent);


    }
}
