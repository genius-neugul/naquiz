package geniusneugul.project.core.crawl.presentation;

import geniusneugul.project.core.crawl.service.InitialDataLoadService;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * naquiz.initial-load.enabled=true 로 실행했을 때만 초기 데이터를 적재한다. 평소 배치 실행에서는 돌지 않는다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "naquiz.initial-load.enabled", havingValue = "true")
public class InitialDataLoadRunner implements ApplicationRunner {

    private final InitialDataLoadService initialDataLoadService;

    @Value("${naquiz.initial-data.dir}")
    private Path dataDir;

    @Override
    public void run(ApplicationArguments args) {
        initialDataLoadService.loadSongs(dataDir);
        initialDataLoadService.loadMovies(dataDir);
    }
}
