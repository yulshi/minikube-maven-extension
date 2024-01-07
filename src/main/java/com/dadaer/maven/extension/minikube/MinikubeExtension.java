package com.dadaer.maven.extension.minikube;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Running the command [<code>minikube docker-env</code>]
 * and put the output into the maven properties.
 *
 * @author shiyulong
 * @since 2024/1/6
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "minikube")
public class MinikubeExtension extends AbstractMavenLifecycleParticipant {

    private final Logger logger;

    @Inject
    public MinikubeExtension(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        injectProperties(session);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        injectProperties(session);
    }

    private void injectProperties(MavenSession session) {
        logger.info("------------------------------------------------------------------------");
        logger.info("[minikube-maven-extension] Trying to get minikube docker env");
        logger.info("------------------------------------------------------------------------");
        for (Map.Entry<String, String> entry : getDockerEnv().entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            logger.info(String.format("%s: %s", key, val));
            session.getSystemProperties().setProperty(key, val);
        }
    }

    /**
     * Get the docker environment variables by running the command [<code>minikube docker-env</code>].
     *
     * @return a Map containing the exported environment variables
     */
    private Map<String, String> getDockerEnv() {

        String command = "minikube docker-env";

        try {
            Process process = Runtime.getRuntime().exec(command);
            return analyzeOutput(process, command);
        } catch (IOException e) {
            logger.warn(String.format("Failed to get docker env from running %s due to %s",
                    command, e.getMessage()));
            return new HashMap<>();
        }

    }

    /**
     * Analyze the output of the command and return the environment variables.
     *
     * @param process The process object to run the command
     * @param command the command to run
     * @return a Map containing the exported environment variables
     */
    private Map<String, String> analyzeOutput(Process process, String command) {
        try {
            boolean execFinished = process.waitFor(5, TimeUnit.SECONDS);
            if (!execFinished) {
                logger.warn(String.format("Timeout when running %s", command));
                return new HashMap<>();
            }
        } catch (InterruptedException e) {
            logger.warn(String.format("Interrupted when running %s", command));
            return new HashMap<>();
        }

        int exitValue = process.exitValue();
        if (exitValue == 0) {
            Map<String, String> env = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("export")) {
                        String envExport = line.substring("export".length() + 1);
                        String[] arr = envExport.split("=");
                        if (arr.length == 2) {
                            env.put(arr[0].trim(), trimQuotes(arr[1].trim()));
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn(String.format("Failed to read output from running %s due to %s",
                        command, e.getMessage()));
            }
            return env;
        }

        logger.warn(String.format("the command [%s] exit with code: %d", command, exitValue));
        processError(process.getInputStream(), command);
        processError(process.getErrorStream(), command);
        return new HashMap<>();

    }

    private void processError(InputStream inputStream, String command) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.warn(line);
            }
        } catch (IOException e) {
            logger.warn(String.format("Failed to read output from running %s due to %s",
                    command, e.getMessage()));
        }
    }

    private String trimQuotes(String str) {
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }
        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}
