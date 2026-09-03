package com.termostato.external.notification;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface NtfyHttpApi {

    @PostExchange("/{topic}")
    void pubblica(@PathVariable("topic") String topic,
                  @RequestBody String messaggio,
                  @RequestHeader("Priority") String priorita);
}
