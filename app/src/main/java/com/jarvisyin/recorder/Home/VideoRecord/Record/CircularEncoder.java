package com.jarvisyin.recorder.Home.VideoRecord.Record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;


import com.jarvisyin.recorder.Common.Utils.JYLog;
import com.jarvisyin.recorder.Home.VideoRecord.BlockInfo;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by jarvisyin on 16/11/17.
 */
public class CircularEncoder {

    public static final String TAG = "VShopVideo CircularEncoder";


    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // sync frame every second

    private final BlockInfo mBlockInfo;
    private final Surface mInputSurface;

    private MediaCodec mEncoder;
    private MediaFormat mFormat;
    private final MediaMuxer mMuxer;
    //private final FileOutputStream mOutputStream;
    private final byte[] bytes = new byte[1024 * 256];
    private final EncoderThread mEncoderThread;

    public interface Callback {
        void fileSaveComplete(int status, int frameCount);

        void bufferStatus(long totalTimeMesec, int frameNum);
    }

    public CircularEncoder(int width, int height, int bitRate, int frameRate, Callback cb, BlockInfo blockInfo) throws Exception {
        mBlockInfo = blockInfo;

        mFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        //此处耗时最多,一般用时为0.15s
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        //TODO try to reset encoder;

        mEncoderThread = new EncoderThread(mEncoder, cb);
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();

        mMuxer = new MediaMuxer(blockInfo.getVideoFile().getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        //mOutputStream = new FileOutputStream(blockInfo.getVideoFile().getPath() + "stream_data");
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    public void shutdown() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN));
        try {
            mEncoderThread.join();
        } catch (Exception e) {
        }

        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    public void frameAvailableSoon() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON));
    }

    public void saveVideo() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SAVE_VIDEO));
    }

    private class EncoderThread extends Thread {
        private MediaCodec mEncoder;
        private MediaFormat mEncodedFormat;
        private MediaCodec.BufferInfo mBufferInfo;

        private EncoderHandler mHandler;
        private Callback mCallback;
        private int mFrameNum;
        private int mVideoTrack;

        private final Object mLock = new Object();
        private volatile boolean mReady = false;

        public EncoderThread(MediaCodec mediaCodec, Callback callback) {

            mEncoder = mediaCodec;
            mCallback = callback;
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new EncoderHandler(this);
            synchronized (mLock) {
                mReady = true;
                mLock.notify();
            }

            Looper.loop();

            synchronized (mLock) {
                mReady = false;
                mHandler = null;
            }
        }

        public void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public EncoderHandler getHandler() {
            synchronized (mLock) {
                if (!mReady) {
                    throw new RuntimeException("not ready");
                }
            }
            return mHandler;
        }

        public void drainEncoder() {
            final int TIMEOUT_USEC = 0;

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mEncodedFormat = mEncoder.getOutputFormat();
                    mVideoTrack = mMuxer.addTrack(mEncodedFormat);
                    mMuxer.start();
                    JYLog.i(TAG,mEncodedFormat.toString());
                } else if (encoderStatus < 0) {

                } else {
                    ByteBuffer encoderData = encoderOutputBuffers[encoderStatus];
                    if (encoderData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        encoderData.position(mBufferInfo.offset);
                        encoderData.limit(mBufferInfo.offset + mBufferInfo.size);

                        JYLog.i(TAG, "offset = %5s , size = %5s , presentationTimeUs = %10s , current_time = %10s , flags = %3s , position = %10s , limit = %5s , size = %5s",
                                mBufferInfo.offset,
                                mBufferInfo.size,
                                mBufferInfo.presentationTimeUs,
                                android.view.animation.AnimationUtils.currentAnimationTimeMillis(),
                                mBufferInfo.flags,
                                encoderData.position(),
                                encoderData.limit(),
                                encoderData.limit() - encoderData.position());

                        mMuxer.writeSampleData(mVideoTrack, encoderData, mBufferInfo);
                        encoderData.get(bytes, 0, mBufferInfo.size);

                        /*try{
                            mOutputStream.write(bytes,0,mBufferInfo.size);
                        }catch (Exception e){

                        }*/

                        if (mBlockInfo.getStartTime() == 0) {
                            mBlockInfo.setStartTime(mBufferInfo.presentationTimeUs);
                        }
                        mBlockInfo.setStopTime(mBufferInfo.presentationTimeUs);
                    }

                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                }
            }
        }

        void frameAvailableSoon() {
            drainEncoder();


            mFrameNum++;
            mCallback.bufferStatus(mBlockInfo.getDuration(), mFrameNum);
        }

        void saveVideo() {
            int result = 0;
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
            }

            /*try {
                mOutputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }*/

            mBlockInfo.setFrameCount(mFrameNum);
            mCallback.fileSaveComplete(result, mFrameNum);
        }

        void shutdown() {
            Looper.myLooper().quit();
        }

        private class EncoderHandler extends Handler {
            public static final int MSG_FRAME_AVAILABLE_SOON = 1;
            public static final int MSG_SAVE_VIDEO = 2;
            public static final int MSG_SHUTDOWN = 3;

            private WeakReference<EncoderThread> mWeakEncoderThread;

            public EncoderHandler(EncoderThread et) {
                mWeakEncoderThread = new WeakReference<>(et);
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                EncoderThread encoderThread = mWeakEncoderThread.get();
                switch (what) {
                    case MSG_FRAME_AVAILABLE_SOON:
                        encoderThread.frameAvailableSoon();
                        break;
                    case MSG_SHUTDOWN:
                        encoderThread.shutdown();
                        break;
                    case MSG_SAVE_VIDEO:
                        encoderThread.saveVideo();
                        break;
                }
            }
        }
    }
}