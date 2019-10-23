package com.lisn.myrxjavademo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 拆分使用
     *
     * @param view
     */
    public void bt_ChaiFen(View view) {

        //1. 创建被观察者
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                // 发送消息
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        });

        //2. 创建观察者
        Observer<Integer> observer = new Observer<Integer>() {
            private Disposable d;

            @Override
            public void onSubscribe(Disposable d) {
                this.d = d;
                System.out.println("建立订阅关系");
            }

            @Override
            public void onNext(Integer integer) {
                //接受到消息
                System.out.println(integer);

                if (integer == 2) {
                    // 在RxJava 2.x 中，新增的Disposable可以做到切断的操作，让Observer观察者不再接收上游事件
                    d.dispose();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                System.out.println("完成");
            }
        };

        //3. 建立订阅关系
        observable.subscribe(observer);
    }

    /**
     * 链式调用
     *
     * @param view
     */
    public void bt_LianShi(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                // 发送消息
                e.onNext(1);
                e.onNext(2);
                e.onComplete();
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                System.out.println("建立订阅关系");
            }

            @Override
            public void onNext(Integer integer) {
                //接受到消息
                System.out.println(integer);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                System.out.println("完成");
            }
        });
    }

    /**
     * 更简单的观察者-消费者
     * <p>
     * Consumer相对Observer简化了很多，没有了onSubscribe() onError () onComplete ()，当然也无法对这些进行监听了。
     *
     * @param view
     */
    public void bt_xiaoFeiZhe(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                // 发送消息
                e.onNext(1);
                e.onNext(2);
                e.onComplete();
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                System.out.println(integer);
            }
        });
    }

    /**
     * 创建操作符
     * 上面用的creat是创建被观察者的一种操作符，另外常用的还有just、fromArray、range、empty，直接看运行结果去理解就好了。
     * empty这里说下，这个使用场景比如一个耗时操作不要任何数据反馈去更新UI，只是显示和隐藏加载动画。
     *
     * @param view
     */
    public void bt_CaoZuoFu(View view) {
        System.out.println("-----------------just");
        Observable.just(1, 2, 3).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                System.out.println(integer);
            }
        });

        System.out.println("-----------------fromArray");
        Observable.fromArray(new Integer[]{1, 2, 3}).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                System.out.println(integer);
            }
        });

        System.out.println("-----------------range");
        Observable.range(0, 3).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                System.out.println(integer);
            }
        });

        System.out.println("-----------------empty");
        Observable.empty().subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                System.out.println("建立订阅关系");
            }

            @Override
            public void onNext(Object object) {
                //接受到消息
                System.out.println(object);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                System.out.println("完成");
            }
        });
    }


    //region 合并操作符

    /**
     * 合并操作符
     * <p>
     * 合并操作是指合并被观察者，用同一个观察者去接受，常用的有concatWith、startWith、concat、merge、zip，
     * 这里为了显示出合并的区别，用了另一个创建创建操作符intervalRange，
     * 比如Observable.intervalRange(0, 10, 0, 1, TimeUnit.SECONDS)，
     * 这个代表从0开始发送10个数，延迟0秒后开始执行，每1秒发送一次。
     * 用这两个被观察者测试上面几个合并操作符：
     * <p>
     * 根据下面输出结果总结：
     * 1.concatWith 和 startWith是执行的先后顺序不一样，是同步执行的
     * 2.concatWith 和 concat都是顺序执行，只是写法不一样
     * 3.concat 和 merge写法一样，但是merge是异步的，两个被观察者没有先后顺序，各自执行。
     *
     * @param view
     */

    //发送0-4
    Observable observable1 = Observable.intervalRange(0, 5, 0, 1, TimeUnit.SECONDS);
    //发送10-14
    Observable observable2 = Observable.intervalRange(10, 5, 0, 1, TimeUnit.SECONDS);

    /**
     * 输出
     * E/MainActivity: -----------------concatWith
     * E/MainActivity: concatWith: 0
     * E/MainActivity: concatWith: 1
     * E/MainActivity: concatWith: 2
     * E/MainActivity: concatWith: 3
     * E/MainActivity: concatWith: 4
     * E/MainActivity: concatWith: 10
     * E/MainActivity: concatWith: 11
     * E/MainActivity: concatWith: 12
     * E/MainActivity: concatWith: 13
     * E/MainActivity: concatWith: 14
     */
    public void concatWith(View view) {
        Log.e(TAG, "-----------------concatWith");
        observable1.concatWith(observable2).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.e(TAG, "concatWith: " + aLong);
            }
        });
    }

    /**
     * 输出
     * E/MainActivity: -----------------startWith
     * E/MainActivity: startWith: 10
     * E/MainActivity: startWith: 11
     * E/MainActivity: startWith: 12
     * E/MainActivity: startWith: 13
     * E/MainActivity: startWith: 14
     * E/MainActivity: startWith: 0
     * E/MainActivity: startWith: 1
     * E/MainActivity: startWith: 2
     * E/MainActivity: startWith: 3
     * E/MainActivity: startWith: 4
     */
    public void startWith(View view) {
        Log.e(TAG, "-----------------startWith");
        observable1.startWith(observable2).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.e(TAG, "startWith: " + aLong);
            }
        });
    }

    /**
     * 输出
     * E/MainActivity: -----------------concat
     * E/MainActivity: concat: 0
     * E/MainActivity: concat: 1
     * E/MainActivity: concat: 2
     * E/MainActivity: concat: 3
     * E/MainActivity: concat: 4
     * E/MainActivity: concat: 10
     * E/MainActivity: concat: 11
     * E/MainActivity: concat: 12
     * E/MainActivity: concat: 13
     * E/MainActivity: concat: 14
     *
     * @param view
     */
    public void concat(View view) {
        Log.e(TAG, "-----------------concat");
        Observable.concat(observable1, observable2).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.e(TAG, "concat: " + aLong);
            }
        });
    }

    /**
     * 输出
     * E/MainActivity: -----------------merge
     * E/MainActivity: merge: 0
     * E/MainActivity: merge: 10
     * E/MainActivity: merge: 1
     * E/MainActivity: merge: 11
     * E/MainActivity: merge: 2
     * E/MainActivity: merge: 12
     * E/MainActivity: merge: 3
     * E/MainActivity: merge: 13
     * E/MainActivity: merge: 4
     * E/MainActivity: merge: 14
     *
     * @param view
     */
    public void merge(View view) {
        Log.e(TAG, "-----------------merge");
        Observable.merge(observable1, observable2).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Exception {
                Log.e(TAG, "merge: " + aLong);
            }
        });
    }

    /**
     * 把被观察者合并时一一对应
     * <p>
     * 输出
     * accept: 语文：100
     * accept: 数学：80
     * accept: 英语：60
     *
     * @param view
     */
    public void zip(View view) {
        Observable observable1 = Observable.just("语文", "数学", "英语");
        Observable observable2 = Observable.just("100", "80", "60");
        Observable.zip(observable1, observable2, new BiFunction() {
            @Override
            public Object apply(Object o, Object o2) throws Exception {
                return o.toString() + "：" + o2.toString();
            }
        }).subscribe(new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                Log.e(TAG, "accept: " + o);
            }
        });
    }
    //endregion

    //region 变换操作符  常见的有map、concatMap、flatMap、groupBy、buffer

    /**
     * 运行输出
     * E/MainActivity: -----------------map
     * E/MainActivity: accept: 转化为String1
     * <p>
     * 也就是说map里可以把被观察者传递过来的数据转换成另一种数据格式传递给观察者，这里是Integer转String，
     * 比如你也可以被观察者传递过来一个URL，在Function直接网络请求，转化成请求结果给观察者。
     *
     * @param view
     */
    public void map(View view) {
        Log.e(TAG, "-----------------map");
        Observable.just(1)
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return "转化为String" + integer;
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String string) throws Exception {
                        Log.e(TAG, "merge: " + string);
                    }
                });
    }

    /**
     * 这个相对map更灵活，map是的Function里直接返回的是转换之后的数据，一对一的，
     * 而flatMap的Function返回的是另一个被观察者，所以这个可以在里面随意发送给观察者。
     * <p>
     * 运行输出
     * E/MainActivity: accept: 2.1
     * E/MainActivity: accept: 2.2
     * E/MainActivity: accept: 2.3
     * E/MainActivity: accept: 1.1
     * E/MainActivity: accept: 3.1
     * E/MainActivity: accept: 3.2
     * E/MainActivity: accept: 3.3
     * E/MainActivity: accept: 1.2
     * E/MainActivity: accept: 1.3
     * <p>
     * 每个都是先.1再.2再.3没错，但是整体并没有按照1、2、3顺序执行，说明他们是异步执行的，
     * 类似合并操作符中的merge（其实内部调用的就是merge）。
     * 看完这个问题，就可以猜到concatMap的作用了，就不贴了，是完全按顺序同步输出的
     *
     * @param view
     */
    public void flatMap2(View view) {
        Observable.just(1, 2, 3)
                .flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer integer) throws Exception {
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            list.add(integer + "." + (1 + i));
                        }
                        return Observable.fromIterable(list).delay(1, TimeUnit.SECONDS);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String string) throws Exception {
                        Log.e(TAG, "accept: " + string);
                    }
                });
    }

    /**
     * groupBy是按条件分组
     * <p>
     * 运行输出
     * E/MainActivity: accept: 20：不及格
     * E/MainActivity: accept: 40：不及格
     * E/MainActivity: accept: 60：及格
     * E/MainActivity: accept: 80：及格
     * E/MainActivity: accept: 100：及格
     *
     * @param view
     */
    public void group(View view) {
        Observable.just(20, 40, 60, 80, 100)
                .groupBy(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return integer >= 60 ? "及格" : "不及格";
                    }
                })
                .subscribe(new Consumer<GroupedObservable<String, Integer>>() {
                    @Override
                    public void accept(final GroupedObservable<String, Integer> stringIntegerGroupedObservable) throws Exception {
                        stringIntegerGroupedObservable.subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                Log.e(TAG, "accept: " + integer + "：" + stringIntegerGroupedObservable.getKey());
                            }
                        });
                    }
                });
    }

    /**
     * buffer是分批发送
     * <p>
     * 运行输出
     * accept: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
     * accept: [20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39]
     * accept: [40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59]
     * accept: [60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79]
     * accept: [80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99]
     *
     * @param view
     */
    public void buffer(View view) {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                        for (int i = 0; i < 100; i++) {
                            e.onNext(i);
                        }
                        e.onComplete();
                    }
                })
                .buffer(20)
                .subscribe(new Consumer<List<Integer>>() {
                    @Override
                    public void accept(List<Integer> integer) throws Exception {
                        Log.d(TAG, "accept: " + integer);
                    }
                });
    }
    //endregion

    //region 过滤操作符
    //条件筛选，输出B、C
    public void filter(View view) {
        Observable.just("A", "B", "C")
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        if ("A".equals(s)) {
                            return false;
                        }

                        return true;
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e(TAG, "accept: " + s);
                    }
                });
    }


    //用于停止定时器，输出0、1、2、3、4
    public void take(View view) {
        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)// 5次之后停下
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Log.e(TAG, "accept: " + aLong);
                    }
                });

    }

    //过滤重复，输出1、2、3
    public void distinct(View view) {
        Observable.just(1, 1, 2, 3, 3)
                .distinct()
                .subscribe(new Consumer<Integer>() { // 下游 观察者
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.d(TAG, "accept: " + integer);
                    }
                });
    }

    //制定发送角标(索引下标)，输出B
    public void elementAt(View view) {
        Observable.just("A", "B", "C")
                .elementAt(1, "X") //如果索引为空使用默认值 "X"
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG, "accept: " + s);
                    }
                });


    }
    //endregion

    //region 条件操作符
    //等于Java中if的连续判断，有一个等于C就返回false，输出false
    public void all(View view) {
        Observable.just("A", "B", "C", "D")
                .all(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        return !s.equals("C");
                    }
                })
                .subscribe(new Consumer<Boolean>() { // 下游 观察者
                    @Override
                    public void accept(Boolean s) throws Exception {
                        Log.d(TAG, "accept: " + s);
                    }
                });
    }

    //判断包含
    public void contains(View view) {
        Observable.just("A", "B", "C", "D")
                .contains("C")
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean s) throws Exception {
                        Log.d(TAG, "accept: " + s);
                    }
                });
    }

    //和上面的All相反，有一个等于C就返回true，输出true
    public void any(View view) {
        Observable.just("A", "B", "C", "D")
                .any(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        return !s.equals("C");
                    }
                })
                .subscribe(new Consumer<Boolean>() { // 下游 观察者
                    @Override
                    public void accept(Boolean s) throws Exception {
                        Log.d(TAG, "accept: " + s);
                    }
                });
    }
    //endregion

    //region 异常处理操作符

    /**
     * 输出
     * D/MainActivity: onNext: 0
     * D/MainActivity: onNext: 1
     * D/MainActivity: onNext: 2
     * D/MainActivity: onNext: 3
     * D/MainActivity: onNext: 4
     * D/MainActivity: onError: 模拟一个错误
     *
     * @param view
     */
    public void onError(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        e.onError(new Throwable("模拟一个错误"));
                    }
                    e.onNext(i);
                }
                e.onComplete();
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }

    /**
     * 输出
     * onNext: 0
     * onNext: 1
     * onNext: 2
     * onNext: 3
     * onNext: 4
     * onNext: 400
     * onComplete:
     *
     * @param view
     */
    public void onErrorReturn(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        e.onError(new Throwable("模拟一个错误"));
                    }
                    e.onNext(i);
                }
                e.onComplete();
            }
        }).onErrorReturn(new Function<Throwable, Integer>() {
            @Override
            public Integer apply(Throwable throwable) throws Exception {
                return 400;
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }

    /**
     * 输出
     * onNext: 0
     * onNext: 1
     * onNext: 2
     * onNext: 3
     * onNext: 4
     * onNext: 400
     * onNext: 4000
     * onNext: 40000
     * onComplete:
     * <p>
     * 需要注意⚠️
     * onErrorReturn发生error后会自动调用onComplete()，而onErrorResumeNext需要根据需要手动调用，
     * 同时也不会触发观察者的onError()回调，除非onErrorResumeNext中再手动调用e.onError()
     *
     * @param view
     */
    public void onErrorResumeNext(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        e.onError(new Throwable("模拟一个错误"));
                    }
                    e.onNext(i);
                }
                e.onComplete();
            }
        }).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends Integer>>() {
            @Override
            public ObservableSource<? extends Integer> apply(Throwable throwable) throws Exception {
                return Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                        e.onNext(400);
                        e.onNext(4000);
                        e.onNext(40000);
                        e.onComplete();
                    }
                });
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }

    /**
     * 输出
     * onNext: 0
     * onNext: 1
     * onNext: 2
     * onNext: 3
     * onNext: 4
     * onError: 模拟一个错误
     * <p>
     * 跟onErrorResumeNext的运行结果对比，很明显没有400、4000、40000，说明新的Observer并不会起作用，
     * 这里用的是Throwable，如果是用Exception，同样也会有400、4000、40000，
     * 所以：onErrorResumeNext和onExceptionResumeNext对Exception的处理是一样的流程，
     * 区别在于对Error处理的时候，是否会使用新的Observer发送消息，也就是onExceptionResumeNext不处理Error，
     * 直接回调观察者的onError ()，onErrorResumeNext都处理，不会再调用观察者的onError ()。
     *
     * @param view
     */
    public void onExceptionResumeNext(View view) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        e.onError(new Throwable("模拟一个错误"));
                    }
                    e.onNext(i);
                }
                e.onComplete();
            }
        }).onExceptionResumeNext(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                observer.onNext(400);
                observer.onNext(4000);
                observer.onNext(40000);
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }

    public void retry(View view) {

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        e.onError(new IllegalAccessError("模拟错误"));
                    }
                    e.onNext(i);
                }
                e.onComplete();
            }
        })
                //不设置重试次数
                .retry(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable throwable) throws Exception {
                        //true表示不停地重试 ，  false表示不重试
                        return true;
                    }
                })

                //设置重试次数
