package com.freemanan.starter.grpc.extensions.transcoderjson.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Freeman
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Tuple3<A, B, C> extends Tuple2<A, B> {
    public final C t3;

    protected Tuple3(A t1, B t2, C t3) {
        super(t1, t2);
        this.t3 = t3;
    }

    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }
}
