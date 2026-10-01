import { describe, expect, it } from "vitest";
import { maskTitleInSynopsis } from "./synopsis";

describe("maskTitleInSynopsis", () => {
  it("본문의 정답과 보조 정답을 가린다", () => {
    expect(maskTitleInSynopsis("명량 해전을 그린 영화. Roaring Currents", "명량", "Roaring Currents")).toBe("○○○ 해전을 그린 영화. ○○○");
  });

  it("대소문자와 공백 차이를 무시한다", () => {
    expect(maskTitleInSynopsis("그들은 train  to busan 에 올랐다", "부산행", "Train to Busan")).toBe("그들은 ○○○ 에 올랐다");
    expect(maskTitleInSynopsis("극한 직업의 세계", "극한직업", "")).toBe("○○○의 세계");
  });
});
