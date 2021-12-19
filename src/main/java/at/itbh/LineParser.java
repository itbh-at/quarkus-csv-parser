package at.itbh;

import java.io.File;
import java.nio.charset.Charset;
import java.util.stream.Stream;
import io.smallrye.mutiny.Multi;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;

/**
 * A reactive (non-blocking) parser for splitting textual input data line by line to a {@link Multi}
 * of {@link String} using {@link Vertx}
 * 
 * <p>
 * Every item in the {@link Multi} represents a line. A line terminator is one of the following: a
 * line feed character "\n" (U+000A), a carriage return character "\r" (U+000D), or a carriage
 * return followed immediately by a line feed "\r\n" (U+000D U+000A). The line terminator isn't part
 * of the returned line.
 * <p>
 */
public class LineParser {

    private final Vertx vertx;
    private final Charset encoding;

    public LineParser(Vertx vertx, Charset encoding) {
        this.vertx = vertx;
        this.encoding = encoding;
    }

    public Multi<String> parse(String data) {
        Buffer buffer = Buffer.buffer().appendString(data, encoding.name());
        return parse(buffer);
    }

    public Multi<String> parse(Buffer data) {
        return parse(Multi.createFrom().item(data));
    }

    public Multi<String> parse(File file) {
        var path = file.getPath();
        var fileHandle = vertx.fileSystem().open(path, new OpenOptions().setRead(true));
        var fileContents = fileHandle.onItem().transformToMulti(asyncFile -> asyncFile.toMulti());
        return parse(fileContents);
    }

    /**
     * Parses the data in the buffers to lines
     * 
     * @param buffers a sequence of bytes which can can be interpreted as a text by the specified
     *        character encoding
     * @return the lines
     */
    public Multi<String> parse(Multi<Buffer> buffers) {
        final StringBuilder tempLine = new StringBuilder();

        // emit line by line
        return Multi.createFrom().emitter(emitter -> {
            buffers
                    // output the last line
                    .onCompletion().invoke(() -> {
                        if (tempLine.length() > 0) {
                            emitter.emit(tempLine.toString());
                        }
                        emitter.complete();
                    })
                    // read all buffers and build lines
                    .subscribe().with(buffer -> {
                        String content = buffer.toString(encoding);
                        if (content.contains("\n")) {
                            // When content contains at least one line break, it is split at the
                            // line breaks. The first line is appended to the current tempLine. All
                            // other lines are treated as a full line except the last one. Empty
                            // lines are discarded.
                            Stream<String> linesInContent = content.lines();
                            Multi.createFrom().items(linesInContent).subscribe().with(line -> {
                                // handle line only if not empty
                                if (line.length() > 0) {
                                    tempLine.append(line);
                                    // output the new full line
                                    emitter.emit(tempLine.toString());
                                }
                                // prepare for new line
                                tempLine.setLength(0);
                            });
                        } else {
                            // When no line break is found the existing temporary line is continued.
                            tempLine.append(content);
                        }
                    });
        });
    }

}
