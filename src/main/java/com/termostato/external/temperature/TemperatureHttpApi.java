package com.termostato.external.temperature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface TemperatureHttpApi {

    @GetExchange("/temperature")
    TemperatureReading leggiTemperatura();
}
