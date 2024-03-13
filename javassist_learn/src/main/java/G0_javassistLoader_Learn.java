import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;

public class G0_javassistLoader_Learn {
    public static void main(String[] args) throws Exception{
        ClassPool pool = ClassPool.getDefault();
        Loader cl = new Loader(pool);

        CtClass ct = pool.get("Bean.SimpleBean");
        ct.setSuperclass(pool.get("Bean.FatherBean"));

        Class c = cl.loadClass("Bean.SimpleBean");
        Object instance = c.newInstance();

        // 可以发现成功继承了父类，说明加载了被修改后的字节
        System.out.println(instance.getClass().getSuperclass());
        instance.getClass().getMethod("setId",int.class).invoke(instance,21);
        System.out.println(instance.getClass().getMethod("getId").invoke(instance));
    }
}
