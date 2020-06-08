package com.mitha.cobabeauty.activitys;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;


import com.crust87.videocropview.VideoCropView;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.mitha.cobabeauty.R;
import com.seerslab.argear.session.ARGFrame;


import org.telegram.messenger.CustomVideoTimelinePlayView;
import org.telegram.messenger.TrimUtils;

import java.io.File;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


//import com.crust87.ffmpegexecutor.FFmpegExecutor;


public class TrimmingActivity extends AppCompatActivity implements View.OnClickListener {


    FrameLayout videoViewWrapper;
    VideoCropView videoView;
    ImageView playBtn;
    ImageView trimBtn;
    ImageView saveBtn;
    LinearLayout trimmer;
    TextView tvSubs;
    TextView trimDurAndSizeTxt;
    TextView trimDurRangeTxt;
    CustomVideoTimelinePlayView timelineView;
    Button ratio;
    Button cancelBtn;


    private Uri videoUri;
    private File videoFile;
    private float videoDuration;
    private long trimStartTime;
    private long trimEndTime;
    private long originalSize;
    private Disposable trimTask;
    private ProgressDialog progressDialog;
    private Runnable updateProgressRunnable;
    private Timer trimDurCounterTimer;
    private File result;
    private StringVideo stringVideo = new StringVideo();
    private static final int VIDEO_MIN_DURATION_MS = 1000;
    private static final int VIDEO_MAX_DURATION_MS = 7000;
    private static final long VIDEO_MAX_SIZE = 10 * 1024 * 1024;
    private boolean mIsVideoProfile;
    private String realPath;
    boolean isselected = false;
    private int mWidthRatio;
    private String originalPatch;
//    private FFmpegExecutor mExecutor;
    private String originalPath;
    private ProgressDialog mProgressDialog;
    private int mRatioWidth;
    private String outputPatch;
    private String realPatch;
    private FFmpeg fFmpeg;


