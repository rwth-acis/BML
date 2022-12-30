package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.events.messenger.MessageEvent;

public interface Context {

    MessageEvent event();

    String intent();

    String entity();
}
