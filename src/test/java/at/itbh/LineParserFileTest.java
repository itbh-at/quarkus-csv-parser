package at.itbh;

import java.io.File;
import java.nio.charset.Charset;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class LineParserFileTest {

    @Inject
    Vertx vertx;

    @Test
    public void testFile() {
        var parser = new LineParser(vertx, Charset.forName("utf8"));
        var fileParser = parser.parse(new File("src/test/resources/LineParser_test_file.txt"));
        fileParser.log().onCompletion().invoke(() -> System.out.println("completed")).onItem()
                .invoke(line -> System.out.println(line)).subscribe()
                .withSubscriber(AssertSubscriber.create(3)).assertCompleted();
    }

}
