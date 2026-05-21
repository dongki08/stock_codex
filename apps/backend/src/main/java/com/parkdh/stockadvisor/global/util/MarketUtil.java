package com.parkdh.stockadvisor.global.util; // 공통 유틸 패키지를 선언한다.

import java.util.Set; // 집합 타입을 가져온다.

public final class MarketUtil { // 시장 관련 공통 유틸 클래스를 정의한다.
    private static final Set<String> KOREAN_MARKETS = Set.of("KOSPI", "KOSDAQ"); // 한국 시장 코드 집합을 정의한다.
    private static final Set<String> US_MARKETS = Set.of("NASDAQ", "NYSE"); // 미국 시장 코드 집합을 정의한다.
    private static final String DEV_PLACEHOLDER = "dev-placeholder"; // 개발용 플레이스홀더 값을 정의한다.

    private MarketUtil() {} // 인스턴스 생성을 방지한다.

    public static boolean isKoreanMarket(String market) { // 한국 시장인지 확인한다.
        return market != null && KOREAN_MARKETS.contains(market); // 한국 시장 코드이면 true를 반환한다.
    } // 한국 시장 확인을 종료한다.

    public static boolean isUsMarket(String market) { // 미국 시장인지 확인한다.
        return market != null && US_MARKETS.contains(market); // 미국 시장 코드이면 true를 반환한다.
    } // 미국 시장 확인을 종료한다.

    public static boolean isDevPlaceholder(String value) { // 개발용 플레이스홀더인지 확인한다.
        return DEV_PLACEHOLDER.equals(value); // 개발용 플레이스홀더 값이면 true를 반환한다.
    } // 개발용 플레이스홀더 확인을 종료한다.

    public static String toDisplayMarket(String market) { // 시장 코드를 표시용 문자열로 변환한다.
        if (isKoreanMarket(market)) { // 한국 시장인지 확인한다.
            return "KR-" + market; // 한국 시장 표시명을 반환한다.
        } // 한국 시장 확인을 종료한다.
        if (isUsMarket(market)) { // 미국 시장인지 확인한다.
            return "US-" + market; // 미국 시장 표시명을 반환한다.
        } // 미국 시장 확인을 종료한다.
        return market != null ? market : "UNKNOWN"; // 알 수 없는 시장 코드를 반환한다.
    } // 시장 표시명 변환을 종료한다.
} // 시장 관련 공통 유틸 클래스를 종료한다.
