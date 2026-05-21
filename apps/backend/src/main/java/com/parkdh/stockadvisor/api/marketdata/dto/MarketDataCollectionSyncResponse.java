package com.parkdh.stockadvisor.api.marketdata.dto;

import java.util.List;

public record MarketDataCollectionSyncResponse(
        String source,
        String market,
        String ticker,
        int fetchedCount,
        int savedCount,
        List<String> sampleKeys
) {
}
