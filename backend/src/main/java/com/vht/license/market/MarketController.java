package com.vht.license.market;

import com.vht.license.market.MarketDtos.CreateMarketRequest;
import com.vht.license.market.MarketDtos.MarketDto;
import com.vht.license.market.MarketDtos.UpdateMarketRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/markets")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping
    public List<MarketDto> list() {
        return marketService.list();
    }

    @GetMapping("/{id}")
    public MarketDto get(@PathVariable Long id) {
        return marketService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public MarketDto create(@Valid @RequestBody CreateMarketRequest req) {
        return marketService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MarketDto update(@PathVariable Long id, @Valid @RequestBody UpdateMarketRequest req) {
        return marketService.update(id, req);
    }
}