//                .retry(3, new Predicate<Throwable>() {
//                    @Override
//                    public boolean test(Throwable throwable) throws Exception {
//                        //true表示按设置的次数重试 ，  false表示不重试
//                        return true;
//                    }
//                })

                //可获取重试次数
//                .retry(new BiPredicate<Integer, Throwable>() {
//                    @Override
//                    public boolean test(Integer integer, Throwable throwable) throws Exception {
//                        //相对上面两种，这个integer表示重试次数， 返回值跟上面一样
//                        return true;
//                    }
//                })

                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                    }
                });
    }

    //endregion

    //region 线程切换

    /**
     * 默认发送和接收都是在主线程
     * <p>
     * 输出
     * E/MainActivity: 发送: main
     * E/MainActivity: 接收: main
     *
     * @param view
     */
    public void schedulers(View view) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Log.e(TAG, "发送: " + Thread.currentThread().getName());
                e.onNext("123");
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.e(TAG, "接收: " + Thread.currentThread().getName());
            }
        });
    }

    /**
     * 可以通过subscribeOn()会同时修改观察者和被观察者的线程，
     * 通过observeOn()只设置观察者线程，
     * 通过AndroidSchedulers.mainThread()得到主线程，
     * 通过Schedulers.io()得到子线程：
     * <p>
     * 这一段，先通过subscribeOn(Schedulers.io())把观察者和被观察者都设置到子线程，
     * 如果不写下面这句observeOn(AndroidSchedulers.mainThread())，会输出：
     * E/MainActivity: 发送: RxCachedThreadScheduler-1
     * E/MainActivity: 接收:  RxCachedThreadScheduler-1
     * <p>
     * 但是下面又用observeOn(AndroidSchedulers.mainThread())把观察者改回子线程，所以输出：
     * E/MainActivity: 发送: RxCachedThreadScheduler-1
     * E/MainActivity: 接收: main
     *
     * @param view
     */
    public void schedulersSwitch(View view) {
        Observable
                .create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> e) throws Exception {
                        Log.e(TAG, "发送: " + Thread.currentThread().getName());
                        e.onNext("123");
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e(TAG, "接收: " + Thread.currentThread().getName());
                    }
                });
    }
    //endregion

    //region 背压模式

