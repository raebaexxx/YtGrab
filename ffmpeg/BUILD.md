# Сборка slim ffmpeg для приложения

Встроенный в APK ffmpeg/ffprobe — минимальная статическая сборка FFmpeg 8.1.2
(LGPL 2.1, без внешних кодировщиков x264/x265/AV1/VP9 и прочих библиотек
termux-сборки). Приложению нужны только: склейка потоков (`-c copy`),
метаданные и обложки.

Конфигурация (одинакова для десктопа-валидации и arm64):

```bash
./configure \
  --enable-cross-compile --target-os=android --arch=aarch64 --cpu=armv8-a \
  --cc=$TOOLCHAIN/bin/aarch64-linux-android31-clang \
  --cxx=$TOOLCHAIN/bin/aarch64-linux-android31-clang++ \
  --ar=$TOOLCHAIN/bin/llvm-ar --nm=$TOOLCHAIN/bin/llvm-nm \
  --ranlib=$TOOLCHAIN/bin/llvm-ranlib --strip=$TOOLCHAIN/bin/llvm-strip \
  --disable-everything \
  --enable-ffmpeg --enable-ffprobe --disable-ffplay \
  --disable-avdevice --disable-autodetect \
  --enable-small --disable-debug --disable-doc \
  --enable-zlib \
  --enable-protocol=file \
  --enable-demuxer=mov,matroska,mpegts,concat,image2,aac,mp3,flac,ogg,wav,ffmetadata \
  --enable-muxer=mov,mp4,ipod,matroska,webm,mpegts,image2,adts,ogg,mp3 \
  --enable-decoder=mjpeg,png,webp,gif \
  --enable-encoder=mjpeg,png \
  --enable-parser=aac,h264,hevc,mpeg4video,vp8,vp9,av1,opus,vorbis,mpegaudio,mjpeg,png,ac3,flac \
  --enable-bsf=aac_adtstoasc,vorbis_comment,dump_extradata,extract_extradata,mjpeg2jpeg \
  --enable-filter=null,copy,format,aformat,scale,crop \
  --disable-iconv --disable-bzlib --disable-lzma --disable-sdl2
# для десктопной (x86_64) валидации: убрать все cross-флаги, добавить --disable-x86asm
make -j$(nproc)
```

Бинарники кладутся в `src/main/jniLibs/arm64-v8a/` как `libffmpeg.so` и
`libffprobe.so` (имена обязательны: jniLibs принимает только `lib*.so`, а
youtubedl-android передаёт `libffmpeg.so` в `--ffmpeg-location`).

Проверено: yt-dlp 2026.09 с этой сборкой — merge mp4/webm, embed-metadata,
конвертация webp-обложки, embed-thumbnail (десктопная сборка той же конфигурации).
