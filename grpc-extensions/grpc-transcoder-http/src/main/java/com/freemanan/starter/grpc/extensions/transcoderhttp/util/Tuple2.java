package com.freemanan.starter.grpc.extensions.transcoderhttp.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Freeman
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Tuple2<A, B> {
    public final A t1;
    public final B t2;

    protected Tuple2(A t1, B t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }
}
