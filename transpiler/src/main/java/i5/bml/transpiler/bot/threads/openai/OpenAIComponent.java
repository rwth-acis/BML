package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Thread-safety?

public class OpenAIComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIComponent.class);

    private final OpenAiService service;

    private final String model;

    private final int tokens;

    public OpenAIComponent(String token, String model, int tokens) {
        service = new OpenAiService(token);
        this.model = model;
        this.tokens = tokens;
        try {
            service.listModels();
        } catch (Exception e) {
            LOGGER.error("Failed to connect to OpenAI API", e);
        }
    }

    public String invokeModel(MessageEvent messageEvent) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(messageEvent.text())
                .model(model)
                .echo(false)
                .user(messageEvent.username())
                .maxTokens(tokens)
                .n(1) // We only want one choice for completion
                .build();
        return service.createCompletion(completionRequest).getChoices().get(0).getText();
    }
}
