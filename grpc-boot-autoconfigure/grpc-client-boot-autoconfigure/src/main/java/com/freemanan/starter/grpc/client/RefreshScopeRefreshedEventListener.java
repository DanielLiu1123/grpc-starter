package com.freemanan.starter.grpc.client;

import io.grpc.stub.AbstractStub;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ObjectUtils;

/**
 * @author Freeman
 */
public class RefreshScopeRefreshedEventListener
        implements ApplicationListener<RefreshScopeRefreshedEvent>, BeanFactoryAware {

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        refreshGrpcStubOptions();
    }

    private void refreshGrpcStubOptions() {
        String[] beanNames = beanFactory.getBeanNamesForType(AbstractStub.class, false, false);

        List<String> grpcStubNames =
                Arrays.stream(beanNames).filter(beanFactory::containsSingleton).collect(Collectors.toList());

        if (ObjectUtils.isEmpty(grpcStubNames)) {
            return;
        }

        GrpcClientProperties properties = beanFactory.getBean(GrpcClientProperties.class);

        grpcStubNames.stream()
                .map(beanFactory::getBean)
                .map(AbstractStub.class::cast)
                .forEach(stub -> {
                    GrpcClientOptions opt = stub.getCallOptions().getOption(GrpcClientOptions.KEY);
                    if (opt != null) {
                        GrpcClientProperties.Channel config = GrpcChannelCreator.getMatchedConfig(
                                AopProxyUtils.ultimateTargetClass(stub), properties);
                        GrpcClientCreator.setOptionValues(opt, config);
                    }
                });
    }
}
