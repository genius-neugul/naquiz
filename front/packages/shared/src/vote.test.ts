import { describe, expect, it } from "vitest";
import { isVotePassed, requiredApprovals } from "./vote";

describe("isVotePassed", () => {
  it("2명 이하는 전원 찬성해야 통과한다", () => {
    expect(isVotePassed(1, 2)).toBe(false);
    expect(isVotePassed(2, 2)).toBe(true);
    expect(isVotePassed(1, 1)).toBe(true);
  });

  it("3명 이상은 절반을 넘어야 통과한다", () => {
    expect(isVotePassed(1, 3)).toBe(false);
    expect(isVotePassed(2, 3)).toBe(true);
    expect(isVotePassed(2, 4)).toBe(false);
    expect(isVotePassed(3, 4)).toBe(true);
    expect(isVotePassed(3, 5)).toBe(true);
  });
});

describe("requiredApprovals", () => {
  it("참가자 수별 필요한 찬성 수", () => {
    expect([1, 2, 3, 4, 5, 10].map(requiredApprovals)).toEqual([1, 2, 2, 3, 3, 6]);
  });
});
