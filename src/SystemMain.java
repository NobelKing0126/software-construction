import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class SystemMain {
    public static void main(String[] args) {
        List<User> users;
        try {
            users = FileUtil.loadUsers();
        } catch (Exception e) {
            System.out.println("用户数据加载失败，请检查数据文件。");
            return;
        }
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("校园资源预约系统");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            try {
                if ("1".equals(choice)) {
                    login(scanner, users);
                } else if ("2".equals(choice)) {
                    register(scanner, users);
                } else if ("0".equals(choice)) {
                    System.out.println("已退出系统。");
                    break;
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println("系统操作失败，请稍后重试。");
            }
        }

        scanner.close();
    }

    private static void login(Scanner scanner, List<User> users) {
        System.out.print("请输入账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (password.isEmpty()) {
            throw new ParameterEmptyException("密码不能为空。");
        }
        User user = findUserByUsername(users, username);

        if (user == null) {
            throw new UserNotFoundException("用户不存在，登录失败。");
        }
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误，登录失败。");
        }
        System.out.println("登录成功，欢迎使用校园资源预约系统！");
    }

    private static void register(Scanner scanner, List<User> users) throws IOException {
        System.out.print("请输入注册账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入注册密码：");
        String password = scanner.nextLine();

        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (!isPasswordValid(password)) {
            throw new ParameterEmptyException("密码不能为空且长度不能少于6位。");
        }
        if (findUserByUsername(users, username) != null) {
            throw new UserAlreadyExistsException("账号已存在，注册失败。");
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        users.add(user);
        FileUtil.saveUsers(users);
        System.out.println("注册成功。");
        System.out.println(user);
    }

    private static User findUserByUsername(List<User> users, String username) {
        for (User user : users) {
            if (username.equals(user.getUsername())) {
                return user;
            }
        }
        return null;
    }

    private static boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private static boolean isPasswordValid(String password) {
        return password != null && !password.isEmpty() && password.length() >= 6;
    }
}
