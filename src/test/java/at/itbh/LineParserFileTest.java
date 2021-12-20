package at.itbh;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class LineParserFileTest {

    @Inject
    Vertx vertx;

    @Test
    public void testFile() throws FileNotFoundException {
        var parser = new LineParser(vertx, Charset.forName("utf8"), 1);
        var fileParser = parser.parse(new File("src/test/resources/LineParser_test_file.txt"));
        fileParser.subscribe().withSubscriber(AssertSubscriber.create(3)).awaitCompletion()
                .assertCompleted()
                .assertItems(new String[] {"First line", "Second line", "Third line"});
    }

    @Test
    public void testFileBig() throws FileNotFoundException, InterruptedException {
        var parser = new LineParser(vertx, Charset.forName("utf8"));
        var fileParser = parser.parse(new File("src/test/resources/bigfile.txt"));
        AtomicInteger counter = new AtomicInteger(0);
        Instant startTimestamp = Instant.now();
        fileParser.subscribe()
        .asIterable().forEach(line -> counter.incrementAndGet());
        Assertions.assertEquals(true, Duration.between(startTimestamp, Instant.now()).get(ChronoUnit.SECONDS) < 20);
        Assertions.assertEquals(1000000, counter.get());
    }

}
