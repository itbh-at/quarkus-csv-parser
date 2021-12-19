package at.itbh;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
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
        var inputs = new String[]{"", "This is one line", "", "", "", "This is another line", "", "This is a third line", "", ""};
        var buffer = Buffer.buffer(String.join("\r\n", inputs), Charset.defaultCharset().name());
        var assertSubscriber =
                parser.parse(buffer).subscribe().withSubscriber(AssertSubscriber.create(3));
                assertSubscriber.assertCompleted();
                assertSubscriber.assertItems(new String[]{inputs[1], inputs[5], inputs[7]});
    }

}
