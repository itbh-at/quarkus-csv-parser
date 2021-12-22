package at.itbh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;

/**
 * A parser for delimited data; e.g. CSV
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4180
 */
public class CsvParser {

    public static enum Mode {
        /**
         * Fields aren't quoted
         */
        NOT_QUOTED,

        /**
         * Fields are quoted using double quotes
         */
        QUOTED,

        /**
         * Fields are optionally quoted using double quotes
         */
        OPTIONALLY_QUOTED;
    }

    public static class Header {
        boolean containsHeader = false;
        String[] header;

        public boolean getContainsHeader() {
            return containsHeader;
        }

        public void setContainsHeader(boolean containsHeader) {
            this.containsHeader = containsHeader;
        }

        public String[] getHeader() {
            return header;
        }

        public void setHeader(String[] header) {
            this.header = header;
        }
    }

    char delimiter = ',';
    LineParser lineParser;
    Mode mode = Mode.NOT_QUOTED;
    private Pattern splitPattern;

    CsvParser(Vertx vertx, Charset encoding) {
        this(vertx, encoding, ',');
    }

    CsvParser(Vertx vertx, Charset encoding, char delimiter) {
        this(vertx, encoding, delimiter, Mode.NOT_QUOTED);
    }

    CsvParser(Vertx vertx, Charset encoding, char delimiter, Mode mode) {
        this.lineParser = new LineParser(vertx, encoding);
        this.mode = mode;
        if (mode == Mode.NOT_QUOTED) {
            this.splitPattern = Pattern.compile(Character.toString(delimiter));
        } else {
            throw new UnsupportedOperationException("Only NOT_QUOTED is supported at the moment");
        }
    }

    List<String> splitLine(String line) {
        // try (Scanner scanner = new Scanner(line)) {
        //     scanner.useDelimiter(splitPattern);
        //     return scanner.tokens().collect(Collectors.toList());
        // }
        return Arrays.asList(splitPattern.split(line));
    }

    Multi<List<String>> parseToList(String data, Header header) {
        Multi<String> reader = lineParser.parse(data);
        if (header.getContainsHeader()) {
            lineParser.parse(data).onItem().transform(this::splitLine).select().first().subscribe()
                    .with(headerLine -> {
                        header.setHeader(headerLine.toArray(new String[] {}));
                    });
            // skip first line
            reader.skip().first();
        }
        return reader.onItem().transform(this::splitLine);
    }

    Multi<List<String>> parseToList(File data, Header header) throws IOException {
        Multi<String> reader = lineParser.parse(data);
        if (header.getContainsHeader()) {
            lineParser.parse(data).onItem().transform(this::splitLine).select().first().subscribe()
                    .with(headerLine -> {
                        header.setHeader(headerLine.toArray(new String[] {}));
                    });
            // skip first line
            reader.skip().first();
        }
        return reader.onItem().transform(this::splitLine);
    }

}
