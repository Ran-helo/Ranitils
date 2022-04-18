package io.github.ran.ranitils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class MatchingUtils {
    public static Stream<MatchResult> results(Matcher matcher) {
        List<MatchResult> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.toMatchResult());
        }
        return list.stream();
    }
}
