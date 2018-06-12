package cms.sre.mongo_reactive_connection_helper.embedded;

import org.junit.Test;

import java.io.IOException;

public class InsecureMongoTest {

    @Test
    public void fullBoat() throws IOException {
        InsecureMongo insecureMongo = new InsecureMongo()
                .setUsername("username")
                .setPassword("password");

        insecureMongo.start();
        insecureMongo.stop();
    }
}
