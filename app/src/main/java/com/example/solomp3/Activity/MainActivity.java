package com.example.solomp3.Activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.solomp3.Adapter.ViewPagerAdapter;
import com.example.solomp3.DB.FavoritesOperations;
import com.example.solomp3.Fragments.AllSongFragment;
import com.example.solomp3.Fragments.CurrentSongFragment;
import com.example.solomp3.Fragments.FavSongFragment;
import com.example.solomp3.Model.SongsList;
import com.example.solomp3.R;
import com.example.solomp3.Services.OnClearFromRecentService;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity<cListener> extends AppCompatActivity implements Playable, View.OnClickListener, AllSongFragment.createDataParse, FavSongFragment.createDataParsed, CurrentSongFragment.createDataParsed {

    private Menu menu;
    NotificationManager notificationManager;
    boolean isPlaying = false;

    private ImageButton imgBtnPlayPause, imgbtnReplay, imgBtnPrev, imgBtnNext, imgBtnSetting;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SeekBar seekbarController;
    private DrawerLayout mDrawerLayout;
    private TextView tvCurrentTime, tvTotalTime;
    private String m_text="";

//    private Button log;


    private ArrayList<SongsList> songList;
    private int currentPosition;
    private String searchText = "";
    private SongsList currSong;

    private boolean checkFlag = false, repeatFlag = false, playContinueFlag = false, favFlag = true, playlistFlag = false;
    private final int MY_PERMISSION_REQUEST = 100;
    private int allSongLength;

    MediaPlayer mediaPlayer;
    Handler handler;
    Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        grantedPermission();
//        gracefullyStopWhenMusicEnds();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onNextClicked(mp);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel();
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }

    }

    public void onNextClicked(MediaPlayer mp){
        if (checkFlag) {
            if (currentPosition + 1 < songList.size()) {
                attachMusic(songList.get(currentPosition + 1).getTitle(), songList.get(currentPosition + 1).getPath());
                currentPosition += 1;
            } else {
                Toast.makeText(this, "Playlist Ended", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Select the Song ..", Toast.LENGTH_SHORT).show();
        }

    }



    /**
     * Initialising the views
     */

    private void init() {
        imgBtnPrev = findViewById(R.id.img_btn_previous);
        imgBtnNext = findViewById(R.id.img_btn_next);
        imgbtnReplay = findViewById(R.id.img_btn_replay);
        imgBtnSetting = findViewById(R.id.img_btn_setting);
//        log = findViewById(R.id.btnLog);

        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        FloatingActionButton refreshSongs = findViewById(R.id.btn_refresh);
        seekbarController = findViewById(R.id.seekbar_controller);
        viewPager = findViewById(R.id.songs_viewpager);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        imgBtnPlayPause = findViewById(R.id.img_btn_play);
        Toolbar toolbar = findViewById(R.id.toolbar);
        handler = new Handler();
        mediaPlayer = new MediaPlayer();

        toolbar.setTitleTextColor(getResources().getColor(R.color.text_color));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.menu_icon);

        imgBtnNext.setOnClickListener(this);
        imgBtnPrev.setOnClickListener(this);
        imgbtnReplay.setOnClickListener(this);
        refreshSongs.setOnClickListener(this);
        imgBtnPlayPause.setOnClickListener(this);
        imgBtnSetting.setOnClickListener(this);
//        log.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                mDrawerLayout.closeDrawers();
                switch (item.getItemId()) {
                    case R.id.nav_about:
                        about();
                        break;
                    case R.id.nav_sleep_timer:
                        showSetTimer();
                        break;
                    case R.id.nav_logIn:
                        showLogin();
                        break;
                }
                return true;
            }
        });
    }


    private void showLogin(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LinearLayout lila1= new LinearLayout(this);
//        lila1.setOrientation(1); //1 is for vertical orientation
        lila1.setOrientation(LinearLayout.VERTICAL);
        final EditText inUsername = new EditText(this);
        inUsername.setInputType(InputType.TYPE_CLASS_TEXT);
        inUsername.setHint("Username");
        final EditText inPass = new EditText(this);
        inPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inPass.setHint("Password");
        lila1.addView(inUsername);
        lila1.addView(inPass);
        alert.setView(lila1);

//        alert.setIcon(R.drawable.icon);
        alert.setTitle("Login");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String username = inUsername.getText().toString().trim();
                String password = inPass.getText().toString().trim();
                Toast.makeText(getApplicationContext(), username+"--"+password, Toast.LENGTH_SHORT).show();
            }                     });
        alert.setNegativeButton("Regist",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showRegist();
                    }
        });
