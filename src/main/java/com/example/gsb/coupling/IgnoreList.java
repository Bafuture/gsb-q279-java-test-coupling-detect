package com.example.gsb.coupling;

/**
 * 白名单（忽略清单）：已确认无害的耦合加入后不再参与告警。
 * 任一字段传 {@code null} 或 {@code "*"} 表示该维度通配。
 */
public final class IgnoreList {

    private final java.util.List<Rule> rules = new java.util.ArrayList<>();

    public static IgnoreList empty() {
        return new IgnoreList();
    }

    public IgnoreList ignore(String writerTest, String readerTest, StateKind kind, String stateName) {
        rules.add(new Rule(writerTest, readerTest, kind, stateName));
        return this;
    }

    public IgnoreList ignoreAllBetween(String writerTest, String readerTest) {
        return ignore(writerTest, readerTest, null, null);
    }

    public IgnoreList ignoreState(StateKey state) {
        return ignore(null, null, state.kind(), state.name());
    }

    public boolean isIgnored(CouplingPair pair) {
        for (Rule rule : rules) {
            if (rule.matches(pair)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return rules.size();
    }

    private record Rule(String writerTest, String readerTest, StateKind kind, String stateName) {

        private Rule {
            writerTest = normalize(writerTest);
            readerTest = normalize(readerTest);
            stateName = normalize(stateName);
        }

        private static String normalize(String value) {
            return (value == null || value.isBlank() || "*".equals(value)) ? null : value;
        }

        boolean matches(CouplingPair pair) {
            return matches(writerTest, pair.writerTest())
                    && matches(readerTest, pair.readerTest())
                    && (kind == null || kind == pair.state().kind())
                    && matches(stateName, pair.state().name());
        }

        private static boolean matches(String pattern, String actual) {
            return pattern == null || pattern.equals(actual);
        }
    }
}
