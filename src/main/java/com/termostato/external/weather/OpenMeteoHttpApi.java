package com.termostato.external.weather;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;

@HttpExchange
public interface OpenMeteoHttpApi {

    @GetExchange("/v1/forecast")
    OpenMeteoResponse current(
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam("current") String current,
            @RequestParam("timezone") String timezone);
}
