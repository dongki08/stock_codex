package com.parkdh.stockadvisor.domain.prediction; // 예측 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.CreatedEntity; // 생성일 공통 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.GeneratedValue; // 기본 키 자동 생성 어노테이션을 가져온다.
import jakarta.persistence.GenerationType; // 기본 키 생성 전략을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "prediction") // 가격 예측 테이블에 매핑한다.
public class PredictionEntity extends CreatedEntity { // 가격 예측 엔티티를 정의한다.
    @Id // 예측 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("예측 ID") // 예측 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 예측 ID 컬럼을 매핑한다.
    private Long id; // 예측 ID를 보관한다.

    @Comment("종목 코드") // 종목 코드 컬럼 설명을 지정한다.
    @Column(name = "ticker", length = 20, nullable = false) // 종목 코드 컬럼을 매핑한다.
    private String ticker; // 종목 코드를 보관한다.

    @Comment("예측 기간 일수") // 예측 기간 일수 컬럼 설명을 지정한다.
    @Column(name = "horizon_days", nullable = false) // 예측 기간 일수 컬럼을 매핑한다.
    private Integer horizonDays; // 예측 기간 일수를 보관한다.

    @Comment("예측 가격") // 예측 가격 컬럼 설명을 지정한다.
    @Column(name = "predicted_price", precision = 18, scale = 4, nullable = false) // 예측 가격 컬럼을 매핑한다.
    private BigDecimal predictedPrice; // 예측 가격을 보관한다.

    @Comment("모델 버전") // 모델 버전 컬럼 설명을 지정한다.
    @Column(name = "model_version", length = 40, nullable = false) // 모델 버전 컬럼을 매핑한다.
    private String modelVersion; // 모델 버전을 보관한다.

    @Comment("예측 생성 일시") // 예측 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "generated_at", nullable = false) // 예측 생성 일시 컬럼을 매핑한다.
    private LocalDateTime generatedAt; // 예측 생성 일시를 보관한다.

    public PredictionEntity(String ticker, Integer horizonDays, BigDecimal predictedPrice, String modelVersion, LocalDateTime generatedAt) { // 예측 생성자를 정의한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.horizonDays = horizonDays; // 예측 기간 일수를 저장한다.
        this.predictedPrice = predictedPrice; // 예측 가격을 저장한다.
        this.modelVersion = modelVersion; // 모델 버전을 저장한다.
        this.generatedAt = generatedAt; // 생성 일시를 저장한다.
    } // 생성자를 종료한다.
} // 가격 예측 엔티티를 종료한다.
