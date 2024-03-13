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
            if (!jarEntry.getName().equals("META-INF/MANIFEST.MF") && !jarEntry.getName().equals("MANIFEST.MF")) {
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
