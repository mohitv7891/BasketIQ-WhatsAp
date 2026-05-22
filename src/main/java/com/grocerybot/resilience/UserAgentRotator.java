package com.grocerybot.resilience;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UserAgentRotator {
    private static final List<String> USER_AGENTS = List.of(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:122.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0"
    );

    private static final AtomicInteger index = new AtomicInteger(0);

    public static String next() {
        return USER_AGENTS.get(Math.abs(index.getAndIncrement()) % USER_AGENTS.size());
    }
}
