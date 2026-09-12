package com.restaurant.bean;

import com.restaurant.dao.CustomerDAO;
import com.restaurant.entity.Customer;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class CustomerBean implements Serializable {
    private Customer customer = new Customer();
    private final CustomerDAO dao = new CustomerDAO();

    public void save() {
        dao.save(customer);
        customer = new Customer();
    }

    public void edit(Customer c) {
        customer = c;
    }

    public void update() {
        dao.update(customer);
        customer = new Customer();
    }

    public void delete(Customer c) {
        dao.delete(c);
    }

    public void cancel() {
        customer = new Customer();
    }

    public List<Customer> getCustomers() {
        return dao.findAll();
    }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
