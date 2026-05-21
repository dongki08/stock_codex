package com.parkdh.stockadvisor.domain.marketdata;

import com.parkdh.stockadvisor.domain.common.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "macro_observation")
public class MacroObservationEntity extends CreatedEntity {
    @Id
    @Comment("Observation key")
    @Column(name = "observation_key", length = 100, nullable = false)
    private String observationKey;

    @Comment("Series ID")
    @Column(name = "series_id", length = 40, nullable = false)
    private String seriesId;

    @Comment("Series name")
    @Column(name = "series_name", length = 200, nullable = false)
    private String seriesName;

    @Comment("Observed date")
    @Column(name = "observed_date", nullable = false)
    private LocalDate observedDate;

    @Comment("Observed value")
    @Column(name = "observed_value", precision = 24, scale = 6)
    private BigDecimal observedValue;

    @Comment("Source")
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Comment("Fetched at")
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    public MacroObservationEntity(String observationKey, String seriesId, String seriesName, LocalDate observedDate,
                                  BigDecimal observedValue, String source, LocalDateTime fetchedAt) {
        this.observationKey = observationKey;
        this.seriesId = seriesId;
        this.seriesName = seriesName;
        this.observedDate = observedDate;
        this.observedValue = observedValue;
        this.source = source;
        this.fetchedAt = fetchedAt;
    }

    public void update(BigDecimal observedValue, LocalDateTime fetchedAt) {
        this.observedValue = observedValue;
        this.fetchedAt = fetchedAt;
    }
}
