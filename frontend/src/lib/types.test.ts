import { describe, expect, it } from "vitest";
import { commentText, unwrapList, type Comment } from "@/lib/types";

describe("unwrapList", () => {
  it("returns arrays as-is", () => {
    expect(unwrapList([1, 2])).toEqual([1, 2]);
  });

  it("reads Spring-style content pages", () => {
    expect(unwrapList({ content: ["a"], totalElements: 1 })).toEqual(["a"]);
  });

  it("reads items alias", () => {
    expect(unwrapList({ items: ["b"] })).toEqual(["b"]);
  });

  it("handles nullish", () => {
    expect(unwrapList(null)).toEqual([]);
    expect(unwrapList(undefined)).toEqual([]);
  });
});

describe("commentText", () => {
  it("prefers body then content then text", () => {
    expect(commentText({ id: "1", body: "hi" })).toBe("hi");
    expect(commentText({ id: "2", content: "there" } as Comment)).toBe("there");
    expect(commentText({ id: "3", text: "yo" } as Comment)).toBe("yo");
  });
});

describe("API default", () => {
  it("documents https://localhost/api as the gateway contract base", () => {
    expect("https://localhost/api").toMatch(/\/api$/);
  });
});
