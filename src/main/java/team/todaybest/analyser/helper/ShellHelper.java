package team.todaybest.analyser.helper;

import java.util.List;
import java.util.Scanner;

/**
 * 和Shell交互相关的工具
 *
 * @author cinea
 */
public class ShellHelper {
    public static class PleaseWaitThread extends Thread {
        public boolean missionFinish = false;

        @Override
        public void run() {
            System.out.print("Please wait");
            try {
                while (!missionFinish) {
                    Thread.sleep(200);
                    System.out.print('.');
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println();
        }
    }

    public static int consoleChooseOne(List<String> list) {
        System.out.println("*".repeat(15));
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("%2d) %s%n", i + 1, list.get(i));
        }
        System.out.println("*".repeat(15));

        var sc = new Scanner(System.in);
        while (true) {
            System.out.print("Please choose one: ");
            var c = sc.nextInt();
            if (c > 0 && c <= list.size()) {
                return c - 1;
            }
            System.out.println("Invalid input! Please try again.");
        }
    }
}
