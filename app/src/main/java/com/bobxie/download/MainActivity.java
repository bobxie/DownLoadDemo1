package com.bobxie.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bobxie.download.entities.FileInfo;
import com.bobxie.download.services.DownloadService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ProgressBar mProgressBar;
    private Button mBtnStart;
    private Button mBtnStop;
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnStart = (Button) findViewById(R.id.button);
        mBtnStop = (Button) findViewById(R.id.button2);
        tvInfo = (TextView) findViewById(R.id.tv_info);

        mProgressBar.setMax(100);

        mBtnStart.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);

        //注册广播接收
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.ACTION_UPDATE);
        registerReceiver(receiver, intentFilter);

    }

    @Override
    public void onClick(View v) {
        FileInfo fileInfo = new FileInfo(0, "http://shouji.360tpcdn.com/161105/dbb921f21f2bfec3aa42ce732f625ed3/com.bobxie.greenwallpaper_6.apk"
                , "MultiThreadDownloader文件", 0, 0);
        if (v.getId() == R.id.button) { //开始
            Intent intent = new Intent(this, DownloadService.class);
            intent.setAction(DownloadService.ACTION_START);
            intent.putExtra(DownloadService.INTENT_FILEINFO, fileInfo);
            startService(intent);
        } else if (v.getId() == R.id.button2) { //停止
            Intent intent = new Intent(this, DownloadService.class);
            intent.setAction(DownloadService.ACTION_STOP);
            intent.putExtra(DownloadService.INTENT_FILEINFO, fileInfo);
            startService(intent);
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
                int finished = intent.getIntExtra("finished", 0);
                mProgressBar.setProgress(finished);
                tvInfo.setText(finished + "%");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
