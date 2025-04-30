package sandbox.methodhandles;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

public class LambdaMetaFactoryPlayground {
  public static void main(String[] args) throws Throwable {
    BasicHelloWorld.demo();
    LambdaWithArgExample.demo();
    CapturedFirstArgLambdaExample.demo();
    DynamicFunctionWithMethodHandle.demo();
    MultiTargetWithTargetMHModificationsLambdaExample.demo();
    UniversalLambdaFactory.demo();
  }
}

/// Equivalent to make a lambda like
///
/// ```java
///   Runnable r = () -> sayHello();
///```
class BasicHelloWorld {
  public static void demo() throws Throwable {
    // MethodHandles.Lookup provides reflective access
    var lookup = MethodHandles.lookup();

    // Target method we want the lambda to call
    var targetMethodHandle = lookup.findStatic(
            BasicHelloWorld.class,
            "sayHello",
            MethodType.methodType(void.class)
    );

    // Signature of the functional interface (Runnable.run): ()V
    var invokedType = MethodType.methodType(Runnable.class); // The type of the lambda expression
    var samMethodType = MethodType.methodType(void.class);   // SAM method: void run()
    var instantiatedMethodType = MethodType.methodType(void.class); // The actual method: void sayHello()

    // Create the CallSite using LambdaMetafactory
    var callSite = LambdaMetafactory.metafactory(
            // lookup context with the accessibility privileges of the caller, needs enough privileges
            lookup,
            // name of the method in the functional interface
            "run",
            // type of the functional interface, expected signature of the CallSite
            invokedType,
            // type of the SAM method, signature of the functional interface
            samMethodType,
            // What method implements this lambda
            targetMethodHandle,
            // The actual method signature to be called
            instantiatedMethodType
    );

    // Get the Runnable instance
    var r = (Runnable) callSite.getTarget().invokeExact();

    // Use it!
    r.run();
  }

  public static void sayHello() {
    System.out.println("Hello from LambdaMetafactory!");
  }
}


/// Equivalent to make a capturing lambda like
///
/// ```java
/// IntUnaryOperator op = (int x) -> increment(x);
///```
class LambdaWithArgExample {
  public static void demo() throws Throwable {
    var lookup = MethodHandles.lookup();

    // Target method that does the actual work: (int) -> int
    var targetMethodHandle = lookup.findStatic(
            LambdaWithArgExample.class,
            "increment",
            MethodType.methodType(int.class, int.class)
    );

    // Functional interface: IntUnaryOperator (method: int applyAsInt(int))
    var invokedType = MethodType.methodType(IntUnaryOperator.class);
    var samMethodType = MethodType.methodType(int.class, int.class); // SAM: applyAsInt(int):int
    var instantiatedMethodType = MethodType.methodType(int.class, int.class); // actual method: increment(int):int

    var callSite = LambdaMetafactory.metafactory(
            lookup,
            "applyAsInt",
            invokedType,
            samMethodType,
            targetMethodHandle,
            instantiatedMethodType
    );

    var op = (IntUnaryOperator) callSite.getTarget().invokeExact();

    // Test the lambda
    int result = op.applyAsInt(41);
    System.out.println("Result: " + result); // should print 42
  }

  public static int increment(int x) {
    return x + 1;
  }
}

