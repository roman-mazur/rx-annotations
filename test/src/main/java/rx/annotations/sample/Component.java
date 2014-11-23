package rx.annotations.sample;

import rx.Observable;
import rx.annotations.RxObservable;
import rx.annotations.SuperClass;

@SuperClass(
    value = SomeFrameworkComponent.class,
    methods = "onLowMemory"
)
public class Component extends Rx_SomeFrameworkComponent {

  @Override
  protected void onCreate() {
    super.onCreate();
    Observable.never().takeUntil(onDestroyObservable());
    Observable.never().takeUntil(onLowMemoryObservable());
  }

  @RxObservable
  @Override
  protected void onDestroy() {
    super.onDestroy();
    System.out.println("Custom implementation");
  }

}
