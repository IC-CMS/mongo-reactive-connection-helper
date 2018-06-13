package cms.sre.mongo_reactive_connection_helper;

import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MongoReactiveClientFactory {
    private static final ServerAddress DEFAULT_LOCALHOST_ADDRESS_AND_PORT = new ServerAddress("localhost", 27017);
    private static boolean isNotEmptyOrNull(String str){
        return str != null && str.length() > 0;
    }

    public static MongoClient getLocalhostMongoClient(){
        return MongoClients.create();
    }

    public static MongoClient getLocahostMongoClient(String databaseName, String username, String password){
        ClusterSettings clusterSettings = ClusterSettings.builder()
                .build();

        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .clusterSettings(clusterSettings)
                .credential(MongoCredential.createCredential(username, databaseName, password.toCharArray()))
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    private static List<ServerAddress> serverAddresses(MongoClientParameters params){
        List<ServerAddress> ret = new LinkedList<ServerAddress>();
        if(params.getReplicaSetLocations().length > 0){
            Arrays.asList(params.getReplicaSetLocations())
                    .forEach(arg -> ret.add(new ServerAddress(arg)));
        }
        return ret.size() > 0 ? ret : null;
    }

    private static MongoCredential mongoCredential(MongoClientParameters mongoClientParameters){
        MongoCredential ret = null;
        if(mongoClientParameters != null && isNotEmptyOrNull(mongoClientParameters.getDatabaseName()) && isNotEmptyOrNull(mongoClientParameters.getMongoUsername()) && isNotEmptyOrNull(mongoClientParameters.getMongoPassword())){
            ret = MongoCredential.createCredential(mongoClientParameters.getMongoUsername(), mongoClientParameters.getDatabaseName(), mongoClientParameters.getMongoPassword().toCharArray());
        }
        return ret;
    }

    private static SSLContext sslContext(MongoClientParameters mongoClientParameters){
        File trustStore = null;
        if(isNotEmptyOrNull(mongoClientParameters.getTrustStoreLocation()) && isNotEmptyOrNull(mongoClientParameters.getTrustStorePassword())){
            trustStore = Paths.get(mongoClientParameters.getTrustStoreLocation())
                    .toFile();
        }

        File keyStore = null;
        if(isNotEmptyOrNull(mongoClientParameters.getKeyStoreLocation()) && isNotEmptyOrNull(mongoClientParameters.getKeyStorePassword())&& isNotEmptyOrNull(mongoClientParameters.getKeyStoreKeyPassword())){
            keyStore = Paths.get(mongoClientParameters.getKeyStoreLocation())
                    .toFile();
        }

        SSLContext sslContext = null;
        try{
            if(keyStore != null || trustStore != null){
                SSLContextBuilder custom = SSLContexts.custom()
                        .setProtocol("TLSv1.2");

                if(keyStore != null){
                    custom = custom.loadKeyMaterial(keyStore,
                            mongoClientParameters.getKeyStorePassword().toCharArray(),
                            mongoClientParameters.getKeyStoreKeyPassword().toCharArray(),
                            (aliases, socket) -> {
                                return aliases.keySet()
                                        .iterator()
                                        .next();
                            });
                }

                if(trustStore != null){
                    custom = custom.loadTrustMaterial(trustStore, mongoClientParameters.getTrustStorePassword().toCharArray());
                }

                sslContext = custom.build();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return sslContext;
    }

    public static MongoClient getMongoClient(MongoClientParameters mongoClientParameters){


        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

        List<ServerAddress> serverAddresses = serverAddresses(mongoClientParameters);
        if(serverAddresses.size() > 0){
            settingsBuilder.clusterSettings(ClusterSettings.builder()
                    .hosts(serverAddresses(mongoClientParameters))
                    .build());
        }



        SSLContext sslContext = sslContext(mongoClientParameters);
        if(sslContext != null){
            settingsBuilder.sslSettings(SslSettings.builder()
                .context(sslContext)
                .enabled(true)
                .invalidHostNameAllowed(true)
                .build());

            NettyStreamFactoryFactory nettyStreamFactoryFactory = NettyStreamFactoryFactory.builder()
                    .eventLoopGroup(new NioEventLoopGroup())
                    .build();

            settingsBuilder.streamFactoryFactory(nettyStreamFactoryFactory);
        }

        MongoCredential mongoCredential = mongoCredential(mongoClientParameters);
        if(mongoCredential != null){
            settingsBuilder.credential(mongoCredential);
        }
        return MongoClients.create(settingsBuilder.build());
    }
}
