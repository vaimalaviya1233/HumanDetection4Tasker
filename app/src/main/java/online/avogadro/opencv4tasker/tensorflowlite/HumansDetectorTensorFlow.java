package online.avogadro.opencv4tasker.tensorflowlite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult;
import com.google.mediapipe.tasks.vision.core.RunningMode;

import online.avogadro.opencv4tasker.app.Util;

public class HumansDetectorTensorFlow {
    private ObjectDetector objectDetector;
    static final String TAG = "HumansDetectorTensorFlow";

    /**
     * Detect humans and return the highest score
     * @param path in the form of file:///{something} or content:///{something}
     * @return 0-100+, lower values are lower scores. '-1' is a failure
     */
    public static int detectHumans(Context context, String path) throws IOException {
        HumansDetectorTensorFlow htc = new HumansDetectorTensorFlow();
        htc.setup(context);
        return htc.detectPerson(context, path);
    }

    public void setup(Context ctx) throws IOException {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("lite-model_efficientdet_lite0_detection_metadata_1.tflite")
                .build();
        ObjectDetector.ObjectDetectorOptions detectorOptions =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setMaxResults(5)
                        .setScoreThreshold(0.5f)
                        .setRunningMode(RunningMode.IMAGE)
                        .build();
        objectDetector = ObjectDetector.createFromOptions(ctx, detectorOptions);
    }

    public int detectPerson(Context ctx, String imagePath) {
        String newPath = null;
        try {
            newPath = Util.contentToFile(ctx, imagePath);
            // Load image from disk
            Bitmap bitmap = BitmapFactory.decodeFile(newPath);

            // Convert bitmap to MPImage
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

            // Run inference
            ObjectDetectorResult result = objectDetector.detect(mpImage);
            List<Detection> detections = result.detections();

            // Process results
            float highestScore = 0f;
            for (Detection detection : detections) {
                if (detection.categories().get(0).categoryName().equals("person")) {
                    float score = detection.categories().get(0).score();
                    if (score > highestScore) {
                        highestScore = score;
                    }
                }
            }

            // Convert the highest score to an integer in the range 0-100
            return Math.round(highestScore * 100);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse file name " + newPath, e);
            return -1;
        } finally {
            if (newPath != null && !newPath.equals(imagePath))
                new File(newPath).delete();
        }
    }
}
