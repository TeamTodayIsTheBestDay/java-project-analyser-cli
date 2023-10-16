package team.todaybest.analyser.helper;

/**
 * 和Shell交互相关的工具
 * @author cinea
 */
public class ShellHelper {
    public static class PleaseWaitThread extends Thread{
        public boolean missionFinish = false;

        @Override
        public void run() {
            System.out.print("Please wait");
            try {
                while (!missionFinish){
                    Thread.sleep(200);
                    System.out.print('.');
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println();
        }
    }
}
