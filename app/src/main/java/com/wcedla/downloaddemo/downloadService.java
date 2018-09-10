package com.wcedla.downloaddemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.wcedla.downloaddemo.MainActivity.isPause;

public class downloadService extends Service {

    private static final String TAG = "downloadService";
    private DownloadTask downloadTask=null;
    public String downloadURL;
    public String fileName;


    /*
    实现下载监听的各种方法，下载监听主要是为了Asynctask子线程与downloadservice之间的通信，为什么要用接口实现，不直接新建类的原因是
    新建类难获取到context对象，很多操作无法执行，像操作Toast，发送通知等。
    * */

    class myDownloadListener implements downloadListener
    {

        @Override
        public void onProgress(int progress)
        {
            getNotificationManager().notify(1, getNotification("正在下载...", progress));
        }

        @Override
        public void onSuccess() {
            stopForeground(true);//取消正在下载的通知前台服务
            MainActivity.isStart=false;
            getNotificationManager().notify(0,getNotification("文件下载完毕",-1));

        }

        @Override
        public void onFailed() {
            stopForeground(true);
            MainActivity.isStart=false;
            MainActivity.isPause=false;
            MainActivity.isCancel=false;
            getNotificationManager().notify(4,getNotification("下载失败",-1));

        }

        @Override
        public void onPaused() {
            stopForeground(true);
            getNotificationManager().notify(2,getNotification("下载已被暂停",-1));
        }

        @Override
        public void onCanceled() {
            stopForeground(true);
            getNotificationManager().notify(3,getNotification("下载已被取消",-1));

        }

        @Override
        public String getFileName() {
            return fileName;
        }
    }

    DownloadBinder downloadBinder = new DownloadBinder();
    myDownloadListener downloadListener=new myDownloadListener();

    class DownloadBinder extends Binder //继承自binder，然后可以自己定义各种自己需要的方法，具体方法看自己需要了。
    {

        public void startDownload(String str) {
            downloadURL=str;
            downloadTask=new DownloadTask(downloadListener);//通过downloadtask的有参构造函数，将下载监听器传入到asynctask中，达到两者之间的通信。
            downloadTask.execute(str);
            startForeground(1,getNotification("开始下载...",0));
            getNotificationManager().cancel(0);//清除之前执行任务产生的通知，
            getNotificationManager().cancel(2);
            getNotificationManager().cancel(3);
            getNotificationManager().cancel(4);
            MainActivity.isStart=true;
            Toast.makeText(downloadService.this,"下载任务开始！",Toast.LENGTH_SHORT).show();
        }

        public void pauseDownload()
        {
            if(downloadTask!=null)//用于判断下载服务是否启动
            {
                downloadTask.pauseDownload();//asynctask的方法，实则就是在将下载文件写入文件之前使用return返回，强制结束任务
                MainActivity.isStart=false;
                MainActivity.isPause=true;
                Toast.makeText(downloadService.this,"下载任务已暂停！",Toast.LENGTH_SHORT).show();

            }
            else
            {
                Toast.makeText(downloadService.this,"没有正在下载的文件",Toast.LENGTH_SHORT).show();
            }
        }

        public void cancelDownload()
        {
            if(downloadTask!=null)//用于判断下载服务是否启动
            {
                downloadTask.cancelDownload();//asynctask的方法，实则就是在将下载文件写入文件之前使用return返回，强制结束任务
                MainActivity.isStart=false;
                MainActivity.isPause=false;
                MainActivity.isCancel=true;
                downloadListener.onCanceled();//下载服务已经暂停，子线程并没有继续执行下载任务，不能再通过return cancel达到创建通知的效果，所以手动调用生成通知。
                getNotificationManager().cancel(2);
                getNotificationManager().cancel(0);
                /*
                将下载的文件删除
                * */
                //String fileName = downloadURL.substring(downloadURL.lastIndexOf("/"),downloadURL.indexOf("?"));
                //文件路径，固定格式，路径存放为手机内存的download文件夹
                String directory = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"wcedla"+File.separator;
                File file = new File(directory +fileName);//操作指定路径下的文件，绑定到file。
                if (file.exists()) {
                    if(file.delete())
                    {
                        Toast.makeText(downloadService.this, "下载任务已取消,文件已删除", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else
            {
                Toast.makeText(downloadService.this,"没有正在下载的文件",Toast.LENGTH_SHORT).show();
            }
        }

        public void setFileName(String name)
        {
            fileName=name;
            Log.d(TAG, "成功"+fileName);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {//使用重构的binder达到和活动通信的目的。
        return downloadBinder;
    }

    private NotificationManager getNotificationManager() {//太多地方需要创建通知了，新建一个方法方便调用使用
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }


    private Notification getNotification(String title, int progress) {//首先设置常用的通知元素，然后在根据第二个参数来控制是否显示进度条，非常巧妙！
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"1");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress >= 0) {
            // 当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }


}
