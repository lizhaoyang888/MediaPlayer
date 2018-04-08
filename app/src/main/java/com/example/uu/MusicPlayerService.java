package com.example.uu;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MusicPlayerService extends Service implements SensorEventListener {//implements Runnable {
    private static final String TAG = "MusicPlayerService";
    private static final int NOTIFICATION_ID = 1; // 如果id设置为0,会导致不能设置为前台service
    public static MediaPlayer mediaPlayer = null;
    private String url = null;
    private String MSG = null;
    private static int curposition;//第几首音乐
    private musicBinder musicbinder = null;
    private int currentPosition = 0;// 设置默认进度条当前位置
    private List<MusicMedia> musiclist;//音乐列表
    private MusicMedia musicMedia;//当前播放的音乐信息
    private SensorManager sensorManager = null;//传感器
    private Vibrator vibrator = null;//震动
    public MusicPlayerService() {
        musicbinder = new musicBinder();
    }

    //通过bind 返回一个IBinder对象，然后改对象调用里面的方法实现参数的传递
    @Override
    public IBinder onBind(Intent intent) {
       return musicbinder;
    }


    /**
     * 自定义的 Binder对象
     */
    public class musicBinder extends Binder {
        public MusicPlayerService getPlayInfo(){
            return MusicPlayerService.this;
        }
    }
    //得到当前播放位置
    public  int getCurrentPosition(){

        if(mediaPlayer != null){
            currentPosition = mediaPlayer.getCurrentPosition();
        }
        return currentPosition;
    }
    //得到总时长
    public  int getDuration(){
        return mediaPlayer.getDuration();// 总时长
    }
    //当前播放音乐
    public MusicMedia getMusicMedia() {
        return musicMedia;
    }

    //得到 mediaPlayer
    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    //得到 当前播放第几个音乐
    public int getCurposition(){
        return curposition;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        musiclist = MusicActivity.musicList;
         // 监听播放是否完成
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playnew();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //取得震动服务的句柄
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
       if(MusicActivity.sharedPreferences.getInt("play_accelerometer",0) == 0){
           // 传感器报告新的值
           int sensorType = event.sensor.getType();
           //values[0]:X轴，values[1]：Y轴，values[2]：Z轴
           float[] values = event.values;
           if (sensorType == Sensor.TYPE_ACCELEROMETER)
           {
               if ((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 || Math
                       .abs(values[2]) > 17))
               {
                   playnew();
                   //摇动手机后，再伴随震动提示~~
                   vibrator.vibrate(500);
               }

           }
       }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //传感器精度的改变
    }
    private void playnew() {
        switch (MusicActivity.sharedPreferences.getInt("play_mode",-1)){
            case 0://随机
                curposition = (new Random()).nextInt(musiclist.size());
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 1://顺序
                curposition = (++curposition) % musiclist.size();
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 2://单曲
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            default:
                break;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // /storage/emulated/0/Music/Download/Selena Gomez - Revival/Hands to Myself.mp3
        if(intent != null){
            MSG = intent.getStringExtra("MSG");
            if(MSG.equals("0")){
                url = intent.getStringExtra("url");
                curposition = intent.getIntExtra("curposition", 0);
                musicMedia = musiclist.get(curposition);
                palyer();
            }else if(MSG.equals("1")){
                mediaPlayer.pause();
            }else if(MSG.equals("2")){
                mediaPlayer.start();
            }

            String name = "Current: "+ url.substring(url.lastIndexOf("/") + 1 , url.lastIndexOf("."));
//        //开启前台service
            Notification notification = null;
            if (Build.VERSION.SDK_INT < 16) {
                notification = new Notification.Builder(this)
                        .setContentTitle("Enter the MusicPlayer").setContentText(name)
                        .setSmallIcon(R.drawable.hua).getNotification();
            } else {
                Notification.Builder builder = new Notification.Builder(this);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, MusicActivity.class), 0);
                builder.setContentIntent(contentIntent);
                builder.setSmallIcon(R.drawable.hua);
                builder.setContentTitle("Enter the MusicPlayer");
                builder.setContentText(name);
                notification = builder.build();
            }

            startForeground(NOTIFICATION_ID, notification);
        }
        //加速度传感器（accelerometer）、陀螺仪（gyroscope）、环境光照传感器（light）、磁力传感器（magnetic field）、方向传感器（orientation）、压力传感器（pressure）、距离传感器（proximity）和温度传感器（temperature）。
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        return super.onStartCommand(intent, flags, startId);
    }


    private void palyer() {
        try {
            mediaPlayer.reset();

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            musicMedia = musiclist.get(curposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //关闭线程
        Thread.currentThread().interrupt();
        stopForeground(true);
        sensorManager.unregisterListener(this);
    }
    public String toTime(int time){
        time /= 1000;
        int minute = time / 60;
        int hour = minute / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }
}
