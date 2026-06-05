package com.parkdh.stockadvisor.infrastructure.marketdata.kr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.KrxOpenApiProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KrxOpenApiClientTest {
    private final KrxOpenApiClient client = new KrxOpenApiClient(
            new KrxOpenApiProperties("test-key"),
            new ObjectMapper()
    );

    @Test
    void parseDailyTradeResponseConvertsKrxNumericFields() {
        String body = """
                {
                  "OutBlock_1": [
                    {
                      "BAS_DD": "20260530",
                      "ISU_CD": "005930",
                      "ISU_NM": "삼성전자",
                      "TDD_OPNPRC": "58,700",
                      "TDD_HGPRC": "59,200",
                      "TDD_LWPRC": "58,100",
                      "TDD_CLSPRC": "58,900",
                      "ACC_TRDVOL": "12,345,678",
                      "ACC_TRDVAL": "725,925,424,200",
                      "MKTCAP": "351,617,000,000,000"
                    }
                  ]
                }
                """;

        List<KrxOpenApiClient.KrxDailyTradeRow> rows = client.parseDailyTradeResponse(body, "KOSPI");

        assertThat(rows).hasSize(1);
        KrxOpenApiClient.KrxDailyTradeRow row = rows.get(0);
        assertThat(row.market()).isEqualTo("KOSPI");
        assertThat(row.ticker()).isEqualTo("005930");
        assertThat(row.name()).isEqualTo("삼성전자");
        assertThat(row.tradeDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(row.openPrice()).isEqualByComparingTo(BigDecimal.valueOf(58700));
        assertThat(row.highPrice()).isEqualByComparingTo(BigDecimal.valueOf(59200));
        assertThat(row.lowPrice()).isEqualByComparingTo(BigDecimal.valueOf(58100));
        assertThat(row.closePrice()).isEqualByComparingTo(BigDecimal.valueOf(58900));
        assertThat(row.volume()).isEqualByComparingTo(BigDecimal.valueOf(12345678));
        assertThat(row.turnover()).isEqualByComparingTo(BigDecimal.valueOf(725925424200L));
        assertThat(row.marketCap()).isEqualByComparingTo(new BigDecimal("351617000000000"));
    }

    @Test
    void parseDailyTradeResponseSkipsRowsWithoutPrices() {
        String body = """
                {
                  "OutBlock_1": [
                    {
                      "BAS_DD": "20260530",
                      "ISU_CD": "000001",
                      "ISU_NM": "거래정지",
                      "TDD_OPNPRC": "-",
                      "TDD_HGPRC": "-",
                      "TDD_LWPRC": "-",
                      "TDD_CLSPRC": "-",
                      "ACC_TRDVOL": "0",
                      "ACC_TRDVAL": "0"
                    }
                  ]
                }
                """;

        List<KrxOpenApiClient.KrxDailyTradeRow> rows = client.parseDailyTradeResponse(body, "KOSPI");

        assertThat(rows).isEmpty();
    }
}
