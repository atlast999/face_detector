package com.example.facedetector.ai;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.facedetector.data.DataHelper;
import com.example.facedetector.data.UserFeatureData;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<Integer, String> labelMap;
    private UserFeatureData userFeatureData;

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

    private static String loadJsonFile(AssetManager assetManager, String fileName) throws IOException{
        InputStream inputStream = assetManager.open(fileName);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        return new String(buffer, "UTF-8");
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

            String json = loadJsonFile(assetManager, "label_map.json");
            JSONObject jsonObject = new JSONObject(json);
            List<String> keys = Lists.newArrayList(jsonObject.keys());
            d.labelMap = new HashMap<>();
            for(String key: keys){
                d.labelMap.put(Integer.parseInt(key), jsonObject.getString(key));
            }

            json = loadJsonFile(assetManager, "user_fea.json");
            Gson gson = new Gson();
            d.userFeatureData = gson.fromJson(json, UserFeatureData.class);
            DataHelper.Companion.checkData(d.userFeatureData);
            DataHelper.Companion.main();
            json = loadJsonFile(assetManager, "user_info.json");

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
        /*
         *Lưu ảnh từ bitmap vào imgDetected để detect
         * */
        convertBitmapToByteBuffer(bitmap);

        float[][] embeddings = new float[1][EMBEDDING_SIZE];
        /*
         *Trả về vector của mặt lưu vào embeddings
         * */
        tfLiteDetection.run(imgDetected, embeddings);

        /*
         *convert embedding sang imgRecognised để làm input
         * */
        convertEmbeddingToByteBuffer(embeddings);
        int out_size = tfLiteRecognition.getOutputTensor(0).shape()[1];
        float[][] out = new float[1][out_size];
        /*
         *Trả về out[0]: độ giống so với các user trong model
         * */
        tfLiteRecognition.run(imgRecognised, out);

        float result = -1;
        int index = -1;
        /*
         *Lấy user có độ giống cao nhất
         * Thông tin user trong file json ở assets
         * */
        for (int i = 0; i < out[0].length; i++) {
//            Log.d("TAG", "recognizeImage:" + i + " out: " + out[0][i]);
            if (result< out[0][i]){
                result = out[0][i];
                index = i;
            }
        }

        String userFeatureId = labelMap.get(index);
//        return "User: " + userFeatureId + " confidence: " + result;
        if (DataHelper.Companion.confirmUser(embeddings[0], userFeatureId, userFeatureData)){
            return "User: " + userFeatureId + " confidence: " + result;
        }
        return String.valueOf(result);

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
