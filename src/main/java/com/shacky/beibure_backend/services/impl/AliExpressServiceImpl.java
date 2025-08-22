package com.shacky.beibure_backend.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shacky.beibure_backend.services.AliExpressService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AliExpressServiceImpl implements AliExpressService {

    @Value("${aliexpress.appkey}")
    private String appKey;

    @Value("${aliexpress.appsecret}")
    private String appSecret;

    @Value("${aliexpress.trackingId}")
    private String trackingId;  // ✅ injected automatically

    private final WebClient webClient = WebClient.create("https://api-sg.aliexpress.com");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<String> searchProducts(String keyword, int pageSize) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("method", "aliexpress.affiliate.product.query");
        params.add("app_key", appKey);
        params.add("keywords", keyword);
        params.add("page_size", String.valueOf(pageSize));
        params.add("format", "json");
        params.add("sign_method", "md5");
        params.add("timestamp", timestamp);

        String sign = calculateSignature(params, appSecret);
        params.add("sign", sign);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/sync").queryParams(params).build())
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public Mono<String> generateAffiliateLink(String urls, int linkType) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("method", "aliexpress.affiliate.link.generate");
        params.add("app_key", appKey);
        params.add("promotion_link_type", String.valueOf(linkType));
        params.add("source_values", urls);
        params.add("tracking_id", trackingId); // ✅ always from config
        params.add("format", "json");
        params.add("sign_method", "md5");
        params.add("timestamp", timestamp);

        String sign = calculateSignature(params, appSecret);
        params.add("sign", sign);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/sync").queryParams(params).build())
                .retrieve()
                .bodyToMono(String.class);
    }

    @Override
    public Mono<String> searchProductsWithAffiliateLinks(String keyword, int pageSize) {
        return searchProducts(keyword, pageSize)
                .flatMap(productResponse -> {
                    try {
                        JsonNode root = objectMapper.readTree(productResponse);
                        JsonNode products = root.path("aliexpress_affiliate_product_query_response")
                                .path("resp_result")
                                .path("result")
                                .path("products");

                        if (products.isMissingNode() || !products.isArray()) {
                            return Mono.just("{\"error\":\"No products found\"}");
                        }

                        List<String> urls = new ArrayList<>();
                        for (JsonNode product : products) {
                            String url = product.path("product_url").asText();
                            if (!url.isEmpty()) urls.add(url);
                        }

                        if (urls.isEmpty()) {
                            return Mono.just("{\"error\":\"No product URLs found\"}");
                        }

                        String joinedUrls = String.join(",", urls);
                        return generateAffiliateLink(joinedUrls, 0)
                                .map(affiliateResponse -> {
                                    ObjectNode combined = objectMapper.createObjectNode();
                                    combined.set("products", products);
                                    combined.put("affiliate_links", affiliateResponse);
                                    return combined.toString();
                                });

                    } catch (Exception e) {
                        return Mono.just("{\"error\":\"Failed to parse product response\"}");
                    }
                });
    }

    // ✅ Signature generator
    private String calculateSignature(MultiValueMap<String, String> params, String secret) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder baseString = new StringBuilder(secret);
        for (String key : keys) {
            baseString.append(key).append(params.getFirst(key));
        }
        baseString.append(secret);

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(baseString.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b).toUpperCase();
                if (hex.length() == 1) hexString.append("0");
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }
}
