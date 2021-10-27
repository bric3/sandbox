/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

public class CheckForPreIncrement {
  public static void main(String[] args) throws Exception {
    // a single argument that is directory from which .java sources are scanned.
    // If no argument supplied, use the current directory
    Path path = Paths.get(args.length == 0 ? "." : args[0]);

    // walk the file system from the given path recursively
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        // if exception (say non-readable dir), just print and continue scanning.
        if (exc != null) {
          System.err.printf("dir visit failed for %s : %s\n", dir, exc);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        // if a file cannot be read, just print error and continue scanning
        if (exc != null) {
          System.err.printf("file visit failed for %s : %s\n", file, exc);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        // is this a .java file?
        if (file.getFileName().toString().endsWith(".java")) {
          try {
            // check for ++i and --i pattern and report
            check(file.toAbsolutePath());
          } catch (IOException exc) {
            // report parse failures and continue scanning other files
            System.err.printf("parsing failed for %s : %s\n", file, exc);
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  // major version of JDK such as 16, 17, 18 etc.
  private static int getJavaMajorVersion() {
    return Runtime.version().feature();
  }

  // javac options we pass to the compiler. We enable preview so that
  // all preview features can be parsed.
  private static final List<String> OPTIONS =
      List.of("--enable-preview", "--release=" + getJavaMajorVersion());

  // get the system java compiler instance
  private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

  private static void check(Path javaSrc) throws IOException {
    // create a compilation task (JavacTask) for the given java source file
    var compUnits = compiler.
        getStandardFileManager(null, null, null).
        getJavaFileObjects(javaSrc);
    // we need to cast to JavacTask so that we can call parse method
    var task = (JavacTask) compiler.getTask(null, null, null,
                                            OPTIONS, null, compUnits);
    // we need this to report line and column numbers of coding patterns we find
    var sourcePositions = Trees.instance(task).getSourcePositions();

    // TreeVisitor implementation using TreeScanner
    var scanner = new TreeScanner<Void, Void>() {
      private CompilationUnitTree compUnit;
      private LineMap lineMap;
      private String fileName;

      // store details of the current compilation unit in instance vars
      @Override
      public Void visitCompilationUnit(CompilationUnitTree t, Void v) {
        compUnit = t;
        lineMap = t.getLineMap();
        fileName = t.getSourceFile().getName();
        return super.visitCompilationUnit(t, v);
      }

      // found a for loop to analyze
      @Override
      public Void visitForLoop(ForLoopTree t, Void v) {
        // check each update expression
        for (var est : t.getUpdate()) {
          // is this a UnaryTree expression statement?
          if (est.getExpression() instanceof UnaryTree unary) {
            // is this prefix decrement or increment?
            var kind = unary.getKind();
            if (kind == Tree.Kind.PREFIX_DECREMENT ||
                kind == Tree.Kind.PREFIX_INCREMENT) {
              // report file name, line number and column number
              var pos = sourcePositions.getStartPosition(compUnit, unary);
              var line = lineMap.getLineNumber(pos);
              var col = lineMap.getColumnNumber(pos);
              System.out.printf("Found ++i or --i in %s %d:%d\n",
                                fileName, line, col);
            }
          }

        }
        return super.visitForLoop(t, v);
      }
    };

    // visit each compilation unit tree object with our scanner
    for (var compUnitTree : task.parse()) {
      compUnitTree.accept(scanner, null);
    }
  }
}

