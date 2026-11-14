package service;

import java.io.IOException;
import java.time.LocalDateTime;

import dao.UserDao;
import entity.User;
import exception.ParameterEmptyException;
import exception.PasswordErrorException;
import exception.PermissionDeniedException;
import exception.UserAlreadyExistsException;
import exception.UserNotFoundException;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class UserService {
    private final UserDao users;

    public UserService(UserDao users) {
        this.users = users;
    }

    public Result<User> login(String username, String password) {
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (password == null || password.isEmpty()) {
            throw new ParameterEmptyException("密码不能为空。");
        }
        User user = users.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("用户不存在，登录失败。");
        }
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误，登录失败。");
        }
        return Result.success("登录成功，欢迎使用校园资源预约系统！", user);
    }

    public Result<User> register(String username, String password) throws IOException {
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (!isPasswordValid(password)) {
            throw new ParameterEmptyException("密码不能为空且长度不能少于6位。");
        }
        if (users.findByUsername(username) != null) {
            throw new UserAlreadyExistsException("账号已存在，注册失败。");
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        users.add(user);
        users.save();
        return Result.success("注册成功。", user);
    }

    public void requireResourceAccess(User user) {
        if (!"admin".equals(user.getRole()) && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }
    }

    private boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private boolean isPasswordValid(String password) {
        return password != null && !password.isEmpty() && password.length() >= 6;
    }
}
