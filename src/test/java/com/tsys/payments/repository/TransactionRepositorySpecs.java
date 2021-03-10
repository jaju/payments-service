package com.tsys.payments.repository;

import com.tsys.payments.domain.Money;
import com.tsys.payments.domain.Transaction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

// To test Spring Data JPA repositories, or any other JPA-related components,
// Spring Boot provides the @DataJpaTest annotation. We can just add it to
// our unit test and it will set up a Spring application context.
//
// The created application context will not contain the whole context needed
// for our Spring Boot application, but instead only a "part" of it containing
// the components needed to initialize any JPA-related components like our
// Spring Data repository. But we can inject a DataSource, JdbcTemplate or
// EntityManager into our test class if we need them. And finally, we can
// inject any of the Spring Data repositories from our application.
//
// All of the above components will be automatically configured to point to an
// embedded, in-memory database (configured in application-test.properties) instead of
// the "real" database (configured in application-development.properties or
// application-production.properties).
//
// Further, by default the application context containing all these components,
// including the in-memory database, is shared between all test methods within
// all @DataJpaTest-annotated test classes.
//
// Also, by default, each test method runs in its own transaction, which is
// rolled back after the method has executed. This way, the database state
// stays clean between tests and the tests stay independent of each other.
@DataJpaTest  // It already has @ExtendWith(SpringExtension.class), so we need not need an explicit one.

// Instead of RunWith for Junit4, we use ExtendWith for Junit5.
//@ExtendWith(SpringExtension.class)

// As we are using flyway to manage the schema, we will use it to generate
// schema for us instead of Hibernate.  Hibernate’s ddl-auto configuration
// will automatically back off if we have not specifically configured it,
// so that Flyway has precedence and will by default execute all SQL scripts
// it finds in the folder src/main/resources/db/migration against our in-memory
// test database.
//
// What is the value of using Flyway in tests?
// If we're using Flyway in production we must also use it in our JPA tests.
// Only then do we know at test time that the flyway scripts work as expected.
// This will work as long as the scripts contain SQL that is valid on both
// the production database and the in-memory database used in the tests (H2 in
// our example).
//
// If this is not the case, we must disable Flyway in our tests by setting
// the spring.flyway.enabled property to false and the
// spring.jpa.hibernate.ddl-auto property to create-drop to let Hibernate
// generate the schema.
@Tag("IntegrationTest")
public class TransactionRepositorySpecs {
    private final UUID successfulTxnId = UUID.nameUUIDFromBytes("PASSED-TXNID-1".getBytes());
    private final String successfulOrderId = "PASSED-ORDER-ID-1";
    private final UUID failedTxnId = UUID.nameUUIDFromBytes("FAILED-TXNID-2".getBytes());
    private final String failedOrderId = "FAILED-ORDER-ID-2";
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionRepository transactionRepository;
    private Date now = Date.from(Instant.now());
    private final Transaction succeeded = new Transaction(successfulTxnId, now, "accepted", successfulOrderId, new Money(Currency.getInstance("INR"), 2000.45));
    private final Transaction failed = new Transaction(failedTxnId, now, "failed", failedOrderId, new Money(Currency.getInstance("INR"), 99.99));

    @Test
    public void dependenciesAreInjected() {
        assertThat(dataSource, notNullValue());
        assertThat(jdbcTemplate, notNullValue());
        assertThat(entityManager, notNullValue());
        assertThat(transactionRepository, notNullValue());
    }

    // Which tests to write?
    // =====================
    // 1. Ideally for all inferred queries, one can avoid writing tests. Ife we have
    //    only one test that tries to start up the Spring application context in our
    //    code base, we do not need to write an extra test for our inferred query.
    //    Just the above test is enough because, if all the dependencies can be
    //    injected, it means that the system started successfully and applied the flyway
    //    migrations successfully.
    //    NOTE: But I've written here just to demo how a Repository Integration tests
    //    can be written.
    //
    // 2. For Custom JPQL Queries, for simple ones, one can avoid writing tests.
    //    In case of complicated custom queries which might include joins with
    //    other tables or return aggregate objects instead of an entity, it is a good
    //    idea to write the tests for those.
    //
    // 3. For Native Queries, neither Hibernate nor Spring Data validate them at startup.
    //    Since the query may contain database-specific SQL, there’s no way Spring Data or
    //    Hibernate can know what to check for.
    //
    //    So, native queries are prime candidates for integration tests. However, if they
    //    really use database-specific SQL, those tests might not work with the embedded
    //    in-memory database, so we would have to provide a real database in the background
    //    (for instance in a docker container that is set up on-demand in the continuous
    //    integration pipeline).
    //    Alternatively, resort to ANSI-compliant SQL so that it work across different
    //    databases.

    // Tests for Inferred Queries
    @Test
    public void startsWithEmptyRepository() {
        assertThat(transactionRepository.count(), is(0L));
    }

    @Test
    public void findsNoTransactionsInAnEmptyRepository() {
        assertThat(toList(transactionRepository.findAll()), hasSize(0));
    }

