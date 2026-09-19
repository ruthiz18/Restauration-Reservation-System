package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.CustomerRequest;
import com.restaurant.reservation.dto.CustomerResponse;
import com.restaurant.reservation.entity.Customer;
import com.restaurant.reservation.entity.ReservationStatus;
import com.restaurant.reservation.exception.BusinessRuleException;
import com.restaurant.reservation.exception.DuplicateResourceException;
import com.restaurant.reservation.exception.ResourceNotFoundException;
import com.restaurant.reservation.repository.CustomerRepository;
import com.restaurant.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Customer.
 *
 * BR-01 create, BR-02 unique email, BR-03 list + search,
 * BR-04 update, BR-05 delete, BR-09 validation before persisting.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;

    public CustomerService(CustomerRepository customerRepository,
                           ReservationRepository reservationRepository) {
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
    }

    /** CREATE - BR-01, BR-02. */
    public CustomerResponse create(CustomerRequest request) {
        String email = normaliseEmail(request.getEmail());

        // BR-02: two customers may not share an email address.
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "A customer is already registered with the email " + email);
        }

        Customer customer = new Customer();
        apply(request, customer, email);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    /** READ all - BR-03. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    /** READ one. */
    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(getEntity(id));
    }

    /** READ by search keyword across name, email and phone - BR-03. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return customerRepository.search(keyword.trim()).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    /** UPDATE - BR-04, with the BR-02 uniqueness check re-applied. */
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getEntity(id);
        String email = normaliseEmail(request.getEmail());

        boolean emailChanged = !customer.getEmail().equalsIgnoreCase(email);
        if (emailChanged && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "Another customer is already registered with the email " + email);
        }

        apply(request, customer, email);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    /**
     * DELETE - BR-05.
     * A customer holding active reservations cannot be removed; the reservations
     * must be cancelled or completed first, so history stays consistent.
     */
    public void delete(Long id) {
        Customer customer = getEntity(id);

        boolean hasActiveReservations = reservationRepository
                .existsByCustomerIdAndStatusIn(id, ReservationStatus.activeStatuses());

        if (hasActiveReservations) {
            throw new BusinessRuleException(
                    "Customer " + customer.getFullName() + " has active reservations. "
                            + "Cancel or complete them before deleting this customer.");
        }

        customerRepository.delete(customer);
    }

    /** Shared lookup so other services can resolve a customer entity. */
    @Transactional(readOnly = true)
    public Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private void apply(CustomerRequest request, Customer customer, String email) {
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setEmail(email);
        customer.setPhone(request.getPhone().trim());
        customer.setAddress(request.getAddress() == null ? null : request.getAddress().trim());
    }

    private String normaliseEmail(String email) {
        return email.trim().toLowerCase();
    }
}
