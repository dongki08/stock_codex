package com.parkdh.stockadvisor.application.recommendation; // 추천 애플리케이션 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드 타입을 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.api.recommendation.dto.ExitConfirmResponse; // Exit Confirm 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.config.CodexCliProperties; // Codex CLI 설정을 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceIntradayEntity; // 장중 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 시장 공통 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient; // Codex CLI 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient.CodexResult; // Codex 호출 결과를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository; // 장중 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 앱 설정 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이지 요청 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.math.RoundingMode; // 반올림 모드를 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Optional; // 선택 값 타입을 가져온다.
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class ExitConfirmService { // Exit Confirm 서비스를 정의한다.
    private static final String DEFAULT_PROFILE = "stock-advisor"; // 기본 Codex profile을 정의한다.
    private static final Pattern ACTION_LINE_PATTERN = Pattern.compile("^ACTION:\\s*(HOLD|CUT|TIGHTEN)$"); // Codex 첫 줄 액션 형식을 정의한다.

    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.
    private final PriceIntradayRepository priceIntradayRepository; // 장중 가격 저장소 의존성을 보관한다.
    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소 의존성을 보관한다.
    private final AppSettingRepository appSettingRepository; // 앱 설정 저장소 의존성을 보관한다.
    private final CodexClient codexClient; // Codex CLI 클라이언트 의존성을 보관한다.
    private final CodexCliProperties codexCliProperties; // Codex CLI 설정 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서 의존성을 보관한다.

    public ExitConfirmResponse confirm(Long recommendationId) { // 추천 ID로 Exit Confirm을 수행한다.
        RecommendationEntity recommendation = recommendationRepository.findById(recommendationId).orElseThrow(() -> new CustomException("추천을 찾을 수 없습니다.", 404)); // 추천이 없으면 404 예외를 던진다.
        Optional<PricePoint> pricePoint = findLatestPrice(recommendation); // 최신 가격을 조회한다.
        if (pricePoint.isEmpty()) { // 가격 데이터가 없는지 확인한다.
            return buildDataRequired(recommendation); // 데이터 필요 응답을 반환한다.
        } // 가격 데이터 확인을 종료한다.
        PricePoint current = pricePoint.get(); // 최신 가격을 가져온다.
        BigDecimal riskBandPercent = getDecimalSetting("exit.riskBand.percent", BigDecimal.valueOf(2.0)); // 손절 위험 구간 폭을 조회한다.
        BigDecimal distancePct = calculateDistancePct(current.price(), recommendation.getStopPrice()); // 손절가 대비 이격률을 계산한다.
        boolean riskZone = isRiskZone(current.price(), recommendation.getStopPrice(), riskBandPercent); // 위험 구간 여부를 확인한다.
        if (!riskZone) { // 위험 구간이 아닌지 확인한다.
            return new ExitConfirmResponse(recommendation.getId(), recommendation.getTicker(), recommendation.getMarket(), current.price(), recommendation.getStopPrice(), distancePct, "HOLD", "현재가가 손절 위험 구간 밖입니다.", true, null, LocalDateTime.now()); // HOLD 응답을 반환한다.
        } // 위험 구간 확인을 종료한다.
        Optional<RuleDecision> ruleDecision = decideByRule(recommendation, current, distancePct); // 명확한 룰 판단이 가능한지 확인한다.
        if (ruleDecision.isPresent()) { // 룰 판단 결과가 있는지 확인한다.
            RuleDecision decision = ruleDecision.get(); // 룰 판단 결과를 가져온다.
            return new ExitConfirmResponse(recommendation.getId(), recommendation.getTicker(), recommendation.getMarket(), current.price(), recommendation.getStopPrice(), distancePct, decision.action(), decision.rationale(), false, null, LocalDateTime.now()); // Codex 호출 없이 룰 판단을 반환한다.
        } // 룰 판단 확인을 종료한다.
        String prompt = buildPrompt(recommendation, current, distancePct, riskBandPercent); // Codex 판단 프롬프트를 구성한다.
        CodexResult result = codexClient.call(prompt, resolveProfile(), "exit-confirm"); // Codex CLI를 호출한다.
        boolean usedFallback = MarketUtil.isDevPlaceholder(codexCliProperties.command()) || !result.succeeded(); // fallback 사용 여부를 정한다.
        String action = usedFallback ? fallbackAction(current.price(), recommendation.getStopPrice()) : parseAction(result.response()); // 판단 액션을 결정한다.
        String rationale = usedFallback ? fallbackRationale(action, distancePct) : result.response(); // 판단 근거를 결정한다.
        return new ExitConfirmResponse(recommendation.getId(), recommendation.getTicker(), recommendation.getMarket(), current.price(), recommendation.getStopPrice(), distancePct, action, rationale, usedFallback, result.errorMessage(), LocalDateTime.now()); // 판단 결과를 반환한다.
    } // 추천 ID 기반 Exit Confirm을 종료한다.

    private Optional<PricePoint> findLatestPrice(RecommendationEntity recommendation) { // 최신 가격을 조회한다.
        List<PriceIntradayEntity> intraday = priceIntradayRepository.findByMarketAndTickerOrderByTickAtDesc(recommendation.getMarket(), recommendation.getTicker(), PageRequest.of(0, 1)); // 최신 장중 가격을 조회한다.
        if (!intraday.isEmpty()) { // 장중 가격이 있는지 확인한다.
            PriceIntradayEntity latest = intraday.getFirst(); // 최신 장중 가격을 가져온다.
            return Optional.of(new PricePoint(latest.getPrice(), latest.getTickAt(), latest.getSource())); // 장중 가격 포인트를 반환한다.
        } // 장중 가격 확인을 종료한다.
        List<PriceDailyEntity> daily = priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(recommendation.getMarket(), recommendation.getTicker(), PageRequest.of(0, 1)); // 최신 일봉 가격을 조회한다.
        if (!daily.isEmpty()) { // 일봉 가격이 있는지 확인한다.
            PriceDailyEntity latest = daily.getFirst(); // 최신 일봉 가격을 가져온다.
            return Optional.of(new PricePoint(latest.getClosePrice(), latest.getTradeDate().atTime(15, 30), latest.getSource())); // 일봉 종가 포인트를 반환한다.
        } // 일봉 가격 확인을 종료한다.
        return Optional.empty(); // 가격 데이터가 없으면 빈 값을 반환한다.
    } // 최신 가격 조회를 종료한다.

    private ExitConfirmResponse buildDataRequired(RecommendationEntity recommendation) { // 가격 데이터 필요 응답을 만든다.
        return new ExitConfirmResponse(recommendation.getId(), recommendation.getTicker(), recommendation.getMarket(), null, recommendation.getStopPrice(), null, "DATA_REQUIRED", "최근 price_intraday 또는 price_daily 데이터가 없어 Exit Confirm을 수행할 수 없습니다.", true, null, LocalDateTime.now()); // 데이터 필요 응답을 반환한다.
    } // 가격 데이터 필요 응답 생성을 종료한다.

    private String buildPrompt(RecommendationEntity recommendation, PricePoint current, BigDecimal distancePct, BigDecimal riskBandPercent) { // Codex 판단 프롬프트를 만든다.
        return """
                아래 추천은 손절 위험 구간에 진입했습니다. HOLD, CUT, TIGHTEN 중 하나로 판단하세요.
                첫 줄은 반드시 ACTION: HOLD 또는 ACTION: CUT 또는 ACTION: TIGHTEN 형식으로 작성하세요.
                제공된 데이터 밖의 뉴스/공시는 추정하지 마세요.

                recommendationId=%d
                market=%s
                ticker=%s
                term=%s
                entryPrice=%s
                targetPrice=%s
                stopPrice=%s
                currentPrice=%s
                distancePct=%s
                riskBandPercent=%s
                priceSource=%s
                priceAt=%s
                confidence=%d
                signalsJson=%s
                """.formatted(
                recommendation.getId(),
                recommendation.getMarket(),
                recommendation.getTicker(),
                recommendation.getTerm(),
                recommendation.getEntryPrice(),
                recommendation.getTargetPrice(),
                recommendation.getStopPrice(),
                current.price(),
                distancePct,
                riskBandPercent,
                current.source(),
                current.priceAt(),
                recommendation.getConfidence(),
                recommendation.getSignalsJson()
        ); // Codex 판단 프롬프트를 반환한다.
    } // Codex 판단 프롬프트 생성을 종료한다.

    private boolean isRiskZone(BigDecimal currentPrice, BigDecimal stopPrice, BigDecimal riskBandPercent) { // 손절 위험 구간 여부를 확인한다.
        BigDecimal threshold = stopPrice.multiply(BigDecimal.ONE.add(riskBandPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))); // 위험 구간 상단 가격을 계산한다.
        return currentPrice.compareTo(threshold) <= 0; // 현재가가 위험 구간 상단 이하이면 true를 반환한다.
    } // 손절 위험 구간 확인을 종료한다.

    private BigDecimal calculateDistancePct(BigDecimal currentPrice, BigDecimal stopPrice) { // 손절가 대비 이격률을 계산한다.
        return currentPrice.subtract(stopPrice).divide(stopPrice, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_UP); // 이격률을 반환한다.
    } // 손절가 대비 이격률 계산을 종료한다.

    private String fallbackAction(BigDecimal currentPrice, BigDecimal stopPrice) { // 규칙 기반 fallback 액션을 결정한다.
        if (currentPrice.compareTo(stopPrice) <= 0) { // 손절가 이하인지 확인한다.
            return "CUT"; // 손절 액션을 반환한다.
        } // 손절가 이하 확인을 종료한다.
        return "TIGHTEN"; // 손절가 위 위험 구간이면 타이트닝 액션을 반환한다.
    } // 규칙 기반 fallback 액션 결정을 종료한다.

    private String fallbackRationale(String action, BigDecimal distancePct) { // 규칙 기반 fallback 근거를 만든다.
        if ("CUT".equals(action)) { // 손절 액션인지 확인한다.
            return "현재가가 손절가 이하입니다. 규칙 기반 fallback은 CUT을 제안합니다. distancePct=" + distancePct; // 손절 근거를 반환한다.
        } // 손절 액션 확인을 종료한다.
        return "현재가가 손절 위험 구간 안에 있습니다. 규칙 기반 fallback은 손절선을 더 촘촘히 관리하는 TIGHTEN을 제안합니다. distancePct=" + distancePct; // 타이트닝 근거를 반환한다.
    } // 규칙 기반 fallback 근거 생성을 종료한다.

    private Optional<RuleDecision> decideByRule(RecommendationEntity recommendation, PricePoint current, BigDecimal distancePct) {
        if (current.price().compareTo(recommendation.getStopPrice()) <= 0) {
            return Optional.of(new RuleDecision("CUT", "현재가가 손절가 이하입니다. 룰 기반으로 즉시 CUT을 제안합니다. distancePct=" + distancePct));
        }
        return Optional.empty();
    }

    private String parseAction(String response) { // Codex 응답에서 액션을 추출한다.
        String firstLine = response == null ? "" : response.lines().findFirst().orElse("").trim().toUpperCase(); // 첫 줄만 액션 후보로 사용한다.
        Matcher matcher = ACTION_LINE_PATTERN.matcher(firstLine); // 엄격한 ACTION 형식을 검사한다.
        if (matcher.matches()) { // 형식이 일치하는지 확인한다.
            return matcher.group(1); // 액션 값을 반환한다.
        } // 액션 형식 확인을 종료한다.
        return "HOLD"; // 기본 액션은 HOLD로 반환한다.
    } // Codex 응답 액션 추출을 종료한다.

    private BigDecimal getDecimalSetting(String key, BigDecimal defaultValue) { // 숫자 설정값을 조회한다.
        return appSettingRepository.findById(key)
                .map(setting -> extractDecimalValue(setting.getValueJson(), defaultValue))
                .orElse(defaultValue); // 설정이 없으면 기본값을 반환한다.
    } // 숫자 설정값 조회를 종료한다.

    private BigDecimal extractDecimalValue(String valueJson, BigDecimal defaultValue) { // 설정 JSON에서 숫자 value를 추출한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode node = objectMapper.readTree(valueJson); // 설정 JSON을 파싱한다.
            return node.path("value").decimalValue(); // value 숫자를 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            return defaultValue; // 파싱 실패 시 기본값을 반환한다.
        } // 예외 처리를 종료한다.
    } // 설정 JSON 숫자 추출을 종료한다.

    private String resolveProfile() { // Codex profile을 조회한다.
        return appSettingRepository.findById("codex.profile")
                .map(setting -> extractStringValue(setting.getValueJson(), DEFAULT_PROFILE))
                .orElse(DEFAULT_PROFILE); // 설정이 없으면 기본 profile을 반환한다.
    } // Codex profile 조회를 종료한다.

    private String extractStringValue(String valueJson, String defaultValue) { // 설정 JSON에서 문자열 value를 추출한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode node = objectMapper.readTree(valueJson); // 설정 JSON을 파싱한다.
            String value = node.path("value").asText(defaultValue); // value 문자열을 읽는다.
            return value.isBlank() ? defaultValue : value; // 빈 값이면 기본값을 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            return defaultValue; // 파싱 실패 시 기본값을 반환한다.
        } // 예외 처리를 종료한다.
    } // 설정 JSON 문자열 추출을 종료한다.

    private record PricePoint(BigDecimal price, LocalDateTime priceAt, String source) { // 판단 기준 가격 포인트를 정의한다.
    } // 판단 기준 가격 포인트를 종료한다.

    private record RuleDecision(String action, String rationale) {
    }
} // Exit Confirm 서비스를 종료한다.
