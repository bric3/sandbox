///usr/bin/env jbang "$0" ; exit $?
//JAVA 11
//DEPS org.graalvm.js:js-scriptengine:22.1.0.1


package sandbox.graaljs;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.StringWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.security.AccessController;

// Needs
// org.graalvm.js:js:22.1.0.1
// org.graalvm.js:js-scriptengine:22.1.0.1
public class GraalJsDemo {
  public static void main(String[] args) throws ScriptException {
    var out = new StringWriter();

    ScriptEngine engine = getScriptEngine();

    engine.getContext().setWriter(out);

    engine.eval("print('Hello World! (print)');");
    engine.eval("console.log('Hello, World! (console.log)')");   // likely to raise a warning mbean registration

    System.out.println("From graal js: " + out);
  }

  private static ScriptEngine getScriptEngine() {
    return GraalJSScriptEngine.create(
            null,
            Context.newBuilder("js")
                   .allowExperimentalOptions(true) // Needed for loading from classpath
                   .allowHostAccess(getHostAccess()) // Allow JS access to public Java methods/members
                   .allowHostClassLookup(s -> true) // Allow JS access to public Java classes
                   .allowIO(false)
                   .option(JSContextOptions.LOAD_FROM_CLASSPATH_NAME, "true")
                   .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2021")
    );
    // Or by
    // ScriptEngineManager manager = new ScriptEngineManager();
    // return manager.getEngineByName("graal.js");
  }

  private static HostAccess getHostAccess() {
    // return HostAccess.ALL;
    // or something more precise
    return HostAccess.newBuilder()
                     .allowPublicAccess(true) // hopefully will get filtering support here - https://github.com/oracle/graal/issues/2425
                     .allowAllImplementations(true)
                     .allowArrayAccess(true)
                     .allowListAccess(true)
                     .allowIterableAccess(true)
                     .allowIteratorAccess(true)
                     .allowBufferAccess(true)
                     .allowMapAccess(true)
                     .denyAccess(ClassLoader.class)
                     .denyAccess(Member.class) // includes Method, Field and Constructor
                     .denyAccess(AnnotatedElement.class) // includes Class
                     .denyAccess(Proxy.class)
                     .denyAccess(Object.class, false) // wait(), notify(), getClass()
                     .denyAccess(System.class) // setProperty(), getProperty(), gc(), exit()
                     .denyAccess(SecurityManager.class)
                     .denyAccess(AccessController.class)
                     .build();
    // or
    // return HostAccess.newBuilder(HostAccess.EXPLICIT)
    //                  .targetTypeMapping(
    //                          org.graalvm.polyglot.Value.class,
    //                          Object.class,
    //                          org.graalvm.polyglot.Value::hasArrayElements,
    //                          v -> new LinkedList<>(v.as(List.class)))
    //                  .build();
  }
}