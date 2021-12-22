package at.itbh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
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

    public static enum Mode {
        /**
         * Uses {@link Files#lines(java.nio.file.Path)} for reading lines of the file
         */
        NIO,

        /**
         * Uses vert.x {@link io.vertx.mutiny.core.file.AsyncFile} for reading the file and custom
         * splitting logic for line separation
         */
        ASYNC_FILE
    }

    private final Vertx vertx;
    private final Charset encoding;
    private final Optional<Integer> readBufferSize;

    private Mode mode = Mode.ASYNC_FILE;

    public LineParser(Vertx vertx, Charset encoding) {
        this.vertx = vertx;
        this.encoding = encoding;
        this.readBufferSize = Optional.empty();
    }

    public LineParser(Vertx vertx, Charset encoding, int readBufferSize) {
        this.vertx = vertx;
        this.encoding = encoding;
        this.readBufferSize = Optional.of(readBufferSize);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Multi<String> parse(String data) {
        Buffer buffer = Buffer.buffer().appendString(data, encoding.name());
        return parse(buffer);
    }

    public Multi<String> parse(Buffer data) {
        return parse(Multi.createFrom().item(data));
    }

    public Multi<String> parse(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath());
        }
        if (getMode() == Mode.ASYNC_FILE) {
            return parseFileWithVertxAsyncFile(file);
        } else {
            return parseFileWithNio(file);
        }
    }

    private Multi<String> parseFileWithVertxAsyncFile(File file) {
        var path = file.getPath();
        var fileHandle =
                vertx.fileSystem().open(path, new OpenOptions().setRead(true).setCreate(false));
        var fileContents = fileHandle.onItem().transformToMulti(asyncFile -> {
            if (readBufferSize.isPresent()) {
                asyncFile.setReadBufferSize(readBufferSize.get());
            }
            return asyncFile.toMulti();
        });
        return parse(fileContents);
    }

    private Multi<String> parseFileWithNio(File file) throws IOException {
        var lines = Files.lines(Paths.get(file.getPath()), encoding);
        return Multi.createFrom().emitter(emitter -> {
            Multi.createFrom().items(lines).onCompletion().invoke(emitter::complete).subscribe()
                    .with(line -> {
                        if (line.length() > 0) {
                            emitter.emit(line);
                        }
                    });
        });
    }

    /**
     * Emits the line as {@link String} if not empty
     * 
     * @param emitter the {@link MultiEmitter} to use
     * @param line the line's contents to be emitted as {@link String}
     */
    void emitLine(MultiEmitter<? super String> emitter, StringBuilder line) {
        if (line.length() > 0) {
            emitter.emit(line.toString());
            line.setLength(0);
        }
    }

    public Multi<String> parse(Multi<Buffer> buffers) {
        return parseWithString(buffers);
    }

    /**
     * Parses the data in the buffers to lines
     * 
     * @param buffers a sequence of bytes which can can be interpreted as a text by the specified
     *        character encoding
     * @return the lines
     */
    Multi<String> parseWithString(Multi<Buffer> buffers) {
        final StringBuilder tempLine = new StringBuilder();

        // emit line by line
        return Multi.createFrom().emitter(emitter -> {
            buffers
                    // output the last line
                    .onCompletion().invoke(() -> {
                        emitLine(emitter, tempLine);
                        emitter.complete();
                    })
                    // read all buffers and build lines
                    .subscribe().with(buffer -> {
                        String content = buffer.toString(encoding);
                        long newLineCount = content.length() - content.replace("\n", "").length();
                        final long[] newLineCounter = {0};
                        Multi.createFrom().items(content.lines()).subscribe().with(line -> {
                            tempLine.append(line);
                            if (++newLineCounter[0] <= newLineCount) {
                                emitLine(emitter, tempLine);
                            }
                        });
                    });
        });
    }

}
