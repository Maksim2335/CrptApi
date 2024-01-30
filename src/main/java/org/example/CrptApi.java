package org.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final Semaphore _semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        _semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() ->
                        _semaphore.release(requestLimit - _semaphore.availablePermits()),
                0, timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 15);
        crptApi.createDocument(new Document(), "Signature");
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {

        _semaphore.acquire();

        HttpClient httpClient = HttpClientBuilder.create().build();
        String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        HttpPost httpPost = new HttpPost(URL);

        StringEntity entity = new StringEntity(getJsonString(document), ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);

        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Signature", signature);

        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost)){
            final HttpEntity httpEntity = response.getEntity();
            System.out.println(EntityUtils.toString(httpEntity));
        }
    }

    public String getJsonString(Document document) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(writer, document);
        return writer.toString();

    }

    @Data
    @NoArgsConstructor
    @JsonAutoDetect
    public static class Document {

        private Description description;

        public String docId;

        private String docStatus;

        private DocType docType;

        private Boolean importRequest;

        private String ownerInn;

        private String producerInn;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date productionDate;

        private String productionType;

        private List<Product> products;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date regDate;

        private String regNumber;

        public enum DocType {
            LP_INTRODUCE_GOODS
        }

    }

    @Data
    @NoArgsConstructor
    @JsonAutoDetect
    private static class Product {

        private String certificateDocument;

        private String certificateDocumentDate;

        private String certificateDocumentNumber;

        private String ownerInn;

        private String producerInn;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date productionDate;

        private String tnvedCode;

        private String uituCode;
    }

    @Data
    @NoArgsConstructor
    @JsonAutoDetect
    private static class Description {
        private String participantInn;
    }

}