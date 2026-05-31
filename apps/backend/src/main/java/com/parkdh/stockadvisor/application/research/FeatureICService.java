package com.parkdh.stockadvisor.application.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.feature.FeatureSnapshotEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.feature.FeatureSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class FeatureICService {
    private static final int MIN_PAIRS = 3;
    private static final BigDecimal ZERO_IC_THRESHOLD = BigDecimal.valueOf(0.03);
    private static final Map<String, String> FEATURE_FIELDS_BY_WEIGHT_PATH = Map.ofEntries(
            Map.entry("value.liquidity", "liquidityScore"),
            Map.entry("value.price", "priceScore"),
            Map.entry("value.technical", "technicalScore"),
            Map.entry("value.context", "contextScore"),
            Map.entry("value.fundamental", "fundamentalScore"),
            Map.entry("value.dataQuality", "dataQualityScore"),
            Map.entry("technical.ma", "movingAverageScore"),
            Map.entry("technical.rsi", "rsiScore"),
            Map.entry("technical.volume", "volumeScore"),
            Map.entry("technical.macd", "macdScore"),
            Map.entry("technical.bollinger", "bollingerScore"),
            Map.entry("context.news", "newsScore"),
            Map.entry("context.disclosure", "disclosureScore"),
            Map.entry("context.macro", "macroScore"),
            Map.entry("context.fundamental", "fundamentalScore")
    );

    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final ObjectMapper objectMapper;

    public FeatureICReport measureLatest() {
        List<FeatureSnapshotEntity> snapshots = featureSnapshotRepository.findByFwdRet5dIsNotNullOrFwdRet20dIsNotNull();
        if (snapshots.isEmpty()) {
            return FeatureICReport.empty();
        }
        List<FeatureIC> results = new ArrayList<>();
        for (Map.Entry<String, String> entry : FEATURE_FIELDS_BY_WEIGHT_PATH.entrySet()) {
            List<Pair> pairs5d = pairsFor(snapshots, entry.getValue(), true);
            List<Pair> pairs20d = pairsFor(snapshots, entry.getValue(), false);
            BigDecimal ic5d = spearman(pairs5d);
            BigDecimal ic20d = spearman(pairs20d);
            if (ic5d != null || ic20d != null) {
                results.add(new FeatureIC(entry.getKey(), entry.getValue(), ic5d, ic20d, pairCount(pairs5d, pairs20d)));
            }
        }
        return new FeatureICReport(results);
    }

    public MutationGuide guideForIteration(int iterNo, List<String> fallbackPaths) {
        FeatureICReport report = measureLatest();
        if (report.ics().isEmpty()) {
            String fallbackPath = fallbackPaths.get((iterNo - 1) % fallbackPaths.size());
            return new MutationGuide(fallbackPath, BigDecimal.valueOf(1.10), report, false);
        }
        Comparator<FeatureIC> comparator = report.hasSignal()
                ? Comparator.comparing(FeatureIC::absGuideIc).reversed().thenComparing(FeatureIC::weightPath)
                : Comparator.comparing(FeatureIC::absGuideIc).thenComparing(FeatureIC::weightPath);
        List<FeatureIC> ordered = report.ics().stream().sorted(comparator).toList();
        FeatureIC selected = ordered.get((iterNo - 1) % ordered.size());
        BigDecimal guideIc = selected.guideIc();
        BigDecimal factor = guideIc.compareTo(ZERO_IC_THRESHOLD) > 0 ? BigDecimal.valueOf(1.10) : BigDecimal.valueOf(0.90);
        return new MutationGuide(selected.weightPath(), factor, report, true);
    }

    private List<Pair> pairsFor(List<FeatureSnapshotEntity> snapshots, String featureField, boolean fiveDay) {
        List<Pair> pairs = new ArrayList<>();
        for (FeatureSnapshotEntity snapshot : snapshots) {
            BigDecimal forwardReturn = fiveDay ? snapshot.getFwdRet5d() : snapshot.getFwdRet20d();
            if (forwardReturn == null) {
                continue;
            }
            BigDecimal featureValue = featureValue(snapshot, featureField);
            if (featureValue != null) {
                pairs.add(new Pair(featureValue.doubleValue(), forwardReturn.doubleValue()));
            }
        }
        return pairs;
    }

    private BigDecimal featureValue(FeatureSnapshotEntity snapshot, String featureField) {
        try {
            JsonNode root = objectMapper.readTree(snapshot.getFeatureJson());
            JsonNode value = root.path(featureField);
            if (value.isNumber()) {
                return value.decimalValue();
            }
            JsonNode rawValue = root.path("raw" + Character.toUpperCase(featureField.charAt(0)) + featureField.substring(1));
            return rawValue.isNumber() ? rawValue.decimalValue() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private BigDecimal spearman(List<Pair> pairs) {
        if (pairs.size() < MIN_PAIRS) {
            return null;
        }
        double[] xRanks = ranks(pairs.stream().mapToDouble(Pair::x).toArray());
        double[] yRanks = ranks(pairs.stream().mapToDouble(Pair::y).toArray());
        double correlation = pearson(xRanks, yRanks);
        if (!Double.isFinite(correlation)) {
            return null;
        }
        return BigDecimal.valueOf(correlation).setScale(6, RoundingMode.HALF_UP);
    }

    private double[] ranks(double[] values) {
        List<RankValue> sorted = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            sorted.add(new RankValue(i, values[i]));
        }
        sorted.sort(Comparator.comparingDouble(RankValue::value));
        double[] ranks = new double[values.length];
        int index = 0;
        while (index < sorted.size()) {
            int end = index + 1;
            while (end < sorted.size() && Double.compare(sorted.get(end).value(), sorted.get(index).value()) == 0) {
                end++;
            }
            double averageRank = (index + 1 + end) / 2.0d;
            for (int i = index; i < end; i++) {
                ranks[sorted.get(i).index()] = averageRank;
            }
            index = end;
        }
        return ranks;
    }

    private double pearson(double[] x, double[] y) {
        double xAverage = average(x);
        double yAverage = average(y);
        double numerator = 0.0d;
        double xVariance = 0.0d;
        double yVariance = 0.0d;
        for (int i = 0; i < x.length; i++) {
            double xDiff = x[i] - xAverage;
            double yDiff = y[i] - yAverage;
            numerator += xDiff * yDiff;
            xVariance += xDiff * xDiff;
            yVariance += yDiff * yDiff;
        }
        if (xVariance == 0.0d || yVariance == 0.0d) {
            return Double.NaN;
        }
        return numerator / Math.sqrt(xVariance * yVariance);
    }

    private double average(double[] values) {
        double sum = 0.0d;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private int pairCount(List<Pair> pairs5d, List<Pair> pairs20d) {
        return Math.max(pairs5d.size(), pairs20d.size());
    }

    public record FeatureICReport(List<FeatureIC> ics) {
        public static FeatureICReport empty() {
            return new FeatureICReport(List.of());
        }

        public boolean hasSignal() {
            return ics.stream().anyMatch(ic -> ic.guideIc().abs().compareTo(ZERO_IC_THRESHOLD) > 0);
        }

        public String summary() {
            if (ics.isEmpty()) {
                return "icGuide=none";
            }
            Map<String, Object> top = new LinkedHashMap<>();
            ics.stream()
                    .sorted(Comparator.comparing(FeatureIC::absGuideIc).reversed().thenComparing(FeatureIC::weightPath))
                    .limit(3)
                    .forEach(ic -> top.put(ic.weightPath(), ic.guideIc()));
            return "icGuide=" + top;
        }
    }

    public record FeatureIC(String weightPath, String featureField, BigDecimal ic5d, BigDecimal ic20d, int pairCount) {
        public BigDecimal guideIc() {
            if (ic20d != null) {
                return ic20d;
            }
            return ic5d == null ? BigDecimal.ZERO : ic5d;
        }

        public BigDecimal absGuideIc() {
            return guideIc().abs();
        }
    }

    public record MutationGuide(String weightPath, BigDecimal factor, FeatureICReport report, boolean icGuided) {
        public String summary() {
            return (icGuided ? "IC-guided" : "cyclic") + " mutation: " + weightPath + " x" + factor + " " + report.summary();
        }
    }

    private record Pair(double x, double y) {
    }

    private record RankValue(int index, double value) {
    }
}
