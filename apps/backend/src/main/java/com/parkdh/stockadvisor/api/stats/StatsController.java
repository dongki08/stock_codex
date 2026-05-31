package com.parkdh.stockadvisor.api.stats;

import com.parkdh.stockadvisor.api.stats.dto.StatsDailyResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsStrategyResponse;
import com.parkdh.stockadvisor.application.stats.StatsService;
import com.parkdh.stockadvisor.global.dto.ResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/summary")
    public ResultDto<StatsSummaryResponse> getSummary() {
        return ResultDto.success(statsService.getSummary());
    }

    @GetMapping("/daily")
    public ResultDto<List<StatsDailyResponse>> getDaily() {
        return ResultDto.success(statsService.getDaily());
    }

    @GetMapping("/by-strategy")
    public ResultDto<List<StatsStrategyResponse>> getByStrategy() {
        return ResultDto.success(statsService.getByStrategy());
    }

    @GetMapping("/paper-trading")
    public ResultDto<StatsPaperTradingResponse> getPaperTrading() {
        return ResultDto.success(statsService.getPaperTrading());
    }
}
