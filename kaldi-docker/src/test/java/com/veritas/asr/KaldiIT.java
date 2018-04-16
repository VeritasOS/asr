package com.veritas.asr;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import static org.assertj.core.api.Assertions.*;

public class KaldiIT {
    String statusUrl = "ws://[DOCKER_IP]:[DOCKER_PORT]/client/ws/status";
    String recognizeUrl = "http://[DOCKER_IP]:[DOCKER_PORT]/client/dynamic/recognize";
    KaldiStatusWebSocketClient webSocketClient;

    @BeforeClass
    public void beforeClass() throws Exception {
        URI statusUri = new URI(statusUrl.replace("[DOCKER_IP]", getDockerIp()).replace("[DOCKER_PORT]", getDockerPort()));
        webSocketClient = new KaldiStatusWebSocketClient(statusUri);
        webSocketClient.connect();
    }

    @AfterClass
    public void afterClass() {
        webSocketClient.close();
    }

    @Test
    public void wavFileIsRecognized_1() throws Exception {
        String file = getTestingFile("cc-01.wav");
        String text = recognizeSpeech(file, "audio/wav", 16000);
        assertThat(text).isEqualTo("well here's a story for you sarah perry was a veterinary nurse.");
    }

    @Test
    public void wavFileIsRecognized_2() throws Exception {
        String file = getTestingFile("cc-21.wav");
        String text = recognizeSpeech(file, "audio/wav", 16000);
        assertThat(text).isEqualTo("but sarah had a different idea.");
    }

    @Test
    public void wavFileIsRecognized_3() throws Exception {
        String file = getTestingFile("cc-03.wav");
        String text = recognizeSpeech(file, "audio/wav", 16000);
        assertThat(text).isEqualTo("so she was very happy to start a new job at a superb private practice.");
    }

    @Test
    public void wavFileIsRecognized_4() throws Exception {
        String file = getTestingFile("a0010.wav");
        String text = recognizeSpeech(file, "audio/wav", 16000);
        assertThat(text).isEqualTo("i'm playing a single and in what looks like a losing game.");
        //expected text: " i'm playing a single hand in what looks like a losing game. "
    }

    @Test
    public void mp3FileIsRecognized_1() throws Exception {
        String file = getTestingFile("b0482.mp3");
        String text = recognizeSpeech(file, "audio/mp3", 16000);
        assertThat(text).isEqualTo("many other similar disconcerting emissions will be noticed in the manuscript.");
    }

    @Test
    public void mp3FileIsRecognized_2() throws Exception {
        String file = getTestingFile("a0011.mp3");
        String text = recognizeSpeech(file, "audio/mp3", 16000);
        assertThat(text).isEqualTo("if i ever needed a fighter in my life like need one now.");
        //expected text: " if i ever needed a fighter in my life i need one now. "
    }

    private String getDockerPort() {
        return System.getProperty("docker.port", "8080");
    }

    private String getDockerIp() {
        return System.getProperty("docker.address", "localhost");
    }

    private String getTestingFile(String filename) {
        return getClass().getClassLoader().getResource(filename).getFile();
    }

    private String recognizeSpeech(String filePath, String mimeType, int sampleRate) throws Exception {
        webSocketClient.waitForAvailableWorkers();

        URI recognizeUri = new URI(recognizeUrl.replace("[DOCKER_IP]", getDockerIp()).replace("[DOCKER_PORT]", getDockerPort()));
        HttpPost post = new HttpPost(recognizeUri);
        InputStreamEntity entity = new InputStreamEntity(new FileInputStream(new File(filePath)));
        entity.setChunked(true);
        entity.setContentType(mimeType + "; rate=" + sampleRate);
        post.setEntity(entity);
        String response = getResponseEntityAsString(post);

        JSONObject obj = new JSONObject(response);
        return obj.getJSONArray("hypotheses").getJSONObject(0).getString("utterance");
    }

    private static String getResponseEntityAsString(HttpUriRequest request) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity == null)
                return null;

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK)
                return null;

            if (entity.getContentEncoding() == null)
                return EntityUtils.toString(entity, "UTF-8");
            else
                return EntityUtils.toString(entity);
        }
        finally {
        }
    }
}
