package com.restaurant.reservation.repository;

import com.restaurant.reservation.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** BR-03: search by name, email or phone with a single keyword. */
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR c.phone            LIKE CONCAT('%', :keyword, '%')
            ORDER BY c.lastName ASC, c.firstName ASC
            """)
    List<Customer> search(@Param("keyword") String keyword);
}
