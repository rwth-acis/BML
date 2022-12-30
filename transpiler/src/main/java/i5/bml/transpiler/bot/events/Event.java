package i5.bml.transpiler.bot.events;

public interface Event {

    EventSource eventSource();

    long arrivalTime();

    String toString();
}
