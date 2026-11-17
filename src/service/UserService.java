package service;

import java.io.IOException;

import entity.User;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public interface UserService {
    Result<User> login(String username, String password);

    Result<User> register(String username, String password) throws IOException;

    void requireResourceAccess(User user);
}
