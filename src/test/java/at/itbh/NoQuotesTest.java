package at.itbh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class NoQuotesTest {

    @Inject
    Vertx vertx;

    @Test
    void testString() {
        var parser = new CsvParser(vertx, Charset.defaultCharset());
        String data =
                "field1,field2,field3\ntest1.1,test1.2,test1.3\ntest2.1,test2.2,test2.3\r\ntest3.1 , test3.2 , test3.3";
        var header = new CsvParser.Header();
        header.setContainsHeader(true);
        parser.parseToList(data, header).subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion()
                .assertLastItem(Arrays.asList(new String[] {"test3.1 ", " test3.2 ", " test3.3"}));
    }

    @Test
    void testLargeFile() throws IOException {
        var parser = new CsvParser(vertx, Charset.defaultCharset());
        var header = new CsvParser.Header();
        header.setContainsHeader(true);
        parser.parseToList(new File("src/test/resources/sales_records.csv"), header).subscribe()
                .withSubscriber(AssertSubscriber.create(50001))
                .awaitCompletion(Duration.ofSeconds(20))
                .assertLastItem(Arrays.asList(new String[] {"Sub-Saharan Africa", "Eritrea",
                        "Vegetables", "Offline", "L", "6/5/2014", "943440902", "6/30/2014", "3983",
                        "154.06", "90.93", "613620.98", "362174.19", "251446.79"}));
    }
}
