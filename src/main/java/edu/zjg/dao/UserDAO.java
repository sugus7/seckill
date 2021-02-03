package edu.zjg.dao;

import edu.zjg.entity.User;

public interface UserDAO {
    User findById(Integer id);
}
