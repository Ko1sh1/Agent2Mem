package _SimpleDemo;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class SimpleExampleMaker {
    private static final ClassPool pool = ClassPool.getDefault();

    public static void main(String[] args) throws Throwable {
        // 创建一个 premain 和需要 被代理的类，具体函数实现请修改 makeClassWithMethod 的第一个参数
        Class<?> testClass = makeClassWithMethod("TestClass",
                "public static void main(String[] args) { System.out.println(\"Main class Running\"); }"
        ,false);

        Class<?> premainDemo = makeClassWithMethod("PremainDemo",
                "public static void premain(String agentArgs, java.lang.instrument.Instrumentation inst) throws Exception {" +
                        "System.out.println(agentArgs.isEmpty() ? \"null\" : agentArgs);" +
                        "System.out.println(\"premain method is invoked!\");" +
                        "}"
        ,false);

        //  创建两个类的jar文件
        makePremainJar(premainDemo,"agent.jar",true);
        makePremainJar(testClass,"demo.jar",false);

        // 写好后执行下面的内容：
        // java -javaagent:agent.jar[=options] -jar demo.jar
        // 例如: java -javaagent:agent.jar=koishi -jar demo.jar
    }

    public static Class<?> makeClassWithMethod(String className, String methodBody,boolean isSave) throws Throwable {
        CtClass ctClass = pool.makeClass(className);
        CtMethod ctMethod = CtNewMethod.make(methodBody, ctClass);
        ctClass.addMethod(ctMethod);
        if (isSave){
            ctClass.writeFile("premain/src/main/java/javas");
        }
        return ctClass.toClass();
    }


    public static void makePremainJar(Class compileClass, String jarName, boolean is_premain) throws Throwable {
        String jarFilePath = "premain/src/main/java/jars/" + jarName;
        String className = compileClass.getName().replace(".","/") + ".class";

        // 创建 Manifest 并设置内容
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (is_premain) {
            manifest.getMainAttributes().put(new Attributes.Name("Premain-Class"), compileClass.getName());
        } else {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, compileClass.getName());
        }

        // 将 class 和  Manifest 写入 jar 文件
        try (FileOutputStream fos = new FileOutputStream(jarFilePath);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            jos.putNextEntry(new JarEntry(className));
            byte[] bytes = pool.get(compileClass.getName()).toBytecode();
            jos.write(bytes);
            jos.closeEntry();
            System.out.println("JAR file created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}