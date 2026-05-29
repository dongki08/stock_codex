package com.parkdh.stockadvisor.infrastructure.marketdata.fundamental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.DartProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DartFundamentalClientTest {
    private final DartFundamentalClient client = new DartFundamentalClient(new DartProperties("dart-key"), new ObjectMapper());

    @Test
    void parseFinancialRowsReturnsCurrentMetricsAndYoyGrowth() {
        String body = """
                {
                  "status":"000",
                  "list":[
                    {"account_nm":"매출액","thstrm_amount":"120,000","frmtrm_amount":"100,000","bsns_year":"2025","sj_div":"IS"},
                    {"account_nm":"영업이익","thstrm_amount":"18,000","frmtrm_amount":"15,000","bsns_year":"2025","sj_div":"IS"},
                    {"account_nm":"당기순이익","thstrm_amount":"9,000","frmtrm_amount":"10,000","bsns_year":"2025","sj_div":"IS"}
                  ]
                }
                """;

        var rows = client.parseFinancialRows(body, "005930", "KOSPI");

        assertThat(rows).extracting(DartFundamentalClient.DartFundamentalRow::metricName)
                .containsExactly(
                        "REVENUE",
                        "REVENUE_GROWTH_YOY",
                        "OPERATING_INCOME",
                        "OPERATING_INCOME_GROWTH_YOY",
                        "NET_INCOME",
                        "NET_INCOME_GROWTH_YOY"
                );
        assertThat(rows.get(0).metricValue()).isEqualByComparingTo("120000");
        assertThat(rows.get(1).metricValue()).isEqualByComparingTo("20.000000");
        assertThat(rows.get(5).metricValue()).isEqualByComparingTo("-10.000000");
        assertThat(rows).allSatisfy(row -> assertThat(row.source()).isEqualTo("DART_FNLTT_SINGLE"));
    }

    @Test
    void parseFinancialRowsSkipsGrowthWhenPreviousAmountIsZero() {
        String body = """
                {
                  "status":"000",
                  "list":[
                    {"account_nm":"매출액","thstrm_amount":"120,000","frmtrm_amount":"0","bsns_year":"2025","sj_div":"IS"}
                  ]
                }
                """;

        var rows = client.parseFinancialRows(body, "005930", "KOSPI");

        assertThat(rows).extracting(DartFundamentalClient.DartFundamentalRow::metricName)
                .containsExactly("REVENUE");
        assertThat(rows.get(0).metricValue()).isEqualByComparingTo(new BigDecimal("120000"));
    }
}
