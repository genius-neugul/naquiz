package geniusneugul.project.core.song.infra;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spotify·YouTube 호출에 쓰는 HTTP 클라이언트. 출제할 때는 참가자가 라운드를 기다리므로 연결과 응답 모두 상한을 둔다.
 */
@Configuration
public class SongApiConfig {

    @Bean
    public RestClient songApiRestClient(@Value("${naquiz.song.request-timeout}") Duration requestTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(requestTimeout);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
