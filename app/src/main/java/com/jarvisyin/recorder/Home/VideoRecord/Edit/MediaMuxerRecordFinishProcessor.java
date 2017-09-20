package com.jarvisyin.recorder.Home.VideoRecord.Edit;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.jarvisyin.recorder.Common.Utils.JYLog;
import com.jarvisyin.recorder.Home.VideoRecord.BlockInfo;
import com.jarvisyin.recorder.Home.VideoRecord.VideoRecordActivity;

import org.xutils.common.util.FileUtil;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jarvisyin on 16/12/9.
 */
public class MediaMuxerRecordFinishProcessor {
    private final static String TAG = "VShopVideo MediaMuxerRecordFinishProcessor";

    private final VideoRecordActivity mContext;
    private final MMRFPHandler mMMRFPHandler;

    public MediaMuxerRecordFinishProcessor(VideoRecordActivity context) {
        mContext = context;
        mMMRFPHandler = new MMRFPHandler(this);
    }

    public void start() {

        new Thread() {
            @Override
            public void run() {
                try {
                    videoMuxer();
                    audioMuxer();
                    copy();
                    mMMRFPHandler.onSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    mMMRFPHandler.onFailure(e.getMessage());
                }
            }
        }.start();

    }

    private static class MMRFPHandler extends Handler {

        private static final int STATUS_SUCCESS = 1;
        private static final int STATUS_FAIL = 2;

        private WeakReference<MediaMuxerRecordFinishProcessor> wr;

        private MMRFPHandler(MediaMuxerRecordFinishProcessor processor) {
            wr = new WeakReference<>(processor);
        }

        public void onSuccess() {
            Message msg = obtainMessage();
            msg.what = STATUS_SUCCESS;
            sendMessage(msg);
        }

        public void onFailure(String message) {
            Message msg = obtainMessage();
            msg.what = STATUS_FAIL;
            msg.obj = message;
            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaMuxerRecordFinishProcessor processor = wr.get();

            if (processor == null) {
                return;
            }


            switch (msg.what) {
                case STATUS_SUCCESS:
                    processor.onSuccess();
                    break;

                case STATUS_FAIL:
                    if (msg.obj != null) {
                        processor.onFailure(msg.obj.toString());
                    }
                    break;
            }

        }
    }

    private void videoMuxer() throws Exception {

        List<BlockInfo> bockInfos = mContext.getBlockInfos();
        if (bockInfos == null || bockInfos.isEmpty()) {
            mMMRFPHandler.onFailure("BlockInfos is empty");
            return;
        }


        MediaMuxer muxer = new MediaMuxer(mContext.getSourceVideoPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int videoTrackIndex = -1;
        int offset = 100;
        ByteBuffer buffer = ByteBuffer.allocate(262144);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long time = 0;

        for (int j = 0; j < bockInfos.size(); j++) {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(bockInfos.get(j).getVideoFile().getPath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                JYLog.i(TAG, "video mine = " + mine);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("video/")) {
                    if (j == 0)
                        videoTrackIndex = muxer.addTrack(format);
                    extractor.selectTrack(i);
                    break;
                }
            }

            if (j == 0) muxer.start();

            long cur = 0;
            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, offset);
                if (bufferInfo.size < 0)
                    break;
                bufferInfo.offset = offset;
                bufferInfo.presentationTimeUs = cur = extractor.getSampleTime() + time;
                bufferInfo.flags = extractor.getSampleFlags();
                JYLog.i(TAG, "presentationTimeUs = %s,  flags = %s,  offset = %s", bufferInfo.presentationTimeUs, bufferInfo.flags, bufferInfo.offset);

                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }
            time = cur;
            extractor.release();

        }
        muxer.stop();
        muxer.release();
    }

    private void audioMuxer() throws Exception {

        List<BlockInfo> bockInfos = mContext.getBlockInfos();
        if (bockInfos == null || bockInfos.isEmpty()) {
            mMMRFPHandler.onFailure("BlockInfos is empty");
            return;
        }


        MediaMuxer muxer = new MediaMuxer(mContext.getSourceAudioPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int videoTrackIndex = -1;
        int offset = 100;
        ByteBuffer buffer = ByteBuffer.allocate(262144);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long time = 0;

        for (int j = 0; j < bockInfos.size(); j++) {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(bockInfos.get(j).getAudioFile().getPath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                JYLog.i(TAG, "audio mine = " + mine);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio/")) {
                    if (j == 0)
                        videoTrackIndex = muxer.addTrack(format);
                    extractor.selectTrack(i);
                    break;
                }
            }

            if (j == 0) muxer.start();

            long cur = 0;
            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, offset);
                if (bufferInfo.size < 0)
                    break;
                bufferInfo.offset = offset;
                bufferInfo.presentationTimeUs = cur = extractor.getSampleTime() + time;
                bufferInfo.flags = extractor.getSampleFlags();
                JYLog.i(TAG, "presentationTimeUs = %s,  flags = %s,  offset = %s", bufferInfo.presentationTimeUs, bufferInfo.flags, bufferInfo.offset);


                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }
            time = cur;
            extractor.release();

        }

        muxer.stop();
        muxer.release();
    }


    private void copy() {
        FileUtil.copy(mContext.getSourceAudioPath(), mContext.getProcessingAudioPath());
        FileUtil.copy(mContext.getSourceVideoPath(), mContext.getProcessingVideoPath());
    }

    public void onSuccess() {
    }

    public void onFailure(String message) {
    }
}
