# business-time

A Clojure library designed to calculate business time and durations according to working schedule with timezones.
It uses famous Java joda-time library.

This is useful when you need to calculate some ticket response/resolve time, SLA and so on. Inspired by article https://engineering.helpshift.com/2016/timezones-FP/.

## Usage

Add the following dependency to your project.clj:

```clj
[mprokopov/business-time "0.1.0"]
```

also add
```clj
[clojure.joda-time "0.7.0"]
```
to use handy date-time functions.

```clj
(ns 'business-time
  (:require [joda-time :as j])

(def business-time/schedule
  { :business-days [true true true true true false false] ;; Sat and Sun are days off
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
```

```clj
(business-time? (j/date-time)) ;;returns true of false if supplied date is working day
=> true
```
```clj
(business-hours-interval (j/date-time)) ;;returns working interval of supplied date, i.e. usually 9:00 - 18:00
=>#object[org.joda.time.Interval 0x1300e802 "2016-12-30T09:00:21.133+02:00/2016-12-30T18:00:21.133+02:00"]
```

```clj
(from-business-duration duration (j/date-time)) ;;returns date-time with respect to working schedule, duration specified as integer in seconds from date specified by second argument.
=> #object[org.joda.time.DateTime 0xb842a0b "2016-12-30T15:10:06.462+02:00"]
```

```clj
(business-seconds-till (j/plus (j/date-time) (j/days 2))) ;;returns integer duration in seconds between two dates according to working schedule. If second parameter ommited, current date-time is used.
=> 13679
```

## License

Copyright © 2016 Maksym Prokopov

Distributed under the MIT License.
