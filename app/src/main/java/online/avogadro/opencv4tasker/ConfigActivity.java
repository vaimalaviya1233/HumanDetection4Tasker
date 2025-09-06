package online.avogadro.opencv4tasker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper;
import online.avogadro.opencv4tasker.tensorflowlite.HumansDetectorTensorFlow;

public class ConfigActivity extends AppCompatActivity {

    private static final String TAG = "ConfigActivity";

    EditText claudeApiKey;
    EditText geminiApiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Configure Claude API key
        claudeApiKey = findViewById(R.id.claudeApiKey);
        String claudeVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.CLAUDE_API_KEY);
        if (claudeVal != null)
            claudeApiKey.setText(claudeVal);
            
        // Configure Gemini API key
        geminiApiKey = findViewById(R.id.geminiApiKey);
        String geminiVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.GEMINI_API_KEY);
        if (geminiVal != null)
            geminiApiKey.setText(geminiVal);

        findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save Claude API key
                SharedPreferencesHelper.save(
                        ConfigActivity.this,
                        SharedPreferencesHelper.CLAUDE_API_KEY,
                        claudeApiKey.getText().toString());
                
                // Save Gemini API key
                SharedPreferencesHelper.save(
                        ConfigActivity.this,
                        SharedPreferencesHelper.GEMINI_API_KEY,
                        geminiApiKey.getText().toString());
                        
                finish(); // return to main activity
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
