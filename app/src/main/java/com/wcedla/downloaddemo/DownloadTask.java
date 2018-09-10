package com.wcedla.downloaddemo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer>
{
    private final static int TYPE_SUCCESS=0;
    private final static int TYPE_FAILED=1;
    private final static int TYPE_PAUSED=2;
    private final static int TYPE_CANCELED=3;

    private downloadService.myDownloadListener listener;//asynctask子线程与downloadservice之间联系用的监听器

    private static final String TAG = "DownloadTask";
    private InputStream inputStream=null;
    private RandomAccessFile tempfile=null;//读取文件任意位置（不能从中间直接插入，如果要直接中间插入必须先把后面的数据读取到缓冲区，写完后再重写写回）
    File file;
    private int lastProgress=0;//为了控制状态栏的下载进度更新速度，太快了通知栏会卡住。
    private boolean isPause=false;
    private boolean isCancel=false;

    //通过构造方法接收service和asynctask之间通讯用的监听器的实例。
    public DownloadTask(downloadService.myDownloadListener listener)
    {
        this.listener=listener;

    }


    @Override
    protected Integer doInBackground(String... strings) {

        int len;
        long downloadedLength = 0;

        try {
                //文件名从链接中获取，链接的最后一个”/“开始截取直到第一个“？”之间的字符串作为文件名
                String fileName=listener.getFileName();
                //文件路径，固定格式，路径存放为手机内存的download文件夹
                String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "wcedla"+File.separator;
                File createFolder = new File(directory);//在手机内存的根目录或者指定目录下新建文件夹，如果不新建直接操作路径，会抛异常无法读取文件，所以要先创建文件夹再操作路径。
                createFolder.mkdirs();
                file = new File(directory + fileName);//操作指定路径下的文件，绑定到file。
                if (file.exists()) {
                    Log.d(TAG, "文件已存在,并且大小为" + file.length());
                    downloadedLength = file.length();
                }
                long size = getContentLength(strings[0]);
                Log.d(TAG, "网络文件大小" + size);
                if (size == 0) {
                    return TYPE_FAILED;
                } else if (size == downloadedLength) {
                    Log.d(TAG, "文件已下载完毕");
                    // 已下载字节和文件总字节相等，说明已经下载完成了
                    return TYPE_SUCCESS;
                }

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        // 断点下载，指定从哪个字节开始下载
                        .addHeader("RANGE", "bytes=" + downloadedLength + "-")//最后的-表示从当前位置到文件结尾
                        .url(strings[0])
                        .build();
                Response response = client.newCall(request).execute();
                inputStream = response.body().byteStream();//获取网络文件字节流。
                //inputStream=new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"56ui/"+"1.apk");
                tempfile = new RandomAccessFile(file, "rw");//将要操作的文件转换成可在任意位置开始写入的文件。
                tempfile.seek(downloadedLength);//seek属性表示移动指针到指定参数的位置，然后文件写入从该位置开始。
                byte[] bytes = new byte[1024];//缓冲区，主要是RandomAccesFile和inputstream需要借助缓冲区读写文件，数组的长度表示一次操作的字节数。
                while ((len = inputStream.read(bytes)) != -1)//反复从网络文件字节流中读取指定缓冲区字节数的数据放入缓冲区，返回值为实际读取的字节数，当读取到文件末尾后返回-1.
                {
                    if (isPause)//暂停按钮按下之后强制退出
                    {
                        return TYPE_PAUSED;

                    } else if (isCancel)//停止下载按钮按下之后强制退出
                    {
                        return TYPE_CANCELED;
                    } else {
                        tempfile.write(bytes, 0, len);//注意每次写入的都是实际读取到的字节数，也就是len，不能完全写入bytes数组，因为不会自动清空bbytes数组。
                        int downloadPercent = (int) ((double) tempfile.length() / (double) size * 100);//先转换成double相除会有小数点，再乘100相当于乘了100%，就能得到百分比了。
                        publishProgress(downloadPercent);//及时更新下载的进度。
                    }
                }
                Log.d(TAG, "最终写入文件大小" + tempfile.length());
                response.body().close();//关闭字节流。
                return TYPE_SUCCESS;//报告状态，下载成功。
            } catch(IOException e){
                e.printStackTrace();
            }
        finally{//关闭各种字节流、文件流。
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (tempfile != null) {
                        tempfile.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return TYPE_FAILED;//前面所有操作都没有执行的话，就表示下载失败，有可能是网络资源无效等其他问题。

    }

    @Override
    protected void onProgressUpdate(Integer... values)
    {
        int progress = values[0];//获取具体的下载进度。
        if (progress > lastProgress) {//借用lastprogress变量来达到下载进度更新之后再更新状态，否则更新太频繁会造成卡顿现象。
            Log.d(TAG, "下载进度"+progress);
            listener.onProgress(progress);//下载监听器的作用，实现服务与子线程的数据交换通信。
            lastProgress = progress;
        }
    }


    //根据网址判断需要下载的文件的大小。
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

    protected void onPostExecute(Integer status) {//处理doinbackground中各种return值，判断文件下载状态，通知服务做出反应。
        switch (status) {
            case TYPE_SUCCESS:
                Log.d(TAG, "下载完成！");
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload()
    {
        isPause=true;
    }

    public void cancelDownload()
    {
        isCancel=true;
    }


}
