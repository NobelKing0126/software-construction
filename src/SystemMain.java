import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SystemMain {
    public static void main(String[] args) {
        Map<String, String> users = new HashMap<String, String>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("校园资源预约系统");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

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
        }

        scanner.close();
    }

    private static void login(Scanner scanner, Map<String, String> users) {
        System.out.print("请输入账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        if (isUsernameValid(username) && users.containsKey(username)
                && users.get(username).equals(password)) {
            System.out.println("登录成功，欢迎使用校园资源预约系统！");
        } else {
            System.out.println("账号或密码错误，登录失败。");
        }
    }

    private static void register(Scanner scanner, Map<String, String> users) {
        System.out.print("请输入注册账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入注册密码：");
        String password = scanner.nextLine();

        if (!isUsernameValid(username)) {
            System.out.println("账号不能为空。");
            return;
        }
        if (!isPasswordValid(password)) {
            System.out.println("密码不能为空且长度不能少于6位。");
            return;
        }
        if (users.containsKey(username)) {
            System.out.println("账号已存在，注册失败。");
            return;
        }

        users.put(username, password);
        System.out.println("注册成功。");
    }

    private static boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private static boolean isPasswordValid(String password) {
        return password != null && !password.isEmpty() && password.length() >= 6;
    }
}
