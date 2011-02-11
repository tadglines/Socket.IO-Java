package com.glines.socketio.annotation;

import com.glines.socketio.server.TransportType;

import java.lang.annotation.*;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Handle {
    TransportType[] value() default {};
}