    @Test
    public void findingByIdInAnEmptyRepositoryYieldsNothing() {
        assertThat(transactionRepository.findById(successfulTxnId), is(Optional.empty()));
    }

    @Test
    public void noTransactionsExistInAnEmptyRepository() {
        assertThat(transactionRepository.existsById(successfulTxnId), is(false));
//    assertThrows(EntityNotFoundException.class, () -> {
//      // Calling assertThat with nullValue() causes HibernateProxy to evaluate and
//      // then throw an javax.persistence.EntityNotFoundException, else
//      // the proxy does not evaluate and no exception in thrown by the following line:
//      // transactionRepository.getOne(txnId);
//      // Wrapping in assertThat causes proxy to evaluate and then throw an Exception.
//      assertThat(transactionRepository.getOne(txnId), nullValue());
//    });
    }

    @Test
    public void savesATransaction() {
        // When
        transactionRepository.save(succeeded);
        // Then
        assertThat(toList(transactionRepository.findAll()), hasSize(1));
        assertThat(transactionRepository.findById(successfulTxnId).get(), is(succeeded));
    }

    @Test
    public void deletesATransaction() {
        // Given
        transactionRepository.save(succeeded);
        assert transactionRepository.findById(successfulTxnId).get().equals(succeeded);

        // When
        transactionRepository.delete(succeeded);

        // Then
        assertThat(toList(transactionRepository.findAll()), hasSize(0));
        assertThat(transactionRepository.findById(successfulTxnId), is(Optional.empty()));
    }

    @Test
    public void deletesById() {
        // Given
        transactionRepository.save(succeeded);
        assert transactionRepository.findById(successfulTxnId).get().equals(succeeded);

        // When
        transactionRepository.deleteById(successfulTxnId);

        // Then
        assertThat(toList(transactionRepository.findAll()), hasSize(0));
        assertThat(transactionRepository.findById(successfulTxnId), is(Optional.empty()));
    }

    @Test
    public void shoutsWhenDeletingByNonExistentId() {
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class,
                () -> transactionRepository.deleteById(successfulTxnId),
                String.format("No class com.tsys.payments.domain.Transaction entity with id %s exists!", successfulTxnId));
    }

    @Test
    public void findsAllById() {
        // Given
        transactionRepository.save(succeeded);
        transactionRepository.save(failed);

        // When
        final var allById = transactionRepository.findAllById(List.of(successfulTxnId, failedTxnId));

        // Then
        assertThat(toList(allById), hasSize(2));
        assertThat(toList(allById), contains(succeeded, failed));
    }

    @Test
    public void savesAll() {
        // When
        transactionRepository.saveAll(List.of(succeeded, failed));

        // Then
        final var allById = transactionRepository.findAllById(List.of(successfulTxnId, failedTxnId));
        assertThat(toList(allById), hasSize(2));
        assertThat(toList(allById), contains(succeeded, failed));
    }

    @Test
    public void deletesAll() {
        // Given
        assertThat(toList(transactionRepository.saveAll(List.of(succeeded, failed))), hasSize(2));

        // When
        transactionRepository.deleteAll(List.of(succeeded, failed));

        // Then
        assertThat(toList(transactionRepository.findAllById(List.of(successfulTxnId, failedTxnId))), hasSize(0));
    }

    // Tests for Custom JPQL Queries
    @Test
    public void findsTransactionByOrderId() {
        transactionRepository.saveAll(List.of(succeeded, failed));

        assertThat(transactionRepository.findByOrderId(successfulOrderId).get(), is(succeeded));
    }

    @Test
    public void findingTransactionByOrderIdInAnEmptyRepositoryYieldsNothing() {
        assertThat(transactionRepository.findByOrderId(successfulOrderId), is(Optional.empty()));
    }

    @Test
    public void findsTransactionByTransactionIdAndOrderId() {
        transactionRepository.saveAll(List.of(succeeded, failed));

        assertThat(transactionRepository.findByTransactionIdAndOrderId(successfulTxnId, successfulOrderId).get(), is(succeeded));
    }

    @Test
    public void findingTransactionByTransactionIdAndOrderIdInAnEmptyRepositoryYieldsNothing() {
        assertThat(transactionRepository.findByTransactionIdAndOrderId(successfulTxnId, successfulOrderId), is(Optional.empty()));
    }

    // Tests for Native SQL Queries
    @Test
    public void findsAllTransactionByOrderIds() {
        transactionRepository.saveAll(List.of(succeeded, failed));

        assertThat(transactionRepository.findAllByOrderIds(List.of(successfulOrderId, failedOrderId)), hasSize(2));
    }

    @Test
    public void findingTransactionsByOrderIdsInAnEmptyRepositoryYieldsNothing() {
        assertThat(transactionRepository.findAllByOrderIds(List.of(successfulOrderId, failedOrderId)), hasSize(0));
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        var list = new ArrayList<T>();
        iterable.forEach(t -> list.add(t));
        return list;
    }
}
