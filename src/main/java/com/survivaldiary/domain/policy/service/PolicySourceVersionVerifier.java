package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.YouthPolicyClient;
import com.survivaldiary.domain.policy.client.YouthPolicyResponseParser;
import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import com.survivaldiary.domain.policy.client.dto.YouthPolicySearchRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 지역별 조회에서 빠진 최신 동일 공고를 확인한다. 외부 신청 사이트를 크롤링하지 않는다. */
@Slf4j
final class PolicySourceVersionVerifier implements AutoCloseable {
    private final YouthPolicyClient client;
    private final YouthPolicyResponseParser parser;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            4, 4, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32), runnable -> {
                Thread thread = new Thread(runnable, "policy-version-lookup");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    PolicySourceVersionVerifier(YouthPolicyClient client, YouthPolicyResponseParser parser) {
        this.client = client;
        this.parser = parser;
    }

    List<YouthPolicyItem> verify(List<YouthPolicyItem> items, boolean detailLookup) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        List<Future<List<YouthPolicyItem>>> lookups = new ArrayList<>();
        int unverified = 0;
        for (YouthPolicyItem item : items) {
            String key = PolicyDuplicateResolver.duplicateKey(item);
            if (key == null || (!detailLookup && !"0057002".equals(item.aplyPrdSeCd()))) {
                continue;
            }
            try {
                lookups.add(executor.submit(() -> parser.parseItems(client.search(
                                new YouthPolicySearchRequest(1, 100, null, null, null, null, item.plcyNm())))
                        .stream()
                        .filter(candidate -> key.equals(PolicyDuplicateResolver.duplicateKey(candidate)))
                        .toList()));
            } catch (RejectedExecutionException exception) {
                unverified++;
            }
        }
        List<YouthPolicyItem> verified = new ArrayList<>(items);
        for (Future<List<YouthPolicyItem>> lookup : lookups) {
            try {
                verified.addAll(lookup.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                unverified++;
            } catch (ExecutionException | TimeoutException exception) {
                unverified++;
            } finally {
                if (!lookup.isDone()) {
                    lookup.cancel(true);
                }
            }
        }
        executor.purge();
        if (unverified > 0) {
            log.warn("정책 최신 등록 확인 일부 생략: unverified={}, reason=LOOKUP_UNAVAILABLE_OR_TIME_BUDGET", unverified);
        }
        return verified;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
