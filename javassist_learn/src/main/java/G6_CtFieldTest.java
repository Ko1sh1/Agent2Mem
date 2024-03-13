import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

public class G6_CtFieldTest {
    public static void main(String[] args) throws Throwable{
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.get("G6_CtFieldTest$Test");
        CtField cf = new CtField(pool.get(int.class.getName()), "name", ctClass);
        cf.setModifiers(javassist.Modifier.PRIVATE);
        ctClass.addField(cf,"5+5");
        ctClass.writeFile("javassist_learn/src/main/java/class_repository");
    }
    static class Test{
        public Test(){
        }
    }
}
