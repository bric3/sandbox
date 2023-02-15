/*
 * PegJs.java
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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.HostAccess.Implementable;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import sandbox.graaljs.PegJs.IOUtils.IOSupplier;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// NOTE
// pegjs is dead : https://github.com/pegjs/pegjs/issues/667 (See s*storm https://github.com/pegjs/pegjs/issues/639)
// replacement is to use https://github.com/peggyjs/peggy
public class PegJs implements AutoCloseable {

  private static final Function<Value, String> stringify = value -> value.getContext()
                                                                         .eval("js", "JSON.stringify")
                                                                         .execute(value)
                                                                         .asString();
  private static final TypeLiteral<List<Object>> LIST_OF_OBJECT = new TypeLiteral<>() {};
  private final JsRunner jsRunner;
  private final Value peggyObject;

  public static void main(String[] args) throws ScriptException {
    try (var pegJs = PegJs.init()) {
      String grammar = "start = ('a' / 'b')+";
      var parser = pegJs.generate(grammar, "basic-example", true);
      {
        var parseResult = parser.parse("aabbabab");
        System.out.printf("grammar: \"%s\", input: \"%s\"%nresult: %s%n",
                          grammar,
                          parseResult.input(),
                          parseResult.getExpression());
      }
      {
        var parseResult = parser.parse("bad expr");
        System.out.printf("err message: %s%n" +
                          "err: %s%n" +
                          "err formatted: %s%n",
                          parseResult.getSyntaxError().message(),
                          parseResult.getSyntaxError().stringify(),
                          parseResult.getSyntaxError().formatted());
      }
    }
  }

  public static PegJs init() {
    return new PegJs();
  }

  public static abstract class IOUtils {
    public static String readFully(InputStream inputStream) {
      if (inputStream == null) {
        return null;
      }

      try (var baos = new ByteArrayOutputStream()) {
        inputStream.transferTo(baos);
        return baos.toString();
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to read input stream", e);
      }
    }

    interface IOSupplier<T> {
      T get() throws IOException;

      static <T> T uncheckIO(IOSupplier<T> supplier) {
        try {
          return supplier.get();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }

  private PegJs() {
    jsRunner = new JsRunner();

    // https://peggyjs.org/documentation
    // Load library is it was in a <script> element

    jsRunner.run(
            IOSupplier.uncheckIO(
                    () -> Source.newBuilder(JavaScriptLanguage.ID,
                                            Objects.requireNonNullElse(
                                                    PegJs.class.getResource("/peggy.min.js"),
                                                    // ⚠️ No SRI verification
                                                    new URL("https://cdn.jsdelivr.net/npm/peggy@2.0.1/browser/peggy.min.js")
                                            ))
                                .mimeType("application/javascript")
                                .build()
            )
    );

    peggyObject = jsRunner.run(Source.newBuilder(JavaScriptLanguage.ID, "peggy", "peg-js-object")
                                     .mimeType("application/javascript")
                                     .buildLiteral());
  }

  public Parser generate(CharSequence grammar) {
    return generate(grammar, null);
  }

  public Parser generate(CharSequence grammar, String grammarSource) {
    return generate(grammar, grammarSource, false);
  }

  public Parser generate(CharSequence grammar, String grammarSource, boolean generateParserSource) {
    // https://peggyjs.org/documentation.html#generating-a-parser-javascript-api
    try {
      var parser = peggyObject.invokeMember(
              "generate",
              grammar,
              ProxyObject.fromMap(Map.of(
                      "grammarSource", grammarSource != null && !grammarSource.isBlank() ? grammarSource : "unknown",
                      "output", generateParserSource ? "source" : "parser",
                      "format", "bare"
              ))
      );

      return new Parser(jsRunner, parser, grammarSource);
    } catch (PolyglotException e) {
      // handle GrammarError or SyntaxError

      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    jsRunner.close();
  }

  public static class Parser {
    private final Value parser;
    public final String source;
    public final String grammarSource;

    private Parser(JsRunner jsRunner, Value parser, String grammarSource) {
      this.grammarSource = grammarSource;
      if (parser.isString()) {
        this.source = parser.asString();
        var _parser = jsRunner.run(
                Source.newBuilder(JavaScriptLanguage.ID, source, "parser-generated-source")
                      .mimeType("application/javascript")
                      .buildLiteral()
        );

        assert _parser.hasMember("parse") : "_parser should have member \"parse\"";
        this.parser = _parser;
      } else if (parser.hasMember("parse")) {
        this.source = null;
        this.parser = parser;
      } else {
        throw new IllegalArgumentException("Parser must be a string or a peggy parser object with a parse method");
      }
    }

    public ParseResult parse(CharSequence input) {
      return parse(input, null);
    }

    public ParseResult parse(CharSequence input, String inputSource) {
      inputSource = inputSource != null && !inputSource.isBlank() ? inputSource : "unknown";
      try {
        var parseResult = parser.invokeMember("parse", input, ProxyObject.fromMap(Map.of(
                "grammarSource", inputSource // the arg name is misleading it's actually the name of the input, that can be used in the SyntaxError formatting
        )));

        return ParseResult.success(input, parseResult);
      } catch (PolyglotException e) {
        /*
        Exception in thread "main" Error: Expected "!=", ".", "<", "<=", "==", ">", ">=", "[", or whitespace but end of input found.
          at <js> peg$SyntaxError(parser-generated-source:14:313-337)
          at <js> peg$buildStructuredError(parser-generated-source:605-610:62654-62776)
          at <js> peg$parse(parser-generated-source:3785-3791:144072-144368)
          at org.graalvm.polyglot.Value.invokeMember(Value.java:973)
         */

        return ParseResult.error(grammarSource, input, inputSource, e.getGuestObject());
      }
    }
  }

  interface ParseResult {
    CharSequence input();

    Object getExpression();

    SyntaxError getSyntaxError();

    boolean isSuccess();

    String stringify();


    static ParseResult success(CharSequence input, Value parseResult) {
      return new ParseResult() {
        public CharSequence input() {
          return input;
        }

        public List<Object> getAsList() {
          return parseResult.as(LIST_OF_OBJECT);
        }

        public Object getExpression() { // TODO improve resulting object
          if (parseResult.hasArrayElements()) {
            return new ArrayList<>(parseResult.as(LIST_OF_OBJECT));
          } else if (parseResult.hasMembers()) {
            return parseResult.as(Map.class);
          } else {
            return parseResult.asString();
          }
        }

        public String stringify() {
          return stringify.apply(parseResult);
        }

        public SyntaxError getSyntaxError() {
          throw new RuntimeException("Invalid call, the parse call returned successfully");
        }

        public boolean isSuccess() {
          return true;
        }
      };
    }

    static ParseResult error(String grammarSource, CharSequence input, String inputSource, Value guestError) {
      return new ParseResult() {
        public CharSequence input() {
          return input;
        }

        public Object getExpression() {
          throw new RuntimeException("Invalid call, the parse call raised an error");
        }

        public SyntaxError getSyntaxError() {
          var syntaxError = guestError.as(SyntaxError.class);
          syntaxError.registerAdditionalValues(grammarSource, input, inputSource);
          return syntaxError;
        }

        public String stringify() {
          return stringify.apply(guestError);
        }

        public boolean isSuccess() {
          return false;
        }
      };
    }
  }

  @Implementable
  public static abstract class FormattableError {
    private String grammarSource;
    private CharSequence input;
    private String inputSource;

    public String formatted() { // don't call me 'format' otherwise the adapter will replace with a call to format without the necessary args
      assert getDelegate() != null && inputSource != null && input != null : "registerAdditionalValues not called";
      if (getDelegate().canInvokeMember("format")) {
        return getDelegate().invokeMember(
                "format",
                ProxyArray.fromArray(
                        ProxyObject.fromMap(Map.of(
                                "source", inputSource,
                                "text", input
                        ))
                )
        ).asString();
      } else {
        return "";
      }
    }

    public String stringify() {
      return stringify.apply(getDelegate());
    }

    void registerAdditionalValues(String grammarSource, CharSequence input, String inputSource) {
      this.grammarSource = grammarSource;
      this.input = input;
      this.inputSource = inputSource;
    }

    private final VarHandle DELEGATE_HANDLE;
    {
      try {
        // com.oracle.truffle.host.HostAdapterBytecodeGenerator.HostAdapterBytecodeGenerator
        DELEGATE_HANDLE = MethodHandles.privateLookupIn(getClass(), MethodHandles.lookup())
                                       .findVarHandle(getClass(), "delegate", Value.class);
      } catch (ReflectiveOperationException e) {
        throw new ExceptionInInitializerError(e);
      }
    }

    Value getDelegate() {
      return (Value) DELEGATE_HANDLE.get(this);
    }
  }

  @Implementable // Needed when host access is explicit
  public static abstract class SyntaxError extends FormattableError {
    public String message() {
      Value delegate = getDelegate();
      return delegate.invokeMember("toString").asString();
    }

    public abstract LocationRange location();

    public abstract Value found();

    public abstract List<Value> expected();


    @Implementable
    interface LocationRange {
      String source();

      Position start();

      Position end();

      @Implementable
      abstract class Position {
        public abstract int offset();

        public abstract int line();

        public abstract int column();
      }
    }
  }

  static class JsRunner implements Closeable {
    private final Context ctx;
    public final OutputStream out;

    public final OutputStream err;

    public JsRunner() {
      this(new ByteArrayOutputStream(), new ByteArrayOutputStream());
    }

    public JsRunner(OutputStream out, OutputStream err) {
      this.ctx = makeGraalvmPolyglotContext(out, err);
      this.out = out;
      this.err = err;
    }

    public Value run(Source source) {
      return run(source, Function.identity());
    }

    public <R> R run(Source source, Function<Value, R> converter) {
      try {
        ctx.enter();
        return converter.apply(ctx.eval(source));
      } finally {
        ctx.leave();
      }
    }


    private Context makeGraalvmPolyglotContext(OutputStream out, OutputStream err) {
      return Context.newBuilder(JavaScriptLanguage.ID)
                    .engine(makePolyglotEngine().build())
                    .out(out)
                    .err(err)
                    .allowExperimentalOptions(true) // Needed for loading from classpath
                    .allowHostAccess(getHostAccess()) // Allow JS access to public Java methods/members
                    .allowHostClassLookup(className -> true) // Allow JS access to public Java classes
                    .allowIO(false)
                    .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                    // Allow getMember(), when they are exported in js 'export ...'
                    // https://www.graalvm.org/22.3/reference-manual/js/Modules/#experimental-module-namespace-exports
                    .option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true")
                    // .option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true") // Needed for sort, shift, etc.
                    .option(JSContextOptions.LOAD_FROM_CLASSPATH_NAME, "true")
                    .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                    .build();
    }

    @NotNull
    private Engine.Builder makePolyglotEngine() {
      // When graalvm is used as dependency only the interpreter mode is available
      // Indeed without using UseJVMCICompiler and patching the module path (--module-path --upgrade-module-path)
      // graalvm cannot produce compiled code, and runs in "interpreter only"
      var engineBuilder = Engine.newBuilder()
                                .option("engine.WarnInterpreterOnly", "false");

      if (isDebugging() & isEnablingChromeDebugger()) {
        // Debug
        // https://www.graalvm.org/latest/tools/chrome-debugger/#programmatic-launch-of-inspector-backend
        // https://stackoverflow.com/questions/68762814/how-to-add-sourcemaps-to-graalvm-js-inspection
        // need org.graalvm.tools:chromeinspector dep
        var port = "4242";
        var path = "peggyjs";

        engineBuilder.option("inspect", port)
                     .option("inspect.Path", path)
                     .option("inspect.Suspend", "true") // change to true to suspend on first line
                     .option("inspect.WaitAttached", "false");
      }
      return engineBuilder;
    }

    private HostAccess getHostAccess() {
      return HostAccess.EXPLICIT;
    }

    private boolean isDebugging() {
      return ProcessHandle.current()
                          .info()
                          .arguments()
                          .map(args -> Arrays.stream(args).anyMatch(arg -> arg.contains("-agentlib:jdwp")))
                          .orElse(false);
    }

    private boolean isEnablingChromeDebugger() {
      return false;
    }

    @Override
    public void close() {
      ctx.close();
    }
  }
}