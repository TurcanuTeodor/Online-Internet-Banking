import java.lang.reflect.Method;

import smile.anomaly.IsolationForest;

public class SmileCheck {
    public static void main(String[] args) {
        for (Method m : IsolationForest.class.getMethods()) {
            if (m.getName().equals("fit")) {
                System.out.println(m);
            }
        }
    }
}
