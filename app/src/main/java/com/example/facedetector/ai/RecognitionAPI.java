package com.example.facedetector.ai;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecognitionAPI {

    private static final int IMAGE_HEIGHT = 160;
    private static final int IMAGE_WIDTH = 160;
    private static final int NUM_CHANNELS = 3;
    private static final int NUM_BYTES_PER_CHANNEL = 4;
    private static final int EMBEDDING_SIZE = 128;

    private int[] intValues;

    private ByteBuffer imgDetected;
    private ByteBuffer imgRecognised;

    private Interpreter tfLiteDetection;
    private Interpreter tfLiteRecognition;

    private RecognitionAPI() {}

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     */
    public static RecognitionAPI create(
            final AssetManager assetManager) {

        final RecognitionAPI d = new RecognitionAPI();

        try {
            d.tfLiteDetection = new Interpreter(loadModelFile(assetManager, "facenet_128.tflite"));
            d.tfLiteRecognition = new Interpreter(loadModelFile(assetManager, "fcnn_20210223_090351.tflite"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.imgDetected = ByteBuffer.allocateDirect(IMAGE_HEIGHT
                        * IMAGE_WIDTH
                        * NUM_CHANNELS
                        * NUM_BYTES_PER_CHANNEL);
        d.imgDetected.order(ByteOrder.nativeOrder());
        d.imgRecognised = ByteBuffer.allocateDirect(EMBEDDING_SIZE
                        * NUM_BYTES_PER_CHANNEL);
        d.imgRecognised.order(ByteOrder.nativeOrder());
        d.intValues = new int[IMAGE_HEIGHT * IMAGE_WIDTH];

        return d;
    }



    public String recognizeImage(Bitmap bitmap) {

        bitmap = resizedBitmap(bitmap);
        convertBitmapToByteBuffer(bitmap);

        float[][] embeddings = new float[1][EMBEDDING_SIZE];
        tfLiteDetection.run(imgDetected, embeddings);

        convertEmbeddingToByteBuffer(embeddings);
        int out_size = tfLiteRecognition.getOutputTensor(0).shape()[1];
        float[][] out = new float[1][out_size];
        tfLiteRecognition.run(imgRecognised, out);

        float result = -1;
        int index = -1;
        for (int i = 0; i < out[0].length; i++) {
            Log.d("TAG", "recognizeImage:" + i + " out: " + out[0][i]);
            if (result< out[0][i]){
                result = out[0][i];
                index = i;
            }
        }

        return "UserID: " + index + " confidence: " + result;
    }

    private Bitmap resizedBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgDetected == null) {
            return;
        }
        imgDetected.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < IMAGE_HEIGHT; ++i) {
            for (int j = 0; j < IMAGE_WIDTH; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    private void addPixelValue(int pixelValue) {
        imgDetected.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
        imgDetected.putFloat(((pixelValue >> 8) & 0xFF) / 255.0f);
        imgDetected.putFloat((pixelValue & 0xFF) / 255.0f);
    }

    private void convertEmbeddingToByteBuffer(float[][] embeddings) {
        if (imgRecognised == null) {
            return;
        }
        imgRecognised.rewind();
        for (int i = 0; i < embeddings[0].length; ++i) {
            imgRecognised.putFloat(embeddings[0][i]);
        }
    }
}
