package io.redis.exercise;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import redis.clients.jedis.Jedis;

@Command(name = "integerDataTest", mixinStandardHelpOptions = true, 
         description = "Loads / Reads the integer data on the Redis database")
public class Application implements Callable<Integer> {
    
    public static enum Action { loadIntegers, readIntegers}

    @Option(names = {"--instanceHost"}, description = "Public IP address to access the Redis database", required = true)
    private String instanceHost;
    
    @Option(names = {"--instancePort"}, description = "Public Port to access the Redis database", required = true)
    private int instancePort;

    @Option(names = {"--instancePassword"}, description = "Password for the Redis database", required = true)
    private String instancePassword;

    @Option(names = {"--key"}, description = "The key for the set to load the integers into", required = false,
     defaultValue="the100")
    private String key;

    @Option(names = {"--action"}, description = "One of: ${COMPLETION-CANDIDATES}", required = true)
    private Action action;


    @Override
    public Integer call() throws Exception {
        System.out.println("Connecting to instanceHost=" + instanceHost + " and instancePort=" + instancePort);

        try(Jedis instance = new Jedis(instanceHost,instancePort)) {
            instance.auth(instancePassword);

            switch(action) {
                case loadIntegers:
                    loadIntegers(instance);
                    break;
                case readIntegers:
                    readIntegers(instance);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }

        }

    
        return 0;
    }


    /**
     * Create the score members of the set.
     * 
     * @param min the minimum number 
     * @param max the max number
     * @return the score members of the set.
     */
    private static Map<String,Double> createScoreMembers(int min, int max) {
        Map<String,Double> scoreMembers = new HashMap<>();
        for (int i=min;i<=max;i++) {
            scoreMembers.put(Integer.toString(i),Double.valueOf(i));
        }
        return scoreMembers;
    }


    /**
     * Load the integers into the specified jedis instance.
     * 
     * @param instance
     */
    private void loadIntegers(Jedis instance) {
        System.out.println("Loading the integers into the key: " + key);
        instance.zadd(key,createScoreMembers(1,100));
    }

     /**
     * Read the integers in reverse order from the specified jedis instance.
     * 
     * @param instance
     */
    private void readIntegers(Jedis instance) {
        System.out.println("Reading the integers in reverse order from the key: " + key);
        Set<String> reverseValues = instance.zrevrange(key,0,-1);
        System.out.println(reverseValues);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

}
