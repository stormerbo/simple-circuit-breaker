package cn.ddlover.util;

import java.util.concurrent.Callable;

/**
 * @author stormer.xia
 * @version 1.0
 * @date 2021/4/7 12:45
 */
public class CircuitBreakerRunner<T> {

  public static <T> T run(CircuitBreaker<T> circuitBreaker, Callable<T> callable) throws Exception {
    if (circuitBreaker.canRun()) {
      boolean success = true;
      try {
        return callable.call();
      } catch (Exception e) {
        success = false;
        throw e;
      } finally {
        circuitBreaker.addExecuteNum(success);
      }
    } else {
      return circuitBreaker.doFallBack();
    }
  }
}
