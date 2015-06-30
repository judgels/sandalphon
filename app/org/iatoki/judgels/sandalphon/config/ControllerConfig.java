package org.iatoki.judgels.sandalphon.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
        "org.iatoki.judgels.sandalphon.controllers",
        "org.iatoki.judgels.jophiel.controllers"
})
public class ControllerConfig {

}