/// Equivalent to make a capturing lambda like
///
/// ```java
/// IntUnaryOperator makeAdder(int base){
///     return x -> add(base, x);
///}
///```
///
/// * The target method [#add(int,int)] takes both `base` and `x` → `add(int base, int x)`
/// * The lambda will capture `base` at creation time, and expose just `x` at invocation time.
/// * The `factoryType` is `(int) → IntUnaryOperator`, so the factory method returns the lambda
/// * This statement `callSite.getTarget().invoke(10)`, means it's capturing `10` as `base`
class CapturedFirstArgLambdaExample {
  public static void demo() throws Throwable {
    var lookup = MethodHandles.lookup();

    // We want to generate a lambda like: x -> x + base
    // So we need to capture `base` when creating the lambda

    // Get a method handle to the actual logic (takes base and x)
    var target = lookup.findStatic(
            CapturedFirstArgLambdaExample.class,
            "add",
            MethodType.methodType(int.class, int.class, int.class) // (int base, int x) -> int
    );

    // invokedType: method that takes the captured arg and returns the lambda
    // i.e., (int) -> IntUnaryOperator
    var factoryType = MethodType.methodType(IntUnaryOperator.class, int.class);

    // SAM: int applyAsInt(int x)
    var samMethodType = MethodType.methodType(int.class, int.class);

    // The actual implementation method: add(int base, int x)
    var implMethodType = MethodType.methodType(int.class, int.class);

    var callSite = LambdaMetafactory.metafactory(
            lookup,
            "applyAsInt",
            factoryType,          // (int) -> IntUnaryOperator
            samMethodType,        // signature of applyAsInt: (int) -> int
            target,               // MethodHandle of add(base, x)
            implMethodType        // instantiated: (int x) -> int (base is captured)
    );

    // Now we "bind" base = 10 and get back a lambda: x -> x + 10
    var add10 = (IntUnaryOperator) callSite.getTarget().invokeExact(10);

    // Try it
    System.out.println("add10(5) = " + add10.applyAsInt(5)); // prints 15
  }

  // The actual method used in the lambda — takes base and x
  public static int add(int base, int x) {
    return x + base;
  }
}

/// Dynamic lambda creation, where the lambda is built using another method handle
/// as a captured argument.
///
/// ```java
/// Function<Object, Object> f = x -> capturedMethodHandle.invokeExact(x)
///```
///
/// * Create a lambda factory that takes a method handle and returns a `Function`.
///   `factoryType` = `MethodType.methodType(Function.class, MethodHandle.class)`
/// * The `interfaceMethodType` is `MethodType.methodType(Object.class, Object.class)` which
///   is the signature of the `apply` method in `Function` and we make this function taking
///   an `Object` and returning an `Object`.
class DynamicFunctionWithMethodHandle {
  public static void demo() throws Throwable {
    var lookup = MethodHandles.lookup();

    // Target method: takes an Object and returns an Object (in this case, a String)
    var target = lookup.findStatic(
            DynamicFunctionWithMethodHandle.class,
            "toUpperCase",
            MethodType.methodType(Object.class, Object.class)
    );

    // We want to create a lambda: x -> methodHandle.invoke(x)
    // So we capture the MethodHandle as an argument to the factory

    var factoryType = MethodType.methodType(Function.class, MethodHandle.class); // factory takes MH and returns Function
    var samMethodType = MethodType.methodType(Object.class, Object.class);       // Function.apply(Object):Object
    var implMethodType = MethodType.methodType(Object.class, Object.class);      // the call to methodHandle.invoke(x)

    // The handle we'll call in the lambda needs to bind the MethodHandle (from capture) and invoke it at runtime
    var invoker = lookup.findVirtual(
            MethodHandle.class, "invokeExact",
            MethodType.methodType(Object.class, Object.class)
    );

    var callSite = LambdaMetafactory.metafactory(
            lookup,
            "apply",  // SAM method of java.util.function.Function
            factoryType,                  // (MethodHandle) -> Function
            samMethodType,                // Function.apply(Object):Object
            invoker,                      // MethodHandle::invokeExact(Object):Object
            implMethodType                // target call: Object -> Object
    );

    // Create the lambda by capturing the target method handle
    var func = (Function<Object, Object>) callSite.getTarget().invokeExact(target);

    // Try it
    System.out.println(func.apply("hello")); // prints HELLO
  }

  public static Object toUpperCase(Object o) {
    return o.toString().toUpperCase();
  }
}

