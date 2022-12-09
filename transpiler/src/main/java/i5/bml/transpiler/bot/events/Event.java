package i5.bml.transpiler.bot.events;

public interface Event {

    EventSource getEventSource();

    long getArrivalTime();

    String toString();
}
