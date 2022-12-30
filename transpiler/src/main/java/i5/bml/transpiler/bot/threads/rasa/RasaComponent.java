package i5.bml.transpiler.bot.threads.rasa;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RasaComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RasaComponent.class);

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
        var fileName = trainModel();

        // Training failed -> we can't load the model, hence, just return
        if (fileName == null) {
            return;
        }

        loadModel(fileName);
    }

    private String trainModel() {
        // TODO: Load this from somewhere?
        var ymlFile = new File("src/main/resources/example_training_data.yml");
        var ymlContent = "";
        try {
            ymlContent = Files.readString(ymlFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var request = new Request.Builder()
                .url(url + "/model/train")
                .post(RequestBody.create(ymlContent, MediaType.parse("application/yml")))
                .build();

        LOGGER.info("Starting model training...");

        final String[] fileName = {null};
        handleResponse(request, response -> {
                    fileName[0] = response.headers().get("filename");
                    if (fileName[0] == null) {
                        LOGGER.error("Rasa model training for file {} failed", ymlFile);
                    } else {
                        LOGGER.info("Model training done!");
                    }
                }, response -> LOGGER.error("Callback URLs are not supported"),
                "Rasa model training for file %s failed: ".formatted(ymlFile));

        return fileName[0];
    }

    private void loadModel(String fileName) {
        var content = new JsonObject();
        content.addProperty("model_file", "models/" + fileName);
        var request = new Request.Builder()
                .url(url + "/model")
                .put(RequestBody.create(content.toString(), MediaType.parse("application/json")))
                .build();

        LOGGER.info("Starting model loading...");
        handleResponse(request, r -> {}, r -> LOGGER.info("Model loading done!"), "Rasa loading model failed: ");
    }

    public void invokeModel(MessageEvent messageEvent) {
        if (messageEvent.text().isEmpty()) {
            // TODO: Add dummy intent and entity or send error message?
            return;
        }

        var content = new JsonObject();
        content.addProperty("text", messageEvent.text());
        var request = new Request.Builder()
                .url(url + "/model/parse")
                .post(RequestBody.create(content.toString(), MediaType.parse("application/json")))
                .build();

        handleResponse(request, response -> {
            if (response.body() == null) {
                LOGGER.error("Rasa parsing message failed because response body is null");
            }

            try {
                var responseSchema = new Gson().fromJson(response.body().string(), RasaParseResponseSchema.class);
                LOGGER.debug(responseSchema.toString());
                messageEvent.entity(responseSchema.entities()[0].value());
                messageEvent.intent(responseSchema.intent().name());
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
                        LOGGER.error("Parsing Rasa response body failed because response body is null");
                    }

                    RasaErrorResponseSchema responseSchema = new Gson().fromJson(response.body().string(), RasaErrorResponseSchema.class);
                    LOGGER.error("Rasa request failed:\n{}", responseSchema);
                    break;
                default:
                    LOGGER.error("Unexpected code {} with response:\n{}", response.code(), response);
            }
        } catch (IOException e) {
            LOGGER.error(errorMessage + e.getMessage());
        }
    }
}
