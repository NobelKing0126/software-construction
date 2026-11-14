package ui.console;

import java.util.List;

import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ConsoleOutput {
    private ConsoleOutput() {
    }

    static void showMainMenu() {
        System.out.println("校园资源预约系统");
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("0. 退出");
        System.out.print("请选择：");
    }

    static void showResourceMenu(boolean admin) {
        if (admin) {
            System.out.println("管理员资源管理菜单");
            System.out.println("1. 新增资源");
            System.out.println("2. 查询全部资源");
            System.out.println("3. 修改资源");
            System.out.println("4. 删除资源");
            System.out.println("5. 按名称查询资源");
            System.out.println("6. 按 ID 查询资源");
            System.out.println("7. 查询全部预约");
            System.out.println("8. 按记录 ID 审核预约");
            System.out.println("9. 按条件查询预约");
            System.out.println("13. 查看全部待审核预约");
        } else {
            System.out.println("学生资源查询菜单");
            System.out.println("1. 查询可预约资源");
            System.out.println("5. 按名称查询资源");
            System.out.println("6. 按 ID 查询资源");
            System.out.println("10. 提交预约申请");
            System.out.println("11. 查询我的预约");
            System.out.println("12. 取消我的预约");
        }
        System.out.println("0. 退出登录");
        System.out.print("请选择：");
    }

    static void showResult(Result<?> result) {
        System.out.println(result.getMsg());
        if (result.isSuccess() && result.getData() != null) {
            if (result.getData() instanceof List<?>) {
                for (Object item : (List<?>) result.getData()) {
                    System.out.println(item);
                }
            } else {
                System.out.println(result.getData());
            }
        }
    }
}
