package com.parkdh.stockadvisor.application.marketdata; // 시장 데이터 애플리케이션 패키지를 선언한다.

import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailyResponse; // 일봉 가격 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse; // 일봉 가격 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.marketdata.dto.PriceIntradayResponse; // 장중 가격 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceIntradayEntity; // 장중 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient; // KIS API 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient; // Stooq 시세 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository; // 장중 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository; // 시장 유니버스 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이지 요청 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class MarketDataSyncService { // 시장 데이터 동기화 서비스를 정의한다.
    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소 의존성을 보관한다.
    private final PriceIntradayRepository priceIntradayRepository; // 장중 가격 저장소 의존성을 보관한다.
    private final MarketUniverseRepository marketUniverseRepository; // 시장 유니버스 저장소 의존성을 보관한다.
    private final KisApiClient kisApiClient; // KIS API 클라이언트 의존성을 보관한다.
    private final StooqQuoteClient stooqQuoteClient; // Stooq 시세 클라이언트 의존성을 보관한다.

    public List<PriceDailyResponse> getDailyPrices(String market, String ticker, Integer limit) { // 일봉 가격 목록을 조회한다.
        int safeLimit = normalizeLimit(limit, 100, 1000, "일봉 조회 limit"); // 조회 개수를 정규화한다.
        List<PriceDailyEntity> entities = findDailyPrices(market, ticker, safeLimit); // 조건에 맞는 일봉을 조회한다.
        return entities.stream().map(this::toResponse).toList(); // 응답 DTO로 변환해 반환한다.
    } // 일봉 가격 목록 조회를 종료한다.

    public List<PriceIntradayResponse> getIntradayPrices(String market, String ticker, Integer limit) { // 장중 가격 목록을 조회한다.
        int safeLimit = normalizeLimit(limit, 100, 1000, "장중 가격 조회 limit"); // 조회 개수를 정규화한다.
        List<PriceIntradayEntity> entities = findIntradayPrices(market, ticker, safeLimit); // 조건에 맞는 장중 가격을 조회한다.
        return entities.stream().map(this::toResponse).toList(); // 응답 DTO로 변환해 반환한다.
    } // 장중 가격 목록 조회를 종료한다.

    @Transactional // 일봉 가격 동기화를 쓰기 트랜잭션으로 처리한다.
    public PriceDailySyncResponse syncDailyPrices(String market, Integer limit, Integer days) { // 후보군의 일봉 가격을 동기화한다.
        String safeMarket = market == null || market.isBlank() ? "ALL" : market; // 시장 기본값을 정한다.
        int safeLimit = normalizeLimit(limit, 20, 500, "일봉 동기화 limit"); // 동기화 후보 수를 정규화한다.
        int safeDays = normalizeLimit(days, 120, 2000, "일봉 동기화 days"); // 조회 기간을 정규화한다.
        LocalDate to = LocalDate.now(); // 종료일을 오늘로 정한다.
        LocalDate from = to.minusDays(safeDays); // 시작일을 조회 기간 기준으로 정한다.
        List<MarketUniverseEntity> candidates = findCandidates(safeMarket).stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getTradable())) // 거래 가능한 후보만 남긴다.
                .sorted(Comparator.comparing(MarketUniverseEntity::getMarket).thenComparing(MarketUniverseEntity::getTicker)) // 시장과 티커 기준으로 정렬한다.
                .limit(safeLimit) // 요청한 후보 수만 남긴다.
                .toList(); // 후보 목록으로 수집한다.
        if (candidates.isEmpty()) { // 동기화 대상이 없는지 확인한다.
            throw new CustomException("일봉 가격을 동기화할 시장 후보군이 없습니다. 먼저 /api/dev/universe/seed 또는 /api/universe/sync/us-symbols를 실행하세요.", 404); // 후보군 없음 예외를 던진다.
        } // 후보군 확인을 종료한다.
        List<PriceDailyEntity> rows = candidates.stream()
                .flatMap(candidate -> fetchDailyPrices(candidate, from, to, safeDays).stream()) // 후보별 외부 일봉을 조회한다.
                .map(this::upsertDailyPrice) // 일봉을 저장 또는 갱신한다.
                .toList(); // 저장된 일봉 목록을 수집한다.
        candidates.forEach(candidate -> updateUniverseFromLatestPrice(candidate, rows)); // 최근 일봉으로 후보군 최근 가격을 갱신한다.
        List<String> sampleKeys = rows.stream().map(PriceDailyEntity::getPriceKey).limit(20).toList(); // 샘플 키 목록을 만든다.
        return new PriceDailySyncResponse(safeMarket, candidates.size(), rows.size(), rows.size(), sampleKeys); // 동기화 결과를 반환한다.
    } // 일봉 가격 동기화를 종료한다.

    @Transactional // 장중 가격 저장을 쓰기 트랜잭션으로 처리한다.
    public PriceIntradayResponse saveIntradayPrice(String ticker, String market, LocalDateTime tickAt, BigDecimal price, BigDecimal volume, String source) { // 장중 가격 스냅샷을 저장한다.
        validateIntradayPrice(ticker, market, tickAt, price, source); // 필수 값을 검증한다.
        LocalDateTime normalizedTickAt = PriceIntradayEntity.normalizeTickAt(tickAt); // 스냅샷 시각을 정규화한다.
        String key = PriceIntradayEntity.buildKey(market, ticker, normalizedTickAt); // 장중 가격 키를 만든다.
        PriceIntradayEntity entity = priceIntradayRepository.findById(key)
                .map(existing -> { // 같은 시각 스냅샷이 있으면 값을 갱신한다.
                    existing.update(price, volume, source); // 현재가와 거래량을 갱신한다.
                    return priceIntradayRepository.save(existing); // 갱신된 엔티티를 저장한다.
                })
                .orElseGet(() -> priceIntradayRepository.save(new PriceIntradayEntity(ticker, market, normalizedTickAt, price, volume, source))); // 신규 스냅샷을 저장한다.
        return toResponse(entity); // 저장 결과를 DTO로 반환한다.
    } // 장중 가격 스냅샷 저장을 종료한다.

    private List<PriceDailyEntity> findDailyPrices(String market, String ticker, int limit) { // 조건별 일봉 조회를 수행한다.
        PageRequest pageRequest = PageRequest.of(0, limit); // 조회 개수 제한을 만든다.
        boolean hasMarket = market != null && !market.isBlank() && !"ALL".equals(market); // 시장 조건 여부를 확인한다.
        boolean hasTicker = ticker != null && !ticker.isBlank(); // 종목 조건 여부를 확인한다.
        if (hasMarket && hasTicker) { // 시장과 종목 조건이 모두 있는지 확인한다.
            return priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(market, ticker, pageRequest); // 시장과 종목으로 조회한다.
        } // 시장과 종목 조건 확인을 종료한다.
        if (hasMarket) { // 시장 조건만 있는지 확인한다.
            return priceDailyRepository.findByMarketOrderByTradeDateDesc(market, pageRequest); // 시장으로 조회한다.
        } // 시장 조건 확인을 종료한다.
        if (hasTicker) { // 종목 조건만 있는지 확인한다.
            return priceDailyRepository.findByTickerOrderByTradeDateDesc(ticker, pageRequest); // 종목으로 조회한다.
        } // 종목 조건 확인을 종료한다.
        return priceDailyRepository.findAllByOrderByTradeDateDesc(pageRequest); // 조건이 없으면 전체를 조회한다.
    } // 조건별 일봉 조회를 종료한다.

    private List<PriceIntradayEntity> findIntradayPrices(String market, String ticker, int limit) { // 조건별 장중 가격 조회를 수행한다.
        PageRequest pageRequest = PageRequest.of(0, limit); // 조회 개수 제한을 만든다.
        boolean hasMarket = market != null && !market.isBlank() && !"ALL".equals(market); // 시장 조건 여부를 확인한다.
        boolean hasTicker = ticker != null && !ticker.isBlank(); // 종목 조건 여부를 확인한다.
        if (hasMarket && hasTicker) { // 시장과 종목 조건이 모두 있는지 확인한다.
            return priceIntradayRepository.findByMarketAndTickerOrderByTickAtDesc(market, ticker, pageRequest); // 시장과 종목으로 조회한다.
        } // 시장과 종목 조건 확인을 종료한다.
        if (hasMarket) { // 시장 조건만 있는지 확인한다.
            return priceIntradayRepository.findByMarketOrderByTickAtDesc(market, pageRequest); // 시장으로 조회한다.
        } // 시장 조건 확인을 종료한다.
        if (hasTicker) { // 종목 조건만 있는지 확인한다.
            return priceIntradayRepository.findByTickerOrderByTickAtDesc(ticker, pageRequest); // 종목으로 조회한다.
        } // 종목 조건 확인을 종료한다.
        return priceIntradayRepository.findAllByOrderByTickAtDesc(pageRequest); // 조건이 없으면 전체를 조회한다.
    } // 조건별 장중 가격 조회를 종료한다.

    private List<MarketUniverseEntity> findCandidates(String market) { // 시장 조건으로 후보군을 조회한다.
        if ("ALL".equals(market)) { // 전체 시장 요청인지 확인한다.
            return marketUniverseRepository.findByTradableAndDelistedAtIsNull(true); // 전체 거래 가능 후보군을 반환한다.
        } // 전체 시장 확인을 종료한다.
        return marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(market, true); // 특정 시장 후보군을 반환한다.
    } // 후보군 조회를 종료한다.

    private List<DailyPriceRow> fetchDailyPrices(MarketUniverseEntity candidate, LocalDate from, LocalDate to, int maxRows) { // 후보군별 외부 일봉을 조회한다.
        String market = candidate.getMarket(); // 시장 구분을 가져온다.
        if ("KOSPI".equals(market) || "KOSDAQ".equals(market)) { // 한국 시장인지 확인한다.
            return kisApiClient.fetchDailyPrices(candidate.getTicker(), from, to).stream()
                    .map(price -> new DailyPriceRow(candidate.getTicker(), market, price.tradeDate(), price.openPrice(), price.highPrice(), price.lowPrice(), price.closePrice(), price.volume(), price.turnover(), "KIS")) // KIS 일봉을 공통 행으로 변환한다.
                    .toList(); // 목록으로 수집한다.
        } // 한국 시장 확인을 종료한다.
        if ("NASDAQ".equals(market) || "NYSE".equals(market)) { // 미국 시장인지 확인한다.
            return stooqQuoteClient.fetchDailyPrices(candidate.getTicker(), from, to, maxRows).stream()
                    .map(price -> new DailyPriceRow(candidate.getTicker(), market, price.tradeDate(), price.openPrice(), price.highPrice(), price.lowPrice(), price.closePrice(), price.volume(), price.turnover(), "STOOQ")) // Stooq 일봉을 공통 행으로 변환한다.
                    .toList(); // 목록으로 수집한다.
        } // 미국 시장 확인을 종료한다.
        return List.of(); // 지원하지 않는 시장이면 빈 목록을 반환한다.
    } // 후보군별 외부 일봉 조회를 종료한다.

    private PriceDailyEntity upsertDailyPrice(DailyPriceRow row) { // 일봉 가격을 저장 또는 갱신한다.
        String key = PriceDailyEntity.buildKey(row.market(), row.ticker(), row.tradeDate()); // 일봉 가격 키를 만든다.
        return priceDailyRepository.findById(key)
                .map(existing -> { // 기존 일봉이 있으면 갱신한다.
                    existing.update(row.openPrice(), row.highPrice(), row.lowPrice(), row.closePrice(), row.volume(), row.turnover(), row.source()); // 가격 값을 갱신한다.
                    return priceDailyRepository.save(existing); // 갱신된 엔티티를 저장한다.
                })
                .orElseGet(() -> priceDailyRepository.save(new PriceDailyEntity(row.ticker(), row.market(), row.tradeDate(), row.openPrice(), row.highPrice(), row.lowPrice(), row.closePrice(), row.volume(), row.turnover(), row.source()))); // 신규 일봉을 저장한다.
    } // 일봉 가격 저장 또는 갱신을 종료한다.

    private void updateUniverseFromLatestPrice(MarketUniverseEntity candidate, List<PriceDailyEntity> rows) { // 최근 일봉으로 후보군 정보를 갱신한다.
        rows.stream()
                .filter(row -> row.getMarket().equals(candidate.getMarket()) && row.getTicker().equals(candidate.getTicker())) // 해당 후보군 일봉만 남긴다.
                .max(Comparator.comparing(PriceDailyEntity::getTradeDate)) // 가장 최근 거래일을 찾는다.
                .ifPresent(latest -> { // 최근 일봉이 있으면 후보군을 갱신한다.
                    candidate.update(candidate.getName(), candidate.getSector(), candidate.getMarketCap(), latest.getTurnover(), latest.getClosePrice(), candidate.getTradable(), latest.getSource(), latest.getTradeDate()); // 최근 가격과 거래대금을 반영한다.
                    marketUniverseRepository.save(candidate); // 갱신된 후보군을 저장한다.
                }); // 후보군 갱신을 종료한다.
    } // 후보군 최근 가격 갱신을 종료한다.

    // TASK-8: 지수 일봉 적재 — regime 필터가 참조하는 KOSPI·SPY 인덱스 적재
    @Transactional
    public int syncIndexDailyPrices(int days) { // 지수 일봉 데이터를 동기화하고 저장 건수를 반환한다.
        LocalDate to = LocalDate.now(); // 종료일을 오늘로 정한다.
        LocalDate from = to.minusDays(days); // 시작일을 조회 기간 기준으로 정한다.
        int saved = 0; // 저장 건수를 초기화한다.
        // KOSPI 지수: Stooq ^ks11 → market=KOSPI, ticker=KOSPI
        try {
            List<StooqQuoteClient.StooqDailyPrice> kosprices = stooqQuoteClient.fetchDailyPricesByStooqSymbol("^ks11", "KOSPI", from, to, days);
            for (StooqQuoteClient.StooqDailyPrice p : kosprices) {
                upsertDailyPrice(new DailyPriceRow("KOSPI", "KOSPI", p.tradeDate(), p.openPrice(), p.highPrice(), p.lowPrice(), p.closePrice(), p.volume(), p.turnover(), "STOOQ_INDEX"));
                saved++;
            }
        } catch (Exception e) {
            // 지수 조회 실패는 non-fatal — 로그 레벨에서 처리
        }
        // SPY: Stooq spy.us → market=NASDAQ, ticker=SPY
        try {
            List<StooqQuoteClient.StooqDailyPrice> spyPrices = stooqQuoteClient.fetchDailyPricesByStooqSymbol("spy.us", "SPY", from, to, days);
            for (StooqQuoteClient.StooqDailyPrice p : spyPrices) {
                upsertDailyPrice(new DailyPriceRow("SPY", "NASDAQ", p.tradeDate(), p.openPrice(), p.highPrice(), p.lowPrice(), p.closePrice(), p.volume(), p.turnover(), "STOOQ_INDEX"));
                saved++;
            }
        } catch (Exception e) {
            // 지수 조회 실패는 non-fatal
        }
        return saved; // 저장 건수를 반환한다.
    } // 지수 일봉 동기화를 종료한다.

    private int normalizeLimit(Integer value, int defaultValue, int maxValue, String label) { // 정수 파라미터를 정규화한다.
        int normalized = value == null ? defaultValue : value; // 값이 없으면 기본값을 사용한다.
        if (normalized < 1 || normalized > maxValue) { // 허용 범위를 벗어났는지 확인한다.
            throw new CustomException(label + "은 1~" + maxValue + " 사이여야 합니다.", 400); // 범위 예외를 던진다.
        } // 허용 범위 확인을 종료한다.
        return normalized; // 정규화된 값을 반환한다.
    } // 정수 파라미터 정규화를 종료한다.

    private void validateIntradayPrice(String ticker, String market, LocalDateTime tickAt, BigDecimal price, String source) { // 장중 가격 필수 값을 검증한다.
        if (ticker == null || ticker.isBlank()) { // 종목 코드가 비어 있는지 확인한다.
            throw new CustomException("장중 가격 ticker는 필수입니다.", 400); // 종목 코드 예외를 던진다.
        } // 종목 코드 확인을 종료한다.
        if (market == null || market.isBlank()) { // 시장 구분이 비어 있는지 확인한다.
            throw new CustomException("장중 가격 market은 필수입니다.", 400); // 시장 구분 예외를 던진다.
        } // 시장 구분 확인을 종료한다.
        if (tickAt == null) { // 스냅샷 시각이 없는지 확인한다.
            throw new CustomException("장중 가격 tickAt은 필수입니다.", 400); // 스냅샷 시각 예외를 던진다.
        } // 스냅샷 시각 확인을 종료한다.
        if (price == null) { // 현재가가 없는지 확인한다.
            throw new CustomException("장중 가격 price는 필수입니다.", 400); // 현재가 예외를 던진다.
        } // 현재가 확인을 종료한다.
        if (source == null || source.isBlank()) { // 출처가 비어 있는지 확인한다.
            throw new CustomException("장중 가격 source는 필수입니다.", 400); // 출처 예외를 던진다.
        } // 출처 확인을 종료한다.
    } // 장중 가격 필수 값 검증을 종료한다.

    private PriceDailyResponse toResponse(PriceDailyEntity entity) { // 일봉 가격 엔티티를 응답 DTO로 변환한다.
        return new PriceDailyResponse(entity.getPriceKey(), entity.getTicker(), entity.getMarket(), entity.getTradeDate(), entity.getOpenPrice(), entity.getHighPrice(), entity.getLowPrice(), entity.getClosePrice(), entity.getVolume(), entity.getTurnover(), entity.getSource()); // 일봉 가격 응답 DTO를 생성한다.
    } // 일봉 가격 DTO 변환을 종료한다.

    private PriceIntradayResponse toResponse(PriceIntradayEntity entity) { // 장중 가격 엔티티를 응답 DTO로 변환한다.
        return new PriceIntradayResponse(entity.getPriceKey(), entity.getTicker(), entity.getMarket(), entity.getTickAt(), entity.getPrice(), entity.getVolume(), entity.getSource()); // 장중 가격 응답 DTO를 생성한다.
    } // 장중 가격 DTO 변환을 종료한다.

    private record DailyPriceRow(String ticker, String market, LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice, BigDecimal volume, BigDecimal turnover, String source) { // 수집된 일봉 가격 행을 정의한다.
    } // 수집된 일봉 가격 행을 종료한다.
} // 시장 데이터 동기화 서비스를 종료한다.
