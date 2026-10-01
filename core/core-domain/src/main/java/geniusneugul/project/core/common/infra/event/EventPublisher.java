package geniusneugul.project.core.common.infra.event;

import geniusneugul.project.core.common.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 인프로세스 이벤트 발행기. implement는 ApplicationEventPublisher 대신 이 클래스만 참조한다.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
