package com.csr.masterapp.scene;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.csr.masterapp.entities.Alarm;

public class TimerAlertActivity extends Activity implements View.OnClickListener {
    private Alarm alarm;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean alarmActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.alert);
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            alarm = (Alarm) bundle.getSerializable("alarm");
            if (null != alarm) {
//                this.setTitle(alarm.getAlarmName());
                startAlarm();
            }
        }
//        SetSlideView();
        SetTelephonyStateChangedListener();
    }

    private void SetTelephonyStateChangedListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(getClass().getSimpleName(), "Incoming call: " + incomingNumber);
                        try {
                            mediaPlayer.pause();
                        } catch (IllegalStateException e) {

                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(getClass().getSimpleName(), "Call State Idle");
                        try {
                            mediaPlayer.start();
                        } catch (IllegalStateException e) {

                        }
                        break;
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

//    private void SetSlideView() {
//        SlideView slideView = (SlideView) findViewById(R.id.slider);
//        slideView.setSlideListener(new SlideView.SlideListener() {
//            @Override
//            public void onDone() {
//                AlarmServiceBroadcastReciever reciever = new AlarmServiceBroadcastReciever();
//                reciever.CancelAlarm(AlarmAlertActivity.this);
//                ReleaseRelease();
//                Toast.makeText(AlarmAlertActivity.this, "早起啦", Toast.LENGTH_SHORT).show();
//                Log.d("SHIT", String.valueOf(Build.VERSION.SDK_INT));
//                if (Build.VERSION.SDK_INT > 15) {
//                    quit();
//                    return;
//                }
//                int pid = android.os.Process.myPid();
//                android.os.Process.killProcess(pid);
//                System.exit(0);
//
//            }
//        });
//    }

    @TargetApi(16)
    protected void quit() {
        finishAffinity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        alarmActive = true;
    }

    private void startAlarm() {
//        if (alarm.getAlarmTonePath() != "") {
//            mediaPlayer = new MediaPlayer();
//            if (alarm.IsVibrate()) {
//                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//                long[] pattern = {1000, 200, 200, 200};
//                vibrator.vibrate(pattern, 0);
//            }
//            try {
//                mediaPlayer.setVolume(1.0f, 1.0f);
//                mediaPlayer.setDataSource(this, Uri.parse(alarm.getAlarmTonePath()));
//                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
//                mediaPlayer.setLooping(true);
//                mediaPlayer.prepare();
//                mediaPlayer.start();
//
//            } catch (Exception e) {
//                mediaPlayer.release();
//                alarmActive = false;
//            }
//        }

    }

    /**
     * 禁止返回取消闹钟
     */
    @Override
    public void onBackPressed() {
        if (!alarmActive)
            super.onBackPressed();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
//    @Override
//    protected void onPause() {
//        super.onPause();
//        StaticWakeLock.lockOff(this);
//    }

    protected void ReleaseRelease() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (vibrator != null)
                vibrator.cancel();
        } catch (Exception e) {

        }
        try {
            mediaPlayer.stop();
        } catch (Exception e) {

        }
        try {
            mediaPlayer.release();
        } catch (Exception e) {

        }
        super.onDestroy();
    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {

    }
}
