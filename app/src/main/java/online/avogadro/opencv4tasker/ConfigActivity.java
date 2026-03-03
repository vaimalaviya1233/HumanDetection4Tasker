package online.avogadro.opencv4tasker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper;
import online.avogadro.opencv4tasker.app.Util;
import online.avogadro.opencv4tasker.llamacpp.GgufModelInfo;
import online.avogadro.opencv4tasker.llamacpp.LlamaCppModelCatalog;
import online.avogadro.opencv4tasker.llamacpp.LlamaCppModelDownloader;

import java.util.ArrayList;
import java.util.List;

public class ConfigActivity extends AppCompatActivity {

    private static final String TAG = "ConfigActivity";
    private static final int REQUEST_PICK_GEMMA_MODEL = 2001;
    private static final int REQUEST_PICK_LLAMACPP_MODEL = 2002;
    private static final int REQUEST_PICK_LLAMACPP_MMPROJ = 2003;
    private static final String GEMMA3N_HUGGINGFACE_URL =
            "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm";

    // Known model IDs for the spinners. The last entry ("") signals Custom.
    private static final String[] CLAUDE_MODEL_IDS = {
            "claude-sonnet-4-6",
            "claude-opus-4-6",
            "claude-sonnet-4-5",
            "claude-haiku-4-5",
            ""
    };
    private static final String[] GEMINI_MODEL_IDS = {
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-3-pro-preview",
            "gemini-3.1-pro-preview",
            ""
    };

    EditText claudeApiKey;
    Spinner  claudeModelSpinner;
    EditText claudeCustomModel;

    EditText geminiApiKey;
    Spinner  geminiModelSpinner;
    EditText geminiCustomModel;

    EditText openRouterApiKey;
    EditText openRouterModel;

    TextView gemma3nModelPathLabel;

