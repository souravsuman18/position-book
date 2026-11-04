package com.positionbook.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for producing application-focused exception diagnostics.
 */
public final class ExceptionUtil {

    /** Prevents instantiation of this utility class. */
    private ExceptionUtil() {
    }

    /**
     * Formats stack frames belonging to this application, omitting framework frames.
     *
     * @param exception exception whose stack trace should be summarized
     * @return application stack frames joined by {@code " at "}
     */
    public static String getCleanStackTrace(final Exception exception) {
        return Stream.of(exception.getStackTrace()).filter(ExceptionUtil::validateStackTraceElement)
                .map(Object::toString).collect(Collectors.joining(" at "));
    }

    /**
     * Determines whether a stack frame should appear in application diagnostics.
     *
     * @param stackTraceElement frame to inspect
     * @return {@code true} when the frame belongs to relevant application code
     */
    private static boolean validateStackTraceElement(final StackTraceElement stackTraceElement) {
        return stackTraceElement.getClassName().startsWith("com.positionbook")
                && !"<generated>".equalsIgnoreCase(stackTraceElement.getFileName())
                && !stackTraceElement.getClassName().contains("com.positionbook.filter");
    }
}