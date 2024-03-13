import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class G1_TestApp {
    private static String testMethod(){
        return "test, test!";
    }

    public static void main(String[] args) throws Exception{
        Method[] methods = G1_TestApp.class.getDeclaredMethods();
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                System.out.println("Method " + method.getName() + " is public");
            }

            if (Modifier.isPrivate(modifiers)) {
                System.out.println("Method " + method.getName() + " is private");
            }
        }
    }
}
