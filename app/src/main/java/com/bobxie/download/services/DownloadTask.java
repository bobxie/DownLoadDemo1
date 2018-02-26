package com.bobxie.download.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bobxie.download.db.dao.ThreadInfoDao;
import com.bobxie.download.entities.FileInfo;
import com.bobxie.download.entities.ThreadInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * 下载任务类
 * Created by bob on 2018/2/25.
 */
public class DownloadTask {
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadInfoDao threadInfoDao = null;
    private int mFinished = 0;
    public boolean isPause = false;

    public DownloadTask(Context context, FileInfo mFileInfo) {
        this.mContext = context;
        this.mFileInfo = mFileInfo;
        threadInfoDao = new ThreadInfoDao(context);
    }

    /**
     * 执行下载
     */
    public void download() {
        //读取数据库的线程信息
        ThreadInfo threadInfo = null;
        try {
            List<ThreadInfo> threadInfos = threadInfoDao.getThreads(mFileInfo.getUrl());
            if (null == threadInfos || threadInfos.isEmpty()) {
                threadInfo = new ThreadInfo();
                threadInfo.setId(0);
                threadInfo.setUrl(mFileInfo.getUrl());
                threadInfo.setStart(0);
                threadInfo.setEnd(mFileInfo.getLength());
                threadInfo.setFinished(0);
            } else {
                threadInfo = threadInfos.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //执行线程开始下载
        new DownloadThread(threadInfo).start();
    }

    /**
     * 下载线程
     */
    class DownloadThread extends Thread {
        private ThreadInfo threadInfo = null;

        public DownloadThread(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {
            //向数据库插入线程信息
            if (!threadInfoDao.isExists(threadInfo)) {
                threadInfoDao.insetThread(threadInfo);
            }
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(threadInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setRequestMethod("GET");
                //设置下载位置
                long start = threadInfo.getStart() + threadInfo.getFinished();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);

                Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                mFinished += threadInfo.getFinished();

                //开始下载
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_PARTIAL) {
                    //读取数据
                    inputStream = conn.getInputStream();
                    byte[] buffer = new byte[1024 * 2];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while ((len = inputStream.read(buffer)) != -1) {
                        //写入文件
                        raf.write(buffer, 0, len);
                        //把下载进度发送广播给Activity
                        mFinished += len;
                        if (System.currentTimeMillis() - time > 100) {
                            time = System.currentTimeMillis();
                            int percent = (int) (mFinished * 100L / mFileInfo.getLength());
                            Log.d("bob", "当前完成：" + mFinished + " 总长度:" + mFileInfo.getLength() + " 已完成=" + percent + "%");
                            intent.putExtra("finished", percent);
                            mContext.sendBroadcast(intent);
                        }
                        //在下载暂停时，保存下载进度
                        if (isPause) {
                            threadInfo.setFinished(mFinished);
                            threadInfoDao.updateThread(threadInfo);
                            return;
                        }
                    }
                }

                //删除线程信息
                threadInfoDao.deleteThread(threadInfo);
                intent.putExtra("finished", 100);
                mContext.sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != conn) {
                        conn.disconnect();
                    }
                    if (null != raf) {
                        raf.close();
                    }
                    if (null != inputStream) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
