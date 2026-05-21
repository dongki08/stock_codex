package com.parkdh.stockadvisor.application.recommendation; // 추천 애플리케이션 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이징 요청 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.math.RoundingMode; // 반올림 모드를 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class PricePredictor { // 추천 가격 산출기를 정의한다.
    private static final int VOLATILITY_LOOKBACK = 20; // 변동성 계산에 사용할 최대 일봉 수를 정의한다.

    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소 의존성을 보관한다.

    public PredictedRecommendation predict(RecommendationCandidate candidate, String term) { // 추천 후보와 기간으로 가격을 산출한다.
        List<PriceDailyEntity> history = getRecentHistory(candidate); // 최근 일봉 히스토리를 조회한다.
        BigDecimal entryPrice = resolveEntryPrice(candidate, history); // 진입 가격을 결정한다.
        BigDecimal volatilityPct = calculateVolatilityPct(history); // 최근 변동성을 계산한다.
        BigDecimal targetMultiplier = targetMultiplier(term, volatilityPct); // 목표가 배수를 계산한다.
        BigDecimal stopMultiplier = stopMultiplier(term, volatilityPct); // 손절가 배수를 계산한다.
        LocalDate expectedExitAt = "SHORT".equals(term) ? LocalDate.now().plusDays(5) : LocalDate.now().plusMonths(6); // 예상 청산일을 계산한다.
        String pricingMethod = history.size() >= 5 ? "volatility-v1" : "fallback-v1"; // 산출 방식을 정한다.
        return new PredictedRecommendation(entryPrice, entryPrice.multiply(targetMultiplier).setScale(4, RoundingMode.HALF_UP), entryPrice.multiply(stopMultiplier).setScale(4, RoundingMode.HALF_UP), expectedExitAt, pricingMethod); // 산출 결과를 반환한다.
    } // 추천 가격 산출을 종료한다.

    private List<PriceDailyEntity> getRecentHistory(RecommendationCandidate candidate) { // 후보의 최근 일봉 히스토리를 조회한다.
        return priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(candidate.market(), candidate.ticker(), PageRequest.of(0, VOLATILITY_LOOKBACK)).stream()
                .sorted(Comparator.comparing(PriceDailyEntity::getTradeDate)) // 계산을 위해 오래된 거래일부터 정렬한다.
                .toList(); // 목록으로 수집한다.
    } // 최근 일봉 히스토리 조회를 종료한다.

    private BigDecimal resolveEntryPrice(RecommendationCandidate candidate, List<PriceDailyEntity> history) { // 진입 가격을 결정한다.
        if (!history.isEmpty()) { // 일봉 히스토리가 있는지 확인한다.
            return history.get(history.size() - 1).getClosePrice().setScale(4, RoundingMode.HALF_UP); // 최근 종가를 진입 가격으로 반환한다.
        } // 일봉 히스토리 확인을 종료한다.
        if (candidate.lastPrice() != null) { // 후보군 최근 가격이 있는지 확인한다.
            return candidate.lastPrice().setScale(4, RoundingMode.HALF_UP); // 후보군 최근 가격을 반환한다.
        } // 후보군 가격 확인을 종료한다.
        int hash = Math.abs(candidate.ticker().hashCode()); // 티커 해시 값을 양수로 변환한다.
        if ("KOSPI".equals(candidate.market()) || "KOSDAQ".equals(candidate.market())) { // 한국 종목인지 확인한다.
            return BigDecimal.valueOf(50000L + hash % 90000L).setScale(4, RoundingMode.HALF_UP); // 한국 종목 fallback 가격을 반환한다.
        } // 한국 종목 확인을 종료한다.
        return BigDecimal.valueOf(100L + hash % 400L).setScale(4, RoundingMode.HALF_UP); // 미국 종목 fallback 가격을 반환한다.
    } // 진입 가격 결정을 종료한다.

    private BigDecimal calculateVolatilityPct(List<PriceDailyEntity> history) { // 최근 종가 변동성을 계산한다.
        if (history.size() < 5) { // 최소 일봉 개수가 있는지 확인한다.
            return BigDecimal.valueOf(0.02); // 데이터 부족 기본 변동성을 반환한다.
        } // 일봉 개수 확인을 종료한다.
        BigDecimal totalAbsMove = BigDecimal.ZERO; // 절대 수익률 합계를 초기화한다.
        int count = 0; // 변화량 개수를 초기화한다.
        for (int i = 1; i < history.size(); i++) { // 일봉 변화량을 순회한다.
            BigDecimal previous = history.get(i - 1).getClosePrice(); // 전일 종가를 가져온다.
            BigDecimal current = history.get(i).getClosePrice(); // 당일 종가를 가져온다.
            if (previous.compareTo(BigDecimal.ZERO) > 0) { // 전일 종가가 유효한지 확인한다.
                BigDecimal move = current.subtract(previous).divide(previous, 8, RoundingMode.HALF_UP).abs(); // 절대 수익률을 계산한다.
                totalAbsMove = totalAbsMove.add(move); // 절대 수익률을 더한다.
                count++; // 변화량 개수를 증가시킨다.
            } // 전일 종가 확인을 종료한다.
        } // 변화량 순회를 종료한다.
        if (count == 0) { // 유효 변화량이 없는지 확인한다.
            return BigDecimal.valueOf(0.02); // 기본 변동성을 반환한다.
        } // 변화량 개수 확인을 종료한다.
        return totalAbsMove.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP).max(BigDecimal.valueOf(0.01)).min(BigDecimal.valueOf(0.08)); // 변동성을 1~8% 범위로 제한해 반환한다.
    } // 최근 종가 변동성 계산을 종료한다.

    private BigDecimal targetMultiplier(String term, BigDecimal volatilityPct) { // 목표가 배수를 계산한다.
        if ("SHORT".equals(term)) { // 단기 추천인지 확인한다.
            return BigDecimal.ONE.add(volatilityPct.multiply(BigDecimal.valueOf(2.5)).max(BigDecimal.valueOf(0.035))); // 단기 목표가 배수를 반환한다.
        } // 단기 추천 확인을 종료한다.
        return BigDecimal.ONE.add(volatilityPct.multiply(BigDecimal.valueOf(8)).max(BigDecimal.valueOf(0.12))); // 장기 목표가 배수를 반환한다.
    } // 목표가 배수 계산을 종료한다.

    private BigDecimal stopMultiplier(String term, BigDecimal volatilityPct) { // 손절가 배수를 계산한다.
        if ("SHORT".equals(term)) { // 단기 추천인지 확인한다.
            return BigDecimal.ONE.subtract(volatilityPct.multiply(BigDecimal.valueOf(1.5)).max(BigDecimal.valueOf(0.025))); // 단기 손절가 배수를 반환한다.
        } // 단기 추천 확인을 종료한다.
        return BigDecimal.ONE.subtract(volatilityPct.multiply(BigDecimal.valueOf(4)).max(BigDecimal.valueOf(0.08))); // 장기 손절가 배수를 반환한다.
    } // 손절가 배수 계산을 종료한다.
} // 추천 가격 산출기를 종료한다.
