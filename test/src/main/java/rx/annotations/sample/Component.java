package rx.annotations.sample;

import rx.Observable;
import rx.annotations.RxObservable;
import rx.annotations.SuperClass;

@SuperClass(SomeFrameworkComponent.class)
public class Component extends Rx_Component {

  @Override
  protected void onCreate() {
    Observable.never().takeUntil(onDestroyObservable());
  }

  @RxObservable
  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

}
