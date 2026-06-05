package com.parkdh.stockadvisor.api.ops;

import com.parkdh.stockadvisor.global.dto.ResultDto;
import com.parkdh.stockadvisor.scheduler.DailyPriceBackfillJob;
import com.parkdh.stockadvisor.scheduler.FeatureSnapshotJob;
import com.parkdh.stockadvisor.scheduler.KrxPreOpenJob;
import com.parkdh.stockadvisor.scheduler.UsPreOpenJob;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ops/jobs")
public class OpsJobTriggerController {
    private final KrxPreOpenJob krxPreOpenJob;
    private final UsPreOpenJob usPreOpenJob;
    private final DailyPriceBackfillJob dailyPriceBackfillJob;
    private final FeatureSnapshotJob featureSnapshotJob;

    @Operation(summary = "스케줄러 Job 수동 트리거", description = """
            지정한 Job을 즉시 한 번 실행한다. 응답은 즉시 반환되며, Job은 백그라운드에서 실행된다.
            완료 결과는 Telegram 알림으로 확인한다.
            **jobName 목록:**
            - `krx-preopen` : KRX 장전 추천 + 유니버스 동기화
            - `us-preopen` : 미국장 장전 추천 + 유니버스 동기화
            - `backfill-kr` : 한국 일봉 히스토리 백필 (KOSPI/KOSDAQ)
            - `backfill-us` : 미국 일봉 히스토리 백필 (NASDAQ/NYSE)
            - `feature-snapshot` : 전 종목 feature 스냅샷 저장
            """)
    @PostMapping("/{jobName}/trigger")
    public ResultDto<?> triggerJob(@PathVariable String jobName) {
        Runnable task = resolveJob(jobName);
        if (task == null) {
            return ResultDto.error(404, "알 수 없는 jobName: " + jobName + ". 허용값: krx-preopen, us-preopen, backfill-kr, backfill-us, feature-snapshot");
        }
        CompletableFuture.runAsync(() -> {
            log.info("OpsJobTriggerController 수동 트리거. jobName={}", jobName);
            try {
                task.run();
                log.info("OpsJobTriggerController 트리거 완료. jobName={}", jobName);
            } catch (Exception e) {
                log.error("OpsJobTriggerController 트리거 실패. jobName={}, error={}", jobName, e.getMessage(), e);
            }
        });
        return ResultDto.success(Map.of("jobName", jobName, "status", "TRIGGERED"));
    }

    private Runnable resolveJob(String jobName) {
        return switch (jobName) {
            case "krx-preopen" -> krxPreOpenJob::trigger;
            case "us-preopen" -> usPreOpenJob::trigger;
            case "backfill-kr" -> dailyPriceBackfillJob::triggerKrxBackfill;
            case "backfill-us" -> dailyPriceBackfillJob::triggerUsBackfill;
            case "feature-snapshot" -> featureSnapshotJob::trigger;
            default -> null;
        };
    }
}
