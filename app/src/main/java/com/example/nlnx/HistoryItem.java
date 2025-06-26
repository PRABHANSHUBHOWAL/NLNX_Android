package com.example.nlnx;

public class HistoryItem {
    private String prompt;
    private String response;
    private String model;
    private long timestamp;

    public HistoryItem(String prompt, String response, String model) {
        this.prompt = prompt;
        this.response = response;
        this.model = model;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getPrompt() { return prompt; }
    public String getResponse() { return response; }
    public String getModel() { return model; }
    public long getTimestamp() { return timestamp; }
}