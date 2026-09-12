package com.restaurant.bean;

import com.restaurant.dao.MenuItemDAO;
import com.restaurant.entity.MenuItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class MenuItemBean implements Serializable {
    private MenuItem menuItem = new MenuItem();
    private final MenuItemDAO dao = new MenuItemDAO();

    public void save() {
        dao.save(menuItem);
        menuItem = new MenuItem();
    }

    public void edit(MenuItem item) {
        menuItem = item;
    }

    public void update() {
        dao.update(menuItem);
        menuItem = new MenuItem();
    }

    public void delete(MenuItem item) {
        dao.delete(item);
    }

    public void cancel() {
        menuItem = new MenuItem();
    }

    public List<MenuItem> getMenuItems() {
        return dao.findAll();
    }

    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
}
