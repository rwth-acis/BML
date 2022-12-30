package i5.bml.transpiler.bot.threads.rasa;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RasaComponent {

    private final String url;

    private final OkHttpClient okHttpClient;

    public RasaComponent(String url) {
        this.url = url;
        okHttpClient = new OkHttpClient.Builder()
                .writeTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.MINUTES)
                .callTimeout(10, TimeUnit.MINUTES)
                .build();
    }

    public void init() {
        var ymlFile = new File("src/main/resources/example_training_data.yml");
        var ymlContent = "";
        try {
            ymlContent = Files.readString(ymlFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var body = RequestBody.create(ymlContent, MediaType.parse("application/yml"));
        var request = new Request.Builder()
                .url(url + "/model/train")
                .post(body)
                .build();

        System.out.println("Starting model training...");

        final String[] fileName = {null};
        handleResponse(request, response -> {
            fileName[0] = response.headers().get("filename");
            if (fileName[0] == null) {
                System.out.printf("Rasa model training for file %s failed%n", ymlFile);
            }
        }, response -> System.err.println("Callback URLs are not supported"),
                "Rasa model training for file %s failed: ".formatted(ymlFile));

        if (fileName[0] == null) {
            return;
        }

        System.out.println("Model training done!");

        var content = new JsonObject();
        content.addProperty("model_file", "models/" + fileName[0]);
        body = RequestBody.create(content.toString(), MediaType.parse("application/json"));
        request = new Request.Builder()
                .url(url + "/model")
                .put(body)
                .build();

        System.out.println("Starting model loading...");
        handleResponse(request, r -> {}, r -> {}, "Rasa loading model for file %s failed: ".formatted(ymlFile));
        System.out.println("Model loading done!");
    }

    public void parseMessage(MessageEvent messageEvent) {
        if (messageEvent.getText().isEmpty()) {
            // TODO: Add dummy intent and entity or send error message?
            return;
        }

        var content = new JsonObject();
        content.addProperty("text", messageEvent.getText());
        var body = RequestBody.create(content.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
                .url(url + "/model/parse")
                .post(body)
                .build();

        handleResponse(request, response -> {
            if (response.body() == null) {
                System.err.println("Rasa parsing message failed because response body is null");
            }

            try {
                var responseSchema = new Gson().fromJson(response.body().string(), RasaParseResponseSchema.class);
                System.out.println(responseSchema);
                messageEvent.setEntity(responseSchema.entities()[0].value());
                messageEvent.setIntent(responseSchema.intent().name());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, r -> {}, "Rasa parsing message %s failed: ".formatted(messageEvent));
    }

    private void handleResponse(Request request, Consumer<Response> code200, Consumer<Response> code204, String errorMessage) {
        try (var response = okHttpClient.newCall(request).execute()) {
            switch (response.code()) {
                case 200:
                    code200.accept(response);
                    break;
                case 204:
                    code204.accept(response);
                    break;
                case 400:
                case 401:
                case 403:
                case 409:
                case 500:
                    if (response.body() == null) {
                        System.err.println("Parsing Rasa response body failed because response body is null");
                    }

                    RasaErrorResponseSchema responseSchema = new Gson().fromJson(response.body().string(), RasaErrorResponseSchema.class);
                    System.err.printf("Rasa request failed:\n%s%n", responseSchema);
                    break;
                default:
                    System.err.printf("Unexpected code %s with response: %s%n", response.code(), response);
            }
        } catch (IOException e) {
            System.err.println(errorMessage + e.getMessage());
        }
    }
}
