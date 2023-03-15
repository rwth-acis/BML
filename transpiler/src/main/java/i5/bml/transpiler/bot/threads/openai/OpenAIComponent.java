package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    private final Map<String, List<ChatMessage>> activeConversations = new HashMap<>();

    public OpenAIComponent(String apiKey, String model, int tokens, Duration timeout, String prompt) {
        service = new OpenAiService(apiKey, timeout);
        LOGGER.info("Using {} timeout", timeout);
        this.model = model;
        this.tokens = tokens;
        LOGGER.info("Using {} tokens", tokens == -1 ? "inf" : tokens);
        this.prompt = prompt;
        try {
            service.listModels();
        } catch (Exception e) {
            LOGGER.error("Failed to connect to OpenAI API: {}", e.getMessage());
            LOGGER.debug("Stacktrace:", e);
            return;
        }
        LOGGER.info("Successfully initialized connection to OpenAI API");
    }

    public String invokeModel(MessageEvent messageEvent) {
        var messages = activeConversations.get(messageEvent.username());
        if (messages == null) {
            messages = new ArrayList<>();
            if (!prompt.isEmpty()) {
                messages.add(new ChatMessage("system", prompt));
            }
            messages.add(new ChatMessage("user", messageEvent.text()));
            activeConversations.put(messageEvent.username(), messages);
        } else {
            messages.add(new ChatMessage("user", messageEvent.text()));
        }

        var completionRequestBuilder = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .user(messageEvent.username())
                .n(1); // We only want one choice for completion
        if (tokens != -1) {
            completionRequestBuilder.maxTokens(tokens);
        }

        var completionRequest = completionRequestBuilder.build();
        LOGGER.debug(completionRequest.toString());

        var result = service.createChatCompletion(completionRequest);
        LOGGER.debug(result.toString());

        String responseContent = result.getChoices().get(0).getMessage().getContent();
        messages.add(new ChatMessage("assistant", responseContent));

        return responseContent;
    }
}
