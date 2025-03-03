package online.avogadro.opencv4tasker.ai;

import android.content.Context;

import org.json.JSONException;

import java.io.IOException;

/**
 * Common interface for AI-based image analysis
 */
public interface AIImageAnalyzer {
    /**
     * Set up the analyzer with necessary API keys and configurations
     * @param ctx Android context
     */
    void setup(Context ctx) throws IOException;
    
    /**
     * Analyze an image with AI using provided prompts
     * @param systemPrompt Instructions for the AI
     * @param userPrompt User's specific question about the image
     * @param imagePath Path to the image file
     * @return The AI's complete response as a string
     */
    String analyzeImage(String systemPrompt, String userPrompt, String imagePath) throws IOException, JSONException;
    
    /**
     * Get the last response from the AI
     */
    String getLastResponse();
    
    /**
     * Get error information if the last call failed
     */
    String getLastError();
}
