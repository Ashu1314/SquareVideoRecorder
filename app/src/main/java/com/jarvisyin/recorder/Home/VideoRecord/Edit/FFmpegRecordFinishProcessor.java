package com.jarvisyin.recorder.Home.VideoRecord.Edit;

import com.jarvisyin.recorder.Common.Utils.JYLog;
import com.jarvisyin.recorder.Home.VideoRecord.BlockInfo;
import com.jarvisyin.recorder.Home.VideoRecord.Common.Utils.FFmpeg.VShopFFmpeg;
import com.jarvisyin.recorder.Home.VideoRecord.VideoRecordActivity;


import org.xutils.common.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by jarvisyin on 16/12/9.
 */
public class FFmpegRecordFinishProcessor {
    private final static String TAG = "VShopVideo FFmpegRecordFinishProcessor";

    private final VideoRecordActivity mContext;
    private File mVideoListFile, mAudioListFile;

    public FFmpegRecordFinishProcessor(VideoRecordActivity context) {
        mContext = context;
    }

    public void start() {
        createVideoList();
    }

    private void createVideoList() {
        FileOutputStream o = null;
        try {
            StringBuffer sb = new StringBuffer();

            List<BlockInfo> blockInfos = mContext.getBlockInfos();

            for (BlockInfo blockInfo : blockInfos) {
                sb.append("file '").append(blockInfo.getVideoFile()).append("'\n");
            }

            String content = sb.toString();
            mVideoListFile = new File(mContext.getCachePath(), "VideoList.txt");
            if (mVideoListFile.exists()) mVideoListFile.delete();

            o = new FileOutputStream(mVideoListFile);
            o.write(content.getBytes("UTF-8"));
            o.close();

            createAudioList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (o != null) {
                try {
                    o.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createAudioList() {
        FileOutputStream o = null;
        try {
            StringBuffer sb = new StringBuffer();

            List<BlockInfo> blockInfos = mContext.getBlockInfos();

            for (BlockInfo blockInfo : blockInfos) {
                sb.append("file '").append(blockInfo.getAudioFile()).append("'\n");
            }

            String content = sb.toString();
            mAudioListFile = new File(mContext.getCachePath(), "AudioList.txt");
            if (mAudioListFile.exists()) mAudioListFile.delete();

            o = new FileOutputStream(mAudioListFile);
            o.write(content.getBytes("UTF-8"));
            o.close();

            concatenateVideo();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (o != null) {
                try {
                    o.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void concatenateVideo() {
        String cmd = String.format(" -f concat -i %s -c copy %s", mVideoListFile.getPath(), mContext.getSourceVideoPath());

        JYLog.i(TAG, "cmd = %s", cmd);


        VShopFFmpeg.getInstance().execute(cmd, new VShopFFmpeg.Callback() {

            @Override
            public void onProgress(String message) {
                JYLog.d(TAG, "ffmpegCmd: onProgress");
            }

            @Override
            public void onFailure(String message) {
                JYLog.d(TAG, "ffmpegCmd: onFailure");
                FFmpegRecordFinishProcessor.this.onFailure(message);
            }

            @Override
            public void onSuccess(String message) {
                JYLog.d(TAG, "ffmpegCmd: onSuccess");
                concatenateAudio();
            }
        });
    }

    private void concatenateAudio() {
        String cmd = String.format(" -f concat -i %s -c copy %s", mAudioListFile.getPath(), mContext.getSourceAudioPath());

        JYLog.i(TAG, "cmd = %s", cmd);

        VShopFFmpeg.getInstance().execute(cmd, new VShopFFmpeg.Callback() {

            @Override
            public void onProgress(String message) {
                JYLog.d(TAG, "ffmpegCmd: onProgress");
            }

            @Override
            public void onFailure(String message) {
                JYLog.d(TAG, "ffmpegCmd: onFailure");
                FFmpegRecordFinishProcessor.this.onFailure(message);
            }

            @Override
            public void onSuccess(String message) {
                JYLog.d(TAG, "ffmpegCmd: onSuccess");
                copy();
            }
        });
    }

    private void copy() {
        FileUtil.copy(mContext.getSourceAudioPath(), mContext.getProcessingAudioPath());
        FileUtil.copy(mContext.getSourceVideoPath(), mContext.getProcessingVideoPath());

        FFmpegRecordFinishProcessor.this.onSuccess(null);
    }

    public void onSuccess() {
    }

    public void onFailure(String message) {
    }
}
