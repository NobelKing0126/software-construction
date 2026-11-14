package dao;

import entity.User;
import java.io.IOException;

public interface UserDao {
    User findByUsername(String username);

    void add(User user);

    void save() throws IOException;
}
