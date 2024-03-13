import Bean.AbstractClass;
import Bean.InterfaceClass;
import javassist.*;

public class CtClass_Learn {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(AbstractClass.class));
        pool.insertClassPath(new ClassClassPath(InterfaceClass.class));
//        // 新建类
//        CtClass ctClass = pool.makeClass("Ko1sh1");
//
//        // 设置父类为抽象类
//        CtClass superClass = pool.get(AbstractClass.class.getName());
//        ctClass.setSuperclass(superClass);

        // 上两步可直接合成为：
        CtClass ctClass = pool.makeClass("Ko1sh1",pool.get(AbstractClass.class.getName()));

        // 创建抽象 show 方法并添加
        CtMethod ctMethod = CtNewMethod.make("public void show(){String name=\"koishi\";System.out.println(name);}", ctClass);
        ctClass.addMethod(ctMethod);

        // 通过 CtClass 的方式获取接口并添加方法
        CtClass interfaceCtClass = pool.makeInterface(InterfaceClass.class.getName());
        CtMethod interface_method = CtNewMethod.abstractMethod(CtClass.voidType, "show2", null,null, interfaceCtClass);
        interfaceCtClass.addMethod(interface_method);

        // 再为原本的类添加一个接口
        ctClass.addInterface(interfaceCtClass);

        // 接口实现抽象方法的方式与抽象类的函数相同
        CtMethod method = CtNewMethod.make("public void show2() { System.out.println(\"Implemented method\"); }", ctClass);
        ctClass.addMethod(method);

        // 保存class文件
//        String savePath = "src/main/java/class_repository/Bean/class_repository";
//        ctClass.writeFile(savePath);

        // 生成实例化对象
//        ctClass.toClass().newInstance();
        Class instance_class = ctClass.toClass();
        Object instance = instance_class.newInstance();
        ((AbstractClass)instance).show();
        ((InterfaceClass)instance).show2();

        // 类冻结
        try {
            ctClass.toClass();
        }catch (javassist.CannotCompileException e){
            System.out.println("已调用 writeFile(), toClass(), toBytecode() 方法转换成一个类文件，此 CtClass 对象已被冻结，不允许再修改");

            // 解冻
            ctClass.defrost();
            method = CtNewMethod.make("public void show3() { System.out.println(\"HAHA! I'm fine again\"); }", ctClass);
            ctClass.addMethod(method);
            try{
                instance = ctClass.toClass().newInstance();
                instance.getClass().getMethod("show3").invoke(instance);
            }catch (javassist.CannotCompileException ex){
                System.out.println("解冻后，即使可以修改class内容，但是也不能再重新实例化了");
            }
        }
    }
}
