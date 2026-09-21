package com.termostato.shelly.external;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ShellyGen3PmHttpApi {

    @PostExchange("/rpc/PM1.GetStatus")
    ShellyPmStatusResponse leggiConsumo(@RequestBody ShellyPmStatusRequest request);
}
