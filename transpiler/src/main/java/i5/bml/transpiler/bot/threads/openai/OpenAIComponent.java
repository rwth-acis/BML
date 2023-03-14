package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
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

    public OpenAIComponent(String token, String model, int tokens, Duration timeout, String prompt) {
        LOGGER.info("Using {} timeout", timeout);
        service = new OpenAiService(token, timeout);
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
        var messages = activeConversations.computeIfAbsent(messageEvent.username(), k -> {
            var chatMessages = new ArrayList<ChatMessage>();
            chatMessages.add(new ChatMessage("user", prompt));
            return chatMessages;
        });
        messages.add(new ChatMessage("user", messageEvent.text()));

        var completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .user(messageEvent.username())
                .maxTokens(tokens)
                .n(1) // We only want one choice for completion
                .build();
        LOGGER.debug(completionRequest.toString());
        var response = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
        messages.add(new ChatMessage("system", response));
        return response;
    }
}
