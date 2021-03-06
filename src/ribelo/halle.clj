(ns ribelo.halle
  (:refer-clojure
   :exclude [first last take take-last reductions every some map reduce]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def ^:const double-type               Double/TYPE)
(def ^:const double-array-type        (Class/forName "[D"))
(def ^:const double-double-array-type (Class/forName "[[D"))
(def ^:const long-type                 Long/TYPE)
(def ^:const long-array-type          (Class/forName "[J"))
(def ^:const long-long-array-type     (Class/forName "[[J"))

(defn double-array? [arr]        (= (type arr) double-array-type))
(defn double-double-array? [arr] (= (type arr) double-double-array-type))
(defn long-array? [arr]          (= (type arr) long-array-type))
(defn long-long-array? [arr]     (= (type arr) long-long-array-type))

(defprotocol SeqToPrimitive
  (->double-array         [xs])
  (->double-double-array  [xs])
  (->long-array           [xs])
  (->long-long-array      [xs])
  (->double-array-or-copy [xs])
  (->long-array-or-copy   [xs])
  (->vec                  [arr]))

(extend-protocol SeqToPrimitive
  java.util.Collection
  (->double-array [xs]
    (into-array double-type xs))
  (->double-double-array [xs]
    (into-array double-array-type (mapv ->double-array xs)))
  (->long-array [xs]
    (into-array long-type xs))
  (->long-long-array [xs]
    (into-array long-array-type (mapv ->long-array xs)))
  (->double-array-or-copy [xs]
    (into-array double-array-type xs))
  (->long-array-or-copy [xs]
    (into-array long-array-type xs))
  (->vec [xs]
    (if-not (instance? clojure.lang.PersistentVector xs) (vec xs) xs)))

(extend-type (Class/forName "[D")
  SeqToPrimitive
  (->double-array [arr] arr)
  (->double-array-or-copy [arr]
    (java.util.Arrays/copyOfRange ^doubles arr 0 (alength ^doubles arr)))
  (->vec [arr]
    (let [n (alength ^doubles arr)]
      (loop [i 0 acc (transient [])]
        (if (< i n)
          (recur (unchecked-inc-int i) (conj! acc (aget ^doubles arr i)))
          (persistent! acc))))))

(extend-type (Class/forName "[J")
  SeqToPrimitive
  (->long-array [arr] arr)
  (->long-array-or-copy [arr]
    (java.util.Arrays/copyOfRange ^longs arr 0 (alength ^longs arr)))
  (->vec [arr]
    (let [n (alength ^longs arr)]
      (loop [i 0 acc (transient [])]
        (if (< i n)
          (recur (unchecked-inc-int i) (conj! acc (aget ^longs arr i)))
          (persistent! acc))))))

(extend-type (Class/forName "[[D")
  SeqToPrimitive
  (->double-double-array [arr] arr)
  (->vec [arr]
    (let [n1 (alength ^"[[D" arr)]
      (loop [i1 0 acc1 (transient [])]
        (if (< i1 n1)
          (let [^doubles iarr (aget ^"[[D" arr i1)
                n2            (alength iarr)
                iv            (loop [i2 0 acc2 (transient [])]
                                (if (< i2 n2)
                                  (recur (unchecked-inc-int i2) (conj! acc2 (aget iarr i2)))
                                  (persistent! acc2)))]
            (recur (unchecked-inc-int i1) (conj! acc1 iv)))
          (persistent! acc1))))))

(extend-type (Class/forName "[[J")
  SeqToPrimitive
  (->long-long-array [arr] arr)
  (->vec [arr]
    (let [n1 (alength ^"[[J" arr)]
      (loop [i1 0 acc1 (transient [])]
        (if (< i1 n1)
          (let [^doubles iarr (aget ^"[[J" arr i1)
                n2            (alength iarr)
                iv            (loop [i2 0 acc2 (transient [])]
                                (if (< i2 n2)
                                  (recur (unchecked-inc-int i2) (conj! acc2 (aget iarr i2)))
                                  (persistent! acc2)))]
            (recur (unchecked-inc-int i1) (conj! acc1 iv)))
          (persistent! acc1))))))

(defprotocol Series
  (first [arr])
  (last [arr])
  (slice [arr start stop] [arr start])
  (-take [arr n])
  (-take-last [arr n])
  (-reductions [arr f])
  (-every [arr pred])
  (-some [arr pred])
  (-map
    [arr f]
    [arr1 arr2 f]
    [arr1 arr2 arr3 f]
    [arr1 arr2 arr3 arr4 f])
  (-reduce
    [arr f]
    [arr f init])
  (transpose [arr2d]))

(extend-protocol Series
  java.util.Collection
  (first [coll] (clojure.core/first coll))
  (last [coll]  (clojure.core/last coll))
  (slice
    ([coll start]
     (let [arr (->double-array coll)
           n   (alength ^doubles arr)]
       (slice arr start n)))
    ([coll start stop]
     (let [arr (->double-array coll)]
       (slice arr start stop))))
  (-take [coll n]
    (let [arr (->double-array coll)]
      (-take arr n)))
  (-take-last [coll n]
    (let [arr (->double-array coll)]
      (-take-last arr n)))
  (-reductions [coll f]
    (let [arr (->double-array coll)]
      (-reductions arr f)))
  (-every [coll f]
    (let [arr (->double-array coll)]
      (-every arr f)))
  (-some [coll f]
    (let [arr (->double-array coll)]
      (-some arr f)))
  (-map
    ([coll f]
     (let [arr (->double-array coll)]
       (-map arr f)))
    ([c1 c2 f]
     (let [a1 (->double-array c1)
           a2 (->double-array c2)]
       (-map a1 a2 f)))
    ([c1 c2 c3 f]
     (let [a1 (->double-array c1)
           a2 (->double-array c2)
           a3 (->double-array c3)]
       (-map a1 a2 a3 f)))
    ([c1 c2 c3 c4 f]
     (let [a1 (->double-array c1)
           a2 (->double-array c2)
           a3 (->double-array c3)
           a4 (->double-array c4)]
       (-map a1 a2 a3 a4 f))))
  (transpose [coll2d]
    (let [arr2d (->double-double-array coll2d)]
      (transpose arr2d))))

(defn take       [n coll]        (-take coll n       ))
(defn take-last  [n coll]        (-take-last coll n  ))
(defn reductions [f coll]        (-reductions coll f ))
(defn every      [pred coll]     (-every coll pred   ))
(defn some       [pred coll]     (-some  coll pred   ))
(defn map       ([f c]           (-map c           f ))
                ([f c1 c2]       (-map c1 c2       f ))
                ([f c1 c2 c3]    (-map c1 c2 c3    f ))
                ([f c1 c2 c3 c4] (-map c1 c2 c3 c4 f)))

(defn reduce
  ([f coll]        (-reduce (->double-array coll) f))
  ([f val coll]    (-reduce (->double-array coll) f val)))

(extend-type (Class/forName "[D")
  Series
  (first [arr]
    (aget ^doubles arr 0))
  (last [arr]
    ^double (aget ^doubles arr (unchecked-dec-int (alength ^doubles arr))))
  (slice
    ([arr ^long start ^long stop]
     (java.util.Arrays/copyOfRange ^doubles arr start (Math/min (int stop) (alength ^doubles arr))))
    ([arr ^long start]
     (java.util.Arrays/copyOfRange ^doubles arr start (alength ^doubles arr))))
  (-take
    [arr ^long n]
    (java.util.Arrays/copyOfRange ^doubles arr 0 (Math/min (int n) (alength ^doubles arr))))
  (-take-last [arr ^long n]
    (java.util.Arrays/copyOfRange
     ^doubles arr (Math/max 0 (- (alength ^doubles arr) n)) (alength ^doubles arr)))
  (-reductions [arr f]
    (let [n (alength ^doubles arr)
          r (double-array n)]
      (loop [i 0 b 1.0]
        (if (< i n)
          (let [tmp (double (f b (aget ^doubles arr i)))]
            (aset r i tmp)
            (recur (unchecked-inc i) tmp))
          r))))
  (-every [arr pred]
    (let [n (alength ^doubles arr)]
      (loop [i 0]
        (if (< i n)
          (if ^boolean (pred (aget ^doubles arr i))
            (recur (unchecked-inc i))
            false)
          true))))
  (-some [arr pred]
    (let [n (alength ^doubles arr)]
      (loop [i 0]
        (if (< i n)
          (if-not ^boolean (pred (aget ^doubles arr i))
            (recur (unchecked-inc i))
            true)
          false))))
  (-map
    ([arr f]
     (let [n (alength ^doubles arr)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles arr i))))
       r))
    ([a1 a2 f]
     (let [n (alength ^doubles arr)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles a1 i)
                              (aget ^doubles a2 i))))
       r)))
  (-map
    ([arr f]
     (let [n (alength ^doubles arr)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles arr i))))
       r))
    ([a1 a2 f]
     (let [n (alength ^doubles a1)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles a1 i)
                              (aget ^doubles a2 i))))
       r))
    ([a1 a2 arr3 f]
     (let [n (alength ^doubles a1)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles a1 i)
                              (aget ^doubles a2 i)
                              (aget ^doubles arr3 i))))
       r))
    ([a1 a2 arr3 arr4 f]
     (let [n (alength ^doubles a1)
           r (double-array n)]
       (dotimes [i n]
         (aset r i ^double (f (aget ^doubles a1 i)
                              (aget ^doubles a2 i)
                              (aget ^doubles arr3 i)
                              (aget ^doubles arr4 i))))
       r)))
  (-reduce
    ([arr f]
     (let [n (alength ^doubles arr)]
       (loop [i 1 r (aget ^doubles arr 0)]
         (if (< i n)
           (recur (unchecked-inc-int i) (double (f r ^double (aget ^doubles arr i))))
           r))))
    ([arr f init]
     (let [n (alength ^doubles arr)]
       (loop [i 0 r (double init)]
         (if (< i n)
           (recur (unchecked-inc-int i) (double (f r ^double (aget ^doubles arr i))))
           r))))))

