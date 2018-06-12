package cms.sre.mongo_reactive_connection_helper.embedded;

import java.io.IOException;

public interface EmbeddedMongo {
    int getPort();
    boolean isSecure();
    void start() throws IOException;
    void stop();
}
