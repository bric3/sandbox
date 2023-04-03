package sandbox;

import java.lang.reflect.AccessFlag;

public class AccessFlagMain {
    public static void main(String[] args) {
        // lookout for the new ClassFile API (JDK21 ?)
        AccessFlag.maskToAccessFlags(AccessFlagMain.class.getModifiers(), AccessFlag.Location.CLASS)
                  .forEach(System.out::println);
    }
}
