package com.parkdh.stockadvisor.application.marketdata;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class MarketSignalScorer {
    private static final List<String> POSITIVE_NEWS = List.of(
            "beat", "beats", "surge", "rally", "upgrade", "raises", "record", "profit", "growth", "contract", "approval",
            "호실적", "상승", "급등", "수주", "증가", "개선", "흑자", "승인", "상향", "최대"
    );
    private static final List<String> NEGATIVE_NEWS = List.of(
            "miss", "falls", "drop", "downgrade", "cuts", "loss", "probe", "lawsuit", "recall", "warning", "plunge",
            "하락", "급락", "적자", "감소", "부진", "소송", "리콜", "경고", "하향", "조사"
    );
    private static final List<String> HIGH_IMPORTANCE_DISCLOSURES = List.of(
            "8-k", "merger", "acquisition", "bankruptcy", "offering", "guidance", "earnings", "material", "restatement",
            "합병", "분할", "유상증자", "무상증자", "전환사채", "영업정지", "횡령", "배임", "상장폐지", "실적", "잠정"
    );
    private static final List<String> MEDIUM_IMPORTANCE_DISCLOSURES = List.of(
            "dividend", "director", "shareholder", "contract", "buyback", "appointment", "resignation",
            "배당", "주주총회", "계약", "자사주", "임원", "선임", "해임", "처분", "취득"
    );

    public BigDecimal scoreNewsSentiment(String title, String summary) {
        String text = normalize(title + " " + nullToEmpty(summary));
        int positive = countMatches(text, POSITIVE_NEWS);
        int negative = countMatches(text, NEGATIVE_NEWS);
        int raw = positive - negative;
        if (raw == 0) {
            return BigDecimal.ZERO.setScale(3);
        }
        double clipped = Math.max(-1.0, Math.min(1.0, raw * 0.35));
        return BigDecimal.valueOf(clipped).setScale(3, java.math.RoundingMode.HALF_UP);
    }

    public Integer scoreDisclosureImportance(String title, String disclosureType) {
        String text = normalize(title + " " + nullToEmpty(disclosureType));
        if (containsAny(text, HIGH_IMPORTANCE_DISCLOSURES)) {
            return 90;
        }
        if (containsAny(text, MEDIUM_IMPORTANCE_DISCLOSURES)) {
            return 70;
        }
        return 50;
    }

    public String classifyDisclosureType(String title, String originalType) {
        if (originalType != null && !originalType.isBlank() && !"8-K".equalsIgnoreCase(originalType)) {
            return originalType;
        }
        String text = normalize(title);
        if (containsAny(text, List.of("earnings", "실적", "잠정"))) {
            return "EARNINGS";
        }
        if (containsAny(text, List.of("merger", "acquisition", "합병", "인수"))) {
            return "M&A";
        }
        if (containsAny(text, List.of("offering", "유상증자", "전환사채", "cb", "bw"))) {
            return "FINANCING";
        }
        if (containsAny(text, List.of("dividend", "배당"))) {
            return "DIVIDEND";
        }
        if (containsAny(text, List.of("contract", "수주", "계약"))) {
            return "CONTRACT";
        }
        return originalType == null || originalType.isBlank() ? "GENERAL" : originalType;
    }

    private int countMatches(String text, List<String> words) {
        int count = 0;
        for (String word : words) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAny(String text, List<String> words) {
        return words.stream().anyMatch(text::contains);
    }

    private String normalize(String value) {
        return nullToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
