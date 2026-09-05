package com.termostato.web;

import com.termostato.domain.control.CurrentStateService;
import com.termostato.domain.model.CurrentState;
import com.termostato.web.dto.CurrentStateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stato")
public class CurrentStateController {

    private final CurrentStateService currentState;

    public CurrentStateController(CurrentStateService currentState) {
        this.currentState = currentState;
    }

    @GetMapping
    public CurrentStateResponse getCurrentState() {
        CurrentState state = currentState.read();
        return new CurrentStateResponse(
                state.temperatura(), state.umidita(), state.temperaturaTarget(), state.relayAcceso(),
                state.temperaturaEsterna(), state.umiditaEsterna());
    }
}
