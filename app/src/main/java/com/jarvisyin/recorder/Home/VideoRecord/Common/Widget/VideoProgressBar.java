package com.jarvisyin.recorder.Home.VideoRecord.Common.Widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.jarvisyin.recorder.Common.Utils.DisplayUtils;
import com.jarvisyin.recorder.Home.VideoRecord.BlockInfo;
import com.jarvisyin.recorder.Home.VideoRecord.VideoRecordActivity;
import com.jarvisyin.recorder.R;

import java.util.List;


/**
 * Created by jarvisyin on 16/4/8.
 */
public class VideoProgressBar extends View {

    private String TAG = "VideoProgressBar";

    private int yellowColor;
    private int greyColor;
    private int whiteColor;

    private int dp1;

    private Paint paint = new Paint();


    public VideoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        yellowColor = getContext().getResources().getColor(R.color.yellow);
        greyColor = 0xff909090;
        whiteColor = 0xffffffff;

        paint.setAntiAlias(true); //抗锯齿
        dp1 = DisplayUtils.dip2px(getContext(), 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(greyColor);

        if (!(getContext() instanceof VideoRecordActivity)) {
            return;
        }
        VideoRecordActivity mContext = (VideoRecordActivity) getContext();

        final float width = canvas.getWidth();
        final float height = canvas.getHeight();

        final float whiteLinePercent = mContext.lessTimeSpan * 1.0f / mContext.wholeTimeSpan;
        paint.setStrokeWidth(height);//线的宽度
        paint.setColor(whiteColor);
        canvas.drawLine(width * whiteLinePercent - dp1, height / 2, width * whiteLinePercent, height / 2, paint);

        List<BlockInfo> blockInfos = mContext.getBlockInfos();
        if (blockInfos == null || blockInfos.isEmpty()) return;

        long sum = 0L;
        float p1, p2;
        paint.setColor(yellowColor);
        for (BlockInfo block : blockInfos) {

            p1 = sum * 1.0f / mContext.wholeTimeSpan;

            sum = sum + (block.getDuration());
            p2 = sum * 1.0f / mContext.wholeTimeSpan;

            Log.i(TAG, "p1 = " + p1 + " p2 = " + p2 + " sum = " + sum + "  block.getTimeSpan() = " + block.getDuration());

            canvas.drawLine(width * p1, height / 2, width * p2 - dp1 * 1.2f, height / 2, paint);
        }
    }

}
