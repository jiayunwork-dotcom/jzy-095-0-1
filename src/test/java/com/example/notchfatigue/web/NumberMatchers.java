package com.example.notchfatigue.web;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * 数值大小匹配器：对 {@link Number}（Integer/Long/Double/BigDecimal 等）统一按
 * doubleValue 比较，避免 JsonPath 返回的 BigDecimal 与 Double 匹配器互相转型失败。
 */
final class NumberMatchers {

    private NumberMatchers() {
    }

    static TypeSafeMatcher<Number> lessThan(final double expected) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Number item) {
                return item.doubleValue() < expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a number less than ").appendValue(expected);
            }
        };
    }

    static TypeSafeMatcher<Number> greaterThan(final double expected) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Number item) {
                return item.doubleValue() > expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a number greater than ").appendValue(expected);
            }
        };
    }
}
