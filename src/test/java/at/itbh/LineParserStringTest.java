package at.itbh;

import java.nio.charset.Charset;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class LineParserStringTest {

    @Inject
    Vertx vertx;

    @Test
    public void testEmptyString() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var input = "";
        var assertionSubscriber =
                parser.parse(input).subscribe().withSubscriber(AssertSubscriber.create());
                assertionSubscriber.assertItems(new String[]{});
                assertionSubscriber.assertCompleted();
    }

    @Test
    public void testOneLineString() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var input = "This is only one line";
        var assertionSubscriber =
                parser.parse(input).subscribe().withSubscriber(AssertSubscriber.create(1));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(input);
    }

    @Test
    public void testTwoLineString() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"This is one line", "This is another line"};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(inputs);
    }

    @Test
    public void testTwoLineStringWithEmptyTrailingLine() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"This is one line", "This is another line", ""};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[0], inputs[1]});
    }

    @Test
    public void testTwoLineStringWithEmptyTrailingLines() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"This is one line", "This is another line", "", "", "", ""};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(4));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[0], inputs[1]});
    }

    @Test
    public void testTwoLineStringWithEmptyLeadingLine() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"", "This is one line", "This is another line"};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[1], inputs[2]});
    }

    @Test
    public void testTwoLineStringWithEmptyLeadingLines() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"", "", "This is one line", "This is another line"};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[2], inputs[3]});
    }

    @Test
    public void testTwoLineStringWithEmptyLeadingAndTrailingLine() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"", "This is one line", "This is another line", ""};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[1], inputs[2]});
    }

    @Test
    public void testTwoLineStringWithEmptyLeadingAndTrailingLines() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"", "", "", "This is one line", "This is another line", "", "", "", "", ""};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[3], inputs[4]});
    }

    @Test
    public void testTwoLineStringWithEmptyLineInBetween() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"This is one line", "", "This is another line"};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[0], inputs[2]});
    }

    @Test
    public void testTwoLineStringWithEmptyLinesInBetween() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"This is one line", "", "", "", "This is another line"};
        var assertionSubscriber =
                parser.parse(String.join("\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(2));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[0], inputs[4]});
    }

    @Test
    public void testMultiLineStringWithEmptyLinesCRLF() {
        var parser = new LineParser(vertx, Charset.defaultCharset());
        var inputs = new String[]{"", "This is one line", "", "", "", "This is another line", "", "This is a third line", "", ""};
        var assertionSubscriber =
                parser.parse(String.join("\r\n", inputs)).subscribe().withSubscriber(AssertSubscriber.create(3));
                assertionSubscriber.assertCompleted();
                assertionSubscriber.assertItems(new String[]{inputs[1], inputs[5], inputs[7]});
    }
    
}
