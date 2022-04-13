/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sandbox.panama;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class MemoryMappedFiles {

  void useMappedFileBuffer(Path src) {
    try (var fileChannel = FileChannel.open(src, StandardOpenOption.READ)) {
      // A mapped byte buffer and the file mapping that it represents remain
      // valid until the buffer itself is garbage-collected.
      var mappedByteBuffer = fileChannel.map(
              FileChannel.MapMode.READ_ONLY,
              0,
              fileChannel.size());

      // do something with the mapped buffer
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void useMemorySegment(Path src) {
    try (var scope = ResourceScope.newConfinedScope()) {
      var fileSize = Files.size(src);
      var mappedFile = MemorySegment.mapFile(
              src,
              0,
              fileSize,
              FileChannel.MapMode.READ_ONLY,
              scope);

      // do something with the mapped buffer
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
