package com.projectlounge;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by main on 24.08.17.
 */
@Configuration
@EnableTransactionManagement
public class SafebrowsingConfig {

    @Bean
    public MapperFacade getMapperFactory() {
        final DefaultMapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        return mapperFactory.getMapperFacade();
    }

}
