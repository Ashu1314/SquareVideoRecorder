package com.jarvisyin.recorder.Home.VideoRecord.Edit.Music;

import android.content.Context;
import android.util.Log;

import com.jarvisyin.recorder.Common.Utils.Trinea.FileUtils;
import com.jarvisyin.recorder.Home.VideoRecord.Common.Utils.FFmpeg.VShopFFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by jarvisyin on 16/12/7.
 */
public class MusicTransfer extends Thread {
    private static final String TAG = "MusicProcessor";

    private Context mContext;
    private File mCacheFile;
    private File mOutFile;
    private int mSourseId;
    private String xm4a, xmpg, x3mpg, x3m4a;

    private Callback mCallback;

    public MusicTransfer(Context context,
                         File cacheFile,
                         int sourseId,
                         File outFile) {
        mContext = context;
        mCacheFile = new File(cacheFile, UUID.randomUUID().toString().replace("-", ""));
        mCacheFile.mkdirs();
        mSourseId = sourseId;
        mOutFile = outFile;

        xm4a = new File(mCacheFile, "x.m4a").getPath();
        xmpg = new File(mCacheFile, "x.mpg").getPath();
        x3mpg = new File(mCacheFile, "x3.mpg").getPath();
        x3m4a = new File(mCacheFile, "x3.m4a").getPath();

    }

    public void run() {
        fileCopy();
    }

    /**
     * 1.将raw文件复制到本地 (在Cache目录中做)
     */
    private void fileCopy() {
        try {
            InputStream inputStream = mContext.getResources().openRawResource(mSourseId);
            OutputStream outputStream = new FileOutputStream(xm4a);

            byte[] bytes = new byte[1024 * 512];//512kb 缓存
            int x;
            while ((x = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, x);
            }
            inputStream.close();
            outputStream.close();
            bytes = null;

            changeM4AtoMPG();

        } catch (Exception e) {
            e.printStackTrace();
            if (mCallback != null) mCallback.onFailure(e.getMessage());
        }
    }


    /**
     * 2.将所有m4a转成mpg
     */
    private void changeM4AtoMPG() {
        StringBuffer cmd = new StringBuffer();
        cmd.append(" -i ");
        cmd.append(xm4a);
        cmd.append(" -f mpeg ");
        cmd.append(xmpg);

        VShopFFmpeg.getInstance().execute(cmd.toString(), new VShopFFmpeg.Callback() {

            @Override
            public void onProgress(String message) {
                if (mCallback != null) mCallback.onProgress(message);
            }

            @Override
            public void onFailure(String message) {
                if (mCallback != null) mCallback.onFailure(message);
            }

            @Override
            public void onSuccess(String message) {
                joinMPG();
            }
        });
    }

    /**
     * 3.将所有mpg连接成一个mpg
     *
     * @throws Exception
     */
    private void joinMPG() {
        try {
            FileUtils.join(x3mpg, new String[]{xmpg, xmpg, xmpg});
            changeMPGtoM4A();
        } catch (Exception e) {
            e.printStackTrace();
            if (mCallback != null) mCallback.onFailure(e.getMessage());
        }
    }

    /**
     * 4.将视频文件转成ACC (保存在MusicFile中)
     */
    private void changeMPGtoM4A() {

        StringBuffer cmd = new StringBuffer();
        cmd.append(" -i ");
        cmd.append(x3mpg);
        cmd.append(" -f  mp4 -strict -2 ");
        cmd.append(x3m4a);

        VShopFFmpeg.getInstance().execute(cmd.toString(), new VShopFFmpeg.Callback() {

            @Override
            public void onProgress(String message) {
                Log.i(TAG, "message = " + message);
            }

            @Override
            public void onFailure(String message) {
                if (mCallback != null) mCallback.onFailure(message);
            }

            @Override
            public void onSuccess(String message) {
                moveACCdeleteCache();
            }
        });
    }

    /**
     * 5.将生成的acc移到目的地址
     */
    private void moveACCdeleteCache() {
        new File(x3m4a).renameTo(mOutFile);
        if (mCallback != null) mCallback.onSuccess();
    }

    public interface Callback {
        void onSuccess();

        void onProgress(String message);

        void onFailure(String message);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
