package com.restaurant.dao;

import com.restaurant.entity.MenuItem;
import com.restaurant.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MenuItemDAO {
    public void save(MenuItem item) {
        execute(session -> session.persist(item));
    }

    public void update(MenuItem item) {
        execute(session -> session.merge(item));
    }

    public void delete(MenuItem item) {
        execute(session -> session.remove(session.merge(item)));
    }

    public List<MenuItem> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from MenuItem order by id desc", MenuItem.class).list();
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
