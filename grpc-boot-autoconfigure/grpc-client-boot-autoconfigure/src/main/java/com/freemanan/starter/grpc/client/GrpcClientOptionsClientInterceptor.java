package com.freemanan.starter.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import java.util.concurrent.TimeUnit;

/**
 * Set stub options from {@link GrpcClientOptions} to {@link CallOptions}.
 *
 * @author Freeman
 * @see ClientInterceptor
 * @see GrpcClientOptions
 * @since 3.2.0
 */
public class GrpcClientOptionsClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        GrpcClientOptions opt = callOptions.getOption(GrpcClientOptions.KEY);
        if (opt == null) {
            return next.newCall(method, callOptions);
        }

        Deadline deadline = callOptions.getDeadline();
        if (deadline == null && opt.getDeadline() != null) {
            callOptions = callOptions.withDeadlineAfter(opt.getDeadline(), TimeUnit.MILLISECONDS);
        }

        Integer maxOutboundMessageSize = callOptions.getMaxOutboundMessageSize();
        if (maxOutboundMessageSize == null && opt.getMaxOutboundMessageSize() != null) {
            callOptions = callOptions.withMaxOutboundMessageSize(opt.getMaxOutboundMessageSize());
        }

        String compressor = callOptions.getCompressor();
        if (compressor == null && opt.getCompression() != null) {
            callOptions = callOptions.withCompression(opt.getCompression());
        }

        return next.newCall(method, callOptions);
    }
}
