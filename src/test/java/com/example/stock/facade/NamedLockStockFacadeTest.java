package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repo.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NamedLockStockFacadeTest {

    private static final Logger log = LoggerFactory.getLogger(NamedLockStockFacadeTest.class);

    @Autowired
    private NamedLockStockFacade namedLockStockFacade;

    @Autowired
    private StockRepository stockRepository;


    @BeforeEach
    public void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }


    @Test
    public void 동시에_100개의_요청() throws InterruptedException {
        int threadCnt = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        //100 개의 요청이 끝날 때까지 기다려야하므로 다른 스레드에서 수행중인 작업이 끝날 떄까지 대기시켜줌
        CountDownLatch latch = new CountDownLatch(threadCnt);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCnt; i++) {
            executorService.submit(() -> {
                try {
                    namedLockStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long end = System.currentTimeMillis();
        long result = end - start;

        log.info("처리 시간: {}", result / 1000.0);

        Stock stock = stockRepository.findById(1L).orElseThrow();
        // 100 - (1 * 100) = 0;
        assertEquals(0, stock.getQuantity());
    }



}