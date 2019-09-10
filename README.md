# TestBed
Песочница для запуска консольных Java-программ под Android


![screenshot](https://github.com/vv73/TestBed/blob/master/screenshot.png)


**Ограничения:**

Главный класс должен располагаться в корне (пример - MyProgram.java) 

![tree](https://github.com/vv73/TestBed/blob/master/tree.png)

Пример консольной программы
```java
import java.util.Scanner;
public class MyProgram{
    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        System.out.println("Enter two numbers");
        int x = in.nextInt(), y = in.nextInt();
        int sum = x + y;
        System.out.println("Their sum is " + sum);
    }
}
```