    Spinner llamacppModelSpinner;
    TextView llamacppStatusLabel;
    ProgressBar llamacppProgressBar;
    LinearLayout llamacppCatalogButtons;
    LinearLayout llamacppCustomSection;
    TextView llamacppModelPathLabel;
    TextView llamacppMmprojPathLabel;
    private volatile boolean llamacppDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // --- Claude API Key ---
        claudeApiKey = findViewById(R.id.claudeApiKey);
        String claudeVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.CLAUDE_API_KEY);
        if (claudeVal != null) claudeApiKey.setText(claudeVal);

        // --- Claude Model Spinner ---
        claudeModelSpinner = findViewById(R.id.claudeModelSpinner);
        claudeCustomModel  = findViewById(R.id.claudeCustomModel);

        String[] claudeNames = {
                getString(R.string.claude_model_sonnet_46),
                getString(R.string.claude_model_opus_46),
                getString(R.string.claude_model_sonnet_45),
                getString(R.string.claude_model_haiku_45),
                getString(R.string.model_custom)
        };
        ArrayAdapter<String> claudeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, claudeNames);
        claudeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        claudeModelSpinner.setAdapter(claudeAdapter);

        String savedClaudeModel = SharedPreferencesHelper.get(this, SharedPreferencesHelper.CLAUDE_MODEL);
        if (savedClaudeModel == null || savedClaudeModel.isEmpty()) {
            claudeModelSpinner.setSelection(0); // nessuna preferenza salvata → default (Sonnet 4.6)
        } else {
            int claudeIdx = findModelIndex(savedClaudeModel, CLAUDE_MODEL_IDS);
            if (claudeIdx >= 0) {
                claudeModelSpinner.setSelection(claudeIdx);
            } else {
                claudeModelSpinner.setSelection(CLAUDE_MODEL_IDS.length - 1); // Custom
                claudeCustomModel.setText(savedClaudeModel);
                claudeCustomModel.setVisibility(View.VISIBLE);
            }
        }
        claudeModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                claudeCustomModel.setVisibility(
                        position == CLAUDE_MODEL_IDS.length - 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Gemini API Key ---
        geminiApiKey = findViewById(R.id.geminiApiKey);
        String geminiVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.GEMINI_API_KEY);
        if (geminiVal != null) geminiApiKey.setText(geminiVal);

        // --- Gemini Model Spinner ---
        geminiModelSpinner = findViewById(R.id.geminiModelSpinner);
        geminiCustomModel  = findViewById(R.id.geminiCustomModel);

        String[] geminiNames = {
                getString(R.string.gemini_model_25_flash),
                getString(R.string.gemini_model_25_pro),
                getString(R.string.gemini_model_30_pro_preview),
                getString(R.string.gemini_model_31_pro_preview),
                getString(R.string.model_custom)
        };
        ArrayAdapter<String> geminiAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, geminiNames);
        geminiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        geminiModelSpinner.setAdapter(geminiAdapter);

        String savedGeminiModel = SharedPreferencesHelper.get(this, SharedPreferencesHelper.GEMINI_MODEL);
        if (savedGeminiModel == null || savedGeminiModel.isEmpty()) {
            geminiModelSpinner.setSelection(0); // nessuna preferenza salvata → default (2.5 Flash)
        } else {
            int geminiIdx = findModelIndex(savedGeminiModel, GEMINI_MODEL_IDS);
            if (geminiIdx >= 0) {
                geminiModelSpinner.setSelection(geminiIdx);
            } else {
                geminiModelSpinner.setSelection(GEMINI_MODEL_IDS.length - 1); // Custom
                geminiCustomModel.setText(savedGeminiModel);
                geminiCustomModel.setVisibility(View.VISIBLE);
            }
        }
        geminiModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                geminiCustomModel.setVisibility(
                        position == GEMINI_MODEL_IDS.length - 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- OpenRouter ---
        openRouterApiKey = findViewById(R.id.openRouterApiKey);
        String openRouterKeyVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.OPENROUTER_API_KEY);
        if (openRouterKeyVal != null) openRouterApiKey.setText(openRouterKeyVal);

        openRouterModel = findViewById(R.id.openRouterModel);
        String openRouterModelVal = SharedPreferencesHelper.get(this, SharedPreferencesHelper.OPENROUTER_MODEL);
        if (openRouterModelVal != null && !openRouterModelVal.isEmpty())
            openRouterModel.setText(openRouterModelVal);
        else
            openRouterModel.setText(SharedPreferencesHelper.DEFAULT_OPENROUTER_MODEL);

        // --- Gemma 3n ---
        gemma3nModelPathLabel = findViewById(R.id.gemma3nModelPathLabel);
        String savedGemma3nPath = SharedPreferencesHelper.get(this, SharedPreferencesHelper.GEMMA3N_MODEL_PATH);
        updateGemma3nPathLabel(savedGemma3nPath);

        findViewById(R.id.buttonGemma3nDownload).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GEMMA3N_HUGGINGFACE_URL));
            startActivity(browserIntent);
        });

        findViewById(R.id.buttonGemma3nSelect).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_PICK_GEMMA_MODEL);
        });

        // --- llama.cpp / GGUF ---
        llamacppModelSpinner = findViewById(R.id.llamacppModelSpinner);
        llamacppStatusLabel = findViewById(R.id.llamacppStatusLabel);
        llamacppProgressBar = findViewById(R.id.llamacppProgressBar);
        llamacppCatalogButtons = findViewById(R.id.llamacppCatalogButtons);
        llamacppCustomSection = findViewById(R.id.llamacppCustomSection);
        llamacppModelPathLabel = findViewById(R.id.llamacppModelPathLabel);
        llamacppMmprojPathLabel = findViewById(R.id.llamacppMmprojPathLabel);

        List<GgufModelInfo> catalogModels = LlamaCppModelCatalog.INSTANCE.getMODELS();
        List<String> llamacppNames = new ArrayList<>();
        for (GgufModelInfo m : catalogModels) {
            llamacppNames.add(m.getDisplayName());
        }
        llamacppNames.add("Personalizzato…");

        ArrayAdapter<String> llamacppAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, llamacppNames);
        llamacppAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        llamacppModelSpinner.setAdapter(llamacppAdapter);

        // Restore saved selection
        String savedSelectedModel = SharedPreferencesHelper.get(this, SharedPreferencesHelper.LLAMACPP_SELECTED_MODEL);
        int savedIdx = -1;
        if (savedSelectedModel != null && !savedSelectedModel.isEmpty()) {
            try { savedIdx = Integer.parseInt(savedSelectedModel); } catch (NumberFormatException ignored) {}
        }
        if (savedIdx >= 0 && savedIdx < catalogModels.size()) {
            llamacppModelSpinner.setSelection(savedIdx);
        } else if (savedIdx == catalogModels.size()) {
            // Custom
            llamacppModelSpinner.setSelection(catalogModels.size());
        } else {
            llamacppModelSpinner.setSelection(0);
        }

        llamacppModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isCustom = position == catalogModels.size();
                llamacppCustomSection.setVisibility(isCustom ? View.VISIBLE : View.GONE);
                llamacppCatalogButtons.setVisibility(isCustom ? View.GONE : View.VISIBLE);
                if (!isCustom) {
                    updateLlamacppCatalogStatus(catalogModels.get(position));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Update custom path labels
        String savedLlamacppModelPath = SharedPreferencesHelper.get(this, SharedPreferencesHelper.LLAMACPP_MODEL_PATH);
        String savedLlamacppMmprojPath = SharedPreferencesHelper.get(this, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH);
        updateLlamacppPathLabel(llamacppModelPathLabel, "Path modello", savedLlamacppModelPath);
        updateLlamacppPathLabel(llamacppMmprojPathLabel, "Path mmproj", savedLlamacppMmprojPath);

        findViewById(R.id.buttonLlamacppDownload).setOnClickListener(v -> {
            int pos = llamacppModelSpinner.getSelectedItemPosition();
            if (pos >= catalogModels.size() || llamacppDownloading) return;
            GgufModelInfo modelInfo = catalogModels.get(pos);
            llamacppDownloading = true;
            llamacppProgressBar.setVisibility(View.VISIBLE);
            llamacppProgressBar.setProgress(0);
            llamacppStatusLabel.setText("Download in corso… 0%");
            ((Button) v).setEnabled(false);

            new Thread(() -> {
                boolean success = LlamaCppModelDownloader.INSTANCE.downloadModel(this, modelInfo,
                        progress -> {
                            runOnUiThread(() -> {
                                llamacppProgressBar.setProgress(progress);
                                llamacppStatusLabel.setText("Download in corso… " + progress + "%");
                            });
                            return kotlin.Unit.INSTANCE;
                        });
                runOnUiThread(() -> {
                    llamacppDownloading = false;
                    llamacppProgressBar.setVisibility(View.GONE);
                    ((Button) v).setEnabled(true);
                    if (success) {
                        llamacppStatusLabel.setText("Scaricato");
                    } else {
                        llamacppStatusLabel.setText("Download fallito. Verifica spazio disponibile.");
                    }
                });
            }).start();
        });

        findViewById(R.id.buttonLlamacppDelete).setOnClickListener(v -> {
            int pos = llamacppModelSpinner.getSelectedItemPosition();
            if (pos >= catalogModels.size()) return;
            GgufModelInfo modelInfo = catalogModels.get(pos);
            LlamaCppModelDownloader.INSTANCE.deleteModel(this, modelInfo);
            updateLlamacppCatalogStatus(modelInfo);
        });

        findViewById(R.id.buttonLlamacppSelectModel).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_PICK_LLAMACPP_MODEL);
        });

        findViewById(R.id.buttonLlamacppSelectMmproj).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_PICK_LLAMACPP_MMPROJ);
        });

        // --- Save button ---
        findViewById(R.id.buttonSave).setOnClickListener(v -> {
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.CLAUDE_API_KEY,
                    claudeApiKey.getText().toString());

            int claudeSel = claudeModelSpinner.getSelectedItemPosition();
            String claudeModel = (claudeSel == CLAUDE_MODEL_IDS.length - 1)
                    ? claudeCustomModel.getText().toString().trim()
                    : CLAUDE_MODEL_IDS[claudeSel];
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.CLAUDE_MODEL, claudeModel);

            SharedPreferencesHelper.save(this, SharedPreferencesHelper.GEMINI_API_KEY,
                    geminiApiKey.getText().toString());

            int geminiSel = geminiModelSpinner.getSelectedItemPosition();
            String geminiModel = (geminiSel == GEMINI_MODEL_IDS.length - 1)
                    ? geminiCustomModel.getText().toString().trim()
                    : GEMINI_MODEL_IDS[geminiSel];
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.GEMINI_MODEL, geminiModel);

            SharedPreferencesHelper.save(this, SharedPreferencesHelper.OPENROUTER_API_KEY,
                    openRouterApiKey.getText().toString());
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.OPENROUTER_MODEL,
                    openRouterModel.getText().toString());

            // Save llama.cpp selected model index
            int llamacppSel = llamacppModelSpinner.getSelectedItemPosition();
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.LLAMACPP_SELECTED_MODEL,
                    String.valueOf(llamacppSel));

            // If a catalog model is selected and downloaded, set paths automatically
            List<GgufModelInfo> models = LlamaCppModelCatalog.INSTANCE.getMODELS();
            if (llamacppSel < models.size()) {
                GgufModelInfo selected = models.get(llamacppSel);
                if (LlamaCppModelDownloader.INSTANCE.isModelDownloaded(this, selected)) {
                    SharedPreferencesHelper.save(this, SharedPreferencesHelper.LLAMACPP_MODEL_PATH,
                            LlamaCppModelDownloader.INSTANCE.getModelPath(this, selected));
                    SharedPreferencesHelper.save(this, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH,
                            LlamaCppModelDownloader.INSTANCE.getMmprojPath(this, selected));
                }
            }

            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        // Take persistent permission
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }

        String resolvedPath = Util.getModelPathFromUri(this, uri);
        String pathOrUri = resolvedPath != null ? resolvedPath : uri.toString();

        if (requestCode == REQUEST_PICK_GEMMA_MODEL) {
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.GEMMA3N_MODEL_PATH, pathOrUri);
            updateGemma3nPathLabel(pathOrUri);
            if (resolvedPath == null) {
                gemma3nModelPathLabel.setText("URI: " + pathOrUri + "\n(Attenzione: impossibile risolvere il path. Inserisci il path assoluto manualmente se necessario.)");
            }
        } else if (requestCode == REQUEST_PICK_LLAMACPP_MODEL) {
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.LLAMACPP_MODEL_PATH, pathOrUri);
            updateLlamacppPathLabel(llamacppModelPathLabel, "Path modello", pathOrUri);
        } else if (requestCode == REQUEST_PICK_LLAMACPP_MMPROJ) {
            SharedPreferencesHelper.save(this, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH, pathOrUri);
            updateLlamacppPathLabel(llamacppMmprojPathLabel, "Path mmproj", pathOrUri);
        }
    }

    private void updateGemma3nPathLabel(String path) {
        if (path == null || path.isEmpty()) {
            gemma3nModelPathLabel.setText("Path modello: (non configurato)");
        } else {
            gemma3nModelPathLabel.setText("Path modello: " + path);
        }
    }

    /** Returns the index of modelId in modelIds (excluding the last "Custom" entry), or -1. */
    private int findModelIndex(String modelId, String[] modelIds) {
        if (modelId == null || modelId.isEmpty()) return -1;
        for (int i = 0; i < modelIds.length - 1; i++) {
            if (modelIds[i].equals(modelId)) return i;
        }
        return -1;
    }

    private void updateLlamacppCatalogStatus(GgufModelInfo modelInfo) {
        boolean downloaded = LlamaCppModelDownloader.INSTANCE.isModelDownloaded(this, modelInfo);
        llamacppStatusLabel.setText(downloaded ? "Scaricato" : "Non scaricato (" + modelInfo.getTotalSizeMB() + " MB)");
    }

    private void updateLlamacppPathLabel(TextView label, String prefix, String path) {
        if (path == null || path.isEmpty()) {
            label.setText(prefix + ": (non configurato)");
        } else {
            label.setText(prefix + ": " + path);
        }
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
