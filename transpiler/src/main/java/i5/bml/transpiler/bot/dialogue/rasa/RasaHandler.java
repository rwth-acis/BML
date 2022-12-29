package i5.bml.transpiler.bot.dialogue.rasa;

import com.google.gson.Gson;
import i5.bml.transpiler.bot.dialogue.RasaErrorResponseSchema;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class RasaHandler {

    private final String url;

    public RasaHandler(String url) {
        this.url = url;
    }

    public void init() {
        var ymlFile = new File("transpiler/src/main/resources/example_training_data.yml");
        var ymlContent = "";
        try {
            ymlContent = Files.readString(ymlFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(ymlContent, MediaType.parse("application/yml"));

        Request request = new Request.Builder()
                .url(url + "/model/train")
                .post(body)
                .build();
        var client = new OkHttpClient();
        try (var response = client.newCall(request).execute()) {
            switch (response.code()) {
                case 200:

                    break;
                case 204:
                    throw new IllegalStateException("Callback URLs are not supported");
                case 400:
                case 401:
                case 403:
                case 500:
                    parseError(response);
                    break;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Rasa model training for file %s failed".formatted(ymlFile), e);
        }
    }

    private void parseError(Response response) throws IOException {
        if (response.body() == null) {
            throw new IllegalStateException("Rasa model training failed because response body is null");
        }

        RasaErrorResponseSchema responseSchema = new Gson().fromJson(response.body().string(), RasaErrorResponseSchema.class);
        throw new IllegalStateException("Rasa model training failed:\n%s".formatted(responseSchema));
    }
}
