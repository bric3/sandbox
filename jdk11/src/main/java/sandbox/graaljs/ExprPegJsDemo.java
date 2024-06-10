package sandbox.graaljs;

import static sandbox.graaljs.PegJs.IOUtils.readFully;

public class ExprPegJsDemo {
    public static void main(String[] args) {
      try (var pegJs = PegJs.init()) {
        var parser = pegJs.generate(readFully(PegJs.class.getResourceAsStream("/expr.pegjs")), "expr.pegjs");

        {
          var parseResult = parser.parse("bad expr");

          System.out.printf("----------------------%n" +
                            "err message: %s%n" +
                            "err: %s%n" +
                            "err formatted: %s%n" +
                            "----------------------%n",
                            parseResult.getSyntaxError().message(),
                            parseResult.getSyntaxError().stringify(),
                            parseResult.getSyntaxError().formatted());
        }
        {
          var parseResult = parser.parse(
                  "(@duration > 500 &&\n" +
                  "            (!(isEmpty(myField)) && localVar1.field1.field2 != 15)) ||\n" +
                  "            (isEmpty(collectionField) || any(collectionField, {\n" +
                  "              isEmpty(@it.name)\n" +
                  "        }))"
          );

          assert parseResult.isSuccess() : "Should have been a success";
          System.out.printf("----------------------%n" +
                            "result: %s%n" +
                            "----------------------%n", parseResult.stringify());
        }
      }
    }
}
