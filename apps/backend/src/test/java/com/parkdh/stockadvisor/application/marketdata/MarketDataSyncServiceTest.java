package com.parkdh.stockadvisor.application.marketdata;

import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.us.YahooFinanceClient;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataSyncServiceTest {
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private PriceIntradayRepository priceIntradayRepository;
    @Mock
    private MarketUniverseRepository marketUniverseRepository;
    @Mock
    private KisApiClient kisApiClient;
    @Mock
    private StooqQuoteClient stooqQuoteClient;
    @Mock
    private YahooFinanceClient yahooFinanceClient;

    private MarketDataSyncService service;

    @BeforeEach
    void setUp() {
        service = new MarketDataSyncService(
                priceDailyRepository,
                priceIntradayRepository,
                marketUniverseRepository,
                kisApiClient,
                stooqQuoteClient,
                yahooFinanceClient
        );
    }

    @Test
    void syncDailyPricesSkipsTickerWhenLatestDailyAlreadyCoversDefaultTargetDate() {
        LocalDate targetDate = previousBusinessDay(LocalDate.now());
        MarketUniverseEntity aapl = universe("AAPL", "NASDAQ");
        PriceDailyEntity latest = daily("AAPL", "NASDAQ", targetDate);

        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull("NASDAQ", true))
                .thenReturn(List.of(aapl));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of(latest));

        var response = service.syncDailyPrices("NASDAQ", 10, 120);

        verify(yahooFinanceClient, never()).fetchDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class), any(Integer.class));
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.skippedUpToDateCount()).isEqualTo(1);
        assertThat(response.requestedTickerCount()).isZero();
        assertThat(response.upsertedCount()).isZero();
    }

    @Test
    void syncDailyPricesBootstrapsMissingTickerOnlyUntilDefaultTargetDate() {
        LocalDate targetDate = previousBusinessDay(LocalDate.now());
        MarketUniverseEntity aapl = universe("AAPL", "NASDAQ");
        YahooFinanceClient.YahooDailyPrice row = new YahooFinanceClient.YahooDailyPrice(
                "AAPL",
                targetDate,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(105000)
        );

        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull("NASDAQ", true))
                .thenReturn(List.of(aapl));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of());
        when(yahooFinanceClient.fetchDailyPrices("AAPL", targetDate.minusDays(30), targetDate, 30))
                .thenReturn(List.of(row));
        when(priceDailyRepository.findById(PriceDailyEntity.buildKey("NASDAQ", "AAPL", targetDate)))
                .thenReturn(Optional.empty());
        when(priceDailyRepository.save(any(PriceDailyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.syncDailyPrices("NASDAQ", 10, 30);

        verify(yahooFinanceClient).fetchDailyPrices("AAPL", targetDate.minusDays(30), targetDate, 30);
        assertThat(response.candidateCount()).isEqualTo(1);
        assertThat(response.skippedUpToDateCount()).isZero();
        assertThat(response.requestedTickerCount()).isEqualTo(1);
        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.upsertedCount()).isEqualTo(1);
    }

    private MarketUniverseEntity universe(String ticker, String market) {
        return new MarketUniverseEntity(ticker, market, ticker, "TECH", null, null, null, true, "TEST", null);
    }

    private PriceDailyEntity daily(String ticker, String market, LocalDate tradeDate) {
        return new PriceDailyEntity(
                ticker,
                market,
                tradeDate,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(105000),
                "TEST"
        );
    }

    private LocalDate previousBusinessDay(LocalDate date) {
        LocalDate cursor = date.minusDays(1);
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }
}
