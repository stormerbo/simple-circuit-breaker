package cn.ddlover.util;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * @author stormer.xia
 * @version 1.0
 * @date 2021/4/7 13:12
 */
public class CircuitBreakerRunnerTest {

  ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 0L, TimeUnit.SECONDS, new SynchronousQueue<>(), new CallerRunsPolicy());

  @Test
  public void run() throws Exception {
    CircuitBreaker<String> circuitBreaker = new CircuitBreaker<>(10, 50,10, TimeUnit.MILLISECONDS, () -> "降级");

    Callable<String> callable = () -> {
      Random r = new Random();
      int i = r.nextInt(2);
      if (i == 1) {
        throw new Exception();
      }
      return "success";
    };
    int i = 100;
    for (int i1 = 0; i1 < i; i1++) {
      executor.submit(() -> {
        try {
          System.out.println(CircuitBreakerRunner.run(circuitBreaker, callable));
        } catch (Exception e) {
          System.out.println("抛异常");
        }
      });
    }
  }
}