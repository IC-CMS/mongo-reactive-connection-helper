package cms.sre.mongo_reactive_connection_helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestConfig.class})
public class ImplementingAbstractReactiveMongoConfigurationTest {

    /**
     * This tests to make sure that MongoReactiveClientFactory enables Spring Reactive Data Mongo Configuration.
     */
    @Test
    public void contextLoadsTest(){
        ;
    }
}
