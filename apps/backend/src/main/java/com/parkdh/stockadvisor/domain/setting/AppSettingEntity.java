package com.parkdh.stockadvisor.domain.setting; // 설정 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.BaseEntity; // 생성일과 수정일 기본 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "app_setting") // 앱 설정 테이블에 매핑한다.
public class AppSettingEntity extends BaseEntity { // 앱 설정 엔티티를 정의한다.
    @Id // 설정 키를 기본 키로 지정한다.
    @Comment("설정 키") // 설정 키 컬럼 설명을 지정한다.
    @Column(name = "setting_key", length = 60, nullable = false) // 설정 키 컬럼을 매핑한다.
    private String settingKey; // 설정 키 값을 보관한다.

    @Comment("설정 값 JSON") // 설정 값 컬럼 설명을 지정한다.
    @Column(name = "value_json", nullable = false, columnDefinition = "nvarchar(max)") // 설정 값 JSON 컬럼을 매핑한다.
    private String valueJson; // 설정 값 JSON 문자열을 보관한다.

    @Comment("설정 설명") // 설정 설명 컬럼 설명을 지정한다.
    @Column(name = "description", nullable = false, columnDefinition = "nvarchar(500)") // 설정 설명 컬럼을 매핑한다.
    private String description; // 설정 설명을 보관한다.

    @Comment("수정자") // 수정자 컬럼 설명을 지정한다.
    @Column(name = "updated_by", length = 40, nullable = false) // 수정자 컬럼을 매핑한다.
    private String updatedBy; // 마지막 수정자를 보관한다.

    public AppSettingEntity(String settingKey, String valueJson, String description, String updatedBy) { // 설정 엔티티 생성자를 정의한다.
        this.settingKey = settingKey; // 설정 키를 저장한다.
        this.valueJson = valueJson; // 설정 값 JSON을 저장한다.
        this.description = description; // 설정 설명을 저장한다.
        this.updatedBy = updatedBy; // 수정자를 저장한다.
    } // 생성자를 종료한다.

    public void updateValue(String valueJson, String updatedBy) { // 설정 값을 갱신한다.
        this.valueJson = valueJson; // 새 설정 값 JSON을 저장한다.
        this.updatedBy = updatedBy; // 새 수정자를 저장한다.
    } // 설정 값 갱신 메서드를 종료한다.
} // 앱 설정 엔티티를 종료한다.
