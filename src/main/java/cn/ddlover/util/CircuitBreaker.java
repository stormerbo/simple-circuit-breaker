package cn.ddlover.util;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author stormer.xia
 * @version 1.0
 * @date 2021/4/7 12:45
 */
public class CircuitBreaker<T> {

  /**
   * totalTime次发生failToBreaker次失败，打开断路器
   */
  private final int failToBreaker;

  /**
   * 采样的次数
   */
  private final int totalTime;

  /**
   * 重置状态的时间
   */
  private final long resetTime;

  /**
   * 重置状态的时间单位
   */
  private final TimeUnit unit;

  /**
   * fallback的逻辑
   */
  private final Supplier<T> fallback;
  /**
   * 断路器的装态，
   * 0-关闭
   * 大于0 -打开
   */
  private final AtomicInteger state = new AtomicInteger(0);
  /**
   * 计算失败了的次数
   */
  private final AtomicInteger failedNum = new AtomicInteger(0);
  /**
   * 用于计算最近50次的次数
   */
  private final Queue<Integer> queue;

  /**
   * 用于重置断路器状态
   */
  private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new SynchronousQueue<>());

  public CircuitBreaker(int failToBreaker, int totalTime, long resetTime, TimeUnit unit, Supplier<T> fallback) {
    this.failToBreaker = failToBreaker;
    this.totalTime = totalTime;
    this.fallback = fallback;
    this.resetTime = resetTime;
    this.unit = unit;
    queue = new LinkedBlockingDeque<>(this.totalTime);
  }

  public boolean canRun() {
    return this.state.get() == 0;
  }

  public T doFallBack() {
    return this.fallback.get();
  }

  /**
   * 重置
   */
  public void reset() {
    this.failedNum.set(0);
    this.state.set(0);
    this.queue.clear();
  }

  /**
   * 执行失败的逻辑
   */
  private void addFailedNum() {
    int failedNum = this.failedNum.addAndGet(1);
    // 判断断路器是否需要打开
    if (failedNum >= failToBreaker && state.get() != 1) {
      int i = state.addAndGet(1);
      // 只允许第一个判断失败的线程去发起定时任务
      // 打开断路器成功后，需要一个定时的关掉断路器的功能
      if (i == 1) {
        System.out.println("打开熔断器, 时间:" + System.currentTimeMillis());
        scheduleToReset();
      }
    }
  }

  /**
   * 定时重置
   */
  private void scheduleToReset() {
    executor.submit(() -> {
      try {
        unit.sleep(resetTime);
        reset();
        System.out.println("关闭熔断器, 时间:" + System.currentTimeMillis());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * 增加执行的次数，通过队列+滑动窗口的方式控制最近totalTime次
   * @param success 执行是否成功
   */
  public void addExecuteNum(boolean success) {
    // 放入成功，说明数量不足totalTime
    if (queue.offer(success ? 0 : 1)) {
      // 不成功的执行，需要计算失败次数，判断是否打开熔断
      if (!success) {
        addFailedNum();
      }
    } else {
      Integer head = queue.poll();
      // 防止刚好发生reset的操作
      if (Objects.nonNull(head)) {
        this.failedNum.addAndGet(-head);
      }
      // 移除第一个元素后，重新放进队列
      addExecuteNum(success);
    }
  }
}