(extend-type (Class/forName "[[D")
  Series
  (transpose [arr2d]
    (let [n1 (alength ^"[[D" arr2d)
          n2 (alength ^doubles (aget ^"[[D"  arr2d 0))
          ro (make-array double-array-type n2)]
      (loop [i1 0]
        (if (< i1 n2)
          (let [ri (make-array double-type n1)]
            (aset ^"[[D" ro i1 ri)
            (loop [i2 0]
              (if (< i2 n1)
                (let [arr (aget ^"[[D" arr2d i2)
                      v   (aget ^doubles arr i1)]
                  (aset ^doubles ri i2 v)
                  (recur (unchecked-inc-int i2)))))
            (recur (unchecked-inc-int i1)))
          ro)))))

(extend-type (Class/forName "[J")
  Series
  (first [arr] (aget ^longs arr 0))
  (last [arr]
    (aget ^longs arr (unchecked-dec (alength ^longs arr))))
  (slice
    ([arr ^long start ^long stop]
     (java.util.Arrays/copyOfRange ^longs arr start (Math/min (int stop) (alength ^longs arr))))
    ([arr ^long start]
     (java.util.Arrays/copyOfRange ^longs arr start (alength ^longs arr))))
  (-take
    [arr ^long n]
    (java.util.Arrays/copyOfRange ^longs arr 0 (Math/min (int n) (alength ^longs arr))))
  (-take-last [arr ^long n]
    (java.util.Arrays/copyOfRange
     ^longs arr (Math/max 0 (- (alength ^longs arr) n)) (alength ^longs arr)))
  (-reductions [arr f]
    (let [n (alength ^longs arr)
          r (long-array n)]
      (loop [i 0 b 1]
        (if (< i n)
          (let [tmp (long (f b (aget ^longs arr i)))]
            (aset r i tmp)
            (recur (unchecked-inc i) tmp))
          r))))
  (-every [arr pred]
    (let [n (alength ^longs arr)]
      (loop [i 0]
        (if (< i n)
          (if ^boolean (pred (aget ^longs arr i))
            (recur (unchecked-inc i))
            false)
          true))))
  (asome [arr pred]
    (let [n (alength ^longs arr)]
      (loop [i 0]
        (if (< i n)
          (if-not ^boolean (pred (aget ^longs arr i))
            (recur (unchecked-inc i))
            true)
          false))))
  (-map
    ([arr f]
     (let [n (alength ^longs arr)
           r (long-array n)]
       (dotimes [i n]
         (aset r i ^long (f (aget ^longs arr i))))
       r))
    ([a1 a2 f]
     (let [n (alength ^longs a1)
           r (long-array n)]
       (dotimes [i n]
         (aset r i ^long (f (aget ^longs a1 i)
                            (aget ^longs a2 i))))
       r))
    ([a1 a2 arr3 f]
     (let [n (alength ^longs a1)
           r (long-array n)]
       (dotimes [i n]
         (aset r i ^long (f (aget ^longs a1 i)
                            (aget ^longs a2 i)
                            (aget ^longs arr3 i))))
       r))
    ([a1 a2 arr3 arr4 f]
     (let [n (alength ^longs a1)
           r (long-array n)]
       (dotimes [i n]
         (aset r i ^long (f (aget ^longs a1 i)
                            (aget ^longs a2 i)
                            (aget ^longs arr3 i)
                            (aget ^longs arr4 i))))
       r))))

(extend-type (Class/forName "[[J")
  Series
  (transpose [arr2d]
    (let [n1 (alength ^"[[J" arr2d)
          n2 (alength ^longs (aget ^"[[J"  arr2d 0))
          ro (make-array long-array-type n2)]
      (loop [i1 0]
        (if (< i1 n2)
          (let [ri (make-array long-type n1)]
            (aset ^"[[J" ro i1 ri)
            (loop [i2 0]
              (if (< i2 n1)
                (let [arr (aget ^"[[J" arr2d i2)
                      v   (aget ^longs arr i1)]
                  (aset ^longs ri i2 v)
                  (recur (unchecked-inc-int i2)))))
            (recur (unchecked-inc-int i1)))
          ro)))))
