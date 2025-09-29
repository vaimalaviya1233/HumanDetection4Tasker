package online.avogadro.opencv4tasker.gemini;

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

public class HumansDetectorGemini implements AIImageAnalyzer {

    private String API_KEY = "YOUR_API_KEY_HERE";

    private static final String MODEL = "gemini-2.5-flash";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent";
    
    private static final String PROMPT_SYSTEM = 
            "The user will be providing images taken from cheap security cameras, these images might be taken during the day or the night and the angle may vary. Images are usually taken top-down, during the night images may be blurry due to person's movements. Please reply him with a single keyword in the first line and a brief explanation of your choice in the second line, chosen among these:\n" +
            "* HUMAN: an human or a part of an human (usually on the border of the image) is visible in the frame. The human may be seen from above since the camera is usually mounted on an high position\n" +
            "* SPIDER: no humans are visible but a spider is near the camera\n" +
            "* CAT: if it's an animal or a cat, it may be a cat walking away from the camera or walking toward the camera\n" +
            "* NONE: neither an human nor a spider are in frame\n" +
            "* UNCERTAIN: you were unable to tell in which of the above categories the image might fit. Use this response if you are not totally sure that the answer is one of the above\n" +
            "Ignore any shadows";

    static final String TAG = "HumansDetectorGemini";
    public static final String GEMINI_MODEL = "gemini-pro-vision";

    private static final String CONTENT_TYPE_JPG = "image/jpeg";
    private static final String CONTENT_TYPE_PNG = "image/png";

    public String lastResponse = null;
    public Exception lastException = null;
    public String lastHttpResponse = null;

    /**
     * Detect humans and return the highest score
     * @param path in the form of file:///{something} or content:///{something}
     * @return 0-100+, lower values are lower scores. '-1' is a failure
     */
    public static int detectHumans(Context context, String path) throws IOException {
        HumansDetectorGemini htg = new HumansDetectorGemini();
        htg.setup(context);
        return htg.detectPerson(context, path);
    }

    @Override
    public void setup(Context ctx) throws IOException {
        API_KEY = SharedPreferencesHelper.get(ctx, SharedPreferencesHelper.GEMINI_API_KEY);
    }

    public int detectPerson(Context ctx, String imagePath) {
        lastResponse = null;
        lastException = null;
        String newPath = null;
        try {
            newPath = Util.contentToFile(ctx, imagePath);
            String geminiResponse = analyzeImage(PROMPT_SYSTEM, null, newPath);
            lastResponse = geminiResponse;
            String[] res = geminiResponse.split("\\r?\\n");
            if (res[0].trim().equals("HUMAN"))
                return 100;
            else if (res[0].trim().equals("NONE"))
                return 0;
            else if (res[0].trim().equals("SPIDER"))
                return 0;
            else if (res[0].trim().equals("CAT"))
                return 0;
            else if (res[0].trim().equals("UNCERTAIN"))
                return 30;
            else
                return -1;  // issues

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
        // API Key is included in the URL for Gemini API
        URL url = new URL(API_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String imageBase64 = encodeImageToBase64(imagePath);
        String imageContentType = imagePath.toLowerCase().endsWith(".png") ? CONTENT_TYPE_PNG : CONTENT_TYPE_JPG;

        // Create JSON body for Gemini API
        JSONObject jsonBody = new JSONObject();
        
        // Create contents array with parts
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        
        // Add parts array with text prompt and image
        JSONArray parts = new JSONArray();
        
        // Add text prompt
        parts.put(new JSONObject().put("text", systemPrompt + (userPrompt != null ? "\n" + userPrompt : "")));
        
        // Add image
        JSONObject imagePart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mimeType", imageContentType);
        inlineData.put("data", imageBase64);
        imagePart.put("inlineData", inlineData);
        parts.put(imagePart);
        
        content.put("parts", parts);
        contents.put(content);
        jsonBody.put("contents", contents);
        
        // Gemini API specific parameters
        jsonBody.put("generationConfig", new JSONObject()
                .put("temperature", 0.0)
                .put("maxOutputTokens", 1000));

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
            
            // Parse Gemini response
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").length() == 0) {
                throw new IOException("No candidates in response: " + response.toString());
            }
            
            JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
            if (!candidate.has("content") || !candidate.getJSONObject("content").has("parts")) {
                throw new IOException("Unexpected response format: " + response.toString());
            }
            
            JSONArray responseParts = candidate.getJSONObject("content").getJSONArray("parts");
            for (int i = 0; i < responseParts.length(); i++) {
                JSONObject part = responseParts.getJSONObject(i);
                if (part.has("text")) {
                    return part.getString("text").trim();
                }
            }
            
            throw new IOException("No text found in response: " + response.toString());
        } else {
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } catch (Exception e) {
                throw new IOException("Error " + responseCode);
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String errorResponse = response.toString();
            lastHttpResponse = errorResponse;
            throw new IOException("Error " + responseCode + " " + errorResponse);
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
