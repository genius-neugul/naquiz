package geniusneugul.project.core.room.implement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 방 단위 락. 같은 방의 상태를 읽고 바꾸는 흐름은 이 락 안에서 한 번에 하나만 실행한다(docs/ARCHITECTURE.md 「동시성」).
 * 방이 다르면 서로 기다리지 않는다. 락 안에서는 메모리 상태만 다루고 I/O(DB, 외부 API, 메시지 발송)는 하지 않는다.
 */
@Component
public class RoomLock {

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(Long roomId, Supplier<T> task) {
        ReentrantLock lock = locks.computeIfAbsent(roomId, id -> new ReentrantLock());
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 지워진 방의 락을 정리한다. 이 직후 같은 방 ID로 새 락이 만들어질 수 있지만,
     * 닫힌 방은 다시 바뀌지 않으므로(Room.leave 등이 아무것도 하지 않음) 두 락이 겹쳐도 상태가 깨지지 않는다.
     */
    public void release(Long roomId) {
        locks.remove(roomId);
    }
}
