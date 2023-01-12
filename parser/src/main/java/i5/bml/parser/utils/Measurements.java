package i5.bml.parser.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class Measurements {

    private static final Logger LOGGER = LoggerFactory.getLogger(Measurements.class);

    private static long nestedMeasurement = 0;

    private Measurements() {}

    public static <T> T measure(String name, Supplier<T> actionToMeasure) {
        var prev = nestedMeasurement;

        var start = System.nanoTime();
        T returnValue = actionToMeasure.get();
        var end = System.nanoTime();

        var post = nestedMeasurement;
        var time = end - start;
        nestedMeasurement += time;

        LOGGER.info("{} took {}", name, calculateUnit(time - (post - prev)));

        return returnValue;
    }

    public static void measure(String name, Runnable actionToMeasure) {
        var prev = nestedMeasurement;

        var start = System.nanoTime();
        actionToMeasure.run();
        var end = System.nanoTime();

        var post = nestedMeasurement;
        var time = end - start;
        nestedMeasurement += time;

        LOGGER.info("{} took {}", name, calculateUnit(time - (post - prev)));
    }

    private static String calculateUnit(long time) {
        if (time < 1_000) {
            return time + " ns";
        } else if (time < 1_000_000) {
            return "%.2f µs".formatted(time / 1_000d);
        } else if (time < 1_000_000_000) {
            return "%.2f ms".formatted(time / 1_000_000d);
        } else {
            return "%.2f s".formatted(time / 1_000_000_000d);
        }
    }
}