    float scale;
    int viewWidth;
    int viewHeight;
    int width;
    int height;
    int positionX;
    int positionY;
    int videoWidth;
    int videoHeight;
    int rotate;
    String start;
    String dur;
    String filter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_trimming);
        ButterKnife.bind(this);
        initialize();
    }


    @SuppressLint({"WrongViewCast", "SetTextI18n"})
    private void initialize() {

        videoViewWrapper = findViewById(R.id.videoViewWrapper);
        videoView = findViewById(R.id.videoView);
        playBtn = findViewById(R.id.playBtn);
        trimBtn = findViewById(R.id.trimVideo);
        trimDurAndSizeTxt = findViewById(R.id.trimDurAndSizeTxt);
        trimDurRangeTxt = findViewById(R.id.trimDurRangeTxt);
        timelineView = findViewById(R.id.timelineView);
        cancelBtn = findViewById(R.id.cancel_button);
        ratio = findViewById(R.id.ratio_button);
        tvSubs = findViewById(R.id.tvSubs);
        trimmer = findViewById(R.id.trimVideoLL);
        saveBtn = findViewById(R.id.saveBtn);
        ratio.setOnClickListener(this);
        videoViewWrapper.setOnClickListener(this);
        trimBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);

        outputPatch = Environment.getExternalStorageDirectory().toString() + File.separator + stringVideo.APP_FOLDER + File.separator;
        trimDurAndSizeTxt.setOnClickListener(view -> new CustomAlertBacksound(TrimmingActivity.this, "Ratio",
                isselected ? "Rubah ratio video ke 16:9" : "Rubah ratio video ke 4:3", "Ya", "Tidak",
                new CustomAlertBacksound.CustomAlertOnResponse() {
                    @Override
                    public void onPositiveButton() {
                        videoView.setRatio(isselected ?1f:4f,isselected ?1f:3f);
                        mRatioWidth = isselected ?1:4;
//                        compressVideo();
                        isselected = !isselected;
                    }

                    @Override
                    public void onNegativeButton() {

                    }
                }));


        try {
            videoUri = getIntent().getParcelableExtra("path");
            mIsVideoProfile = getIntent().getBooleanExtra("isVideoProfile", false);
            realPath = FileUtils.getRealPath(this, videoUri);
            videoFile = new File(realPath);
            initVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setRatio(){
        int width= (int)getResources().getDimension(R.dimen.btnwidth);
        int heigthfull= (int)getResources().getDimension(R.dimen.btnfull);
        int heigth43= (int)getResources().getDimension(R.dimen.btn43);
        int heigth11= (int)getResources().getDimension(R.dimen.btn11);

        if (ratio.getTag().equals("ganti")) {
            ratio.setTag("update");
            ratio.setLayoutParams(new LinearLayout.LayoutParams(width,heigth43));
            videoView.setRatio(4,3);
            tvSubs.setVisibility(View.VISIBLE);

        }else if (ratio.getTag().equals("update")) {
            ratio.setLayoutParams(new LinearLayout.LayoutParams(width,heigth11));
            ratio.setTag("ratio");
            videoView.setRatio(1,1);
            tvSubs.setVisibility(View.VISIBLE);

        }else if (ratio.getTag().equals("ratio")){
            ratio.setHeight(24);
            ratio.setLayoutParams(new LinearLayout.LayoutParams(heigthfull,width));
            ratio.setTag("rasio");
            videoView.setRatio(9,16);
            tvSubs.setVisibility(View.GONE);

        }else if(ratio.getTag().equals("rasio")){
           ratio.setLayoutParams(new LinearLayout.LayoutParams(width,heigthfull));
           ratio.setTag("ganti");
           videoView.setRatio(16,9);
           tvSubs.setVisibility(View.VISIBLE);
        }
    }

    private void initVideo() {
        updateProgressRunnable = () -> {
            if (videoView == null || !videoView.isPlaying()) {
                timelineView.removeCallbacks(updateProgressRunnable);
            }
            timelineView.postDelayed(updateProgressRunnable, 17);
        };

        originalSize = videoFile.length();
        videoView.setOnPreparedListener(mediaPlayer -> {
            videoDuration = mediaPlayer.getDuration();
            initVideoTimelineView();
            playBtn.setVisibility(View.VISIBLE);
            updateVideoInfo();
        });
        videoView.setVideoURI(videoUri);
    }

    private void initVideoTimelineView() {

        if (videoDuration > VIDEO_MIN_DURATION_MS) {
            if (videoDuration >= (VIDEO_MAX_DURATION_MS)) {
                timelineView.setMinProgressDiff(VIDEO_MAX_DURATION_MS / videoDuration);
                timelineView.setMaxProgressDiff(VIDEO_MAX_DURATION_MS / videoDuration);
            } else {
                timelineView.setMinProgressDiff(videoDuration / videoDuration);
                timelineView.setMaxProgressDiff(videoDuration / videoDuration);
            }
        } else if (videoDuration < VIDEO_MIN_DURATION_MS) {
            String str_must_more = "Duration video must be more than";
            String str_second = "second";
            Toast.makeText(this, str_must_more + " " + VIDEO_MIN_DURATION_MS + " " + str_second, Toast.LENGTH_SHORT).show();
            trimBtn.setVisibility(View.GONE);
        }

        timelineView.setMaxVideoSize(VIDEO_MAX_SIZE, originalSize);
        timelineView.setDelegate(new CustomVideoTimelinePlayView.VideoTimelineViewDelegate() {

            @Override
            public void onLeftProgressChanged(float progress) {
                Log.d("onLeftProgressChanged ", String.valueOf(progress));
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
                //videoView.seekTo((int) (videoDuration * progress));
                timelineView.setProgress(0);
                updateVideoInfo();
            }

            @Override
            public void onRightProgressChanged(float progress) {
                Log.d("onRightProgressChanged ", String.valueOf(progress));
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
                //videoView.seekTo((int) (videoDuration * progress));
                timelineView.setProgress(0);
                updateVideoInfo();
            }

            @Override
            public void onPlayProgressChanged(float progress) {
                Log.d("onPlayProgressChanged ", String.valueOf(progress));
                videoView.seekTo((int) (videoDuration * progress));
            }

            @Override
            public void didStartDragging() {
                Log.d("didStartDragging", "");
            }

            @Override
            public void didStopDragging() {
                Log.d("didStopDragging", "");
                videoView.seekTo((int) (videoDuration * timelineView.getLeftProgress()));
            }
        });
        timelineView.setVideoPath(videoUri);
    }

    private void updateVideoInfo() {
        trimStartTime = (long) Math.ceil(timelineView.getLeftProgress() * videoDuration);
        trimEndTime = (long) Math.ceil(timelineView.getRightProgress() * videoDuration);
        long estimatedDuration = trimEndTime - trimStartTime;
        long estimatedSize = (int) (originalSize * ((float) estimatedDuration / videoDuration));
        String videoTimeSize = String.format(Locale.US, "%s, ~%s", UtilsVideoTrimming.getMinuteSeconds(estimatedDuration), UtilsVideoTrimming.formatFileSize(estimatedSize));
        trimDurAndSizeTxt.setText(videoTimeSize);
        String trimRangeDurStr = UtilsVideoTrimming.getMinuteSeconds(trimStartTime) + "-" + UtilsVideoTrimming.getMinuteSeconds(trimEndTime);
        trimDurRangeTxt.setText(trimRangeDurStr);
    }

    @Override
    public void onClick(View view) {
        if (videoViewWrapper == view) {
            playingVideoTrimmer();
        } else if (trimBtn == view) {
           trimmer.setVisibility(View.VISIBLE);
        } else if (cancelBtn == view) {
            cancelTrimmingVideo();
        }else if (ratio == view){
            setRatio();
        }else if (saveBtn == view){
            trimSave();
        }
    }

    private void trimSave(){
        Toast.makeText(this, "Trim Video Berhasil", Toast.LENGTH_SHORT).show();
        trimmer.setVisibility(View.GONE);
        ratio.setVisibility(View.GONE);
        LinearLayout lnSubs = findViewById(R.id.lnSubs);
        lnSubs.setVisibility(View.GONE);
        saveBtn.setImageResource(R.drawable.ic_edit_ok);
    }

    private void playingVideoTrimmer() {
        if (videoView.isPlaying()) {
            trimDurCounterTimer.cancel();
            videoView.pause();
            playBtn.setVisibility(View.VISIBLE);
        } else {
            trimDurCounterTimer = new Timer();
            trimDurCounterTimer.scheduleAtFixedRate(new TimerTask() {
                long currentTime;

                @Override
                public void run() {
                    try {
                        currentTime = videoView.getCurrentPosition();
                        String trimRangeDurStr = UtilsVideoTrimming.getMinuteSeconds(currentTime) + "-" + UtilsVideoTrimming.getMinuteSeconds(trimEndTime);
                        runOnUiThread(() -> trimDurRangeTxt.setText(trimRangeDurStr));
                        if (currentTime >= trimEndTime) {
                            trimDurCounterTimer.cancel();
                            runOnUiThread(() -> {
                                videoView.pause();
                                videoView.seekTo((int) trimStartTime);
                                String trimRangeDurStr2 = UtilsVideoTrimming.getMinuteSeconds(trimStartTime) + "-" + UtilsVideoTrimming.getMinuteSeconds(trimEndTime);
                                trimDurRangeTxt.setText(trimRangeDurStr2);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 100);
            videoView.start();
            timelineView.post(updateProgressRunnable);
            playBtn.setVisibility(View.GONE);
        }
    }

    private void trimmingVideo() {
        try {
            File outDir = new File(outputPatch);
            Completable trimCompletable = Completable.fromAction(() -> {
                result = TrimUtils.trimVideo(videoFile, outDir, (int) trimStartTime, (int) trimEndTime);
            });
            trimTask = trimCompletable
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(disposable -> {
                        String str_loading = "Trim in progress, please wait...";
                        showLoading(true, str_loading);
                    })
                    .subscribe(() -> {
                        convertVideo();
//                        showLoading(false, null);
//                        Intent intent = new Intent();
//                        intent.setDataAndType(Uri.fromFile(result), "video/mp4");
//                        setResult(RESULT_OK, intent);
//                        finish();
                    }, throwable -> showLoading(false, null));
        } catch (Exception e) {
            showLoading(false, null);
            e.printStackTrace();
        }
    }

    private void convertVideo() {
        scale = videoView.getScale();
        viewWidth = videoView.getWidth();
        viewHeight = videoView.getHeight();
        width = (int)(viewWidth * scale);
        height = (int)(viewHeight * scale);
        positionX = (int) videoView.getRealPositionX();
        positionY = (int) videoView.getRealPositionY();
        videoWidth = videoView.getVideoWidth();
        videoHeight = videoView.getVideoHeight();
        rotate = videoView.getRotate();

        try {
            File outDir = new File(outputPatch);
            Completable trimCompletable = Completable.fromAction(() -> {
                result = TrimUtils.convertVideo(videoFile, outDir);
            });
            trimTask = trimCompletable
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(disposable -> {
                        String str_loading = "Trim in progress, please wait...";
                        showLoading(true, str_loading);
                    })
                    .subscribe(() -> {
                        showLoading(false, null);
                        Intent intent = new Intent();
                        intent.setDataAndType(Uri.fromFile(result), "video/mp4");
                        setResult(RESULT_OK, intent);
                        finish();
                    }, throwable -> showLoading(false, null));
        } catch (Exception e) {
            showLoading(false, null);
            e.printStackTrace();
        }
    }

    private void cancelTrimmingVideo() {
        try {
            if (videoView != null) {
                videoView.stopPlayback();
                if (trimTask != null) {
                    trimTask.dispose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mIsVideoProfile) {
            setResult(RESULT_OK, null);
            finish();
        } else {
            finish();
        }

    }

    private void showLoading(boolean show, String message) {
        try {
            if (!TrimmingActivity.this.isFinishing()) {
                if (show) {
                    progressDialog = new ProgressDialog(TrimmingActivity.this);
                    progressDialog.setMessage(message);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                } else if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (trimTask != null) {
            trimTask.dispose();
        }
        super.onDestroy();
    }

    public void compressVideo() {

        fFmpeg = FFmpeg.getInstance(this);
        try{
            fFmpeg.loadBinary(new LoadBinaryResponseHandler(){
                @Override
                public void onFailure() {
                    super.onFailure();
                }

                @Override
                public void onSuccess() {
                    super.onSuccess();
                    executeFFmpeg();
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    executeFFmpeg();
                }
            });
        }catch (FFmpegNotSupportedException e){
            e.printStackTrace();
            Log.e("ERROR", String.valueOf(e));
        }

    }


    @SuppressLint("DefaultLocale")
    public void executeFFmpeg() {

//        mProgressDialog = ProgressDialog.show(TrimmingActivity.this, null, "execute....", true);
        realPatch = FileUtils.getFilePath(TrimmingActivity.this, videoUri);
        scale = videoView.getScale();
        viewWidth = videoView.getWidth();
        viewHeight = videoView.getHeight();
        width = (int)(viewWidth * scale);
        height = (int)(viewHeight * scale);
        positionX = (int) videoView.getRealPositionX();
        positionY = (int) videoView.getRealPositionY();
        videoWidth = videoView.getVideoWidth();
        videoHeight = videoView.getVideoHeight();
        rotate = videoView.getRotate();

        start = String.format("00:%02d:%02.2f", trimStartTime, trimEndTime/1000f);
        dur = String.format("00:00:%02.2f", videoDuration/1000f);

        // When need crop
        // FIXME
        if(mRatioWidth != 0) {
            // FIXME
            String filterScale = "setsar=1:1";
            if(mRatioWidth == 4) {
                filterScale = "scale=640:480, setsar=1:1";
            } else if(mRatioWidth == 3) {
                filterScale = "scale=480:640, setsar=1:1";
            } else if(mRatioWidth == 1) {
                filterScale = "scale=640:640, setsar=1:1";
            }

            if(rotate == 0) {
                filter = "crop="+width+":"+height+":"+positionX+":"+positionY+ ", " + filterScale;
            } else if(rotate == 90) {
                filter = "crop="+height+":"+width+":"+positionY+":"+positionX + ", " + filterScale;
            } else if(rotate == 180) {
                filter = "crop="+width+":"+height+":"+(videoWidth - positionX - width)+":"+positionY + ", " + filterScale;
            } else if(rotate == 270) {
                filter = "crop="+height+":"+width+":"+(videoHeight - positionY - height)+":"+positionX + ", " + filterScale;
            } else {
                filter = "crop="+width+":"+height+":"+positionX+":"+positionY + ", " + filterScale;
            }
        }


        String[] commandParams = new String[20];
        commandParams[0] = "-y";
        commandParams[1] = "-i";
        commandParams[2] = realPatch;
        commandParams[3] = "-vcodec";
        commandParams[4] = "libx264";
        commandParams[5] = "-profile:v";
        commandParams[6] = "baseline";
        commandParams[7] = "-level";
        commandParams[8] = "3.1";
        commandParams[9] = "-b:v";
        commandParams[10] = "1000k";
        commandParams[11] = "-ss";
        commandParams[12] = start;
        commandParams[13] = "-t";
        commandParams[14] = dur;
        commandParams[15] = "-c:a";
        commandParams[16] = "copy";
        commandParams[17] = outputPatch;
        commandParams[18] = "-vf";
        commandParams[19] = filter;

        try {
            fFmpeg.execute(commandParams, new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                }

                @Override
                public void onProgress(String message) {
                    Log.e("VideoCronProgress", message);
                }

                @Override
                public void onFailure(String message) {
                    Log.e("VideoCompressor", message);
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    Log.e("VideoCronProgress", "finnished");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }

    }

//    @SuppressLint("StaticFieldLeak")
//    public void cropVideo() {
//        videoView.pause();
//
//        new AsyncTask<Void, Void, Void>() {
//
//            float scale;
//            int viewWidth;
//            int viewHeight;
//            int width;
//            int height;
//            int positionX;
//            int positionY;
//            int videoWidth;
//            int videoHeight;
//            int rotate;
//            String start;
//            String dur;
//
//            @SuppressLint("DefaultLocale")
//            @Override
//            protected void onPreExecute() {
//                mExecutor.init();
//                mProgressDialog = ProgressDialog.show(TrimmingActivity.this, null, "execute....", true);
//                realPatch = FileUtils.getFilePath(TrimmingActivity.this, videoUri);
//                scale = videoView.getScale();
//                viewWidth = videoView.getWidth();
//                viewHeight = videoView.getHeight();
//                width = (int)(viewWidth * scale);
//                height = (int)(viewHeight * scale);
//                positionX = (int) videoView.getRealPositionX();
//                positionY = (int) videoView.getRealPositionY();
//                videoWidth = videoView.getVideoWidth();
//                videoHeight = videoView.getVideoHeight();
//                rotate = videoView.getRotate();
//
//                start = String.format("00:%02d:%02.2f", trimStartTime, trimEndTime/1000f);
//                dur = String.format("00:00:%02.2f", videoDuration/1000f);
//            }
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                try {
//                    mExecutor.putCommand("-y")
//                            .putCommand("-i")
//                            .putCommand(realPatch)
//                            .putCommand("-vcodec")
//                            .putCommand("libx264")
//                            .putCommand("-profile:v")
//                            .putCommand("baseline")
//                            .putCommand("-level")
//                            .putCommand("3.1")
//                            .putCommand("-b:v")
//                            .putCommand("1000k")
//                            .putCommand("-ss")
//                            .putCommand(start)
//                            .putCommand("-t")
//                            .putCommand(dur);
//
//                    // When need crop
//                    // FIXME
//                    if(mRatioWidth != 0) {
//                        String filter = "";
//
//                        // FIXME
//                        String filterScale = "setsar=1:1";
//                        if(mRatioWidth == 4) {
//                            filterScale = "scale=640:480, setsar=1:1";
//                        } else if(mRatioWidth == 3) {
//                            filterScale = "scale=480:640, setsar=1:1";
//                        } else if(mRatioWidth == 1) {
//                            filterScale = "scale=640:640, setsar=1:1";
//                        }
//
//                        if(rotate == 0) {
//                            filter = "crop="+width+":"+height+":"+positionX+":"+positionY+ ", " + filterScale;
//                        } else if(rotate == 90) {
//                            filter = "crop="+height+":"+width+":"+positionY+":"+positionX + ", " + filterScale;
//                        } else if(rotate == 180) {
//                            filter = "crop="+width+":"+height+":"+(videoWidth - positionX - width)+":"+positionY + ", " + filterScale;
//                        } else if(rotate == 270) {
//                            filter = "crop="+height+":"+width+":"+(videoHeight - positionY - height)+":"+positionX + ", " + filterScale;
//                        } else {
//                            filter = "crop="+width+":"+height+":"+positionX+":"+positionY + ", " + filterScale;
//                        }
//
//                        mExecutor.putCommand("-vf")
//                                .putCommand(filter);
//                    }
//
//                    mExecutor.putCommand("-c:a")
//                            .putCommand("copy")
//                            .putCommand(outputPatch)
//                            .executeCommand();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//                mProgressDialog.dismiss();
//                mProgressDialog = null;
//            }
//
//        }.execute();
//    }

}
