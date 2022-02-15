package com.spring.batch.springbatchstudy;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableBatchProcessing
public class SpringBatchApplication {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(SpringBatchApplication.class, args)));
	}

	@Bean
	@Primary	// 스프링 부트에서는 TaskExecutor가 기본적으로 빈으로 생성 되어있기 때문에, 지금 만드는 TaskExecutor를 기본 빈으로 사용하겠다는 설정
	TaskExecutor taskExecutor() {
		// ThreadPoolTaskExecutor 풀안에서 스레드를 미리 생성해놓고 필요할때 미리 꺼내 쓸 수 있기때문에 다른 구현체보다 효율적이다.
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(10);
		taskExecutor.setMaxPoolSize(20);
		taskExecutor.setThreadNamePrefix("batch-thread-");
		taskExecutor.initialize();
		return taskExecutor;
	}

}
