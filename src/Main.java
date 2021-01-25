import VelichkoA.Manager;
import ru.spbstu.pipeline.RC;

public class Main {
    public static void main(String[] args) {
        Manager manager = new Manager();
        if (manager.setConfig(args[0]) == RC.CODE_SUCCESS) {
            manager.start();
        }
    }
}
