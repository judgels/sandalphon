package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class LessonStatementUtils {
    private static final Map<String, String> DEFAULT_STATEMENTS = ImmutableMap.of(
            "id-ID",
                    "<h3>Deskripsi</h3>\n",
            "en-US",
                    "<h3>Description</h3>\n"
    );

    private LessonStatementUtils() {
        // prevent instantiation
    }

    public static String getDefaultStatement(String languageCode) {
        if (DEFAULT_STATEMENTS.containsKey(languageCode)) {
            return DEFAULT_STATEMENTS.get(languageCode);
        } else {
            return DEFAULT_STATEMENTS.get("en-US");
        }
    }
}
