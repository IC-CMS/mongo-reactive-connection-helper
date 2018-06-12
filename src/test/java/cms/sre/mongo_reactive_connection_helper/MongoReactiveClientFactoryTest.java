package cms.sre.mongo_reactive_connection_helper;

import cms.sre.httpclient_connection_helper.embedded.PathUtils;
import cms.sre.mongo_reactive_connection_helper.embedded.InsecureMongo;
import org.junit.Test;

import java.io.IOException;

public class MongoReactiveClientFactoryTest {



    @Test
    public void testSslConnection() throws IOException {


        String keyStoreLocation = PathUtils.getAbsolutePathForClasspathResource("client_keystore.jks");
        String trustStoreLocation = PathUtils.getAbsolutePathForClasspathResource("cacerts.jks");

        MongoClientParameters mongoClientParameters = new MongoClientParameters()
                .setTrustStoreLocation(trustStoreLocation)
                .setTrustStorePassword("changeit")
                .setKeyStoreKeyPassword("changeit")
                .setKeyStorePassword("changeit")
                .setKeyStoreLocation(keyStoreLocation)
                .setMongoPassword("password")
                .setMongoUsername("username")
                .setReplicaSetLocations(new String[]{"localhost"});

        InsecureMongo insecureMongo = new InsecureMongo()
                .setPassword(mongoClientParameters.getMongoPassword())
                .setUsername(mongoClientParameters.getMongoUsername())
                .setPort(27017);

        insecureMongo.start();

        MongoReactiveClientFactory.getMongoClient(mongoClientParameters)
            .listDatabaseNames();
    }


}
