package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

@SpringBootTest
public class TodoRepositoryTest {

    @Autowired
    TodoRepository todoRepository;

    @Autowired
    DatabaseClient database;

    @BeforeEach
    public void setUp() {
        Hooks.onOperatorDebug();

        database.execute("DELETE FROM todo;").fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void test_Whether_FindAll_Returns_All_Rows_From_DB() {

        Todo grocery = new Todo("Buy Milk", "Buy Milk from store tomorrow", false);
        Todo carwash = new Todo("Car Wash", "Get car wash done today", false);

        insertTodos(grocery, carwash);

        todoRepository.findAll()
                .as(StepVerifier::create)
                .assertNext(grocery::equals)
                .assertNext(carwash::equals)
                .verifyComplete();
    }

    @Test
    public void test_Whether_Save_Inserts_Data() {
        Todo icecream = new Todo("Buy Icecream", "Buy Icecream for kids today", false);
        todoRepository.save(icecream)
                .as(StepVerifier::create)
                .expectNextMatches(todo -> todo.getId() != null)
                .verifyComplete();
    }

    @Test
    public void test_Whether_Delete_Removes_Data() {
        Todo gas = new Todo("Fill Gas", "Fill gas in jeep today", false);

        Mono<Todo> deleted = todoRepository
                .save(gas)
                .flatMap(saved -> todoRepository.delete(saved).thenReturn(saved));

        StepVerifier
                .create(deleted)
                .expectNextMatches(customer -> gas.getDescription().equalsIgnoreCase("Fill Gas"))
                .verifyComplete();
    }

    @Test
    public void test_Whether_Update_Changes_Flag() {
        Todo laundry = new Todo("Laundry", "Do laundry today", false);

        Mono<Todo> saved = todoRepository
                .save(laundry)
                .flatMap(todo -> {
                    todo.setDone(true);
                    return todoRepository.save(todo);
                });

        StepVerifier
                .create(saved)
                .expectNextMatches(Todo::isDone)
                .verifyComplete();
    }

    private void insertTodos(Todo... todos) {

        this.todoRepository.saveAll(Arrays.asList(todos))
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
    }
}