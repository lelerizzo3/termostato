package com.termostato.shelly.external;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ShellyGen1RelayHttpApi {

    @GetExchange("/relay/0")
    ShellyGen1RelayResponse leggiStato();

    @PostExchange("/relay/0")
    void inviaComando(@RequestParam("turn") String cmd);
}
