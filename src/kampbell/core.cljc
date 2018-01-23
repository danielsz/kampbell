(ns kampbell.core
  (:require [konserve.core :as k]
            [konserve.filestore :as f]
            [inflections.core :refer [plural]]
            [lang-utils.core :refer [seek p]]
            [#?(:clj clojure.spec.alpha :cljs cljs.spec.alpha) :as s]
            #?(:clj [clojure.core.async :as a :refer [<!! <! go]])
            #?(:cljs [cljs.core.async    :as a :refer [<!]])))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

(declare save-coll delete-coll get-coll)
;; housekeeping
#?(:clj
   (do
     (defn list-collections [store]
       (<!! (f/list-keys store)))
     (defn seed-db [store collections]
       (doseq [coll-name collections]
         (when-not (<!! (k/exists? store coll-name))
           (<!! (k/assoc-in store [coll-name] [])))))
     (defn delete-db! [store]
       (doseq [coll-name (list-collections store)]
         (delete-coll store (first coll-name)))))
   (defn delete-collections [store collections]
     (doseq [coll-name collections]
       (delete-coll store coll-name))
     (defn reset-db! [store]
       (delete-db! store)
       (seed-db store)))
   (defn copy-coll [store source target]
     (when-not (<!! (k/exists? store target))
       (let [coll (<!! (k/get-in store [source]))]
         (<!! (k/assoc-in store [target] coll))))
     (defn migrate-entity [store coll-name old-value new-value]
       (let [coll (<!! (get-coll store coll-name))
             new-coll (into [] (remove #(= old-value %) coll))
             new-coll (conj new-coll new-value)]
         (<!! (delete-coll store coll-name))
         (<!! (save-coll store coll-name new-coll))))))
;;

;; utils
(def equality-specs #{:created_at :created-at :updated-at :updated_at})

(defn same?
  ([x y]
   (same? x y equality-specs))
  ([x y specs]
   (let [x (apply dissoc x specs)
         y (apply dissoc y specs)]
     (= x y))))
;;
;; core operations
(defn get-coll [store coll-name]
 (k/get-in store [coll-name])) 

(def get-entities get-coll)

(defn save-coll [store coll-name coll]
  (k/assoc-in store [coll-name] coll))

(defn delete-coll [store coll-name]
  (k/dissoc store coll-name))

(defn update-entity [store coll v specs]
  (if (every? #(s/valid? % (% v)) specs)
    (k/update-in store [coll] (fn [coll]
                                (reduce (fn [xs y] (if (same? v y specs)
                                                    (conj xs v)
                                                    (conj xs y))) [] coll)))
    (throw (ex-info "Invalid input" (some #(s/explain-data % (% v)) specs)))))

(defn save-entity [store spec v]
  (if (s/valid? spec v)
    (go (let [entities (plural (name spec))
              coll (<! (k/get-in store [entities]))
              exists? (p same? v)]
          (when-not (some exists? coll) 
            (save-coll store entities (conj coll v)))))
    (throw (ex-info "Invalid input" (s/explain-data spec v)))))

(defn get-entity [store coll-name field v]
  (go
    (let [coll (get-coll store coll-name)
          field= (fn [x] (= v (field x)))]
      (seek field= (<! coll)))))

(defn delete-entity [store spec v]
  (go (let [entities (plural (name spec))
            coll (<! (k/get-in store [entities]))]
        (when (some #{v} coll)
          (save-coll store entities (into [] (filter #(not= v %) coll)))))))
