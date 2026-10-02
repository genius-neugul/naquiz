package geniusneugul.project.core.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

/**
 * IDE·bootRun으로 실행할 때 Spring Boot Docker Compose 지원이 게임 웹 compose 파일(compose.game-web.yml)을 쓰게 한다.
 * 실행 위치(작업 디렉터리)가 IDE 설정마다 달라 상대 경로를 고정할 수 없으므로, 작업 디렉터리에서 위로 올라가며 찾는다.
 * 못 찾으면 Docker Compose 지원을 끈다. 작업 디렉터리의 다른 compose 파일(game-api까지 띄우는 compose.yml)을 쓰지 않게 하려는 것이다.
 * Docker Compose 모듈은 developmentOnly 의존성이라 실행 jar(컨테이너)에서는 아무것도 하지 않는다.
 */
public class GameWebComposeFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String COMPOSE_FILE_NAME = "compose.game-web.yml";

    private static final String DOCKER_COMPOSE_LISTENER =
            "org.springframework.boot.docker.compose.lifecycle.DockerComposeListener";

    private final Log log;

    public GameWebComposeFileEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!ClassUtils.isPresent(DOCKER_COMPOSE_LISTENER, getClass().getClassLoader())) {
            return;
        }
        Path workingDirectory = Path.of("").toAbsolutePath();
        Map<String, Object> properties = findUpward(workingDirectory)
                .<Map<String, Object>>map(file -> Map.of("spring.docker.compose.file", file.toString()))
                .orElseGet(() -> {
                    log.warn("[GameWebComposeFileEnvironmentPostProcessor.postProcessEnvironment] Compose file not found. "
                            + "dockerCompose=disabled, fileName=" + COMPOSE_FILE_NAME + ", workingDirectory=" + workingDirectory);
                    return Map.of("spring.docker.compose.enabled", false);
                });
        // 가장 낮은 우선순위로 넣어 환경 변수·실행 인자로 준 값이 이긴다.
        environment.getPropertySources().addLast(new MapPropertySource("gameWebComposeFile", properties));
    }

    private Optional<Path> findUpward(Path start) {
        return Stream.iterate(start, directory -> directory != null, Path::getParent)
                .map(directory -> directory.resolve(COMPOSE_FILE_NAME))
                .filter(Files::isRegularFile)
                .findFirst();
    }
}
