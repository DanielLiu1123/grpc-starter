package com.freemanan.starter.grpc.extensions.discovery.registration.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.freemanan.starter.grpc.server.GrpcServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ConsulAutoServiceRegistration.class)
public class Consul {

    @Bean
    GrpcConsulAutoServiceRegistrationListener grpcConsulAutoServiceRegistrationListener(
            ConsulAutoServiceRegistration registration,
            ConsulAutoRegistration consulAutoRegistration,
            GrpcServer grpcServer) {
        return new GrpcConsulAutoServiceRegistrationListener(registration, consulAutoRegistration, grpcServer);
    }

    @RequiredArgsConstructor
    static class GrpcConsulAutoServiceRegistrationListener implements ApplicationListener<ApplicationReadyEvent> {

        private final ConsulAutoServiceRegistration registration;
        private final ConsulAutoRegistration consulAutoRegistration;
        private final GrpcServer grpcServer;

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            if (grpcServer.getPort() > 0) {
                NewService.Check oldCheck = consulAutoRegistration.getService().getCheck();

                // not support grpc health check
                consulAutoRegistration.getService().setCheck(null);
                registration.start();

                // restore old check
                consulAutoRegistration.getService().setCheck(oldCheck);
            }
        }
    }
}
