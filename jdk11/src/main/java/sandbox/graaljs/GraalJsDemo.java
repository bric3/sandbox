/*
 * GraalJsDemo.java
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
///usr/bin/env jbang "$0" ; exit $?
//JAVA 11
//DEPS org.graalvm.js:js:22.1.0.1
//DEPS org.graalvm.js:js-scriptengine:22.1.0.1


package sandbox.graaljs;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.util.Date;
import java.util.Map;

public class GraalJsDemo {
  public static void main(String[] args) throws ScriptException {
    (new ScriptEngineManager())
            .getEngineFactories()
            .stream()
            .map(f -> f.getLanguageName() + " " + f.getEngineName() + " " + f.getNames().toString())
            .forEach(System.out::println);

    var printStatement = "print('Hello World! (print)');";
    var consoleLogStatement = "console.log('Hello, World! (console.log)')";

    var stringifyFunctionStatement = "(o, replacer, space) => { return JSON.stringify(o, replacer, space); }";
    // var stringifyFunctionStatement = "(o) => JSON.stringify(o)";

    {
      var out = new StringWriter();
      try (var engine = usingScriptEngineApi()) {
        engine.getContext().setWriter(out);
        engine.getContext().setErrorWriter(out);

        engine.eval(printStatement);
        engine.eval(consoleLogStatement);   // likely to raise a warning mbean registration
        System.out.println("From graal js: \n------------\n" + out + "\n----\n");
      }
    }

    {
      var print = Source.create(JavaScriptLanguage.ID, printStatement);
      var consoleLog = Source.create(JavaScriptLanguage.ID, consoleLogStatement);
      var stringifyFunction = Source.create(JavaScriptLanguage.ID, stringifyFunctionStatement);
      var out = new ByteArrayOutputStream();
      try (var ctx = usingGraalVMPolyglotApi(out)) {
        ctx.eval(print);
        ctx.eval(consoleLog);

        // bindings
        ctx.getBindings(JavaScriptLanguage.ID).putMember("r", new Record());
        Value evalResult0 = ctx.eval(JavaScriptLanguage.ID, "r.name = 'John'; r.age = 42; " +
                                                            "print('Is java obj: ' + Java.isJavaObject(r)); " +
                                                            "({ n: r.name, i: 42 }) " +
                                                            "");

        System.out.println(evalResult0.as(Map.class));

        Value evalResult1 = ctx.eval(JavaScriptLanguage.ID, "var nativeDate = new Date(new Date().toLocaleString(\"en-US\", {timeZone: \"Europe/Berlin\"}));\n" +
                                                            "nativeDate.setHours(12);\n" +
                                                            "nativeDate.setMinutes(0);\n" +
                                                            "nativeDate.setSeconds(0);\n" +
                                                            "nativeDate.setMilliseconds(0);\n" +
                                                            "({nativeDate: nativeDate})");
        System.out.println("evalResult: " + evalResult1.getMember("nativeDate").as(Date.class));

        // call function
        Value f = ctx.eval(stringifyFunction);
        Value r = f.execute(Value.asValue(new Record()), null, 2);

        System.out.println(
                "From graal vm: \n------------\n" +
                out +
                "\n" +
                "stringify function: " + r.asString() +
                "\n----\n"
        );
      }
    }
  }

  static public class Record {
    public final int a = 10_200;
    public final String s = "Hello, World!";
    final String notPublic = "Not public";

    @Override
    public String toString() {
      return "Record{" +
             "a=" + a +
             ", s='" + s + '\'' +
             ", notPublic='" + notPublic + '\'' +
             '}';
    }
  }

  private static Context usingGraalVMPolyglotApi(ByteArrayOutputStream out) {
    return Context.newBuilder(JavaScriptLanguage.ID)
                  .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                  .engine(Engine.newBuilder()
                                .option("engine.WarnInterpreterOnly", "false")
                                .build()
                  )
                  .out(out)
                  .err(out)
                  .allowExperimentalOptions(true) // Needed for loading from classpath
                  .allowHostAccess(getHostAccess()) // Allow JS access to public Java methods/members
                  .allowHostClassLookup(className -> true) // Allow JS access to public Java classes
                  .allowIO(false)
                  .option(JSContextOptions.LOAD_FROM_CLASSPATH_NAME, "true")
                  .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                  // https://www.graalvm.org/latest/tools/chrome-debugger/#programmatic-launch-of-inspector-backend
                  // https://stackoverflow.com/questions/68762814/how-to-add-sourcemaps-to-graalvm-js-inspection
                  // .option("inspect", "4444")
                  .build();
  }

  private static GraalJSScriptEngine usingScriptEngineApi() {
    return GraalJSScriptEngine.create(
            // need to pass the engine with the option here to avoid the warning
            Engine.newBuilder()
                  .option("engine.WarnInterpreterOnly", "false")
                  .build(),
            Context.newBuilder(JavaScriptLanguage.ID)
                   .allowExperimentalOptions(true) // Needed for loading from classpath
                   .allowHostAccess(getHostAccess()) // Allow JS access to public Java methods/members
                   .allowHostClassLookup(className -> true) // Allow JS access to public Java classes

                   .allowIO(false)
                   .option(JSContextOptions.LOAD_FROM_CLASSPATH_NAME, "true")
                   .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                   .option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true")
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
                     // .allowAccessAnnotatedBy(HostAccess.Export.class) // require annotation on public members
                     .allowAllImplementations(true)
                     .allowAllClassImplementations(true)
                     .allowAccessInheritance(true)
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
    //                          Value.class, List.class,
    //                          Value::hasArrayElements,
    //                          (v) -> new ArrayList(v.as(List.class))
    //                  )
    //                  .build();
  }
}