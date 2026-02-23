package online.avogadro.opencv4tasker.openrouter;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import online.avogadro.opencv4tasker.ai.AIImageAnalyzer;
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper;
import online.avogadro.opencv4tasker.app.Util;

public class HumansDetectorOpenRouter implements AIImageAnalyzer {

    private String API_KEY = "YOUR_API_KEY_HERE";
    private String MODEL_NAME = SharedPreferencesHelper.DEFAULT_OPENROUTER_MODEL;
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final String PROMPT_SYSTEM =
            "The user will be providing images taken from cheap security cameras, these images might be taken during the day or the night and the angle may vary. Images are usually taken top-down, during the night images may be blurry due to person's movements. Please reply him with a single keyword in the first line and a brief explanation of your choice in the second line, chosen among these:\n" +
            "* HUMAN: an human or a part of an human (usually on the border of the image) is visible in the frame. The human may be seen from above since the camera is usually mounted on an high position\n" +
            "* SPIDER: no humans are visible but a spider is near the camera\n" +
            "* CAT: if it's an animal or a cat, it may be a cat walking away from the camera or walking toward the camera\n" +
            "* NONE: neither an human nor a spider are in frame\n" +
            "* UNCERTAIN: you were unable to tell in which of the above categories the image might fit. Use this response if you are not totally sure that the answer is one of the above\n" +
            "Ignore any shadows";

    static final String TAG = "HumansDetectorOpenRouter";

    public String lastResponse = null;
    public Exception lastException = null;
    public String lastHttpResponse = null;

    public static int detectHumans(Context context, String path) throws IOException {
        HumansDetectorOpenRouter detector = new HumansDetectorOpenRouter();
        detector.setup(context);
        return detector.detectPerson(context, path);
    }

    @Override
    public void setup(Context ctx) throws IOException {
        API_KEY = SharedPreferencesHelper.get(ctx, SharedPreferencesHelper.OPENROUTER_API_KEY);
        MODEL_NAME = SharedPreferencesHelper.get(ctx, SharedPreferencesHelper.OPENROUTER_MODEL);
        if (MODEL_NAME == null || MODEL_NAME.isEmpty()) {
            MODEL_NAME = SharedPreferencesHelper.DEFAULT_OPENROUTER_MODEL;
        }
    }

    public int detectPerson(Context ctx, String imagePath) {
        lastResponse = null;
        lastException = null;
        String newPath = null;
        try {
            newPath = Util.contentToFile(ctx, imagePath);
            String openRouterResponse = analyzeImage(PROMPT_SYSTEM, null, newPath);
            lastResponse = openRouterResponse;
            String[] res = openRouterResponse.split("\\r?\\n");
            if (res[0].trim().startsWith("HUMAN"))
                return 100;
            else if (res[0].trim().startsWith("NONE"))
                return 0;
            else if (res[0].trim().startsWith("SPIDER"))
                return 0;
            else if (res[0].trim().startsWith("CAT"))
                return 0;
            else if (res[0].trim().startsWith("UNCERTAIN"))
                return 30;
            else
                return -1;

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to examine file " + newPath, e);
            lastException = e;
            return -1;
        } finally {
            if (newPath != null && !newPath.equals(imagePath))
                new File(newPath).delete();
        }
    }

    @Override
    public String getLastResponse() {
        return lastResponse;
    }

    @Override
    public String getLastError() {
        String res = lastHttpResponse;
        if (lastException != null)
            res += "\n" + lastException;
        return res;
    }

    @Override
    public String analyzeImage(String systemPrompt, String userPrompt, String imagePath) throws IOException, JSONException {
        lastHttpResponse = null;
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        String imageBase64 = encodeImageToBase64(imagePath);
        String imageType = imagePath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        String imageDataUrl = "data:" + imageType + ";base64," + imageBase64;

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", MODEL_NAME);
        jsonBody.put("max_tokens", 1000);
        jsonBody.put("temperature", 0.0);

        JSONArray messages = new JSONArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        
        JSONArray contentArray = new JSONArray();
        
        if (userPrompt != null && !userPrompt.isEmpty()) {
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", userPrompt);
            contentArray.put(textContent);
        }
        
        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", imageDataUrl);
        imageContent.put("image_url", imageUrl);
        contentArray.put(imageContent);

        userMessage.put("content", contentArray);
        messages.put(userMessage);

        jsonBody.put("messages", messages);

        OutputStream os = connection.getOutputStream();
        os.write(jsonBody.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            lastHttpResponse = response.toString();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getString("content");
            }
            throw new IOException("No choices found in response: " + response.toString());
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = errorReader.readLine()) != null) {
                response.append(inputLine);
            }
            errorReader.close();
            String r = response.toString();
            lastHttpResponse = r;
            throw new IOException("Error " + responseCode + " " + r);
        }
    }

    private String encodeImageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = 0;
            int offset = 0;
            while (offset < bytes.length && (bytesRead = fis.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += bytesRead;
            }
            if (offset != bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}