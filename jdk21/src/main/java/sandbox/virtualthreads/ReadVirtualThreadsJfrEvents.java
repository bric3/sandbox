package sandbox.virtualthreads;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReadVirtualThreadsJfrEvents {
  private static final String default_fg = "\u001B[39m";
  private static final String red = "\u001B[31m";
  private static final String green = "\u001B[32m";
  private static final String blue = "\u001B[34m";
  private static final String gray = "\u001B[37m";
  private static final String bold = "\u001B[01m";
  private static final String italic = "\u001B[03m";
  private static final String underline = "\u001B[04m";
  private static final String framed = "\u001B[51m";
  private static final String reset = "\u001B[00m";

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: ReadVirtualThreadsJfrEvents <jfr-files>");
      System.exit(1);
    }
    var jfrFiles = Arrays.stream(args).map(File::new).toList();
    for (var arg : jfrFiles) {
      if (!arg.isFile()) {
        System.err.println("File " + arg + " does not exist");
        System.exit(1);
      }

    }

    var events = loadEvents(jfrFiles);


    var freq = events.parallelStream()
                     .collect(Collectors.groupingBy(
                             IItemIterable::getType,
                             Collectors.summarizingLong(itemIterable -> itemIterable.parallelStream().count())
                     ));


    var typeIndex = freq.keySet()
                        .stream()
                        .collect(Collectors.toMap(
                                iItemIType -> iItemIType.getIdentifier(),
                                Function.identity()
                        ));

    // typeIndex.forEach((key, value) -> System.out.println(key + ": " + value.getName()));


    var virtualThreadEvents = Set.of("jdk.VirtualThreadStart", "jdk.VirtualThreadEnd", "jdk.VirtualThreadSubmitFailed", "jdk.VirtualThreadPinned");
    var continuationEvents = Set.of("jdk.ContinuationThaw", "jdk.ContinuationThawYoung", "jdk.ContinuationThawOld", "jdk.ContinuationFreeze", "jdk.ContinuationFreezeYoung", "jdk.ContinuationFreezeOld");

    // event metadata
    // Stream.concat(virtualThreadEvents.stream(), continuationEvents.stream())
    //       .forEach(eventName -> {
    //         var iItemIType = typeIndex.get(eventName);
    //         var eventStatistics = freq.get(iItemIType);
    //         printEvent(iItemIType, eventStatistics.getSum());
    //       });
    //
    //
    // var map = events.parallelStream()
    //                           .filter(itemIterable -> virtualThreadEvents.contains(itemIterable.getType().getIdentifier()))
    //                           .flatMap(itemIterable -> {
    //                             System.out.println("\n" + itemIterable.getType().getIdentifier() + " " + itemIterable.getItemCount());
    //                             return itemIterable.parallelStream();
    //                           })
    //                           .collect(Collectors.groupingBy(
    //                                   item -> {
    //                                     JfrAttributes.EVENT_THREAD
    //
    //                                     var javaThreadId = Attribute.attr("javaThreadId", UnitLookup.RAW_LONG);
    //                                     var type = item.getType();
    //
    //                                     IMemberAccessor<Long, ?> accessor = javaThreadId.getAccessor(type);
    //                                     // var accessor = type.getAccessor(javaThreadId.getKey());
    //
    //
    //                                     // ItemIterableToolkit.sorted(itemITerable, );
    //                                     Long member = accessor.getMember(item);
    //
    //                                     return ;
    //                                   }
    //                           ));


  }

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  private static void printEvent(IType<IItem> iItemIType, long sum) {
    var sb = new StringBuilder();

    sb.append(bold + red + iItemIType.getIdentifier() + ": " + blue + iItemIType.getName() + default_fg + " (" + sum + ")" + reset + "\n");
    iItemIType.getAccessorKeys().forEach((k, v) -> {
      sb.append("  " + green + k.getIdentifier() + reset + ": " + v.getName() + italic + " (" + k.getContentType() + ")" + reset + "\n");
    });

    System.out.println(sb);
  }

  private static IItemCollection loadEvents(List<File> jfrFiles) {
    try {
      return JfrLoaderToolkit.loadEvents(jfrFiles);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (CouldNotLoadRecordingException e) {
      throw new RuntimeException(e);
    }
  }
}
