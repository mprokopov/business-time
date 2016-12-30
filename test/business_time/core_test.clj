(ns business-time.core-test
  (:require [clojure.test :refer :all]
            [business-time.core :refer :all]
            [joda-time :as t]
            [joda-time.accessors :as a]))

(def day-work (t/date-time 2016 12 28))
(def time-work (t/date-time 2016 12 28 13 30))
(def day-off (t/date-time 2016 12 31))
(def start-day-work (t/date-time 2016 12 28 9 0))
(def end-day-work (t/date-time 2016 12 28 18 0))
(def time-off-work (t/partial {:hourOfDay 15 :minuteOfHour 30}))
(def date-time-off-work (t/date-time {:partial time-off-work :base day-off}))

(deftest test-business-day
  (is (= (business-day? day-work) true))
  (is (= (business-day? day-off) false)))

(deftest test-timings-for-day
  (is (= (timings-for-day day-work)
         { :from { :hours 9 :minutes 0}
           :to { :hours 18 :minutes 0}
           :timezone "Europe/Kiev"})
      (= (timings-for-day day-off)
         nil)))

(deftest test-business-hours-interval
  (is (= (business-hours-interval day-work))
      (t/interval start-day-work end-day-work)))

(deftest test-next-business-day
  (is (= (next-business-day day-work)
         (t/plus day-work (t/days 1)))
      (= (next-business-day day-off)
         (t/plus day-off (t/days 2)))))

(deftest test-from-business-duration
  (is (= (from-business-duration  43200 time-work) ;; 12H -> 16:30 next day
         (t/date-time 2016 12 29 16 30)))
  (is (= (from-business-duration (duration-from-h-m 5 30) date-time-off-work)
         (t/date-time 2017 01 02 14 30))));; 5H30M

(deftest test-business-seconds-till
  (is
    (= (business-seconds-till (t/date-time 2016 12 28 13 00) (t/date-time 2016 12 28 14 30))
      5400))
  (is (= (business-seconds-till (t/date-time 2016 12 28 14 30) (t/date-time 2016 12 28 13 00)))
      -5400)
  (testing "Days off"
    (let [t1 (t/date-time "2016-07-02T09:27:02.000+03:00")
          t2 (t/date-time "2016-06-29T17:04:51.000+03:00")]
      (is
        (= (business-seconds-till t2 t1)
           68111)))))


(deftest test-print
  (is (= (print (t/duration -11111111))
        "-03:05"))
  (is (= (print (t/duration 11111111))
        "03:05")))
