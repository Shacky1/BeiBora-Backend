package com.shacky.beibure_backend.services;

import reactor.core.publisher.Mono;

public interface AliExpressService {

    Mono<String> searchProducts(String keyword, int pageSize);

    Mono<String> generateAffiliateLink(String urls, int linkType);

    Mono<String> searchProductsWithAffiliateLinks(String keyword, int pageSize);
}
