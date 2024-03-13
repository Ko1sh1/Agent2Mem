import javassist.*;

public class G1_MyTranslator implements Translator {
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {}
    public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        System.out.println("Translating " + classname);
        CtClass cc = pool.get(classname);
        // 将类设为 public
        cc.setModifiers(Modifier.PUBLIC);
        // 将指定方法设为 public, 并且要写齐，public 和 static 都要写，否则会产生报错
        cc.getDeclaredMethod("testMethod").setModifiers(Modifier.PUBLIC | Modifier.STATIC);

    }
}