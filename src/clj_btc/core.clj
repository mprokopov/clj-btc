(ns clj-btc.core
  (:require [clojure.data.json :as json])
  (:require [org.httpkit.client :as http])
  (:require [clojure.java.io :as jio])
  (:use [clojure.string :only (split)]))

(def id-num (java.util.concurrent.atomic.AtomicInteger.))

(defn- parse-config
  "Return a Map of properties from the given file, or from the default configuration file"
  ([] (parse-config (default-config-file)))
  ;; Straight from http://stackoverflow.com/questions/7777882/loading-configuration-file-in-clojure-as-data-structure
  ([file-name]
     (let [config
           (with-open [^java.io.Reader reader (jio/reader file-name)]
             (let [props (java.util.Properties.)]
               (.load props reader)
               (into {} (for [[k v] props] [(keyword k) (read-string v)]))))
           ;; default values
           testnet (and (number? (config :testnet)) (> (config :testnet) 0))
           rpcport (get config :rpcport (if testnet 18332 8332))]
       (assoc config :testnet testnet :rpcport rpcport))))

(defn- default-config-file
  "Return the full path (as a vector of strings) to the default bitcoin.conf file,
   by OS (default Linux). This is in accordance with
   https://en.bitcoin.it/wiki/Running_Bitcoin#Bitcoin.conf_Configuration_File"
  []
  (let [nix-data-dir #(System/getProperty "user.home")
        win-data-dir #(System/getenv "AppData")
        os-name (System/getProperty "os.name")
        path (case (first (split os-name #"\s"))
               "Mac" [(nix-data-dir) "Library" "Application Support"
                      "Bitcoin" "bitcoin.conf"]
               "Windows" [(win-data-dir) "Bitcoin" "bitcoin.conf"]
               ;; "Linux" is the default
               [(nix-data-dir) ".bitcoin" "bitcoin.conf"])]
    (str (apply jio/file path))))

(defn getinfo
  "FIXME: This must be deleted"
  ([] (getinfo (parse-config)))
  ([config]
     (-> @(http/post
           "http://127.0.0.1:18332"
           {:basic-auth [(config :rpcuser) (config :rpcpassword)],
            :headers {"Content-Type" "application/json; charset=utf-8"},
            :body (json/write-str {"version" "2.0", "params" [],
                                   "method" "getinfo", "id" (.incrementAndGet id-num)})})
         :body json/read-json :result)))

(defn getaccount
  "Returns the account associated with the given address."
  ([bitcoinaddress] (getaccount bitcoinaddress (parse-config)))
  ([bitcoinaddress config]
     (let [resp @(http/post
                  (str "http://127.0.0.1" (config :rpcport))
                  {:basic-auth [(config :rpcuser) (config :rpcpassword)],
                   :headers {"Content-Type" "application/json; charset=utf-8"},
                   :body (json/write-str {"version" "2.0", "params" [bitcoinaddress],
                                          "method" "getaccount",
                                          "id" (.incrementAndGet id-num)})})]
       (if (= 200 (:status resp))
         (-> resp :body json/read-json :result)
         (-> resp :body json/read-json :error)))))

(defn importprivkey
  "Adds a private key (as returned by dumpprivkey) to your wallet.
   This may take a while, as a rescan is done, looking for existing transactions.
   Optional [rescan] parameter added in 0.8.0.
   Supported Usage:
    + bitcoinprivkey is required
    + config is optional (default file will be parsed)
    + label is optional
    + rescan is optional (default to false)
    (importprivkey :bitcoinprivkey \"KEY\")
     ;;=> uses default config, no label rescan is false
    (importprivkey :config {:configs :map} :bitcoinprivkey \"KEY\")
     ;;=> no label and rescan is false
    (importprivkey
   "
  [{:keys [config bitcoinprivkey label rescan]
    :as args
    :or {config (parse-config), rescan false}}]
  {:pre [(string? bitcoinprivkey)
         (map? config)]}

  )
