package cms.sre.mongo_reactive_connection_helper;

import cms.sre.mongo_reactive_connection_helper.embedded.InsecureMongo;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;

import java.io.IOException;

@SpringBootApplication
public class TestConfig extends AbstractReactiveMongoConfiguration {
    private InsecureMongo insecureMongo;

    @Override
    public com.mongodb.reactivestreams.client.MongoClient reactiveMongoClient() {
        try {
            this.insecureMongo = new InsecureMongo();
            this.insecureMongo.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return MongoReactiveClientFactory.getLocalhostMongoClient();
    }

    @Override
    protected String getDatabaseName() {
        return "ThisDoesNotMatter";
    }

    @Bean
    public ExitCodeGenerator exitCodeGenerator() {
        return () -> {
            this.insecureMongo.stop();
            return 0;
        };
    }
}
