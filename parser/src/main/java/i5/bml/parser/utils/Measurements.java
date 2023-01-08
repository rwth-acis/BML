package i5.bml.parser.utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;

public class Measurements {

    private static final Logger LOGGER = LoggerFactory.getLogger(Measurements.class);

    private static long nestedMeasurement = 0;

    public static <T> T measure(String name, Supplier<T> actionToMeasure) {

        var prev = nestedMeasurement;

        var start = System.currentTimeMillis();
        T returnValue = actionToMeasure.get();
        var end = System.currentTimeMillis();

        var post = nestedMeasurement;
        var time = end - start;
        nestedMeasurement += time;

        LOGGER.info("{} took {} ms", name, time - (post - prev));

        return returnValue;
    }

    public static void measure(String name, Runnable actionToMeasure) {
        var prev = nestedMeasurement;

        var start = System.currentTimeMillis();
        actionToMeasure.run();
        var end = System.currentTimeMillis();

        var post = nestedMeasurement;
        var time = end - start;
        nestedMeasurement += time;

        LOGGER.info("{} took {} ms", name, time - (post - prev));
    }
}
