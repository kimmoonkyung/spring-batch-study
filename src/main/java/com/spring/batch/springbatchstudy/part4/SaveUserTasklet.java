package com.spring.batch.springbatchstudy.part4;

import com.spring.batch.springbatchstudy.part5.Orders;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//@RequiredArgsConstructor
public class SaveUserTasklet implements Tasklet { // user를 저장하기 위한 tasklet

    private final int SIZE = 10_000;
    private final UserRepository userRepository;

    public SaveUserTasklet(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<User> users = createUsers();

        Collections.shuffle(users);

        userRepository.saveAll(users);

        return RepeatStatus.FINISHED;
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(1_000)
                            .createdDate(LocalDate.of(2022,2,10))
                            .itemName("item" + i)
                            .build()))
                    .username("test username" + i)
                    .build());
        }
        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(200_000)
                            .createdDate(LocalDate.of(2022,1,11))
                            .itemName("item" + i)
                            .build()))
                    .username("test username" + i)
                    .build());
        }
        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(300_000)
                            .createdDate(LocalDate.of(2022,2,12))
                            .itemName("item" + i)
                            .build()))
                    .username("test username" + i)
                    .build());
        }
        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(500_000)
                            .createdDate(LocalDate.of(2022,2,13))
                            .itemName("item" + i)
                            .build()))
                    .username("test username" + i)
                    .build());
        }

        return users;
    }
}
