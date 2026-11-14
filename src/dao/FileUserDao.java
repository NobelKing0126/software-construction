package dao;

import entity.User;
import utils.FileUtil;

import java.io.IOException;
import java.util.List;

public class FileUserDao implements UserDao {
    private final List<User> users;

    public FileUserDao() throws IOException {
        users = FileUtil.loadUsers();
    }

    @Override
    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public void add(User user) {
        users.add(user);
    }

    @Override
    public void save() throws IOException {
        FileUtil.saveUsers(users);
    }
}
