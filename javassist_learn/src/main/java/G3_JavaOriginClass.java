import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;

public class G3_JavaOriginClass {
    public static void main(String[] args) throws Throwable{
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("java.lang.String");
        CtField f = new CtField(CtClass.intType, "hiddenValue", cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);
        cc.writeFile("javassist_learn/src/main/java");
    }
}
