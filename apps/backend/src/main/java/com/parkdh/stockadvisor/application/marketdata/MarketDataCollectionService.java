package com.parkdh.stockadvisor.application.marketdata;

import com.parkdh.stockadvisor.api.marketdata.dto.DisclosureEventResponse;
import com.parkdh.stockadvisor.api.marketdata.dto.FundamentalMetricResponse;
import com.parkdh.stockadvisor.api.marketdata.dto.MacroObservationResponse;
import com.parkdh.stockadvisor.api.marketdata.dto.MarketDataCollectionSyncResponse;
import com.parkdh.stockadvisor.api.marketdata.dto.NewsArticleResponse;
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity;
import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.global.exception.CustomException;
import com.parkdh.stockadvisor.infrastructure.marketdata.disclosure.DisclosureClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.fundamental.SecFundamentalClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.macro.FredMacroClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.news.RssNewsClient;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MarketDataCollectionService {
    private final NewsArticleRepository newsArticleRepository;
    private final DisclosureEventRepository disclosureEventRepository;
    private final MacroObservationRepository macroObservationRepository;
    private final FundamentalMetricRepository fundamentalMetricRepository;
    private final RssNewsClient rssNewsClient;
    private final DisclosureClient disclosureClient;
    private final FredMacroClient fredMacroClient;
    private final SecFundamentalClient secFundamentalClient;
    private final MarketSignalScorer marketSignalScorer;

    public List<NewsArticleResponse> getNewsArticles(String market, String ticker, Integer limit) {
        int safeLimit = normalizeLimit(limit, 100, 1000, "news limit");
        PageRequest pageRequest = PageRequest.of(0, safeLimit);
        boolean hasMarket = hasValue(market) && !"ALL".equals(market);
        boolean hasTicker = hasValue(ticker);
        List<NewsArticleEntity> entities;
        if (hasMarket && hasTicker) {
            entities = newsArticleRepository.findByMarketAndTickerOrderByPublishedAtDesc(market, ticker, pageRequest);
        } else if (hasMarket) {
            entities = newsArticleRepository.findByMarketOrderByPublishedAtDesc(market, pageRequest);
        } else if (hasTicker) {
            entities = newsArticleRepository.findByTickerOrderByPublishedAtDesc(ticker, pageRequest);
        } else {
            entities = newsArticleRepository.findAllByOrderByPublishedAtDesc(pageRequest);
        }
        return entities.stream().map(this::toResponse).toList();
    }

    public List<DisclosureEventResponse> getDisclosureEvents(String market, String ticker, Integer limit) {
        int safeLimit = normalizeLimit(limit, 100, 1000, "disclosure limit");
        PageRequest pageRequest = PageRequest.of(0, safeLimit);
        boolean hasMarket = hasValue(market) && !"ALL".equals(market);
        boolean hasTicker = hasValue(ticker);
        List<DisclosureEventEntity> entities;
        if (hasMarket && hasTicker) {
            entities = disclosureEventRepository.findByMarketAndTickerOrderByDisclosedAtDesc(market, ticker, pageRequest);
        } else if (hasMarket) {
            entities = disclosureEventRepository.findByMarketOrderByDisclosedAtDesc(market, pageRequest);
        } else if (hasTicker) {
            entities = disclosureEventRepository.findByTickerOrderByDisclosedAtDesc(ticker, pageRequest);
        } else {
            entities = disclosureEventRepository.findAllByOrderByDisclosedAtDesc(pageRequest);
        }
        return entities.stream().map(this::toResponse).toList();
    }

    public List<MacroObservationResponse> getMacroObservations(String seriesId, Integer limit) {
        int safeLimit = normalizeLimit(limit, 100, 1000, "macro limit");
        PageRequest pageRequest = PageRequest.of(0, safeLimit);
        List<MacroObservationEntity> entities = hasValue(seriesId)
                ? macroObservationRepository.findBySeriesIdOrderByObservedDateDesc(seriesId, pageRequest)
                : macroObservationRepository.findAllByOrderByObservedDateDesc(pageRequest);
        return entities.stream().map(this::toResponse).toList();
    }

    public List<FundamentalMetricResponse> getFundamentalMetrics(String market, String ticker, Integer limit) {
        int safeLimit = normalizeLimit(limit, 100, 1000, "fundamental limit");
        PageRequest pageRequest = PageRequest.of(0, safeLimit);
        boolean hasMarket = hasValue(market) && !"ALL".equals(market);
        boolean hasTicker = hasValue(ticker);
        List<FundamentalMetricEntity> entities;
        if (hasMarket && hasTicker) {
            entities = fundamentalMetricRepository.findByMarketAndTickerOrderByPeriodEndDesc(market, ticker, pageRequest);
        } else if (hasMarket) {
            entities = fundamentalMetricRepository.findByMarketOrderByPeriodEndDesc(market, pageRequest);
        } else if (hasTicker) {
            entities = fundamentalMetricRepository.findByTickerOrderByPeriodEndDesc(ticker, pageRequest);
        } else {
            entities = fundamentalMetricRepository.findAllByOrderByPeriodEndDesc(pageRequest);
        }
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MarketDataCollectionSyncResponse syncNewsArticles(String market, String ticker, Integer limit) {
        String safeMarket = defaultMarket(market);
        int safeLimit = normalizeLimit(limit, 20, 100, "news sync limit");
        List<RssNewsClient.NewsRow> rows = rssNewsClient.fetchNews(safeMarket, ticker, safeLimit);
        List<NewsArticleEntity> saved = rows.stream().map(this::upsertNews).toList();
        return new MarketDataCollectionSyncResponse("RSS", safeMarket, ticker, rows.size(), saved.size(), saved.stream().map(NewsArticleEntity::getArticleKey).limit(20).toList());
    }

    @Transactional
    public MarketDataCollectionSyncResponse syncDisclosureEvents(String market, String ticker, Integer limit) {
        String safeMarket = defaultMarket(market);
        int safeLimit = normalizeLimit(limit, 20, 100, "disclosure sync limit");
        List<DisclosureClient.DisclosureRow> rows = disclosureClient.fetchDisclosures(safeMarket, ticker, safeLimit);
        List<DisclosureEventEntity> saved = rows.stream().map(this::upsertDisclosure).toList();
        return new MarketDataCollectionSyncResponse("DISCLOSURE", safeMarket, ticker, rows.size(), saved.size(), saved.stream().map(DisclosureEventEntity::getDisclosureKey).limit(20).toList());
    }

    @Transactional
    public MarketDataCollectionSyncResponse syncMacroObservations(String seriesId, Integer limit) {
        int safeLimit = normalizeLimit(limit, 5, 100, "macro sync limit");
        List<FredMacroClient.MacroRow> rows = hasValue(seriesId)
                ? fredMacroClient.fetchSeries(seriesId, safeLimit)
                : fredMacroClient.fetchDefaultSeries(safeLimit);
        List<MacroObservationEntity> saved = rows.stream().map(this::upsertMacro).toList();
        return new MarketDataCollectionSyncResponse("FRED", null, seriesId, rows.size(), saved.size(), saved.stream().map(MacroObservationEntity::getObservationKey).limit(20).toList());
    }

    @Transactional
    public MarketDataCollectionSyncResponse syncFundamentalMetrics(String market, String ticker) {
        String safeMarket = market == null || market.isBlank() || "ALL".equals(market) ? "NASDAQ" : market;
        if (!"NASDAQ".equals(safeMarket) && !"NYSE".equals(safeMarket)) {
            return new MarketDataCollectionSyncResponse("SEC_COMPANYFACTS", safeMarket, ticker, 0, 0, List.of());
        }
        List<SecFundamentalClient.FundamentalRow> rows = secFundamentalClient.fetchFundamentals(ticker, safeMarket);
        List<FundamentalMetricEntity> saved = rows.stream().map(this::upsertFundamental).toList();
        return new MarketDataCollectionSyncResponse("SEC_COMPANYFACTS", safeMarket, ticker, rows.size(), saved.size(), saved.stream().map(FundamentalMetricEntity::getMetricKey).limit(20).toList());
    }

    private NewsArticleEntity upsertNews(RssNewsClient.NewsRow row) {
        String key = "NEWS:" + hash(row.source() + ":" + row.url());
        java.math.BigDecimal sentimentScore = marketSignalScorer.scoreNewsSentiment(row.title(), row.summary());
        return newsArticleRepository.findById(key)
                .map(existing -> {
                    existing.update(row.title(), row.publishedAt(), row.summary(), sentimentScore);
                    return newsArticleRepository.save(existing);
                })
                .orElseGet(() -> newsArticleRepository.save(new NewsArticleEntity(key, row.ticker(), row.market(), row.title(), row.url(), row.source(), row.publishedAt(), row.summary(), sentimentScore)));
    }

    private DisclosureEventEntity upsertDisclosure(DisclosureClient.DisclosureRow row) {
        String key = row.externalKey() == null || row.externalKey().isBlank() ? "DISC:" + hash(row.source() + ":" + row.url()) : row.externalKey();
        String disclosureType = marketSignalScorer.classifyDisclosureType(row.title(), row.disclosureType());
        Integer importanceScore = marketSignalScorer.scoreDisclosureImportance(row.title(), disclosureType);
        return disclosureEventRepository.findById(key)
                .map(existing -> {
                    existing.update(row.title(), row.url(), disclosureType, importanceScore, row.disclosedAt(), row.rawJson());
                    return disclosureEventRepository.save(existing);
                })
                .orElseGet(() -> disclosureEventRepository.save(new DisclosureEventEntity(key, row.ticker(), row.market(), row.title(), row.url(), row.source(), disclosureType, importanceScore, row.disclosedAt(), row.rawJson())));
    }

    private MacroObservationEntity upsertMacro(FredMacroClient.MacroRow row) {
        String key = "FRED:" + row.seriesId() + ":" + row.observedDate();
        return macroObservationRepository.findById(key)
                .map(existing -> {
                    existing.update(row.observedValue(), LocalDateTime.now());
                    return macroObservationRepository.save(existing);
                })
                .orElseGet(() -> macroObservationRepository.save(new MacroObservationEntity(key, row.seriesId(), row.seriesName(), row.observedDate(), row.observedValue(), row.source(), LocalDateTime.now())));
    }

    private FundamentalMetricEntity upsertFundamental(SecFundamentalClient.FundamentalRow row) {
        String periodKey = row.periodEnd() == null ? "NA" : row.periodEnd().toString();
        String key = "FUND:%s:%s:%s:%s".formatted(row.source(), row.market(), row.ticker(), row.metricName() + ":" + periodKey);
        return fundamentalMetricRepository.findById(key)
                .map(existing -> {
                    existing.update(row.metricValue(), row.unit(), row.fiscalYear(), row.fiscalPeriod(), row.periodEnd(), LocalDateTime.now());
                    return fundamentalMetricRepository.save(existing);
                })
                .orElseGet(() -> fundamentalMetricRepository.save(new FundamentalMetricEntity(key, row.ticker(), row.market(), row.metricName(), row.metricValue(), row.unit(), row.fiscalYear(), row.fiscalPeriod(), row.periodEnd(), row.source(), LocalDateTime.now())));
    }

    private NewsArticleResponse toResponse(NewsArticleEntity entity) {
        return new NewsArticleResponse(entity.getArticleKey(), entity.getTicker(), entity.getMarket(), entity.getTitle(), entity.getUrl(), entity.getSource(), entity.getPublishedAt(), entity.getSummary(), entity.getSentimentScore());
    }

    private DisclosureEventResponse toResponse(DisclosureEventEntity entity) {
        return new DisclosureEventResponse(entity.getDisclosureKey(), entity.getTicker(), entity.getMarket(), entity.getTitle(), entity.getUrl(), entity.getSource(), entity.getDisclosureType(), entity.getImportanceScore(), entity.getDisclosedAt());
    }

    private MacroObservationResponse toResponse(MacroObservationEntity entity) {
        return new MacroObservationResponse(entity.getObservationKey(), entity.getSeriesId(), entity.getSeriesName(), entity.getObservedDate(), entity.getObservedValue(), entity.getSource(), entity.getFetchedAt());
    }

    private FundamentalMetricResponse toResponse(FundamentalMetricEntity entity) {
        return new FundamentalMetricResponse(entity.getMetricKey(), entity.getTicker(), entity.getMarket(), entity.getMetricName(), entity.getMetricValue(), entity.getUnit(), entity.getFiscalYear(), entity.getFiscalPeriod(), entity.getPeriodEnd(), entity.getSource(), entity.getFetchedAt());
    }

    private int normalizeLimit(Integer value, int defaultValue, int maxValue, String label) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < 1 || normalized > maxValue) {
            throw new CustomException(label + " must be between 1 and " + maxValue + ".", 400);
        }
        return normalized;
    }

    private String defaultMarket(String market) {
        return market == null || market.isBlank() ? "ALL" : market;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toUnsignedString(text.hashCode());
        }
    }
}