//        return alert.create();
        alert.create().show();
    }

    private void showRegist() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LinearLayout lila1= new LinearLayout(this);
//        lila1.setOrientation(1); //1 is for vertical orientation
        lila1.setOrientation(LinearLayout.VERTICAL);
        final EditText inUsername = new EditText(this);
        inUsername.setInputType(InputType.TYPE_CLASS_TEXT);
        inUsername.setHint("Username");

        final EditText inPass = new EditText(this);
        inPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inPass.setHint("Password");

        final EditText inPassConfirm = new EditText(this);
        inPassConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inPassConfirm.setHint("Confirm Password");

        final EditText nameUser = new EditText(this);
        nameUser.setInputType(InputType.TYPE_CLASS_TEXT);
        nameUser.setHint("Your Name");

        lila1.addView(inUsername);
        lila1.addView(inPass);
        lila1.addView(inPassConfirm);
        lila1.addView(nameUser);
        alert.setView(lila1);

//        alert.setIcon(R.drawable.icon);
        alert.setTitle("Register");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String username = inUsername.getText().toString().trim();
                String password = inPass.getText().toString().trim();
                String passwordConfirm = inPassConfirm.getText().toString().trim();
                String name = nameUser.getText().toString().trim();

                Toast.makeText(getApplicationContext(), username+"--"+password+"--"+passwordConfirm+"--"+name, Toast.LENGTH_SHORT).show();
            }                     });
        alert.setNegativeButton("Cancle",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
//        return alert.create();
        alert.create().show();

    }

    private void showSetTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hẹn giờ.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Nhập số phút chuẩn bị tắt.");
        builder.setView(input);
        builder.setItems(R.array.time_level, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String levels[] = getResources().getStringArray(R.array.time_level);
                Toast.makeText(MainActivity.this, "time to sleep" + levels[which], Toast.LENGTH_SHORT).show();
                int timeSleep = Integer.parseInt(levels[which]);
                setOff(timeSleep);
            }
        });
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_text = input.getText().toString();
                Toast.makeText(MainActivity.this, m_text, Toast.LENGTH_SHORT).show();
                int timeSleep = Integer.parseInt(m_text);
                setOff(timeSleep);
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void setOff(int time) {
        new CountDownTimer(time*1000, 1000) {

            public void onTick(long millisUntilFinished) {
//                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                Log.e("second_remaing", String.valueOf(millisUntilFinished/1000));
            }

            public void onFinish() {

                System.exit(0);
            }
        }.start();
    }

    /**
     * Function to ask user to grant the permission.
     */

    private void grantedPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar snackbar = Snackbar.make(mDrawerLayout, "Provide the Storage Permission", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        } else {
            setPagerLayout();
        }
    }

    /**
     * Checking if the permission is granted or not
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                        setPagerLayout();
                    } else {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout, "Provide the Storage Permission", Snackbar.LENGTH_LONG);
                        snackbar.show();
                        finish();
                    }
                }
        }
    }

    /**
     * Setting up the tab layout with the viewpager in it.
     */

    private void setPagerLayout() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getContentResolver());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    /**
     * Function to show the Nav --
     */
    private void about() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.about))
                .setMessage(getString(R.string.about_text))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "OK Man", Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog alertDialog = builder.create();
        LayoutInflater inflater = getLayoutInflater();
        View dialogAboutView = inflater.inflate(R.layout.about, null);
        alertDialog.setView(dialogAboutView);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText;
                queryText();
                setPagerLayout();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(Gravity.START);
                return true;
            case R.id.menu_search:
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_favorites:
                if (checkFlag)
                    if (mediaPlayer != null) {
                        if (favFlag) {
                            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                            item.setIcon(R.drawable.ic_favorite_filled);
                            SongsList favList = new SongsList(songList.get(currentPosition).getTitle(),
                                    songList.get(currentPosition).getSubTitle(), songList.get(currentPosition).getPath());
                            FavoritesOperations favoritesOperations = new FavoritesOperations(this);
                            favoritesOperations.addSongFav(favList);
                            setPagerLayout();
                            favFlag = false;
                        } else {
                            item.setIcon(R.drawable.favorite_icon);
                            favFlag = true;
                        }
                    }
                return true;
        }

        return super.onOptionsItemSelected(item);

    }

    /**
     * Function auto next music
     *
     */
