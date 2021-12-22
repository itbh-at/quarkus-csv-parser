package at.itbh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class LineParserFileTest {

    @Inject
    Vertx vertx;

    @BeforeAll
    static void setup() throws IOException {
        File bigFile = new File("src/test/resources/bigfile.txt");
        if (bigFile.exists()) {
            System.out.println("Deleting big file");
            bigFile.delete();
        }

        // from https://www.baeldung.com/java-compress-and-uncompress
        File destDir = new File("src/test/resources/");
        byte[] buffer = new byte[4096];
        ZipInputStream zis = new ZipInputStream(
                new FileInputStream(new File("src/test/resources/bigfile.txt.zip")));
        ZipEntry zipEntry = zis.getNextEntry();
        System.out.println("Unzipping big file");
        while (zipEntry != null) {
            System.out.println("Extracting content of " + zipEntry.getName());
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        System.out.println("Test setup complete");
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Parses a tiny file in standard parsing mode
     * 
     * @throws IOException
     */
    @Test
    public void testFile() throws IOException {
        var parser = new LineParser(vertx, Charset.forName("utf8"), 1);
        var fileParser = parser.parse(new File("src/test/resources/LineParser_test_file.txt"));
        fileParser.subscribe().withSubscriber(AssertSubscriber.create(6)).awaitCompletion()
                .assertCompleted()
                .assertItems(new String[] {"First line", "Second line", "Third line"});
    }

    /**
     * Parses a 10 million lines file in both parsing modes
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testFileBig() throws InterruptedException, IOException {
        var fileParser =
                buildFileParser(new File("src/test/resources/bigfile.txt"), LineParser.Mode.NIO);
        runTestFileBig(fileParser, LineParser.Mode.NIO);
        fileParser = buildFileParser(new File("src/test/resources/bigfile.txt"),
                LineParser.Mode.ASYNC_FILE);
        runTestFileBig(fileParser, LineParser.Mode.ASYNC_FILE);
    }

    private void runTestFileBig(Multi<String> fileParser, LineParser.Mode mode) {
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        Instant startTimestamp = Instant.now();
        fileParser.subscribe().asIterable().forEach(line -> counter[0]++);
        System.out.println(mode + ": big file test runtime: "
                + (System.currentTimeMillis() - startTime) + "ms");
        Assertions.assertEquals(true,
                Duration.between(startTimestamp, Instant.now()).get(ChronoUnit.SECONDS) < 40);
        Assertions.assertEquals(10000000, counter[0]);
    }

    private Multi<String> buildFileParser(File file, LineParser.Mode mode) throws IOException {
        var parser = new LineParser(vertx, Charset.forName("utf8"));
        parser.setMode(mode);
        var fileParser = parser.parse(file);
        return fileParser;
    }

}