/// Dynamic lambda creation with multiple targets,
/// where the lambda is built using another method handle.
///
/// This example requires tweaking the targets to match the signature of the lambda.
/// I.e. by using [MethodHandles#dropArguments] or [MethodHandles#insertArguments].
class MultiTargetWithTargetMHModificationsLambdaExample {
  public static void demo() throws Throwable {
    var lookup = MethodHandles.lookup();

    // Lambda factory signature: (MethodHandle) -> Function<Object, Object>
    var factoryType = MethodType.methodType(Function.class, MethodHandle.class);
    var samMethodType = MethodType.methodType(Object.class, Object.class); // Function.apply(Object):Object
    var implMethodType = MethodType.methodType(Object.class, Object.class); // methodHandle.invokeExact(Object)

    // Invoker: MethodHandle.invokeExact(Object):Object, equivalent to
    // var invoker = lookup.findVirtual(
    //         MethodHandle.class, "invokeExact",
    //         MethodType.methodType(Object.class, Object.class)
    // );
    var invoker = MethodHandles.exactInvoker(
            MethodType.methodType(Object.class, Object.class)
    );

    // Build the factory call site
    var callSite = LambdaMetafactory.metafactory(
            lookup,
            "apply",
            factoryType,
            samMethodType,
            invoker,
            implMethodType
    );

    // Extract the factory target
    var lambdaFactory = callSite.getTarget();

    // --- Target 1: No-arg method => constant result
    var noArgHandle = MethodHandles.constant(Object.class, "No args here!");
    var noArgHandleAdapted = MethodHandles.dropArguments(noArgHandle, 0, Object.class); // to match (Object) -> Object
    var noArgFunc = (Function<Object, Object>) lambdaFactory.invokeExact(noArgHandleAdapted);
    System.out.println("noArgFunc: " + noArgFunc.apply(null)); // → No args here!

    // --- Target 2: One-arg method: toUpper(Object)
    var oneArgHandle = lookup.findStatic(
            MultiTargetWithTargetMHModificationsLambdaExample.class, "toUpper",
            MethodType.methodType(Object.class, Object.class)
    );
    var oneArgFunc = (Function<Object, Object>) lambdaFactory.invokeExact(oneArgHandle);
    System.out.println("oneArgFunc: " + oneArgFunc.apply("hello")); // → HELLO

    // --- Target 3: Two-arg method: concat(Object, Object), bind one arg
    var twoArgHandle = lookup.findStatic(
            MultiTargetWithTargetMHModificationsLambdaExample.class, "concat",
            MethodType.methodType(Object.class, Object.class, Object.class)
    );
    var boundHandle = MethodHandles.insertArguments(twoArgHandle, 0, "Prefix: ");
    // Now boundHandle has type (Object) -> Object
    var twoArgFunc = (Function<Object, Object>) lambdaFactory.invokeExact(boundHandle);
    System.out.println("twoArgFunc: " + twoArgFunc.apply("world")); // → Prefix: world
  }

  public static Object toUpper(Object o) {
    return o.toString().toUpperCase();
  }

  public static Object concat(Object a, Object b) {
    return "[" + a + ", " + b + "]";
  }
}

/// Universal lambda factory that can adapt any method handle to a function
///
/// This is a more advanced example that shows how to create a lambda factory
/// that can adapt any method handle to a function with a variable number of arguments.
/// It uses the `MethodHandles#asSpreader` method to transform the method handle
/// to accept an array of arguments, and then uses the `LambdaMetafactory`
/// to create the lambda.
///
/// The use of a custom invoker allows us to call the method handle with an array of arguments,
/// which is useful when the number of arguments is not known at compile time.
///
/// E.g., suppose a java method named `wrap` with the following signature: `(int, String) -> String`
/// this creates a lambda that takes an array of `Object` and calls the method handle
///
/// ```java
/// Function<Object[], Object> f = args -> methodHandleWithSpreader.invokeExact(args);
/// ```
///
/// Or more exactly
///
/// ```java
/// Function<Object[], Object> f = args -> methodHandle.invokeExact(args[0], args[1]);
/// ```
///
/// Which is equivalent to:
///
/// ```java
/// Function<Object[], Object> f = args -> wrap(args[0], args[1]);
/// ```
class UniversalLambdaFactory {
  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

