import java.util.Scanner;

public class SystemMain {
    public static void main(String[] args) {
        String adminUsername = "admin";
        String adminPassword = "123456";
        String studentUsername = "student";
        String studentPassword = "123456";

        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入账号：");
        String username = scanner.nextLine();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            System.out.println("管理员登录成功，欢迎使用校园资源预约系统！");
        } else if (studentUsername.equals(username) && studentPassword.equals(password)) {
            System.out.println("学生登录成功，欢迎使用校园资源预约系统！");
        } else {
            System.out.println("账号或密码错误，登录失败。");
        }

        scanner.close();
    }
}
