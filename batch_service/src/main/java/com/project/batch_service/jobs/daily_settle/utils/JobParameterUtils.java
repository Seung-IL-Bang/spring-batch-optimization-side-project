package com.project.batch_service.jobs.daily_settle.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class JobParameterUtils {

    private static final int CHUNK_SIZE = 1000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private JobParameterUtils() {
    }

    public static LocalDate parseSettlementDate(String settlementDateStr) {
        return settlementDateStr != null ? LocalDate.parse(settlementDateStr, DATE_FORMATTER) : LocalDate.parse(LocalDate.now().format(DATE_FORMATTER), DATE_FORMATTER);
    }

    public static int parseChunkSize(Integer integer) {
        return integer != null ? integer : CHUNK_SIZE;
    }
}
