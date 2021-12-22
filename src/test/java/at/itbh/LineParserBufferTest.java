package at.itbh;

import java.nio.charset.Charset;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;

@QuarkusTest
public class LineParserBufferTest {

        @Inject
        Vertx vertx;

        @Test
        public void testMultiLineStringWithEmptyLinesCRLF() {
                var parser = new LineParser(vertx, Charset.defaultCharset());
                var inputs = new String[] {"", "This is one line", "", "", "",
                                "This is another line", "", "This is a third line", "", ""};
                var buffer = Buffer.buffer(String.join("\r\n", inputs),
                                Charset.defaultCharset().name());
                var assertSubscriber = parser.parse(buffer).subscribe()
                                .withSubscriber(AssertSubscriber.create(3));
                assertSubscriber.awaitCompletion().assertCompleted();
                assertSubscriber.assertItems(new String[] {inputs[1], inputs[5], inputs[7]});
        }

        @Test
        public void testMultiLineStringInBuffers() {
                var parser = new LineParser(vertx, Charset.defaultCharset());
                var buffers = new Buffer[] {Buffer.buffer("line one\n"),
                                Buffer.buffer("line two\r\n"), Buffer.buffer(""),
                                Buffer.buffer("line three\n"), Buffer.buffer("line four\n")};
                var assertSubscriber = parser.parse(Multi.createFrom().items(buffers))
                                .subscribe().withSubscriber(AssertSubscriber.create(4));
                assertSubscriber.awaitCompletion().assertCompleted().assertItems("line one",
                                "line two", "line three", "line four");
        }

}
