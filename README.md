# SquareVideoRecorder 正方形 短视频 分段 录制

## 主要流程说明：

### 方形 视频 预览 实现流程：
Camera 获取数据 ---> OpenGL ES 切割成正方形 ---> SurfaceView显示

### 方形 视频 编码 封装 保存 实现流程：
Camera 获取数据 ---> OpenGL ES 切割成正方形 ---> MediaCodec编码 ---> MediaMuxer封装，保存本地

### n段视频连接 实现方式：
ffmpeg  concat 命令

### 添加字幕 实现方式：
OpenGL ES纹理贴图

### 音乐混合（背景音乐和录音混合）实现方式：
ffmpeg  filter_complex 命令