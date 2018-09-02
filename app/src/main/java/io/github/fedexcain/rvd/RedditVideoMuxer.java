package io.github.fedexcain.rvd;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RedditVideoMuxer {

    static void muxVideoAudio(String outputFileName, String videoURL, String audioURL) throws IOException {
        MediaExtractor extractorVideo = new MediaExtractor();
        MediaExtractor extractorAudio = new MediaExtractor();

        extractorAudio.setDataSource(audioURL);
        extractorVideo.setDataSource(videoURL);
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), outputFileName + ".mp4");
        for (int number = 2; outputFile.exists(); number++) {
            outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), outputFileName + " (" + Integer.toString(number) + ").mp4");
        }
        outputFile.createNewFile();

        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        extractorVideo.selectTrack(0);
        extractorAudio.selectTrack(0);

        MediaFormat videoFormat = extractorVideo.getTrackFormat(0);
        MediaFormat audioFormat = extractorAudio.getTrackFormat(0);

        int audioTrackIndex = muxer.addTrack(audioFormat);
        int videoTrackIndex = muxer.addTrack(videoFormat);

        boolean sawEOS = false;
        boolean sawAudioEOS = false;
        //TODO Make Buffer the write size
        int bufferSize = 20000000;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        int offset = 100;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        muxer.start();

        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractorVideo.readSampleData(dstBuf, offset);
            if (bufferInfo.size < 0) {
                sawEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractorVideo.getSampleTime();
                bufferInfo.flags = extractorVideo.getSampleFlags();
                muxer.writeSampleData(videoTrackIndex, dstBuf, bufferInfo);
                extractorVideo.advance();
            }
        }
        ByteBuffer audioBuf = ByteBuffer.allocate(bufferSize);
        while (!sawAudioEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractorAudio.readSampleData(audioBuf, offset);
            if (bufferInfo.size < 0) {
                sawAudioEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractorAudio.getSampleTime();
                bufferInfo.flags = extractorAudio.getSampleFlags();
                muxer.writeSampleData(audioTrackIndex, audioBuf, bufferInfo);
                extractorAudio.advance();
            }
        }
        muxer.stop();
        muxer.release();
    }
}
