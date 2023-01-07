package i5.bml.transpiler.bot.threads.rasa;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.utils.IOUtil;
import i5.bml.transpiler.bot.utils.PersistentStorage;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        // Check if bot has already been started and model name was saved
        var settings = PersistentStorage.getBotSettings();
        if (settings.rasaModelName() != null) {
            var rasaModelName = settings.rasaModelName();
            if (!rasaModelName.equals(getLoadedModel())) {
                loadModel(rasaModelName);
            } else {
                LOGGER.info("Desired model is already loaded");
            }
        } else {
            var rasaModelName = trainModel();

            // Training failed -> we can't load the model, hence, just return
            if (rasaModelName == null) {
                return;
            }

            settings.rasaModelName(rasaModelName);
            PersistentStorage.writeBotSettings(settings);
            loadModel(rasaModelName);
        }
    }

    private String trainModel() {
        var ymlContent = "";
        try {
            ymlContent = IOUtil.getResourceFileAsString("example_training_data.yml");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read training file", e);
        }

        var request = new Request.Builder()
                .url(url + "/model/train")
                .post(RequestBody.create(ymlContent, MediaType.parse("application/yml")))
                .build();

        LOGGER.info("Starting model training...");

        final String[] rasaModelName = {null};
        handleResponse(request, code200Response -> {
            rasaModelName[0] = code200Response.headers().get("filename");
            if (rasaModelName[0] == null) {
                throw new IllegalStateException("Rasa model training failed, response header, does not contain rasaModelName");
            } else {
                LOGGER.info("Model training done!");
            }
        }, code204Response -> {
            throw new IllegalStateException("Callback URLs are not supported");
        }, "Rasa model training failed: ");

        return rasaModelName[0];
    }

    private void loadModel(String rasaModelName) {
        var content = new JsonObject();
        content.addProperty("model_file", "models/" + rasaModelName);
        var request = new Request.Builder()
                .url(url + "/model")
                .put(RequestBody.create(content.toString(), MediaType.parse("application/json")))
                .build();

        LOGGER.info("Starting model loading...");
        handleResponse(request, code200Response -> {
            throw new IllegalStateException("Rasa loading model failed, it seems that the model name %s is not known".formatted(rasaModelName));
        }, r -> LOGGER.info("Model loading done!"), "Rasa loading model failed: ");
    }

    public void invokeModel(MessageEvent messageEvent) {
        if (messageEvent.text().isEmpty()) {
            return;
        }

        var content = new JsonObject();
        content.addProperty("text", messageEvent.text());
        var request = new Request.Builder()
                .url(url + "/model/parse")
                .post(RequestBody.create(content.toString(), MediaType.parse("application/json")))
                .build();

        handleResponse(request, code200Response -> {
            if (code200Response.body() == null) {
                throw new IllegalStateException("Rasa parsing message failed because response body is null");
            }

            try {
                var responseSchema = new Gson().fromJson(code200Response.body().string(), RasaParseResponseSchema.class);
                LOGGER.debug(responseSchema.toString());
                if (responseSchema.entities().length > 0) {
                    messageEvent.entity(responseSchema.entities()[0].value());
                } else {
                    messageEvent.entity("{no_entity_found}");
                }
                messageEvent.intent(responseSchema.intent().name());
            } catch (IOException e) {
                throw new IllegalStateException("Rasa parsing message failed while retrieving response body", e);
            }
        }, code204Response -> {
        }, "Rasa parsing message %s failed: ".formatted(messageEvent));
    }

    private String getLoadedModel() {
        var request = new Request.Builder()
                .url(url + "/status")
                .get()
                .build();

        final String[] rasaModelName = {null};
        handleResponse(request, code200Response -> {
            if (code200Response.body() == null) {
                throw new IllegalStateException("Rasa getting server status failed because response body is null");
            }

            try {
                var responseSchema = new Gson().fromJson(code200Response.body().string(), RasaStatusResponseSchema.class);
                rasaModelName[0] = responseSchema.modelFile();
            } catch (IOException e) {
                throw new IllegalStateException("Rasa getting server status failed while retrieving response body", e);
            }
        }, code204Response -> {
            throw new IllegalStateException("Callback URLs are not supported");
        }, "Rasa getting server status failed: ");

        return rasaModelName[0];
    }

    private void handleResponse(Request request, Consumer<Response> code200, Consumer<Response> code204, String errorMessage) {
        try (var response = okHttpClient.newCall(request).execute()) {
            switch (response.code()) {
                case 200 -> code200.accept(response);
                case 204 -> code204.accept(response);
                case 400, 401, 403, 409, 500 -> {
                    if (response.body() == null) {
                        throw new IllegalStateException("Parsing Rasa response body failed because response body is null");
                    }
                    RasaErrorResponseSchema responseSchema = new Gson().fromJson(response.body().string(), RasaErrorResponseSchema.class);
                    throw new IllegalStateException("Rasa request failed:\n%s".formatted(responseSchema));
                }
                default ->
                        throw new IllegalStateException("Unexpected code %s with response:\n%s".formatted(response.code(), response));
            }
        } catch (IOException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
