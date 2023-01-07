package i5.bml.parser.utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Measurements {

    private static final Logger LOGGER = LoggerFactory.getLogger(Measurements.class);

    private Measurements() {
    }

    private static final Map<String, Pair<String, Long>> measuredTimes = new LinkedHashMap<>();

    private static long totalTime = 0;

    private static Pair<Double, String> calculateUnit(long time) {
        if (time < 1_000) {
            return new ImmutablePair<>((double) time, "ns");
        } else if (time < 1_000_000) {
            return new ImmutablePair<>(time / 1_000d, "µs");
        } else if (time < 1_000_000_000) {
            return new ImmutablePair<>(time / 1_000_000d, "ms");
        } else {
            return new ImmutablePair<>(time / 1_000_000_000d, "s");
        }
    }

    public static void add(String name, long time) {
        var p = calculateUnit(time);
        totalTime += time;
        measuredTimes.put(name,
                new ImmutablePair<>("%s: %.2f %s".formatted(name, p.getLeft(), p.getRight()), time));
    }

    public static void add(String name, String subtractTime, long time) {
        time -= measuredTimes.getOrDefault(subtractTime, new ImmutablePair<>("", 0L)).getRight();
        add(name, time);
    }

    public static void print() {
        System.out.println("--------------- MEASUREMENTS ---------------");
        for (var p : measuredTimes.values()) {
            System.out.println(p.getLeft());
        }
        System.out.println("---------------");
        var p = calculateUnit(totalTime);
        System.out.printf("TOTAL: %s %s%n", p.getLeft(), p.getRight());
        System.out.println("--------------- MEASUREMENTS ---------------");

        measuredTimes.clear();
        totalTime = 0;
    }

    public static void print(StringBuilder stringBuilder) {
        stringBuilder.append("--------------- MEASUREMENTS ---------------\n");
        for (var p : measuredTimes.values()) {
            stringBuilder.append(p.getLeft()).append("\n");
        }
        stringBuilder.append("---------------\n");
        var p = calculateUnit(totalTime);
        stringBuilder.append("TOTAL: %s %s\n".formatted(p.getLeft(), p.getRight()));
        stringBuilder.append("--------------- MEASUREMENTS ---------------\n");

        measuredTimes.clear();
        totalTime = 0;
    }

    public static <T> T measure(String name, Supplier<T> actionToMeasure) {
        var start = System.currentTimeMillis();
        T returnValue = actionToMeasure.get();
        var end = System.currentTimeMillis();
        LOGGER.info("{} took {} ms", name, (end - start));

        return returnValue;
    }

    public static void measure(String name, Runnable actionToMeasure) {
        var start = System.currentTimeMillis();
        actionToMeasure.run();
        var end = System.currentTimeMillis();
        LOGGER.info("{} took {} ms", name, (end - start));
    }
}
