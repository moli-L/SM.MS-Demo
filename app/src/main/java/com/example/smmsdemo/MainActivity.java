package com.example.smmsdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.smmsdemo.service.FileUploadObserver;
import com.example.smmsdemo.service.HttpRequest;
import com.example.smmsdemo.service.ProgressRequestBody;
import com.nanchen.compresshelper.CompressHelper;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.button_choose)
    Button chooseImg;
    @BindView(R.id.button_upload)
    Button upload;
    @BindView(R.id.size)
    TextView size;
    @BindView(R.id.image_origin)
    ImageView imageOrigin;
    @BindView(R.id.image_upload)
    ImageView imageUpload;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.button_delete)
    Button delete;
    @BindView(R.id.button_clear)
    Button clear;

    private static final int CHOOSE_PHOTO = 2;
    private File file = null;
    private ProgressDialog progressDialog;
    private JsonBean jsonBean = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        progressDialog = new ProgressDialog(this);

        //选择图片
        chooseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission();
            }
        });

        //上传图片
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file != null) {
                    upload(file);
                }
            }
        });

        //清空历史上传
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        //删除刚刚上传的图片
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deleteUrl = jsonBean.data.delete;
                if (!deleteUrl.isEmpty()) {
                    Uri uri = Uri.parse(deleteUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            }
        });
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum();
            } else {
                //if refused
                requestPermission();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_PHOTO) {
            if (resultCode == RESULT_OK) {
                File oldFile, newFile;
                if (Build.VERSION.SDK_INT >= 19) {
                    //4.4及以上系统
                    oldFile = handleImage(data);
                } else {
                    //4.4及以下系统
                    oldFile = handleImageBeforeKitKat(data);
                }

                if (oldFile.length() > 4 * 1024 * 1024) {
                    //大于4M压缩
                    file = newFile = new CompressHelper.Builder(this)
                            .setQuality(100)
                            .build()
                            .compressToFile(oldFile);
                    size.setText("size: " + (newFile.length() / 1024) + "KB");
                    Glide.with(this).load(newFile).into(imageOrigin);
                } else {
                    size.setText("size: " + (oldFile.length() / 1024) + "KB");
                    file = oldFile;
                    Glide.with(this).load(oldFile).into(imageOrigin);
                }
            }
        }
    }

    @TargetApi(19)
    private File handleImage(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //document类型
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; //解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse(
                        "content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //content类型
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //file类型
            imagePath = uri.getPath();
        }

        return new File(imagePath);
    }

    private File handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        return new File(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过uri和selection来获取真实图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    //上传至图床
    private void upload(File file) {
        FileUploadObserver<JsonBean> fileUploadObserver = new FileUploadObserver<JsonBean>() {
            @Override
            public void onSuccess(JsonBean bean) {
                hideProgressDialog();
                jsonBean = bean;
                if (jsonBean.code.equals("success")) {
                    showToast("上传成功");
                    text.setVisibility(View.VISIBLE);
                    text.setText("图片链接：" + jsonBean.data.url);
                    delete.setVisibility(View.VISIBLE);
                    //根据返回的外链加载该图
                    Glide.with(getBaseContext()).load(jsonBean.data.url).into(imageUpload);
                } else {
                    //上传失败
                    showToast("上传失败: " + jsonBean.msg);
                }
            }

            @Override
            public void onFail(Throwable e) {
                hideProgressDialog();
                showToast("上传失败");
                Log.e("MainActivity", e.toString());
            }

            @Override
            public void onProgress(int progress) {
                Log.d("progress:", progress + "");
                progressDialog.setProgress(progress);
            }

            @Override
            public void onSubscribe(Disposable d) {
                showProgressDialog();
            }
        };

        HttpRequest.getApi()
                .upload(fileToMultipartBodyPart(file, fileUploadObserver))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileUploadObserver);
    }

    //file转换为MultipartBodyPart
    private MultipartBody.Part fileToMultipartBodyPart(File file, FileUploadObserver<JsonBean> fileUploadObserver) {
        ProgressRequestBody requestBody = new ProgressRequestBody(file, fileUploadObserver);
        return MultipartBody.Part.createFormData("smfile", file.getName(), requestBody);
    }

    //显示进度
    private void showProgressDialog() {
        progressDialog.setMessage("正在上传...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        progressDialog.dismiss();
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void clear(){
        HttpRequest.getApi()
                .clear()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<JsonBean>() {
                    @Override
                    public void accept(JsonBean jsonBean) throws Exception {
                        if(jsonBean.code.equals("success")){
                            showToast("删除成功");
                        } else {
                            showToast("删除失败: "+jsonBean.msg);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        showToast("删除失败");
                        Log.e("MainActivity", throwable.toString());
                    }
                });
    }
}
