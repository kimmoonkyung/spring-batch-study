package com.spring.batch.springbatchstudy.part4;

import com.spring.batch.springbatchstudy.part5.JobParametersDecide;
import com.spring.batch.springbatchstudy.part5.OrderStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
//@RequiredArgsConstructor
public class UserConfiguration {

    private final int CHUNK = 100;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    public UserConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, UserRepository userRepository, EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.userRepository = userRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public Job userJob() throws Exception {
        return this.jobBuilderFactory.get("userJob")
                .incrementer(new RunIdIncrementer())
                .start(this.saveUserStep())
                .next(this.userLevelUpStep())
                .listener(new LevelUpJobExecutionListener(userRepository))
                .next(new JobParametersDecide("date"))
                .on(JobParametersDecide.CONTINUE.getName())
                .to(this.orderStatisticsStep(null))
                .build()
                .build();
    }

    @Bean
    @JobScope
    public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) throws Exception {
        return this.stepBuilderFactory.get("orderStatisticsStep")
                .<OrderStatistics, OrderStatistics>chunk(CHUNK)
                .reader(orderStatisticsItemReader(date))
                .writer(orderStatisticsItemWriter(date))
                .build();
    }

    private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        String fileName = yearMonth.getYear() + "년_" + yearMonth.getMonthValue() + "월_일별_주문_금액.csv";

        BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"amount", "date"}); // OrderStatistics

        DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
                .resource(new FileSystemResource("output/" + fileName))
                .lineAggregator(lineAggregator)
                .name("orderStatisticsItemWriter")
                .encoding("UTF-8")
                .headerCallback(writer -> writer.write("total_amount,date"))
                .build();
        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", yearMonth.atDay(1));
        parameters.put("endDate", yearMonth.atEndOfMonth());

        Map<String, Order> sortKey = new HashMap<>();
        sortKey.put("created_date", Order.ASCENDING);

        JdbcPagingItemReader<OrderStatistics> itemReader = new JdbcPagingItemReaderBuilder<OrderStatistics>()
                .dataSource(this.dataSource)
                .rowMapper((rs, rowNum) -> OrderStatistics.builder()
                        .amount(rs.getString(1))
                        .date(LocalDate.parse(rs.getString(2), DateTimeFormatter.ISO_DATE))
                        .build())
                .pageSize(CHUNK)
                .name("orderStatisticsItemReader")
                .selectClause("sum(amount), created_date")
                .fromClause("orders")
                .whereClause("created_date >= :startDate and created_date <= :endDate")
                .groupClause("created_date")
                .parameterValues(parameters)
                .sortKeys(sortKey)
                .build();
        itemReader.afterPropertiesSet();

        return itemReader;
    }

    @Bean
    public Step saveUserStep() {
        return this.stepBuilderFactory.get("saveUserStep")
                .tasklet(new SaveUserTasklet(userRepository))
                .build();
    }

    @Bean
    public Step userLevelUpStep() throws Exception {
        return this.stepBuilderFactory.get("userLevelUpStep")
                .<User, User>chunk(CHUNK)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<? super User> itemWriter() {
        return users -> users.forEach(x -> {
                x.levelUp();
                userRepository.save(x);
            });
    }

    private ItemProcessor<? super User, ? extends User> itemProcessor() {
        return user -> {
            if (user.availableLevelUp()) { // 등급 상향 대상이라면 유저를 리턴
                return user;
            }

            return null;
        };
    }

    private ItemReader<? extends User> itemReader() throws Exception {
        JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
                .queryString("select u from User u")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK) // chunk size 와 동일하게 설정
                .name("userItemReader")
                .build();
        itemReader.afterPropertiesSet();

        return itemReader;
    }

}
