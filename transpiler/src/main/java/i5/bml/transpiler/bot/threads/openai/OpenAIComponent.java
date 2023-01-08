package i5.bml.transpiler.bot.threads.openai;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;

public class OpenAIComponent {

    private final OpenAiService service;

    private final String model;

    public OpenAIComponent(String token, String model) {
        service = new OpenAiService(token);
        this.model = model;
    }

    public String invokeModel(MessageEvent messageEvent) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(messageEvent.text())
                .model(model)
                .echo(false)
                .user(messageEvent.username())
                .n(1) // We only want one choice for completion
                .build();
        return service.createCompletion(completionRequest).getChoices().get(0).getText();
    }
}
