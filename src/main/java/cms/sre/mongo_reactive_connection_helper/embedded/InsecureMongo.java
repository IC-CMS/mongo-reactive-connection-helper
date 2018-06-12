package cms.sre.mongo_reactive_connection_helper.embedded;

import de.flapdoodle.embed.mongo.*;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.*;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;

public class InsecureMongo implements EmbeddedMongo{
    public static final String REPLSET_OK_TOKEN_3 = "transition to primary complete";
    public static final int INIT_TIMEOUT_MS = 30000;
    public static final String USER_ADDED_TOKEN = "Successfully added user";
    public IStreamProcessor iStreamProcessor = Processors.logTo(LoggerFactory.getLogger(InsecureMongo.class), Slf4jLevel.INFO);

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;

    private int port;

    private String username;
    private String password;

    public InsecureMongo(){
        this.port = ThreadLocalRandom
                .current()
                .nextInt(27017, 27116);
    }

    public MongodExecutable getMongodExecutable() {
        return mongodExecutable;
    }

    public InsecureMongo setMongodExecutable(MongodExecutable mongodExecutable) {
        this.mongodExecutable = mongodExecutable;
        return this;
    }

    @Override
    public int getPort() {
        return port;
    }

    public InsecureMongo setPort(int port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public InsecureMongo setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public InsecureMongo setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    private File writeTmpScriptFile(String scriptText) throws IOException {
        File scriptFile = File.createTempFile("tempfile", ".js");
        scriptFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter(new FileWriter(scriptFile));
        bw.write(scriptText);
        bw.close();
        return scriptFile;
    }

    private void runScriptAndWait(String scriptText, String token, String[] failures, String dbName, String username, String password) throws IOException {
        IStreamProcessor mongoOutput;
        if (!StringUtils.isEmpty(token)) {
            mongoOutput = new LogWatchStreamProcessor(
                    format(token),
                    (failures != null) ? new HashSet<>(Arrays.asList(failures)) : Collections.<String>emptySet(),
                    iStreamProcessor);
        } else {
            mongoOutput = iStreamProcessor;
        }
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.Mongo)
                .processOutput(new ProcessOutput(mongoOutput, iStreamProcessor,iStreamProcessor))
                .build();
        MongoShellStarter starter = MongoShellStarter.getInstance(runtimeConfig);
        final File scriptFile = writeTmpScriptFile(scriptText);
        final MongoShellConfigBuilder builder = new MongoShellConfigBuilder();
        if (!StringUtils.isEmpty(dbName)) {
            builder.dbName(dbName);
        }
//        if (!StringUtils.isEmpty(this.username)) {
//            builder.username(username);
//        }
//        if (!StringUtils.isEmpty(this.password)) {
//            builder.password(password);
//        }
        starter.prepare(builder
                .scriptName(scriptFile.getAbsolutePath())
                .version(Version.Main.DEVELOPMENT)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build()).start();
        if (mongoOutput instanceof LogWatchStreamProcessor) {
            ((LogWatchStreamProcessor) mongoOutput).waitForResult(INIT_TIMEOUT_MS);
        }
    }

    private boolean startupServer() throws IOException {
        final MongoCmdOptionsBuilder cmdOptionsBuilder = new MongoCmdOptionsBuilder();
        cmdOptionsBuilder.enableAuth(this.username != null && this.password != null);


        MongodStarter mongodStarter = MongodStarter.getDefaultInstance();

        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.DEVELOPMENT)
                .cmdOptions(cmdOptionsBuilder.build())
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();



        this.mongodExecutable = mongodStarter.prepare(mongodConfig);

        this.mongodProcess = this.mongodExecutable.start();
        return true;
    }

    private boolean addAdmin() throws IOException {
        String addUser = "db.createUser(";
        addUser       += "{";
        addUser       += "\"user\": \""+this.username+"\",";
        addUser       += "\"pwd\" : \""+this.password+"\",";
        addUser       += "\"roles\":[";
        addUser       += "\"root\",";
        addUser       += "{\"role\":\"userAdmin\",\"db\":\"admin\"},";
        addUser       += "{\"role\":\"dbAdmin\",\"db\":\"admin\"},";
        addUser       += "{\"role\":\"userAdminAnyDatabase\",\"db\":\"admin\"},";
        addUser       += "{\"role\":\"dbAdminAnyDatabase\",\"db\":\"admin\"},";
        addUser       += "{\"role\":\"clusterAdmin\",\"db\":\"admin\"},";
        addUser       += "{\"role\":\"dbOwner\",\"db\":\"admin\"},";
        addUser       += "]";
        addUser       += "}";
        addUser       += ");";

        this.runScriptAndWait(addUser, USER_ADDED_TOKEN, new String[]{"couldn't add user", "failed to load", "login failed"}, "admin", null, null);
        return true;
    }

    @Override
    public void start() throws IOException{
        this.startupServer();
        this.addAdmin();
    }

    @Override
    public void stop() {
        if(this.mongodExecutable != null){
            this.mongodProcess.stop();
        }
    }

    @Override
    public void finalize(){
        this.stop();
    }
}
