import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import javassist.*;

import java.io.IOException;

public class G2_SelfClassLoader_Learn extends ClassLoader {


    private ClassPool pool;

    public G2_SelfClassLoader_Learn() throws NotFoundException {
        pool = new ClassPool();
        pool.insertClassPath("javassist_learn/src/main/java/g2_classes"); // 指定 class 文件位置，该目录不能是程序的 class 输出位置，否则 JVM 会用默认的类加载器去加载该类。
    }

    // 调用 指定类 的 main 方法
    public static void main(String[] args) throws Throwable {
        G2_SelfClassLoader_Learn selfLoader = new G2_SelfClassLoader_Learn();
        Class c = selfLoader.loadClass("G2_TestApp");
        c.getDeclaredMethod("main", new Class[] { String[].class }).invoke(null, new Object[] { args });
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            CtClass cc = pool.get(name);
            // 在这里可以自己自定义去动态修改一些内容
            // 比如像 CC 中常见的那样插入一个弹计算器方法，这样每个使用该加载器加载的类都会弹计算器
            pool.insertClassPath(new ClassClassPath(AbstractTranslet.class));
            String cmd = "java.lang.Runtime.getRuntime().exec(\"%s\");";
            cmd = String.format(cmd, "calc");
            cc.makeClassInitializer().insertBefore(cmd);
            cc.setSuperclass(pool.get(AbstractTranslet.class.getName()));

//            cc.setSuperclass(pool.get(AbstractTranslet.class.getName()));
            byte[] b = cc.toBytecode();
            return defineClass(name, b, 0, b.length);
        } catch (NotFoundException | IOException | CannotCompileException e) {
            e.printStackTrace();
            return null;
        }
    }
}