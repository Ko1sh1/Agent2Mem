public class G3_TestApp {
    public static void main(String[] args) throws Exception{
        System.out.println(String.class.getField("hiddenValue").getName());
        // javac G3_TestApp
        // java -Xbootclasspath/p:. G3_TestApp
    }
}
