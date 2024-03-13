package Bean;

public class SimpleBean {
    private int age;
    private String name;

    public SimpleBean(int age, String name) {
        this.age = age;
        this.name = name;
    }

    public SimpleBean() {
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
