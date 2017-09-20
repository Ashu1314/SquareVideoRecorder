# SquareVideoRecorder 正方形 短视频 分段 录制<br/>

## 主要流程说明：
### 1.方形 视频 预览 实现流程：
Camera 获取数据 ---> OpenGL ES 切割成正方形 ---> SurfaceView显示
<br/><br/>
### 2.方形 视频 编码 封装 保存 实现流程：
Camera 获取数据 ---> OpenGL ES 切割成正方形 ---> MediaCodec编码 ---> MediaMuxer封装，保存本地
<br/><br/>
### 3.n段视频连接 实现方式：
Android API MediaMuxer
<br/><br/>
### 4.添加字幕 实现方式：
OpenGL ES纹理贴图
<br/><br/>
### 5.音乐混合（背景音乐和录音混合）实现方式：
ffmpeg  filter_complex 命令
<br/><br/>
PS：每一段音频数据在个别设备中会比视频数据少 0.5s ，造成最终的音频比视频短，不能同步。原因未知，问题没解决
<br/>
<br/>
## 效果展示：
<br/>

![视频拍摄](https://github.com/jarvisyin/SquareVideoRecorder/blob/master/demo_picture/4.png "视频拍摄")
![视频编辑](https://github.com/jarvisyin/SquareVideoRecorder/blob/master/demo_picture/5.png "视频编辑")
![添加字幕](https://github.com/jarvisyin/SquareVideoRecorder/blob/master/demo_picture/6.png "添加字幕") 
![添加音乐](https://github.com/jarvisyin/SquareVideoRecorder/blob/master/demo_picture/7.png "添加音乐") 
