package com.spring.batch.springbatchstudy.part3;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@RequiredArgsConstructor
public class DuplicateValidationProcessor<T> implements ItemProcessor<T, T> {

    private final Map<String, Object> keyPool = new ConcurrentHashMap<>();
    private final Function<T, String> keyExtractor;
    private final boolean allowDuplicate;

    @Override
    public T process(T item) throws Exception {
        if (allowDuplicate) { // allowDuplicate TRUE 면 필터링을 하지 않는다는 의미이기 때문에 item을 그대로 리턴, FALSE 는 항상 필터링 한다는 의미
            return item;
        }

        String key = keyExtractor.apply(item); // item 에서 key 추출

        if (keyPool.containsKey(key)) {
            return null; // 중복이 되었다는 의미기 때문에 null 리턴
        }

        keyPool.put(key, key);

        return item;
    }

}
