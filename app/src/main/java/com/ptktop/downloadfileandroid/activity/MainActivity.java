package com.ptktop.downloadfileandroid.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.NotificationCompat;
import androidx.core.widget.ContentLoadingProgressBar;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.ptktop.downloadfileandroid.R;
import com.ptktop.downloadfileandroid.manager.DownloadFileAll;
import com.ptktop.downloadfileandroid.manager.PermissionValidate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/*********************
 * Created by PTKTOP *
 *********************/

public class MainActivity extends AppCompatActivity implements
        PermissionValidate.EventListener
        , DownloadFileAll.DownloadListener {

    private AppCompatTextView tvPercent;
    private ContentLoadingProgressBar progressDownload;
    private AppCompatEditText edtUrl;

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private PermissionListener permissionStorage;

    private int REQUEST_SETTING = 1000;
    private String folderDownload = "/DownloadFileAndroid/Downloads/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        createPermissionWriteStorage();
    }

    private void initView() {
        tvPercent = findViewById(R.id.tvPercent);
        progressDownload = findViewById(R.id.progressDownload);
        edtUrl = findViewById(R.id.edtUrl);
        AppCompatButton btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> checkPermissionStorage());
    }

    /****************** Permission ******************/
    private void createPermissionWriteStorage() {
        PermissionListener permissionListener = new PermissionValidate(this);
        permissionStorage = new CompositePermissionListener(permissionListener);
    }

    private void checkPermissionStorage() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(permissionStorage)
                .check();
    }

    @Override
    public void showPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (edtUrl.getText() != null) {
                String text = edtUrl.getText().toString().trim();
                if (!text.equals("")) {
                    // you can change type file
                    startDownloadFile(text, ".txt");
                } else {
                    String url = "https://unsplash.com/photos/IErnUm1Y8PY/download?force=true";
                    startDownloadFile(url, ".jpg");
                }

            }
        }
    }

    @Override
    public void showPermissionDenied(String permission, boolean isPermanentlyDenied) {
        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (isPermanentlyDenied) {
                // go to setting
                showDialogPermission();
            } else {
                // not check box ask me later
                showDialogPermission();
            }
        }
    }

    @Override
    public void showPermissionRationale(PermissionToken token) {
        token.continuePermissionRequest();
    }

    private void showDialogPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                .setCancelable(false)
                .setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
                .setTitle("Write storage permission request")
                .setMessage("File will not be available until you accept the permission request.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivityForResult(intent, REQUEST_SETTING);
                });
        AppCompatDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTING) {
            if (resultCode == Activity.RESULT_CANCELED) {
                checkPermissionStorage();
            }
        }
    }

    /****************** Download ******************/
    @SuppressLint("SimpleDateFormat")
    private void startDownloadFile(String url, String typeFile) {
        checkFolderInsideStorage();
        showNotification();
        new DownloadFileAll(
                this,
                url,
                folderDownload,
                "test_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + typeFile)
                .run();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkFolderInsideStorage() {
        String folderApp = "/DownloadFileAndroid/";
        File pathApp = new File(Environment.getExternalStorageDirectory() + folderApp);
        File pathDownload = new File(Environment.getExternalStorageDirectory() + folderDownload);
        if (!pathApp.exists()) { // not found folder app
            pathApp.mkdir();
            if (!pathDownload.exists()) { // not found folder download
                pathDownload.mkdir();
            }
        } else { // have folder main
            if (!pathDownload.exists()) { // have folder app , but not found folder download
                pathDownload.mkdir();
            }
        }
    }

    private void showNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading....")
                .setContentText("Please wait a few minute.")
                .setAutoCancel(true);
        notificationManager.notify(0, notificationBuilder.build());
    }

    long startTime = System.currentTimeMillis();
    int timeCount = 1;

    @SuppressLint("SetTextI18n")
    @Override
    public void downloadProcess(long bytesRead, long contentLength, boolean done) {
        runOnUiThread(() -> {
            int totalFileSize = (int) Math.round(contentLength / (Math.pow(1024, 2)));
            int current = (int) Math.round(bytesRead / (Math.pow(1024, 2)));
            int progress = (int) ((bytesRead * 100) / contentLength);
            long currentTime = System.currentTimeMillis() - startTime;

            tvPercent.setText(progress + "%");
            progressDownload.setProgress(progress);
            if (currentTime > 1000 * timeCount) {
                setProgressNotification(progress, current, totalFileSize);
                timeCount++;
            }
            if (done) onDownloadComplete();
        });
    }

    @Override
    public void storageFileComplete(boolean complete, String message) {
        if (complete) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    .setCancelable(false)
                    .setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
                    .setTitle("Alert")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            AppCompatDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void setProgressNotification(int progress, int currentFileSize, int totalFileSize) {
        notificationBuilder.setProgress(100, progress, false);
        notificationBuilder.setContentText("Downloading file " + currentFileSize + "/" + totalFileSize + " MB");
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void onDownloadComplete() {
        notificationManager.cancel(0);
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("Success");
        notificationManager.notify(0, notificationBuilder.build());
    }
}
