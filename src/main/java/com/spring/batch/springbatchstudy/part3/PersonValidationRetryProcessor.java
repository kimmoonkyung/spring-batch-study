package com.spring.batch.springbatchstudy.part3;


import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

@Slf4j
public class PersonValidationRetryProcessor implements ItemProcessor<Person, Person> {

    private final RetryTemplate retryTemplate;

    public PersonValidationRetryProcessor() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(6) // retry limit 과 비슷한 것
                .retryOn(NotFoundNameException.class)
                .withListener(new SavePersonRetryListener())
                .build();
    }

    @Override
    public Person process(Person item) throws Exception {
        return this.retryTemplate.execute(context -> {
            // RetryCallBack
            if (item.isNotEmptyName()) {
                return item;
            }
            throw new NotFoundNameException();
        }, context -> {
            // RecoveryCallBack
            return item.unknownName();
        });
    }

    public static class SavePersonRetryListener implements RetryListener {

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            // retry를 시작하는 설정, return true 이어야 retry가 적용이 됨
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            // retry 종료 후 호출 됨.
            log.info("retry close!");
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            // retry 템플릿에 정의한 익셉션이 발생하면 호출 여기선 NotFoundNameException가 던져 졌을 때
            log.info("retry onError!");
        }

    }

}