//    当上下游运行在不同的线程中，且上游发射数据的速度大于下游接收处理数据的速度时，就会产生背压问题，
//    内存使用越来越多，这时候就需要用Flowable去处理。Flowable会对上游发送的时间进行缓存，
//    缓存池也满了（超出128）的时候会有4种不通的处理方式：
//
//    BackpressureStrategy.ERROR：就会抛出异常
//    BackpressureStrategy.DROP：把后面发射的事件丢弃
//    BackpressureStrategy.LATEST：把前面发射的事件丢弃
//    BackpressureStrategy.BUFFER：这种不会有上限，但是如果上游发送太多，也会造成内存使用越来越大

    /**
     * 基本使用
     * Flowable的使用跟Observable很类似，简单使用:
     *
     * @param view
     */
    public void backpressure(View view) {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                // 改成129就会崩溃
                for (int i = 0; i < 128; i++) {
                    e.onNext(i); // todo 1
                }
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG, "接收: " + integer);
                    }
                });
    }

    /**
     * 这一会发现观察者收不到任何消息，这里跟Observable有个区别，就是订阅的方法subscribe()的参数，
     * Observable订阅对应的是Observer，而Flowable对应的是Subscriber，
     * Observer和Subscriber对应的回调onSubscribe(..)参数不同，
     * Subscriber的onSubscribe(..)参数拿到的是一个Subscription，这个需要主动去取数据，比如：
     *
     * @param view
     * @Override public void onSubscribe(Subscription s) {
     * s.request(10);
     * }
     * <p>
     * 这样就会onNext()中就会收到前10个。
     * 那这个使用就很灵活了，根据代码需要，可以在需要的地方主动调用s.request(..)，让观察者接收到数据。
     */
    public void backpressure1(View view) {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                // 改成129就会崩溃
                for (int i = 0; i < 128; i++) {
                    e.onNext(i); // todo 1
                }
                e.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e(TAG, "接收: " + integer);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
    //endregion

    //region 一个展示网络图片的例子

    private ProgressDialog progressDialog;
    private ImageView imageView;


    private void getImage(final String path) {
        Observable.just(path)
                // 通过map变换操作符把String转换成Bitmap
                .map(new Function<String, Bitmap>() {
                    @Override
                    public Bitmap apply(String s) throws Exception {
                        URL url = new URL(path);
                        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                        httpURLConnection.setConnectTimeout(5000);
                        int responseCode = httpURLConnection.getResponseCode();
                        if (HttpURLConnection.HTTP_OK == responseCode) {
                            Bitmap bitmap = BitmapFactory.decodeStream(httpURLConnection.getInputStream());
                            return bitmap;
                        }

                        return null;
                    }
                })
                // 下载图片在子线程中
                .subscribeOn(Schedulers.io())
                // 设置图片在主线程中
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {

                    @Override
                    // 开始操作前
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("正在下载中...");
                        progressDialog.show();
                    }

                    @Override
                    // 收到Bitmap
                    public void onNext(Bitmap bitmap) {
                        if (imageView != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                    }

                    @Override
                    // 下载错误
                    public void onError(Throwable e) {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        if (imageView != null) {
                            imageView.setImageResource(R.mipmap.ic_launcher);
                        }
                        Log.e(TAG, "onError: " + e.toString());
                    }

                    @Override
                    // 下载完成
                    public void onComplete() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                    }
                });
    }

    public void testImage(View view) {
        if (imageView == null) {
            imageView = findViewById(R.id.iv);
        }
        getImage("https://ss0.bdstatic.com/94oJfD_bAAcT8t7mm9GUKT-xh_/timg?image&quality=100&size=b4000_4000&sec=1571727836&di=20ca822ec7322483691659c9b4d64d58&src=http://5b0988e595225.cdn.sohucs.com/images/20181206/d8fd5e62fdee4479ae6bfb74b9b70f07.jpeg");
    }
    //endregion
}
