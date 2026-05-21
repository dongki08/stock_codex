import { useState } from "react";
import { AdminPage } from "./pages/AdminPage";
import { InstrumentsPage } from "./pages/InstrumentsPage";
import { MarketDataPage } from "./pages/MarketDataPage";
import { RecommendationsPage } from "./pages/RecommendationsPage";
import { StatsPage } from "./pages/StatsPage";
import { UniversePage } from "./pages/UniversePage";

type Page = "admin" | "instruments" | "marketData" | "recommendations" | "stats" | "universe";

export function App() {
  const [page, setPage] = useState<Page>("recommendations");

  return (
    <>
      <nav className="app-nav">
        <button className={page === "recommendations" ? "active" : ""} type="button" onClick={() => setPage("recommendations")}>
          오늘의 추천
        </button>
        <button className={page === "universe" ? "active" : ""} type="button" onClick={() => setPage("universe")}>
          시장 후보군
        </button>
        <button className={page === "marketData" ? "active" : ""} type="button" onClick={() => setPage("marketData")}>
          수집 데이터
        </button>
        <button className={page === "instruments" ? "active" : ""} type="button" onClick={() => setPage("instruments")}>
          종목 관리
        </button>
        <button className={page === "stats" ? "active" : ""} type="button" onClick={() => setPage("stats")}>
          성과 통계
        </button>
        <button className={page === "admin" ? "active" : ""} type="button" onClick={() => setPage("admin")}>
          관리자 설정
        </button>
      </nav>
      {page === "recommendations" && <RecommendationsPage />}
      {page === "universe" && <UniversePage />}
      {page === "marketData" && <MarketDataPage />}
      {page === "instruments" && <InstrumentsPage />}
      {page === "stats" && <StatsPage />}
      {page === "admin" && <AdminPage />}
    </>
  );
}
