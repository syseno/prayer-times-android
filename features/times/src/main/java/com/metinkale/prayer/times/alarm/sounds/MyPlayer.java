/*
 * Copyright (c) 2013-2019 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metinkale.prayer.times.alarm.sounds;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.widget.SeekBar;

import com.crashlytics.android.Crashlytics;
import com.metinkale.prayer.App;
import com.metinkale.prayer.times.alarm.Alarm;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class MyPlayer {

    //internal variabke
    private MediaPlayer mediaPlayer;

    //no lombok annotation due to special handling of volume
    private int volume = -1;
    //no lombok annotation due to existence of from(Sound sound)
    private List<Sound> sound;

    private SeekBar seekbar;
    private Alarm alarm;
    private OnCompletionListener onComplete;

    public interface OnCompletionListener {
        void onComplete();
    }

    private MyPlayer() {
    }

    public static MyPlayer from(Alarm alarm) {
        if (alarm.getSounds() == null || alarm.getSounds().isEmpty()) return null;
        MyPlayer myPlayer = new MyPlayer();
        myPlayer.sound = alarm.getSounds();
        myPlayer.volume(alarm.getVolume());
        return myPlayer;
    }

    public static MyPlayer from(Sound sound) {
        MyPlayer myPlayer = new MyPlayer();
        myPlayer.sound = Collections.singletonList(sound);
        return myPlayer;
    }

    public MyPlayer volume(int volume) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (this.volume >= 0 && volume >= 0) {
                AudioManager am = (AudioManager) App.get().getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            } else {
                stop();
            }
        }

        this.volume = volume;
        return this;
    }

    private int getStreamType() {
        switch (volume) {
            case Alarm.VOLUME_MODE_ALARM:
                return AudioManager.STREAM_ALARM;
            case Alarm.VOLUME_MODE_RINGTONE:
                return AudioManager.STREAM_RING;
            case Alarm.VOLUME_MODE_NOTIFICATION:
                return AudioManager.STREAM_NOTIFICATION;
            case Alarm.VOLUME_MODE_MEDIA:
            default:
                return AudioManager.STREAM_MUSIC;
        }
    }


    public MyPlayer play() {
        Iterator<Sound> iter = sound.iterator();

        if (iter.hasNext()) {
            play(iter);
        }

        return this;
    }

    private void play(final Iterator<Sound> iter) {
        mediaPlayer = iter.next().createMediaPlayer(alarm);
        final AudioManager am = (AudioManager) App.get().getSystemService(Context.AUDIO_SERVICE);

        final int streamType;
        final int oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (volume < 0) {
            streamType = getStreamType();
        } else {
            streamType = AudioManager.STREAM_MUSIC;
            am.setStreamVolume(streamType, volume, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(getStreamType()).build());
        } else {
            mediaPlayer.setAudioStreamType(getStreamType());
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            if (volume > 0) {
                am.setStreamVolume(streamType, oldvol, 0);
            }

            if (iter.hasNext()) {
                play(iter);
            } else if (onComplete != null) {
                onComplete.onComplete();
            }

        });

        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
            setupSeekbar();
        } catch (IOException e) {
            Crashlytics.logException(e);
        }


    }

    private void setupSeekbar() {
        if (seekbar != null) {
            seekbar.setMax(mediaPlayer.getDuration());

            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            seekbar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            seekbar.setProgress(mediaPlayer.getCurrentPosition(), true);
                        } else {
                            seekbar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                        seekbar.postDelayed(this, 100);
                    }
                }
            }, 100);
        }
    }


    public void stop() {
        mediaPlayer.reset();
        mediaPlayer = null;
        if (onComplete != null) {
            onComplete.onComplete();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public MyPlayer seekbar(SeekBar seekbar) {
        this.seekbar = seekbar;
        return this;
    }

    public MyPlayer alarm(Alarm alarm) {
        this.alarm = alarm;
        return this;
    }

    public MyPlayer onComplete(OnCompletionListener onComplete) {
        this.onComplete = onComplete;
        return this;
    }
}
