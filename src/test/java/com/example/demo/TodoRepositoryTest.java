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

        Todo grocery = new Todo("Todo 1", "This is the first todo", false);
        Todo carwash = new Todo("Todo 2", "This is the second todo", false);

        insertTodos(grocery, carwash);

        todoRepository.findAll()
                .as(StepVerifier::create)
                .assertNext(grocery::equals)
                .assertNext(carwash::equals)
                .verifyComplete();
    }

    @Test
    public void test_Whether_Save_Inserts_Data() {
        Todo icecream = new Todo("Todo 3", "This is the third todo", false);
        todoRepository.save(icecream)
                .as(StepVerifier::create)
                .expectNextMatches(todo -> todo.getId() != null)
                .verifyComplete();
    }

    @Test
    public void test_Whether_Delete_Removes_Data() {
        Todo gas = new Todo("Todo 4", "This is the fourth todo", false);

        Mono<Todo> deleted = todoRepository
                .save(gas)
                .flatMap(saved -> todoRepository.delete(saved).thenReturn(saved));

        StepVerifier
                .create(deleted)
                .expectNextMatches(customer -> gas.getDescription().equalsIgnoreCase("Todo 4"))
                .verifyComplete();
    }

    @Test
    public void test_Whether_Update_Changes_Flag() {
        Todo laundry = new Todo("Todo 5", "This is the fifth todo", false);

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