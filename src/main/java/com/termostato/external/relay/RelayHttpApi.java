package com.termostato.external.relay;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface RelayHttpApi {

    @GetExchange("/relay")
    RelayStateResponse leggiStato();

    @PostExchange("/relay")
    void inviaComando(@RequestBody RelayCommand comando);
}
