package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

/**
 * User: Yarden
 * Date: Oct 19, 2009
 * Time: 10:48:47 PM
 */
public class TunerEngine extends Thread {

    static {
        System.loadLibrary("FFT");
    }

    private Context context;

    public native double processSampleData(byte[] sample, int sampleRate);

    private static final int[] OPT_SAMPLE_RATES = {11025, 8000, 22050, 44100};
    private static final int[] BUFFERSIZE_PER_SAMPLE_RATE = {8 * 1024, 4 * 1024, 16 * 1024, 32 * 1024};

    public double currentFrequency = 0.0;

    int SAMPLE_RATE = 8000;
    int READ_BUFFERSIZE = 4 * 1024;

    AudioRecord targetDataLine_;

    final Handler mHandler;
    Runnable callback;

    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    public TunerEngine(Handler mHandler, Runnable callback, Context context) {
        this.mHandler = mHandler;
        this.callback = callback;
        this.context = context;

        ActivityCompat.requestPermissions((Activity) context, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        initAudioRecord();

//        System.out.println("Path thing: " + System.getProperty("java.library.path"));
    }

    private void initAudioRecord() {
        int counter = 0;
        for (int sampleRate : OPT_SAMPLE_RATES) {
            initAudioRecord(sampleRate);
            if (targetDataLine_.getState() == AudioRecord.STATE_INITIALIZED) {
                SAMPLE_RATE = sampleRate;
                READ_BUFFERSIZE = BUFFERSIZE_PER_SAMPLE_RATE[counter];
                break;
            }
            counter++;
        }
    }

    private void initAudioRecord(int sampleRate) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        targetDataLine_ = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                sampleRate * 6
        );
    }

    byte[] bufferRead;
    //    long l;
    public void run(){       // fft

        targetDataLine_.startRecording();
        bufferRead = new byte[READ_BUFFERSIZE];
        int n = -1;
        while ( (n = targetDataLine_.read(bufferRead, 0,READ_BUFFERSIZE)) > 0 ) {
//            l = System.currentTimeMillis();
            currentFrequency = processSampleData(bufferRead,SAMPLE_RATE);
//            System.out.println("process time  = " + (System.currentTimeMillis() - l));
            if(currentFrequency > 0){
                mHandler.post(callback);
                try {
                    targetDataLine_.stop();
//                    Thread.sleep(20);
                    targetDataLine_.startRecording();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
        }
    }

    public void close(){
//        targetDataLine_.stop();
        targetDataLine_.release();
    }

}

