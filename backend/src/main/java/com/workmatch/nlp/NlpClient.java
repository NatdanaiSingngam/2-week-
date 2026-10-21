package com.workmatch.nlp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** เรียก service NLP ของณัฐดนัย (FastAPI) ตาม nlp-service/api-contract.md */
@Component
public class NlpClient {

    private final RestClient restClient;

    public NlpClient(@Value("${nlp.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // เชื่อมต่อไม่ได้ภายใน 2 วินาที = ล้มเหลว
        factory.setReadTimeout(5000);    // รอผลเกิน 5 วินาที = ล้มเหลว
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** POST /extract {"text": "..."} -> {"skills":[{"name","category"}]} */
    public List<ExtractedSkill> extractSkills(String text) {
        try {
            ExtractResponse body = restClient.post()
                    .uri("/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(ExtractResponse.class);
            if (body == null || body.skills() == null) return List.of();
            return body.skills();
        } catch (RestClientException e) {
            throw new NlpUnavailableException(e);
        }
    }

    record ExtractResponse(List<ExtractedSkill> skills) {
    }
}
