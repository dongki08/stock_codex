package com.parkdh.stockadvisor.api.dev;

import com.parkdh.stockadvisor.api.brief.dto.DailyBriefResponse;
import com.parkdh.stockadvisor.application.brief.DailyBriefService;
import com.parkdh.stockadvisor.global.dto.ResultDto;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dev/brief")
public class DevBriefGenerateController {
    private final DailyBriefService dailyBriefService;

    @Operation(summary = "개발용 데일리 브리프 Codex 생성",
            description = """
                    최근 추천, 시장 후보군, 일봉 가격, 성과 통계를 컨텍스트로 묶어 Codex CLI를 호출하고 daily_brief 테이블에 저장한다.
                    dev-placeholder 모드에서는 같은 컨텍스트를 사용한 로컬 템플릿 브리프를 저장한다.
                    **요청 파라미터:**
                    - **marketTrack** : KRX / US / US_CLOSE
                    - **prompt** *(선택)* : 브리프에 반영할 추가 요청
                    """)
    @PostMapping("/generate")
    public ResultDto<DailyBriefResponse> generate(
            @RequestParam(defaultValue = "KRX") String marketTrack,
            @RequestParam(required = false) String prompt) {
        return ResultDto.success(dailyBriefService.generateDailyBrief(marketTrack, prompt));
    }
}
