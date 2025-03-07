package com.example.listi;
import java.io.IOException;

import okhttp3.*;

public class AzureTTSHelper {
    private static final String TOKEN_URL = "https://%s.api.cognitive.microsoft.com/sts/v1.0/issuetoken";
    private static final String TTS_URL = "https://%s.tts.speech.microsoft.com/cognitiveservices/v1";

    private final String subscriptionKey;
    private final String region;
    private final OkHttpClient client = new OkHttpClient();

    public AzureTTSHelper(String subscriptionKey, String region) {
        this.subscriptionKey = subscriptionKey;
        this.region = region;
    }

    public void synthesizeSpeech(String text, Callback callback) {
        getToken(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String token = response.body().string();
                    makeTtsRequest(token, text, callback);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(call, e);
            }
        });
    }

    private void getToken(Callback callback) {
        Request request = new Request.Builder()
                .url(String.format(TOKEN_URL, region))
                .post(RequestBody.create("", null))
                .addHeader("Ocp-Apim-Subscription-Key", subscriptionKey)
                .build();

        client.newCall(request).enqueue(callback);
    }

    private void makeTtsRequest(String token, String text, Callback callback) {
        String ssml = String.format(
                "<speak version='1.0' xml:lang='mt-MT'>" +
                        "<voice name='mt-MT-GraceNeural'>%s</voice></speak>",
                text
        );

        Request request = new Request.Builder()
                .url(String.format(TTS_URL, region))
                .post(RequestBody.create(ssml, MediaType.parse("application/ssml+xml")))
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("X-Microsoft-OutputFormat", "riff-24khz-16bit-mono-pcm")
                .addHeader("User-Agent", "YourAppName")
                .build();

        client.newCall(request).enqueue(callback);
    }
}