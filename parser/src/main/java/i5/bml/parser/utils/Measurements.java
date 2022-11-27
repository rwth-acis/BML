package i5.bml.parser.utils;

import org.antlr.v4.misc.OrderedHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Measurements {

    private static final Map<String, Pair<String, Long>> MEASUREMENTS = new LinkedHashMap<>();

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
        MEASUREMENTS.put(name,
                new ImmutablePair<>("%s: %.2f %s".formatted(name, p.getLeft(), p.getRight()), time));
    }

    public static void add(String name, String subtractTime, long time) {
        time -= MEASUREMENTS.getOrDefault(subtractTime, new ImmutablePair<>("", 0L)).getRight();
        add(name, time);
    }

    public static void print() {
        System.out.println("--------------- MEASUREMENTS ---------------");
        for (var p : MEASUREMENTS.values()) {
            System.out.println(p.getLeft());
        }
        System.out.println("---------------");
        var p = calculateUnit(totalTime);
        System.out.printf("TOTAL: %s %s\n", p.getLeft(), p.getRight());
        System.out.println("--------------- MEASUREMENTS ---------------");

        MEASUREMENTS.clear();
        totalTime = 0;
    }
}
