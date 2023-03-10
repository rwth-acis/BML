package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.threads.Session;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Thread-safety?

public class OpenAIComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIComponent.class);

    private final OpenAiService service;

    private final String model;

    private final int tokens;

    private final String prompt;

    private final Map<String, StringBuffer> activeConversations = new HashMap<>();

    public OpenAIComponent(String token, String model, int tokens, String prompt) {
        service = new OpenAiService(token);
        this.model = model;
        this.tokens = tokens;
        this.prompt = prompt;
        try {
            service.listModels();
        } catch (Exception e) {
            LOGGER.error("Failed to connect to OpenAI API", e);
            return;
        }
        LOGGER.info("Successfully initialized connection to OpenAI API");
    }

    public String invokeModel(MessageEvent messageEvent) {
        var history = activeConversations.get(messageEvent.username());
        if (history != null) {
            history.append("\n").append(messageEvent.text().replaceAll("\n", ""));
        } else {
            history = new StringBuffer(prompt + "\n" + messageEvent.text().replaceAll("\n", ""));
        }

        var completionRequest = CompletionRequest.builder()
                .prompt(history.toString())
                .model(model)
                .echo(false)
                .user(messageEvent.username())
                .maxTokens(tokens)
                .n(1) // We only want one choice for completion
                .build();
        var response = service.createCompletion(completionRequest).getChoices().get(0).getText();
        activeConversations.put(messageEvent.username(), history.append("\n").append(response.replaceAll("\n", "")));
        return response;
    }
}
