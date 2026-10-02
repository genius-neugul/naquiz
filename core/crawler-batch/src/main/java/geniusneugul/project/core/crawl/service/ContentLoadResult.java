package geniusneugul.project.core.crawl.service;

/**
 * 콘텐츠 적재 결과. 읽은 수와 새로 저장한 수의 차이가 이미 있어서 건너뛴 수다.
 */
public record ContentLoadResult(int readCount, int addedCount) {
}