  public static void demo() throws Throwable {
    // --- noArgs: () -> String
    var mh0 = lookup.findStatic(
            UniversalLambdaFactory.class, "noArgs",
            MethodType.methodType(String.class)
    );
    var fn0 = makeLambda(mh0);
    System.out.println(fn0.apply(new Object[]{})); // → "No arguments"

    // --- echo: (String) -> String
    var mh1 = lookup.findStatic(
            UniversalLambdaFactory.class, "echo",
            MethodType.methodType(String.class, String.class)
    );
    var fn1 = makeLambda(mh1);
    System.out.println(fn1.apply(new Object[]{"Hello"}));

    // --- wrap: (int, String) -> String
    var mh2 = lookup.findStatic(
            UniversalLambdaFactory.class, "wrap",
            MethodType.methodType(String.class, int.class, String.class)
    );
    var fn2 = makeLambda(mh2);
    System.out.println(fn2.apply(new Object[]{42, "data"}));

    // --- wrap: (String[]) -> String
    var mh3 = lookup.findStatic(
            UniversalLambdaFactory.class, "wrap",
            MethodType.methodType(String.class, String[].class)
    );
    var fn3 = makeLambda(mh3);
    System.out.println(fn3.apply(new Object[]{new String[]{"any", "string", "arguments"}}));
  }

  private static Function<Object[], Object> makeLambda(MethodHandle targetMH) throws Throwable {
    // Factory type: (MethodHandle) -> Function<Object[], Object>
    // i.e., how the call-site can be used to build a lambda, the type
    // describes what type of the lambda to create, here it's a `Function`
    // and from which arguments, here it's a `MethodHandle`.
    var factoryType = MethodType.methodType(Function.class, MethodHandle.class);
    
    // The interface method type: Function.apply(Object):Object
    // The method is named `apply`, and this describes the signature of the method
    // this call-site will create.
    var samMethodName = "apply";
    var samMethodType = MethodType.methodType(Object.class, Object.class);

    // This method handle represents the implementation of the lambda
    // Basically, it takes the arguments of the factory type, these arguments are "bound" (or captured)
    // by the metafactory to this method handle, so this method handle just needs
    // the remaining arguments to be passed to it, i.e. the `Object[]` which will
    // be passed via the `Function.apply(Object[])`.

    // This produces a method handle equivalent to: `(delegate, args) -> delegate.invokeExact(args)`.
    // Builds a dispatcher that executes `targetMethodHandle.invokeExact(arg1, arg2, ..., argN)`.
    var invokerMH = MethodHandles.exactInvoker(MethodType.methodType(Object.class, Object[].class));

    // Wanted external signature: Function.apply(Object[]):Object
    var dynamicMethodType = MethodType.methodType(Object.class, Object[].class);

    // Configure LambdaMetafactory, it returns a `CallSite` which
    // is a factory for the lambda of type `Function<Object[], Object>`.
    // The `Function.apply` method has a signature of (Object)Object, but we want this function
    // to accept an array of arguments, so the dynamic signature is (Object[])Object.
    var callSite = LambdaMetafactory.metafactory(
            lookup,
            samMethodName,
            factoryType,
            samMethodType,
            invokerMH,
            dynamicMethodType
    );

    // Actually the `invoke` method handle
    var lambdaFactory = callSite.getTarget();

    var spreadInvoker = targetMH
            // Transform the method handle to accept a fixed number of arguments
            .asFixedArity()
            // Transform the method handle to accept an array of arguments, i.e.,
            // instead of having `mh.invokeWithArguments(arg0, arg1)`, this allows invoking
            // using `mh.invoke(args)` where `args` is an array of `Object`.
            .asSpreader(Object[].class, targetMH.type().parameterCount())
            // Allows matching the signature of what `invoker` expects.
            .asType(MethodType.methodType(Object.class, Object[].class));

    return (Function<Object[], Object>) lambdaFactory.invokeExact(spreadInvoker);
  }

  /// Method with no arguments
  public static String noArgs() {
    return "No arguments";
  }

  /// One-arg method
  public static String echo(String s) {
    return s;
  }

  /// Two-arg method
  public static String wrap(int a, String b) {
    return "[" + a + " -> " + b + "]";
  }

  /// Two-arg method
  public static String wrap(String... strings) {
    return String.join(" - ", strings);
  }
}
