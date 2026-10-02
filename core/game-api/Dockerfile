# syntax=docker/dockerfile:1
# game-api 실행 이미지. 빌드 컨텍스트는 core/ 다(멀티모듈 전체가 필요하다).
# 바뀌는 빈도가 낮은 것부터 쌓아서, 소스만 바뀌면 의존성 단계는 건너뛰고 실행 이미지도 마지막 레이어만 바뀌게 한다.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 1) 의존성: Gradle wrapper와 빌드 스크립트만 복사해 의존성을 먼저 받는다. build.gradle이 바뀔 때만 다시 실행된다.
#    받은 jar는 캐시 마운트(/root/.gradle)에 둔다. 이미지 레이어에 구우면 Gradle이 빌드마다 그 안의 캐시 파일을 고치느라 느려진다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY core-domain/build.gradle core-domain/
COPY game-api/build.gradle game-api/
COPY admin-api/build.gradle admin-api/
COPY crawler-batch/build.gradle crawler-batch/
COPY <<'GRADLE' /tmp/resolve-dependencies.gradle
// 컴파일·실행에 필요한 의존성 jar를 미리 내려받는다(프로젝트 빌드 스크립트는 건드리지 않는다).
allprojects {
    tasks.register('resolveDependencies') {
        def resolvable = configurations.matching {
            it.name in ['compileClasspath', 'runtimeClasspath', 'annotationProcessor']
        }
        doLast { resolvable.each { it.resolve() } }
    }
}
GRADLE
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --init-script /tmp/resolve-dependencies.gradle \
        :core-domain:resolveDependencies :game-api:resolveDependencies --no-daemon -q

# 2) 소스: 소스만 바뀌면 여기부터 다시 실행된다.
#    build/·.gradle/도 캐시 마운트로 남겨 바뀐 클래스만 다시 컴파일한다(증분 컴파일).
#    마운트 안의 결과물은 이미지에 남지 않으므로 같은 RUN 안에서 jar를 꺼내 레이어별로 푼다.
#    (캐시 마운트가 비워져도 동작하도록 --offline은 쓰지 않는다)
COPY core-domain/src core-domain/src
COPY game-api/src game-api/src
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/workspace/.gradle \
    --mount=type=cache,target=/workspace/core-domain/build \
    --mount=type=cache,target=/workspace/game-api/build \
    rm -f game-api/build/libs/*.jar \
    && ./gradlew :game-api:bootJar -x test --no-daemon -q \
    && cp game-api/build/libs/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 3) 실행 이미지: Spring Boot 레이어 순서대로 복사한다. 코드만 바뀌면 마지막 application 레이어(수십 kB)만 바뀐다.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
