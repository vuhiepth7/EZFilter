package cn.ezandroid.ezfilter.media.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音视频复用器
 * <p>
 * 将音频和视频合成mp4
 */
public class MediaMuxerWrapper {

    private String mOutputPath;
    private final MediaMuxer mMediaMuxer;
    private int mEncoderCount, mStartedCount;
    private volatile boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder;

    /**
     * @param outPath 视频输出路径
     * @throws IOException
     */
    public MediaMuxerWrapper(String outPath) throws IOException {
        mOutputPath = outPath;
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStartedCount = 0;
        mIsStarted = false;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public void prepare() throws IOException {
        if (mVideoEncoder != null) {
            mVideoEncoder.prepare();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
    }

    public void startRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.startRecording();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.startRecording();
        }
    }

    public void stopRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stopRecording();
        }
        mVideoEncoder = null;
        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
        }
        mAudioEncoder = null;
    }

    public synchronized boolean isRecording() {
        boolean audioRecording = true;
        if (mAudioEncoder != null) {
            audioRecording = mAudioEncoder.mIsCapturing;
        }
        boolean videoRecording = false;
        if (mVideoEncoder != null) {
            videoRecording = mVideoEncoder.mIsCapturing;
        }
        return audioRecording && videoRecording;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null) {
                throw new IllegalArgumentException("Audio encoder already added.");
            }
            mAudioEncoder = encoder;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    synchronized boolean start() {
        mStartedCount++;
        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
        }
        return mIsStarted;
    }

    synchronized void stop() {
        mStartedCount--;
        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
        }
    }

    synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        return mMediaMuxer.addTrack(format);
    }

    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }
}
