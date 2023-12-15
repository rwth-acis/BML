package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        var responseContent = getRequestResult(completionRequestBuilder, false);
        messages.add(new ChatMessage("assistant", responseContent));

        return responseContent;
    }

    public String annotateMessage(String message, Map<String, String> keywords) {
        // Prepare messages
        var messages = new ArrayList<ChatMessage>();
        messages.add(new ChatMessage("user", """
                Identify entities, exclusively nouns as single words, from the messages sent
                to you that would not be understood by a layman. Only provide
                entities that are present in the text. Find all the entities according to
                the previous conditions and respond with a comma-separated list in the
                format <entity>:<category>. The category is one of the following: %s.
                Match the categories to the entities that have the best semantic fit. Use
                the category ’other’ as a fallback if no category matches. Respond with
                a single ’none’ if no entities are present in the given message. The first
                message is: %s
                """.formatted(String.join(", ", keywords.keySet()), message.replaceAll("\n", ""))));

        // Fetch result
        var completionRequestBuilder = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .n(1); // We only want one choice for completion
        var responseContent = getRequestResult(completionRequestBuilder, true);

        if (responseContent.strip().replaceAll("\n", "").equals("none")) {
            return message;
        }

        var entities = new HashMap<String, String>();
        var pairs = responseContent.replaceAll("\\.", "").split(",");
        for (var pair : pairs) {
            var splitted = pair.split(":");
            entities.put(splitted[0].strip(), splitted[1].strip());
        }

        // Annotate message
        for (Map.Entry<String, String> entry : entities.entrySet()) {
            var entity = entry.getKey();
            var topic = entry.getValue();
            System.out.println("entity = " + entity);
            System.out.println("topic = " + topic);
            var url = keywords.get(topic);
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += entity.replaceAll(" ", "_");
            message = message.replaceFirst(" " + entity + " ", " [%s](%s) ".formatted(entity, url));
        }

        return message;
    }

    private String getRequestResult(ChatCompletionRequest.ChatCompletionRequestBuilder completionRequestBuilder, boolean log) {
        if (tokens != -1) {
            completionRequestBuilder.maxTokens(tokens);
        }

        var completionRequest = completionRequestBuilder.build();
        if (log) {
            LOGGER.debug(completionRequest.toString());
        }

        var result = service.createChatCompletion(completionRequest);
        if (log) {
            LOGGER.debug(result.toString());
        }

        return result.getChoices().get(0).getMessage().getContent();
    }
}
