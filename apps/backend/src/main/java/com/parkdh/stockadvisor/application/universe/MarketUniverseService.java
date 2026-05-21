package com.parkdh.stockadvisor.application.universe; // 시장 유니버스 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.universe.dto.DevUniverseSeedResponse; // 개발용 seed 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.universe.dto.MarketUniverseResponse; // 시장 유니버스 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.universe.dto.MarketUniverseSyncResponse; // 시장 유니버스 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KrxSymbolClient; // KRX 심볼 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.marketdata.us.NasdaqTraderSymbolClient; // NASDAQ Trader 심볼 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient; // Stooq 시세 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository; // 시장 유니버스 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Map; // 맵 타입을 가져온다.
import java.util.stream.Collectors; // 스트림 수집 도구를 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class MarketUniverseService { // 시장 유니버스 서비스를 정의한다.
    private final MarketUniverseRepository marketUniverseRepository; // 시장 유니버스 저장소 의존성을 보관한다.
    private final KrxSymbolClient krxSymbolClient; // KRX 심볼 클라이언트 의존성을 보관한다.
    private final NasdaqTraderSymbolClient nasdaqTraderSymbolClient; // 미국 심볼 클라이언트 의존성을 보관한다.
    private final StooqQuoteClient stooqQuoteClient; // Stooq 시세 클라이언트 의존성을 보관한다.

    public List<MarketUniverseResponse> getUniverse(String market, Boolean tradable, BigDecimal minMarketCap, BigDecimal minAvgTurnover, BigDecimal minLastPrice) { // 시장 유니버스 목록을 조회한다.
        List<MarketUniverseEntity> entities = findBaseUniverse(market, tradable); // 기본 조건으로 후보군을 조회한다.
        return entities.stream()
                .filter(entity -> matchesMin(entity.getMarketCap(), minMarketCap)) // 시가총액 하한 조건을 적용한다.
                .filter(entity -> matchesMin(entity.getAvgTurnover(), minAvgTurnover)) // 평균 거래대금 하한 조건을 적용한다.
                .filter(entity -> matchesMin(entity.getLastPrice(), minLastPrice)) // 최근 가격 하한 조건을 적용한다.
                .sorted(Comparator.comparing(MarketUniverseEntity::getMarket).thenComparing(MarketUniverseEntity::getTicker)) // 시장과 티커 기준으로 정렬한다.
                .map(this::toResponse) // 응답 DTO로 변환한다.
                .toList(); // 목록으로 수집한다.
    } // 시장 유니버스 목록 조회를 종료한다.

    @Transactional // 개발용 seed 저장을 쓰기 트랜잭션으로 처리한다.
    public DevUniverseSeedResponse seedDevUniverse(String market) { // 개발용 후보군을 저장한다.
        String safeMarket = market == null || market.isBlank() ? "ALL" : market; // 시장 조건 기본값을 정한다.
        List<MarketUniverseEntity> seedCandidates = buildSeedCandidates().stream()
                .filter(candidate -> "ALL".equals(safeMarket) || candidate.getMarket().equals(safeMarket)) // 선택 시장만 남긴다.
                .toList(); // seed 후보 목록으로 수집한다.
        if (seedCandidates.isEmpty()) { // 지원하지 않는 시장인지 확인한다.
            throw new CustomException("개발용 후보군을 만들 시장이 없습니다.", 404); // 후보군 없음 예외를 던진다.
        } // 시장 확인을 종료한다.
        List<MarketUniverseEntity> saved = seedCandidates.stream().map(this::upsert).toList(); // 후보군을 저장 또는 갱신한다.
        List<String> universeKeys = saved.stream().map(MarketUniverseEntity::getUniverseKey).toList(); // 저장된 키 목록을 만든다.
        return new DevUniverseSeedResponse(safeMarket, saved.size(), universeKeys); // seed 결과를 반환한다.
    } // 개발용 후보군 저장을 종료한다.

    @Transactional // 미국 심볼 동기화를 쓰기 트랜잭션으로 처리한다.
    public MarketUniverseSyncResponse syncUsSymbols(String market) { // 미국 공개 심볼을 동기화한다.
        String safeMarket = market == null || market.isBlank() ? "ALL" : market; // 시장 조건 기본값을 정한다.
        List<NasdaqTraderSymbolClient.UsSymbol> symbols = fetchUsSymbols(safeMarket); // 대상 미국 심볼 목록을 가져온다.
        if (symbols.isEmpty()) { // 동기화할 심볼이 없는지 확인한다.
            throw new CustomException("동기화할 미국 심볼이 없습니다.", 404); // 심볼 없음 예외를 던진다.
        } // 심볼 확인을 종료한다.
        LocalDate today = LocalDate.now(); // 현재 날짜를 구한다.
        Map<String, MarketUniverseEntity> existingMap = marketUniverseRepository.findAll().stream() // 기존 universe 전체를 Map으로 로딩한다.
                .collect(Collectors.toMap(MarketUniverseEntity::getUniverseKey, e -> e)); // 키-엔티티 맵을 생성한다.
        List<MarketUniverseEntity> toSave = symbols.stream()
                .map(symbol -> { // 각 심볼을 신규 또는 기존 엔티티로 변환한다.
                    String key = MarketUniverseEntity.buildKey(symbol.market(), symbol.ticker()); // 유니버스 키를 생성한다.
                    MarketUniverseEntity existing = existingMap.get(key); // 기존 엔티티를 조회한다.
                    if (existing != null) { // 기존 엔티티가 있으면 갱신한다.
                        existing.update(symbol.name(), null, null, null, null, symbol.tradable(), "NASDAQ_TRADER", today); // 기존 값을 갱신한다.
                        return existing; // 갱신된 엔티티를 반환한다.
                    } // 기존 엔티티 확인을 종료한다.
                    return new MarketUniverseEntity(symbol.ticker(), symbol.market(), symbol.name(), null, null, null, null, symbol.tradable(), "NASDAQ_TRADER", today); // 신규 엔티티를 생성한다.
                })
                .toList(); // 저장 대상 목록으로 수집한다.
        List<MarketUniverseEntity> saved = marketUniverseRepository.saveAll(toSave); // 신규·갱신 대상을 일괄 저장한다.
        List<String> sampleKeys = saved.stream().map(MarketUniverseEntity::getUniverseKey).limit(20).toList(); // 샘플 키 목록을 만든다.
        return new MarketUniverseSyncResponse("NASDAQ_TRADER", safeMarket, symbols.size(), saved.size(), sampleKeys); // 동기화 결과를 반환한다.
    } // 미국 공개 심볼 동기화를 종료한다.

    @Transactional // 한국 심볼 동기화를 쓰기 트랜잭션으로 처리한다.
    public MarketUniverseSyncResponse syncKrSymbols(String market) { // 한국 공개 심볼을 동기화한다.
        String safeMarket = market == null || market.isBlank() ? "ALL" : market; // 시장 조건 기본값을 정한다.
        List<KrxSymbolClient.KrxSymbol> symbols = fetchKrSymbols(safeMarket); // 대상 한국 심볼 목록을 가져온다.
        if (symbols.isEmpty()) { // 동기화할 심볼이 없는지 확인한다.
            throw new CustomException("동기화할 한국 심볼이 없습니다.", 404); // 심볼 없음 예외를 던진다.
        } // 심볼 확인을 종료한다.
        LocalDate today = LocalDate.now(); // 현재 날짜를 구한다.
        Map<String, MarketUniverseEntity> existingMap = marketUniverseRepository.findAll().stream() // 기존 universe 전체를 Map으로 로딩한다.
                .collect(Collectors.toMap(MarketUniverseEntity::getUniverseKey, e -> e)); // 키-엔티티 맵을 생성한다.
        List<MarketUniverseEntity> toSave = symbols.stream()
                .map(symbol -> { // 각 심볼을 신규 또는 기존 엔티티로 변환한다.
                    String key = MarketUniverseEntity.buildKey(symbol.market(), symbol.ticker()); // 유니버스 키를 생성한다.
                    MarketUniverseEntity existing = existingMap.get(key); // 기존 엔티티를 조회한다.
                    if (existing != null) { // 기존 엔티티가 있으면 갱신한다.
                        existing.update(symbol.name(), existing.getSector(), existing.getMarketCap(), existing.getAvgTurnover(), existing.getLastPrice(), symbol.tradable(), "KIND", today); // 기존 가격 데이터는 유지하고 종목 정보를 갱신한다.
                        return existing; // 갱신된 엔티티를 반환한다.
                    } // 기존 엔티티 확인을 종료한다.
                    return new MarketUniverseEntity(symbol.ticker(), symbol.market(), symbol.name(), null, null, null, null, symbol.tradable(), "KIND", today); // 신규 엔티티를 생성한다.
                })
                .toList(); // 저장 대상 목록으로 수집한다.
        List<MarketUniverseEntity> saved = marketUniverseRepository.saveAll(toSave); // 신규·갱신 대상을 일괄 저장한다.
        List<String> sampleKeys = saved.stream().map(MarketUniverseEntity::getUniverseKey).limit(20).toList(); // 샘플 키 목록을 만든다.
        return new MarketUniverseSyncResponse("KIND", safeMarket, symbols.size(), saved.size(), sampleKeys); // 동기화 결과를 반환한다.
    } // 한국 공개 심볼 동기화를 종료한다.

    @Transactional // 미국 시세 동기화를 쓰기 트랜잭션으로 처리한다.
    public MarketUniverseSyncResponse syncUsPrices(String market, Integer limit) { // 미국 후보군 시세를 동기화한다.
        String safeMarket = market == null || market.isBlank() ? "ALL" : market; // 시장 조건 기본값을 정한다.
        int safeLimit = normalizeSyncLimit(limit); // 동기화 개수를 정규화한다.
        List<MarketUniverseEntity> candidates = getUsUniverseCandidates(safeMarket).stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getTradable())) // 거래 가능한 후보만 남긴다.
                .sorted(Comparator.comparing(MarketUniverseEntity::getTicker)) // 티커 기준으로 정렬한다.
                .limit(safeLimit) // 요청한 개수만 남긴다.
                .toList(); // 후보 목록으로 수집한다.
        if (candidates.isEmpty()) { // 시세를 조회할 후보가 없는지 확인한다.
            throw new CustomException("시세를 동기화할 미국 후보군이 없습니다. 먼저 미국 심볼 동기화를 실행하세요.", 404); // 후보군 없음 예외를 던진다.
        } // 후보 확인을 종료한다.
        List<MarketUniverseEntity> toUpdate = candidates.stream()
                .map(this::applyStooqQuote) // Stooq 시세로 후보를 메모리에서 갱신한다.
                .flatMap(java.util.Optional::stream) // 시세가 있는 후보만 남긴다.
                .toList(); // 갱신 대상 후보를 수집한다.
        List<MarketUniverseEntity> updated = marketUniverseRepository.saveAll(toUpdate); // 갱신된 후보를 일괄 저장한다.
        List<String> sampleKeys = updated.stream().map(MarketUniverseEntity::getUniverseKey).limit(20).toList(); // 샘플 키 목록을 만든다.
        return new MarketUniverseSyncResponse("STOOQ", safeMarket, candidates.size(), updated.size(), sampleKeys); // 시세 동기화 결과를 반환한다.
    } // 미국 후보군 시세 동기화를 종료한다.

    private List<MarketUniverseEntity> findBaseUniverse(String market, Boolean tradable) { // 기본 조건으로 후보군을 조회한다.
        if (market != null && !market.isBlank() && tradable != null) { // 시장과 거래 가능 여부가 모두 있는지 확인한다.
            return marketUniverseRepository.findByMarketAndTradable(market, tradable); // 시장과 거래 가능 여부로 조회한다.
        } // 두 조건 확인을 종료한다.
        if (market != null && !market.isBlank()) { // 시장 조건만 있는지 확인한다.
            return marketUniverseRepository.findByMarket(market); // 시장으로 조회한다.
        } // 시장 조건 확인을 종료한다.
        if (tradable != null) { // 거래 가능 조건만 있는지 확인한다.
            return marketUniverseRepository.findByTradable(tradable); // 거래 가능 여부로 조회한다.
        } // 거래 가능 조건 확인을 종료한다.
        return marketUniverseRepository.findAll(); // 조건이 없으면 전체 후보군을 조회한다.
    } // 기본 후보군 조회를 종료한다.

    private boolean matchesMin(BigDecimal actual, BigDecimal min) { // 숫자 하한 조건을 확인한다.
        return min == null || actual != null && actual.compareTo(min) >= 0; // 하한이 없거나 실제 값이 하한 이상이면 true를 반환한다.
    } // 숫자 하한 조건 확인을 종료한다.

    private MarketUniverseEntity upsert(MarketUniverseEntity candidate) { // 후보군을 저장 또는 갱신한다.
        return marketUniverseRepository.findById(candidate.getUniverseKey())
                .map(existing -> { // 기존 후보가 있으면 갱신한다.
                    existing.update(candidate.getName(), candidate.getSector(), candidate.getMarketCap(), candidate.getAvgTurnover(), candidate.getLastPrice(), candidate.getTradable(), candidate.getSource(), candidate.getLastSyncedAt()); // 기존 값을 갱신한다.
                    return marketUniverseRepository.save(existing); // 갱신된 후보를 저장한다.
                })
                .orElseGet(() -> marketUniverseRepository.save(candidate)); // 기존 후보가 없으면 새로 저장한다.
    } // 후보군 저장 또는 갱신을 종료한다.

    private java.util.Optional<MarketUniverseEntity> applyStooqQuote(MarketUniverseEntity entity) { // Stooq 시세로 후보군을 메모리에서 갱신한다.
        return stooqQuoteClient.fetchQuote(entity.getTicker()).map(quote -> { // Stooq 시세가 있으면 갱신한다.
            entity.update(entity.getName(), entity.getSector(), entity.getMarketCap(), quote.turnover(), quote.close(), true, "STOOQ", quote.quoteDate()); // 가격과 거래대금을 갱신한다.
            return entity; // 메모리에서 갱신된 엔티티를 반환한다(저장은 일괄 처리).
        }); // 갱신 결과를 반환한다.
    } // Stooq 시세 후보군 갱신을 종료한다.

    private List<MarketUniverseEntity> getUsUniverseCandidates(String market) { // 미국 시장 후보군을 조회한다.
        if ("NASDAQ".equals(market) || "NYSE".equals(market)) { // 특정 미국 시장인지 확인한다.
            return marketUniverseRepository.findByMarket(market); // 해당 시장 후보군을 반환한다.
        } // 특정 미국 시장 확인을 종료한다.
        if ("ALL".equals(market)) { // 전체 미국 시장인지 확인한다.
            return java.util.stream.Stream.concat(marketUniverseRepository.findByMarket("NASDAQ").stream(), marketUniverseRepository.findByMarket("NYSE").stream()).toList(); // NASDAQ과 NYSE 후보군을 합쳐 반환한다.
        } // 전체 미국 시장 확인을 종료한다.
        throw new CustomException("미국 시세 동기화는 NASDAQ, NYSE, ALL만 지원합니다.", 400); // 지원하지 않는 시장 예외를 던진다.
    } // 미국 시장 후보군 조회를 종료한다.

    private int normalizeSyncLimit(Integer limit) { // 시세 동기화 개수를 정규화한다.
        int value = limit == null ? 50 : limit; // 값이 없으면 기본값 50을 사용한다.
        if (value < 1 || value > 500) { // 허용 범위를 확인한다.
            throw new CustomException("시세 동기화 limit은 1~500 사이여야 합니다.", 400); // 범위 예외를 던진다.
        } // 허용 범위 확인을 종료한다.
        return value; // 정규화된 개수를 반환한다.
    } // 시세 동기화 개수 정규화를 종료한다.

    private List<NasdaqTraderSymbolClient.UsSymbol> fetchUsSymbols(String market) { // 시장 조건에 맞는 미국 심볼을 가져온다.
        if ("NASDAQ".equals(market)) { // NASDAQ만 요청했는지 확인한다.
            return nasdaqTraderSymbolClient.fetchNasdaqListed(); // NASDAQ 상장 심볼을 반환한다.
        } // NASDAQ 확인을 종료한다.
        if ("NYSE".equals(market)) { // NYSE만 요청했는지 확인한다.
            return nasdaqTraderSymbolClient.fetchNyseListed(); // NYSE 상장 심볼을 반환한다.
        } // NYSE 확인을 종료한다.
        if ("ALL".equals(market)) { // 전체 미국 시장을 요청했는지 확인한다.
            return java.util.stream.Stream.concat(nasdaqTraderSymbolClient.fetchNasdaqListed().stream(), nasdaqTraderSymbolClient.fetchNyseListed().stream()).toList(); // NASDAQ과 NYSE 심볼을 합쳐 반환한다.
        } // 전체 미국 시장 확인을 종료한다.
        throw new CustomException("미국 심볼 동기화는 NASDAQ, NYSE, ALL만 지원합니다.", 400); // 지원하지 않는 시장 예외를 던진다.
    } // 미국 심볼 조회를 종료한다.

    private List<KrxSymbolClient.KrxSymbol> fetchKrSymbols(String market) { // 시장 조건에 맞는 한국 심볼을 가져온다.
        if ("KOSPI".equals(market)) { // KOSPI만 요청했는지 확인한다.
            return krxSymbolClient.fetchKospiListed(); // KOSPI 상장 심볼을 반환한다.
        } // KOSPI 확인을 종료한다.
        if ("KOSDAQ".equals(market)) { // KOSDAQ만 요청했는지 확인한다.
            return krxSymbolClient.fetchKosdaqListed(); // KOSDAQ 상장 심볼을 반환한다.
        } // KOSDAQ 확인을 종료한다.
        if ("ALL".equals(market)) { // 전체 한국 시장을 요청했는지 확인한다.
            return java.util.stream.Stream.concat(krxSymbolClient.fetchKospiListed().stream(), krxSymbolClient.fetchKosdaqListed().stream()).toList(); // KOSPI와 KOSDAQ 심볼을 합쳐 반환한다.
        } // 전체 한국 시장 확인을 종료한다.
        throw new CustomException("한국 심볼 동기화는 KOSPI, KOSDAQ, ALL만 지원합니다.", 400); // 지원하지 않는 시장 예외를 던진다.
    } // 한국 심볼 조회를 종료한다.

    private List<MarketUniverseEntity> buildSeedCandidates() { // 개발용 seed 후보군을 만든다.
        LocalDate today = LocalDate.now(); // 현재 날짜를 구한다.
        return List.of(
                new MarketUniverseEntity("005930", "KOSPI", "삼성전자", "반도체", BigDecimal.valueOf(480_000_000_000_000L), BigDecimal.valueOf(900_000_000_000L), BigDecimal.valueOf(79000), true, "DEV", today), // 삼성전자 후보를 만든다.
                new MarketUniverseEntity("000660", "KOSPI", "SK하이닉스", "반도체", BigDecimal.valueOf(160_000_000_000_000L), BigDecimal.valueOf(650_000_000_000L), BigDecimal.valueOf(221000), true, "DEV", today), // SK하이닉스 후보를 만든다.
                new MarketUniverseEntity("035420", "KOSPI", "NAVER", "인터넷", BigDecimal.valueOf(32_000_000_000_000L), BigDecimal.valueOf(120_000_000_000L), BigDecimal.valueOf(198000), true, "DEV", today), // NAVER 후보를 만든다.
                new MarketUniverseEntity("035720", "KOSPI", "카카오", "인터넷", BigDecimal.valueOf(25_000_000_000_000L), BigDecimal.valueOf(90_000_000_000L), BigDecimal.valueOf(57000), true, "DEV", today), // 카카오 후보를 만든다.
                new MarketUniverseEntity("247540", "KOSDAQ", "에코프로비엠", "2차전지", BigDecimal.valueOf(18_000_000_000_000L), BigDecimal.valueOf(140_000_000_000L), BigDecimal.valueOf(185000), true, "DEV", today), // 에코프로비엠 후보를 만든다.
                new MarketUniverseEntity("091990", "KOSDAQ", "셀트리온헬스케어", "헬스케어", BigDecimal.valueOf(10_000_000_000_000L), BigDecimal.valueOf(80_000_000_000L), BigDecimal.valueOf(76000), true, "DEV", today), // 셀트리온헬스케어 후보를 만든다.
                new MarketUniverseEntity("AAPL", "NASDAQ", "Apple", "Technology", BigDecimal.valueOf(3_200_000_000_000L), BigDecimal.valueOf(8_000_000_000L), BigDecimal.valueOf(215), true, "DEV", today), // Apple 후보를 만든다.
                new MarketUniverseEntity("NVDA", "NASDAQ", "NVIDIA", "Semiconductor", BigDecimal.valueOf(2_900_000_000_000L), BigDecimal.valueOf(12_000_000_000L), BigDecimal.valueOf(118), true, "DEV", today), // NVIDIA 후보를 만든다.
                new MarketUniverseEntity("MSFT", "NASDAQ", "Microsoft", "Technology", BigDecimal.valueOf(3_000_000_000_000L), BigDecimal.valueOf(7_000_000_000L), BigDecimal.valueOf(425), true, "DEV", today), // Microsoft 후보를 만든다.
                new MarketUniverseEntity("JPM", "NYSE", "JPMorgan Chase", "Financials", BigDecimal.valueOf(580_000_000_000L), BigDecimal.valueOf(2_000_000_000L), BigDecimal.valueOf(205), true, "DEV", today), // JPMorgan 후보를 만든다.
                new MarketUniverseEntity("LLY", "NYSE", "Eli Lilly", "Healthcare", BigDecimal.valueOf(760_000_000_000L), BigDecimal.valueOf(2_800_000_000L), BigDecimal.valueOf(805), true, "DEV", today) // Eli Lilly 후보를 만든다.
        ); // 개발용 seed 후보군 목록을 반환한다.
    } // 개발용 seed 후보군 생성을 종료한다.

    private MarketUniverseResponse toResponse(MarketUniverseEntity entity) { // 시장 유니버스 엔티티를 응답 DTO로 변환한다.
        return new MarketUniverseResponse(entity.getUniverseKey(), entity.getTicker(), entity.getMarket(), entity.getName(), entity.getSector(), entity.getMarketCap(), entity.getAvgTurnover(), entity.getLastPrice(), entity.getTradable(), entity.getSource(), entity.getLastSyncedAt()); // 시장 유니버스 응답 DTO를 생성한다.
    } // 시장 유니버스 DTO 변환을 종료한다.
} // 시장 유니버스 서비스를 종료한다.
