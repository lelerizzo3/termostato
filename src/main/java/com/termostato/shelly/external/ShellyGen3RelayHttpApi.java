package com.termostato.shelly.external;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ShellyGen3RelayHttpApi {

    @PostExchange("/rpc/Switch.Get")
    ShellyGen3SwitchGetResponse leggiStato(@RequestBody ShellyGen3SwitchGetRequest request);

    @PostExchange("/rpc/Switch.Set")
    void setStato(@RequestBody ShellyGen3SwitchSetRequest request);
}
