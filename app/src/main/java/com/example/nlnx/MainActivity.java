package com.example.nlnx;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageButton;
import android.widget.TextView;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import org.json.JSONArray;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etPrompt, etResponse;
    private MaterialButton btnSearch;
    private RequestQueue requestQueue;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyItems = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    private static final String COHERE_API_KEY = "plsSkwhQ9GCkWsH4KEgtTE26Nk06EVZMyDOS5SRc";
    private static final String GEMINI_API_KEY = "AIzaSyAkk2Z-tR_FVTo17K7mNsoilbODrmeMTCM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d("APP_START", "onCreate started");
            setContentView(R.layout.activity_main);

            // Initialize UI components
            etPrompt = findViewById(R.id.et_prompt);
            etResponse = findViewById(R.id.et_response);
            btnSearch = findViewById(R.id.btn_search);
            ImageButton btnCopy = findViewById(R.id.btn_copy);
            RecyclerView rvHistory = findViewById(R.id.rv_history);
            TextView tvEmptyHistory = findViewById(R.id.tv_empty_history);

            // Setup RecyclerView
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            historyAdapter = new HistoryAdapter(historyItems, this::deleteHistoryItem);
            rvHistory.setAdapter(historyAdapter);

            // Setup model spinner - FIXED
            AutoCompleteTextView modelSpinner = findViewById(R.id.model_spinner);
            String[] models = getResources().getStringArray(R.array.models_array);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    models
            );
            modelSpinner.setAdapter(adapter);
            modelSpinner.setText(models[0], false);

            // Initialize Volley
            requestQueue = Volley.newRequestQueue(this);

            // Load history
            loadHistory();
            updateHistoryVisibility();

            // Setup button listeners
            btnSearch.setOnClickListener(v -> handleSearch());
            btnCopy.setOnClickListener(v -> copyResponseToClipboard());

        } catch (Exception e) {
            Log.e("APP_CRASH", "Startup error", e);
            Toast.makeText(this, "App failed to start: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleSearch() {
        String userPrompt = etPrompt.getText().toString().trim();
        String model = ((AutoCompleteTextView) findViewById(R.id.model_spinner)).getText().toString();

        if (userPrompt.isEmpty()) {
            Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add prefix instruction to the prompt
        String fullPrompt = "Only give the Linux/Bash command. No Explanation required. " + userPrompt;

        if (model.equals(getString(R.string.cohere))) {
            callCohereAPI(fullPrompt, userPrompt);  // Pass both full and original prompts
        } else if (model.equals(getString(R.string.gemini))) {
            callGeminiAPI(fullPrompt, userPrompt);  // Pass both full and original prompts
        }
    }

    private void callCohereAPI(String apiPrompt, String originalPrompt) {
        String url = "https://api.cohere.ai/v1/chat";

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "command-r-plus");
            requestBody.put("message", apiPrompt);  // Use the modified prompt
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 50);

            Log.d("COHERE_REQUEST", requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        Log.d("COHERE_RESPONSE", response.toString());
                        try {
                            String result = response.getString("text");
                            // Clean up Cohere response
                            result = result.trim()
                                    .replaceAll("^\"|\"$", "")  // Remove quotes if present
                                    .replaceAll("\\n", " ")     // Remove newlines
                                    .trim();

                            etResponse.setText(result);
                            // Save original user prompt in history
                            saveToHistory(originalPrompt, result, "Cohere");
                        } catch (JSONException e) {
                            handleError("Cohere response parsing error: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = "Cohere API error: ";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                errorMsg += "Status: " + error.networkResponse.statusCode +
                                        ", Body: " + responseBody;
                            } catch (UnsupportedEncodingException e) {
                                errorMsg += "Failed to parse error response";
                            }
                        } else {
                            errorMsg += error.getMessage() != null ?
                                    error.getMessage() : "Unknown network error";
                        }
                        handleError(errorMsg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + COHERE_API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            handleError("Request creation error: " + e.getMessage());
        }
    }

    private void callGeminiAPI(String apiPrompt, String originalPrompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObject = new JSONObject();
            JSONArray partsArray = new JSONArray();

            // Create text part
            JSONObject textPart = new JSONObject();
            textPart.put("text", apiPrompt);
            partsArray.put(textPart);

            // Build content object
            contentObject.put("parts", partsArray);
            contentsArray.put(contentObject);
            requestBody.put("contents", contentsArray);

            // Add safety settings to prevent blocking
            JSONArray safetySettings = new JSONArray();
            JSONObject setting = new JSONObject();
            setting.put("category", "HARM_CATEGORY_DANGEROUS_CONTENT");
            setting.put("threshold", "BLOCK_NONE");
            safetySettings.put(setting);
            requestBody.put("safetySettings", safetySettings);

            // Add generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.5);
            generationConfig.put("maxOutputTokens", 100);
            requestBody.put("generationConfig", generationConfig);

            Log.d("GEMINI_REQUEST", requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        Log.d("GEMINI_RESPONSE", response.toString());
                        try {
                            JSONArray candidates = response.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject firstCandidate = candidates.getJSONObject(0);
                                JSONObject content = firstCandidate.getJSONObject("content");
                                JSONArray parts = content.getJSONArray("parts");
                                JSONObject firstPart = parts.getJSONObject(0);
                                String result = firstPart.getString("text").trim();

                                // Clean up response
                                result = result.replace("bash\n", "")
                                        .replace("\n", " ")
                                        .replaceAll("^\"|\"$", "")
                                        .trim();

                                etResponse.setText(result);
                                saveToHistory(originalPrompt, result, "Gemini");
                            } else {
                                handleError("Gemini returned no candidates");
                            }
                        } catch (JSONException e) {
                            handleError("Gemini response parsing error: " + e.getMessage());
                        }
                    },
                    error -> {
                        String errorMsg = "Gemini API error: ";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                errorMsg += "Status: " + error.networkResponse.statusCode +
                                        ", Body: " + responseBody;
                            } catch (UnsupportedEncodingException e) {
                                errorMsg += "Failed to parse error response";
                            }
                        } else {
                            errorMsg += error.getMessage() != null ?
                                    error.getMessage() : "Unknown network error";
                        }
                        handleError(errorMsg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            handleError("Request creation error: " + e.getMessage());
        }
    }

    private void saveToHistory(String prompt, String response, String model) {
        // Remove oldest if limit reached
        if (historyItems.size() >= MAX_HISTORY) {
            historyItems.remove(historyItems.size() - 1);
        }

        // Add to beginning
        historyItems.add(0, new HistoryItem(prompt, response, model));
        saveHistory();
        updateHistoryVisibility();
    }

    private void loadHistory() {
        String json = getPreferences(Context.MODE_PRIVATE).getString("history", "");
        if (!json.isEmpty()) {
            historyItems = new Gson().fromJson(json,
                    new TypeToken<ArrayList<HistoryItem>>(){}.getType());
        }
        historyAdapter.updateData(historyItems);
    }

    private void saveHistory() {
        String json = new Gson().toJson(historyItems);
        getPreferences(Context.MODE_PRIVATE).edit()
                .putString("history", json)
                .apply();
        historyAdapter.notifyDataSetChanged();
    }

    private void deleteHistoryItem(int position) {
        historyItems.remove(position);
        saveHistory();
        updateHistoryVisibility();
    }

    private void updateHistoryVisibility() {
        findViewById(R.id.rv_history).setVisibility(
                historyItems.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.tv_empty_history).setVisibility(
                historyItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void copyResponseToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Command", etResponse.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void handleError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        etResponse.setText("Error: " + message);
    }
}