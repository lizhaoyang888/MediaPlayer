package com.example.uu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicActivity extends AppCompatActivity {


    public static List<MusicMedia> musicList = null; //音乐信息列表
    private ImageView btn_play_pause = null;
    public static SeekBar audioSeekBar = null;//定义进度条
    public static TextView textView = null;
    public static TextView timeView = null;
    private Intent intent = null;
    private static int currentposition = -1;//当前播放列表里哪首音乐
    private boolean isplay = false;//音乐是否在播放
    private static MusicPlayerService musicPlayerService = null;
    private static MediaPlayer mediaPlayer = null;
    public static Handler handler = null;//处理界面更新，seekbar ,textview
    private boolean isservicerunning = false;//退出应用再进入时（点击app图标或者在通知栏点击service）使用，判断服务是否在启动
    private boolean isExit = false;//返回键
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;//保存播放模式
    private ImageView playMode ,playaccelerometer;
    private int[] modepic = {R.drawable.ic_shuffle_black_24dp,R.drawable.ic_repeat_black_24dp,R.drawable.ic_repeat_one_black_24dp};
    private int clicktime = 0;//accelerometer 切换

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MusicAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicList = scanAllMusicFile();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeAsUpIndicator(R.drawable.icon);
        }
        navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.item1);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                drawerLayout.closeDrawers();
                return true;
            }
        });

        LinearLayoutManager manager = new LinearLayoutManager(this);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycle_view);
        //StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(3,StaggeredGridLayoutManager.VERTICAL);
        adapter = new MusicAdapter(musicList,MusicActivity.this);
        //manager.setOrientation(LinearLayout.HORIZONTAL);
        adapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                currentposition = position;
                player(currentposition);
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.toolbarColor);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT,"我的微信：LZY834560891");
                shareIntent.setType("text/plain");
                //设置分享列表
                startActivity(Intent.createChooser(shareIntent,"分享到"));
            case R.id.item1:
                break;
            case R.id.item2:
                break;
            case R.id.item3:
                break;
            case R.id.item4:
                break;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
                break;
        }
        return true;
    }

    private void refresh(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MusicActivity.this, "hello", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();

    }



    private void init() {
        intent = new Intent(MusicActivity.this,MusicPlayerService.class);
        //默认随机播放
        playMode = (ImageView)findViewById(R.id.play_mode);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();
        int playmode = sharedPreferences.getInt("play_mode", -1);
        if(playmode == -1){//没有设置模式，默认随机
            editor.putInt("play_mode",0).commit();
        }else{
            changeMode(playmode);
        }
        //摇一摇
        playaccelerometer = (ImageView)findViewById(R.id.paly_accelerometer);
        if(sharedPreferences.getInt("play_accelerometer",0) == 0){
            //默认摇一摇是打开的
            clicktime = 0;
            playaccelerometer.setBackgroundResource(R.drawable.ic_alarm_on_black_24dp);
        }else{
            clicktime = 1;
            playaccelerometer.setBackgroundResource(R.drawable.ic_alarm_off_black_24dp);
        }

        handler = new Handler();

        textView  = (TextView)findViewById(R.id.musicinfo);

        timeView  = (TextView)findViewById(R.id.time);

        btn_play_pause = (ImageView)findViewById(R.id.play_pause);

        //进度条
        audioSeekBar = (SeekBar) findViewById(R.id.seekBar);

        //退出后再次进去程序时，进度条保持持续更新
        if(MusicPlayerService.mediaPlayer!=null){
            reinit();//更新页面布局以及变量相关
        }

         //播放进度监 ，使用静态变量时别忘了Service里面还有个进度条刷新
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (currentposition == -1) {
                    //还没有选择要播放的音乐
                    showInfo("请选择要播放的音乐");
                } else {
                    //假设改变源于用户拖动
                    if (fromUser) {
                        //这里有个问题，如果播放时用户拖进度条还好说，但是如果是暂停时，拖完会自动播放，所以还需要把图标设置一下
                        btn_play_pause.setBackgroundResource(R.drawable.pause);
                        MusicPlayerService.mediaPlayer.seekTo(progress);// 当进度条的值改变时，音乐播放器从新的位置开始播放
                    }

                }           }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        });

    }

    private void reinit() {

        isservicerunning = true;
        //如果是正在播放
        if(MusicPlayerService.mediaPlayer.isPlaying()){
            isplay = true;
            btn_play_pause.setBackgroundResource(R.drawable.pause);
        }
        //重新绑定service
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }


    public void previous(View view) {
        previousMusic();
    }



    public void play_pause(View view) {
        //当前是pause的图标,（使用图标来判断是否播放，就不需要再新定义变量为状态了,表示没能找到得到当前背景的图片的）实际上播放着的，暂停
        if(isservicerunning){//服务启动着，这里点击播放暂停按钮时只需要当前音乐暂停或者播放就好
            if (isplay) {
                pause();
            } else {
                 //暂停--->继续播放
                 player("2");
            }
        }else {
            if (isplay) {
                pause();
            } else {
                //当前是play的图标,是 暂停 着的
                //初始化时，没有点击列表，直接点击了播放按钮
                if (currentposition == -1) {
                    showInfo("请选择要播放的音乐");
                } else {
                    //暂停--->继续播放
                    player("2");
                }
            }
        }

    }

    public void next(View view) {
        nextMusic();
    }

    private void player() {
        player(currentposition);
    }

    private void player(int position){

        intent.putExtra("curposition", position);//把位置传回去，方便再启动时调用
        intent.putExtra("url", musicList.get(position).getUrl());
        intent.putExtra("MSG","0");
        isplay = true;
        btn_play_pause.setBackgroundResource(R.drawable.pause);

        startService(intent);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

    }
    private ServiceConnection conn = new ServiceConnection() {
        /** 获取服务对象时的操作 */
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            musicPlayerService = ((MusicPlayerService.musicBinder)service).getPlayInfo();
            mediaPlayer = musicPlayerService.getMediaPlayer();
            currentposition = musicPlayerService.getCurposition();
            //设置进度条
            handler.post(seekBarHandler);
        }

        /** 无法获取到服务对象时的操作 */
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerService = null;
        }

    };

     static Runnable seekBarHandler = new Runnable() {
        @Override
        public void run() {
            audioSeekBar.setMax(musicPlayerService.getDuration());
            audioSeekBar.setProgress(musicPlayerService.getCurrentPosition());
            textView.setText(musicPlayerService.getMusicMedia().getTitle()+" - "+musicPlayerService.getMusicMedia().getArtist());
            timeView.setText(musicPlayerService.toTime(musicPlayerService.getCurrentPosition())+" / " +
                    musicPlayerService.toTime(musicPlayerService.getDuration()));
            handler.postDelayed(seekBarHandler, 1000);

        }
    };


    private void player(String info){
        intent.putExtra("MSG",info);
        isplay = true;
        btn_play_pause.setBackgroundResource(R.drawable.pause);
        startService(intent);
    }
    /*
    * MSG :
    *  0  未播放--->播放
    *  1    播放--->暂停
    *  2    暂停--->继续播放
    *
    * */
    private void pause() {
        intent.putExtra("MSG","1");
        isplay = false;
        btn_play_pause.setBackgroundResource(R.drawable.play);
        startService(intent);
    }
    public  void previousMusic() {
        if(currentposition > 0){
            currentposition -= 1;
            player();
        }else{
            showInfo("已经是第一首音乐了");
        }
    }

    private void nextMusic() {
        if(currentposition >= musicList.size()-1){
            showInfo("已经是最后一首音乐了");
        }else{
            currentposition += 1;
            player();
        }
    }

    private void showInfo(String info) {
        Toast.makeText(this,info,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //绑定服务了
        if(musicPlayerService != null){
            unbindService(conn);
        }
        handler.removeCallbacks(seekBarHandler);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    private void exit(String info) {
        if(!isExit) {
            isExit = true;
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            finish();
        }
    }
    //按两次返回键退出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            //音乐服务启动了，隐藏至通知栏
            if(musicPlayerService != null){
                exit("再按一次隐藏至通知栏");
            }else{
                exit("再按一次退出程序");
            }

        }
        return false;
    }

    //修改播放模式  单曲循环 随机播放 顺序播放
    int clicktimes = 0;
    public void changeMode(View view) {
        switch (clicktimes){
            case 0://随机 --> 顺序
                clicktimes++;
                changeMode(clicktimes);
                break;
            case 1://顺序 --> 单曲
                clicktimes++;
                changeMode(clicktimes);
                break;
            case 2://单曲 --> 随机
                clicktimes = 0;
                changeMode(clicktimes);
                break;
            default:
                break;
        }

    }
    private void changeMode(int playmode) {
        editor.putInt("play_mode",playmode).commit();
        playMode.setBackgroundResource(modepic[playmode]);
    }

    public void changeAccelerometer(View view) {
        if(clicktime == 0){
            //当前是摇一摇打开的状态--> 关闭摇一摇
            clicktime = 1;
            playaccelerometer.setBackgroundResource(R.drawable.ic_alarm_off_black_24dp);
            editor.putInt("play_accelerometer", 1).commit();
        }else {
            //关闭-->打开
            clicktime = 0;
            playaccelerometer.setBackgroundResource(R.drawable.ic_alarm_on_black_24dp);
            editor.putInt("play_accelerometer", 0).commit();
        }
    }

    /**
     * 扫描所有的音乐文件
     * @param
     */
    public List<MusicMedia> scanAllMusicFile(){
        List<MusicMedia> list = new ArrayList<>();
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        //遍历媒体数据库
        if(cursor.moveToFirst()){
            while (!cursor.isAfterLast()) {
                //歌曲编号
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                //歌曲标题
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                //歌曲的专辑名：MediaStore.Audio.Media.ALBUM
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                int albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                //歌曲的歌手名： MediaStore.Audio.Media.ARTIST
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                //歌曲文件的路径 ：MediaStore.Audio.Media.DATA
                String url = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                //歌曲的总播放时长 ：MediaStore.Audio.Media.DURATION
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                //歌曲文件的大小 ：MediaStore.Audio.Media.SIZE
                Long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));

                if (size >1024*800){//大于800K

                    MusicMedia music = new MusicMedia();
                    music.setId(id);
                    music.setAlbum(album);
                    music.setArtist(artist);
                    music.setSize(size);
                    music.setTime(duration);
                    music.setUrl(url);
                    music.setTitle(title);

                    list.add(music);
                }
                cursor.moveToNext();
            }
        }
        return list;
    }

}