//    private void gracefullyStopWhenMusicEnds() {
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                next(view);
//            }
//        });
//    }
//
//
//    public void next(View view)
//    {
//        Song nextSong = SongCollection.getNextSong(songId);
//
//        if ( nextSong != null)
//        {
//            songId = nextSong.getId();
//            title = nextSong.getTitle();
//            artist = nextSong.getArtist();
//            fileLink = nextSong.getFileLink();
//            coverArt = nextSong.getCoverArt();
//
//            url = BASE_URL + fileLink;
//            displaySong(title,artist,coverArt);
//            stopActivities();
//            playOrPauseMusic(view);
//        }
//
//    }


//    MediaPlayer.OnCompletionListener cListener = new MediaPlayer.OnCompletionListener(){
//
//        public void onCompletion(MediaPlayer mp){
//            //do something
//        }
//    };
//
////    mediaPlayer.setOnCompletionListener();
//
//
//    public MediaPlayer getMediaPlayer() {
//        return mediaPlayer;
//    }
//
    /**
     * Function Notification
     */
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CreateNotification.CHANNEL_ID,
                    "CHANEL SOLOMP3", NotificationManager.IMPORTANCE_LOW);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            switch (action){
                case CreateNotification.ACTION_PREVIUOS:
                    onTrackPrevious();
                    break;
                case CreateNotification.ACTION_PLAY:
                    if (mediaPlayer.isPlaying()){
                        onTrackPause();
                    } else {
                        onTrackPlay();
                    }
                    break;
                case CreateNotification.ACTION_NEXT:
                    onTrackNext();
                    break;
            }
        }
    };

    @Override
    public void onTrackPrevious() {

        currentPosition--;
        CreateNotification.createNotification(MainActivity.this, songList.get(currentPosition),
                R.drawable.ic_pause_black_24dp, currentPosition, songList.size()-1);
//        title.setText(songList.get(currentPosition).getTitle());
        Toast.makeText(this, "On track previus ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTrackPlay() {

        CreateNotification.createNotification(MainActivity.this, songList.get(currentPosition),
                R.drawable.ic_pause_black_24dp, currentPosition, songList.size()-1);
//        play.setImageResource(R.drawable.ic_pause_black_24dp);
//        title.setText(tracks.get(position).getTitle());
        isPlaying = true;
        Toast.makeText(this, "On track play in player_layout", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onTrackPause() {

        CreateNotification.createNotification(MainActivity.this, songList.get(currentPosition),
                R.drawable.ic_play_arrow_black_24dp, currentPosition, songList.size()-1);
//        play.setImageResource(R.drawable.ic_play_arrow_black_24dp);
//        title.setText(tracks.get(position).getTitle());
        Toast.makeText(this, "On track pause in player_layout", Toast.LENGTH_SHORT).show();
        isPlaying = false;

    }

    @Override
    public void onTrackNext() {

        currentPosition++;
        CreateNotification.createNotification(MainActivity.this, songList.get(currentPosition),
                R.drawable.ic_pause_black_24dp, currentPosition, songList.size()-1);
        Toast.makeText(this, "On track Next", Toast.LENGTH_SHORT).show();
//        title.setText(tracks.get(position).getTitle());

    }




    /**
     * Function to handle the click events.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_btn_play:
                if (checkFlag) {
                    //pause
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        imgBtnPlayPause.setImageResource(R.drawable.play_icon);
                        CreateNotification.createNotification(this,songList.get(currentPosition),R.drawable.play_icon,currentPosition,songList.size()-1);
                    } else if (!mediaPlayer.isPlaying()) {
                        //play
                        mediaPlayer.start();
                        imgBtnPlayPause.setImageResource(R.drawable.pause_icon);
                        playCycle();
                        CreateNotification.createNotification(this,songList.get(currentPosition),R.drawable.play_icon,currentPosition,songList.size()-1);
                    }
                } else {
                    Toast.makeText(this, "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_refresh:
                Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show();
                setPagerLayout();
                break;
            case R.id.img_btn_replay:

                if (repeatFlag) {
                    Toast.makeText(this, "Replaying Removed..", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setLooping(false);
                    repeatFlag = false;
                } else {
                    Toast.makeText(this, "Replaying Added..", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setLooping(true);
                    repeatFlag = true;
                }
                break;
            case R.id.img_btn_previous:
                if (checkFlag) {
                    if (mediaPlayer.getCurrentPosition() > 10) {
                        if (currentPosition - 1 > -1) {
                            attachMusic(songList.get(currentPosition - 1).getTitle(), songList.get(currentPosition - 1).getPath());
                            currentPosition = currentPosition - 1;
                            CreateNotification.createNotification(this,songList.get(currentPosition),R.drawable.play_icon,currentPosition,songList.size()-1);
                        } else {
                            attachMusic(songList.get(currentPosition).getTitle(), songList.get(currentPosition).getPath());
                        }
                    } else {
                        attachMusic(songList.get(currentPosition).getTitle(), songList.get(currentPosition).getPath());
                    }
                } else {
                    Toast.makeText(this, "Select a Song . .", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.img_btn_next:
                if (checkFlag) {
                    if (currentPosition + 1 < songList.size()) {
                        attachMusic(songList.get(currentPosition + 1).getTitle(), songList.get(currentPosition + 1).getPath());
                        currentPosition += 1;
//                        CreateNotification.createNotification(getContext(),songsList.get(position),R.drawable.ic_pause_black_24dp,position,songsList.size()-1);
                        CreateNotification.createNotification(this,songList.get(currentPosition),R.drawable.play_icon,currentPosition,songList.size()-1);
                    } else {
                        Toast.makeText(this, "Playlist Ended", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.img_btn_setting:
                if (!playContinueFlag) {
                    playContinueFlag = true;
                    Toast.makeText(this, "Loop Added", Toast.LENGTH_SHORT).show();
                } else {
                    playContinueFlag = false;
                    Toast.makeText(this, "Loop Removed", Toast.LENGTH_SHORT).show();
                }
                break;
//            case R.id.btnLog:
//                Toast.makeText(this,songList.get(currentPosition).getTitle() , Toast.LENGTH_SHORT).show();
////                Log.e("TAG", songList.get(currentPosition + 1).getTitle());
//                break;
        }
    }

    /**
     * Function to attach the song to the music player
     *
     * @param name
     * @param path
     */

    private void attachMusic(String name, String path) {
        imgBtnPlayPause.setImageResource(R.drawable.play_icon);
        setTitle(name);
        menu.getItem(1).setIcon(R.drawable.favorite_icon);
        favFlag = true;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            setControls();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imgBtnPlayPause.setImageResource(R.drawable.play_icon);
                if (playContinueFlag) {
                    if (currentPosition + 1 < songList.size()) {
                        attachMusic(songList.get(currentPosition + 1).getTitle(), songList.get(currentPosition + 1).getPath());
                        currentPosition += 1;
                    } else {
                        Toast.makeText(MainActivity.this, "PlayList Ended", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * Function to set the controls according to the song
     */

    private void setControls() {
        seekbarController.setMax(mediaPlayer.getDuration());
        mediaPlayer.start();
        playCycle();
        checkFlag = true;
        if (mediaPlayer.isPlaying()) {
            imgBtnPlayPause.setImageResource(R.drawable.pause_icon);
            tvTotalTime.setText(getTimeFormatted(mediaPlayer.getDuration()));
        }

        seekbarController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(getTimeFormatted(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Function to play the song using a thread
     */
    private void playCycle() {
        try {
            seekbarController.setProgress(mediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(getTimeFormatted(mediaPlayer.getCurrentPosition()));
            if (mediaPlayer.isPlaying()) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        playCycle();

                    }
                };
                handler.postDelayed(runnable, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTimeFormatted(long milliSeconds) {
        String finalTimerString = "";
        String secondsString;

        //Converting total duration into time
        int hours = (int) (milliSeconds / 3600000);
        int minutes = (int) (milliSeconds % 3600000) / 60000;
        int seconds = (int) ((milliSeconds % 3600000) % 60000 / 1000);

        // Adding hours if any
        if (hours > 0)
            finalTimerString = hours + ":";

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10)
            secondsString = "0" + seconds;
        else
            secondsString = "" + seconds;

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // Return timer String;
        return finalTimerString;
    }


    /**
     * Function Overrided to receive the data from the fragment
     *
     * @param name
     * @param path
     */

    @Override
    public void onDataPass(String name, String path) {
        Toast.makeText(this, name, Toast.LENGTH_LONG).show();
        attachMusic(name, path);
    }

    @Override
    public void getLength(int length) {
        this.allSongLength = length;
    }

    @Override
    public void fullSongList(ArrayList<SongsList> songList, int position) {
        this.songList = songList;
        this.currentPosition = position;
        this.playlistFlag = songList.size() == allSongLength;
        this.playContinueFlag = !playlistFlag;
    }

    @Override
    public String queryText() {
        return searchText.toLowerCase();
    }

    @Override
    public SongsList getSong() {
        currentPosition = -1;
        return currSong;
    }

    @Override
    public boolean getPlaylistFlag() {
        return playlistFlag;
    }

    @Override
    public void currentSong(SongsList songsList) {
        this.currSong = songsList;
    }

    @Override
    public int getPosition() {
        return currentPosition;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        handler.removeCallbacks(runnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationManager.cancelAll();
        }

        unregisterReceiver(broadcastReceiver);
    }

}
