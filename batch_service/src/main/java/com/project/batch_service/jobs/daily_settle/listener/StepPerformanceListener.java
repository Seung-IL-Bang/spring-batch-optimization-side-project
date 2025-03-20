package com.project.batch_service.jobs.daily_settle.listener;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepPerformanceListener implements StepExecutionListener, ItemReadListener<OrderProductDTO>,
        ItemProcessListener<OrderProductDTO, DailySettlementDetail>, ItemWriteListener<DailySettlementDetail> {

    private long stepStartTime;
    private long readStartTime;
    private long processStartTime;
    private long writeStartTime;
    private int readCount;
    private int processCount;
    private int writeCount;
    private long totalReadTime;
    private long totalProcessTime;
    private long totalWriteTime;
    private int chunkSize;
    private int lastReadLogged;

    // Step 리스너 메서드
    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTime = System.currentTimeMillis();
        readCount = 0;
        processCount = 0;
        writeCount = 0;
        lastReadLogged = 0;
        totalReadTime = 0;
        totalProcessTime = 0;
        totalWriteTime = 0;
        chunkSize = stepExecution.getJobParameters().getLong("chunkSize", 1000L).intValue();
        log.info("Step 시작: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long stepEndTime = System.currentTimeMillis();
        long stepTotalTime = stepEndTime - stepStartTime;

        // 전체 통계 로깅
        log.info("Step 종료: {} (상태: {})", stepExecution.getStepName(), stepExecution.getStatus());
        log.info("총 처리 시간: {}ms", stepTotalTime);

        if (readCount > 0) {
            log.info("읽기 성능: 총 {}개 항목, 총 {}ms (항목당 평균 {:.2f}ms, 전체의 {:.2f}%)",
                    readCount, totalReadTime,
                    (double) totalReadTime / readCount,
                    (double) totalReadTime * 100 / stepTotalTime);
        }

        if (processCount > 0) {
            log.info("처리 성능: 총 {}개 항목, 총 {}ms (항목당 평균 {:.2f}ms, 전체의 {:.2f}%)",
                    processCount, totalProcessTime,
                    (double) totalProcessTime / processCount,
                    (double) totalProcessTime * 100 / stepTotalTime);
        }

        if (writeCount > 0) {
            log.info("쓰기 성능: 총 {}개 항목, 총 {}ms (항목당 평균 {:.2f}ms, 전체의 {:.2f}%)",
                    writeCount, totalWriteTime,
                    (double) totalWriteTime / writeCount,
                    (double) totalWriteTime * 100 / stepTotalTime);
        }

        // 기타 작업 시간 (트랜잭션 관리, 청크 처리 오버헤드 등)
        long otherTime = stepTotalTime - totalReadTime - totalProcessTime - totalWriteTime;
        log.info("기타 처리 시간: {}ms (전체의 {:.2f}%)",
                otherTime, (double) otherTime * 100 / stepTotalTime);

        return stepExecution.getExitStatus();
    }

    // Item Read 리스너 메서드
    @Override
    public void beforeRead() {
        // 청크의 첫 번째 항목을 읽기 시작할 때만 시간 측정 시작
        if (readCount % chunkSize == 0) {
            readStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public void afterRead(OrderProductDTO item) {
        readCount++;

        // 청크의 마지막 항목을 읽었거나, 청크 크기의 배수일 때만 시간 측정 및 로깅
        if (readCount % chunkSize == 0) {
            long readEndTime = System.currentTimeMillis();
            long readTime = readEndTime - readStartTime;
            totalReadTime += readTime;

            log.info("읽기 완료: 항목 {} ~ {} ({}개), 소요 시간: {}ms",
                    readCount - chunkSize + 1, readCount, chunkSize, readTime);
            lastReadLogged = readCount;
        }
    }

    @Override
    public void onReadError(Exception ex) {
        log.error("읽기 오류: {}", ex.getMessage(), ex);
    }

    // Item Process 리스너 메서드
    @Override
    public void beforeProcess(OrderProductDTO item) {
        // 청크의 첫 번째 항목을 처리 시작할 때만 시간 측정 시작
        if (processCount % chunkSize == 0) {
            processStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public void afterProcess(OrderProductDTO item, DailySettlementDetail result) {
        processCount++;

        // 청크의 마지막 항목을 처리했거나, 청크 크기의 배수일 때만 시간 측정 및 로깅
        if (processCount % chunkSize == 0) {
            long processEndTime = System.currentTimeMillis();
            long processTime = processEndTime - processStartTime;
            totalProcessTime += processTime;

            log.info("처리 완료: 항목 {} ~ {} ({}개), 소요 시간: {}ms",
                    processCount - chunkSize + 1, processCount, chunkSize, processTime);
        }
    }

    @Override
    public void onProcessError(OrderProductDTO item, Exception ex) {
        log.error("처리 오류 (항목 ID: {}): {}", item.getOrderProductId(), ex.getMessage(), ex);
    }

    // Item Write 리스너 메서드
    @Override
    public void beforeWrite(Chunk<? extends DailySettlementDetail> items) {
        writeStartTime = System.currentTimeMillis();
    }

    @Override
    public void afterWrite(Chunk<? extends DailySettlementDetail> items) {
        long writeEndTime = System.currentTimeMillis();
        long writeTime = writeEndTime - writeStartTime;
        totalWriteTime += writeTime;
        int itemCount = items.size();
        writeCount += itemCount;

        log.info("쓰기 완료: {}개 항목, 소요 시간: {}ms (항목당 평균: {:.2f}ms)",
                itemCount, writeTime, (double) writeTime / itemCount);

        // 마지막으로 로깅된 읽기 작업 이후 추가로 읽은 항목이 있는 경우 (마지막 청크에서 발생 가능)
        if (readCount > lastReadLogged) {
            log.info("읽기 완료: 항목 {} ~ {} ({}개), 소요 시간: 측정되지 않음",
                    lastReadLogged + 1, readCount, readCount - lastReadLogged);
            lastReadLogged = readCount;
        }
    }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends DailySettlementDetail> items) {
        log.error("쓰기 오류 ({}개 항목): {}", items.size(), ex.getMessage(), ex);
    }
}