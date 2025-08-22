package com.shacky.beibure_backend.controllers;

import com.shacky.beibure_backend.services.AliExpressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allow Angular frontend
public class AliExpressController {

    private final AliExpressService aliExpressService;

    @Autowired
    public AliExpressController(AliExpressService aliExpressService) {
        this.aliExpressService = aliExpressService;
    }

    // 🔍 Search Products
    @GetMapping("/products")
    public Mono<String> searchProducts(@RequestParam String keyword,
                                       @RequestParam(defaultValue = "20") int pageSize) {
        return aliExpressService.searchProducts(keyword, pageSize);
    }

    // 🔗 Generate Affiliate Link (trackingId now comes from application.properties)
    @GetMapping("/affiliate-link")
    public Mono<String> generateAffiliateLink(
            @RequestParam String urls,
            @RequestParam(defaultValue = "0") int linkType) {
        return aliExpressService.generateAffiliateLink(urls, linkType);
    }

    // 🔍 Search products and auto-generate affiliate links
    @GetMapping("/search-with-links")
    public Mono<String> searchWithAffiliateLinks(@RequestParam String keyword,
                                                 @RequestParam(defaultValue = "10") int pageSize) {
        return aliExpressService.searchProductsWithAffiliateLinks(keyword, pageSize);
    }
}
