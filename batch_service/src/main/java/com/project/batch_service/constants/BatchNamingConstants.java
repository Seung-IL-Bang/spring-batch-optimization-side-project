package com.project.batch_service.constants;

public final class BatchNamingConstants {

    private BatchNamingConstants() {
    }

    public static final class Job {
        public static final String DAILY_SETTLE_JOB = "dailySettlementJob";
    }

    public static final class Step {

        // Step Names of DailySettlementJob
        public static final String PURCHASE_CONFIRMED_STEP = getStepName(Job.DAILY_SETTLE_JOB, "purchaseConfirmedStep");
        public static final String CREATE_DAILY_SETTLEMENT_STEP = getStepName(Job.DAILY_SETTLE_JOB, "createDailySettlementStep");
        public static final String PLUS_SETTLEMENT_STEP = getStepName(Job.DAILY_SETTLE_JOB, "plusSettlementStep");
        public static final String MINUS_SETTLEMENT_STEP = getStepName(Job.DAILY_SETTLE_JOB, "minusSettlementStep");
        public static final String AGGREGATE_SETTLEMENT_STEP = getStepName(Job.DAILY_SETTLE_JOB, "aggregateSettlementStep");
        public static final String AGGREGATE_SETTLEMENT_DETAIL_STEP = getStepName(Job.DAILY_SETTLE_JOB, "aggregateSettlementDetailStep");
        public static final String AGGREGATE_SETTLEMENT_STEP2 = getStepName(Job.DAILY_SETTLE_JOB, "aggregateSettlementStep2");


    }

    private static String getStepName(String jobName, String stepName) {
        return jobName + "_" + stepName;
    }

}
