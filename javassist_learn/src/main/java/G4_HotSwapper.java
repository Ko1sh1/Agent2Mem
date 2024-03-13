import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.util.HotSwapper;

public class G4_HotSwapper {
    static class Person{
        public void say(){
            System.out.println("whoami???");
        }
    }

    public static void main(String[] args) throws Throwable{
        Person person = new Person();

        /* 创建线程循环调用Person类的 say 方法 */
        new Thread(() -> {
            while (true){
                person.say();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        /* Javassist 运行时修改 Person 类的 say 方法 */
        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = classPool.get(person.getClass().getName());

        CtMethod ctMethod = ctClass.getDeclaredMethod("say");
        ctMethod.setBody("System.out.println(\"Oh, I'm Ko1sh1!\");");

        /*
         * HotSwapper热修改Person类，需要开启 JPDA，监听 8000 端口
         * java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000
         */
        HotSwapper hs = new HotSwapper(8000);
        hs.reload(person.getClass().getName(), ctClass.toBytecode());
    }
}