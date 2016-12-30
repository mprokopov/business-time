(ns business-time.core
  (:require [joda-time :as t]
            [joda-time.accessors :as a])
  (:import [org.joda.time Seconds Duration]
           [org.joda.time.format PeriodFormatterBuilder]))

(defn duration-from-h-m [^Integer hours ^Integer minutes]
  (t/duration {:start 0 :period (t/period {:hours hours :minutes minutes})}))

(defn duration-from-seconds [secs]
  (t/duration (* 1000 (t/seconds-in (t/seconds secs)))))

(defn- appendMinus
  "in case Duration is negative prepend 'st'"
  [^PeriodFormatterBuilder result ^Duration duration st]
  (if (< (t/seconds-in duration) 0)
    (.appendPrefix result st)
    result))

(defn print
  "prints duration in format HH:MM with zeroes appended with negation"
  [^Duration duration]
  (let [formatter (-> (PeriodFormatterBuilder.)
                      (appendMinus duration "-")
                      (.minimumPrintedDigits 2)
                      (.printZeroAlways)
                      (.appendHours)
                      (.appendSeparator ":")
                      (.appendMinutes)
                      (.toFormatter))]
    (.print formatter (t/abs (.toPeriod duration)))))

(def schedule
  { :business-days [true true true true true false false]
    :business-hours [ ;; Monday
                      {:from {:hours 9 :minutes 0}
                        :to {:hours 18 :minutes 0}
                        :timezone "Europe/Kiev"}
                      {:from {:hours 9 :minutes 0}
                        :to {:hours 18 :minutes 0}
                        :timezone "Europe/Kiev"}
                      {:from {:hours 9 :minutes 0}
                        :to {:hours 18 :minutes 0}
                        :timezone "Europe/Kiev"}
                      {:from {:hours 9 :minutes 0}
                        :to {:hours 18 :minutes 0}
                        :timezone "Europe/Kiev"}
                      {:from {:hours 9 :minutes 0}
                        :to {:hours 18 :minutes 0}
                        :timezone "Europe/Kiev"}]})

(defn business-day?
  "returns true if dt is business day"
  [dt]
  (let [day (dec (a/day-of-week dt))
        days (schedule :business-days)]
    (nth days day)))

(defn timings-for-day
  "returns map of working hours {:from start :to end} of dt"
  [dt]
  (when (business-day? dt)
    (let [day (dec (a/day-of-week dt))]
      (-> schedule
          :business-hours
          (nth day)))))

(defn date-time-h-m
  "created date-time from hour, minutes and timezone"
  [dt hour minutes timezone]
  (t/with-zone
    (t/date-time {:partial (t/partial {:hourOfDay hour :minuteOfHour minutes}) :base dt})
    (t/timezone timezone)))

(defn business-hours-interval
  "returns working hours interval for dt day"
  [dt]
  (let [{:keys [from to timezone]} (timings-for-day dt)
        {from-hour :hours  from-minutes :minutes} from
        {to-hour :hours  to-minutes :minutes} to
        start-time (date-time-h-m dt from-hour from-minutes timezone)
        end-time (date-time-h-m dt to-hour to-minutes timezone)]
    (when (business-day? dt)
      (t/interval start-time end-time))))


(defn next-business-day
  "returns next business day for dt according to schedule"
  [dt]
  (let [next-day (t/plus dt (t/days 1))]
    (if (business-day? next-day)
      next-day
      (next-business-day next-day))))

(defn prev-business-day [dt]
  (let [prev-day (t/minus dt (t/days 1))]
    (if (business-day? prev-day)
      prev-day
      (prev-business-day prev-day))))

(defn prev-business-day-end [dt]
  (-> (prev-business-day dt)
      (business-hours-interval)
      (t/end)))

(defn next-business-day-start
  "returns starting time for next business day for dt"
  [dt]
  (-> (next-business-day dt)
      (business-hours-interval)
      (t/start)))

(defn from-business-duration
  "duration is integer seconds,
  returns date and time according to work schedule,
  dt is optional staring point, unless dt specified now is used"
  ([duration] (from-business-duration duration (t/local-time)))
  ([duration dt]
   (loop [dt2 (if (business-day? dt) dt (next-business-day-start dt))
          accu (t/seconds-in duration)]
     (let [ period (t/seconds accu)
            over (t/overlap (t/interval dt2 (t/plus dt2 period)) (business-hours-interval dt2))] ;; interval overelapping business hours
       (if-not (t/contains? (business-hours-interval dt2) (t/interval dt2 (t/plus dt2 period)))
         (recur (next-business-day-start dt2) (- accu (t/seconds-in over)))
         (t/plus dt2 (t/seconds accu)))))))

(defn business-seconds-till
  "returns integer seconds between two dates,
  in case dt1 is not specified, now is used.
  Could be negative"
  ([dt2] (business-seconds-till (t/date-time) dt2))
  ([dt1 dt2]
   (if (t/after? dt1 dt2)
     (t/negate (business-seconds-till dt2 dt1))
     (let [dt2n (if (business-day? dt2) dt2 (prev-business-day-end dt2))]
       (loop [dt (if (business-day? dt1) dt1 (next-business-day-start dt1))
              accu 0]
         (let [bi (business-hours-interval dt)
               i (t/interval dt dt2n)
               over (try (t/seconds-in (t/overlap bi i)))]
           (if (t/contains? bi i)
             (+ over accu)
             (recur (next-business-day-start dt) (+ accu over)))))))))
