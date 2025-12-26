package com.antigravity.rpg.core.script;

import com.antigravity.rpg.api.lua.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.Varargs;

import java.lang.reflect.Method;

public class LuaFunctionRegistrar {

    /**
     * Registers all @LuaFunction annotated methods from the target object into the
     * lua value (table).
     */
    public static void register(LuaValue table, Object target) {
        for (Method method : target.getClass().getMethods()) {
            if (method.isAnnotationPresent(LuaFunction.class)) {
                LuaFunction annotation = method.getAnnotation(LuaFunction.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                table.set(name, new ReflectionLuaFunction(target, method));
            }
        }
    }

    static class ReflectionLuaFunction extends VarArgFunction {
        private final Object target;
        private final Method method;

        public ReflectionLuaFunction(Object target, Method method) {
            this.target = target;
            this.method = method;
        }

        @Override
        public Varargs invoke(Varargs args) {
            try {
                Class<?>[] paramTypes = method.getParameterTypes();
                Object[] javaArgs = new Object[paramTypes.length];

                int luaArgIndex = 1;

                for (int i = 0; i < paramTypes.length; i++) {
                    if (luaArgIndex > args.narg()) {
                        javaArgs[i] = null;
                        continue;
                    }
                    LuaValue arg = args.arg(luaArgIndex++);
                    javaArgs[i] = CoerceLuaToJava.coerce(arg, paramTypes[i]);
                }

                Object result = method.invoke(target, javaArgs);
                return result == null ? LuaValue.NIL : CoerceJavaToLua.coerce(result);
            } catch (Exception e) {
                // e.printStackTrace();
                throw new RuntimeException("Error calling Lua function: " + method.getName(), e);
            }
        }
    }
}
