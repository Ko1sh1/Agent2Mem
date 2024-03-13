import javassist.ClassPool;
import javassist.Loader;
import javassist.Translator;

public class G1_Translator_Learn {
    public static void main(String[] args) throws Throwable {
        Translator t = new G1_MyTranslator();
        ClassPool pool = ClassPool.getDefault();
        Loader cl = new Loader();
        cl.addTranslator(pool, t);
        // 这里的 args 就是触发 TestApp main 方法传入的 args
        cl.run("G1_TestApp", args);
    }
}
