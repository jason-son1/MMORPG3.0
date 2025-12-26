package com.antigravity.rpg.api.lua;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as exposed to Lua scripts.
 * The method name in Lua will be the method name in Java, or the value if
 * provided.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LuaFunction {
    String value() default "";
}
