package com.wcedla.downloaddemo;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    public static boolean isStart = false;//用于判读下载服务是否启动,设置数值在downloadservice的startDownload方法下。
    public static boolean isPause = false;//用于判断下载服务是否暂停，设置数值在downloadservice的pauseDownload方法下。
    public static boolean isCancel = false;//用于判断下载服务是否停止，设置数值在downloadservice的cancelDownload方法下。
    private static final String TAG = "MainActivity";
    private downloadService.DownloadBinder downloadBinder;
    public String downloadURL;
    private static String fileName;
    public myServiceConnection connection = new myServiceConnection();//实例化重构的服务连接方法
    Button startDownload;
    Button pauseDownload;
    Button stopDownload;
    EditText urlText;
    EditText filenameText;
    EditText filePath;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startDownload = (Button) findViewById(R.id.start_download);
        pauseDownload = (Button) findViewById(R.id.pause_download);
        stopDownload = (Button) findViewById(R.id.stop_download);
        urlText = (EditText) findViewById(R.id.url_text);
        filenameText=(EditText)findViewById(R.id.filename_text);
        filePath=(EditText)findViewById(R.id.path_text);
        filePath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        stopDownload.setOnClickListener(this);
        urlText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(urlText.getText().toString().contains("/"))
                {
                    if(!urlText.getText().toString().contains("?"))
                        filenameText.setText(urlText.getText().toString().substring(urlText.getText().toString().lastIndexOf("/")+1));
                    else
                        filenameText.setText(urlText.getText().toString().substring(urlText.getText().toString().lastIndexOf("/")+1,urlText.getText().toString().indexOf("?")));

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {


            }
        });
        /*
        绑定服务代码，以及申请权限代码，单纯在androidmanifest.xml文件声明权限，不使用运行时权限那么详单与没有权限。
        * */
        Intent downloadServiceStart = new Intent(MainActivity.this, downloadService.class);
        bindService(downloadServiceStart, connection, BIND_AUTO_CREATE);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    /*
    活动和服务相连接需要使用Serviceconnect来实现，然后可以通过重写它的连接服务获取到BInder实例
    然后可以操作服务中binder的方法。
    * */
    class myServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (downloadService.DownloadBinder) iBinder;
            Log.d(TAG, "ManActity获取Binder" + downloadBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start_download: {
                downloadURL=urlText.getText().toString();

                if (downloadBinder != null) {
                    if (!downloadURL.equals("")) {
                        if(downloadURL.indexOf("http://")==0||downloadURL.indexOf("https://")==0||downloadURL.indexOf("HTTP://")==0||downloadURL.indexOf("HTTPS://")==0) {
                            fileName=filenameText.getText().toString();
                            if (startDownload.getText().equals("开始下载"))//判断按钮文本主要是因为有暂停按钮，暂停之后，改变按钮文本为开始下载。
                            {
                                if (isStart)//判断下载服务是否已启动，启动的话就不能再添加下载任务，而应该提示用户下载服务已建立。
                                {
                                    Toast.makeText(MainActivity.this, "下载服务已启动!", Toast.LENGTH_SHORT).show();
                                } else {
                                    downloadBinder.setFileName(fileName);
                                    downloadBinder.startDownload(downloadURL);//通过serviceConnection获取到的binder可以代用服务中的方法，从而管理控制服务。

                                }

                            } else if (startDownload.getText().equals("继续下载"))//判断按钮文本主要是因为有暂停按钮，暂停之后，改变按钮文本为开始下载。
                            {
                                startDownload.setText("开始下载");//继续下载任务执行后，还原按钮文本。
                                downloadBinder.startDownload(downloadURL);//继续啊下载文件，其实就是开始重新下载文件。
                            }
                        } else {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("警告");
                            dialog.setMessage("输入网址格式不正确，请重新输入正确的网址！\n例如:http://downloag.lg.com/lg.apk");
                            dialog.setCancelable(true);
                            dialog.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            dialog.show();
                        }
                    } else {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("警告");
                        dialog.setMessage("输入网址为空，请重新输入正确的网址！");
                        dialog.setCancelable(true);
                        dialog.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        dialog.show();
                    }
                }
            }
            break;
            case R.id.pause_download: {
                if (downloadBinder != null && isStart) {//暂停时首先应该保证服务已经再运行了，否则提示没有下载任务。
                    downloadBinder.pauseDownload();//服务中binder的暂停下载方法，实则在下载文件时，直接不让写入文件，提前return
                    startDownload.setText("继续下载");
                } else
                    Toast.makeText(MainActivity.this, "没有下载任务", Toast.LENGTH_SHORT).show();
            }
            break;
            case R.id.stop_download: {
                if (downloadBinder != null && (isStart || isPause)) {//判断下载服务是否运行，没有运行的话查看是不是处于暂停状态。
                    //unbindService(connection);不能再这边解绑服务，绑定服务是在活动创建的时候绑定的，点击按钮解绑之后，再次点击的时候服务并没有绑定，会报错。
                    downloadBinder.cancelDownload();//服务中binder的取消方法，实则在下载文件时，不执行写入文件，提前return，并把下载的文件删除。
                    startDownload.setText("开始下载");
                } else {
                    //String fileName = downloadURL.substring(downloadURL.lastIndexOf("/"), downloadURL.indexOf("?"));//通过下载链接获得文件名
                    //文件路径，固定格式，路径存放为手机内存的download文件夹
                    String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "wcedla"+File.separator;
                    File file = new File(directory + fileName);//操作指定路径下的文件，绑定到file。
                    if (file.exists())
                        if(file.delete())//删除文件
                            Toast.makeText(MainActivity.this, "下载任务已取消,文件已删除", Toast.LENGTH_SHORT).show();

                }
            }
            break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {//运行时权限
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }


}