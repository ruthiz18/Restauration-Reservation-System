package com.restaurant.dao;

import com.restaurant.entity.Customer;
import com.restaurant.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CustomerDAO {
    public void save(Customer customer) {
        execute(session -> session.persist(customer));
    }

    public void update(Customer customer) {
        execute(session -> session.merge(customer));
    }

    public void delete(Customer customer) {
        execute(session -> session.remove(session.merge(customer)));
    }

    public List<Customer> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Customer order by id desc", Customer.class).list();
        }
    }

    private void execute(java.util.function.Consumer<Session> action) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            action.accept(session);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
