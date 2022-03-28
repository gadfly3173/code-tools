import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Gadfly
 * @since 2022-02-16 15:30
 */
@Slf4j
public class FunctionUtil {
    public static void retryFunction(ThrowExceptionRunnable runnable, int time) {
        while (true) {
            try {
                runnable.run();
                return;
            } catch (Exception e) {
                time--;
                if (time <= 0) throw new RuntimeException(e);
            }
        }
    }
    public static <T, R> R retryFunction(ThrowExceptionFunction<T, R> function, T t, int time) {
        while (true) {
            try {
                return function.apply(t);
            } catch (Exception e) {
                time--;
                if (time <= 0) throw new RuntimeException(e);
            }
        }
    }
    public static <T, U, R> R retryFunction(ThrowExceptionBiFunction<T, U, R> function, T t, U u, int time) {
        while (true) {
            try {
                return function.apply(t, u);
            } catch (Exception e) {
                time--;
                if (time <= 0) throw new RuntimeException(e);
            }
        }
    }

    public static <T, R> R cacheFunction(Function<T, R> function, T t, Map<T, R> cache) {
        R r = cache.get(t);
        if (r != null) return r;
        R result = function.apply(t);
        cache.put(t,result);
        return result;
    }

    public static <T, R> R computeOrGetDefault(ThrowExceptionFunction<T, R> function, T t, R r) {
        try {
            return function.apply(t);
        } catch (Exception e) {
            return r;
        }
    }
    public static <R> R computeOrGetDefault(ThrowExceptionSupplier<R> supplier,R r){
        try {
            return supplier.get();
        } catch (Exception e) {
            return r;
        }
    }

    public static <T, R> R computeAndDealException(ThrowExceptionFunction<T, R> function, T t, Function<Exception, R> dealFunc) {
        try {
            return function.apply(t);
        } catch (Exception e) {
            return dealFunc.apply(e);
        }
    }

    public static <T, U, R> R computeAndDealException(ThrowExceptionBiFunction<T,U, R> function, T t, U u,Function<Exception, R> dealFunc) {
        try {
            return function.apply(t,u);
        } catch (Exception e) {
            return dealFunc.apply(e);
        }
    }

    public static <R> R computeAndDealException(ThrowExceptionSupplier<R> supplier, Function<Exception, R> dealFunc) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return dealFunc.apply(e);
        }
    }

    public static <T, R> R logFunction(Function<T, R> function, T t, String logTitle) {
        long startTime = System.currentTimeMillis();
        log.info("[[title={}]],request={},requestTime={}", logTitle, t.toString(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        R apply = function.apply(t);
        long endTime = System.currentTimeMillis();
        log.info("[[title={}]],response={},spendTime={}ms", logTitle, apply.toString(), endTime - startTime);
        return apply;
    }

    @FunctionalInterface
    interface ThrowExceptionFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    interface ThrowExceptionBiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }
    @FunctionalInterface
    interface ThrowExceptionSupplier<T> {
        T get() throws Exception;
    }
    @FunctionalInterface
    interface ThrowExceptionRunnable {
        void run() throws Exception;
    }
}
